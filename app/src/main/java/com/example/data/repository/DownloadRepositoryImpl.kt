package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.local.database.dao.DownloadDao
import com.example.data.local.database.dao.DownloadWithDetails
import com.example.data.local.database.entity.DownloadEntity
import com.example.domain.model.DownloadStatus
import com.example.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class DownloadRepositoryImpl(
    private val downloadDao: DownloadDao
) : DownloadRepository {

    override fun getAllDownloads(): Flow<List<DownloadWithDetails>> {
        return downloadDao.getAllDownloads()
    }

    override fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadWithDetails>> {
        return downloadDao.getDownloadsByStatus(status)
    }

    override fun getDownloadsForAnime(animeId: String): Flow<List<DownloadWithDetails>> {
        return downloadDao.getDownloadsForAnime(animeId)
    }

    override fun getDownloadForEpisode(episodeId: String): Flow<DownloadWithDetails?> {
        return downloadDao.getDownloadForEpisode(episodeId)
    }

    override suspend fun getDownloadDirect(episodeId: String): DownloadEntity? {
        return downloadDao.getDownloadDirect(episodeId)
    }

    override fun getDownloadedEpisodes(): Flow<List<DownloadWithDetails>> {
        return downloadDao.getDownloadedEpisodes()
    }

    override suspend fun getDownloadedEpisodesDirect(): List<DownloadEntity> {
        return downloadDao.getDownloadedEpisodesDirect()
    }

    override suspend fun getAllDownloadsDirect(): List<DownloadEntity> {
        return downloadDao.getAllDownloadsDirect()
    }

    override fun getTotalDownloadedBytes(): Flow<Long?> {
        return downloadDao.getTotalDownloadedBytes()
    }

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
    ): AppResult<Unit> {
        return try {
            val existing = downloadDao.getDownloadDirect(episodeId)
            val entity = (existing ?: DownloadEntity(
                episodeId = episodeId,
                animeId = animeId
            )).copy(
                animeId = animeId,
                sourceId = sourceId.ifBlank { existing?.sourceId ?: "" },
                quality = quality.ifBlank { existing?.quality ?: "1080p" },
                downloadUrl = downloadUrl.ifBlank { existing?.downloadUrl ?: "" },
                animeTitle = animeTitle.ifBlank { existing?.animeTitle ?: "" },
                episodeTitle = episodeTitle.ifBlank { existing?.episodeTitle ?: "" },
                episodeNumber = if (episodeNumber > 0) episodeNumber else (existing?.episodeNumber ?: 1f),
                thumbnail = thumbnail.ifBlank { existing?.thumbnail ?: "" },
                status = DownloadStatus.QUEUED,
                error = null,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            downloadDao.upsertDownload(entity)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("DownloadRepo", "Failed to queue download for $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to queue download", e))
        }
    }

    override suspend fun saveDownload(entity: DownloadEntity): AppResult<Unit> {
        return try {
            downloadDao.upsertDownload(entity)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("DownloadRepo", "Failed to save download for ${entity.episodeId}", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to save download", e))
        }
    }

    override suspend fun updateDownloadProgress(
        episodeId: String,
        downloadedBytes: Long,
        totalBytes: Long,
        status: DownloadStatus
    ): AppResult<Unit> {
        return try {
            downloadDao.updateProgress(episodeId, downloadedBytes, totalBytes, status)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("DownloadRepo", "Failed to update download progress for $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update progress", e))
        }
    }

    override suspend fun updateDownloadStatus(
        episodeId: String,
        status: DownloadStatus,
        error: String?
    ): AppResult<Unit> {
        return try {
            downloadDao.updateStatus(episodeId, status, error)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("DownloadRepo", "Failed to update download status for $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update status", e))
        }
    }

    override suspend fun markDownloadCompleted(
        episodeId: String,
        localFilePath: String,
        totalBytes: Long
    ): AppResult<Unit> {
        return try {
            downloadDao.updateCompleted(
                episodeId = episodeId,
                localFilePath = localFilePath,
                totalBytes = totalBytes,
                status = DownloadStatus.COMPLETED
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("DownloadRepo", "Failed to mark download completed for $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to mark completed", e))
        }
    }

    override suspend fun removeDownload(episodeId: String): AppResult<Unit> {
        return try {
            downloadDao.deleteDownload(episodeId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("DownloadRepo", "Failed to remove download $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to remove download", e))
        }
    }

    override suspend fun removeDownloadsForAnime(animeId: String): AppResult<Unit> {
        return try {
            downloadDao.deleteDownloadsForAnime(animeId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("DownloadRepo", "Failed to remove downloads for anime $animeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to remove anime downloads", e))
        }
    }

    override suspend fun deleteAllDownloads(): AppResult<Unit> {
        return try {
            downloadDao.deleteAllDownloads()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("DownloadRepo", "Failed to delete all downloads", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete all downloads", e))
        }
    }
}
