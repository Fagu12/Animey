package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.data.local.database.entity.AnimeEntity
import com.example.data.local.database.entity.EpisodeEntity
import com.example.data.local.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

data class HistoryWithDetails(
    @Embedded
    val history: HistoryEntity,
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
interface HistoryDao {
    @Transaction
    @Query("SELECT * FROM history ORDER BY watchedAt DESC")
    fun getAllHistory(): Flow<List<HistoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM history WHERE animeId = :animeId ORDER BY watchedAt DESC LIMIT 1")
    fun getLastWatchedForAnime(animeId: String): Flow<HistoryWithDetails?>

    @Query("SELECT * FROM history WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getHistoryForEpisode(episodeId: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE episodeId = :episodeId")
    suspend fun deleteHistoryByEpisodeId(episodeId: String)

    @Query("DELETE FROM history WHERE animeId = :animeId")
    suspend fun deleteHistoryForAnime(animeId: String)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}
