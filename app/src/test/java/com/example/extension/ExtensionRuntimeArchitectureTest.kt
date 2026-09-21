package com.example.extension

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.extension.builtin.TestAnimeExtension
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.DefaultExtensionManager
import com.example.domain.extension.factory.DefaultExtensionRuntimeFactory
import com.example.domain.extension.loader.DefaultExtensionLoader
import com.example.domain.extension.logging.CircuitBreakerState
import com.example.domain.extension.logging.DefaultExtensionLogger
import com.example.domain.extension.logging.ExtensionLogLevel
import com.example.domain.extension.manager.DefaultExtensionRuntimeManager
import com.example.domain.extension.parser.DefaultExtensionManifestParser
import com.example.domain.extension.runtime.BuiltInExtensionRuntime
import com.example.domain.extension.runtime.ExtensionRuntimeState
import com.example.domain.extension.runtime.IsolatedAnimeExtension
import com.example.domain.extension.validator.DefaultExtensionValidator
import com.example.domain.extension.validator.ExtensionValidationResult
import com.example.domain.model.Anime
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.ExtensionRepository
import com.example.domain.model.ExtensionType
import com.example.domain.model.RepositoryIndex
import com.example.domain.model.VideoSource
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExtensionRuntimeArchitectureTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var manifestParser: DefaultExtensionManifestParser
    private lateinit var validator: DefaultExtensionValidator
    private lateinit var logger: DefaultExtensionLogger
    private lateinit var builtInRuntime: BuiltInExtensionRuntime
    private lateinit var runtimeFactory: DefaultExtensionRuntimeFactory
    private lateinit var loader: DefaultExtensionLoader
    private lateinit var runtimeManager: DefaultExtensionRuntimeManager

    @Before
    fun setUp() {
        manifestParser = DefaultExtensionManifestParser()
        validator = DefaultExtensionValidator()
        logger = DefaultExtensionLogger(maxLogsPerExtension = 10)
        builtInRuntime = BuiltInExtensionRuntime(logger)
        runtimeFactory = DefaultExtensionRuntimeFactory(builtInRuntime)
        loader = DefaultExtensionLoader(
            validator = validator,
            runtimeFactory = runtimeFactory,
            logger = logger,
            currentAppVersion = "1.0.0"
        )
        runtimeManager = DefaultExtensionRuntimeManager(
            builtInRuntime = builtInRuntime,
            runtimeFactory = runtimeFactory,
            validator = validator,
            logger = logger,
            currentAppVersion = "1.0.0"
        )
    }

    @Test
    fun testExtensionManifestParser_parsesJsonCorrectly() {
        val rawJson = """
            {
                "id": "moe.anime.sample",
                "name": "Sample Anime Source",
                "version": "1.2.3",
                "lang": "en",
                "type": "anime",
                "iconUrl": "https://example.com/icon.png",
                "description": "High quality anime stream",
                "downloadUrl": "https://example.com/ext.apk",
                "minAppVersion": "1.0.0",
                "author": "AnimeDev"
            }
        """.trimIndent()

        val result = manifestParser.parseManifest(rawJson)
        assertTrue("Parser should succeed", result is AppResult.Success)
        val manifest = (result as AppResult.Success).data

        assertEquals("moe.anime.sample", manifest.id)
        assertEquals("Sample Anime Source", manifest.name)
        assertEquals("1.2.3", manifest.version)
        assertEquals("en", manifest.language)
        assertEquals(ExtensionType.ANIME, manifest.type)
        assertEquals("https://example.com/icon.png", manifest.iconUrl)
        assertEquals("AnimeDev", manifest.author)

        // Test serialization round-trip
        val serialized = manifestParser.serializeManifest(manifest)
        val reParsed = manifestParser.parseManifest(serialized)
        assertTrue(reParsed is AppResult.Success)
        assertEquals(manifest.id, (reParsed as AppResult.Success).data.id)
    }

    @Test
    fun testExtensionManifestParser_parsesRepositoryIndex() {
        val repoJson = """
            {
                "name": "Community Hub",
                "version": 2,
                "extensions": [
                    {
                        "id": "source.one",
                        "name": "Source 1",
                        "version": "1.0.0"
                    },
                    {
                        "id": "source.two",
                        "name": "Source 2",
                        "version": "2.0.0",
                        "language": "ja"
                    }
                ]
            }
        """.trimIndent()

        val result = manifestParser.parseRepositoryIndex(repoJson)
        assertTrue("Repository index parse should succeed", result is AppResult.Success)
        val index = (result as AppResult.Success).data
        assertEquals("Community Hub", index.name)
        assertEquals(2, index.version)
        assertEquals(2, index.extensions.size)
        assertEquals("source.one", index.extensions[0].id)
        assertEquals("ja", index.extensions[1].language)
    }

    @Test
    fun testExtensionValidator_detectsInvalidAndSecurityViolations() {
        // 1. Blank ID & name
        val invalidManifest = ExtensionManifest(
            id = "",
            name = "",
            version = "1.0.0",
            language = "en"
        )
        val invalidResult = validator.validate(invalidManifest)
        assertTrue("Should detect invalid manifest", invalidResult is ExtensionValidationResult.Invalid)

        // 2. Illegal chars in ID
        val badIdManifest = ExtensionManifest(
            id = "bad id with spaces!",
            name = "Bad ID Extension",
            version = "1.0.0",
            language = "en"
        )
        val badIdResult = validator.validate(badIdManifest)
        assertTrue("Should reject invalid id regex", badIdResult is ExtensionValidationResult.Invalid)

        // 3. Security violation in download URL
        val dangerousManifest = ExtensionManifest(
            id = "security.exploit",
            name = "Exploit Extension",
            version = "1.0.0",
            language = "en",
            downloadUrl = "file:///sdcard/malicious.apk"
        )
        val secResult = validator.validate(dangerousManifest)
        assertTrue("Should reject file:// protocol in downloadUrl", secResult is ExtensionValidationResult.Invalid)

        // 4. Incompatible version requirement
        val futureManifest = ExtensionManifest(
            id = "future.extension",
            name = "Future Extension",
            version = "1.0.0",
            language = "en",
            minAppVersion = "9.9.9" // Requires future app version
        )
        val compatResult = validator.validate(futureManifest, currentAppVersion = "1.0.0")
        assertTrue("Should detect incompatible minAppVersion", compatResult is ExtensionValidationResult.Incompatible)

        // 5. Valid manifest
        val validManifest = ExtensionManifest(
            id = "valid.extension.test",
            name = "Valid Extension",
            version = "1.0.0",
            language = "en",
            downloadUrl = "https://example.com/valid.apk",
            minAppVersion = "1.0.0"
        )
        val validResult = validator.validate(validManifest, currentAppVersion = "1.0.0")
        assertTrue("Should accept valid manifest", validResult.isValid)
    }

    @Test
    fun testExtensionLogger_buffersAndIsolatesLogs() {
        val extId = "test.logger.ext"

        logger.d(extId, "TestTag", "Debug message")
        logger.i(extId, "TestTag", "Info message")
        logger.w(extId, "TestTag", "Warning message")
        logger.e(extId, "TestTag", "Error message", RuntimeException("Crash simulation"))

        val logs = logger.getLogs(extId)
        assertEquals(4, logs.size)
        assertEquals(ExtensionLogLevel.DEBUG, logs[0].level)
        assertEquals(ExtensionLogLevel.ERROR, logs[3].level)
        assertEquals("Crash simulation", logs[3].error)

        // Ensure other extensions have isolated empty logs
        val otherLogs = logger.getLogs("other.unknown.ext")
        assertTrue("Unrelated extensions should have empty logs", otherLogs.isEmpty())

        // Clear logs
        logger.clearLogs(extId)
        assertTrue(logger.getLogs(extId).isEmpty())
    }

    @Test
    fun testIsolatedAnimeExtension_circuitBreakerAndTelemetry() = runTest(testDispatcher) {
        val failingExtension = object : AnimeExtension {
            override val id = "failing.ext"
            override val name = "Failing Provider"
            override val lang = "en"
            override val iconUrl = ""
            override val baseUrl = "https://failing.example"
            override val manifest = ExtensionManifest(id, name, "1.0.0", lang)

            override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
                throw RuntimeException("Network Scraper 500 Internal Error")
            }
            override suspend fun getLatestAnime(page: Int): AppResult<List<Anime>> = throw RuntimeException()
            override suspend fun searchAnime(query: String, page: Int, filters: Map<String, Any>): AppResult<List<Anime>> = throw RuntimeException()
            override suspend fun getAnimeDetails(sourceAnimeId: String): AppResult<Anime> = throw RuntimeException()
            override suspend fun getEpisodes(sourceAnimeId: String): AppResult<List<Episode>> = throw RuntimeException()
            override suspend fun getEpisodeStreams(sourceEpisodeId: String, extra: Map<String, String>): AppResult<List<VideoSource>> = throw RuntimeException()
        }

        val isolated = IsolatedAnimeExtension(
            delegate = failingExtension,
            logger = logger,
            maxConsecutiveFailures = 3,
            circuitCooldownMs = 10_000L
        )

        // 1st failure
        val res1 = isolated.getPopularAnime(1)
        assertTrue(res1 is AppResult.Error)
        assertEquals(1, isolated.getStats().failedRequests)
        assertEquals(CircuitBreakerState.CLOSED, isolated.getStats().circuitState)

        // 2nd failure
        isolated.getPopularAnime(1)
        assertEquals(2, isolated.getStats().failedRequests)

        // 3rd failure -> trips circuit breaker to OPEN
        isolated.getPopularAnime(1)
        assertEquals(3, isolated.getStats().failedRequests)
        assertEquals(CircuitBreakerState.OPEN, isolated.getStats().circuitState)

        // 4th request -> Fast-failed by OPEN circuit breaker without even invoking delegate
        val fastFailResult = isolated.getPopularAnime(1)
        assertTrue(fastFailResult is AppResult.Error)
        val errorMsg = (fastFailResult as AppResult.Error).error.message
        assertTrue("Should mention temporarily paused/circuit breaker", errorMsg.contains("temporarily paused"))
    }

    @Test
    fun testExtensionRuntimeManager_fullLifecycle() = runTest(testDispatcher) {
        val testExt = TestAnimeExtension()
        builtInRuntime.register(testExt)

        // 1. Initial State
        assertFalse(runtimeManager.isLoaded(testExt.id))
        val initialStatus = runtimeManager.runtimeStatuses.value
        assertTrue("Built-in runtime should be ready", initialStatus.containsKey("builtin"))

        // 2. Load Extension
        val loadResult = runtimeManager.load(testExt.manifest)
        assertTrue("Load should succeed", loadResult is AppResult.Success)
        assertTrue(runtimeManager.isLoaded(testExt.id))

        val loadedInstance = runtimeManager.getExtension(testExt.id)
        assertNotNull(loadedInstance)

        // 3. Perform Anime Operations through Sandboxed Runtime
        val searchResult = loadedInstance!!.searchAnime("frieren")
        assertTrue("Search should succeed", searchResult is AppResult.Success)
        val animeList = (searchResult as AppResult.Success).data
        assertEquals(1, animeList.size)
        assertEquals("Frieren: Beyond Journey's End", animeList.first().title)

        // Verify Stats recorded
        val stats = runtimeManager.getStats(testExt.id)
        assertNotNull(stats)
        assertTrue("Should have recorded at least 1 successful request", stats!!.successfulRequests >= 1)
        assertEquals(CircuitBreakerState.CLOSED, stats.circuitState)

        // 4. Unload Extension
        val unloadResult = runtimeManager.unload(testExt.id)
        assertTrue("Unload should succeed", unloadResult is AppResult.Success)
        assertFalse(runtimeManager.isLoaded(testExt.id))
        assertNull(runtimeManager.getExtension(testExt.id))
    }
}
