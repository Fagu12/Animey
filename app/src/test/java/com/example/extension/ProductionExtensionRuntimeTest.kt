package com.example.extension

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.logging.CircuitBreakerState
import com.example.domain.extension.logging.DefaultExtensionLogger
import com.example.domain.extension.manager.DefaultExtensionRuntimeManager
import com.example.domain.extension.runtime.DefaultSandboxedAnimeExtension
import com.example.domain.extension.runtime.ExtensionLifecycleState
import com.example.domain.extension.runtime.ExtensionRuntime
import com.example.domain.extension.runtime.IsolatedAnimeExtension
import com.example.domain.extension.runtime.SandboxedExtensionRuntime
import com.example.domain.extension.security.ControlledExtensionHttpClient
import com.example.domain.extension.security.DefaultExtensionNetworkPolicyEnforcer
import com.example.domain.extension.security.DefaultExtensionPermissionManager
import com.example.domain.extension.security.NetworkPolicyDecision
import com.example.domain.extension.validator.DefaultExtensionValidator
import com.example.domain.extension.validator.ExtensionValidationResult
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionCapability
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.VideoSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProductionExtensionRuntimeTest {

    private lateinit var validator: DefaultExtensionValidator
    private lateinit var permissionManager: DefaultExtensionPermissionManager
    private lateinit var networkPolicyEnforcer: DefaultExtensionNetworkPolicyEnforcer
    private lateinit var sandboxedRuntime: SandboxedExtensionRuntime
    private lateinit var logger: DefaultExtensionLogger

    @Before
    fun setUp() {
        validator = DefaultExtensionValidator()
        permissionManager = DefaultExtensionPermissionManager()
        networkPolicyEnforcer = DefaultExtensionNetworkPolicyEnforcer(maxRequestsPerMinute = 5)
        logger = DefaultExtensionLogger(maxLogsPerExtension = 20)
        sandboxedRuntime = SandboxedExtensionRuntime(
            validator = validator,
            permissionManager = permissionManager,
            networkPolicyEnforcer = networkPolicyEnforcer,
            logger = logger,
            operationTimeoutMs = 1_000L
        )
    }

    // =========================================================================
    // 1. Version Compatibility & Extension API Version
    // =========================================================================

    @Test
    fun testVersionCompatibility_compatibleExtensionAccepted() {
        val manifest = ExtensionManifest(
            id = "moe.test.compatible",
            name = "Compatible Source",
            version = "1.0.0",
            language = "en",
            apiVersion = 1,
            minApiVersion = 1,
            minAppVersion = "1.0.0"
        )

        val result = validator.validate(manifest, currentAppVersion = "1.2.0", currentApiVersion = 1)
        assertTrue("Expected valid result for compatible extension", result.isValid)
    }

    @Test
    fun testVersionCompatibility_futureApiVersionRejected() {
        val manifest = ExtensionManifest(
            id = "moe.test.future",
            name = "Future API Source",
            version = "2.0.0",
            language = "en",
            apiVersion = 3,
            minApiVersion = 3, // Host is on API version 1
            minAppVersion = "1.0.0"
        )

        val result = validator.validate(manifest, currentAppVersion = "1.0.0", currentApiVersion = 1)
        assertTrue("Future API extension must be marked incompatible", result is ExtensionValidationResult.Incompatible)
        val reason = (result as ExtensionValidationResult.Incompatible).reason
        assertTrue(reason.contains("API version"))
    }

    @Test
    fun testVersionCompatibility_futureAppVersionRejected() {
        val manifest = ExtensionManifest(
            id = "moe.test.futureapp",
            name = "Future App Source",
            version = "1.0.0",
            language = "en",
            minAppVersion = "2.5.0" // Current app is 1.0.0
        )

        val result = validator.validate(manifest, currentAppVersion = "1.0.0", currentApiVersion = 1)
        assertTrue("Incompatible app version must be rejected", result is ExtensionValidationResult.Incompatible)
    }

    // =========================================================================
    // 2. Manifest Validation & Capabilities
    // =========================================================================

    @Test
    fun testManifestValidation_invalidIdAndNameRejected() {
        val invalidManifest = ExtensionManifest(
            id = "invalid id with spaces!",
            name = "",
            version = "1.0.0",
            language = "en"
        )

        val result = validator.validate(invalidManifest)
        assertTrue(result is ExtensionValidationResult.Invalid)
        val errors = (result as ExtensionValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("Extension ID") })
        assertTrue(errors.any { it.contains("Extension name") })
    }

    @Test
    fun testManifestValidation_maliciousUrlProtocolsRejected() {
        val maliciousFileManifest = ExtensionManifest(
            id = "moe.malicious.file",
            name = "Exploit Extension",
            version = "1.0.0",
            language = "en",
            downloadUrl = "file:///etc/passwd"
        )

        val result = validator.validate(maliciousFileManifest)
        assertTrue(result is ExtensionValidationResult.Invalid)
        val errors = (result as ExtensionValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("Forbidden protocol") || it.contains("must use http or https") })
    }

    @Test
    fun testManifestValidation_unrecognizedCapabilityProducesWarning() {
        val manifest = ExtensionManifest(
            id = "moe.test.caps",
            name = "Capabilities Source",
            version = "1.0.0",
            language = "en",
            capabilities = listOf("NETWORK", "UNKNOWN_FUTURE_CAPABILITY")
        )

        val result = validator.validate(manifest)
        assertTrue(result is ExtensionValidationResult.Warning)
        val warnings = (result as ExtensionValidationResult.Warning).warnings
        assertTrue(warnings.any { it.contains("Unrecognized capability") })
    }

    // =========================================================================
    // 3. Controlled Networking & SSRF Protection
    // =========================================================================

    @Test
    fun testControlledNetworking_requiresNetworkCapability() {
        val noNetManifest = ExtensionManifest(
            id = "moe.offline.only",
            name = "Offline Only",
            version = "1.0.0",
            language = "en",
            capabilities = listOf("PREFERENCES") // No NETWORK capability
        )

        val decision = networkPolicyEnforcer.evaluateRequest(noNetManifest, "https://api.example.com/anime")
        assertTrue(decision is NetworkPolicyDecision.Denied)
        val reason = (decision as NetworkPolicyDecision.Denied).reason
        assertTrue(reason.contains("NETWORK"))
    }

    @Test
    fun testControlledNetworking_blocksSsrfAndPrivateNetworks() {
        val netManifest = ExtensionManifest(
            id = "moe.test.ssrf",
            name = "Network Source",
            version = "1.0.0",
            language = "en",
            capabilities = listOf("NETWORK")
        )

        val targetsToBlock = listOf(
            "http://127.0.0.1:8080/secret",
            "http://localhost/admin",
            "http://10.0.0.1/internal-api",
            "http://192.168.1.1/router",
            "http://172.20.0.1/docker",
            "http://169.254.169.254/computeMetadata/v1",
            "file:///data/local/tmp"
        )

        for (target in targetsToBlock) {
            val decision = networkPolicyEnforcer.evaluateRequest(netManifest, target)
            assertTrue("Expected block for SSRF target: $target", decision is NetworkPolicyDecision.Denied)
        }
    }

    @Test
    fun testControlledNetworking_enforcesDomainWhitelisting() {
        val whitelistedManifest = ExtensionManifest(
            id = "moe.test.whitelisted",
            name = "Whitelisted Source",
            version = "1.0.0",
            language = "en",
            capabilities = listOf("NETWORK"),
            allowedDomains = listOf("api.animeprovider.com", "cdn.animeprovider.com")
        )

        val allowedDecision = networkPolicyEnforcer.evaluateRequest(
            whitelistedManifest,
            "https://api.animeprovider.com/v1/search"
        )
        assertEquals(NetworkPolicyDecision.Allowed, allowedDecision)

        val subdomainDecision = networkPolicyEnforcer.evaluateRequest(
            whitelistedManifest,
            "https://sub.api.animeprovider.com/v1/search"
        )
        assertEquals(NetworkPolicyDecision.Allowed, subdomainDecision)

        val blockedDecision = networkPolicyEnforcer.evaluateRequest(
            whitelistedManifest,
            "https://malicious-external-site.com/track"
        )
        assertTrue(blockedDecision is NetworkPolicyDecision.Denied)
    }

    @Test
    fun testControlledNetworking_rateLimitingEnforced() {
        val rateManifest = ExtensionManifest(
            id = "moe.test.ratelimit",
            name = "Rate Limit Source",
            version = "1.0.0",
            language = "en",
            capabilities = listOf("NETWORK")
        )

        // maxRequestsPerMinute was configured as 5 in setUp()
        for (i in 1..5) {
            val decision = networkPolicyEnforcer.evaluateRequest(rateManifest, "https://api.example.com/item$i")
            assertEquals("Request $i should be allowed", NetworkPolicyDecision.Allowed, decision)
        }

        // 6th request within the same minute should be rejected
        val overLimitDecision = networkPolicyEnforcer.evaluateRequest(rateManifest, "https://api.example.com/item6")
        assertTrue("6th request must exceed rate limit", overLimitDecision is NetworkPolicyDecision.Denied)
    }

    // =========================================================================
    // 4. Extension Lifecycle: Load, Unload, State Management
    // =========================================================================

    @Test
    fun testExtensionLifecycle_loadAndUnloadStages() = runTest {
        val manifest = ExtensionManifest(
            id = "moe.test.lifecycle",
            name = "Lifecycle Source",
            version = "1.0.0",
            language = "en",
            downloadUrl = "https://api.lifecyclesource.org"
        )

        assertEquals(ExtensionLifecycleState.UNLOADED, sandboxedRuntime.getLifecycleState(manifest.id))

        val loadResult = sandboxedRuntime.loadExtension(manifest)
        assertTrue(loadResult is AppResult.Success)
        assertEquals(ExtensionLifecycleState.ACTIVE, sandboxedRuntime.getLifecycleState(manifest.id))
        assertTrue(sandboxedRuntime.getLoadedExtensionIds().contains(manifest.id))

        val unloadResult = sandboxedRuntime.unloadExtension(manifest.id)
        assertTrue(unloadResult is AppResult.Success)
        assertEquals(ExtensionLifecycleState.TERMINATED, sandboxedRuntime.getLifecycleState(manifest.id))
        assertFalse(sandboxedRuntime.getLoadedExtensionIds().contains(manifest.id))
    }

    // =========================================================================
    // 5. Crash Isolation, Exception Containment & Circuit Breaker
    // =========================================================================

    @Test
    fun testCrashIsolation_exceptionsDoNotCrashHostProcess() = runTest {
        val crashingDelegate = object : AnimeExtension {
            override val id = "moe.test.crashing"
            override val name = "Crashing Source"
            override val lang = "en"
            override val iconUrl = ""
            override val baseUrl = "https://example.com"
            override val manifest = ExtensionManifest(id, name, "1.0.0", lang)

            override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
                throw IllegalStateException("Simulation of an untrusted crash inside extension logic!")
            }
            override suspend fun getLatestAnime(page: Int) = AppResult.Success(emptyList<Anime>())
            override suspend fun searchAnime(query: String, page: Int, filters: Map<String, Any>) = AppResult.Success(emptyList<Anime>())
            override suspend fun getAnimeDetails(sourceAnimeId: String) = AppResult.Error(AppError.NotFoundError())
            override suspend fun getEpisodes(sourceAnimeId: String) = AppResult.Success(emptyList<Episode>())
            override suspend fun getEpisodeStreams(sourceEpisodeId: String, extra: Map<String, String>) = AppResult.Success(emptyList<VideoSource>())
            override suspend fun getPreferences() = emptyList<ExtensionPreference>()
            override suspend fun setPreference(key: String, value: Any) {}
        }

        val isolated = IsolatedAnimeExtension(
            delegate = crashingDelegate,
            logger = logger,
            timeoutMs = 500L,
            maxConsecutiveFailures = 3,
            circuitCooldownMs = 10_000L
        )

        // Calling crashing method must return typed AppResult.Error and not throw
        val result = isolated.getPopularAnime(1)
        assertTrue(result is AppResult.Error)
        val err = (result as AppResult.Error).error
        assertTrue(err is AppError.ProviderError)
        assertEquals("Crashing Source", (err as AppError.ProviderError).provider)
        assertTrue(err.message.contains("Simulation of an untrusted crash"))
    }

    @Test
    fun testCircuitBreaker_tripsAfterRepeatedFailures() = runTest {
        val failingDelegate = object : AnimeExtension {
            override val id = "moe.test.failing"
            override val name = "Failing Source"
            override val lang = "en"
            override val iconUrl = ""
            override val baseUrl = "https://example.com"
            override val manifest = ExtensionManifest(id, name, "1.0.0", lang)

            override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
                return AppResult.Error(AppError.NetworkError("Remote server 503"))
            }
            override suspend fun getLatestAnime(page: Int) = AppResult.Success(emptyList<Anime>())
            override suspend fun searchAnime(query: String, page: Int, filters: Map<String, Any>) = AppResult.Success(emptyList<Anime>())
            override suspend fun getAnimeDetails(sourceAnimeId: String) = AppResult.Error(AppError.NotFoundError())
            override suspend fun getEpisodes(sourceAnimeId: String) = AppResult.Success(emptyList<Episode>())
            override suspend fun getEpisodeStreams(sourceEpisodeId: String, extra: Map<String, String>) = AppResult.Success(emptyList<VideoSource>())
            override suspend fun getPreferences() = emptyList<ExtensionPreference>()
            override suspend fun setPreference(key: String, value: Any) {}
        }

        val isolated = IsolatedAnimeExtension(
            delegate = failingDelegate,
            logger = logger,
            timeoutMs = 500L,
            maxConsecutiveFailures = 3,
            circuitCooldownMs = 10_000L
        )

        assertEquals(CircuitBreakerState.CLOSED, isolated.getStats().circuitState)

        // Execute 3 failures
        isolated.getPopularAnime(1)
        isolated.getPopularAnime(2)
        isolated.getPopularAnime(3)

        val stats = isolated.getStats()
        assertEquals(CircuitBreakerState.OPEN, stats.circuitState)
        assertEquals(3, stats.consecutiveFailures)

        // 4th call is fast-rejected by open circuit breaker
        val fastFailResult = isolated.getPopularAnime(4)
        assertTrue(fastFailResult is AppResult.Error)
        val err = (fastFailResult as AppResult.Error).error
        assertTrue(err.message.contains("temporarily paused"))
    }

    // =========================================================================
    // 6. Timeout Handling
    // =========================================================================

    @Test
    fun testTimeoutHandling_slowOperationsCancelGracefully() = runTest {
        val slowDelegate = object : AnimeExtension {
            override val id = "moe.test.slow"
            override val name = "Slow Source"
            override val lang = "en"
            override val iconUrl = ""
            override val baseUrl = "https://example.com"
            override val manifest = ExtensionManifest(id, name, "1.0.0", lang)

            override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
                delay(3_000L) // Takes 3 seconds, timeout is 200ms
                return AppResult.Success(emptyList())
            }
            override suspend fun getLatestAnime(page: Int) = AppResult.Success(emptyList<Anime>())
            override suspend fun searchAnime(query: String, page: Int, filters: Map<String, Any>) = AppResult.Success(emptyList<Anime>())
            override suspend fun getAnimeDetails(sourceAnimeId: String) = AppResult.Error(AppError.NotFoundError())
            override suspend fun getEpisodes(sourceAnimeId: String) = AppResult.Success(emptyList<Episode>())
            override suspend fun getEpisodeStreams(sourceEpisodeId: String, extra: Map<String, String>) = AppResult.Success(emptyList<VideoSource>())
            override suspend fun getPreferences() = emptyList<ExtensionPreference>()
            override suspend fun setPreference(key: String, value: Any) {}
        }

        val isolated = IsolatedAnimeExtension(
            delegate = slowDelegate,
            logger = logger,
            timeoutMs = 200L
        )

        val result = isolated.getPopularAnime(1)
        assertTrue(result is AppResult.Error)
        val err = (result as AppResult.Error).error
        assertTrue(err is AppError.TimeoutError)
        assertTrue(err.message.contains("timed out"))
    }

    // =========================================================================
    // 7. Atomic Extension Updates & Preference Migration
    // =========================================================================

    @Test
    fun testExtensionUpdate_preservesPreferencesAndMigratesAtomically() = runTest {
        val runtimeManager = DefaultExtensionRuntimeManager(
            sandboxedRuntime = sandboxedRuntime,
            validator = validator,
            logger = logger
        )

        val v1Manifest = ExtensionManifest(
            id = "moe.test.dynamicupdate",
            name = "Dynamic Source",
            version = "1.0.0",
            language = "en",
            downloadUrl = "https://api.dynamic.org"
        )

        // 1. Initial load v1
        val loadResult = runtimeManager.load(v1Manifest)
        assertTrue(loadResult is AppResult.Success)

        // 2. Set dynamic user preferences
        sandboxedRuntime.setPreferenceDescriptors(
            v1Manifest.id,
            listOf(
                ExtensionPreference.StringPreference(
                    key = "server_region",
                    title = "Server Region",
                    defaultValue = "us-east",
                    value = "us-east"
                ),
                ExtensionPreference.BooleanPreference(
                    key = "prefer_sub",
                    title = "Prefer Subtitles",
                    defaultValue = true,
                    value = true
                )
            )
        )
        runtimeManager.setExtensionPreference(v1Manifest.id, "server_region", "eu-central")
        runtimeManager.setExtensionPreference(v1Manifest.id, "prefer_sub", false)

        val prefsBefore = runtimeManager.getExtensionPreferences(v1Manifest.id)
        assertEquals("eu-central", prefsBefore.find { it.key == "server_region" }?.asString())
        assertEquals(false, prefsBefore.find { it.key == "prefer_sub" }?.asBoolean())

        // 3. Perform atomic update to v1.1.0
        val v2Manifest = v1Manifest.copy(version = "1.1.0")
        val updateResult = runtimeManager.update(v2Manifest)
        assertTrue(updateResult is AppResult.Success)

        val updatedExtension = runtimeManager.getExtension(v1Manifest.id)
        assertNotNull(updatedExtension)
        assertEquals("1.1.0", updatedExtension?.manifest?.version)

        // 4. Verify preferences migrated across update
        val prefsAfter = runtimeManager.getExtensionPreferences(v1Manifest.id)
        assertEquals("eu-central", prefsAfter.find { it.key == "server_region" }?.asString())
        assertEquals(false, prefsAfter.find { it.key == "prefer_sub" }?.asBoolean())
    }

    @Test
    fun testExtensionUpdate_rollbackOnFailurePreservesExistingInstance() = runTest {
        val runtimeManager = DefaultExtensionRuntimeManager(
            sandboxedRuntime = sandboxedRuntime,
            validator = validator,
            logger = logger
        )

        val v1Manifest = ExtensionManifest(
            id = "moe.test.rollback",
            name = "Rollback Source",
            version = "1.0.0",
            language = "en",
            downloadUrl = "https://api.rollback.org"
        )

        // Load working version 1.0.0
        val loadResult = runtimeManager.load(v1Manifest)
        assertTrue(loadResult is AppResult.Success)
        assertTrue(runtimeManager.isLoaded(v1Manifest.id))

        // Attempt update with invalid/incompatible manifest (e.g. requires impossible API version 999)
        val brokenV2Manifest = v1Manifest.copy(
            version = "2.0.0",
            minApiVersion = 999
        )

        val updateResult = runtimeManager.update(brokenV2Manifest)
        assertTrue("Update must fail for incompatible version", updateResult is AppResult.Error)

        // Rollback verification: Old instance must still be active and intact!
        assertTrue(runtimeManager.isLoaded(v1Manifest.id))
        val activeExt = runtimeManager.getExtension(v1Manifest.id)
        assertNotNull(activeExt)
        assertEquals("1.0.0", activeExt?.manifest?.version)
    }

    // =========================================================================
    // 8. Core Contract Preservation: Extension -> Normalized Models -> VideoSource -> Player
    // =========================================================================

    @Test
    fun testCoreContractPreservation_returnsNormalizedModels() = runTest {
        val contractDelegate = object : AnimeExtension {
            override val id = "moe.test.contract"
            override val name = "Contract Source"
            override val lang = "en"
            override val iconUrl = "https://example.com/icon.png"
            override val baseUrl = "https://contract.example.com"
            override val manifest = ExtensionManifest(id, name, "1.0.0", lang)

            override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
                return AppResult.Success(
                    listOf(
                        Anime(
                            localId = "ext-anime-101",
                            sourceId = id,
                            sourceAnimeId = "steins-gate",
                            title = "Steins;Gate",
                            poster = "https://example.com/steins.jpg",
                            banner = "",
                            rating = 9.1,
                            type = "TV",
                            status = "FINISHED",
                            genres = listOf("Sci-Fi", "Thriller")
                        )
                    )
                )
            }

            override suspend fun getLatestAnime(page: Int) = AppResult.Success(emptyList<Anime>())
            override suspend fun searchAnime(query: String, page: Int, filters: Map<String, Any>) = AppResult.Success(emptyList<Anime>())
            override suspend fun getAnimeDetails(sourceAnimeId: String) = AppResult.Error(AppError.NotFoundError())

            override suspend fun getEpisodes(sourceAnimeId: String): AppResult<List<Episode>> {
                return AppResult.Success(
                    listOf(
                        Episode(
                            id = "ep-1",
                            animeId = "ext-anime-101",
                            sourceId = id,
                            sourceEpisodeId = "source-ep-1",
                            number = 1.0f,
                            title = "Turning Point"
                        )
                    )
                )
            }

            override suspend fun getEpisodeStreams(
                sourceEpisodeId: String,
                extra: Map<String, String>
            ): AppResult<List<VideoSource>> {
                return AppResult.Success(
                    listOf(
                        VideoSource(
                            id = "source-1",
                            providerId = id,
                            serverName = "Default",
                            url = "https://stream.example.com/master.m3u8",
                            quality = "1080p",
                            type = com.example.domain.model.SourceType.HLS,
                            headers = mapOf("Referer" to "https://contract.example.com")
                        )
                    )
                )
            }

            override suspend fun getPreferences() = emptyList<ExtensionPreference>()
            override suspend fun setPreference(key: String, value: Any) {}
        }

        val isolated = IsolatedAnimeExtension(
            delegate = contractDelegate,
            logger = logger
        )

        // 1. Extension -> Normalized Anime Model
        val popularResult = isolated.getPopularAnime(1)
        assertTrue(popularResult is AppResult.Success)
        val animeList = (popularResult as AppResult.Success).data
        assertEquals(1, animeList.size)
        val anime = animeList[0]
        assertEquals("Steins;Gate", anime.title)

        // 2. Extension -> Normalized Episode Model
        val episodesResult = isolated.getEpisodes(anime.sourceAnimeId)
        assertTrue(episodesResult is AppResult.Success)
        val episodeList = (episodesResult as AppResult.Success).data
        assertEquals(1, episodeList.size)
        val episode = episodeList[0]
        assertEquals("Turning Point", episode.title)

        // 3. Extension -> Normalized VideoSource -> Ready for ExoPlayer
        val streamsResult = isolated.getEpisodeStreams(episode.sourceEpisodeId, emptyMap())
        assertTrue(streamsResult is AppResult.Success)
        val streams = (streamsResult as AppResult.Success).data
        assertEquals(1, streams.size)
        val videoSource = streams[0]
        assertEquals("https://stream.example.com/master.m3u8", videoSource.url)
        assertEquals("1080p", videoSource.quality)
        assertEquals(com.example.domain.model.SourceType.HLS, videoSource.type)
    }
}
