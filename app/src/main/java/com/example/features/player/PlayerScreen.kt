package com.example.features.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.AnimeyApplication
import com.example.domain.model.Episode
import com.example.domain.model.SkipType
import com.example.domain.model.VideoSource
import com.example.domain.player.EngineType
import com.example.domain.player.PlaybackState
import com.example.features.settings.SubtitleSettingsSheet
import com.example.player.UnifiedPlayerEngine
import com.example.ui.components.player.SubtitleOverlay
import com.example.ui.components.source.SourceSelectionSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    animeId: String,
    episodeId: String,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val container = (context.applicationContext as AnimeyApplication).container

    val playerEngine = remember { UnifiedPlayerEngine(context) }

    val viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.provideFactory(
            animeId = animeId,
            episodeId = episodeId,
            animeRepository = container.animeRepository,
            episodeRepository = container.episodeRepository,
            historyRepository = container.historyRepository,
            settingsRepository = container.settingsRepository,
            sourceSelector = container.sourceSelector,
            playerEngine = playerEngine,
            trackingRepository = container.trackingRepository,
            skipMetadataRepository = container.skipMetadataRepository,
            downloadRepository = container.downloadRepository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val sourceState by viewModel.sourceSelectionState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()

    // Gesture feedback state
    var gestureOverlayMessage by remember { mutableStateOf<String?>(null) }
    var gestureOverlayIcon by remember { mutableStateOf<ImageVector?>(null) }
    var originalSpeed by remember { mutableFloatStateOf(1.0f) }

    // Synchronize Keep Screen Awake
    DisposableEffect(uiState.appSettings.keepScreenAwake) {
        if (activity != null) {
            if (uiState.appSettings.keepScreenAwake) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Synchronize Fullscreen & System Insets
    DisposableEffect(playerState.isFullscreen) {
        if (activity != null) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (playerState.isFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            if (activity != null) {
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    BackHandler {
        if (playerState.isFullscreen) {
            viewModel.setFullscreen(false)
        } else {
            onNavigateBack()
        }
    }

    val gestures = uiState.appSettings.gestures

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_root")
    ) {
        // Video Surface Box with gesture handling
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gestures, playerState.durationMs, playerState.currentPositionMs) {
                    detectTapGestures(
                        onTap = {
                            viewModel.setControlsVisible(!uiState.isControlsVisible)
                        },
                        onDoubleTap = { offset ->
                            if (gestures.enableDoubleTapSeek) {
                                val isRight = offset.x > size.width / 2
                                val seekSec = gestures.doubleTapSeekSeconds
                                if (isRight) {
                                    viewModel.seekRelative(seekSec)
                                    gestureOverlayMessage = "+${seekSec}s"
                                    gestureOverlayIcon = Icons.Filled.Forward10
                                } else {
                                    viewModel.seekRelative(-seekSec)
                                    gestureOverlayMessage = "-${seekSec}s"
                                    gestureOverlayIcon = Icons.Filled.Replay10
                                }
                                coroutineScope.launch {
                                    delay(1200)
                                    gestureOverlayMessage = null
                                    gestureOverlayIcon = null
                                }
                            } else {
                                viewModel.setControlsVisible(!uiState.isControlsVisible)
                            }
                        },
                        onLongPress = {
                            if (gestures.enableLongPressSpeed) {
                                originalSpeed = playerState.playbackSpeed
                                viewModel.setPlaybackSpeed(gestures.longPressSpeedMultiplier)
                                gestureOverlayMessage = "${gestures.longPressSpeedMultiplier}x Speed"
                                gestureOverlayIcon = Icons.Filled.FastForward
                            }
                        }
                    )
                }
                .pointerInput(gestures, playerState.durationMs) {
                    var startX = 0f
                    var startY = 0f
                    var dragX = 0f
                    var dragY = 0f
                    var initialPos = 0L
                    var initialVol = 1.0f
                    var initialBright = 0.5f
                    var dragMode = 0 // 1: Horizontal seek, 2: Brightness (left), 3: Volume (right)

                    detectDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
                            startY = offset.y
                            dragX = 0f
                            dragY = 0f
                            initialPos = playerState.currentPositionMs
                            initialVol = playerState.volume
                            initialBright = activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
                            dragMode = 0
                        },
                        onDrag = { change, dragAmount ->
                            dragX += dragAmount.x
                            dragY += dragAmount.y

                            if (dragMode == 0) {
                                if (kotlin.math.abs(dragX) > kotlin.math.abs(dragY) + 20f && gestures.enableSwipeSeek) {
                                    dragMode = 1
                                } else if (kotlin.math.abs(dragY) > kotlin.math.abs(dragX) + 20f) {
                                    if (startX < size.width / 2 && gestures.enableBrightnessGesture) {
                                        dragMode = 2
                                    } else if (startX >= size.width / 2 && gestures.enableVolumeGesture) {
                                        dragMode = 3
                                    }
                                }
                            }

                            val duration = playerState.durationMs.coerceAtLeast(1L)

                            when (dragMode) {
                                1 -> { // Horizontal Swipe Seek
                                    val fraction = (dragX / size.width.toFloat()).coerceIn(-1f, 1f)
                                    val deltaMs = (fraction * 90_000L).toLong()
                                    val targetMs = (initialPos + deltaMs).coerceIn(0L, duration)
                                    viewModel.seekTo(targetMs)
                                    gestureOverlayMessage = "${formatTime(targetMs)} / ${formatTime(duration)}"
                                    gestureOverlayIcon = Icons.Filled.Swipe
                                }
                                2 -> { // Vertical Brightness
                                    val delta = -dragY / size.height.toFloat()
                                    val newBright = (initialBright + delta).coerceIn(0.01f, 1.0f)
                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        lp.screenBrightness = newBright
                                        act.window.attributes = lp
                                    }
                                    gestureOverlayMessage = "Brightness: ${(newBright * 100).toInt()}%"
                                    gestureOverlayIcon = Icons.Filled.Brightness6
                                }
                                3 -> { // Vertical Volume
                                    val delta = -dragY / size.height.toFloat()
                                    val newVol = (initialVol + delta).coerceIn(0.0f, 1.0f)
                                    playerEngine.setVolume(newVol)
                                    gestureOverlayMessage = "Volume: ${(newVol * 100).toInt()}%"
                                    gestureOverlayIcon = Icons.Filled.VolumeUp
                                }
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                delay(1000)
                                gestureOverlayMessage = null
                                gestureOverlayIcon = null
                            }
                        },
                        onDragCancel = {
                            gestureOverlayMessage = null
                            gestureOverlayIcon = null
                        }
                    )
                }
        ) {
            // Media3 PlayerView or WebView
            if (playerState.engineType == EngineType.EMBED) {
                EmbedPlayerView(
                    webView = playerEngine.embedEngine.getWebView(),
                    onToggleControls = { viewModel.setControlsVisible(!uiState.isControlsVisible) }
                )
            } else {
                val exoPlayer = playerEngine.media3Engine.getExoPlayer()
                if (exoPlayer != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                resizeMode = parseResizeMode(uiState.appSettings.videoFitMode)
                            }
                        },
                        update = { playerView ->
                            playerView.resizeMode = parseResizeMode(uiState.appSettings.videoFitMode)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("player_video_surface")
                    )
                }
            }
        }

        // Custom Subtitle Text Overlay
        SubtitleOverlay(
            text = playerState.currentSubtitleText,
            settings = uiState.appSettings.subtitles,
            isVisible = playerState.isSubtitleVisible
        )

        // Gesture Feedback Overlay
        gestureOverlayMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                GestureOverlayBadge(message = msg, icon = gestureOverlayIcon)
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = uiState.isControlsVisible || playerState.playbackState == PlaybackState.BUFFERING || playerState.playbackState == PlaybackState.ERROR,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControlsOverlay(
                uiState = uiState,
                playerState = playerState,
                sourceState = sourceState,
                onNavigateBack = {
                    if (playerState.isFullscreen) {
                        viewModel.setFullscreen(false)
                    } else {
                        onNavigateBack()
                    }
                },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onSeekRelative = { viewModel.seekRelative(it) },
                onPreviousEpisode = { viewModel.playPreviousEpisode() },
                onNextEpisode = { viewModel.playNextEpisode() },
                onToggleFullscreen = { viewModel.setFullscreen(!playerState.isFullscreen) },
                onOpenSourceSheet = { viewModel.setSourceSheetVisible(true) },
                onOpenSettingsSheet = { viewModel.setSettingsSheetVisible(true) },
                onOpenEpisodesDrawer = { viewModel.setEpisodesDrawerVisible(true) },
                onRetry = { viewModel.retryPlayback() }
            )
        }

        // Skip Segment Floating Button (Intro/Outro/Recap)
        playerState.activeSkipSegment?.let { segment ->
            if (uiState.appSettings.manualSkip) {
                val buttonTag = when (segment.type) {
                    SkipType.INTRO -> "skip_intro_button"
                    SkipType.OUTRO -> "skip_outro_button"
                    SkipType.RECAP -> "skip_recap_button"
                }
                val buttonText = when (segment.type) {
                    SkipType.INTRO -> "Skip Intro"
                    SkipType.OUTRO -> "Skip Outro"
                    SkipType.RECAP -> "Skip Recap"
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 96.dp, end = 24.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Surface(
                        onClick = { viewModel.skipCurrentSegment() },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .testTag("player_skip_segment_btn")
                            .testTag(buttonTag)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = buttonText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Snackbar Host for status notifications
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
        )

        // 1. Source Selection Bottom Sheet (Providers, Servers, Quality)
        if (uiState.isSourceSheetVisible) {
            SourceSelectionSheet(
                state = sourceState,
                onSelectProvider = { viewModel.selectProvider(it) },
                onSelectServer = { viewModel.selectServer(it) },
                onSelectQuality = { viewModel.selectQuality(it) },
                onSetPreferredProvider = { id, isGlobal -> viewModel.setPreferredProvider(id, isGlobal) },
                onSetPreferredServer = { name, isGlobal -> viewModel.setPreferredServer(name, isGlobal) },
                onSetPreferredQuality = { q, isGlobal -> viewModel.setPreferredQuality(q, isGlobal) },
                onSwitchToFallback = { viewModel.switchFallbackManually() },
                onDismiss = { viewModel.setSourceSheetVisible(false) }
            )
        }

        // 2. Playback & Advanced Player Settings Sheet
        if (uiState.isSettingsSheetVisible) {
            PlayerSettingsSheet(
                playerState = playerState,
                appSettings = uiState.appSettings,
                sourceSelectionState = sourceState,
                onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
                onSelectAudioTrack = { viewModel.selectAudioTrack(it) },
                onSetPreferredAudioLanguage = { viewModel.setPreferredAudioLanguage(it) },
                onSelectSubtitleTrack = { viewModel.selectSubtitleTrack(it) },
                onToggleSubtitleVisibility = { viewModel.setSubtitleVisible(it) },
                onSelectQualityTrack = { viewModel.selectVideoTrack(it) },
                onSelectQuality = { viewModel.selectQuality(it) },
                onSetPreferredQuality = { q, isGlobal -> viewModel.setPreferredQuality(q, isGlobal) },
                onSetVideoFitMode = { viewModel.setVideoFitMode(it) },
                onSetKeepScreenAwake = { viewModel.setKeepScreenAwake(it) },
                onSetDoubleTapSeek = { viewModel.setDoubleTapSeek(it) },
                onSetDoubleTapSeekSeconds = { viewModel.setDoubleTapSeekSeconds(it) },
                onSetSwipeSeek = { viewModel.setSwipeSeek(it) },
                onSetBrightnessGesture = { viewModel.setBrightnessGesture(it) },
                onSetVolumeGesture = { viewModel.setVolumeGesture(it) },
                onSetLongPressSpeed = { viewModel.setLongPressSpeed(it) },
                onSetLongPressSpeedMultiplier = { viewModel.setLongPressSpeedMultiplier(it) },
                onSetAutoPlayNext = { viewModel.setAutoPlayNext(it) },
                onSetAutoSkipIntro = { viewModel.setAutoSkipIntro(it) },
                onSetAutoSkipOutro = { viewModel.setAutoSkipOutro(it) },
                onSetAutoSkipRecap = { viewModel.setAutoSkipRecap(it) },
                onSetSkipOnce = { viewModel.setSkipOnce(it) },
                onSetManualSkip = { viewModel.setManualSkip(it) },
                onStartOver = { viewModel.startOver() },
                onLaunchPip = { launchPip(activity) },
                onLaunchExternalPlayer = { launchExternalPlayer(context, playerState.currentSource) },
                onOpenSubtitleStylingSheet = { viewModel.setSubtitleSettingsSheetVisible(true) },
                onDismiss = { viewModel.setSettingsSheetVisible(false) }
            )
        }

        // 3. Subtitle Settings Sheet
        if (uiState.isSubtitleSettingsSheetVisible) {
            SubtitleSettingsSheet(
                settings = uiState.appSettings.subtitles,
                onSetLanguage = { viewModel.setSubtitleLanguage(it) },
                onSetDelayMs = { viewModel.setSubtitleDelayMs(it) },
                onSetFontSize = { viewModel.setSubtitleFontSize(it) },
                onSetFontStyle = { viewModel.setSubtitleFontStyle(it) },
                onSetTextColor = { viewModel.setSubtitleTextColor(it) },
                onSetTextOpacity = { viewModel.setSubtitleTextOpacity(it) },
                onSetBackgroundColor = { viewModel.setSubtitleBackgroundColor(it) },
                onSetBackgroundOpacity = { viewModel.setSubtitleBackgroundOpacity(it) },
                onSetOutlineEnabled = { viewModel.setSubtitleOutlineEnabled(it) },
                onSetOutlineColor = { viewModel.setSubtitleOutlineColor(it) },
                onSetBottomMarginDp = { viewModel.setSubtitleBottomMarginDp(it) },
                onDismiss = { viewModel.setSubtitleSettingsSheetVisible(false) }
            )
        }

        // 4. Episodes Drawer Modal
        if (uiState.isEpisodesDrawerVisible) {
            EpisodesDrawerModal(
                allEpisodes = uiState.allEpisodes,
                currentEpisodeId = uiState.currentEpisode?.id ?: "",
                onSelectEpisode = { viewModel.selectEpisode(it) },
                onDismiss = { viewModel.setEpisodesDrawerVisible(false) }
            )
        }
    }
}

@Composable
private fun GestureOverlayBadge(
    message: String,
    icon: ImageVector?
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.8f),
        contentColor = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.testTag("gesture_overlay_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun launchPip(activity: Activity?) {
    if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        activity.enterPictureInPictureMode(params)
    }
}

private fun launchExternalPlayer(context: Context, source: VideoSource?) {
    if (source == null) return
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                Uri.parse(source.url),
                if (source.url.contains(".m3u8", ignoreCase = true)) "application/x-mpegURL" else "video/*"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val headerList = mutableListOf<String>()
            source.headers.forEach { (k, v) ->
                headerList.add(k)
                headerList.add(v)
            }
            if (source.headers.none { it.key.equals("Referer", ignoreCase = true) } && !source.referer.isNullOrBlank()) {
                headerList.add("Referer")
                headerList.add(source.referer)
            }
            if (headerList.isNotEmpty()) {
                putExtra("headers", headerList.toTypedArray())
            }
        }
        val chooser = Intent.createChooser(intent, "Play with External Player").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot launch external player: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(UnstableApi::class)
private fun parseResizeMode(mode: String): Int {
    return when (mode.uppercase()) {
        "CROP" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        "FILL" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        "STRETCH" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        "16:9" -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        "4:3" -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }
}

@Composable
private fun PlayerControlsOverlay(
    uiState: PlayerUiState,
    playerState: com.example.domain.player.PlayerState,
    sourceState: com.example.domain.model.SourceSelectionState,
    onNavigateBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Int) -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenSourceSheet: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    onOpenEpisodesDrawer: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            )
            .testTag("player_controls_overlay")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("player_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = uiState.anime?.title ?: "Anime Player",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    uiState.currentEpisode?.let { ep ->
                        Text(
                            text = "Episode ${ep.number.toInt()}${if (ep.title.isNotBlank()) " - ${ep.title}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Provider / Source Selector Button
                IconButton(
                    onClick = onOpenSourceSheet,
                    modifier = Modifier.testTag("player_sources_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Dns,
                        contentDescription = "Sources & Providers",
                        tint = Color.White
                    )
                }

                // Settings Button (Speed, Audio, Subtitles, Quality, Gestures)
                IconButton(
                    onClick = onOpenSettingsSheet,
                    modifier = Modifier.testTag("player_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Player Settings",
                        tint = Color.White
                    )
                }

                // Episodes List Drawer Button
                IconButton(
                    onClick = onOpenEpisodesDrawer,
                    modifier = Modifier.testTag("player_episodes_drawer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoLibrary,
                        contentDescription = "Episodes Drawer",
                        tint = Color.White
                    )
                }
            }
        }

        // Center Controls (Play/Pause, Seek Back/Forward, Buffering, Retry)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (playerState.playbackState) {
                PlaybackState.BUFFERING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Buffering stream...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                PlaybackState.ERROR -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = playerState.error?.message ?: uiState.error ?: "Playback error encountered",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FilledTonalButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag("player_error_retry_btn")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Playback")
                        }
                    }
                }
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Previous Episode
                        IconButton(
                            onClick = onPreviousEpisode,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .testTag("player_prev_ep_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous Episode",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Rewind 10s
                        IconButton(
                            onClick = { onSeekRelative(-10) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .testTag("player_rewind_10s_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Main Play / Pause Button
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(68.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .testTag("player_play_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = { onSeekRelative(10) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .testTag("player_forward_10s_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Next Episode
                        IconButton(
                            onClick = onNextEpisode,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .testTag("player_next_ep_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next Episode",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Seek Bar and Time Indicators
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Active Source / Quality Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ) {
                    Text(
                        text = "${sourceState.activeSource?.serverName ?: "Server"} (${sourceState.selectedQuality})",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.testTag("player_fullscreen_btn")
                ) {
                    Icon(
                        imageVector = if (playerState.isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = "Fullscreen Toggle",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Seek Slider
            val duration = playerState.durationMs.coerceAtLeast(1L)
            val currentPos = playerState.currentPositionMs.coerceIn(0L, duration)

            Slider(
                value = currentPos.toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_seek_slider")
            )

            // Time Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPos),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodesDrawerModal(
    allEpisodes: List<Episode>,
    currentEpisodeId: String,
    onSelectEpisode: (Episode) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Episodes (${allEpisodes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(allEpisodes, key = { it.id }) { episode ->
                    val isSelected = episode.id == currentEpisodeId
                    Surface(
                        onClick = { onSelectEpisode(episode) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_ep_${episode.number.toInt()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ep ${episode.number.toInt()}${if (episode.title.isNotBlank()) ": ${episode.title}" else ""}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (episode.durationMs > 0) {
                                    Text(
                                        text = "${formatTime(episode.lastPositionMs)} / ${formatTime(episode.durationMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (episode.isWatched) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Watched",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
