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
import com.example.data.local.database.entity.LibraryEntity
import com.example.domain.model.LibraryStatus
import kotlinx.coroutines.flow.Flow

data class LibraryWithAnime(
    @Embedded
    val library: LibraryEntity,
    @Relation(
        parentColumn = "animeId",
        entityColumn = "localId"
    )
    val anime: AnimeEntity?
)

@Dao
interface LibraryDao {
    @Transaction
    @Query("SELECT * FROM library ORDER BY updatedAt DESC")
    fun getAllLibraryEntries(): Flow<List<LibraryWithAnime>>

    @Transaction
    @Query("SELECT * FROM library WHERE status = :status ORDER BY updatedAt DESC")
    fun getLibraryEntriesByStatus(status: LibraryStatus): Flow<List<LibraryWithAnime>>

    @Transaction
    @Query("SELECT * FROM library WHERE animeId = :animeId LIMIT 1")
    fun getLibraryEntryForAnime(animeId: String): Flow<LibraryWithAnime?>

    @Query("SELECT * FROM library WHERE animeId = :animeId LIMIT 1")
    suspend fun getLibraryEntryDirect(animeId: String): LibraryEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE animeId = :animeId)")
    fun isAnimeInLibrary(animeId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLibraryEntry(entry: LibraryEntity)

    @Update
    suspend fun updateLibraryEntry(entry: LibraryEntity)

    @Query("UPDATE library SET status = :status, updatedAt = :updatedAt WHERE animeId = :animeId")
    suspend fun updateStatus(animeId: String, status: LibraryStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE library SET totalEpisodesWatched = :count, updatedAt = :updatedAt WHERE animeId = :animeId")
    suspend fun updateProgress(animeId: String, count: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM library WHERE animeId = :animeId")
    suspend fun deleteLibraryEntry(animeId: String)
}
