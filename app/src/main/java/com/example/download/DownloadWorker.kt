package com.example.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.logging.AppLogger
import com.example.data.local.database.AnimeyDatabase
import com.example.data.repository.DownloadRepositoryImpl
import com.example.data.repository.EpisodeRepositoryImpl
import com.example.domain.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Background worker handling episode downloads via WorkManager.
 * Enforces legitimate source checks and does not bypass provider restrictions.
 */
class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_EPISODE_ID = "episode_id"
        const val KEY_ANIME_ID = "anime_id"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_QUALITY = "quality"
        const val KEY_PROGRESS = "progress"
        const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
        const val KEY_TOTAL_BYTES = "total_bytes"

        const val TAG = "DownloadWorker"
        const val WORK_NAME_PREFIX = "download_episode_"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val episodeId = inputData.getString(KEY_EPISODE_ID) ?: return@withContext Result.failure()
        val animeId = inputData.getString(KEY_ANIME_ID) ?: return@withContext Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()

        val db = AnimeyDatabase.getInstance(applicationContext)
        val downloadRepo = DownloadRepositoryImpl(db.downloadDao())
        val episodeRepo = EpisodeRepositoryImpl(db.episodeDao())

        val download = downloadRepo.getDownloadDirect(episodeId)
        if (download == null) {
            AppLogger.w(TAG, "Download record not found for episode $episodeId")
            return@withContext Result.failure()
        }

        if (isStopped) {
            return@withContext Result.retry()
        }

        // Validate legitimate downloadable source
        if (!DownloadManager.isSourceLegitimatelyDownloadable(downloadUrl)) {
            val errorMsg = "Source is DRM-protected, restricted, or not legitimately downloadable"
            AppLogger.e(TAG, "Cannot download episode $episodeId: $errorMsg (URL: $downloadUrl)")
            downloadRepo.updateDownloadStatus(episodeId, DownloadStatus.FAILED, errorMsg)
            return@withContext Result.failure(workDataOf("error" to errorMsg))
        }

        // Update status to DOWNLOADING
        downloadRepo.updateDownloadStatus(episodeId, DownloadStatus.DOWNLOADING)

        val downloadsDir = DownloadManager.getDownloadsDirectory(applicationContext, animeId)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val destinationFile = File(downloadsDir, "$episodeId.mp4")
        val tempFile = File(downloadsDir, "$episodeId.tmp")

        var existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        val requestBuilder = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Animey/1.0 (Android; Mobile)")

        // If resuming and partial bytes exist, request Range
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful && response.code != 206) {
                // Provider restriction or HTTP error: do not attempt to bypass
                val errorMsg = "Server returned HTTP ${response.code} ${response.message}"
                AppLogger.e(TAG, "Download failed for $episodeId: $errorMsg")
                downloadRepo.updateDownloadStatus(episodeId, DownloadStatus.FAILED, errorMsg)
                response.close()
                return@withContext Result.failure(workDataOf("error" to errorMsg))
            }

            val body = response.body
            if (body == null) {
                val errorMsg = "Empty response body from server"
                downloadRepo.updateDownloadStatus(episodeId, DownloadStatus.FAILED, errorMsg)
                return@withContext Result.failure(workDataOf("error" to errorMsg))
            }

            val contentLength = body.contentLength()
            val isPartial = response.code == 206
            val totalBytes = if (isPartial) {
                existingBytes + (if (contentLength > 0) contentLength else 0L)
            } else {
                existingBytes = 0L // restarted from 0
                if (contentLength > 0) contentLength else 0L
            }

            val outputStream = if (isPartial && existingBytes > 0) {
                FileOutputStream(tempFile, true)
            } else {
                FileOutputStream(tempFile, false)
            }

            val inputStream = body.byteStream()
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalDownloaded = existingBytes
            var lastUpdateMs = System.currentTimeMillis()

            try {
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        response.close()

                        val currentStatus = downloadRepo.getDownloadDirect(episodeId)?.status
                        return@withContext if (currentStatus == DownloadStatus.PAUSED) {
                            AppLogger.d(TAG, "Download worker paused for $episodeId at $totalDownloaded bytes")
                            Result.success()
                        } else {
                            AppLogger.d(TAG, "Download worker stopped for $episodeId")
                            Result.retry()
                        }
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    totalDownloaded += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUpdateMs > 500 || totalDownloaded == totalBytes) {
                        lastUpdateMs = now
                        val progressPercent = if (totalBytes > 0) {
                            (totalDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        setProgress(
                            workDataOf(
                                KEY_PROGRESS to progressPercent,
                                KEY_DOWNLOADED_BYTES to totalDownloaded,
                                KEY_TOTAL_BYTES to totalBytes
                            )
                        )
                        downloadRepo.updateDownloadProgress(
                            episodeId = episodeId,
                            downloadedBytes = totalDownloaded,
                            totalBytes = totalBytes,
                            status = DownloadStatus.DOWNLOADING
                        )
                    }
                }

                outputStream.flush()
            } finally {
                outputStream.close()
                inputStream.close()
                response.close()
            }

            // Move temp file to final destination
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            if (!tempFile.renameTo(destinationFile)) {
                tempFile.copyTo(destinationFile, overwrite = true)
                tempFile.delete()
            }

            val finalSize = destinationFile.length()
            downloadRepo.markDownloadCompleted(
                episodeId = episodeId,
                localFilePath = destinationFile.absolutePath,
                totalBytes = finalSize
            )

            // Update episode entity if it exists
            episodeRepo.getEpisodeByIdDirect(episodeId)?.let { ep ->
                episodeRepo.saveEpisode(ep.copy(isDownloaded = true))
            }

            AppLogger.i(TAG, "Download finished successfully for episode $episodeId ($finalSize bytes)")
            Result.success()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception during download for episode $episodeId", e)
            if (isStopped) {
                Result.retry()
            } else {
                downloadRepo.updateDownloadStatus(episodeId, DownloadStatus.FAILED, e.message ?: "Download failed")
                Result.failure(workDataOf("error" to (e.message ?: "Download failed")))
            }
        }
    }
}
