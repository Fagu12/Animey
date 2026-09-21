package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.local.database.dao.TrackingBindingDao
import com.example.data.local.database.entity.TrackingBindingEntity
import com.example.domain.model.TrackingBinding
import com.example.domain.model.TrackingPlatform
import com.example.domain.repository.TrackingBindingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackingBindingRepositoryImpl(
    private val trackingBindingDao: TrackingBindingDao
) : TrackingBindingRepository {

    override fun getBindingsForAnime(localAnimeId: String): Flow<List<TrackingBinding>> {
        return trackingBindingDao.getBindingsForAnime(localAnimeId).map { list -> list.map { it.toDomain() } }
    }

    override fun getBinding(localAnimeId: String, platform: TrackingPlatform): Flow<TrackingBinding?> {
        return trackingBindingDao.getBinding(localAnimeId, platform).map { it?.toDomain() }
    }

    override suspend fun saveBinding(binding: TrackingBinding): AppResult<Unit> {
        return try {
            trackingBindingDao.upsertBinding(TrackingBindingEntity.fromDomain(binding))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("TrackingBindingRepo", "Failed to save binding for ${binding.localAnimeId}", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to save tracking binding", e))
        }
    }

    override suspend fun updateProgress(
        localAnimeId: String,
        platform: TrackingPlatform,
        progress: Int
    ): AppResult<Unit> {
        return try {
            trackingBindingDao.updateProgress(localAnimeId, platform, progress)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("TrackingBindingRepo", "Failed to update tracking progress for $localAnimeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update tracking progress", e))
        }
    }

    override suspend fun deleteBinding(localAnimeId: String, platform: TrackingPlatform): AppResult<Unit> {
        return try {
            trackingBindingDao.deleteBinding(localAnimeId, platform)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("TrackingBindingRepo", "Failed to delete tracking binding for $localAnimeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete tracking binding", e))
        }
    }

    override suspend fun deleteBindingsForAnime(localAnimeId: String): AppResult<Unit> {
        return try {
            trackingBindingDao.deleteBindingsForAnime(localAnimeId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("TrackingBindingRepo", "Failed to delete all bindings for anime $localAnimeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete bindings", e))
        }
    }
}
