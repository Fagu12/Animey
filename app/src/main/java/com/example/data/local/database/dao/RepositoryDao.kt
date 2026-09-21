package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.database.entity.RepositoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepositoryDao {
    @Query("SELECT * FROM extension_repositories ORDER BY name ASC")
    fun getAllRepositories(): Flow<List<RepositoryEntity>>

    @Query("SELECT * FROM extension_repositories WHERE isEnabled = 1 ORDER BY name ASC")
    fun getEnabledRepositories(): Flow<List<RepositoryEntity>>

    @Query("SELECT * FROM extension_repositories WHERE id = :id LIMIT 1")
    fun getRepositoryById(id: String): Flow<RepositoryEntity?>

    @Query("SELECT * FROM extension_repositories WHERE id = :id LIMIT 1")
    suspend fun getRepositoryByIdDirect(id: String): RepositoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repo: RepositoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepositories(repos: List<RepositoryEntity>)

    @Update
    suspend fun updateRepository(repo: RepositoryEntity)

    @Query("UPDATE extension_repositories SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setRepositoryEnabled(id: String, isEnabled: Boolean)

    @Query("UPDATE extension_repositories SET extensionCount = :count, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateSyncInfo(id: String, count: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM extension_repositories WHERE id = :id")
    suspend fun deleteRepository(id: String)
}
