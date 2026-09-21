package com.example.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.AnimeyApplication
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.ui.theme.AccentColor
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import com.example.core.cache.CacheBreakdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val container = (context.applicationContext as AnimeyApplication).container
    val settingsRepo = container.settingsRepository
    val cacheManager = container.cacheManager
    val networkMonitor = container.networkMonitor

    val appSettings by settingsRepo.settings.collectAsState(initial = AppSettings())
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyOnline)
    val scope = rememberCoroutineScope()

    var cacheBreakdown by remember { mutableStateOf(CacheBreakdown()) }
    var confirmClearDialog by remember { mutableStateOf<String?>(null) } // "all", "images", "metadata"
    var showResetSettingsDialog by remember { mutableStateOf(false) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }
    var showSubtitleSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cacheBreakdown = cacheManager.getCacheBreakdown()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen_root")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tracking & Accounts
            item {
                SettingsCategoryCard(title = "Tracking & Accounts") {
                    SettingsNavigationRow(
                        title = "AniList Account & Tracking",
                        value = "Sync progress & library",
                        icon = Icons.Default.Sync,
                        onClick = onNavigateToAccount,
                        tag = "setting_anilist_account"
                    )
                }
            }

            // General & Appearance
            item {
                SettingsCategoryCard(title = "Appearance") {
                    val currentThemeModeDisplay = when (appSettings.themeMode) {
                        AppThemeMode.SYSTEM -> "System Default"
                        AppThemeMode.DARK -> "Dark Theme"
                        AppThemeMode.LIGHT -> "Light Theme"
                        AppThemeMode.AMOLED -> "AMOLED Black"
                    }
                    SettingsNavigationRow(
                        title = "Theme Mode",
                        value = currentThemeModeDisplay,
                        icon = Icons.Default.Brightness4,
                        onClick = {
                            val nextMode = when (appSettings.themeMode) {
                                AppThemeMode.SYSTEM -> AppThemeMode.DARK
                                AppThemeMode.DARK -> AppThemeMode.LIGHT
                                AppThemeMode.LIGHT -> AppThemeMode.AMOLED
                                AppThemeMode.AMOLED -> AppThemeMode.SYSTEM
                            }
                            scope.launch { settingsRepo.setThemeMode(nextMode) }
                        },
                        tag = "setting_theme_mode"
                    )
                    SettingsSwitchRow(
                        title = "Dark Theme",
                        subtitle = "Use dark color palette for OLED/LCD screens",
                        icon = Icons.Default.DarkMode,
                        checked = appSettings.themeMode == AppThemeMode.DARK || appSettings.themeMode == AppThemeMode.AMOLED,
                        onCheckedChange = { isDark ->
                            scope.launch {
                                settingsRepo.setThemeMode(if (isDark) AppThemeMode.DARK else AppThemeMode.LIGHT)
                            }
                        },
                        tag = "setting_dark_theme"
                    )
                    SettingsSwitchRow(
                        title = "Pure Black AMOLED",
                        subtitle = "Pitch black surfaces for extreme power efficiency",
                        icon = Icons.Default.DarkMode,
                        checked = appSettings.amoledMode || appSettings.themeMode == AppThemeMode.AMOLED,
                        onCheckedChange = { isAmoled ->
                            scope.launch {
                                settingsRepo.setAmoledMode(isAmoled)
                            }
                        },
                        tag = "setting_amoled"
                    )
                    SettingsSwitchRow(
                        title = "Dynamic Colors",
                        subtitle = "Harmonize app colors with Android wallpaper (Material You)",
                        icon = Icons.Default.Palette,
                        checked = appSettings.dynamicColor,
                        onCheckedChange = { dynamic ->
                            scope.launch {
                                settingsRepo.setDynamicColor(dynamic)
                            }
                        },
                        tag = "setting_dynamic_color"
                    )

                    val currentAccent = AccentColor.fromId(appSettings.accentColor)
                    SettingsNavigationRow(
                        title = "Accent Color",
                        value = currentAccent.displayName,
                        icon = Icons.Default.Palette,
                        onClick = {
                            val allAccents = AccentColor.entries
                            val currentIndex = allAccents.indexOf(currentAccent)
                            val nextAccent = allAccents[(currentIndex + 1) % allAccents.size]
                            scope.launch { settingsRepo.setAccentColor(nextAccent.id) }
                        },
                        tag = "setting_accent_color"
                    )

                    // Visual Accent Color Palette Swatches
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("setting_accent_swatches_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccentColor.entries.forEach { accent ->
                            val isSelected = accent.id.equals(appSettings.accentColor, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accent.previewColor)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        scope.launch { settingsRepo.setAccentColor(accent.id) }
                                    }
                                    .testTag("setting_accent_chip_${accent.id.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Player & Playback
            item {
                SettingsCategoryCard(title = "Player & Playback") {
                    SettingsSwitchRow(
                        title = "Auto Play Next Episode",
                        subtitle = "Automatically load next episode with a countdown",
                        icon = Icons.Default.PlayCircleOutline,
                        checked = appSettings.autoPlayNext,
                        onCheckedChange = { scope.launch { settingsRepo.setAutoPlayNext(it) } },
                        tag = "setting_autoplay_next"
                    )
                    SettingsSwitchRow(
                        title = "Automatic Intro Skip",
                        subtitle = "Skip opening themes automatically",
                        icon = Icons.Default.FastForward,
                        checked = appSettings.autoSkipIntro,
                        onCheckedChange = { scope.launch { settingsRepo.setAutoSkipIntro(it) } },
                        tag = "setting_auto_skip_intro"
                    )
                    SettingsSwitchRow(
                        title = "Automatic Outro Skip",
                        subtitle = "Skip ending themes automatically",
                        icon = Icons.Default.FastForward,
                        checked = appSettings.autoSkipOutro,
                        onCheckedChange = { scope.launch { settingsRepo.setAutoSkipOutro(it) } },
                        tag = "setting_auto_skip_outro"
                    )
                    SettingsSwitchRow(
                        title = "Automatic Recap Skip",
                        subtitle = "Skip episode recaps automatically",
                        icon = Icons.Default.FastForward,
                        checked = appSettings.autoSkipRecap,
                        onCheckedChange = { scope.launch { settingsRepo.setAutoSkipRecap(it) } },
                        tag = "setting_auto_skip_recap"
                    )
                    SettingsSwitchRow(
                        title = "Skip Only Once",
                        subtitle = "Prevent infinite loop when seeking back into skipped segments",
                        icon = Icons.Default.FastForward,
                        checked = appSettings.skipOnce,
                        onCheckedChange = { scope.launch { settingsRepo.setSkipOnce(it) } },
                        tag = "setting_skip_once"
                    )
                    SettingsSwitchRow(
                        title = "Manual Skip Buttons",
                        subtitle = "Show manual skip buttons on current segments",
                        icon = Icons.Default.FastForward,
                        checked = appSettings.manualSkip,
                        onCheckedChange = { scope.launch { settingsRepo.setManualSkip(it) } },
                        tag = "setting_manual_skip"
                    )
                    SettingsNavigationRow(
                        title = "Default Quality",
                        value = appSettings.defaultQuality,
                        icon = Icons.Default.Tune,
                        onClick = {
                            val nextQ = when (appSettings.defaultQuality) {
                                "1080p" -> "720p"
                                "720p" -> "480p"
                                "480p" -> "360p"
                                else -> "1080p"
                            }
                            scope.launch { settingsRepo.setDefaultQuality(nextQ) }
                        },
                        tag = "setting_default_quality"
                    )
                }
            }

            // Gestures & Controls
            item {
                SettingsCategoryCard(title = "Gestures & Controls") {
                    SettingsSwitchRow(
                        title = "Double Tap Seek",
                        subtitle = "Double tap left/right to skip time",
                        icon = Icons.Default.Gesture,
                        checked = appSettings.gestures.enableDoubleTapSeek,
                        onCheckedChange = { scope.launch { settingsRepo.setDoubleTapSeek(it) } },
                        tag = "setting_double_tap_seek"
                    )
                    SettingsNavigationRow(
                        title = "Seek Duration",
                        value = "${appSettings.gestures.doubleTapSeekSeconds} seconds",
                        icon = Icons.Default.Gesture,
                        onClick = {
                            val nextSec = when (appSettings.gestures.doubleTapSeekSeconds) {
                                5 -> 10
                                10 -> 15
                                15 -> 30
                                else -> 5
                            }
                            scope.launch { settingsRepo.setDoubleTapSeekSeconds(nextSec) }
                        },
                        tag = "setting_seek_duration"
                    )
                }
            }

            // Subtitles & Audio
            item {
                SettingsCategoryCard(title = "Audio & Subtitles") {
                    SettingsNavigationRow(
                        title = "Preferred Audio Language",
                        value = appSettings.preferredAudioLanguage.uppercase(),
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        onClick = {
                            val nextLang = if (appSettings.preferredAudioLanguage == "jp") "en" else "jp"
                            scope.launch { settingsRepo.setPreferredAudioLanguage(nextLang) }
                        },
                        tag = "setting_audio_lang"
                    )
                    SettingsNavigationRow(
                        title = "Subtitle Styling, Size & Delay",
                        value = "${appSettings.subtitles.fontSizeSp}sp • ${appSettings.subtitles.textColorHex} • ${if (appSettings.subtitles.delayMs != 0L) "${appSettings.subtitles.delayMs}ms" else "No Delay"}",
                        icon = Icons.Default.Subtitles,
                        onClick = { showSubtitleSheet = true },
                        tag = "setting_subtitles"
                    )
                }
            }

            // Tracking & Sync
            item {
                SettingsCategoryCard(title = "Tracking & Sync") {
                    SettingsNavigationRow(
                        title = "AniList Account & Integration",
                        value = "Manage Account",
                        icon = Icons.Default.Sync,
                        onClick = onNavigateToAccount,
                        tag = "setting_anilist_auth"
                    )
                    SettingsSwitchRow(
                        title = "Auto Update AniList",
                        subtitle = "Automatically update episode progress on AniList during watch",
                        icon = Icons.Default.Sync,
                        checked = appSettings.tracking.autoUpdateAniList,
                        onCheckedChange = { scope.launch { settingsRepo.setAutoUpdateAniList(it) } },
                        tag = "setting_auto_update_anilist"
                    )
                    SettingsSwitchRow(
                        title = "Sync Progress During Playback",
                        subtitle = "Periodically sync watch position while video is playing",
                        icon = Icons.Default.Sync,
                        checked = appSettings.tracking.syncProgress,
                        onCheckedChange = { scope.launch { settingsRepo.setSyncProgress(it) } },
                        tag = "setting_sync_progress"
                    )
                    SettingsSwitchRow(
                        title = "Auto Mark Completed",
                        subtitle = "Mark episode as watched when completion threshold is reached",
                        icon = Icons.Default.Tune,
                        checked = appSettings.tracking.autoMarkCompleted,
                        onCheckedChange = { scope.launch { settingsRepo.setAutoMarkCompleted(it) } },
                        tag = "setting_auto_mark_completed"
                    )
                    SettingsNavigationRow(
                        title = "Completion Threshold",
                        value = "${appSettings.tracking.completionPercentage}%",
                        icon = Icons.Default.Tune,
                        onClick = {
                            val nextThreshold = when (appSettings.tracking.completionPercentage) {
                                85 -> 90
                                90 -> 95
                                95 -> 70
                                70 -> 80
                                else -> 85
                            }
                            scope.launch { settingsRepo.setCompletionPercentage(nextThreshold) }
                        },
                        tag = "setting_completion_percentage"
                    )
                }
            }

            // Privacy & History
            item {
                SettingsCategoryCard(title = "Privacy & History") {
                    SettingsSwitchRow(
                        title = "Incognito Mode",
                        subtitle = "Do not record watch history or update AniList progress during playback",
                        icon = Icons.Default.VisibilityOff,
                        checked = appSettings.incognitoMode,
                        onCheckedChange = { scope.launch { settingsRepo.setIncognitoMode(it) } },
                        tag = "setting_incognito_mode"
                    )
                }
            }

            // Downloads
            item {
                SettingsCategoryCard(title = "Downloads") {
                    SettingsSwitchRow(
                        title = "Download over Wi-Fi Only",
                        subtitle = "Only download episodes when connected to unmetered Wi-Fi",
                        icon = Icons.Default.Download,
                        checked = appSettings.downloads.downloadOverWifiOnly,
                        onCheckedChange = { scope.launch { settingsRepo.setDownloadOverWifiOnly(it) } },
                        tag = "setting_download_wifi_only"
                    )
                    SettingsNavigationRow(
                        title = "Max Concurrent Downloads",
                        value = "${appSettings.downloads.maxConcurrentDownloads}",
                        subtitle = "Maximum active background download tasks",
                        icon = Icons.Default.Tune,
                        onClick = {
                            val nextMax = when (appSettings.downloads.maxConcurrentDownloads) {
                                1 -> 2
                                2 -> 3
                                3 -> 4
                                else -> 1
                            }
                            scope.launch { settingsRepo.setMaxConcurrentDownloads(nextMax) }
                        },
                        tag = "setting_max_concurrent_downloads"
                    )
                }
            }

            // Storage & Cache Management
            item {
                SettingsCategoryCard(title = "Storage & Cache") {
                    SettingsNavigationRow(
                        title = "Cache Size",
                        value = cacheBreakdown.totalSizeFormatted,
                        subtitle = "Images: ${cacheBreakdown.imageSizeFormatted} • Metadata: ${cacheBreakdown.metadataSizeFormatted} (${cacheBreakdown.metadataSummary})",
                        icon = Icons.Default.Storage,
                        onClick = {
                            scope.launch {
                                cacheBreakdown = cacheManager.getCacheBreakdown()
                                statusFeedback = "Cache recalculated: ${cacheBreakdown.totalSizeFormatted}"
                            }
                        },
                        tag = "setting_cache_size"
                    )
                    SettingsNavigationRow(
                        title = "Clear Cache",
                        value = "Free up space",
                        subtitle = "Clear images, temporary files, and unpinned metadata",
                        icon = Icons.Default.CleaningServices,
                        onClick = { confirmClearDialog = "all" },
                        tag = "setting_clear_all_cache"
                    )
                    SettingsNavigationRow(
                        title = "Clear Images",
                        value = cacheBreakdown.imageSizeFormatted,
                        subtitle = "Remove cached cover art, thumbnails, and posters",
                        icon = Icons.Default.Image,
                        onClick = { confirmClearDialog = "images" },
                        tag = "setting_clear_images"
                    )
                    SettingsNavigationRow(
                        title = "Clear Metadata",
                        value = "${cacheBreakdown.metadataAnimeCount} anime",
                        subtitle = "Remove unpinned browse metadata (preserves library, history & downloads)",
                        icon = Icons.Default.Delete,
                        onClick = { confirmClearDialog = "metadata" },
                        tag = "setting_clear_metadata"
                    )
                }
            }

            // Extensions & Network
            item {
                SettingsCategoryCard(title = "Extensions & Network") {
                    SettingsNavigationRow(
                        title = "Network Status",
                        value = if (isOnline) "Connected (Online)" else "Offline",
                        subtitle = if (isOnline) "Internet connection active" else "Running in offline mode (Library, History & Downloads accessible)",
                        icon = if (isOnline) Icons.Default.Wifi else Icons.Default.CloudOff,
                        onClick = { /* Status view */ },
                        tag = "setting_network_status"
                    )
                    SettingsNavigationRow(
                        title = "Network & Proxy",
                        value = "Default (Direct)",
                        icon = Icons.Default.NetworkCheck,
                        onClick = { /* Network settings */ },
                        tag = "setting_network"
                    )
                    SettingsNavigationRow(
                        title = "Mangayomi Runtime Diagnostics",
                        subtitle = "Run ES6, Rhino, Preamble & Live Extension Self-Tests",
                        icon = Icons.Default.Extension,
                        onClick = onNavigateToDiagnostics,
                        tag = "setting_mangayomi_diagnostics"
                    )
                }
            }

            // Advanced & System
            item {
                SettingsCategoryCard(title = "Advanced & Subsystems") {
                    SettingsNavigationRow(
                        title = "Reset All Settings",
                        value = "Defaults",
                        subtitle = "Restore all preferences and subsystems to factory defaults",
                        icon = Icons.Default.RestartAlt,
                        onClick = { showResetSettingsDialog = true },
                        tag = "setting_reset_all_settings"
                    )
                }
            }
        }
    }

    // Confirm Clear Dialogs
    when (confirmClearDialog) {
        "all" -> {
            AlertDialog(
                onDismissRequest = { confirmClearDialog = null },
                title = { Text("Clear All Cache?") },
                text = { Text("This will clear cached images and unpinned browse metadata. Your Library, History, and Downloaded episodes will NOT be deleted.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmClearDialog = null
                            scope.launch {
                                cacheManager.clearAllCache()
                                cacheBreakdown = cacheManager.getCacheBreakdown()
                                statusFeedback = "All cache cleared"
                            }
                        }
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClearDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
        "images" -> {
            AlertDialog(
                onDismissRequest = { confirmClearDialog = null },
                title = { Text("Clear Image Cache?") },
                text = { Text("This will clear cached covers and banners (${cacheBreakdown.imageSizeFormatted}). Images will reload when connected.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmClearDialog = null
                            scope.launch {
                                cacheManager.clearImages()
                                cacheBreakdown = cacheManager.getCacheBreakdown()
                                statusFeedback = "Image cache cleared"
                            }
                        }
                    ) {
                        Text("Clear Images")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClearDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
        "metadata" -> {
            AlertDialog(
                onDismissRequest = { confirmClearDialog = null },
                title = { Text("Clear Cached Metadata?") },
                text = { Text("This will remove unpinned browse anime and episode details. Any anime in your Library, Watch History, or Downloads will be safely preserved.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmClearDialog = null
                            scope.launch {
                                val clearedCount = cacheManager.clearMetadata()
                                cacheBreakdown = cacheManager.getCacheBreakdown()
                                statusFeedback = "Cleared non-essential metadata"
                            }
                        }
                    ) {
                        Text("Clear Metadata")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClearDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showResetSettingsDialog = false },
            title = { Text("Reset All Settings?") },
            text = { Text("Are you sure you want to restore all settings to their default values? Your library, watch history, and downloaded episodes will not be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetSettingsDialog = false
                        scope.launch {
                            settingsRepo.resetAllSettings()
                            statusFeedback = "All settings reset to defaults"
                        }
                    },
                    modifier = Modifier.testTag("confirm_reset_all_settings_button")
                ) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetSettingsDialog = false },
                    modifier = Modifier.testTag("cancel_reset_all_settings_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSubtitleSheet) {
        SubtitleSettingsSheet(
            settings = appSettings.subtitles,
            onSetLanguage = { scope.launch { settingsRepo.setSubtitleLanguage(it) } },
            onSetDelayMs = { scope.launch { settingsRepo.setSubtitleDelayMs(it) } },
            onSetFontSize = { scope.launch { settingsRepo.setSubtitleFontSize(it) } },
            onSetFontStyle = { scope.launch { settingsRepo.setSubtitleFontStyle(it) } },
            onSetTextColor = { scope.launch { settingsRepo.setSubtitleTextColor(it) } },
            onSetTextOpacity = { scope.launch { settingsRepo.setSubtitleTextOpacity(it) } },
            onSetBackgroundColor = { scope.launch { settingsRepo.setSubtitleBackgroundColor(it) } },
            onSetBackgroundOpacity = { scope.launch { settingsRepo.setSubtitleBackgroundOpacity(it) } },
            onSetOutlineEnabled = { scope.launch { settingsRepo.setSubtitleOutlineEnabled(it) } },
            onSetOutlineColor = { scope.launch { settingsRepo.setSubtitleOutlineColor(it) } },
            onSetBottomMarginDp = { scope.launch { settingsRepo.setSubtitleBottomMarginDp(it) } },
            onDismiss = { showSubtitleSheet = false }
        )
    }
}

@Composable
fun SettingsCategoryCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("${tag}_switch")
        )
    }
}

@Composable
fun SettingsNavigationRow(
    title: String,
    value: String? = null,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
