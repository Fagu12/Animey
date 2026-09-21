package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.database.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    fun getEpisodeById(episodeId: String): Flow<EpisodeEntity?>

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    suspend fun getEpisodeByIdDirect(episodeId: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE animeId = :animeId ORDER BY seasonNumber ASC, number ASC")
    fun getEpisodesForAnime(animeId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE animeId = :animeId ORDER BY seasonNumber ASC, number ASC")
    suspend fun getEpisodesForAnimeDirect(animeId: String): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE animeId = :animeId AND seasonNumber = :seasonNumber ORDER BY number ASC")
    fun getEpisodesForSeason(animeId: String, seasonNumber: Int): Flow<List<EpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: EpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Update
    suspend fun updateEpisode(episode: EpisodeEntity)

    @Query("UPDATE episodes SET lastPositionMs = :positionMs, durationMs = :durationMs, isWatched = :isWatched WHERE id = :episodeId")
    suspend fun updateEpisodeProgress(episodeId: String, positionMs: Long, durationMs: Long, isWatched: Boolean)

    @Query("UPDATE episodes SET isWatched = :isWatched WHERE id = :episodeId")
    suspend fun markEpisodeWatched(episodeId: String, isWatched: Boolean)

    @Query("UPDATE episodes SET isWatched = :isWatched WHERE animeId = :animeId AND number <= :upToNumber")
    suspend fun markEpisodesWatchedUpTo(animeId: String, upToNumber: Float, isWatched: Boolean)

    @Query("DELETE FROM episodes WHERE id = :episodeId")
    suspend fun deleteEpisodeById(episodeId: String)

    @Query("DELETE FROM episodes WHERE animeId = :animeId")
    suspend fun deleteEpisodesForAnime(animeId: String)

    @Query("""
        SELECT COUNT(*) FROM episodes 
        WHERE animeId NOT IN (SELECT animeId FROM library) 
          AND animeId NOT IN (SELECT animeId FROM history)
          AND id NOT IN (SELECT episodeId FROM downloads)
    """)
    suspend fun getNonEssentialEpisodeCount(): Int

    @Query("""
        DELETE FROM episodes 
        WHERE animeId NOT IN (SELECT animeId FROM library) 
          AND animeId NOT IN (SELECT animeId FROM history)
          AND id NOT IN (SELECT episodeId FROM downloads)
    """)
    suspend fun deleteNonEssentialEpisodes(): Int
}
