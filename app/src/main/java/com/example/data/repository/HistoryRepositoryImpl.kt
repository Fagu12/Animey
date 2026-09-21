package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.local.database.dao.HistoryDao
import com.example.data.local.database.entity.HistoryEntity
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.HistoryEntry
import com.example.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<HistoryEntry>> {
        return historyDao.getAllHistory().map { list ->
            list.mapNotNull { item ->
                val anime = item.anime?.toDomain() ?: Anime(localId = item.history.animeId, title = "Unknown")
                val episode = item.episode?.toDomain() ?: Episode(
                    id = item.history.episodeId,
                    animeId = item.history.animeId,
                    sourceId = item.history.sourceId,
                    sourceEpisodeId = item.history.episodeId,
                    number = 1f
                )
                HistoryEntry(
                    anime = anime,
                    episode = episode,
                    positionMs = item.history.positionMs,
                    durationMs = item.history.durationMs,
                    watchedAt = item.history.watchedAt,
                    sourceId = item.history.sourceId
                )
            }
        }
    }

    override fun getLastWatchedForAnime(animeId: String): Flow<HistoryEntry?> {
        return historyDao.getLastWatchedForAnime(animeId).map { item ->
            item?.let {
                val anime = it.anime?.toDomain() ?: Anime(localId = it.history.animeId, title = "Unknown")
                val episode = it.episode?.toDomain() ?: Episode(
                    id = it.history.episodeId,
                    animeId = it.history.animeId,
                    sourceId = it.history.sourceId,
                    sourceEpisodeId = it.history.episodeId,
                    number = 1f
                )
                HistoryEntry(
                    anime = anime,
                    episode = episode,
                    positionMs = it.history.positionMs,
                    durationMs = it.history.durationMs,
                    watchedAt = it.history.watchedAt,
                    sourceId = it.history.sourceId
                )
            }
        }
    }

    override suspend fun recordHistory(
        animeId: String,
        episodeId: String,
        sourceId: String,
        positionMs: Long,
        durationMs: Long
    ): AppResult<Unit> {
        return try {
            val entity = HistoryEntity(
                episodeId = episodeId,
                animeId = animeId,
                sourceId = sourceId,
                positionMs = positionMs,
                durationMs = durationMs,
                watchedAt = System.currentTimeMillis()
            )
            historyDao.upsertHistory(entity)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("HistoryRepo", "Failed to record history for $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to record history", e))
        }
    }

    override suspend fun removeHistoryForEpisode(episodeId: String): AppResult<Unit> {
        return try {
            historyDao.deleteHistoryByEpisodeId(episodeId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("HistoryRepo", "Failed to remove history for episode $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete history", e))
        }
    }

    override suspend fun removeHistoryForAnime(animeId: String): AppResult<Unit> {
        return try {
            historyDao.deleteHistoryForAnime(animeId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("HistoryRepo", "Failed to remove history for anime $animeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete anime history", e))
        }
    }

    override suspend fun clearAllHistory(): AppResult<Unit> {
        return try {
            historyDao.clearAllHistory()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("HistoryRepo", "Failed to clear history", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to clear history", e))
        }
    }
}
