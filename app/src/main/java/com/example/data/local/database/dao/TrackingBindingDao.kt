package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.database.entity.TrackingBindingEntity
import com.example.domain.model.TrackingPlatform
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingBindingDao {
    @Query("SELECT * FROM tracking_bindings WHERE localAnimeId = :localAnimeId")
    fun getBindingsForAnime(localAnimeId: String): Flow<List<TrackingBindingEntity>>

    @Query("SELECT * FROM tracking_bindings WHERE localAnimeId = :localAnimeId AND platform = :platform LIMIT 1")
    fun getBinding(localAnimeId: String, platform: TrackingPlatform): Flow<TrackingBindingEntity?>

    @Query("SELECT * FROM tracking_bindings WHERE localAnimeId = :localAnimeId AND platform = :platform LIMIT 1")
    suspend fun getBindingDirect(localAnimeId: String, platform: TrackingPlatform): TrackingBindingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBinding(binding: TrackingBindingEntity)

    @Update
    suspend fun updateBinding(binding: TrackingBindingEntity)

    @Query("UPDATE tracking_bindings SET currentProgress = :progress, lastSyncedAt = :timestamp WHERE localAnimeId = :localAnimeId AND platform = :platform")
    suspend fun updateProgress(localAnimeId: String, platform: TrackingPlatform, progress: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM tracking_bindings WHERE localAnimeId = :localAnimeId AND platform = :platform")
    suspend fun deleteBinding(localAnimeId: String, platform: TrackingPlatform)

    @Query("DELETE FROM tracking_bindings WHERE localAnimeId = :localAnimeId")
    suspend fun deleteBindingsForAnime(localAnimeId: String)
}
