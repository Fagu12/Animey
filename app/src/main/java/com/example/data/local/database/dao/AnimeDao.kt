package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.database.entity.AnimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
    @Query("SELECT * FROM anime WHERE localId = :localId LIMIT 1")
    fun getAnimeById(localId: String): Flow<AnimeEntity?>

    @Query("SELECT * FROM anime WHERE localId = :localId LIMIT 1")
    suspend fun getAnimeByIdDirect(localId: String): AnimeEntity?

    @Query("SELECT * FROM anime WHERE sourceId = :sourceId AND sourceAnimeId = :sourceAnimeId LIMIT 1")
    suspend fun getAnimeBySourceId(sourceId: String, sourceAnimeId: String): AnimeEntity?

    @Query("SELECT * FROM anime ORDER BY lastUpdated DESC")
    fun getAllAnime(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE isFavorite = 1 ORDER BY lastUpdated DESC")
    fun getFavoriteAnime(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE title LIKE '%' || :query || '%' ORDER BY lastUpdated DESC")
    fun searchLocalAnime(query: String): Flow<List<AnimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnime(anime: AnimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeList(animeList: List<AnimeEntity>)

    @Update
    suspend fun updateAnime(anime: AnimeEntity)

    @Query("UPDATE anime SET isFavorite = :isFavorite WHERE localId = :localId")
    suspend fun updateFavoriteStatus(localId: String, isFavorite: Boolean)

    @Query("DELETE FROM anime WHERE localId = :localId")
    suspend fun deleteAnimeById(localId: String)

    @Query("""
        SELECT COUNT(*) FROM anime 
        WHERE localId NOT IN (SELECT animeId FROM library) 
          AND localId NOT IN (SELECT animeId FROM history)
          AND localId NOT IN (SELECT animeId FROM downloads)
          AND isFavorite = 0
    """)
    suspend fun getNonEssentialAnimeCount(): Int

    @Query("""
        DELETE FROM anime 
        WHERE localId NOT IN (SELECT animeId FROM library) 
          AND localId NOT IN (SELECT animeId FROM history)
          AND localId NOT IN (SELECT animeId FROM downloads)
          AND isFavorite = 0
    """)
    suspend fun deleteNonEssentialAnime(): Int
}
