package com.example.player

import com.example.core.result.AppResult
import com.example.data.extension.builtin.MirrorTestAnimeExtension
import com.example.data.extension.builtin.TestAnimeExtension
import com.example.domain.extension.DefaultExtensionManager
import com.example.domain.extension.ExtensionManager
import com.example.domain.extension.factory.DefaultExtensionRuntimeFactory
import com.example.domain.extension.logging.DefaultExtensionLogger
import com.example.domain.extension.manager.DefaultExtensionRuntimeManager
import com.example.domain.extension.parser.DefaultExtensionManifestParser
import com.example.domain.extension.runtime.BuiltInExtensionRuntime
import com.example.domain.extension.validator.DefaultExtensionValidator
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionRepository
import com.example.domain.player.selector.ProviderSelector
import com.example.domain.player.selector.ServerSelector
import com.example.domain.player.selector.SourceSelector
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SourcePreferenceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourceSelectionSystemTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakePrefRepo: FakeSourcePreferenceRepository
    private lateinit var extensionManager: ExtensionManager
    private lateinit var providerSelector: ProviderSelector
    private lateinit var serverSelector: ServerSelector
    private lateinit var sourceSelector: SourceSelector

    private val testAnimeId = "anime-demon-slayer-001"
    private val testEpisode = Episode(
        id = "ep-test-01",
        animeId = testAnimeId,
        sourceId = "builtin.test.anime",
        sourceEpisodeId = "test-ep-1",
        number = 1.0f,
        title = "Cruelty"
    )

    @Before
    fun setup() {
        fakePrefRepo = FakeSourcePreferenceRepository()

        val logger = DefaultExtensionLogger()
        val validator = DefaultExtensionValidator()
        val manifestParser = DefaultExtensionManifestParser()
        val runtime = BuiltInExtensionRuntime(logger)
        val runtimeFactory = DefaultExtensionRuntimeFactory(runtime)
        val runtimeManager = DefaultExtensionRuntimeManager(
            builtInRuntime = runtime,
            runtimeFactory = runtimeFactory,
            validator = validator,
            logger = logger
        )
        val fakeStorage = FakeExtensionStorageRepository()
        val fakeSettings = FakeSettingsRepo()

        extensionManager = DefaultExtensionManager(
            storageRepository = fakeStorage,
            settingsRepository = fakeSettings,
            runtimeManager = runtimeManager,
            validator = validator,
            manifestParser = manifestParser
        ).apply {
            registerExtension(TestAnimeExtension())
            registerExtension(MirrorTestAnimeExtension())
        }

        providerSelector = ProviderSelector(extensionManager, fakePrefRepo)
        serverSelector = ServerSelector(fakePrefRepo)
        sourceSelector = SourceSelector(
            extensionManager = extensionManager,
            providerSelector = providerSelector,
            serverSelector = serverSelector,
            sourcePreferenceRepository = fakePrefRepo
        )
    }

    @Test
    fun `test source preferences global and per-anime storage`() = testScope.runTest {
        fakePrefRepo.setGlobalPreferredProvider("builtin.test.anime")
        fakePrefRepo.setPreferredServer("Tokyo Mirror")
        fakePrefRepo.setPreferredQuality("720p")

        assertEquals("builtin.test.anime", fakePrefRepo.globalPreferredProvider.first())
        assertEquals("Tokyo Mirror", fakePrefRepo.preferredServer.first())
        assertEquals("720p", fakePrefRepo.preferredQuality.first())

        // Per-anime override
        fakePrefRepo.setPreferredProviderForAnime(testAnimeId, "builtin.mirror.anime")
        fakePrefRepo.setPreferredServerForAnime(testAnimeId, "Akamai Mirror")
        fakePrefRepo.setPreferredQualityForAnime(testAnimeId, "1080p")

        assertEquals("builtin.mirror.anime", fakePrefRepo.getPreferredProviderForAnime(testAnimeId).first())
        assertEquals("Akamai Mirror", fakePrefRepo.getPreferredServerForAnime(testAnimeId).first())
        assertEquals("1080p", fakePrefRepo.getPreferredQualityForAnime(testAnimeId).first())

        // Another anime falls back to null/global
        assertEquals(null, fakePrefRepo.getPreferredProviderForAnime("other-anime").first())
    }

    @Test
    fun `test provider selector discovers and evaluates multiple providers`() = testScope.runTest {
        val providers = providerSelector.getAvailableProviders(testAnimeId)
        assertTrue(providers.size >= 2)

        val providerIds = providers.map { it.id }
        assertTrue(providerIds.contains("builtin.test.anime"))
        assertTrue(providerIds.contains("builtin.mirror.anime"))

        // Default resolution
        val resolved = providerSelector.resolveBestProvider(testAnimeId)
        assertNotNull(resolved)

        // Set per-anime preference to mirror provider
        providerSelector.setPreferredProvider(testAnimeId, "builtin.mirror.anime", isGlobal = false)
        val resolvedAfterPref = providerSelector.resolveBestProvider(testAnimeId)
        assertEquals("builtin.mirror.anime", resolvedAfterPref?.id)
    }

    @Test
    fun `test server selector groups distinct servers and respects preferences`() = testScope.runTest {
        val testExt = TestAnimeExtension()
        val streams = (testExt.getEpisodeStreams("test-ep-1") as AppResult.Success).data
        assertTrue(streams.isNotEmpty())

        val servers = serverSelector.extractServers(streams, testAnimeId)
        assertTrue(servers.size >= 2)

        val serverNames = servers.map { it.name }
        assertTrue(serverNames.contains("FastCDN (Global)"))
        assertTrue(serverNames.contains("Mirror 1 (Tokyo)"))
        assertTrue(serverNames.contains("Backup Cloud"))

        // Best server default
        val bestServer = serverSelector.resolveBestServer(streams, testAnimeId)
        assertEquals("FastCDN (Global)", bestServer)

        // Filter sources for specific server
        val tokyoSources = serverSelector.filterSourcesForServer(streams, "Mirror 1 (Tokyo)")
        assertTrue(tokyoSources.all { it.serverName == "Mirror 1 (Tokyo)" })
        assertEquals(2, tokyoSources.size)
    }

    @Test
    fun `test source selector end-to-end flow with provider, server, and quality separation`() = testScope.runTest {
        // 1. Load sources for episode
        val state = sourceSelector.loadSourcesForEpisode(
            animeId = testAnimeId,
            episode = testEpisode
        )

        assertFalse(state.isLoading)
        assertTrue(state.hasSources)
        assertEquals("builtin.test.anime", state.selectedProviderId)
        assertNotNull(state.selectedServerName)
        assertNotNull(state.selectedQuality)
        assertNotNull(state.activeSource)

        // Verify qualities are extracted
        val availableQualityStrings = state.availableQualities.map { it.quality }
        assertTrue(availableQualityStrings.contains("1080p"))
        assertTrue(availableQualityStrings.contains("720p"))
        assertTrue(availableQualityStrings.contains("480p"))
        assertTrue(availableQualityStrings.contains("360p"))

        // 2. Select Quality 720p
        sourceSelector.selectQuality("720p")
        val stateAfterQuality = sourceSelector.state.value
        assertEquals("720p", stateAfterQuality.selectedQuality)
        assertEquals("720p", stateAfterQuality.activeSource?.quality)

        // 3. Select Server "Tokyo Mirror"
        sourceSelector.selectServer("Tokyo Mirror")
        val stateAfterServer = sourceSelector.state.value
        assertEquals("Tokyo Mirror", stateAfterServer.selectedServerName)
        assertEquals("Tokyo Mirror", stateAfterServer.activeSource?.serverName)

        // 4. Switch Provider to "builtin.mirror.anime"
        sourceSelector.selectProvider("builtin.mirror.anime")
        val stateAfterProvider = sourceSelector.state.value
        assertEquals("builtin.mirror.anime", stateAfterProvider.selectedProviderId)
        assertTrue(stateAfterProvider.activeSource?.providerId == "builtin.mirror.anime")
    }

    @Test
    fun `test source failure triggers automatic fallback to alternative server and provider`() = testScope.runTest {
        sourceSelector.loadSourcesForEpisode(
            animeId = testAnimeId,
            episode = testEpisode
        )

        val initialSource = sourceSelector.state.value.activeSource
        assertNotNull(initialSource)

        // Mark initial source as failed
        val fallbackSource1 = sourceSelector.markSourceFailed(initialSource!!.id)
        assertNotNull(fallbackSource1)
        assertNotEquals(initialSource.id, fallbackSource1?.id)
        assertTrue(sourceSelector.state.value.failedSourceIds.contains(initialSource.id))

        // Fail another source to force server fallback
        val currentActive = sourceSelector.state.value.activeSource!!
        val fallbackSource2 = sourceSelector.markSourceFailed(currentActive.id)
        assertNotNull(fallbackSource2)
        assertNotEquals(currentActive.id, fallbackSource2?.id)
    }

    // ==================== Test Fakes ====================

    class FakeSourcePreferenceRepository : SourcePreferenceRepository {
        private val _globalProvider = MutableStateFlow<String?>(null)
        override val globalPreferredProvider: Flow<String?> = _globalProvider.asStateFlow()

        private val _globalServer = MutableStateFlow<String?>(null)
        override val preferredServer: Flow<String?> = _globalServer.asStateFlow()

        private val _globalQuality = MutableStateFlow("1080p")
        override val preferredQuality: Flow<String> = _globalQuality.asStateFlow()

        private val animeProviders = MutableStateFlow<Map<String, String>>(emptyMap())
        private val animeServers = MutableStateFlow<Map<String, String>>(emptyMap())
        private val animeQualities = MutableStateFlow<Map<String, String>>(emptyMap())

        override suspend fun setGlobalPreferredProvider(providerId: String?) {
            _globalProvider.value = providerId
        }

        override suspend fun setPreferredServer(serverName: String?) {
            _globalServer.value = serverName
        }

        override suspend fun setPreferredQuality(quality: String) {
            _globalQuality.value = quality
        }

        override fun getPreferredProviderForAnime(animeId: String): Flow<String?> =
            animeProviders.map { it[animeId] }

        override suspend fun setPreferredProviderForAnime(animeId: String, providerId: String?) {
            animeProviders.update { current ->
                if (providerId == null) current - animeId else current + (animeId to providerId)
            }
        }

        override fun getPreferredServerForAnime(animeId: String): Flow<String?> =
            animeServers.map { it[animeId] }

        override suspend fun setPreferredServerForAnime(animeId: String, serverName: String?) {
            animeServers.update { current ->
                if (serverName == null) current - animeId else current + (animeId to serverName)
            }
        }

        override fun getPreferredQualityForAnime(animeId: String): Flow<String?> =
            animeQualities.map { it[animeId] }

        override suspend fun setPreferredQualityForAnime(animeId: String, quality: String?) {
            animeQualities.update { current ->
                if (quality == null) current - animeId else current + (animeId to quality)
            }
        }

        override suspend fun clearAnimePreferences(animeId: String) {
            animeProviders.update { it - animeId }
            animeServers.update { it - animeId }
            animeQualities.update { it - animeId }
        }
    }

    class FakeExtensionStorageRepository : ExtensionStorageRepository {
        override fun getInstalledExtensions(): Flow<List<ExtensionManifest>> = flowOf(emptyList())
        override fun getEnabledExtensions(): Flow<List<ExtensionManifest>> = flowOf(emptyList())
        override fun getExtensionsWithUpdates(): Flow<List<ExtensionManifest>> = flowOf(emptyList())
        override fun getExtensionById(id: String): Flow<ExtensionManifest?> = flowOf(null)
        override suspend fun saveInstalledExtension(manifest: ExtensionManifest): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setExtensionEnabled(id: String, isEnabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setHasUpdate(id: String, hasUpdate: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setExtensionOrder(id: String, order: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun reorderExtensions(orderedIds: List<String>): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteExtension(id: String): AppResult<Unit> = AppResult.Success(Unit)

        override fun getAllRepositories(): Flow<List<ExtensionRepository>> = flowOf(emptyList())
        override fun getEnabledRepositories(): Flow<List<ExtensionRepository>> = flowOf(emptyList())
        override suspend fun saveRepository(repo: ExtensionRepository): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setRepositoryEnabled(id: String, isEnabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateRepositorySyncInfo(id: String, count: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteRepository(id: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    class FakeSettingsRepo : SettingsRepository {
        private val _settings = MutableStateFlow(com.example.domain.model.AppSettings())
        override val settings = _settings.asStateFlow()

        override suspend fun setThemeMode(mode: com.example.domain.model.AppThemeMode) {}
        override suspend fun setAmoledMode(enabled: Boolean) {}
        override suspend fun setDynamicColor(enabled: Boolean) {}
        override suspend fun setAutoPlayNext(enabled: Boolean) {}
        override suspend fun setAutoSkipIntro(enabled: Boolean) {}
        override suspend fun setAutoSkipOutro(enabled: Boolean) {}
        override suspend fun setAutoSkipRecap(enabled: Boolean) {}
        override suspend fun setSkipOnce(enabled: Boolean) {}
        override suspend fun setManualSkip(enabled: Boolean) {}
        override suspend fun setDefaultQuality(quality: String) { _settings.update { it.copy(defaultQuality = quality) } }
        override suspend fun setDoubleTapSeekSeconds(seconds: Int) {}
        override suspend fun setDoubleTapSeek(enabled: Boolean) {}
        override suspend fun setPreferredAudioLanguage(language: String) {}
        override suspend fun setSubtitleLanguage(language: String) {}
        override suspend fun setSubtitleFontSize(sizeSp: Int) {}
        override suspend fun setDefaultExtensionId(extensionId: String) { _settings.update { it.copy(defaultExtensionId = extensionId) } }
        override suspend fun setSwipeSeek(enabled: Boolean) {}
        override suspend fun setBrightnessGesture(enabled: Boolean) {}
        override suspend fun setVolumeGesture(enabled: Boolean) {}
        override suspend fun setLongPressSpeed(enabled: Boolean) {}
        override suspend fun setLongPressSpeedMultiplier(multiplier: Float) {}
        override suspend fun setKeepScreenAwake(enabled: Boolean) {}
        override suspend fun setVideoFitMode(fitMode: String) {}
        override suspend fun setSubtitleDelayMs(delayMs: Long) {}
        override suspend fun setSubtitleFontStyle(fontStyle: String) {}
        override suspend fun setSubtitleTextColor(colorHex: String) {}
        override suspend fun setSubtitleTextOpacity(opacity: Float) {}
        override suspend fun setSubtitleBackgroundColor(colorHex: String) {}
        override suspend fun setSubtitleBackgroundOpacity(opacity: Float) {}
        override suspend fun setSubtitleOutlineEnabled(enabled: Boolean) {}
        override suspend fun setSubtitleOutlineColor(colorHex: String) {}
        override suspend fun setSubtitleBottomMarginDp(marginDp: Int) {}
        override suspend fun setAutoUpdateAniList(enabled: Boolean) {}
        override suspend fun setCompletionPercentage(percentage: Int) {}
        override suspend fun setSyncProgress(enabled: Boolean) {}
        override suspend fun setAutoMarkCompleted(enabled: Boolean) {}
    }
}
