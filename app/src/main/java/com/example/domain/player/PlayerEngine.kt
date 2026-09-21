package com.example.domain.player

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.model.SkipSegment
import com.example.domain.model.TrackInfo
import com.example.domain.model.VideoSource
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR
}

enum class EngineType {
    MEDIA3,
    EMBED
}

data class VideoTrack(
    val id: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val label: String,
    val isSelected: Boolean = false
)

data class PlayerState(
    val engineType: EngineType = EngineType.MEDIA3,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val isFullscreen: Boolean = false,
    val isPipActive: Boolean = false,
    val currentSource: VideoSource? = null,
    val availableVideoTracks: List<VideoTrack> = emptyList(),
    val selectedVideoTrack: VideoTrack? = null,
    val availableAudioTracks: List<TrackInfo> = emptyList(),
    val selectedAudioTrack: TrackInfo? = null,
    val availableSubtitles: List<TrackInfo> = emptyList(),
    val selectedSubtitle: TrackInfo? = null,
    val isSubtitleVisible: Boolean = true,
    val currentSubtitleText: String = "",
    val subtitleDelayMs: Long = 0L,
    val activeSkipSegment: SkipSegment? = null,
    val error: AppError? = null
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    
    val bufferedProgress: Float
        get() = if (durationMs > 0) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * Core interface for the media player engine.
 * Decoupled from Android UI and provider implementations.
 */
interface PlayerEngine {
    val state: StateFlow<PlayerState>

    fun prepare(source: VideoSource, startPositionMs: Long = 0L, autoPlay: Boolean = true)
    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun seekForward(offsetMs: Long = 10_000L)
    fun seekBackward(offsetMs: Long = 10_000L)
    fun setPlaybackSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun setMuted(isMuted: Boolean)
    fun selectVideoTrack(trackId: String?)
    fun selectAudioTrack(trackId: String?)
    fun selectSubtitle(trackId: String?)
    fun setSubtitleVisible(isVisible: Boolean)
    fun setSubtitleDelay(delayMs: Long)
    fun setSkipSegments(segments: List<SkipSegment>)
    fun setFullscreen(isFullscreen: Boolean)
    fun setPipActive(isPip: Boolean)
    fun retry()
    fun release()
}
