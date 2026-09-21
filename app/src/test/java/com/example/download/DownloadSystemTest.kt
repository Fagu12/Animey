package com.example.download

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.core.result.AppResult
import com.example.data.local.database.AnimeyDatabase
import com.example.data.local.database.entity.DownloadEntity
import com.example.data.repository.DownloadRepositoryImpl
import com.example.domain.model.DownloadInfo
import com.example.domain.model.DownloadStatus
import com.example.domain.model.StorageUsage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DownloadSystemTest {

    private lateinit var context: Context
    private lateinit var database: AnimeyDatabase
    private lateinit var downloadRepository: DownloadRepositoryImpl
    private lateinit var downloadManager: DownloadManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        database = Room.inMemoryDatabaseBuilder(context, AnimeyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        downloadRepository = DownloadRepositoryImpl(database.downloadDao())
        downloadManager = DownloadManager(
            context = context,
            downloadRepository = downloadRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `legitimate source validation enforces policy and rejects DRM or embed streams`() {
        // Legitimate direct streams
        assertTrue(DownloadManager.isSourceLegitimatelyDownloadable("https://cdn.example.com/videos/ep1.mp4"))
        assertTrue(DownloadManager.isSourceLegitimatelyDownloadable("http://storage.example.org/stream/hls/ep1.m3u8"))

        // Blank or invalid protocols
        assertFalse(DownloadManager.isSourceLegitimatelyDownloadable(""))
        assertFalse(DownloadManager.isSourceLegitimatelyDownloadable("   "))
        assertFalse(DownloadManager.isSourceLegitimatelyDownloadable("ftp://example.com/video.mp4"))
        assertFalse(DownloadManager.isSourceLegitimatelyDownloadable("rtmp://example.com/live"))

        // Protected DRM sources: strictly forbidden to bypass
        assertFalse(DownloadManager.isSourceLegitimatelyDownloadable("https://drm.example.com/stream.mpd?drm=widevine"))
        assertFalse(DownloadManager.isSourceLegitimatelyDownloadable("https://playready.license.example.com/stream"))
        assertFalse(DownloadManager.isSourceLegitimatelyDownloadable("https://fairplay.video.example.com/asset.m3u8"))

        // Web embed / iframe wrappers: not legitimate media files
        assertFalse(DownloadManager.isSourceLegitimatelyDownloadable("https://player.example.com/embed/12345"))
        assertFalse(DownloadManager.isSourceLegitimatelyDownloadable("https://embed.provider.org/iframe/video.html"))
        assertFalse(DownloadManager.isSourceLegitimatelyDownloadable("https://stream.provider.to/watch.htm"))
    }

    @Test
    fun `storage usage formatBytes formats values correctly`() {
        assertEquals("0 B", StorageUsage.formatBytes(0L))
        assertEquals("0 B", StorageUsage.formatBytes(-100L))
        assertEquals("512.0 B", StorageUsage.formatBytes(512L))
        assertEquals("1.0 KB", StorageUsage.formatBytes(1024L))
        assertEquals("1.5 MB", StorageUsage.formatBytes(1572864L))
        assertEquals("2.0 GB", StorageUsage.formatBytes(2147483648L))

        val storage = StorageUsage(
            usedBytes = 1048576L * 500,
            freeBytes = 1048576L * 1500,
            totalBytes = 1048576L * 2000
        )
        assertEquals(0.25f, storage.usagePercent, 0.001f)
    }

    @Test
    fun `download info state and progress helpers evaluate correctly`() {
        val queued = DownloadInfo(
            episodeId = "ep_1",
            animeId = "anime_1",
            sourceId = "src_1",
            status = DownloadStatus.QUEUED
        )
        assertEquals(0f, queued.progressPercent, 0.001f)
        assertFalse(queued.isFinished)
        assertFalse(queued.isDownloading)
        assertFalse(queued.isPaused)
        assertFalse(queued.isCompleted)

        val downloading = DownloadInfo(
            episodeId = "ep_1",
            animeId = "anime_1",
            sourceId = "src_1",
            totalBytes = 1000L,
            downloadedBytes = 500L,
            status = DownloadStatus.DOWNLOADING
        )
        assertEquals(0.5f, downloading.progressPercent, 0.001f)
        assertTrue(downloading.isDownloading)
        assertFalse(downloading.isCompleted)

        val completed = downloading.copy(
            downloadedBytes = 1000L,
            status = DownloadStatus.COMPLETED
        )
        assertEquals(1.0f, completed.progressPercent, 0.001f)
        assertTrue(completed.isCompleted)
        assertTrue(completed.isFinished)
    }

    @Test
    fun `queue legitimate download schedules WorkManager task and stores record`() {
        runBlocking {
            val result = downloadManager.queueDownload(
                episodeId = "ep_101",
                animeId = "anime_frieren",
                sourceUrl = "https://cdn.animey.test/frieren/ep1.mp4",
                quality = "1080p",
                animeTitle = "Frieren",
                episodeTitle = "The End of the Journey",
                episodeNumber = 1f
            )

            assertTrue(result is AppResult.Success)

            // Verify database entry was created with QUEUED status
            val stored = downloadRepository.getDownloadDirect("ep_101")
            assertNotNull(stored)
            assertEquals("ep_101", stored?.episodeId)
            assertEquals("anime_frieren", stored?.animeId)
            assertEquals(DownloadStatus.QUEUED, stored?.status)
            assertEquals("Frieren", stored?.animeTitle)
            assertEquals(1f, stored?.episodeNumber)

            // Verify WorkManager scheduled the unique work
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(DownloadWorker.WORK_NAME_PREFIX + "ep_101")
                .get()
            assertFalse(workInfos.isEmpty())
            val workInfo = workInfos.first()
            assertTrue(
                workInfo.state == WorkInfo.State.ENQUEUED ||
                    workInfo.state == WorkInfo.State.RUNNING ||
                    workInfo.state == WorkInfo.State.BLOCKED
            )
        }
    }

    @Test
    fun `queue protected DRM source fails immediately without scheduling work`() {
        runBlocking {
            val result = downloadManager.queueDownload(
                episodeId = "ep_drm",
                animeId = "anime_drm",
                sourceUrl = "https://restricted.provider.com/manifest.mpd?drm=widevine"
            )

            assertTrue(result is AppResult.Error)

            // DB record should not be stored
            val stored = downloadRepository.getDownloadDirect("ep_drm")
            assertNull(stored)
        }
    }

    @Test
    fun `pause, resume, cancel, and delete state transitions update correctly`() {
        runBlocking {
            // 1. Queue download
            downloadManager.queueDownload(
                episodeId = "ep_transition",
                animeId = "anime_test",
                sourceUrl = "https://cdn.animey.test/ep.mp4",
                quality = "720p"
            )
            assertEquals(DownloadStatus.QUEUED, downloadRepository.getDownloadDirect("ep_transition")?.status)

            // 2. Pause
            downloadManager.pauseDownload("ep_transition")
            assertEquals(DownloadStatus.PAUSED, downloadRepository.getDownloadDirect("ep_transition")?.status)

            // 3. Resume
            downloadManager.resumeDownload("ep_transition")
            assertEquals(DownloadStatus.QUEUED, downloadRepository.getDownloadDirect("ep_transition")?.status)

            // 4. Cancel
            downloadManager.cancelDownload("ep_transition")
            assertEquals(DownloadStatus.CANCELLED, downloadRepository.getDownloadDirect("ep_transition")?.status)

            // 5. Delete
            downloadManager.deleteDownload("ep_transition")
            assertNull(downloadRepository.getDownloadDirect("ep_transition"))
        }
    }

    @Test
    fun `download completion and downloaded episodes filtering`() {
        runBlocking {
            val fakeFile = File(context.cacheDir, "test_ep_completed.mp4")
            fakeFile.writeBytes(ByteArray(1024) { 1 })

            downloadRepository.saveDownload(
                DownloadEntity(
                    episodeId = "ep_done",
                    animeId = "anime_1",
                    quality = "1080p",
                    localFilePath = fakeFile.absolutePath,
                    totalBytes = fakeFile.length(),
                    downloadedBytes = fakeFile.length(),
                    status = DownloadStatus.COMPLETED,
                    animeTitle = "Steins;Gate",
                    episodeTitle = "Prologue",
                    episodeNumber = 1f
                )
            )

            val downloadedList = downloadManager.getDownloadedEpisodes().first()
            assertEquals(1, downloadedList.size)
            assertEquals("ep_done", downloadedList.first().episodeId)
            assertEquals("Steins;Gate", downloadedList.first().animeTitle)
            assertTrue(downloadedList.first().isCompleted)

            fakeFile.delete()
        }
    }
}
