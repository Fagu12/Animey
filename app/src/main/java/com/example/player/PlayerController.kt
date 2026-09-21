package com.example.player

import com.example.core.logging.AppLogger
import com.example.domain.model.AppSettings
import com.example.domain.model.Episode
import com.example.domain.model.SkipSegment
import com.example.domain.model.VideoSource
import com.example.domain.player.PlaybackState
import com.example.domain.player.PlayerEngine
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.TrackingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Controller coordinating playback business logic:
 * - Resume position restoration and sync
 * - Auto-skipping intro/outro based on user preferences
 * - Auto-playing next episode
 * - Persisting progress to watch history & episode entities
 * - Throttled sync to AniList tracking
 */
class PlayerController(
    private val playerEngine: PlayerEngine,
    private val historyRepository: HistoryRepository,
    private val episodeRepository: EpisodeRepository,
    private val settingsRepository: SettingsRepository,
    private val trackingRepository: TrackingRepository? = null,
    private val onAutoPlayNextEpisode: (() -> Unit)? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : PlayerEngine by playerEngine {

    private var currentEpisode: Episode? = null
    private var currentAnimeId: String? = null
    private var appSettings: AppSettings = AppSettings()

    private var hasReachedThreshold: Boolean = false
    private var lastLocalDbSyncTimeMs: Long = 0L
    private var lastAniListSyncTimeMs: Long = 0L
    private var lastSyncedAniListProgress: Int = -1
    private val automaticallySkippedSegments = mutableSetOf<String>()

    init {
        scope.launch {
            settingsRepository.settings.collect { settings ->
                appSettings = settings
            }
        }

        observePlaybackForAutoSkipAndHistory()
    }

    fun playEpisode(
        animeId: String,
        episode: Episode,
        source: VideoSource,
        autoPlay: Boolean = true
    ) {
        currentAnimeId = animeId
        currentEpisode = episode
        hasReachedThreshold = false
        lastLocalDbSyncTimeMs = 0L
        lastAniListSyncTimeMs = 0L
        lastSyncedAniListProgress = -1
        automaticallySkippedSegments.clear()

        val resumePosition = episode.lastPositionMs
        playerEngine.setSkipSegments(episode.skipSegments)
        playerEngine.prepare(source, resumePosition, autoPlay)
    }

    fun skipCurrentSegment() {
        val active = state.value.activeSkipSegment ?: return
        val segmentKey = "${active.type}_${active.startTime}_${active.endTime}"
        automaticallySkippedSegments.add(segmentKey)
        playerEngine.seekTo(active.endTime)
    }

    private fun observePlaybackForAutoSkipAndHistory() {
        scope.launch {
            state.collect { state ->
                val ep = currentEpisode ?: return@collect
                val animeId = currentAnimeId ?: return@collect
                val now = System.currentTimeMillis()

                // 1. Auto skip intro / outro / recap
                val activeSegment = state.activeSkipSegment
                if (activeSegment != null) {
                    val segmentKey = "${activeSegment.type}_${activeSegment.startTime}_${activeSegment.endTime}"
                    val hasBeenSkipped = appSettings.skipOnce && automaticallySkippedSegments.contains(segmentKey)

                    if (!hasBeenSkipped) {
                        val shouldSkip = when (activeSegment.type) {
                            com.example.domain.model.SkipType.INTRO -> appSettings.autoSkipIntro
                            com.example.domain.model.SkipType.OUTRO -> appSettings.autoSkipOutro
                            com.example.domain.model.SkipType.RECAP -> appSettings.autoSkipRecap
                        }
                        if (shouldSkip && state.currentPositionMs in activeSegment.startTime until activeSegment.endTime) {
                            automaticallySkippedSegments.add(segmentKey)
                            playerEngine.seekTo(activeSegment.endTime)
                        }
                    }
                }

                if (state.currentPositionMs <= 0) return@collect

                val duration = if (state.durationMs > 0) state.durationMs else ep.durationMs
                val progressPercent = if (duration > 0) {
                    ((state.currentPositionMs.toFloat() / duration.toFloat()) * 100f).coerceIn(0f, 100f)
                } else 0f

                val thresholdPercent = appSettings.tracking.completionPercentage.toFloat()
                val isThresholdReached = progressPercent >= thresholdPercent

                // 2. Handle completion threshold transition
                if (isThresholdReached && !hasReachedThreshold) {
                    hasReachedThreshold = true
                    AppLogger.d("PlayerController", "Reached completion threshold ($progressPercent% >= $thresholdPercent%) for ep ${ep.number}")

                    if (appSettings.tracking.autoMarkCompleted) {
                        episodeRepository.updateProgress(
                            episodeId = ep.id,
                            positionMs = state.currentPositionMs,
                            durationMs = duration,
                            isWatched = true
                        )
                    }

                    if (!appSettings.incognitoMode) {
                        historyRepository.recordHistory(
                            animeId = animeId,
                            episodeId = ep.id,
                            sourceId = ep.sourceId,
                            positionMs = state.currentPositionMs,
                            durationMs = duration
                        )
                    }

                    if (appSettings.tracking.autoUpdateAniList && trackingRepository != null) {
                        val epNum = ep.number.toInt()
                        if (epNum > 0 && lastSyncedAniListProgress != epNum) {
                            lastSyncedAniListProgress = epNum
                            scope.launch {
                                trackingRepository.syncLocalProgressToAniList(
                                    localAnimeId = animeId,
                                    episodeNumber = epNum
                                )
                            }
                        }
                    }
                }

                // 3. Throttled local DB progress sync (every 5 seconds or upon pause/ended)
                val isPausedOrEnded = !state.isPlaying || state.playbackState == PlaybackState.ENDED
                val timeSinceLastLocalSync = now - lastLocalDbSyncTimeMs

                if (isPausedOrEnded || timeSinceLastLocalSync >= 5000L) {
                    lastLocalDbSyncTimeMs = now
                    episodeRepository.updateProgress(
                        episodeId = ep.id,
                        positionMs = state.currentPositionMs,
                        durationMs = duration,
                        isWatched = hasReachedThreshold || ep.isWatched
                    )

                    if (!appSettings.incognitoMode) {
                        historyRepository.recordHistory(
                            animeId = animeId,
                            episodeId = ep.id,
                            sourceId = ep.sourceId,
                            positionMs = state.currentPositionMs,
                            durationMs = duration
                        )
                    }
                }

                // 4. Throttled AniList progress sync during playback (every 30s or ended)
                if (appSettings.tracking.syncProgress && appSettings.tracking.autoUpdateAniList && trackingRepository != null) {
                    val timeSinceLastAniListSync = now - lastAniListSyncTimeMs
                    if (state.playbackState == PlaybackState.ENDED || timeSinceLastAniListSync >= 30000L) {
                        lastAniListSyncTimeMs = now
                        val epNum = ep.number.toInt()
                        if (epNum > 0 && lastSyncedAniListProgress != epNum) {
                            lastSyncedAniListProgress = epNum
                            scope.launch {
                                trackingRepository.syncLocalProgressToAniList(
                                    localAnimeId = animeId,
                                    episodeNumber = epNum
                                )
                            }
                        }
                    }
                }

                // 5. Auto Play Next Episode
                if (state.playbackState == PlaybackState.ENDED && appSettings.autoPlayNext) {
                    onAutoPlayNextEpisode?.invoke()
                }
            }
        }
    }
}
