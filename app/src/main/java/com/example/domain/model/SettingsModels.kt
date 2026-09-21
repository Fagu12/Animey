package com.example.domain.model

enum class AppThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
    AMOLED
}

enum class StartPage {
    HOME,
    DISCOVER,
    SEARCH,
    LIBRARY,
    HISTORY,
    EXTENSIONS
}

data class PlayerGestureSettings(
    val enableDoubleTapSeek: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val enableSwipeSeek: Boolean = true,
    val enableBrightnessGesture: Boolean = true,
    val enableVolumeGesture: Boolean = true,
    val enableLongPressSpeed: Boolean = true,
    val longPressSpeedMultiplier: Float = 2.0f
)

data class SubtitleSettings(
    val preferredLanguage: String = "en",
    val delayMs: Long = 0L,
    val fontSizeSp: Int = 18,
    val fontStyle: String = "NORMAL", // NORMAL, BOLD, ITALIC, BOLD_ITALIC
    val textColorHex: String = "#FFFFFF",
    val textOpacity: Float = 1.0f,
    val backgroundColorHex: String = "#000000",
    val backgroundOpacity: Float = 0.5f,
    val outlineEnabled: Boolean = true,
    val outlineColorHex: String = "#000000",
    val bottomMarginDp: Int = 24
)

data class TrackingSettings(
    val autoUpdateAniList: Boolean = true,
    val completionPercentage: Int = 85,
    val syncProgress: Boolean = true,
    val autoMarkCompleted: Boolean = true
)

data class DownloadSettings(
    val downloadOverWifiOnly: Boolean = true,
    val maxConcurrentDownloads: Int = 2
)

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val amoledMode: Boolean = false,
    val dynamicColor: Boolean = true,
    val accentColor: String = "INDIGO",
    val startPage: StartPage = StartPage.HOME,
    val defaultQuality: String = "1080p",
    val autoPlayNext: Boolean = true,
    val autoPlayCountdownSec: Int = 5,
    val resumePlayback: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoSkipIntro: Boolean = false,
    val autoSkipOutro: Boolean = false,
    val autoSkipRecap: Boolean = false,
    val skipOnce: Boolean = true,
    val manualSkip: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val videoFitMode: String = "FIT",
    val gestures: PlayerGestureSettings = PlayerGestureSettings(),
    val subtitles: SubtitleSettings = SubtitleSettings(),
    val tracking: TrackingSettings = TrackingSettings(),
    val preferredAudioLanguage: String = "jp",
    val defaultExtensionId: String = "",
    val incognitoMode: Boolean = false,
    val downloads: DownloadSettings = DownloadSettings()
)
