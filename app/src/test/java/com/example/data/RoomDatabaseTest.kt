package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.AnimeyDatabase
import com.example.data.local.database.entity.AnimeEntity
import com.example.data.local.database.entity.EpisodeEntity
import com.example.data.local.database.entity.ExtensionEntity
import com.example.data.local.database.entity.HistoryEntity
import com.example.data.local.database.entity.LibraryEntity
import com.example.data.local.database.entity.RepositoryEntity
import com.example.data.local.database.entity.TrackingBindingEntity
import com.example.data.repository.AnimeRepositoryImpl
import com.example.data.repository.HistoryRepositoryImpl
import com.example.data.repository.LibraryRepositoryImpl
import com.example.domain.model.Anime
import com.example.domain.model.LibraryStatus
import com.example.domain.model.TrackingPlatform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {

    private lateinit var db: AnimeyDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AnimeyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveAnime() = runBlocking {
        val animeDao = db.animeDao()
        val anime = AnimeEntity(
            localId = "test-1",
            sourceId = "faststream",
            sourceAnimeId = "frieren",
            title = "Frieren",
            rating = 9.2
        )
        animeDao.insertAnime(anime)

        val retrieved = animeDao.getAnimeByIdDirect("test-1")
        assertNotNull(retrieved)
        assertEquals("Frieren", retrieved?.title)
        assertEquals(9.2, retrieved?.rating)
    }

    @Test
    fun animeRepositoryRoundTrip() = runBlocking {
        val repo = AnimeRepositoryImpl(db.animeDao())
        val anime = Anime(
            localId = "anime-123",
            sourceId = "faststream",
            sourceAnimeId = "jujutsu-kaisen",
            title = "Jujutsu Kaisen",
            rating = 8.8
        )
        repo.saveAnime(anime)

        val retrieved = repo.getAnimeByIdDirect("anime-123")
        assertNotNull(retrieved)
        assertEquals("Jujutsu Kaisen", retrieved?.title)

        val all = repo.getAllAnime().first()
        assertEquals(1, all.size)
        assertEquals("Jujutsu Kaisen", all[0].title)
    }

    @Test
    fun libraryRepositoryAndEntityTest() = runBlocking {
        val animeDao = db.animeDao()
        val anime = AnimeEntity(
            localId = "lib-anime",
            sourceId = "src1",
            sourceAnimeId = "srcA1",
            title = "Solo Leveling"
        )
        animeDao.insertAnime(anime)

        val libRepo = LibraryRepositoryImpl(db.libraryDao())
        libRepo.addToLibrary(
            animeId = "lib-anime",
            status = LibraryStatus.WATCHING,
            score = 9.0
        )

        val entries = libRepo.getLibraryEntriesByStatus(LibraryStatus.WATCHING).first()
        assertEquals(1, entries.size)
        assertEquals("Solo Leveling", entries[0].anime.title)
        assertEquals(LibraryStatus.WATCHING, entries[0].status)
    }

    @Test
    fun historyRepositoryTest() = runBlocking {
        val animeDao = db.animeDao()
        animeDao.insertAnime(AnimeEntity(localId = "hist-anime", sourceId = "src1", sourceAnimeId = "a1", title = "Demon Slayer"))

        val episodeDao = db.episodeDao()
        episodeDao.insertEpisode(
            EpisodeEntity(
                id = "hist-ep-1",
                animeId = "hist-anime",
                sourceId = "src1",
                sourceEpisodeId = "ep1",
                number = 1f,
                title = "Cruelty"
            )
        )

        val historyRepo = HistoryRepositoryImpl(db.historyDao())
        historyRepo.recordHistory(
            animeId = "hist-anime",
            episodeId = "hist-ep-1",
            sourceId = "src1",
            positionMs = 600000L,
            durationMs = 1440000L
        )

        val historyList = historyRepo.getAllHistory().first()
        assertEquals(1, historyList.size)
        assertEquals("Demon Slayer", historyList[0].anime.title)
        assertEquals("Cruelty", historyList[0].episode.title)
        assertEquals(600000L, historyList[0].positionMs)
    }

    @Test
    fun extensionAndRepositoryDaoTest() = runBlocking {
        val extDao = db.extensionDao()
        extDao.insertExtension(
            ExtensionEntity(
                id = "ext.test",
                name = "Test Provider",
                version = "1.0.0",
                language = "en"
            )
        )
        val ext = extDao.getExtensionByIdDirect("ext.test")
        assertNotNull(ext)
        assertEquals("Test Provider", ext?.name)

        val repoDao = db.repositoryDao()
        repoDao.insertRepository(
            RepositoryEntity(
                id = "repo.main",
                name = "Main Repo",
                url = "https://example.com/index.json",
                extensionCount = 10
            )
        )
        val repo = repoDao.getRepositoryByIdDirect("repo.main")
        assertNotNull(repo)
        assertEquals(10, repo?.extensionCount)
    }

    @Test
    fun trackingBindingDaoTest() = runBlocking {
        val animeDao = db.animeDao()
        animeDao.insertAnime(AnimeEntity(localId = "track-anime", sourceId = "src1", sourceAnimeId = "a1", title = "Vinland Saga"))

        val trackDao = db.trackingBindingDao()
        trackDao.upsertBinding(
            TrackingBindingEntity(
                localAnimeId = "track-anime",
                sourceId = "src1",
                sourceAnimeId = "a1",
                platform = TrackingPlatform.ANILIST,
                aniListId = 101348,
                season = 1,
                mappingConfidence = 1.0f,
                currentProgress = 12
            )
        )

        val binding = trackDao.getBindingDirect("track-anime", TrackingPlatform.ANILIST)
        assertNotNull(binding)
        assertEquals(101348, binding?.aniListId)
        assertEquals(12, binding?.currentProgress)
    }
}
