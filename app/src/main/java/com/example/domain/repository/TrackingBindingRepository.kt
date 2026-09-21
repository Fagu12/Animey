package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.TrackingBinding
import com.example.domain.model.TrackingPlatform
import kotlinx.coroutines.flow.Flow

interface TrackingBindingRepository {
    fun getBindingsForAnime(localAnimeId: String): Flow<List<TrackingBinding>>
    fun getBinding(localAnimeId: String, platform: TrackingPlatform): Flow<TrackingBinding?>
    suspend fun saveBinding(binding: TrackingBinding): AppResult<Unit>
    suspend fun updateProgress(localAnimeId: String, platform: TrackingPlatform, progress: Int): AppResult<Unit>
    suspend fun deleteBinding(localAnimeId: String, platform: TrackingPlatform): AppResult<Unit>
    suspend fun deleteBindingsForAnime(localAnimeId: String): AppResult<Unit>
}
