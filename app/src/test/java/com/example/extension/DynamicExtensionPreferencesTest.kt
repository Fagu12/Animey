package com.example.extension

import com.example.core.result.AppResult
import com.example.data.extension.builtin.TestAnimeExtension
import com.example.domain.extension.DefaultExtensionManager
import com.example.domain.extension.factory.DefaultExtensionRuntimeFactory
import com.example.domain.extension.loader.DefaultExtensionLoader
import com.example.domain.extension.logging.DefaultExtensionLogger
import com.example.domain.extension.manager.DefaultExtensionRuntimeManager
import com.example.domain.extension.parser.DefaultExtensionManifestParser
import com.example.domain.extension.runtime.BuiltInExtensionRuntime
import com.example.domain.extension.validator.DefaultExtensionValidator
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.ExtensionRepository
import com.example.domain.model.PreferenceType
import com.example.domain.model.RepositoryIndex
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DynamicExtensionPreferencesTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var logger: DefaultExtensionLogger
    private lateinit var builtInRuntime: BuiltInExtensionRuntime
    private lateinit var runtimeFactory: DefaultExtensionRuntimeFactory
    private lateinit var validator: DefaultExtensionValidator
    private lateinit var runtimeManager: DefaultExtensionRuntimeManager
    private lateinit var extensionManager: DefaultExtensionManager
    private lateinit var testExtension: TestAnimeExtension

    @Before
    fun setUp() {
        logger = DefaultExtensionLogger(maxLogsPerExtension = 20)
        builtInRuntime = BuiltInExtensionRuntime(logger)
        runtimeFactory = DefaultExtensionRuntimeFactory(builtInRuntime)
        validator = DefaultExtensionValidator()
        runtimeManager = DefaultExtensionRuntimeManager(
            builtInRuntime = builtInRuntime,
            runtimeFactory = runtimeFactory,
            validator = validator,
            logger = logger,
            currentAppVersion = "1.0.0"
        )
        testExtension = TestAnimeExtension()
        builtInRuntime.register(testExtension)

        val dummyStorageRepo = object : ExtensionStorageRepository {
            private val manifestsFlow = MutableStateFlow<List<ExtensionManifest>>(emptyList())
            private val reposFlow = MutableStateFlow<List<ExtensionRepository>>(emptyList())

            override fun getInstalledExtensions(): Flow<List<ExtensionManifest>> = manifestsFlow
            override fun getEnabledExtensions(): Flow<List<ExtensionManifest>> = manifestsFlow.map { it.filter { ext -> ext.isEnabled } }
            override fun getExtensionsWithUpdates(): Flow<List<ExtensionManifest>> = manifestsFlow.map { it.filter { ext -> ext.hasUpdate } }
            override fun getExtensionById(id: String): Flow<ExtensionManifest?> = manifestsFlow.map { it.firstOrNull { ext -> ext.id == id } }

            override suspend fun saveInstalledExtension(manifest: ExtensionManifest): AppResult<Unit> {
                val list = manifestsFlow.value.toMutableList()
                val index = list.indexOfFirst { it.id == manifest.id }
                if (index != -1) list[index] = manifest else list.add(manifest)
                manifestsFlow.value = list
                return AppResult.Success(Unit)
            }

            override suspend fun setExtensionEnabled(id: String, isEnabled: Boolean): AppResult<Unit> {
                val list = manifestsFlow.value.map { if (it.id == id) it.copy(isEnabled = isEnabled) else it }
                manifestsFlow.value = list
                return AppResult.Success(Unit)
            }

            override suspend fun setHasUpdate(id: String, hasUpdate: Boolean): AppResult<Unit> {
                val list = manifestsFlow.value.map { if (it.id == id) it.copy(hasUpdate = hasUpdate) else it }
                manifestsFlow.value = list
                return AppResult.Success(Unit)
            }

            override suspend fun setExtensionOrder(id: String, order: Int): AppResult<Unit> = AppResult.Success(Unit)
            override suspend fun reorderExtensions(orderedIds: List<String>): AppResult<Unit> = AppResult.Success(Unit)

            override suspend fun deleteExtension(id: String): AppResult<Unit> {
                manifestsFlow.value = manifestsFlow.value.filterNot { it.id == id }
                return AppResult.Success(Unit)
            }

            override fun getAllRepositories(): Flow<List<ExtensionRepository>> = reposFlow
            override fun getEnabledRepositories(): Flow<List<ExtensionRepository>> = reposFlow.map { it.filter { repo -> repo.isEnabled } }

            override suspend fun saveRepository(repo: ExtensionRepository): AppResult<Unit> {
                val list = reposFlow.value.toMutableList()
                val index = list.indexOfFirst { it.id == repo.id }
                if (index != -1) list[index] = repo else list.add(repo)
                reposFlow.value = list
                return AppResult.Success(Unit)
            }

            override suspend fun setRepositoryEnabled(id: String, isEnabled: Boolean): AppResult<Unit> {
                val list = reposFlow.value.map { if (it.id == id) it.copy(isEnabled = isEnabled) else it }
                reposFlow.value = list
                return AppResult.Success(Unit)
            }

            override suspend fun updateRepositorySyncInfo(id: String, count: Int): AppResult<Unit> {
                val list = reposFlow.value.map { if (it.id == id) it.copy(extensionCount = count) else it }
                reposFlow.value = list
                return AppResult.Success(Unit)
            }

            override suspend fun deleteRepository(id: String): AppResult<Unit> {
                reposFlow.value = reposFlow.value.filterNot { it.id == id }
                return AppResult.Success(Unit)
            }
        }

        val dummySettingsRepo = object : SettingsRepository {
            private val settingsFlow = MutableStateFlow(AppSettings())
            override val settings: Flow<AppSettings> = settingsFlow.asStateFlow()

            override suspend fun setThemeMode(mode: AppThemeMode) {}
            override suspend fun setAmoledMode(enabled: Boolean) {}
            override suspend fun setDynamicColor(enabled: Boolean) {}
            override suspend fun setAutoPlayNext(enabled: Boolean) {}
            override suspend fun setAutoSkipIntro(enabled: Boolean) {}
            override suspend fun setAutoSkipOutro(enabled: Boolean) {}
            override suspend fun setAutoSkipRecap(enabled: Boolean) {}
            override suspend fun setSkipOnce(enabled: Boolean) {}
            override suspend fun setManualSkip(enabled: Boolean) {}
            override suspend fun setDefaultQuality(quality: String) {}
            override suspend fun setDoubleTapSeekSeconds(seconds: Int) {}
            override suspend fun setDoubleTapSeek(enabled: Boolean) {}
            override suspend fun setSwipeSeek(enabled: Boolean) {}
            override suspend fun setBrightnessGesture(enabled: Boolean) {}
            override suspend fun setVolumeGesture(enabled: Boolean) {}
            override suspend fun setLongPressSpeed(enabled: Boolean) {}
            override suspend fun setLongPressSpeedMultiplier(multiplier: Float) {}
            override suspend fun setKeepScreenAwake(enabled: Boolean) {}
            override suspend fun setVideoFitMode(fitMode: String) {}
            override suspend fun setPreferredAudioLanguage(language: String) {}
            override suspend fun setSubtitleLanguage(language: String) {}
            override suspend fun setSubtitleDelayMs(delayMs: Long) {}
            override suspend fun setSubtitleFontSize(sizeSp: Int) {}
            override suspend fun setSubtitleFontStyle(fontStyle: String) {}
            override suspend fun setSubtitleTextColor(colorHex: String) {}
            override suspend fun setSubtitleTextOpacity(opacity: Float) {}
            override suspend fun setSubtitleBackgroundColor(colorHex: String) {}
            override suspend fun setSubtitleBackgroundOpacity(opacity: Float) {}
            override suspend fun setSubtitleOutlineEnabled(enabled: Boolean) {}
            override suspend fun setSubtitleOutlineColor(colorHex: String) {}
            override suspend fun setSubtitleBottomMarginDp(marginDp: Int) {}
            override suspend fun setDefaultExtensionId(extensionId: String) {}
            override suspend fun setAutoUpdateAniList(enabled: Boolean) {}
            override suspend fun setCompletionPercentage(percentage: Int) {}
            override suspend fun setSyncProgress(enabled: Boolean) {}
            override suspend fun setAutoMarkCompleted(enabled: Boolean) {}
        }

        extensionManager = DefaultExtensionManager(
            storageRepository = dummyStorageRepo,
            settingsRepository = dummySettingsRepo,
            runtimeManager = runtimeManager,
            validator = validator,
            manifestParser = DefaultExtensionManifestParser(),
            scope = testScope
        )
    }

    @Test
    fun testTestAnimeExtension_exposesAllSupportedPreferenceTypes() = runTest(testDispatcher) {
        val prefs = testExtension.getPreferences()

        val types = prefs.map { it.preferenceType }.toSet()

        assertTrue("Must support BOOLEAN", types.contains(PreferenceType.BOOLEAN))
        assertTrue("Must support STRING", types.contains(PreferenceType.STRING))
        assertTrue("Must support INTEGER", types.contains(PreferenceType.INTEGER))
        assertTrue("Must support NUMBER", types.contains(PreferenceType.NUMBER))
        assertTrue("Must support SELECT", types.contains(PreferenceType.SELECT))
        assertTrue("Must support MULTI_SELECT", types.contains(PreferenceType.MULTI_SELECT))
    }

    @Test
    fun testPromptExampleSelectPreference_matchesSpecification() = runTest(testDispatcher) {
        val prefs = testExtension.getPreferences()
        val languagePref = prefs.find { it.key == "language" }

        assertNotNull("Should contain 'language' preference", languagePref)
        assertEquals(PreferenceType.SELECT, languagePref!!.preferenceType)
        assertEquals("Language", languagePref.title)
        assertTrue(languagePref.options.contains("English"))
        assertTrue(languagePref.options.contains("Japanese"))
        assertEquals("English", languagePref.asString())
    }

    @Test
    fun testChangesSentBackThroughExtensionRuntime_mutateExtensionState() = runTest(testDispatcher) {
        // Load the extension into the runtime manager
        runtimeManager.load(testExtension.manifest)

        // 1. Mutate SELECT preference via ExtensionRuntime
        val selectRes = runtimeManager.setExtensionPreference(testExtension.id, "language", "Japanese")
        assertTrue(selectRes is AppResult.Success)

        // Verify updated via runtime getExtensionPreferences
        val updatedPrefs = runtimeManager.getExtensionPreferences(testExtension.id)
        val languagePref = updatedPrefs.find { it.key == "language" }
        assertEquals("Japanese", languagePref?.asString())

        // 2. Mutate BOOLEAN preference via ExtensionRuntime
        val boolRes = runtimeManager.setExtensionPreference(testExtension.id, "enable_4k", true)
        assertTrue(boolRes is AppResult.Success)
        val updatedBoolPref = runtimeManager.getExtensionPreferences(testExtension.id).find { it.key == "enable_4k" }
        assertEquals(true, updatedBoolPref?.asBoolean())

        // 3. Mutate STRING preference via ExtensionRuntime
        val strRes = runtimeManager.setExtensionPreference(testExtension.id, "custom_user_agent", "CustomUA/2.0")
        assertTrue(strRes is AppResult.Success)
        val updatedStrPref = runtimeManager.getExtensionPreferences(testExtension.id).find { it.key == "custom_user_agent" }
        assertEquals("CustomUA/2.0", updatedStrPref?.asString())

        // 4. Mutate INTEGER preference via ExtensionRuntime
        val intRes = runtimeManager.setExtensionPreference(testExtension.id, "max_buffer_seconds", 120)
        assertTrue(intRes is AppResult.Success)
        val updatedIntPref = runtimeManager.getExtensionPreferences(testExtension.id).find { it.key == "max_buffer_seconds" }
        assertEquals(120, updatedIntPref?.asInt())

        // 5. Mutate NUMBER preference via ExtensionRuntime
        val numRes = runtimeManager.setExtensionPreference(testExtension.id, "playback_speed", 1.5)
        assertTrue(numRes is AppResult.Success)
        val updatedNumPref = runtimeManager.getExtensionPreferences(testExtension.id).find { it.key == "playback_speed" }
        assertEquals(1.5, updatedNumPref?.asDouble() ?: 0.0, 0.001)

        // 6. Mutate MULTI_SELECT preference via ExtensionRuntime
        val multiRes = runtimeManager.setExtensionPreference(
            testExtension.id,
            "preferred_qualities",
            listOf("1080p", "480p")
        )
        assertTrue(multiRes is AppResult.Success)
        val updatedMultiPref = runtimeManager.getExtensionPreferences(testExtension.id).find { it.key == "preferred_qualities" }
        assertEquals(listOf("1080p", "480p"), updatedMultiPref?.asMultiSelect())
    }

    @Test
    fun testExtensionManager_routesPreferencesThroughExtensionRuntime() = runTest(testDispatcher) {
        runtimeManager.load(testExtension.manifest)

        // Mutate through ExtensionManager
        val result = extensionManager.setExtensionPreference(testExtension.id, "language", "Spanish")
        assertTrue("ExtensionManager should forward to runtime successfully", result is AppResult.Success)

        val prefs = extensionManager.getExtensionPreferences(testExtension.id)
        val langPref = prefs.find { it.key == "language" }
        assertEquals("Spanish", langPref?.asString())
    }

    @Test
    fun testJsonParsing_parsesAllSixPreferenceTypes() {
        val sampleJson = """
            [
              {
                "key": "language",
                "type": "SELECT",
                "title": "Language",
                "options": ["English", "Japanese"],
                "default": "English"
              },
              {
                "key": "enable_subtitles",
                "type": "BOOLEAN",
                "title": "Subtitles",
                "default": true
              },
              {
                "key": "custom_header",
                "type": "STRING",
                "title": "Header",
                "default": "Bearer token"
              },
              {
                "key": "buffer_size",
                "type": "INTEGER",
                "title": "Buffer",
                "default": 30
              },
              {
                "key": "rate",
                "type": "NUMBER",
                "title": "Speed",
                "default": 1.25
              },
              {
                "key": "servers",
                "type": "MULTI_SELECT",
                "title": "Allowed Servers",
                "options": ["US", "EU", "ASIA"],
                "default": ["US", "ASIA"]
              }
            ]
        """.trimIndent()

        val parsed = ExtensionPreference.fromJsonArray(sampleJson)
        assertEquals(6, parsed.size)

        assertEquals(PreferenceType.SELECT, parsed[0].preferenceType)
        assertEquals("Language", parsed[0].title)
        assertEquals("English", parsed[0].asString())
        assertEquals(listOf("English", "Japanese"), parsed[0].options)

        assertEquals(PreferenceType.BOOLEAN, parsed[1].preferenceType)
        assertTrue(parsed[1].asBoolean())

        assertEquals(PreferenceType.STRING, parsed[2].preferenceType)
        assertEquals("Bearer token", parsed[2].asString())

        assertEquals(PreferenceType.INTEGER, parsed[3].preferenceType)
        assertEquals(30, parsed[3].asInt())

        assertEquals(PreferenceType.NUMBER, parsed[4].preferenceType)
        assertEquals(1.25, parsed[4].asDouble(), 0.001)

        assertEquals(PreferenceType.MULTI_SELECT, parsed[5].preferenceType)
        assertEquals(listOf("US", "ASIA"), parsed[5].asMultiSelect())
    }
}
