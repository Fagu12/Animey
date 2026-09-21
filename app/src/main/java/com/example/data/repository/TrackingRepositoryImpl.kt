package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.model.TrackingBinding
import com.example.domain.model.TrackingPlatform
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.model.tracking.AniListUser
import com.example.domain.model.tracking.SyncStatus
import com.example.domain.model.tracking.TrackStatus
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.TrackingBindingRepository
import com.example.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class TrackingRepositoryImpl(
    private val aniListRepository: AniListRepository,
    private val trackingBindingRepository: TrackingBindingRepository
) : TrackingRepository {

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)

    override fun getSyncStatus(): Flow<SyncStatus> = _syncStatus.asStateFlow()

    override fun getAniListUser(): Flow<AniListUser?> = aniListRepository.getStoredUser()

    override suspend fun syncLocalProgressToAniList(
        localAnimeId: String,
        episodeNumber: Int,
        score: Double?
    ): AppResult<Unit> {
        val token = aniListRepository.getAccessToken().first()
        if (token.isNull_or_blank_safe()) {
            _syncStatus.value = SyncStatus.NOT_LOGGED_IN
            return AppResult.Error(AppError.AuthenticationError("Not logged into AniList"))
        }

        val binding = trackingBindingRepository.getBinding(localAnimeId, TrackingPlatform.ANILIST).first()
            ?: return AppResult.Error(AppError.ValidationError("No AniList binding found for local anime $localAnimeId"))

        val mediaId = binding.platformAnimeId.toIntOrNull()
            ?: return AppResult.Error(AppError.ValidationError("Invalid AniList media ID: ${binding.platformAnimeId}"))

        _syncStatus.value = SyncStatus.SYNCING
        AppLogger.d("TrackingRepository", "Syncing progress: anime=$localAnimeId, episode=$episodeNumber, aniListMediaId=$mediaId")

        // Only update if episode progress is higher or score updated
        val updateRes = aniListRepository.updateMediaProgress(
            mediaId = mediaId,
            status = TrackStatus.WATCHING,
            progress = episodeNumber,
            score = score
        )

        return when (updateRes) {
            is AppResult.Success -> {
                trackingBindingRepository.updateProgress(localAnimeId, TrackingPlatform.ANILIST, episodeNumber)
                _syncStatus.value = SyncStatus.SYNCED
                AppLogger.d("TrackingRepository", "Progress successfully synced to AniList")
                AppResult.Success(Unit)
            }
            is AppResult.Error -> {
                _syncStatus.value = SyncStatus.ERROR
                AppLogger.e("TrackingRepository", "Failed to sync progress to AniList")
                AppResult.Error(updateRes.error)
            }
            else -> AppResult.Error(AppError.GeneralError("Sync cancelled or loading"))
        }
    }

    override suspend fun bindAndSyncAnime(
        localAnimeId: String,
        sourceId: String,
        sourceAnimeId: String,
        aniListMediaId: Int,
        currentProgress: Int
    ): AppResult<Unit> {
        val binding = TrackingBinding(
            localAnimeId = localAnimeId,
            sourceId = sourceId,
            sourceAnimeId = sourceAnimeId,
            platform = TrackingPlatform.ANILIST,
            aniListId = aniListMediaId,
            currentProgress = currentProgress
        )

        val saveResult = trackingBindingRepository.saveBinding(binding)
        if (saveResult is AppResult.Error) return saveResult

        // Sync initial progress if logged in
        return if (!aniListRepository.getAccessToken().first().isNull_or_blank_safe()) {
            syncLocalProgressToAniList(localAnimeId, currentProgress)
        } else {
            AppResult.Success(Unit)
        }
    }

    override suspend fun updateTrackEntry(
        mediaId: Int,
        status: TrackStatus,
        progress: Int,
        score: Double?
    ): AppResult<AniListMediaEntry> {
        _syncStatus.value = SyncStatus.SYNCING
        val res = aniListRepository.updateMediaProgress(mediaId, status, progress, score)
        if (res is AppResult.Success) {
            _syncStatus.value = SyncStatus.SYNCED
        } else {
            _syncStatus.value = SyncStatus.ERROR
        }
        return res
    }

    override suspend fun getUserAnimeList(status: TrackStatus?): AppResult<List<AniListMediaEntry>> {
        return aniListRepository.getUserMediaList(status)
    }

    private fun String?.isNull_or_blank_safe(): Boolean = this == null || this.trim().isEmpty()
}
