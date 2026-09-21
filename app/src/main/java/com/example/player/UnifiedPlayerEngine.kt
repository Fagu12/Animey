package com.example.player

import android.content.Context
import com.example.domain.model.SkipSegment
import com.example.domain.model.SourceType
import com.example.domain.model.VideoSource
import com.example.domain.player.EngineType
import com.example.domain.player.PlayerEngine
import com.example.domain.player.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Unified Player Engine that dynamically routes stream sources:
 * - HLS / MP4 / FILE -> Media3PlayerEngine (ExoPlayer)
 * - EMBED -> EmbedPlayerEngine (Isolated WebView Player)
 *
 * Strictly adheres to: Never send an EMBED webpage URL directly to ExoPlayer.
 */
class UnifiedPlayerEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    val media3Engine: Media3PlayerEngine = Media3PlayerEngine(context),
    val embedEngine: EmbedPlayerEngine = EmbedPlayerEngine(context)
) : PlayerEngine {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var activeEngine: PlayerEngine = media3Engine
    private var stateObservationJob: Job? = null

    init {
        observeActiveEngineState()
    }

    private fun observeActiveEngineState() {
        stateObservationJob?.cancel()
        stateObservationJob = scope.launch {
            activeEngine.state.collect { newState ->
                _state.value = newState
            }
        }
    }

    override fun prepare(source: VideoSource, startPositionMs: Long, autoPlay: Boolean) {
        val targetEngine: PlayerEngine = when (source.type) {
            SourceType.EMBED -> {
                // Ensure ExoPlayer is paused and inactive
                media3Engine.pause()
                embedEngine
            }
            SourceType.HLS, SourceType.MP4, SourceType.FILE, SourceType.DASH, SourceType.UNKNOWN -> {
                // Ensure Embed Player is paused and inactive
                embedEngine.pause()
                media3Engine
            }
        }

        if (activeEngine != targetEngine) {
            activeEngine = targetEngine
            observeActiveEngineState()
        }

        activeEngine.prepare(source, startPositionMs, autoPlay)
    }

    fun getActiveEngineType(): EngineType {
        return if (activeEngine === embedEngine) EngineType.EMBED else EngineType.MEDIA3
    }

    override fun play() {
        activeEngine.play()
    }

    override fun pause() {
        activeEngine.pause()
    }

    override fun togglePlayPause() {
        activeEngine.togglePlayPause()
    }

    override fun seekTo(positionMs: Long) {
        activeEngine.seekTo(positionMs)
    }

    override fun seekForward(offsetMs: Long) {
        activeEngine.seekForward(offsetMs)
    }

    override fun seekBackward(offsetMs: Long) {
        activeEngine.seekBackward(offsetMs)
    }

    override fun setPlaybackSpeed(speed: Float) {
        activeEngine.setPlaybackSpeed(speed)
    }

    override fun setVolume(volume: Float) {
        activeEngine.setVolume(volume)
    }

    override fun setMuted(isMuted: Boolean) {
        activeEngine.setMuted(isMuted)
    }

    override fun selectVideoTrack(trackId: String?) {
        activeEngine.selectVideoTrack(trackId)
    }

    override fun selectAudioTrack(trackId: String?) {
        activeEngine.selectAudioTrack(trackId)
    }

    override fun selectSubtitle(trackId: String?) {
        activeEngine.selectSubtitle(trackId)
    }

    override fun setSubtitleVisible(isVisible: Boolean) {
        activeEngine.setSubtitleVisible(isVisible)
    }

    override fun setSubtitleDelay(delayMs: Long) {
        activeEngine.setSubtitleDelay(delayMs)
    }

    override fun setSkipSegments(segments: List<SkipSegment>) {
        media3Engine.setSkipSegments(segments)
        embedEngine.setSkipSegments(segments)
    }

    override fun setFullscreen(isFullscreen: Boolean) {
        _state.value = _state.value.copy(isFullscreen = isFullscreen)
        activeEngine.setFullscreen(isFullscreen)
    }

    override fun setPipActive(isPip: Boolean) {
        _state.value = _state.value.copy(isPipActive = isPip)
        activeEngine.setPipActive(isPip)
    }

    override fun retry() {
        activeEngine.retry()
    }

    override fun release() {
        stateObservationJob?.cancel()
        media3Engine.release()
        embedEngine.release()
    }
}
