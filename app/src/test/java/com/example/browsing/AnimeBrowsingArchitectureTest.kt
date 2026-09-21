package com.example.browsing

import com.example.core.result.AppResult
import com.example.data.extension.builtin.TestAnimeExtension
import com.example.data.local.database.dao.AnimeDao
import com.example.data.local.database.dao.EpisodeDao
import com.example.data.local.database.entity.AnimeEntity
import com.example.data.local.database.entity.EpisodeEntity
import com.example.data.repository.AnimeRepositoryImpl
import com.example.domain.extension.DefaultExtensionManager
import com.example.domain.model.Anime
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionRepository
import com.example.domain.model.HistoryEntry
import com.example.domain.model.LibraryEntry
import com.example.domain.model.LibraryStatus
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.LibraryRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.usecase.AnimeSearchUseCase
import com.example.domain.usecase.GetAnimeDetailsUseCase
import com.example.domain.usecase.GetEpisodesUseCase
import com.example.domain.usecase.GetLatestAnimeUseCase
import com.example.domain.usecase.GetPopularAnimeUseCase
import com.example.domain.model.TrackingBinding
import com.example.domain.model.TrackingPlatform
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.model.tracking.AniListUser
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.TrackingBindingRepository
import com.example.domain.usecase.tracking.AutoMapAnimeUseCase
import com.example.domain.usecase.tracking.SearchAniListMediaUseCase
import com.example.features.details.AnimeDetailsViewModel
import com.example.features.home.HomeViewModel
import com.example.features.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeBrowsingArchitectureTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeAnimeDao: FakeAnimeDao
    private lateinit var fakeEpisodeDao: FakeEpisodeDao
    private lateinit var extensionManager: DefaultExtensionManager
    private lateinit var animeRepository: AnimeRepository
    private lateinit var episodeRepository: FakeEpisodeRepository
    private lateinit var historyRepository: FakeHistoryRepository
    private lateinit var libraryRepository: FakeLibraryRepository

    private lateinit var searchUseCase: AnimeSearchUseCase
    private lateinit var popularUseCase: GetPopularAnimeUseCase
    private lateinit var latestUseCase: GetLatestAnimeUseCase
    private lateinit var detailsUseCase: GetAnimeDetailsUseCase
    private lateinit var episodesUseCase: GetEpisodesUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeAnimeDao = FakeAnimeDao()
        fakeEpisodeDao = FakeEpisodeDao()
        episodeRepository = FakeEpisodeRepository()
        historyRepository = FakeHistoryRepository()
        libraryRepository = FakeLibraryRepository()

        val fakeStorage = FakeExtensionStorageRepository()
        val fakeSettings = FakeSettingsRepository()

        extensionManager = DefaultExtensionManager(fakeStorage, fakeSettings)
        val testExtension = TestAnimeExtension()
        extensionManager.registerExtension(testExtension)

        animeRepository = AnimeRepositoryImpl(
            animeDao = fakeAnimeDao,
            episodeDao = fakeEpisodeDao,
            extensionManager = extensionManager
        )

        searchUseCase = AnimeSearchUseCase(animeRepository)
        popularUseCase = GetPopularAnimeUseCase(animeRepository)
        latestUseCase = GetLatestAnimeUseCase(animeRepository)
        detailsUseCase = GetAnimeDetailsUseCase(animeRepository)
        episodesUseCase = GetEpisodesUseCase(animeRepository, episodeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetPopularAnimeUseCase_returnsDataFromExtensionAndCachesToLocalDao() = runTest(testDispatcher) {
        val result = popularUseCase(page = 1)

        assertTrue(result is AppResult.Success)
        val list = (result as AppResult.Success).data
        assertTrue(list.isNotEmpty())
        assertTrue(list.any { it.title.contains("Frieren") })

        // Verify local DB cache
        assertTrue(fakeAnimeDao.cachedEntities.isNotEmpty())
    }

    @Test
    fun testGetLatestAnimeUseCase_returnsLatestReleases() = runTest(testDispatcher) {
        val result = latestUseCase(page = 1)

        assertTrue(result is AppResult.Success)
        val list = (result as AppResult.Success).data
        assertTrue(list.isNotEmpty())
    }

    @Test
    fun testAnimeSearchUseCase_filtersAndReturnsMatches() = runTest(testDispatcher) {
        val emptyResult = searchUseCase(query = "   ")
        assertTrue(emptyResult is AppResult.Success)
        assertTrue((emptyResult as AppResult.Success).data.isEmpty())

        val matchResult = searchUseCase(query = "Solo Leveling")
        assertTrue(matchResult is AppResult.Success)
        val matches = (matchResult as AppResult.Success).data
        assertTrue(matches.isNotEmpty())
        assertEquals("Solo Leveling", matches.first().title)
    }

    @Test
    fun testGetAnimeDetailsUseCase_loadsFullMetadata() = runTest(testDispatcher) {
        val detailsResult = detailsUseCase(
            sourceId = "builtin.test.anime",
            sourceAnimeId = "frieren-beyond-journeys-end"
        )

        assertTrue(detailsResult is AppResult.Success)
        val anime = (detailsResult as AppResult.Success).data
        assertEquals("Frieren: Beyond Journey's End", anime.title)
        assertTrue(anime.genres.contains("Adventure"))
        assertEquals(2023, anime.year)
        assertEquals("Madhouse", anime.studio)
    }

    @Test
    fun testGetEpisodesUseCase_mergesLocalWatchProgress() = runTest(testDispatcher) {
        val animeId = "builtin.test.anime:frieren-beyond-journeys-end"
        // Seed local episode state with matching sourceEpisodeId
        episodeRepository.saveEpisode(
            Episode(
                id = "$animeId:ep1",
                animeId = animeId,
                sourceId = "builtin.test.anime",
                sourceEpisodeId = "ep1",
                number = 1f,
                title = "The Journey's End",
                lastPositionMs = 500000L,
                durationMs = 1440000L,
                isWatched = false
            )
        )

        val result = episodesUseCase(
            sourceId = "builtin.test.anime",
            sourceAnimeId = "frieren-beyond-journeys-end",
            animeLocalId = animeId
        )

        assertTrue(result is AppResult.Success)
        val episodes = (result as AppResult.Success).data
        assertTrue(episodes.isNotEmpty())
        val ep1 = episodes.first { it.number == 1f }
        assertEquals(500000L, ep1.lastPositionMs)
        assertTrue(ep1.progressPercent > 0.3f)
    }

    @Test
    fun testHomeViewModel_loadsFeedAndHistory() = runTest(testDispatcher) {
        val sampleAnime = Anime(localId = "a1", title = "Test Anime")
        val sampleEp = Episode(id = "e1", animeId = "a1", sourceId = "s1", sourceEpisodeId = "se1", number = 1f)
        historyRepository.recordHistory(
            animeId = "a1",
            episodeId = "e1",
            sourceId = "s1",
            positionMs = 1000L,
            durationMs = 2000L
        )

        val viewModel = HomeViewModel(
            getPopularAnimeUseCase = popularUseCase,
            getLatestAnimeUseCase = latestUseCase,
            historyRepository = historyRepository,
            episodeRepository = episodeRepository
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.popular.isNotEmpty())
        assertTrue(state.trending.isNotEmpty())
        assertTrue(state.latest.isNotEmpty())
    }

    @Test
    fun testSearchViewModel_managesSearchHistoryAndSources() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(
            animeSearchUseCase = searchUseCase,
            extensionManager = extensionManager
        )

        advanceUntilIdle()

        viewModel.addSearchHistory("Demon Slayer")
        assertTrue(viewModel.uiState.value.searchHistory.contains("Demon Slayer"))

        viewModel.onQueryChange("Frieren")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSearching)
        assertTrue(viewModel.uiState.value.results.any { it.title.contains("Frieren") })

        viewModel.removeSearchHistory("Demon Slayer")
        assertFalse(viewModel.uiState.value.searchHistory.contains("Demon Slayer"))
    }

    @Test
    fun testAnimeDetailsViewModel_togglesFavoriteAndWatched() = runTest(testDispatcher) {
        val fakeTrackingRepo = FakeTrackingBindingRepository()
        val fakeAniListRepo = FakeAniListRepository()
        val viewModel = AnimeDetailsViewModel(
            localId = "builtin.test.anime:frieren-beyond-journeys-end",
            sourceId = "builtin.test.anime",
            sourceAnimeId = "frieren-beyond-journeys-end",
            getAnimeDetailsUseCase = detailsUseCase,
            getEpisodesUseCase = episodesUseCase,
            getPopularAnimeUseCase = popularUseCase,
            animeRepository = animeRepository,
            episodeRepository = episodeRepository,
            libraryRepository = libraryRepository,
            historyRepository = historyRepository,
            trackingBindingRepository = fakeTrackingRepo,
            autoMapAnimeUseCase = AutoMapAnimeUseCase(fakeAniListRepo),
            searchAniListMediaUseCase = SearchAniListMediaUseCase(fakeAniListRepo)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.anime)
        assertEquals("Frieren: Beyond Journey's End", state.anime?.title)
        assertTrue(state.episodes.isNotEmpty())

        // Toggle Favorite
        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFavorite)

        // Toggle Watched
        val ep1 = viewModel.uiState.value.episodes.first()
        viewModel.toggleEpisodeWatched(ep1)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.episodes.first().isWatched)
    }

    // Fakes
    class FakeAnimeDao : AnimeDao {
        val cachedEntities = mutableMapOf<String, AnimeEntity>()

        override fun getAnimeById(localId: String): Flow<AnimeEntity?> = flowOf(cachedEntities[localId])
        override suspend fun getAnimeByIdDirect(localId: String): AnimeEntity? = cachedEntities[localId]
        override suspend fun getAnimeBySourceId(sourceId: String, sourceAnimeId: String): AnimeEntity? =
            cachedEntities.values.firstOrNull { it.sourceId == sourceId && it.sourceAnimeId == sourceAnimeId }
        override fun getAllAnime(): Flow<List<AnimeEntity>> = flowOf(cachedEntities.values.toList())
        override fun getFavoriteAnime(): Flow<List<AnimeEntity>> =
            flowOf(cachedEntities.values.filter { it.isFavorite })
        override fun searchLocalAnime(query: String): Flow<List<AnimeEntity>> =
            flowOf(cachedEntities.values.filter { it.title.contains(query, ignoreCase = true) })
        override suspend fun insertAnime(anime: AnimeEntity) { cachedEntities[anime.localId] = anime }
        override suspend fun insertAnimeList(animeList: List<AnimeEntity>) {
            animeList.forEach { cachedEntities[it.localId] = it }
        }
        override suspend fun updateAnime(anime: AnimeEntity) {
            cachedEntities[anime.localId] = anime
        }
        override suspend fun updateFavoriteStatus(localId: String, isFavorite: Boolean) {
            cachedEntities[localId]?.let { cachedEntities[localId] = it.copy(isFavorite = isFavorite) }
        }
        override suspend fun deleteAnimeById(localId: String) { cachedEntities.remove(localId) }
        override suspend fun getNonEssentialAnimeCount(): Int = 0
        override suspend fun deleteNonEssentialAnime(): Int = 0
    }

    class FakeEpisodeDao : EpisodeDao {
        val cached = mutableMapOf<String, EpisodeEntity>()
        override fun getEpisodeById(episodeId: String): Flow<EpisodeEntity?> = flowOf(cached[episodeId])
        override suspend fun getEpisodeByIdDirect(episodeId: String): EpisodeEntity? = cached[episodeId]
        override fun getEpisodesForAnime(animeId: String): Flow<List<EpisodeEntity>> =
            flowOf(cached.values.filter { it.animeId == animeId })
        override suspend fun getEpisodesForAnimeDirect(animeId: String): List<EpisodeEntity> =
            cached.values.filter { it.animeId == animeId }
        override fun getEpisodesForSeason(animeId: String, seasonNumber: Int): Flow<List<EpisodeEntity>> =
            flowOf(cached.values.filter { it.animeId == animeId && it.seasonNumber == seasonNumber })
        override suspend fun insertEpisode(episode: EpisodeEntity) { cached[episode.id] = episode }
        override suspend fun insertEpisodes(episodes: List<EpisodeEntity>) {
            episodes.forEach { cached[it.id] = it }
        }
        override suspend fun updateEpisode(episode: EpisodeEntity) {
            cached[episode.id] = episode
        }
        override suspend fun updateEpisodeProgress(episodeId: String, positionMs: Long, durationMs: Long, isWatched: Boolean) {
            cached[episodeId]?.let {
                cached[episodeId] = it.copy(lastPositionMs = positionMs, durationMs = durationMs, isWatched = isWatched)
            }
        }
        override suspend fun markEpisodeWatched(episodeId: String, isWatched: Boolean) {
            cached[episodeId]?.let { cached[episodeId] = it.copy(isWatched = isWatched) }
        }
        override suspend fun markEpisodesWatchedUpTo(animeId: String, upToNumber: Float, isWatched: Boolean) {
            cached.values.filter { it.animeId == animeId && it.number <= upToNumber }.forEach {
                cached[it.id] = it.copy(isWatched = isWatched)
            }
        }
        override suspend fun deleteEpisodeById(episodeId: String) {
            cached.remove(episodeId)
        }
        override suspend fun deleteEpisodesForAnime(animeId: String) {
            cached.entries.removeIf { it.value.animeId == animeId }
        }
        override suspend fun getNonEssentialEpisodeCount(): Int = 0
        override suspend fun deleteNonEssentialEpisodes(): Int = 0
    }

    class FakeEpisodeRepository : EpisodeRepository {
        private val list = mutableMapOf<String, Episode>()
        override fun getEpisodeById(episodeId: String): Flow<Episode?> = flowOf(list[episodeId])
        override suspend fun getEpisodeByIdDirect(episodeId: String): Episode? = list[episodeId]
        override fun getEpisodesForAnime(animeId: String): Flow<List<Episode>> =
            flowOf(list.values.filter { it.animeId == animeId })
        override suspend fun getEpisodesForAnimeDirect(animeId: String): List<Episode> =
            list.values.filter { it.animeId == animeId }
        override fun getEpisodesForSeason(animeId: String, seasonNumber: Int): Flow<List<Episode>> =
            flowOf(list.values.filter { it.animeId == animeId && it.seasonNumber == seasonNumber })
        override suspend fun saveEpisode(episode: Episode): AppResult<Unit> {
            list[episode.id] = episode
            return AppResult.Success(Unit)
        }
        override suspend fun saveEpisodes(episodes: List<Episode>): AppResult<Unit> {
            episodes.forEach { list[it.id] = it }
            return AppResult.Success(Unit)
        }
        override suspend fun updateProgress(episodeId: String, positionMs: Long, durationMs: Long, isWatched: Boolean): AppResult<Unit> {
            list[episodeId]?.let {
                list[episodeId] = it.copy(lastPositionMs = positionMs, durationMs = durationMs, isWatched = isWatched)
            }
            return AppResult.Success(Unit)
        }
        override suspend fun markWatched(episodeId: String, isWatched: Boolean): AppResult<Unit> {
            list[episodeId]?.let { list[episodeId] = it.copy(isWatched = isWatched) }
            return AppResult.Success(Unit)
        }
        override suspend fun markWatchedUpTo(animeId: String, upToNumber: Float, isWatched: Boolean): AppResult<Unit> {
            list.values.filter { it.animeId == animeId && it.number <= upToNumber }.forEach {
                list[it.id] = it.copy(isWatched = isWatched)
            }
            return AppResult.Success(Unit)
        }
        override suspend fun deleteEpisode(episodeId: String): AppResult<Unit> {
            list.remove(episodeId)
            return AppResult.Success(Unit)
        }
    }

    class FakeHistoryRepository : HistoryRepository {
        private val entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
        override fun getAllHistory(): Flow<List<HistoryEntry>> = entries
        override fun getLastWatchedForAnime(animeId: String): Flow<HistoryEntry?> =
            flowOf(entries.value.firstOrNull { it.anime.localId == animeId })
        override suspend fun recordHistory(animeId: String, episodeId: String, sourceId: String, positionMs: Long, durationMs: Long): AppResult<Unit> {
            val entry = HistoryEntry(
                anime = Anime(localId = animeId, title = "History Anime"),
                episode = Episode(id = episodeId, animeId = animeId, sourceId = sourceId, sourceEpisodeId = episodeId, number = 1f),
                positionMs = positionMs,
                durationMs = durationMs,
                sourceId = sourceId
            )
            entries.value = listOf(entry) + entries.value.filterNot { it.episode.id == episodeId }
            return AppResult.Success(Unit)
        }
        override suspend fun removeHistoryForEpisode(episodeId: String): AppResult<Unit> {
            entries.value = entries.value.filterNot { it.episode.id == episodeId }
            return AppResult.Success(Unit)
        }
        override suspend fun removeHistoryForAnime(animeId: String): AppResult<Unit> {
            entries.value = entries.value.filterNot { it.anime.localId == animeId }
            return AppResult.Success(Unit)
        }
        override suspend fun clearAllHistory(): AppResult<Unit> {
            entries.value = emptyList()
            return AppResult.Success(Unit)
        }
    }

    class FakeLibraryRepository : LibraryRepository {
        override fun getAllLibraryEntries(): Flow<List<LibraryEntry>> = flowOf(emptyList())
        override fun getLibraryEntriesByStatus(status: LibraryStatus): Flow<List<LibraryEntry>> = flowOf(emptyList())
        override fun getLibraryEntryForAnime(animeId: String): Flow<LibraryEntry?> = flowOf(null)
        override fun isAnimeInLibrary(animeId: String): Flow<Boolean> = flowOf(false)
        override suspend fun addToLibrary(animeId: String, status: LibraryStatus, customCategory: String, score: Double?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateStatus(animeId: String, status: LibraryStatus): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateProgress(animeId: String, watchedCount: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun removeFromLibrary(animeId: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    class FakeExtensionStorageRepository : ExtensionStorageRepository {
        private val list = mutableListOf<ExtensionManifest>()
        private val repos = mutableListOf<ExtensionRepository>()
        override fun getInstalledExtensions(): Flow<List<ExtensionManifest>> = flowOf(list.filter { it.isInstalled })
        override fun getEnabledExtensions(): Flow<List<ExtensionManifest>> = flowOf(list.filter { it.isEnabled })
        override fun getExtensionsWithUpdates(): Flow<List<ExtensionManifest>> = flowOf(emptyList())
        override fun getExtensionById(id: String): Flow<ExtensionManifest?> = flowOf(list.firstOrNull { it.id == id })
        override suspend fun saveInstalledExtension(manifest: ExtensionManifest): AppResult<Unit> {
            list.removeIf { it.id == manifest.id }
            list.add(manifest)
            return AppResult.Success(Unit)
        }
        override suspend fun setExtensionEnabled(id: String, isEnabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setHasUpdate(id: String, hasUpdate: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setExtensionOrder(id: String, order: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun reorderExtensions(orderedIds: List<String>): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteExtension(id: String): AppResult<Unit> {
            list.removeIf { it.id == id }
            return AppResult.Success(Unit)
        }
        override fun getAllRepositories(): Flow<List<ExtensionRepository>> = flowOf(repos)
        override fun getEnabledRepositories(): Flow<List<ExtensionRepository>> = flowOf(repos.filter { it.isEnabled })
        override suspend fun saveRepository(repo: ExtensionRepository): AppResult<Unit> {
            repos.removeIf { it.id == repo.id }
            repos.add(repo)
            return AppResult.Success(Unit)
        }
        override suspend fun setRepositoryEnabled(id: String, isEnabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateRepositorySyncInfo(id: String, count: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteRepository(id: String): AppResult<Unit> {
            repos.removeIf { it.id == id }
            return AppResult.Success(Unit)
        }
    }

    class FakeSettingsRepository : SettingsRepository {
        override val settings: Flow<AppSettings> = flowOf(AppSettings())
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

    class FakeTrackingBindingRepository : TrackingBindingRepository {
        private val bindings = mutableMapOf<String, TrackingBinding>()
        override fun getBindingsForAnime(localAnimeId: String): Flow<List<TrackingBinding>> = flowOf(bindings.values.filter { it.localAnimeId == localAnimeId })
        override fun getBinding(localAnimeId: String, platform: TrackingPlatform): Flow<TrackingBinding?> = flowOf(bindings["${localAnimeId}_${platform.name}"])
        override suspend fun saveBinding(binding: TrackingBinding): AppResult<Unit> {
            bindings["${binding.localAnimeId}_${binding.platform.name}"] = binding
            return AppResult.Success(Unit)
        }
        override suspend fun updateProgress(localAnimeId: String, platform: TrackingPlatform, progress: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteBinding(localAnimeId: String, platform: TrackingPlatform): AppResult<Unit> {
            bindings.remove("${localAnimeId}_${platform.name}")
            return AppResult.Success(Unit)
        }
        override suspend fun deleteBindingsForAnime(localAnimeId: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    class FakeAniListRepository : AniListRepository {
        override fun getAccessToken(): Flow<String?> = flowOf(null)
        override fun getStoredUser(): Flow<AniListUser?> = flowOf(null)
        override suspend fun loginWithToken(token: String): AppResult<AniListUser> = AppResult.Success(AniListUser(1, "Test"))
        override suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun fetchCurrentUserProfile(): AppResult<AniListUser> = AppResult.Success(AniListUser(1, "Test"))
        override suspend fun updateMediaProgress(mediaId: Int, status: com.example.domain.model.tracking.TrackStatus, progress: Int, score: Double?): AppResult<AniListMediaEntry> = AppResult.Success(AniListMediaEntry(mediaId, "Test"))
        override suspend fun getMediaEntry(mediaId: Int): AppResult<AniListMediaEntry?> = AppResult.Success(null)
        override suspend fun getUserMediaList(status: com.example.domain.model.tracking.TrackStatus?): AppResult<List<AniListMediaEntry>> = AppResult.Success(emptyList())
        override suspend fun searchAniListMedia(query: String): AppResult<List<AniListMediaEntry>> = AppResult.Success(emptyList())
    }
}
