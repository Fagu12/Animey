package com.example.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppSettings
import com.example.domain.model.SourceSelectionState
import com.example.domain.model.TrackInfo
import com.example.domain.player.PlayerState
import com.example.ui.components.player.AudioTrackSelector
import com.example.ui.components.player.QualitySelector
import com.example.ui.components.player.VideoTrackSelector

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerSettingsSheet(
    playerState: PlayerState,
    appSettings: AppSettings,
    sourceSelectionState: SourceSelectionState? = null,
    onSelectSpeed: (Float) -> Unit,
    onSelectAudioTrack: (TrackInfo?) -> Unit,
    onSetPreferredAudioLanguage: (String) -> Unit = {},
    onSelectSubtitleTrack: (TrackInfo?) -> Unit,
    onToggleSubtitleVisibility: (Boolean) -> Unit,
    onSelectQualityTrack: (String?) -> Unit,
    onSelectQuality: (String) -> Unit = {},
    onSetPreferredQuality: (quality: String, isGlobal: Boolean) -> Unit = { _, _ -> },
    onSetVideoFitMode: (String) -> Unit,
    onSetKeepScreenAwake: (Boolean) -> Unit,
    onSetDoubleTapSeek: (Boolean) -> Unit,
    onSetDoubleTapSeekSeconds: (Int) -> Unit,
    onSetSwipeSeek: (Boolean) -> Unit,
    onSetBrightnessGesture: (Boolean) -> Unit,
    onSetVolumeGesture: (Boolean) -> Unit,
    onSetLongPressSpeed: (Boolean) -> Unit,
    onSetLongPressSpeedMultiplier: (Float) -> Unit,
    onSetAutoPlayNext: (Boolean) -> Unit,
    onSetAutoSkipIntro: (Boolean) -> Unit,
    onSetAutoSkipOutro: (Boolean) -> Unit,
    onSetAutoSkipRecap: (Boolean) -> Unit = {},
    onSetSkipOnce: (Boolean) -> Unit = {},
    onSetManualSkip: (Boolean) -> Unit = {},
    onStartOver: () -> Unit,
    onLaunchPip: () -> Unit,
    onLaunchExternalPlayer: () -> Unit,
    onOpenSubtitleStylingSheet: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("Playback", "Display", "Gestures", "Controls")
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val seekDurations = listOf(5, 10, 15, 30)
    val speedMultipliers = listOf(1.5f, 2.0f, 2.5f, 3.0f)
    val fitModes = listOf("FIT", "CROP", "FILL", "STRETCH", "16:9", "4:3")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("player_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Player Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_sheet_btn")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                        modifier = Modifier.testTag("settings_tab_${title.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // ==================== TAB 0: Playback & Tracks ====================
                        // 1. Speed
                        SectionHeader(icon = Icons.Filled.Speed, title = "Playback Speed")
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            speeds.forEach { speed ->
                                val isSelected = (playerState.playbackSpeed - speed).let { kotlin.math.abs(it) < 0.05f }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectSpeed(speed) },
                                    label = { Text(if (speed == 1.0f) "1.0x (Normal)" else "${speed}x") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("speed_chip_${speed}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Audio Track & Preferred Language Selector
                        AudioTrackSelector(
                            availableTracks = playerState.availableAudioTracks,
                            selectedTrack = playerState.selectedAudioTrack,
                            preferredLanguage = appSettings.preferredAudioLanguage,
                            onSelectTrack = { onSelectAudioTrack(it) },
                            onSetPreferredLanguage = onSetPreferredAudioLanguage
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Subtitle Tracks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(icon = Icons.Filled.Subtitles, title = "Subtitles")
                            Switch(
                                checked = playerState.isSubtitleVisible,
                                onCheckedChange = { onToggleSubtitleVisibility(it) },
                                modifier = Modifier.testTag("subtitle_visibility_switch")
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val allSubtitles = (playerState.availableSubtitles + (playerState.currentSource?.subtitles ?: emptyList())).distinctBy { it.id.ifBlank { it.url } }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TrackItemRow(
                                label = "Off",
                                isSelected = playerState.selectedSubtitle == null || !playerState.isSubtitleVisible,
                                onClick = { onSelectSubtitleTrack(null) },
                                testTag = "subtitle_track_off"
                            )
                            allSubtitles.forEach { sub ->
                                TrackItemRow(
                                    label = sub.label.ifBlank { "Subtitle (${sub.language.ifBlank { "und" }})" },
                                    isSelected = playerState.isSubtitleVisible && playerState.selectedSubtitle?.id == sub.id,
                                    onClick = {
                                        onToggleSubtitleVisibility(true)
                                        onSelectSubtitleTrack(sub)
                                    },
                                    testTag = "subtitle_track_${sub.id.ifBlank { sub.language }}"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onOpenSubtitleStylingSheet,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_subtitle_styling_sheet_btn")
                        ) {
                            Icon(Icons.Filled.Subtitles, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configure Styling, Colors, Delay & Size")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Media3 Stream Video Tracks
                        VideoTrackSelector(
                            availableTracks = playerState.availableVideoTracks,
                            selectedTrack = playerState.selectedVideoTrack,
                            onSelectTrack = { onSelectQualityTrack(it) }
                        )

                        // 5. Video Stream Quality Selector
                        if (sourceSelectionState != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            QualitySelector(
                                availableQualities = sourceSelectionState.availableQualities,
                                selectedQuality = sourceSelectionState.selectedQuality,
                                onSelectQuality = onSelectQuality,
                                onSetPreferredQuality = onSetPreferredQuality
                            )
                        }
                    }

                    1 -> {
                        // ==================== TAB 1: Display & Screen ====================
                        // 1. Video Fit Mode
                        SectionHeader(icon = Icons.Filled.AspectRatio, title = "Video Fit / Aspect Ratio")
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            fitModes.forEach { mode ->
                                val isSelected = appSettings.videoFitMode.equals(mode, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetVideoFitMode(mode) },
                                    label = { Text(mode) },
                                    modifier = Modifier.testTag("fit_mode_chip_${mode.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Keep Screen Awake Toggle
                        SwitchSettingRow(
                            icon = Icons.Filled.ScreenLockPortrait,
                            title = "Keep Screen Awake",
                            subtitle = "Prevent screen from dimming or turning off during video playback",
                            checked = appSettings.keepScreenAwake,
                            onCheckedChange = onSetKeepScreenAwake,
                            testTag = "keep_screen_awake_switch"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Quick Actions: PiP and External Player
                        SectionHeader(icon = Icons.Filled.OpenInNew, title = "External & Special Modes")
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onLaunchPip,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("launch_pip_btn")
                            ) {
                                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PiP Mode")
                            }

                            Button(
                                onClick = onLaunchExternalPlayer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("launch_external_player_btn")
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("External Player")
                            }
                        }
                    }

                    2 -> {
                        // ==================== TAB 2: Gestures ====================
                        // 1. Double Tap Seek
                        SwitchSettingRow(
                            icon = Icons.Filled.TouchApp,
                            title = "Double Tap Seek",
                            subtitle = "Double tap left/right sides of video surface to seek backward/forward",
                            checked = appSettings.gestures.enableDoubleTapSeek,
                            onCheckedChange = onSetDoubleTapSeek,
                            testTag = "double_tap_seek_switch"
                        )

                        if (appSettings.gestures.enableDoubleTapSeek) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Seek Duration:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                seekDurations.forEach { dur ->
                                    val isSel = appSettings.gestures.doubleTapSeekSeconds == dur
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { onSetDoubleTapSeekSeconds(dur) },
                                        label = { Text("${dur}s") },
                                        modifier = Modifier.testTag("seek_duration_${dur}s")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Horizontal Swipe Seek
                        SwitchSettingRow(
                            icon = Icons.Filled.Swipe,
                            title = "Horizontal Swipe Seek",
                            subtitle = "Drag finger left/right across video surface to scrub playback position",
                            checked = appSettings.gestures.enableSwipeSeek,
                            onCheckedChange = onSetSwipeSeek,
                            testTag = "swipe_seek_switch"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Vertical Brightness Gesture
                        SwitchSettingRow(
                            icon = Icons.Filled.Brightness6,
                            title = "Vertical Swipe Brightness",
                            subtitle = "Drag finger up/down on left half of screen to adjust display brightness",
                            checked = appSettings.gestures.enableBrightnessGesture,
                            onCheckedChange = onSetBrightnessGesture,
                            testTag = "brightness_gesture_switch"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Vertical Volume Gesture
                        SwitchSettingRow(
                            icon = Icons.Filled.VolumeUp,
                            title = "Vertical Swipe Volume",
                            subtitle = "Drag finger up/down on right half of screen to adjust audio volume",
                            checked = appSettings.gestures.enableVolumeGesture,
                            onCheckedChange = onSetVolumeGesture,
                            testTag = "volume_gesture_switch"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 5. Long Press Temporary Speed
                        SwitchSettingRow(
                            icon = Icons.Filled.FastForward,
                            title = "Long Press Fast Speed",
                            subtitle = "Press and hold finger on video surface to temporarily boost playback speed",
                            checked = appSettings.gestures.enableLongPressSpeed,
                            onCheckedChange = onSetLongPressSpeed,
                            testTag = "long_press_speed_switch"
                        )

                        if (appSettings.gestures.enableLongPressSpeed) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Fast Speed Multiplier:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                speedMultipliers.forEach { mult ->
                                    val isSel = (appSettings.gestures.longPressSpeedMultiplier - mult).let { kotlin.math.abs(it) < 0.05f }
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { onSetLongPressSpeedMultiplier(mult) },
                                        label = { Text("${mult}x") },
                                        modifier = Modifier.testTag("long_press_multiplier_${mult}x")
                                    )
                                }
                            }
                        }
                    }

                    3 -> {
                        // ==================== TAB 3: Controls & Automation ====================
                        SwitchSettingRow(
                            icon = Icons.Filled.Gesture,
                            title = "Auto-Play Next Episode",
                            subtitle = "Automatically load and play next episode when current episode ends",
                            checked = appSettings.autoPlayNext,
                            onCheckedChange = onSetAutoPlayNext,
                            testTag = "auto_play_next_switch"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        SwitchSettingRow(
                            icon = Icons.Filled.FastForward,
                            title = "Auto-Skip Intro",
                            subtitle = "Automatically skip opening themes when intro segment marker is detected",
                            checked = appSettings.autoSkipIntro,
                            onCheckedChange = onSetAutoSkipIntro,
                            testTag = "auto_skip_intro_switch"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        SwitchSettingRow(
                            icon = Icons.Filled.FastForward,
                            title = "Auto-Skip Outro",
                            subtitle = "Automatically skip ending credits when outro segment marker is detected",
                            checked = appSettings.autoSkipOutro,
                            onCheckedChange = onSetAutoSkipOutro,
                            testTag = "auto_skip_outro_switch"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        SwitchSettingRow(
                            icon = Icons.Filled.FastForward,
                            title = "Auto-Skip Recap",
                            subtitle = "Automatically skip recap when recap segment marker is detected",
                            checked = appSettings.autoSkipRecap,
                            onCheckedChange = onSetAutoSkipRecap,
                            testTag = "auto_skip_recap_switch"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        SwitchSettingRow(
                            icon = Icons.Filled.FastForward,
                            title = "Skip Only Once",
                            subtitle = "Prevent re-skipping when seeking backwards into segment",
                            checked = appSettings.skipOnce,
                            onCheckedChange = onSetSkipOnce,
                            testTag = "skip_once_switch"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        SwitchSettingRow(
                            icon = Icons.Filled.FastForward,
                            title = "Manual Skip Buttons",
                            subtitle = "Show floating skip buttons when inside segment marker",
                            checked = appSettings.manualSkip,
                            onCheckedChange = onSetManualSkip,
                            testTag = "manual_skip_switch"
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(20.dp))

                        // Restart Episode Button
                        Button(
                            onClick = {
                                onStartOver()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_over_btn")
                        ) {
                            Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Over Episode from Beginning", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SwitchSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun TrackItemRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
