package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAllHistory(): Flow<List<HistoryEntry>>
    fun getLastWatchedForAnime(animeId: String): Flow<HistoryEntry?>
    suspend fun recordHistory(animeId: String, episodeId: String, sourceId: String, positionMs: Long, durationMs: Long): AppResult<Unit>
    suspend fun removeHistoryForEpisode(episodeId: String): AppResult<Unit>
    suspend fun removeHistoryForAnime(animeId: String): AppResult<Unit>
    suspend fun clearAllHistory(): AppResult<Unit>
}
