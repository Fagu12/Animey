package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.database.entity.AnimeEntity
import com.example.data.local.database.entity.DownloadEntity
import com.example.data.local.database.entity.EpisodeEntity
import com.example.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

data class DownloadWithDetails(
    @Embedded
    val download: DownloadEntity,
    @Relation(
        parentColumn = "animeId",
        entityColumn = "localId"
    )
    val anime: AnimeEntity?,
    @Relation(
        parentColumn = "episodeId",
        entityColumn = "id"
    )
    val episode: EpisodeEntity?
)

@Dao
interface DownloadDao {
    @Transaction
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadWithDetails>>

    @Transaction
    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt ASC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadWithDetails>>

    @Transaction
    @Query("SELECT * FROM downloads WHERE animeId = :animeId ORDER BY createdAt DESC")
    fun getDownloadsForAnime(animeId: String): Flow<List<DownloadWithDetails>>

    @Transaction
    @Query("SELECT * FROM downloads WHERE episodeId = :episodeId LIMIT 1")
    fun getDownloadForEpisode(episodeId: String): Flow<DownloadWithDetails?>

    @Transaction
    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY createdAt DESC")
    fun getDownloadedEpisodes(): Flow<List<DownloadWithDetails>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY createdAt DESC")
    suspend fun getDownloadedEpisodesDirect(): List<DownloadEntity>

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    suspend fun getAllDownloadsDirect(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getDownloadDirect(episodeId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, status = :status WHERE episodeId = :episodeId")
    suspend fun updateProgress(episodeId: String, downloadedBytes: Long, totalBytes: Long, status: DownloadStatus)

    @Query("UPDATE downloads SET status = :status, error = :error WHERE episodeId = :episodeId")
    suspend fun updateStatus(episodeId: String, status: DownloadStatus, error: String? = null)

    @Query("UPDATE downloads SET localFilePath = :localFilePath, totalBytes = :totalBytes, downloadedBytes = :totalBytes, status = :status, error = null WHERE episodeId = :episodeId")
    suspend fun updateCompleted(
        episodeId: String,
        localFilePath: String,
        totalBytes: Long,
        status: DownloadStatus = DownloadStatus.COMPLETED
    )

    @Query("SELECT SUM(downloadedBytes) FROM downloads WHERE status = 'COMPLETED'")
    fun getTotalDownloadedBytes(): Flow<Long?>

    @Query("DELETE FROM downloads WHERE episodeId = :episodeId")
    suspend fun deleteDownload(episodeId: String)

    @Query("DELETE FROM downloads WHERE animeId = :animeId")
    suspend fun deleteDownloadsForAnime(animeId: String)

    @Query("DELETE FROM downloads")
    suspend fun deleteAllDownloads()
}
