package com.example.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.domain.model.SkipSegment
import com.example.domain.model.SourceType
import com.example.domain.model.TrackInfo
import com.example.domain.model.VideoSource
import com.example.domain.player.EngineType
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

/**
 * Embed Player Engine for rendering embedded web video streams (iframe embeds, web player URLs).
 * Strictly isolated from ExoPlayer. Never routes EMBED URLs to Media3.
 */
class EmbedPlayerEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : PlayerEngine {

    private val _state = MutableStateFlow(
        PlayerState(engineType = EngineType.EMBED)
    )
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var webView: WebView? = null
    private var lastPreparedSource: VideoSource? = null
    private var lastStartPositionMs: Long = 0L
    private var autoPlayOnReady: Boolean = true
    private var skipSegments: List<SkipSegment> = emptyList()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressPollerJob: Job? = null

    init {
        mainHandler.post {
            initWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        if (webView != null) return
        try {
            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = false
                    allowContentAccess = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                addJavascriptInterface(EmbedBridge(), "AnimeyBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        _state.update {
                            it.copy(
                                playbackState = PlaybackState.BUFFERING,
                                error = null
                            )
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        injectVideoListeners()
                        _state.update {
                            it.copy(playbackState = PlaybackState.READY)
                        }
                        if (lastStartPositionMs > 0) {
                            seekTo(lastStartPositionMs)
                        }
                        if (autoPlayOnReady) {
                            play()
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val msg = error?.description?.toString() ?: "Web player loading error"
                            AppLogger.e("EmbedPlayerEngine", "Embed error on main frame: $msg")
                            _state.update {
                                it.copy(
                                    playbackState = PlaybackState.ERROR,
                                    error = AppError.PlayerError(
                                        errorCode = error?.errorCode ?: -1,
                                        message = "Failed to load embed stream: $msg"
                                    )
                                )
                            }
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                            val code = errorResponse?.statusCode ?: 500
                            _state.update {
                                it.copy(
                                    playbackState = PlaybackState.ERROR,
                                    error = AppError.PlayerError(
                                        errorCode = code,
                                        message = "Server returned HTTP $code for embed player"
                                    )
                                )
                            }
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        // Reject insecure SSL certificates
                        handler?.cancel()
                        _state.update {
                            it.copy(
                                playbackState = PlaybackState.ERROR,
                                error = AppError.PlayerError(
                                    errorCode = -2,
                                    message = "SSL Certificate error on embed stream"
                                )
                            )
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress < 100 && _state.value.playbackState == PlaybackState.IDLE) {
                            _state.update { it.copy(playbackState = PlaybackState.BUFFERING) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("EmbedPlayerEngine", "Failed to create WebView: ${e.message}", e)
        }

        startProgressPolling()
    }

    fun getWebView(): WebView? = webView

    override fun prepare(source: VideoSource, startPositionMs: Long, autoPlay: Boolean) {
        lastPreparedSource = source
        lastStartPositionMs = startPositionMs
        autoPlayOnReady = autoPlay

        _state.update {
            it.copy(
                engineType = EngineType.EMBED,
                currentSource = source,
                playbackState = PlaybackState.BUFFERING,
                error = null,
                currentPositionMs = startPositionMs,
                durationMs = 0L,
                bufferedPositionMs = 0L,
                availableVideoTracks = listOf(VideoTrack(id = "embed", width = 1920, height = 1080, bitrate = 0, label = source.quality, isSelected = true)),
                availableAudioTracks = source.audioTracks,
                availableSubtitles = source.subtitles
            )
        }

        mainHandler.post {
            initWebView()
            val extraHeaders = mutableMapOf<String, String>()
            source.headers.forEach { (k, v) -> extraHeaders[k] = v }
            if (!source.referer.isNullOrBlank()) {
                extraHeaders["Referer"] = source.referer
            }
            if (!extraHeaders.containsKey("User-Agent")) {
                extraHeaders["User-Agent"] = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            }

            try {
                webView?.loadUrl(source.url, extraHeaders)
            } catch (e: Exception) {
                AppLogger.e("EmbedPlayerEngine", "Error loading embed URL: ${e.message}", e)
                _state.update {
                    it.copy(
                        playbackState = PlaybackState.ERROR,
                        error = AppError.PlayerError(errorCode = -1, message = "Could not load embed source")
                    )
                }
            }
        }
    }

    private fun injectVideoListeners() {
        val js = """
            (function() {
                function hookVideo(v) {
                    if (v.__animeyHooked) return;
                    v.__animeyHooked = true;
                    
                    v.addEventListener('play', function() {
                        if (window.AnimeyBridge) window.AnimeyBridge.onPlay();
                    });
                    v.addEventListener('pause', function() {
                        if (window.AnimeyBridge) window.AnimeyBridge.onPause();
                    });
                    v.addEventListener('timeupdate', function() {
                        if (window.AnimeyBridge) window.AnimeyBridge.onTimeUpdate(v.currentTime, v.duration);
                    });
                    v.addEventListener('waiting', function() {
                        if (window.AnimeyBridge) window.AnimeyBridge.onBuffering();
                    });
                    v.addEventListener('playing', function() {
                        if (window.AnimeyBridge) window.AnimeyBridge.onPlaying();
                    });
                    v.addEventListener('ended', function() {
                        if (window.AnimeyBridge) window.AnimeyBridge.onEnded();
                    });
                }
                
                document.querySelectorAll('video').forEach(hookVideo);
                
                var observer = new MutationObserver(function(mutations) {
                    document.querySelectorAll('video').forEach(hookVideo);
                });
                observer.observe(document.body, { childList: true, subtree: true });
            })();
        """.trimIndent()
        evalJs(js)
    }

    private fun evalJs(script: String) {
        mainHandler.post {
            try {
                webView?.evaluateJavascript(script, null)
            } catch (_: Exception) {}
        }
    }

    override fun play() {
        evalJs("document.querySelector('video')?.play();")
        _state.update { it.copy(isPlaying = true) }
    }

    override fun pause() {
        evalJs("document.querySelector('video')?.pause();")
        _state.update { it.copy(isPlaying = false) }
    }

    override fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        evalJs("var v = document.querySelector('video'); if (v) { v.currentTime = $seconds; }")
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    override fun seekForward(offsetMs: Long) {
        val target = _state.value.currentPositionMs + offsetMs
        seekTo(target)
    }

    override fun seekBackward(offsetMs: Long) {
        val target = (_state.value.currentPositionMs - offsetMs).coerceAtLeast(0L)
        seekTo(target)
    }

    override fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        evalJs("var v = document.querySelector('video'); if (v) { v.playbackRate = $clamped; }")
        _state.update { it.copy(playbackSpeed = clamped) }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        evalJs("var v = document.querySelector('video'); if (v) { v.volume = $clamped; }")
        _state.update { it.copy(volume = clamped, isMuted = clamped == 0f) }
    }

    override fun setMuted(isMuted: Boolean) {
        evalJs("var v = document.querySelector('video'); if (v) { v.muted = $isMuted; }")
        _state.update { it.copy(isMuted = isMuted) }
    }

    override fun selectVideoTrack(trackId: String?) {}

    override fun selectAudioTrack(trackId: String?) {
        val selected = _state.value.availableAudioTracks.find { it.id == trackId }
        _state.update { it.copy(selectedAudioTrack = selected) }
    }

    override fun selectSubtitle(trackId: String?) {
        val selected = _state.value.availableSubtitles.find { it.id == trackId }
        _state.update { it.copy(selectedSubtitle = selected) }
    }

    override fun setSubtitleVisible(isVisible: Boolean) {
        _state.update { it.copy(isSubtitleVisible = isVisible) }
    }

    override fun setSubtitleDelay(delayMs: Long) {
        _state.update { it.copy(subtitleDelayMs = delayMs) }
    }

    override fun setSkipSegments(segments: List<SkipSegment>) {
        this.skipSegments = segments
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
        progressPollerJob?.cancel()
        progressPollerJob = null
        mainHandler.post {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    private fun startProgressPolling() {
        progressPollerJob?.cancel()
        progressPollerJob = scope.launch {
            while (isActive) {
                if (_state.value.playbackState == PlaybackState.READY && _state.value.isPlaying) {
                    evalJs("""
                        (function() {
                            var v = document.querySelector('video');
                            if (v && window.AnimeyBridge) {
                                window.AnimeyBridge.onTimeUpdate(v.currentTime, v.duration);
                            }
                        })();
                    """.trimIndent())
                }
                delay(1000)
            }
        }
    }

    private inner class EmbedBridge {
        @JavascriptInterface
        fun onPlay() {
            _state.update { it.copy(isPlaying = true, playbackState = PlaybackState.READY) }
        }

        @JavascriptInterface
        fun onPause() {
            _state.update { it.copy(isPlaying = false) }
        }

        @JavascriptInterface
        fun onBuffering() {
            _state.update { it.copy(playbackState = PlaybackState.BUFFERING) }
        }

        @JavascriptInterface
        fun onPlaying() {
            _state.update { it.copy(isPlaying = true, playbackState = PlaybackState.READY) }
        }

        @JavascriptInterface
        fun onEnded() {
            _state.update { it.copy(isPlaying = false, playbackState = PlaybackState.ENDED) }
        }

        @JavascriptInterface
        fun onTimeUpdate(currentTimeSec: Double, durationSec: Double) {
            val currentMs = (currentTimeSec * 1000.0).toLong().coerceAtLeast(0L)
            val durMs = if (!durationSec.isNaN() && durationSec > 0) (durationSec * 1000.0).toLong() else _state.value.durationMs

            _state.update {
                it.copy(
                    currentPositionMs = currentMs,
                    durationMs = durMs
                )
            }

            // Check skip segments
            val active = skipSegments.firstOrNull { currentMs in it.startTime..it.endTime }
            if (_state.value.activeSkipSegment != active) {
                _state.update { it.copy(activeSkipSegment = active) }
            }
        }
    }
}
