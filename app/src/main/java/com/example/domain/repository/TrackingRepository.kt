package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.model.tracking.AniListUser
import com.example.domain.model.tracking.SyncStatus
import com.example.domain.model.tracking.TrackStatus
import kotlinx.coroutines.flow.Flow

interface TrackingRepository {
    fun getSyncStatus(): Flow<SyncStatus>
    fun getAniListUser(): Flow<AniListUser?>
    suspend fun syncLocalProgressToAniList(
        localAnimeId: String,
        episodeNumber: Int,
        score: Double? = null
    ): AppResult<Unit>
    suspend fun bindAndSyncAnime(
        localAnimeId: String,
        sourceId: String,
        sourceAnimeId: String,
        aniListMediaId: Int,
        currentProgress: Int
    ): AppResult<Unit>
    suspend fun updateTrackEntry(
        mediaId: Int,
        status: TrackStatus,
        progress: Int,
        score: Double?
    ): AppResult<AniListMediaEntry>
    suspend fun getUserAnimeList(status: TrackStatus? = null): AppResult<List<AniListMediaEntry>>
}
