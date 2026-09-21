package com.example.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
import com.example.domain.model.Anime
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionRepository
import com.example.domain.model.HistoryEntry
import com.example.domain.model.SourceType
import com.example.domain.model.VideoSource
import com.example.domain.player.EngineType
import com.example.domain.player.PlaybackState
import com.example.domain.player.selector.ProviderSelector
import com.example.domain.player.selector.ServerSelector
import com.example.domain.player.selector.SourceSelector
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SourcePreferenceRepository
import com.example.features.player.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlaybackPipelineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var extensionManager: ExtensionManager
    private lateinit var preferenceRepository: SourcePreferenceRepository
    private lateinit var providerSelector: ProviderSelector
    private lateinit var serverSelector: ServerSelector
    private lateinit var sourceSelector: SourceSelector
    private lateinit var animeRepo: AnimeRepository
    private lateinit var episodeRepo: EpisodeRepository
    private lateinit var historyRepo: HistoryRepository
    private lateinit var settingsRepo: SettingsRepository

    private val testAnime = Anime(
        localId = "test-anime-1",
        title = "Demon Slayer",
        description = "Tanjirou's journey.",
        sourceId = "builtin.test.anime",
        sourceAnimeId = "test-anime-1"
    )

    private val testEpisode = Episode(
        id = "ep-test-01",
        animeId = "test-anime-1",
        sourceId = "builtin.test.anime",
        sourceEpisodeId = "test-ep-1",
        number = 1.0f,
        title = "Cruelty",
        lastPositionMs = 45000L,
        durationMs = 1420000L
    )

    private val testEpisode2 = Episode(
        id = "ep-test-02",
        animeId = "test-anime-1",
        sourceId = "builtin.test.anime",
        sourceEpisodeId = "test-ep-2",
        number = 2.0f,
        title = "Trainer Sakonji",
        lastPositionMs = 0L,
        durationMs = 1420000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

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
        val fakeStorage = TestExtensionStorageRepo()
        val fakeSettings = TestSettingsRepo()

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

        preferenceRepository = TestSourcePrefRepo()
        providerSelector = ProviderSelector(extensionManager, preferenceRepository)
        serverSelector = ServerSelector(preferenceRepository)
        sourceSelector = SourceSelector(
            extensionManager = extensionManager,
            providerSelector = providerSelector,
            serverSelector = serverSelector,
            sourcePreferenceRepository = preferenceRepository
        )

        animeRepo = TestAnimeRepo(listOf(testAnime))
        episodeRepo = TestEpisodeRepo(listOf(testEpisode, testEpisode2))
        historyRepo = TestHistoryRepo()
        settingsRepo = fakeSettings
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `video source types are strictly routed to appropriate engines`() = runTest {
        val unifiedEngine = UnifiedPlayerEngine(context)

        // 1. HLS source -> routed to Media3
        val hlsSource = VideoSource(
            id = "src-hls",
            providerId = "builtin.test.anime",
            serverName = "FastCDN",
            url = "https://test.mux.dev/stream.m3u8",
            type = SourceType.HLS,
            quality = "1080p"
        )
        unifiedEngine.prepare(hlsSource, 0L, true)
        assertEquals(EngineType.MEDIA3, unifiedEngine.getActiveEngineType())
        assertEquals(EngineType.MEDIA3, unifiedEngine.state.value.engineType)

        // 2. MP4 source -> routed to Media3
        val mp4Source = VideoSource(
            id = "src-mp4",
            providerId = "builtin.test.anime",
            serverName = "Tokyo Mirror",
            url = "https://test.com/video.mp4",
            type = SourceType.MP4,
            quality = "720p"
        )
        unifiedEngine.prepare(mp4Source, 5000L, true)
        assertEquals(EngineType.MEDIA3, unifiedEngine.getActiveEngineType())
        assertEquals(EngineType.MEDIA3, unifiedEngine.state.value.engineType)

        // 3. FILE source -> routed to Media3
        val fileSource = VideoSource(
            id = "src-file",
            providerId = "builtin.test.anime",
            serverName = "Backup Cloud",
            url = "https://test.com/file.mp4",
            type = SourceType.FILE,
            quality = "1080p",
            headers = mapOf("X-Custom" to "Header"),
            referer = "https://backup.cloud"
        )
        unifiedEngine.prepare(fileSource, 0L, true)
        assertEquals(EngineType.MEDIA3, unifiedEngine.getActiveEngineType())

        // 4. EMBED source -> strictly routed to EmbedPlayerEngine (Never sent to ExoPlayer)
        val embedSource = VideoSource(
            id = "src-embed",
            providerId = "builtin.test.anime",
            serverName = "StreamEmbed",
            url = "https://embed.domain.com/video/123",
            type = SourceType.EMBED,
            quality = "1080p",
            headers = mapOf("User-Agent" to "CustomUA"),
            referer = "https://embed.domain.com"
        )
        unifiedEngine.prepare(embedSource, 12000L, true)
        assertEquals(EngineType.EMBED, unifiedEngine.getActiveEngineType())
        assertEquals(EngineType.EMBED, unifiedEngine.state.value.engineType)

        unifiedEngine.release()
    }

    @Test
    fun `media3 player engine rejects EMBED sources with descriptive error`() = runTest {
        val media3Engine = Media3PlayerEngine(context)
        val embedSource = VideoSource(
            id = "embed-src",
            providerId = "builtin.test.anime",
            serverName = "Embed Server",
            url = "https://embed.web/player",
            type = SourceType.EMBED,
            quality = "1080p"
        )

        media3Engine.prepare(embedSource, 0L, true)

        assertEquals(PlaybackState.ERROR, media3Engine.state.value.playbackState)
        assertTrue(media3Engine.state.value.error?.message?.contains("EMBED") == true)
        media3Engine.release()
    }

    @Test
    fun `full playback flow connects Episode to Provider to VideoSource to ViewModel`() = runTest {
        val unifiedEngine = UnifiedPlayerEngine(context)
        val viewModel = PlayerViewModel(
            animeId = testAnime.localId,
            initialEpisodeId = testEpisode.id,
            animeRepository = animeRepo,
            episodeRepository = episodeRepo,
            historyRepository = historyRepo,
            settingsRepository = settingsRepo,
            sourceSelector = sourceSelector,
            playerEngine = unifiedEngine
        )

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        val sourceState = viewModel.sourceSelectionState.value

        assertEquals(testAnime.localId, uiState.anime?.localId)
        assertEquals(testEpisode.id, uiState.currentEpisode?.id)
        assertTrue(sourceState.hasSources)
        assertNotNull(sourceState.activeSource)

        // Verify quality and server switching
        viewModel.selectQuality("720p")
        advanceUntilIdle()
        assertEquals("720p", viewModel.sourceSelectionState.value.selectedQuality)

        // Verify playback speed changes
        viewModel.setPlaybackSpeed(1.5f)
        advanceUntilIdle()
        assertEquals(1.5f, viewModel.playerState.value.playbackSpeed, 0.01f)

        // Verify seek relative
        viewModel.seekRelative(10)
        advanceUntilIdle()

        // Verify next episode switching
        viewModel.playNextEpisode()
        advanceUntilIdle()
        assertEquals(testEpisode2.id, viewModel.uiState.value.currentEpisode?.id)

        unifiedEngine.release()
    }

    @Test
    fun `player resume position starts from stored episode progress`() = runTest {
        val unifiedEngine = UnifiedPlayerEngine(context)
        val viewModel = PlayerViewModel(
            animeId = testAnime.localId,
            initialEpisodeId = testEpisode.id,
            animeRepository = animeRepo,
            episodeRepository = episodeRepo,
            historyRepository = historyRepo,
            settingsRepository = settingsRepo,
            sourceSelector = sourceSelector,
            playerEngine = unifiedEngine
        )

        advanceUntilIdle()

        // Should initialize to testEpisode.lastPositionMs (45,000 ms)
        assertEquals(45000L, viewModel.playerState.value.currentPositionMs)

        unifiedEngine.release()
    }

    private class TestSourcePrefRepo : SourcePreferenceRepository {
        private val _globalProvider = MutableStateFlow<String?>("builtin.test.anime")
        override val globalPreferredProvider: Flow<String?> = _globalProvider.asStateFlow()

        private val _globalServer = MutableStateFlow<String?>("FastCDN (Global)")
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

    private class TestExtensionStorageRepo : ExtensionStorageRepository {
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

    private class TestSettingsRepo : SettingsRepository {
        private val _settings = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = _settings.asStateFlow()

        override suspend fun setThemeMode(mode: AppThemeMode) {}
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
        override suspend fun setDefaultExtensionId(extensionId: String) {}
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

    private class TestAnimeRepo(private val animes: List<Anime>) : AnimeRepository {
        override fun getAnimeById(localId: String): Flow<Anime?> = flowOf(animes.find { it.localId == localId })
        override suspend fun getAnimeByIdDirect(localId: String): Anime? = animes.find { it.localId == localId }
        override suspend fun getAnimeBySource(sourceId: String, sourceAnimeId: String): Anime? = animes.find { it.sourceId == sourceId && it.sourceAnimeId == sourceAnimeId }
        override fun getAllAnime(): Flow<List<Anime>> = flowOf(animes)
        override fun getFavoriteAnime(): Flow<List<Anime>> = flowOf(emptyList())
        override fun searchLocalAnime(query: String): Flow<List<Anime>> = flowOf(animes)
        override suspend fun saveAnime(anime: Anime): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun saveAnimeList(animeList: List<Anime>): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun toggleFavorite(localId: String, isFavorite: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteAnime(localId: String): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun getPopularAnime(extensionId: String?, page: Int): AppResult<List<Anime>> = AppResult.Success(animes)
        override suspend fun getLatestAnime(extensionId: String?, page: Int): AppResult<List<Anime>> = AppResult.Success(animes)
        override suspend fun searchAnime(query: String, extensionId: String?, page: Int, filters: Map<String, Any>): AppResult<List<Anime>> = AppResult.Success(animes)
        override suspend fun getAnimeDetails(sourceId: String, sourceAnimeId: String): AppResult<Anime> = AppResult.Success(animes.first())
        override suspend fun getEpisodes(sourceId: String, sourceAnimeId: String): AppResult<List<Episode>> = AppResult.Success(emptyList())
    }

    private class TestEpisodeRepo(private val episodes: List<Episode>) : EpisodeRepository {
        override fun getEpisodeById(episodeId: String): Flow<Episode?> = flowOf(episodes.find { it.id == episodeId })
        override suspend fun getEpisodeByIdDirect(episodeId: String): Episode? = episodes.find { it.id == episodeId }
        override fun getEpisodesForAnime(animeId: String): Flow<List<Episode>> = flowOf(episodes.filter { it.animeId == animeId })
        override suspend fun getEpisodesForAnimeDirect(animeId: String): List<Episode> = episodes.filter { it.animeId == animeId }
        override fun getEpisodesForSeason(animeId: String, seasonNumber: Int): Flow<List<Episode>> = flowOf(episodes.filter { it.animeId == animeId })
        override suspend fun saveEpisode(episode: Episode): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun saveEpisodes(episodes: List<Episode>): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateProgress(episodeId: String, positionMs: Long, durationMs: Long, isWatched: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun markWatched(episodeId: String, isWatched: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun markWatchedUpTo(animeId: String, upToNumber: Float, isWatched: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteEpisode(episodeId: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class TestHistoryRepo : HistoryRepository {
        override fun getAllHistory(): Flow<List<HistoryEntry>> = flowOf(emptyList())
        override fun getLastWatchedForAnime(animeId: String): Flow<HistoryEntry?> = flowOf(null)
        override suspend fun recordHistory(animeId: String, episodeId: String, sourceId: String, positionMs: Long, durationMs: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun removeHistoryForEpisode(episodeId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun removeHistoryForAnime(animeId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun clearAllHistory(): AppResult<Unit> = AppResult.Success(Unit)
    }
}
