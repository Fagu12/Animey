package com.example.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.model.DownloadInfo
import com.example.domain.model.DownloadStatus
import com.example.domain.model.StorageUsage
import com.example.domain.repository.DownloadRepository
import com.example.domain.repository.EpisodeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Central Download Manager coordinating WorkManager background execution,
 * download lifecycle (queue, pause, resume, cancel, retry, delete),
 * storage monitoring, and legitimate source protection.
 */
class DownloadManager(
    private val context: Context,
    private val downloadRepository: DownloadRepository,
    private val episodeRepository: EpisodeRepository? = null,
    private val settingsRepository: com.example.domain.repository.SettingsRepository? = null
) {
    private val workManager = WorkManager.getInstance(context)
    private var downloadOverWifiOnly: Boolean = true

    init {
        settingsRepository?.let { repo ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                repo.settings.collect { settings ->
                    downloadOverWifiOnly = settings.downloads.downloadOverWifiOnly
                }
            }
        }
    }

    companion object {
        private const val TAG = "DownloadManager"

        /**
         * Verifies whether a given media URL is legitimately downloadable.
         * Only allows legitimate direct media sources over HTTP/HTTPS.
         * Explicitly rejects DRM streams (Widevine, PlayReady, FairPlay)
         * and embedded iframe / web player pages.
         */
        fun isSourceLegitimatelyDownloadable(url: String): Boolean {
            if (url.isBlank()) return false
            val lower = url.trim().lowercase()

            // Must use legitimate HTTP/HTTPS protocol
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                return false
            }

            // DRM protection indicators: cannot download without unauthorized bypass
            if (lower.contains("widevine") || lower.contains("playready") || lower.contains("fairplay")) {
                return false
            }

            // Web page / embed / iframe players: not legitimate direct video downloads
            if (lower.contains("iframe") || lower.endsWith(".html") || lower.endsWith(".htm") || lower.contains("/embed/")) {
                return false
            }

            // Valid direct or stream URL
            return true
        }

        /**
         * Returns the local storage directory for downloads of a specific anime.
         */
        fun getDownloadsDirectory(context: Context, animeId: String): File {
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            val downloadsDir = File(baseDir, "downloads")
            val animeDir = File(downloadsDir, sanitizeFileName(animeId))
            if (!animeDir.exists()) {
                animeDir.mkdirs()
            }
            return animeDir
        }

        /**
         * Returns the root downloads folder for storage inspection.
         */
        fun getRootDownloadsDirectory(context: Context): File {
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            val downloadsDir = File(baseDir, "downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            return downloadsDir
        }

        private fun sanitizeFileName(name: String): String {
            return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        }
    }

    /**
     * Observe all downloads with real-time details from the database.
     */
    fun getAllDownloads(): Flow<List<DownloadInfo>> {
        return downloadRepository.getAllDownloads().map { list ->
            list.map { item ->
                DownloadInfo(
                    episodeId = item.download.episodeId,
                    animeId = item.download.animeId,
                    sourceId = item.download.sourceId,
                    quality = item.download.quality,
                    localFilePath = item.download.localFilePath,
                    totalBytes = item.download.totalBytes,
                    downloadedBytes = item.download.downloadedBytes,
                    status = item.download.status,
                    error = item.download.error,
                    createdAt = item.download.createdAt,
                    downloadUrl = item.download.downloadUrl,
                    animeTitle = item.anime?.title ?: item.download.animeTitle,
                    episodeTitle = item.episode?.title ?: item.download.episodeTitle,
                    episodeNumber = item.episode?.number ?: item.download.episodeNumber,
                    thumbnail = item.episode?.thumbnail?.takeIf { it.isNotBlank() }
                        ?: item.anime?.poster
                        ?: item.download.thumbnail
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Observe completed, downloaded episodes available for offline playback.
     */
    fun getDownloadedEpisodes(): Flow<List<DownloadInfo>> {
        return downloadRepository.getDownloadedEpisodes().map { list ->
            list.map { item ->
                DownloadInfo(
                    episodeId = item.download.episodeId,
                    animeId = item.download.animeId,
                    sourceId = item.download.sourceId,
                    quality = item.download.quality,
                    localFilePath = item.download.localFilePath,
                    totalBytes = item.download.totalBytes,
                    downloadedBytes = item.download.downloadedBytes,
                    status = item.download.status,
                    error = item.download.error,
                    createdAt = item.download.createdAt,
                    downloadUrl = item.download.downloadUrl,
                    animeTitle = item.anime?.title ?: item.download.animeTitle,
                    episodeTitle = item.episode?.title ?: item.download.episodeTitle,
                    episodeNumber = item.episode?.number ?: item.download.episodeNumber,
                    thumbnail = item.episode?.thumbnail?.takeIf { it.isNotBlank() }
                        ?: item.anime?.poster
                        ?: item.download.thumbnail
                )
            }.filter { info ->
                // Ensure local file actually exists
                info.localFilePath.isNotBlank() && File(info.localFilePath).exists()
            }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Get specific episode download information.
     */
    fun getDownloadForEpisode(episodeId: String): Flow<DownloadInfo?> {
        return downloadRepository.getDownloadForEpisode(episodeId).map { item ->
            item?.let {
                DownloadInfo(
                    episodeId = it.download.episodeId,
                    animeId = it.download.animeId,
                    sourceId = it.download.sourceId,
                    quality = it.download.quality,
                    localFilePath = it.download.localFilePath,
                    totalBytes = it.download.totalBytes,
                    downloadedBytes = it.download.downloadedBytes,
                    status = it.download.status,
                    error = it.download.error,
                    createdAt = it.download.createdAt,
                    downloadUrl = it.download.downloadUrl,
                    animeTitle = it.anime?.title ?: it.download.animeTitle,
                    episodeTitle = it.episode?.title ?: it.download.episodeTitle,
                    episodeNumber = it.episode?.number ?: it.download.episodeNumber,
                    thumbnail = it.episode?.thumbnail?.takeIf { thumb -> thumb.isNotBlank() }
                        ?: it.anime?.poster
                        ?: it.download.thumbnail
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Queue an episode for background download.
     * Enforces legitimate source check.
     */
    suspend fun queueDownload(
        episodeId: String,
        animeId: String,
        sourceUrl: String,
        quality: String = "1080p",
        sourceId: String = "",
        animeTitle: String = "",
        episodeTitle: String = "",
        episodeNumber: Float = 1f,
        thumbnail: String = ""
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (!isSourceLegitimatelyDownloadable(sourceUrl)) {
            val errorMsg = "Cannot download: Source is DRM-protected, restricted, or not legitimately downloadable"
            AppLogger.e(TAG, errorMsg)
            return@withContext AppResult.Error(AppError.NetworkError(errorMsg))
        }

        // Save download entry with QUEUED status
        val queueResult = downloadRepository.queueDownload(
            episodeId = episodeId,
            animeId = animeId,
            sourceId = sourceId,
            quality = quality,
            downloadUrl = sourceUrl,
            animeTitle = animeTitle,
            episodeTitle = episodeTitle,
            episodeNumber = episodeNumber,
            thumbnail = thumbnail
        )

        if (queueResult is AppResult.Error) {
            return@withContext queueResult
        }

        // Schedule WorkManager worker
        scheduleWorker(
            episodeId = episodeId,
            animeId = animeId,
            downloadUrl = sourceUrl,
            quality = quality
        )

        AppResult.Success(Unit)
    }

    /**
     * Pause an ongoing or queued download.
     */
    suspend fun pauseDownload(episodeId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Cancel background worker
            workManager.cancelUniqueWork(DownloadWorker.WORK_NAME_PREFIX + episodeId)
            // Update status to PAUSED (preserves downloaded bytes)
            downloadRepository.updateDownloadStatus(episodeId, DownloadStatus.PAUSED)
            AppLogger.d(TAG, "Paused download for episode $episodeId")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to pause download $episodeId", e)
            AppResult.Error(AppError.UnknownError(e.message ?: "Failed to pause download", e))
        }
    }

    /**
     * Resume a paused or failed download.
     */
    suspend fun resumeDownload(episodeId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        val download = downloadRepository.getDownloadDirect(episodeId)
            ?: return@withContext AppResult.Error(AppError.DatabaseError("Download not found"))

        if (download.downloadUrl.isBlank()) {
            return@withContext AppResult.Error(AppError.NetworkError("No valid download URL found to resume"))
        }

        if (!isSourceLegitimatelyDownloadable(download.downloadUrl)) {
            val errorMsg = "Source is restricted or not legitimately downloadable"
            downloadRepository.updateDownloadStatus(episodeId, DownloadStatus.FAILED, errorMsg)
            return@withContext AppResult.Error(AppError.NetworkError(errorMsg))
        }

        // Set status back to QUEUED
        downloadRepository.updateDownloadStatus(episodeId, DownloadStatus.QUEUED)

        // Reschedule worker
        scheduleWorker(
            episodeId = download.episodeId,
            animeId = download.animeId,
            downloadUrl = download.downloadUrl,
            quality = download.quality
        )

        AppLogger.d(TAG, "Resumed download for episode $episodeId")
        AppResult.Success(Unit)
    }

    /**
     * Retry a failed download.
     */
    suspend fun retryDownload(episodeId: String): AppResult<Unit> {
        return resumeDownload(episodeId)
    }

    /**
     * Cancel an active or queued download.
     */
    suspend fun cancelDownload(episodeId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            workManager.cancelUniqueWork(DownloadWorker.WORK_NAME_PREFIX + episodeId)
            downloadRepository.updateDownloadStatus(episodeId, DownloadStatus.CANCELLED)

            // Clean up any temp file
            val download = downloadRepository.getDownloadDirect(episodeId)
            if (download != null) {
                val animeDir = getDownloadsDirectory(context, download.animeId)
                val tempFile = File(animeDir, "$episodeId.tmp")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }

            AppLogger.d(TAG, "Cancelled download for episode $episodeId")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cancel download $episodeId", e)
            AppResult.Error(AppError.UnknownError(e.message ?: "Failed to cancel download", e))
        }
    }

    /**
     * Delete a download completely from disk and database.
     */
    suspend fun deleteDownload(episodeId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Cancel running work if active
            workManager.cancelUniqueWork(DownloadWorker.WORK_NAME_PREFIX + episodeId)

            val download = downloadRepository.getDownloadDirect(episodeId)
            if (download != null) {
                // Delete local media file
                if (download.localFilePath.isNotBlank()) {
                    val localFile = File(download.localFilePath)
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                }

                // Delete temp file if any
                val animeDir = getDownloadsDirectory(context, download.animeId)
                val tempFile = File(animeDir, "$episodeId.tmp")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                val destFile = File(animeDir, "$episodeId.mp4")
                if (destFile.exists()) {
                    destFile.delete()
                }
            }

            // Remove from database
            downloadRepository.removeDownload(episodeId)

            // Update episode entity isDownloaded status if available
            episodeRepository?.let { repo ->
                repo.getEpisodeByIdDirect(episodeId)?.let { ep ->
                    repo.saveEpisode(ep.copy(isDownloaded = false))
                }
            }

            AppLogger.i(TAG, "Deleted download for episode $episodeId")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete download $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete download", e))
        }
    }

    /**
     * Delete all downloads for an entire anime.
     */
    suspend fun deleteDownloadsForAnime(animeId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val downloads = downloadRepository.getAllDownloadsDirect().filter { it.animeId == animeId }
            for (item in downloads) {
                workManager.cancelUniqueWork(DownloadWorker.WORK_NAME_PREFIX + item.episodeId)
                episodeRepository?.let { repo ->
                    repo.getEpisodeByIdDirect(item.episodeId)?.let { ep ->
                        repo.saveEpisode(ep.copy(isDownloaded = false))
                    }
                }
            }

            // Delete anime directory from storage
            val animeDir = getDownloadsDirectory(context, animeId)
            if (animeDir.exists()) {
                animeDir.deleteRecursively()
            }

            downloadRepository.removeDownloadsForAnime(animeId)
            AppLogger.i(TAG, "Deleted all downloads for anime $animeId")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete downloads for anime $animeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete anime downloads", e))
        }
    }

    /**
     * Delete all downloads in the app.
     */
    suspend fun deleteAllDownloads(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val downloads = downloadRepository.getAllDownloadsDirect()
            for (item in downloads) {
                workManager.cancelUniqueWork(DownloadWorker.WORK_NAME_PREFIX + item.episodeId)
                episodeRepository?.let { repo ->
                    repo.getEpisodeByIdDirect(item.episodeId)?.let { ep ->
                        repo.saveEpisode(ep.copy(isDownloaded = false))
                    }
                }
            }

            val rootDir = getRootDownloadsDirectory(context)
            if (rootDir.exists()) {
                rootDir.deleteRecursively()
            }

            downloadRepository.deleteAllDownloads()
            AppLogger.i(TAG, "Deleted all downloads across app")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete all downloads", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete all downloads", e))
        }
    }

    /**
     * Calculate current storage usage:
     * - usedBytes: Total size of downloaded files in the downloads folder
     * - freeBytes: Usable storage available on device
     * - totalBytes: Total storage on device partition
     */
    suspend fun getStorageUsage(): StorageUsage = withContext(Dispatchers.IO) {
        try {
            val rootDir = getRootDownloadsDirectory(context)
            val usedBytes = if (rootDir.exists()) {
                rootDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else {
                0L
            }

            val storageDir = context.getExternalFilesDir(null) ?: context.filesDir
            val freeBytes = storageDir.usableSpace
            val totalBytes = storageDir.totalSpace

            StorageUsage(
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                totalBytes = totalBytes
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to calculate storage usage", e)
            StorageUsage()
        }
    }

    private fun scheduleWorker(
        episodeId: String,
        animeId: String,
        downloadUrl: String,
        quality: String
    ) {
        val inputData = workDataOf(
            DownloadWorker.KEY_EPISODE_ID to episodeId,
            DownloadWorker.KEY_ANIME_ID to animeId,
            DownloadWorker.KEY_DOWNLOAD_URL to downloadUrl,
            DownloadWorker.KEY_QUALITY to quality
        )

        val networkType = if (downloadOverWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("download")
            .addTag("anime_$animeId")
            .addTag("episode_$episodeId")
            .build()

        workManager.enqueueUniqueWork(
            DownloadWorker.WORK_NAME_PREFIX + episodeId,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
