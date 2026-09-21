package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.domain.model.SkipSegment
import com.example.domain.model.SourceType
import com.example.domain.model.TrackInfo
import com.example.domain.model.VideoSource
import com.example.domain.player.PlaybackState
import com.example.domain.player.PlayerEngine
import com.example.domain.player.PlayerState
import com.example.domain.player.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class Media3PlayerEngine(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : PlayerEngine {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var progressJob: Job? = null
    private var skipSegments: List<SkipSegment> = emptyList()

    private var lastPreparedSource: VideoSource? = null
    private var lastStartPositionMs: Long = 0L

    init {
        initPlayer()
    }

    private fun initPlayer() {
        trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredTextLanguage("en")
                    .setSelectUndeterminedTextLanguage(true)
            )
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000, // min buffer
                50_000, // max buffer
                1_500,  // playback buffer
                2_500   // re-buffer
            )
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector!!)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setUsePlatformDiagnostics(false)
            .build().apply {
                addListener(PlayerEventListener())
            }

        startProgressTracking()
    }

    fun getExoPlayer(): ExoPlayer? = exoPlayer

    override fun prepare(source: VideoSource, startPositionMs: Long, autoPlay: Boolean) {
        lastPreparedSource = source
        lastStartPositionMs = startPositionMs

        _state.update {
            it.copy(
                engineType = com.example.domain.player.EngineType.MEDIA3,
                currentSource = source,
                playbackState = PlaybackState.BUFFERING,
                error = null,
                currentPositionMs = startPositionMs,
                durationMs = 0L,
                bufferedPositionMs = 0L
            )
        }

        if (source.type == SourceType.EMBED) {
            val error = AppError.PlayerError(
                errorCode = -1,
                message = "EMBED sources must not be passed to ExoPlayer. Use EmbedPlayerEngine instead."
            )
            _state.update { it.copy(playbackState = PlaybackState.ERROR, error = error) }
            return
        }

        val mediaSource = buildMediaSource(source)
        exoPlayer?.apply {
            setMediaSource(mediaSource)
            if (startPositionMs > 0) {
                seekTo(startPositionMs)
            }
            playWhenReady = autoPlay
            prepare()
        }
    }

    private fun buildMediaSource(source: VideoSource): MediaSource {
        val httpHeaders = mutableMapOf<String, String>()
        source.headers.forEach { (k, v) -> httpHeaders[k] = v }
        if (!source.referer.isNullOrBlank()) {
            httpHeaders["Referer"] = source.referer
            if (!httpHeaders.containsKey("Origin")) {
                try {
                    val uri = android.net.Uri.parse(source.referer)
                    if (uri.scheme != null && uri.host != null) {
                        httpHeaders["Origin"] = "${uri.scheme}://${uri.host}"
                    }
                } catch (_: Exception) {}
            }
        }

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(httpHeaders)
            .setUserAgent(httpHeaders["User-Agent"] ?: "Animey/1.0 (Android; OkHttp)")

        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(source.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(source.serverName)
                    .build()
            )

        // Attach external subtitle tracks if present
        if (source.subtitles.isNotEmpty()) {
            val subtitleConfigs = source.subtitles.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(sub.language)
                    .setLabel(sub.label)
                    .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
        }

        val mediaItem = mediaItemBuilder.build()

        return when (source.type) {
            SourceType.HLS -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            SourceType.MP4, SourceType.FILE, SourceType.DASH, SourceType.UNKNOWN -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            SourceType.EMBED -> {
                throw IllegalArgumentException("EMBED sources must not be passed to ExoPlayer. Use EmbedPlayerEngine instead.")
            }
        }
    }

    override fun play() {
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun togglePlayPause() {
        if (exoPlayer?.isPlaying == true) {
            pause()
        } else {
            play()
        }
    }

    override fun seekTo(positionMs: Long) {
        val duration = exoPlayer?.duration ?: 0L
        val clamped = if (duration > 0) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        exoPlayer?.seekTo(clamped)
        _state.update { it.copy(currentPositionMs = clamped) }
    }

    override fun seekForward(offsetMs: Long) {
        val current = exoPlayer?.currentPosition ?: 0L
        seekTo(current + offsetMs)
    }

    override fun seekBackward(offsetMs: Long) {
        val current = exoPlayer?.currentPosition ?: 0L
        seekTo(current - offsetMs)
    }

    override fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        exoPlayer?.playbackParameters = PlaybackParameters(clamped)
        _state.update { it.copy(playbackSpeed = clamped) }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        exoPlayer?.volume = clamped
        _state.update { it.copy(volume = clamped, isMuted = clamped == 0f) }
    }

    override fun setMuted(isMuted: Boolean) {
        if (isMuted) {
            exoPlayer?.volume = 0f
            _state.update { it.copy(isMuted = true) }
        } else {
            val vol = if (_state.value.volume > 0f) _state.value.volume else 1.0f
            exoPlayer?.volume = vol
            _state.update { it.copy(isMuted = false, volume = vol) }
        }
    }

    override fun selectVideoTrack(trackId: String?) {
        val selector = trackSelector ?: return
        val currentTracks = exoPlayer?.currentTracks ?: return

        if (trackId == null) {
            // Auto quality
            selector.setParameters(
                selector.parameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            )
            _state.update { it.copy(selectedVideoTrack = null) }
            return
        }

        for (group in currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.id == trackId || "${format.width}x${format.height}" == trackId) {
                        selector.setParameters(
                            selector.parameters.buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(group.mediaTrackGroup, i)
                                )
                        )
                        val selected = VideoTrack(
                            id = trackId,
                            width = format.width,
                            height = format.height,
                            bitrate = format.bitrate,
                            label = "${format.height}p",
                            isSelected = true
                        )
                        _state.update { it.copy(selectedVideoTrack = selected) }
                        return
                    }
                }
            }
        }
    }

    override fun selectAudioTrack(trackId: String?) {
        val selector = trackSelector ?: return
        if (trackId == null) {
            selector.setParameters(
                selector.parameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            )
            _state.update { it.copy(selectedAudioTrack = null) }
            return
        }

        val currentTracks = exoPlayer?.currentTracks ?: return
        for (group in currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.id == trackId || format.language == trackId) {
                        selector.setParameters(
                            selector.parameters.buildUpon()
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                        )
                        val selected = TrackInfo(
                            id = trackId,
                            label = format.label ?: format.language ?: "Audio",
                            language = format.language ?: "und"
                        )
                        _state.update { it.copy(selectedAudioTrack = selected) }
                        return
                    }
                }
            }
        }
    }

    override fun selectSubtitle(trackId: String?) {
        val selector = trackSelector ?: return
        if (trackId == null) {
            selector.setParameters(
                selector.parameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
            _state.update { it.copy(selectedSubtitle = null) }
            return
        }

        selector.setParameters(
            selector.parameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        )

        val currentTracks = exoPlayer?.currentTracks ?: return
        for (group in currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.id == trackId || format.language == trackId) {
                        selector.setParameters(
                            selector.parameters.buildUpon()
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                        )
                        val selected = TrackInfo(
                            id = trackId,
                            label = format.label ?: format.language ?: "Subtitle",
                            language = format.language ?: "und"
                        )
                        _state.update { it.copy(selectedSubtitle = selected) }
                        return
                    }
                }
            }
        }
    }

    override fun setSubtitleVisible(isVisible: Boolean) {
        val selector = trackSelector ?: return
        selector.setParameters(
            selector.parameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !isVisible)
        )
        _state.update { it.copy(isSubtitleVisible = isVisible) }
    }

    override fun setSubtitleDelay(delayMs: Long) {
        _state.update { it.copy(subtitleDelayMs = delayMs) }
    }

    override fun setSkipSegments(segments: List<SkipSegment>) {
        this.skipSegments = segments
        checkSkipSegments(exoPlayer?.currentPosition ?: 0L)
    }

    override fun setFullscreen(isFullscreen: Boolean) {
        _state.update { it.copy(isFullscreen = isFullscreen) }
    }

    override fun setPipActive(isPip: Boolean) {
        _state.update { it.copy(isPipActive = isPip) }
    }

    override fun retry() {
        val src = lastPreparedSource ?: return
        prepare(src, _state.value.currentPositionMs, true)
    }

    override fun release() {
        progressJob?.cancel()
        progressJob = null
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.playbackState != Player.STATE_IDLE) {
                    val currentPos = player.currentPosition.coerceAtLeast(0L)
                    val duration = player.duration.coerceAtLeast(0L)
                    val buffered = player.bufferedPosition.coerceAtLeast(0L)

                    _state.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = duration,
                            bufferedPositionMs = buffered,
                            isPlaying = player.isPlaying
                        )
                    }

                    checkSkipSegments(currentPos)
                }
                delay(250)
            }
        }
    }

    private fun checkSkipSegments(currentPos: Long) {
        val active = skipSegments.firstOrNull { currentPos in it.startTime..it.endTime }
        if (_state.value.activeSkipSegment != active) {
            _state.update { it.copy(activeSkipSegment = active) }
        }
    }

    private fun updateTracks(tracks: Tracks) {
        val videoTracks = mutableListOf<VideoTrack>()
        val audioTracks = mutableListOf<TrackInfo>()
        val subtitleTracks = mutableListOf<TrackInfo>()

        for (group in tracks.groups) {
            when (group.type) {
                C.TRACK_TYPE_VIDEO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val id = format.id ?: "${format.width}x${format.height}"
                        videoTracks.add(
                            VideoTrack(
                                id = id,
                                width = format.width,
                                height = format.height,
                                bitrate = format.bitrate,
                                label = if (format.height > 0) "${format.height}p" else "Auto",
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        audioTracks.add(
                            TrackInfo(
                                id = format.id ?: format.language ?: "audio_$i",
                                label = format.label ?: format.language ?: "Audio track $i",
                                language = format.language ?: "und",
                                isDefault = group.isTrackSelected(i)
                            )
                        )
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        subtitleTracks.add(
                            TrackInfo(
                                id = format.id ?: format.language ?: "sub_$i",
                                label = format.label ?: format.language ?: "Subtitle $i",
                                language = format.language ?: "und",
                                isDefault = group.isTrackSelected(i)
                            )
                        )
                    }
                }
            }
        }

        _state.update {
            it.copy(
                availableVideoTracks = videoTracks.distinctBy { vt -> vt.id },
                availableAudioTracks = audioTracks.distinctBy { at -> at.id },
                availableSubtitles = subtitleTracks.distinctBy { st -> st.id }
            )
        }
    }

    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val newState = when (state) {
                Player.STATE_IDLE -> PlaybackState.IDLE
                Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                Player.STATE_READY -> PlaybackState.READY
                Player.STATE_ENDED -> PlaybackState.ENDED
                else -> PlaybackState.IDLE
            }
            _state.update {
                it.copy(
                    playbackState = newState,
                    durationMs = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTracks(tracks)
        }

        override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
            val text = cueGroup.cues.joinToString("\n") { cue -> cue.text?.toString() ?: "" }.trim()
            _state.update { it.copy(currentSubtitleText = text) }
        }

        override fun onPlayerError(error: PlaybackException) {
            AppLogger.e("Media3PlayerEngine", "Player error code=${error.errorCode}: ${error.message}", error)
            val appError = AppError.PlayerError(
                errorCode = error.errorCode,
                message = error.localizedMessage ?: "Playback error",
                cause = error
            )
            _state.update {
                it.copy(
                    playbackState = PlaybackState.ERROR,
                    error = appError
                )
            }
        }
    }
}
