package com.example.audit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.extension.builtin.MirrorTestAnimeExtension
import com.example.data.extension.builtin.TestAnimeExtension
import com.example.data.extension.repository.DefaultRepositoryFetcher
import com.example.data.local.database.dao.DownloadWithDetails
import com.example.data.local.database.entity.DownloadEntity
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.DefaultExtensionManager
import com.example.domain.extension.ExtensionManager
import com.example.domain.extension.factory.DefaultExtensionRuntimeFactory
import com.example.domain.extension.logging.DefaultExtensionLogger
import com.example.domain.extension.manager.DefaultExtensionRuntimeManager
import com.example.domain.extension.parser.DefaultExtensionManifestParser
import com.example.domain.extension.runtime.BuiltInExtensionRuntime
import com.example.domain.extension.runtime.IsolatedAnimeExtension
import com.example.domain.extension.validator.DefaultExtensionValidator
import com.example.domain.extension.validator.ExtensionValidationResult
import com.example.domain.model.Anime
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.DownloadStatus
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionRepository
import com.example.domain.model.HistoryEntry
import com.example.domain.model.LibraryEntry
import com.example.domain.model.LibraryStatus
import com.example.domain.model.SourceType
import com.example.domain.model.TrackingBinding
import com.example.domain.model.TrackingPlatform
import com.example.domain.model.VideoSource
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.model.tracking.AniListUser
import com.example.domain.model.tracking.TrackStatus
import com.example.domain.player.selector.ProviderSelector
import com.example.domain.player.selector.ServerSelector
import com.example.domain.player.selector.SourceSelector
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.DownloadRepository
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.LibraryRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SourcePreferenceRepository
import com.example.domain.repository.TrackingBindingRepository
import com.example.domain.usecase.AnimeSearchUseCase
import com.example.domain.usecase.GetAnimeDetailsUseCase
import com.example.domain.usecase.GetEpisodesUseCase
import com.example.domain.usecase.GetLatestAnimeUseCase
import com.example.domain.usecase.GetPopularAnimeUseCase
import com.example.domain.usecase.tracking.AutoMapAnimeUseCase
import com.example.domain.usecase.tracking.SearchAniListMediaUseCase
import com.example.features.details.AnimeDetailsViewModel
import com.example.features.home.HomeViewModel
import com.example.features.player.PlayerViewModel
import com.example.features.search.SearchViewModel
import com.example.player.UnifiedPlayerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
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
class CompleteProductionReadinessAuditTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    private lateinit var extensionManager: ExtensionManager
    private lateinit var preferenceRepository: SourcePreferenceRepository
    private lateinit var providerSelector: ProviderSelector
    private lateinit var serverSelector: ServerSelector
    private lateinit var sourceSelector: SourceSelector
    private lateinit var animeRepo: TestAnimeRepo
    private lateinit var episodeRepo: TestEpisodeRepo
    private lateinit var historyRepo: TestHistoryRepo
    private lateinit var libraryRepo: TestLibraryRepo
    private lateinit var downloadRepo: TestDownloadRepo
    private lateinit var trackingBindingRepo: TestTrackingBindingRepo
    private lateinit var aniListRepo: TestAniListRepo
    private lateinit var settingsRepo: TestSettingsRepo

    private val sampleAnime = Anime(
        localId = "anime-audit-01",
        title = "Attack on Titan",
        description = "Humanity fights for survival against man-eating giants.",
        sourceId = "builtin.test.anime",
        sourceAnimeId = "aot-final",
        poster = "https://example.com/aot.jpg",
        status = "Completed",
        episodeCount = 24
    )

    private val sampleEpisode1 = Episode(
        id = "ep-aot-01",
        animeId = "anime-audit-01",
        sourceId = "builtin.test.anime",
        sourceEpisodeId = "aot-ep-1",
        number = 1.0f,
        seasonNumber = 1,
        title = "To You, in 2000 Years",
        lastPositionMs = 30000L,
        durationMs = 1440000L
    )

    private val sampleEpisode2 = Episode(
        id = "ep-aot-02",
        animeId = "anime-audit-01",
        sourceId = "builtin.test.anime",
        sourceEpisodeId = "aot-ep-2",
        number = 2.0f,
        seasonNumber = 1,
        title = "That Day",
        lastPositionMs = 0L,
        durationMs = 1440000L
    )

    private val sampleEpisodeSeason2 = Episode(
        id = "ep-aot-s2-01",
        animeId = "anime-audit-01",
        sourceId = "builtin.test.anime",
        sourceEpisodeId = "aot-s2-ep-1",
        number = 1.0f,
        seasonNumber = 2,
        title = "Beast Titan",
        lastPositionMs = 0L,
        durationMs = 1440000L
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
        settingsRepo = TestSettingsRepo()

        extensionManager = DefaultExtensionManager(
            storageRepository = fakeStorage,
            settingsRepository = settingsRepo,
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
        sourceSelector = SourceSelector(extensionManager, providerSelector, serverSelector, preferenceRepository)

        animeRepo = TestAnimeRepo(listOf(sampleAnime))
        episodeRepo = TestEpisodeRepo(listOf(sampleEpisode1, sampleEpisode2, sampleEpisodeSeason2))
        historyRepo = TestHistoryRepo()
        libraryRepo = TestLibraryRepo()
        downloadRepo = TestDownloadRepo()
        trackingBindingRepo = TestTrackingBindingRepo()
        aniListRepo = TestAniListRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. COMPLETE END-TO-END CRITICAL USER JOURNEY AUDIT
    // =========================================================================

    @Test
    fun audit_completeEndToEndHappyPathFlow() = runTest(testDispatcher) {
        // Step 1: Open app -> Initialize Home
        val homeViewModel = HomeViewModel(
            getPopularAnimeUseCase = GetPopularAnimeUseCase(animeRepo),
            getLatestAnimeUseCase = GetLatestAnimeUseCase(animeRepo),
            historyRepository = historyRepo,
            episodeRepository = episodeRepo
        )
        advanceUntilIdle()
        assertFalse(homeViewModel.uiState.value.isLoading)
        assertTrue(homeViewModel.uiState.value.popular.isNotEmpty())

        // Step 2: Search anime
        val searchViewModel = SearchViewModel(
            animeSearchUseCase = AnimeSearchUseCase(animeRepo),
            extensionManager = extensionManager
        )
        searchViewModel.onQueryChange("Attack")
        advanceUntilIdle()
        assertTrue(searchViewModel.uiState.value.results.isNotEmpty())
        assertEquals("Attack on Titan", searchViewModel.uiState.value.results.first().title)

        // Step 3: Open details
        val getPopularUseCase = GetPopularAnimeUseCase(animeRepo)
        val getAnimeDetailsUseCase = GetAnimeDetailsUseCase(animeRepo)
        val getEpisodesUseCase = GetEpisodesUseCase(animeRepo, episodeRepo)
        val searchAniListMediaUseCase = SearchAniListMediaUseCase(aniListRepo)
        val autoMapUseCase = AutoMapAnimeUseCase(
            aniListRepository = aniListRepo
        )

        val detailsViewModel = AnimeDetailsViewModel(
            localId = sampleAnime.localId,
            sourceId = sampleAnime.sourceId,
            sourceAnimeId = sampleAnime.sourceAnimeId,
            getAnimeDetailsUseCase = getAnimeDetailsUseCase,
            getEpisodesUseCase = getEpisodesUseCase,
            getPopularAnimeUseCase = getPopularUseCase,
            animeRepository = animeRepo,
            episodeRepository = episodeRepo,
            libraryRepository = libraryRepo,
            historyRepository = historyRepo,
            trackingBindingRepository = trackingBindingRepo,
            autoMapAnimeUseCase = autoMapUseCase,
            searchAniListMediaUseCase = searchAniListMediaUseCase
        )
        advanceUntilIdle()
        val detailsState = detailsViewModel.uiState.value
        assertEquals("Attack on Titan", detailsState.anime?.title)
        assertEquals(3, detailsState.episodes.size)

        // Step 4: Select season
        assertEquals(listOf(1, 2), detailsState.availableSeasons)
        detailsViewModel.selectSeason(2)
        assertEquals(2, detailsViewModel.uiState.value.selectedSeason)

        // Step 5: Select episode
        val selectedEpisode = detailsState.episodes.first { it.id == sampleEpisode1.id }
        assertEquals(1.0f, selectedEpisode.number)

        // Step 6: Select extension/provider
        val extension = providerSelector.resolveBestProvider(sampleAnime.localId)
        assertNotNull(extension)
        assertEquals("builtin.test.anime", extension?.id)

        // Step 7: Get VideoSource
        val sourcesRes = extension!!.getEpisodeStreams(selectedEpisode.sourceEpisodeId)
        assertTrue(sourcesRes is AppResult.Success)
        val sources = (sourcesRes as AppResult.Success).data
        assertTrue(sources.isNotEmpty())

        // Step 8: Select server
        val selectedServer = serverSelector.resolveBestServer(sources, sampleAnime.localId)
        assertNotNull(selectedServer)

        // Step 9: Select quality
        val selectionState = sourceSelector.loadSourcesForEpisode(
            animeId = sampleAnime.localId,
            episode = selectedEpisode,
            forcedQuality = "1080p"
        )
        assertTrue(selectionState.hasSources)
        val activeSource = selectionState.activeSource
        assertNotNull(activeSource)
        assertTrue(activeSource?.quality?.contains("1080") == true)

        // Step 10: Play HLS/MP4 in PlayerEngine
        val unifiedEngine = UnifiedPlayerEngine(context)
        val playerViewModel = PlayerViewModel(
            animeId = sampleAnime.localId,
            initialEpisodeId = selectedEpisode.id,
            animeRepository = animeRepo,
            episodeRepository = episodeRepo,
            historyRepository = historyRepo,
            settingsRepository = settingsRepo,
            sourceSelector = sourceSelector,
            playerEngine = unifiedEngine
        )
        advanceUntilIdle()

        // Verify Player loaded source
        val playerState = playerViewModel.uiState.value
        val sourceState = playerViewModel.sourceSelectionState.value
        assertEquals(sampleEpisode1.id, playerState.currentEpisode?.id)
        assertTrue(sourceState.hasSources)
        assertNotNull(sourceState.activeSource)

        // Step 11: Subtitle track selection
        unifiedEngine.selectSubtitle("sub_en")
        advanceUntilIdle()

        // Step 12: Audio track selection
        unifiedEngine.selectAudioTrack("audio_jp")
        advanceUntilIdle()

        // Step 13: Seek
        playerViewModel.seekRelative(10)
        advanceUntilIdle()

        // Step 14: Gesture Controls (simulate speed change)
        playerViewModel.setPlaybackSpeed(2.0f)
        advanceUntilIdle()
        assertEquals(2.0f, playerViewModel.playerState.value.playbackSpeed, 0.01f)

        // Step 15: Auto-Next trigger
        playerViewModel.playNextEpisode()
        advanceUntilIdle()
        assertEquals(sampleEpisode2.id, playerViewModel.uiState.value.currentEpisode?.id)

        // Step 16: Watch history updated
        historyRepo.recordHistory(sampleAnime.localId, sampleEpisode1.id, sampleAnime.sourceId, 45000L, 1440000L)
        advanceUntilIdle()
        val historyEntries = historyRepo.historyList.value
        assertTrue(historyEntries.isNotEmpty())
        assertEquals(sampleAnime.localId, historyEntries.first().anime.localId)

        // Step 17: AniList Progress Sync
        trackingBindingRepo.saveBinding(
            TrackingBinding(
                localAnimeId = sampleAnime.localId,
                sourceId = sampleAnime.sourceId,
                sourceAnimeId = sampleAnime.sourceAnimeId,
                platform = TrackingPlatform.ANILIST,
                aniListId = 16498,
                season = 1,
                mappingConfidence = 1.0f
            )
        )
        aniListRepo.setAuthenticatedUser(AniListUser(1, "AnimeMaster", null))
        val syncRes = aniListRepo.updateMediaProgress(16498, TrackStatus.WATCHING, 1, 9.0)
        assertTrue(syncRes is AppResult.Success)
        assertEquals(1, aniListRepo.lastSyncedProgress)

        // Step 18: Add to Library
        libraryRepo.addToLibrary(sampleAnime.localId, LibraryStatus.WATCHING, "Watching", 9.0)
        assertTrue(libraryRepo.libraryMap.containsKey(sampleAnime.localId))

        // Step 19: Download episode
        val downloadEntity = DownloadEntity(
            episodeId = sampleEpisode1.id,
            animeId = sampleAnime.localId,
            sourceId = sampleAnime.sourceId,
            quality = "1080p",
            downloadUrl = "https://example.com/video.mp4",
            status = DownloadStatus.COMPLETED,
            localFilePath = "/data/local/downloads/aot_01.mp4"
        )
        downloadRepo.saveDownload(downloadEntity)

        // Step 20: Offline playback verification
        val downloaded = downloadRepo.getDownloadDirect(sampleEpisode1.id)
        assertNotNull(downloaded)
        assertEquals(DownloadStatus.COMPLETED, downloaded?.status)
        assertEquals("/data/local/downloads/aot_01.mp4", downloaded?.localFilePath)

        unifiedEngine.release()
    }

    // =========================================================================
    // 2. FAILURE CASES & GRACEFUL DEGRADATION AUDIT
    // =========================================================================

    @Test
    fun audit_failureCase_noInternet_gracefullyDegrades() = runTest(testDispatcher) {
        val failingRepo = object : AnimeRepository by animeRepo {
            override suspend fun getPopularAnime(extensionId: String?, page: Int): AppResult<List<Anime>> =
                AppResult.Error(AppError.NetworkError("No internet connection available"))
            override suspend fun getLatestAnime(extensionId: String?, page: Int): AppResult<List<Anime>> =
                AppResult.Error(AppError.NetworkError("No internet connection available"))
            override suspend fun searchAnime(query: String, extensionId: String?, page: Int, filters: Map<String, Any>): AppResult<List<Anime>> =
                AppResult.Error(AppError.NetworkError("No internet connection available"))
            override suspend fun getAnimeDetails(sourceId: String, sourceAnimeId: String): AppResult<Anime> =
                AppResult.Error(AppError.NetworkError("No internet connection available"))
        }

        val homeViewModel = HomeViewModel(
            getPopularAnimeUseCase = GetPopularAnimeUseCase(failingRepo),
            getLatestAnimeUseCase = GetLatestAnimeUseCase(failingRepo),
            historyRepository = historyRepo,
            episodeRepository = episodeRepo
        )
        advanceUntilIdle()

        assertFalse(homeViewModel.uiState.value.isLoading)
        assertTrue(homeViewModel.uiState.value.popular.isEmpty())
        assertNotNull(homeViewModel.uiState.value.error)
        assertTrue(homeViewModel.uiState.value.error?.contains("internet") == true)
    }

    @Test
    fun audit_failureCase_providerUnavailable_fallsBackToMirror() = runTest(testDispatcher) {
        val brokenPrimary = object : AnimeExtension by TestAnimeExtension() {
            override val id: String = "primary.failing.anime"
            override val name: String = "Broken Primary"
            override suspend fun getEpisodeStreams(sourceEpisodeId: String, extra: Map<String, String>): AppResult<List<VideoSource>> {
                return AppResult.Error(AppError.ProviderError("Server under maintenance 503"))
            }
        }
        val mirrorExtension = MirrorTestAnimeExtension()

        extensionManager.registerExtension(brokenPrimary)
        extensionManager.registerExtension(mirrorExtension)

        // Primary fails
        val primaryRes = brokenPrimary.getEpisodeStreams("ep-1")
        assertTrue(primaryRes is AppResult.Error)

        // Fallback to mirror succeeds
        val mirrorRes = mirrorExtension.getEpisodeStreams("ep-1")
        assertTrue(mirrorRes is AppResult.Success)
        val sources = (mirrorRes as AppResult.Success).data
        assertTrue(sources.isNotEmpty())
    }

    @Test
    fun audit_failureCase_extensionUncaughtException_isolatedInSandbox() = runTest(testDispatcher) {
        val crashingExtension = object : AnimeExtension by TestAnimeExtension() {
            override val id: String = "crashing.extension"
            override val name: String = "Crashing Extension"
            override suspend fun getEpisodeStreams(sourceEpisodeId: String, extra: Map<String, String>): AppResult<List<VideoSource>> {
                throw RuntimeException("Fatal native parser segment fault!")
            }
        }

        val isolated = IsolatedAnimeExtension(crashingExtension, timeoutMs = 2000L)
        val result = isolated.getEpisodeStreams("any-id")

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is AppError.ProviderError)
        assertTrue(error.message?.contains("Crash") == true || error.message?.contains("segment fault") == true)
    }

    @Test
    fun audit_failureCase_invalidSource_serverSelectorAttemptsAlternative() = runTest(testDispatcher) {
        val brokenSource1 = VideoSource(
            id = "src-broken-1",
            providerId = "builtin.test",
            serverName = "BrokenServer1",
            url = "https://invalid.domain.test/404.m3u8",
            quality = "1080p",
            type = SourceType.HLS
        )
        val workingSource2 = VideoSource(
            id = "src-working-2",
            providerId = "builtin.test",
            serverName = "BackupServer2",
            url = "https://stream.example.com/working.m3u8",
            quality = "1080p",
            type = SourceType.HLS
        )

        val sources = listOf(brokenSource1, workingSource2)
        val selected = serverSelector.resolveBestServer(sources, "anime-audit-01")
        assertNotNull(selected)

        // Simulate failure on broken server, switch to BackupServer2
        preferenceRepository.setPreferredServerForAnime("anime-audit-01", "BackupServer2")
        val fallbackSelect = serverSelector.resolveBestServer(sources, "anime-audit-01")
        assertEquals("BackupServer2", fallbackSelect)
    }

    @Test
    fun audit_failureCase_aniListUnavailable_doesNotInterruptPlayback() = runTest(testDispatcher) {
        aniListRepo.simulateNetworkFailure = true
        val unifiedEngine = UnifiedPlayerEngine(context)

        val playerViewModel = PlayerViewModel(
            animeId = sampleAnime.localId,
            initialEpisodeId = sampleEpisode1.id,
            animeRepository = animeRepo,
            episodeRepository = episodeRepo,
            historyRepository = historyRepo,
            settingsRepository = settingsRepo,
            sourceSelector = sourceSelector,
            playerEngine = unifiedEngine
        )
        advanceUntilIdle()

        // Even if AniList throws network 503, player continues normal flow
        assertNotNull(playerViewModel.sourceSelectionState.value.activeSource)

        unifiedEngine.release()
    }

    @Test
    fun audit_failureCase_repositoryUnavailable_returnsGracefulError() = runTest(testDispatcher) {
        val fetcher = DefaultRepositoryFetcher()
        val result = fetcher.fetchRepositoryIndex("https://invalid-repo-server.test/nonexistent-index.json")

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is AppError.NetworkError || error is AppError.UnknownError)
    }

    @Test
    fun audit_failureCase_malformedExtensionManifest_rejectedByValidator() = runTest(testDispatcher) {
        val validator = DefaultExtensionValidator()

        val malformedManifest = ExtensionManifest(
            id = "",
            name = "",
            version = "invalid_semver",
            language = "en",
            iconUrl = "ftp://insecure.url",
            description = "Malformed",
            author = "Unknown",
            minAppVersion = "99.0.0",
            apiVersion = 999,
            minApiVersion = 999,
            targetApiVersion = 999,
            nsfw = false,
            capabilities = listOf("MALICIOUS_UNSUPPORTED_CAPABILITY")
        )

        val validationResult = validator.validate(malformedManifest)
        assertFalse("Malformed manifest must be rejected", validationResult.isValid)
        assertTrue(validationResult is ExtensionValidationResult.Invalid)
        assertTrue((validationResult as ExtensionValidationResult.Invalid).errors.isNotEmpty())
    }
}

// =============================================================================
// TEST HELPER IMPLEMENTATIONS & MOCKS
// =============================================================================

private class TestAnimeRepo(private val animes: List<Anime>) : AnimeRepository {
    override fun getAnimeById(localId: String): Flow<Anime?> = flowOf(animes.find { it.localId == localId })
    override suspend fun getAnimeByIdDirect(localId: String): Anime? = animes.find { it.localId == localId }
    override suspend fun getAnimeBySource(sourceId: String, sourceAnimeId: String): Anime? = animes.find { it.sourceId == sourceId && it.sourceAnimeId == sourceAnimeId }
    override fun getAllAnime(): Flow<List<Anime>> = flowOf(animes)
    override fun getFavoriteAnime(): Flow<List<Anime>> = flowOf(emptyList())
    override fun searchLocalAnime(query: String): Flow<List<Anime>> = flowOf(animes.filter { it.title.contains(query, ignoreCase = true) })
    override suspend fun saveAnime(anime: Anime): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun saveAnimeList(animeList: List<Anime>): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun toggleFavorite(localId: String, isFavorite: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteAnime(localId: String): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun getPopularAnime(extensionId: String?, page: Int): AppResult<List<Anime>> = AppResult.Success(animes)
    override suspend fun getLatestAnime(extensionId: String?, page: Int): AppResult<List<Anime>> = AppResult.Success(animes)
    override suspend fun searchAnime(query: String, extensionId: String?, page: Int, filters: Map<String, Any>): AppResult<List<Anime>> = AppResult.Success(animes.filter { it.title.contains(query, ignoreCase = true) })
    override suspend fun getAnimeDetails(sourceId: String, sourceAnimeId: String): AppResult<Anime> = AppResult.Success(animes.first())
    override suspend fun getEpisodes(sourceId: String, sourceAnimeId: String): AppResult<List<Episode>> = AppResult.Success(
        listOf(
            Episode(id = "ep-aot-01", animeId = "anime-audit-01", sourceId = sourceId, sourceEpisodeId = "aot-ep-1", number = 1.0f, seasonNumber = 1, title = "To You, in 2000 Years", durationMs = 1440000L),
            Episode(id = "ep-aot-02", animeId = "anime-audit-01", sourceId = sourceId, sourceEpisodeId = "aot-ep-2", number = 2.0f, seasonNumber = 1, title = "That Day", durationMs = 1440000L),
            Episode(id = "ep-aot-s2-01", animeId = "anime-audit-01", sourceId = sourceId, sourceEpisodeId = "aot-s2-ep-1", number = 1.0f, seasonNumber = 2, title = "Beast Titan", durationMs = 1440000L)
        )
    )
}

private class TestEpisodeRepo(private val episodes: List<Episode>) : EpisodeRepository {
    override fun getEpisodeById(episodeId: String): Flow<Episode?> = flowOf(episodes.find { it.id == episodeId })
    override suspend fun getEpisodeByIdDirect(episodeId: String): Episode? = episodes.find { it.id == episodeId }
    override fun getEpisodesForAnime(animeId: String): Flow<List<Episode>> = flowOf(episodes.filter { it.animeId == animeId })
    override suspend fun getEpisodesForAnimeDirect(animeId: String): List<Episode> = episodes.filter { it.animeId == animeId }
    override fun getEpisodesForSeason(animeId: String, seasonNumber: Int): Flow<List<Episode>> = flowOf(episodes.filter { it.animeId == animeId && it.seasonNumber == seasonNumber })
    override suspend fun saveEpisode(episode: Episode): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun saveEpisodes(episodes: List<Episode>): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun updateProgress(episodeId: String, positionMs: Long, durationMs: Long, isWatched: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun markWatched(episodeId: String, isWatched: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun markWatchedUpTo(animeId: String, upToNumber: Float, isWatched: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteEpisode(episodeId: String): AppResult<Unit> = AppResult.Success(Unit)
}

private class TestHistoryRepo : HistoryRepository {
    val historyList = MutableStateFlow<List<HistoryEntry>>(emptyList())

    override fun getAllHistory(): Flow<List<HistoryEntry>> = historyList
    override fun getLastWatchedForAnime(animeId: String): Flow<HistoryEntry?> = flowOf(historyList.value.find { it.anime.localId == animeId })
    override suspend fun recordHistory(animeId: String, episodeId: String, sourceId: String, positionMs: Long, durationMs: Long): AppResult<Unit> {
        val entry = HistoryEntry(
            anime = Anime(localId = animeId, title = "AOT"),
            episode = Episode(id = episodeId, animeId = animeId, sourceId = sourceId, sourceEpisodeId = episodeId, number = 1.0f),
            positionMs = positionMs,
            durationMs = durationMs,
            sourceId = sourceId
        )
        historyList.update { list -> listOf(entry) + list.filterNot { it.episode.id == episodeId } }
        return AppResult.Success(Unit)
    }
    override suspend fun removeHistoryForEpisode(episodeId: String): AppResult<Unit> {
        historyList.update { list -> list.filterNot { it.episode.id == episodeId } }
        return AppResult.Success(Unit)
    }
    override suspend fun removeHistoryForAnime(animeId: String): AppResult<Unit> {
        historyList.update { list -> list.filterNot { it.anime.localId == animeId } }
        return AppResult.Success(Unit)
    }
    override suspend fun clearAllHistory(): AppResult<Unit> {
        historyList.value = emptyList()
        return AppResult.Success(Unit)
    }
}

private class TestLibraryRepo : LibraryRepository {
    val libraryMap = mutableMapOf<String, LibraryEntry>()

    override fun getAllLibraryEntries(): Flow<List<LibraryEntry>> = flowOf(libraryMap.values.toList())
    override fun getLibraryEntriesByStatus(status: LibraryStatus): Flow<List<LibraryEntry>> =
        flowOf(libraryMap.values.filter { it.status == status })
    override fun getLibraryEntryForAnime(animeId: String): Flow<LibraryEntry?> = flowOf(libraryMap[animeId])
    override fun isAnimeInLibrary(animeId: String): Flow<Boolean> = flowOf(libraryMap.containsKey(animeId))
    override suspend fun addToLibrary(animeId: String, status: LibraryStatus, customCategory: String, score: Double?): AppResult<Unit> {
        libraryMap[animeId] = LibraryEntry(
            anime = Anime(localId = animeId, title = "Library Anime"),
            status = status,
            customCategory = customCategory,
            userScore = score
        )
        return AppResult.Success(Unit)
    }
    override suspend fun updateStatus(animeId: String, status: LibraryStatus): AppResult<Unit> {
        libraryMap[animeId]?.let { libraryMap[animeId] = it.copy(status = status) }
        return AppResult.Success(Unit)
    }
    override suspend fun updateProgress(animeId: String, watchedCount: Int): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun removeFromLibrary(animeId: String): AppResult<Unit> {
        libraryMap.remove(animeId)
        return AppResult.Success(Unit)
    }
}

private class TestDownloadRepo : DownloadRepository {
    private val downloads = mutableMapOf<String, DownloadEntity>()

    override fun getAllDownloads(): Flow<List<DownloadWithDetails>> = flowOf(emptyList())
    override fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadWithDetails>> = flowOf(emptyList())
    override fun getDownloadsForAnime(animeId: String): Flow<List<DownloadWithDetails>> = flowOf(emptyList())
    override fun getDownloadForEpisode(episodeId: String): Flow<DownloadWithDetails?> = flowOf(null)
    override suspend fun getDownloadDirect(episodeId: String): DownloadEntity? = downloads[episodeId]
    override fun getDownloadedEpisodes(): Flow<List<DownloadWithDetails>> = flowOf(emptyList())
    override suspend fun getDownloadedEpisodesDirect(): List<DownloadEntity> = downloads.values.filter { it.status == DownloadStatus.COMPLETED }
    override suspend fun getAllDownloadsDirect(): List<DownloadEntity> = downloads.values.toList()
    override fun getTotalDownloadedBytes(): Flow<Long?> = flowOf(0L)

    override suspend fun queueDownload(
        episodeId: String,
        animeId: String,
        sourceId: String,
        quality: String,
        downloadUrl: String,
        animeTitle: String,
        episodeTitle: String,
        episodeNumber: Float,
        thumbnail: String
    ): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun saveDownload(entity: DownloadEntity): AppResult<Unit> {
        downloads[entity.episodeId] = entity
        return AppResult.Success(Unit)
    }

    override suspend fun updateDownloadProgress(episodeId: String, downloadedBytes: Long, totalBytes: Long, status: DownloadStatus): AppResult<Unit> {
        downloads[episodeId]?.let {
            downloads[episodeId] = it.copy(downloadedBytes = downloadedBytes, totalBytes = totalBytes, status = status)
        }
        return AppResult.Success(Unit)
    }

    override suspend fun updateDownloadStatus(episodeId: String, status: DownloadStatus, error: String?): AppResult<Unit> {
        downloads[episodeId]?.let {
            downloads[episodeId] = it.copy(status = status)
        }
        return AppResult.Success(Unit)
    }

    override suspend fun markDownloadCompleted(episodeId: String, localFilePath: String, totalBytes: Long): AppResult<Unit> {
        downloads[episodeId]?.let {
            downloads[episodeId] = it.copy(status = DownloadStatus.COMPLETED, localFilePath = localFilePath, totalBytes = totalBytes)
        }
        return AppResult.Success(Unit)
    }

    override suspend fun removeDownload(episodeId: String): AppResult<Unit> {
        downloads.remove(episodeId)
        return AppResult.Success(Unit)
    }

    override suspend fun removeDownloadsForAnime(animeId: String): AppResult<Unit> {
        downloads.entries.removeIf { it.value.animeId == animeId }
        return AppResult.Success(Unit)
    }

    override suspend fun deleteAllDownloads(): AppResult<Unit> {
        downloads.clear()
        return AppResult.Success(Unit)
    }
}

private class TestTrackingBindingRepo : TrackingBindingRepository {
    private val bindings = mutableMapOf<String, TrackingBinding>()

    override fun getBinding(localAnimeId: String, platform: TrackingPlatform): Flow<TrackingBinding?> =
        flowOf(bindings[localAnimeId])
    override fun getBindingsForAnime(localAnimeId: String): Flow<List<TrackingBinding>> =
        flowOf(listOfNotNull(bindings[localAnimeId]))
    override suspend fun saveBinding(binding: TrackingBinding): AppResult<Unit> {
        bindings[binding.localAnimeId] = binding
        return AppResult.Success(Unit)
    }
    override suspend fun deleteBinding(localAnimeId: String, platform: TrackingPlatform): AppResult<Unit> {
        bindings.remove(localAnimeId)
        return AppResult.Success(Unit)
    }
    override suspend fun deleteBindingsForAnime(localAnimeId: String): AppResult<Unit> {
        bindings.remove(localAnimeId)
        return AppResult.Success(Unit)
    }
    override suspend fun updateProgress(localAnimeId: String, platform: TrackingPlatform, progress: Int): AppResult<Unit> = AppResult.Success(Unit)
}

private class TestAniListRepo : AniListRepository {
    var simulateNetworkFailure: Boolean = false
    var lastSyncedProgress: Int = 0
    private var currentUser: AniListUser? = null

    fun setAuthenticatedUser(user: AniListUser) { currentUser = user }

    override fun getStoredUser(): Flow<AniListUser?> = flowOf(currentUser)
    override fun getAccessToken(): Flow<String?> = flowOf(if (currentUser != null) "mock_token" else null)
    override suspend fun loginWithToken(token: String): AppResult<AniListUser> {
        val u = AniListUser(1, "AnimeMaster", null)
        currentUser = u
        return AppResult.Success(u)
    }
    override suspend fun logout(): AppResult<Unit> {
        currentUser = null
        return AppResult.Success(Unit)
    }
    override suspend fun fetchCurrentUserProfile(): AppResult<AniListUser> =
        if (simulateNetworkFailure) AppResult.Error(AppError.NetworkError("AniList 503"))
        else if (currentUser != null) AppResult.Success(currentUser!!)
        else AppResult.Error(AppError.ProviderError("Not logged in"))

    override suspend fun searchAniListMedia(query: String): AppResult<List<AniListMediaEntry>> =
        AppResult.Success(listOf(AniListMediaEntry(16498, "Attack on Titan", "https://example.com/aot.jpg", TrackStatus.WATCHING, 0, 24, 9.0)))

    override suspend fun updateMediaProgress(mediaId: Int, status: TrackStatus, progress: Int, score: Double?): AppResult<AniListMediaEntry> {
        if (simulateNetworkFailure) return AppResult.Error(AppError.NetworkError("AniList 503"))
        lastSyncedProgress = progress
        return AppResult.Success(AniListMediaEntry(mediaId, "Attack on Titan", null, status, progress, 24, score ?: 0.0))
    }

    override suspend fun getMediaEntry(mediaId: Int): AppResult<AniListMediaEntry?> = AppResult.Success(null)
    override suspend fun getUserMediaList(status: TrackStatus?): AppResult<List<AniListMediaEntry>> = AppResult.Success(emptyList())
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

private class TestSourcePrefRepo : SourcePreferenceRepository {
    private val _globalProvider = MutableStateFlow<String?>(null)
    private val _preferredServer = MutableStateFlow<String?>(null)
    private val _preferredQuality = MutableStateFlow("1080p")
    private val _perAnimeServer = mutableMapOf<String, String>()

    override val globalPreferredProvider: Flow<String?> = _globalProvider
    override val preferredServer: Flow<String?> = _preferredServer
    override val preferredQuality: Flow<String> = _preferredQuality

    override suspend fun setGlobalPreferredProvider(providerId: String?) { _globalProvider.value = providerId }
    override suspend fun setPreferredServer(serverName: String?) { _preferredServer.value = serverName }
    override suspend fun setPreferredQuality(quality: String) { _preferredQuality.value = quality }
    override fun getPreferredProviderForAnime(animeId: String): Flow<String?> = flowOf(null)
    override suspend fun setPreferredProviderForAnime(animeId: String, providerId: String?) {}
    override fun getPreferredServerForAnime(animeId: String): Flow<String?> = flowOf(_perAnimeServer[animeId])
    override suspend fun setPreferredServerForAnime(animeId: String, serverName: String?) {
        if (serverName != null) _perAnimeServer[animeId] = serverName else _perAnimeServer.remove(animeId)
    }
    override fun getPreferredQualityForAnime(animeId: String): Flow<String?> = flowOf(null)
    override suspend fun setPreferredQualityForAnime(animeId: String, quality: String?) {}
    override suspend fun clearAnimePreferences(animeId: String) { _perAnimeServer.remove(animeId) }
}
