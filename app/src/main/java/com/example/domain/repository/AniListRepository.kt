package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.model.tracking.AniListUser
import com.example.domain.model.tracking.TrackStatus
import kotlinx.coroutines.flow.Flow

interface AniListRepository {
    fun getAccessToken(): Flow<String?>
    fun getStoredUser(): Flow<AniListUser?>
    suspend fun loginWithToken(token: String): AppResult<AniListUser>
    suspend fun logout(): AppResult<Unit>
    suspend fun fetchCurrentUserProfile(): AppResult<AniListUser>
    suspend fun updateMediaProgress(
        mediaId: Int,
        status: TrackStatus,
        progress: Int,
        score: Double? = null
    ): AppResult<AniListMediaEntry>
    suspend fun getMediaEntry(mediaId: Int): AppResult<AniListMediaEntry?>
    suspend fun getUserMediaList(status: TrackStatus? = null): AppResult<List<AniListMediaEntry>>
    suspend fun searchAniListMedia(query: String): AppResult<List<AniListMediaEntry>>
}
