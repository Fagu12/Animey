package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.data.local.database.dao.DownloadWithDetails
import com.example.data.local.database.entity.DownloadEntity
import com.example.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadWithDetails>>
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadWithDetails>>
    fun getDownloadsForAnime(animeId: String): Flow<List<DownloadWithDetails>>
    fun getDownloadForEpisode(episodeId: String): Flow<DownloadWithDetails?>
    suspend fun getDownloadDirect(episodeId: String): DownloadEntity?
    fun getDownloadedEpisodes(): Flow<List<DownloadWithDetails>>
    suspend fun getDownloadedEpisodesDirect(): List<DownloadEntity>
    suspend fun getAllDownloadsDirect(): List<DownloadEntity>
    fun getTotalDownloadedBytes(): Flow<Long?>

    suspend fun queueDownload(
        episodeId: String,
        animeId: String,
        sourceId: String,
        quality: String = "1080p",
        downloadUrl: String = "",
        animeTitle: String = "",
        episodeTitle: String = "",
        episodeNumber: Float = 1f,
        thumbnail: String = ""
    ): AppResult<Unit>

    suspend fun saveDownload(entity: DownloadEntity): AppResult<Unit>
    suspend fun updateDownloadProgress(episodeId: String, downloadedBytes: Long, totalBytes: Long, status: DownloadStatus): AppResult<Unit>
    suspend fun updateDownloadStatus(episodeId: String, status: DownloadStatus, error: String? = null): AppResult<Unit>
    suspend fun markDownloadCompleted(episodeId: String, localFilePath: String, totalBytes: Long): AppResult<Unit>
    suspend fun removeDownload(episodeId: String): AppResult<Unit>
    suspend fun removeDownloadsForAnime(animeId: String): AppResult<Unit>
    suspend fun deleteAllDownloads(): AppResult<Unit>
}
