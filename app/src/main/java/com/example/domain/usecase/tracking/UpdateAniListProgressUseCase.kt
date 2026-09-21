package com.example.domain.usecase.tracking

import com.example.core.result.AppResult
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.model.tracking.TrackStatus
import com.example.domain.repository.TrackingRepository

class UpdateAniListProgressUseCase(
    private val trackingRepository: TrackingRepository
) {
    suspend operator fun invoke(
        mediaId: Int,
        status: TrackStatus,
        progress: Int,
        score: Double? = null
    ): AppResult<AniListMediaEntry> {
        return trackingRepository.updateTrackEntry(mediaId, status, progress, score)
    }

    suspend fun syncLocalProgress(
        localAnimeId: String,
        episodeNumber: Int,
        score: Double? = null
    ): AppResult<Unit> {
        return trackingRepository.syncLocalProgressToAniList(localAnimeId, episodeNumber, score)
    }
}
