package com.example.offline

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.cache.CacheManager
import com.example.core.network.ConnectivityNetworkMonitor
import com.example.core.network.TestNetworkMonitor
import com.example.core.result.AppResult
import com.example.data.local.database.AnimeyDatabase
import com.example.data.local.database.entity.AnimeEntity
import com.example.data.local.database.entity.DownloadEntity
import com.example.data.local.database.entity.EpisodeEntity
import com.example.data.local.database.entity.HistoryEntity
import com.example.data.local.database.entity.LibraryEntity
import com.example.data.local.datastore.SettingsDataStore
import com.example.data.repository.AnimeRepositoryImpl
import com.example.data.repository.DownloadRepositoryImpl
import com.example.data.repository.HistoryRepositoryImpl
import com.example.data.repository.LibraryRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.domain.model.AppThemeMode
import com.example.domain.model.DownloadStatus
import com.example.domain.model.LibraryStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineFunctionalityTest {

    private lateinit var context: Context
    private lateinit var database: AnimeyDatabase
    private lateinit var cacheManager: CacheManager
    private lateinit var libraryRepository: LibraryRepositoryImpl
    private lateinit var historyRepository: HistoryRepositoryImpl
    private lateinit var downloadRepository: DownloadRepositoryImpl
    private lateinit var settingsRepository: SettingsRepositoryImpl
    private lateinit var animeRepository: AnimeRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        database = Room.inMemoryDatabaseBuilder(context, AnimeyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        cacheManager = CacheManager(
            context = context,
            animeDao = database.animeDao(),
            episodeDao = database.episodeDao()
        )

        libraryRepository = LibraryRepositoryImpl(
            libraryDao = database.libraryDao()
        )

        historyRepository = HistoryRepositoryImpl(
            historyDao = database.historyDao()
        )

        downloadRepository = DownloadRepositoryImpl(database.downloadDao())

        val settingsDataStore = SettingsDataStore(context)
        settingsRepository = SettingsRepositoryImpl(settingsDataStore)

        animeRepository = AnimeRepositoryImpl(
            extensionManager = null,
            animeDao = database.animeDao(),
            episodeDao = database.episodeDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== 1. Network Monitor & Offline State ====================

    @Test
    fun testNetworkMonitor_offlineStateTransition(): Unit = runBlocking {
        val monitor = TestNetworkMonitor(initialOnline = true)
        assertTrue(monitor.isCurrentlyOnline)
        assertEquals(true, monitor.isOnline.first())

        monitor.setOnline(false)
        assertFalse(monitor.isCurrentlyOnline)
        assertEquals(false, monitor.isOnline.first())

        monitor.setOnline(true)
        assertTrue(monitor.isCurrentlyOnline)
    }

    @Test
    fun testConnectivityNetworkMonitor_canBeInitialized() {
        val monitor = ConnectivityNetworkMonitor(context)
        val online = monitor.isCurrentlyOnline
        assertNotNull(online)
    }

    // ==================== 2. Offline: Settings ====================

    @Test
    fun testSettings_offlineAccessAndPersistence(): Unit = runBlocking {
        // Can read and update settings locally without any network access
        settingsRepository.setThemeMode(AppThemeMode.DARK)
        settingsRepository.setAutoSkipIntro(true)
        settingsRepository.setSwipeSeek(false)

        val settings = settingsRepository.settings.first()
        assertEquals(AppThemeMode.DARK, settings.themeMode)
        assertTrue(settings.autoSkipIntro)
        assertFalse(settings.gestures.enableSwipeSeek)
    }

    // ==================== 3. Offline: Library ====================

    @Test
    fun testLibrary_offlineAccessAndPersistence(): Unit = runBlocking {
        val testAnime = AnimeEntity(
            localId = "ext_1:anime_101",
            sourceId = "ext_1",
            sourceAnimeId = "anime_101",
            title = "Frieren: Beyond Journey's End",
            poster = "https://example.com/cover.jpg",
            description = "Elven mage journey",
            status = "Completed",
            isFavorite = true
        )
        database.animeDao().insertAnime(testAnime)

        // Add to library
        libraryRepository.addToLibrary("ext_1:anime_101", LibraryStatus.COMPLETED)

        // Read from library
        val libraryItems = libraryRepository.getAllLibraryEntries().first()
        assertEquals(1, libraryItems.size)
        assertEquals("Frieren: Beyond Journey's End", libraryItems[0].anime.title)
        assertEquals(LibraryStatus.COMPLETED, libraryItems[0].status)

        // Filter by status offline
        val completed = libraryRepository.getLibraryEntriesByStatus(LibraryStatus.COMPLETED).first()
        assertEquals(1, completed.size)

        val watching = libraryRepository.getLibraryEntriesByStatus(LibraryStatus.WATCHING).first()
        assertEquals(0, watching.size)
    }

    // ==================== 4. Offline: History & Local Watch Progress ====================

    @Test
    fun testHistoryAndWatchProgress_offlinePersistence(): Unit = runBlocking {
        val anime = AnimeEntity(
            localId = "ext_1:anime_202",
            sourceId = "ext_1",
            sourceAnimeId = "anime_202",
            title = "Jujutsu Kaisen",
            poster = "",
            description = "Sorcery fight",
            status = "Ongoing"
        )
        database.animeDao().insertAnime(anime)

        val episode = EpisodeEntity(
            id = "ep_202_1",
            animeId = "ext_1:anime_202",
            sourceId = "ext_1",
            sourceEpisodeId = "ep_1",
            number = 1f,
            title = "Episode 1",
            lastPositionMs = 120_000L,
            durationMs = 1_440_000L,
            isWatched = false
        )
        database.episodeDao().insertEpisodes(listOf(episode))

        // Record history locally
        val recordResult = historyRepository.recordHistory(
            animeId = "ext_1:anime_202",
            episodeId = "ep_202_1",
            sourceId = "ext_1",
            positionMs = 180_000L,
            durationMs = 1_440_000L
        )
        assertTrue(recordResult is AppResult.Success)

        val historyList = historyRepository.getAllHistory().first()
        assertEquals(1, historyList.size)
        assertEquals("Jujutsu Kaisen", historyList[0].anime.title)
        assertEquals(180_000L, historyList[0].positionMs)

        // Update local episode progress directly
        database.episodeDao().updateEpisodeProgress("ep_202_1", 180_000L, 1_440_000L, false)
        val updatedEp = database.episodeDao().getEpisodeByIdDirect("ep_202_1")
        assertNotNull(updatedEp)
        assertEquals(180_000L, updatedEp?.lastPositionMs)
    }

    // ==================== 5. Offline: Downloaded Episodes ====================

    @Test
    fun testDownloadedEpisodes_offlinePlaybackAccess(): Unit = runBlocking {
        val tempFile = File(context.cacheDir, "test_offline_ep.mp4")
        tempFile.writeText("sample offline video payload")

        val download = DownloadEntity(
            episodeId = "ep_offline_001",
            animeId = "anime_offline_001",
            animeTitle = "Solo Leveling",
            episodeTitle = "Episode 5",
            episodeNumber = 5f,
            thumbnail = "",
            status = DownloadStatus.COMPLETED,
            downloadedBytes = 50_000_000L,
            totalBytes = 50_000_000L,
            localFilePath = tempFile.absolutePath,
            quality = "1080p"
        )
        downloadRepository.saveDownload(download)

        val downloadedList = downloadRepository.getDownloadsByStatus(DownloadStatus.COMPLETED).first()
        assertEquals(1, downloadedList.size)
        assertEquals("Solo Leveling", downloadedList[0].download.animeTitle)
        assertTrue(File(downloadedList[0].download.localFilePath).exists())

        // Direct query
        val direct = downloadRepository.getDownloadDirect("ep_offline_001")
        assertNotNull(direct)
        assertEquals(DownloadStatus.COMPLETED, direct?.status)

        tempFile.delete()
    }

    // ==================== 6. Offline: Cached Anime Metadata Fallback ====================

    @Test
    fun testCachedAnimeMetadata_offlineFallback(): Unit = runBlocking {
        val cachedAnime = AnimeEntity(
            localId = "mirror:anime_cached",
            sourceId = "mirror",
            sourceAnimeId = "anime_cached",
            title = "Demon Slayer",
            poster = "https://example.com/ds.jpg",
            description = "Swordsmith Village Arc",
            status = "Completed"
        )
        database.animeDao().insertAnime(cachedAnime)

        val cachedEp = EpisodeEntity(
            id = "ds_ep_1",
            animeId = "mirror:anime_cached",
            sourceId = "mirror",
            sourceEpisodeId = "ep_1",
            number = 1f,
            title = "Someone's Dream"
        )
        database.episodeDao().insertEpisodes(listOf(cachedEp))

        // When offline / extension is null or failing, animeRepository returns cached Room data
        val detailsResult = animeRepository.getAnimeDetails("mirror", "anime_cached")
        assertTrue(detailsResult is AppResult.Success)
        val anime = (detailsResult as AppResult.Success).data
        assertEquals("Demon Slayer", anime.title)

        val episodesResult = animeRepository.getEpisodes("mirror", "anime_cached")
        assertTrue(episodesResult is AppResult.Success)
        val episodes = (episodesResult as AppResult.Success).data
        assertEquals(1, episodes.size)
        assertEquals("Someone's Dream", episodes[0].title)
    }

    // ==================== 7. Cache Management: Size, Clear Images, Clear Metadata ====================

    @Test
    fun testCacheManager_calculateAndClear(): Unit = runBlocking {
        // Create dummy non-essential anime
        val nonEssentialAnime = AnimeEntity(
            localId = "ext:random_browse_1",
            sourceId = "ext",
            sourceAnimeId = "random_browse_1",
            title = "Unsaved Anime 1",
            poster = "",
            description = "Just browsed",
            status = "Ongoing",
            isFavorite = false
        )
        database.animeDao().insertAnime(nonEssentialAnime)

        val nonEssentialEp = EpisodeEntity(
            id = "random_ep_1",
            animeId = "ext:random_browse_1",
            sourceId = "ext",
            sourceEpisodeId = "ep_1",
            number = 1f,
            title = "Episode 1"
        )
        database.episodeDao().insertEpisodes(listOf(nonEssentialEp))

        // Create essential anime in Library
        val libraryAnime = AnimeEntity(
            localId = "ext:lib_saved_1",
            sourceId = "ext",
            sourceAnimeId = "lib_saved_1",
            title = "Library Anime",
            poster = "",
            description = "In library",
            status = "Ongoing",
            isFavorite = true
        )
        database.animeDao().insertAnime(libraryAnime)
        database.libraryDao().upsertLibraryEntry(
            LibraryEntity(
                animeId = "ext:lib_saved_1",
                status = LibraryStatus.PLANNING
            )
        )

        // Create essential anime in History
        val historyAnime = AnimeEntity(
            localId = "ext:hist_saved_1",
            sourceId = "ext",
            sourceAnimeId = "hist_saved_1",
            title = "History Anime",
            poster = "",
            description = "In history",
            status = "Ongoing"
        )
        database.animeDao().insertAnime(historyAnime)
        val historyEp = EpisodeEntity(
            id = "hist_ep_1",
            animeId = "ext:hist_saved_1",
            sourceId = "ext",
            sourceEpisodeId = "ep_1",
            number = 1f,
            title = "History Ep 1"
        )
        database.episodeDao().insertEpisodes(listOf(historyEp))
        database.historyDao().upsertHistory(
            HistoryEntity(
                animeId = "ext:hist_saved_1",
                episodeId = "hist_ep_1",
                sourceId = "ext",
                positionMs = 5000L,
                durationMs = 24000L,
                watchedAt = System.currentTimeMillis()
            )
        )

        // Calculate cache breakdown
        val breakdownBefore = cacheManager.getCacheBreakdown()
        assertEquals(1, breakdownBefore.metadataAnimeCount)
        assertEquals(1, breakdownBefore.metadataEpisodeCount)

        // Clear images
        val clearImagesResult = cacheManager.clearImages()
        assertTrue(clearImagesResult is AppResult.Success)

        // Clear metadata
        val clearMetaResult = cacheManager.clearMetadata()
        assertTrue(clearMetaResult is AppResult.Success)
        val clearedRecords = (clearMetaResult as AppResult.Success).data
        assertTrue(clearedRecords >= 1)

        // Verify non-essential anime was deleted
        val deletedAnime = database.animeDao().getAnimeByIdDirect("ext:random_browse_1")
        assertEquals(null, deletedAnime)

        // Verify Library and History anime were SAFELY PRESERVED!
        val preservedLib = database.animeDao().getAnimeByIdDirect("ext:lib_saved_1")
        assertNotNull(preservedLib)
        assertEquals("Library Anime", preservedLib?.title)

        val preservedHist = database.animeDao().getAnimeByIdDirect("ext:hist_saved_1")
        assertNotNull(preservedHist)
        assertEquals("History Anime", preservedHist?.title)

        // Verify breakdown after clearing
        val breakdownAfter = cacheManager.getCacheBreakdown()
        assertEquals(0, breakdownAfter.metadataAnimeCount)
        assertEquals(0, breakdownAfter.metadataEpisodeCount)

        // Test clear all cache
        val clearAllResult = cacheManager.clearAllCache()
        assertTrue(clearAllResult is AppResult.Success)
    }
}
