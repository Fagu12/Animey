package com.example.domain.repository

import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.StartPage
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setAmoledMode(enabled: Boolean)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setAccentColor(accent: String) {}
    suspend fun setAutoPlayNext(enabled: Boolean)
    suspend fun setAutoSkipIntro(enabled: Boolean)
    suspend fun setAutoSkipOutro(enabled: Boolean)
    suspend fun setAutoSkipRecap(enabled: Boolean)
    suspend fun setSkipOnce(enabled: Boolean)
    suspend fun setManualSkip(enabled: Boolean)
    suspend fun setDefaultQuality(quality: String)
    suspend fun setDoubleTapSeekSeconds(seconds: Int)
    suspend fun setDoubleTapSeek(enabled: Boolean)
    suspend fun setSwipeSeek(enabled: Boolean)
    suspend fun setBrightnessGesture(enabled: Boolean)
    suspend fun setVolumeGesture(enabled: Boolean)
    suspend fun setLongPressSpeed(enabled: Boolean)
    suspend fun setLongPressSpeedMultiplier(multiplier: Float)
    suspend fun setKeepScreenAwake(enabled: Boolean)
    suspend fun setVideoFitMode(fitMode: String)
    suspend fun setPreferredAudioLanguage(language: String)
    suspend fun setSubtitleLanguage(language: String)
    suspend fun setSubtitleDelayMs(delayMs: Long)
    suspend fun setSubtitleFontSize(sizeSp: Int)
    suspend fun setSubtitleFontStyle(fontStyle: String)
    suspend fun setSubtitleTextColor(colorHex: String)
    suspend fun setSubtitleTextOpacity(opacity: Float)
    suspend fun setSubtitleBackgroundColor(colorHex: String)
    suspend fun setSubtitleBackgroundOpacity(opacity: Float)
    suspend fun setSubtitleOutlineEnabled(enabled: Boolean)
    suspend fun setSubtitleOutlineColor(colorHex: String)
    suspend fun setSubtitleBottomMarginDp(marginDp: Int)
    suspend fun setDefaultExtensionId(extensionId: String)
    suspend fun setAutoUpdateAniList(enabled: Boolean)
    suspend fun setCompletionPercentage(percentage: Int)
    suspend fun setSyncProgress(enabled: Boolean)
    suspend fun setAutoMarkCompleted(enabled: Boolean)
    suspend fun setIncognitoMode(enabled: Boolean) {}
    suspend fun setDownloadOverWifiOnly(enabled: Boolean) {}
    suspend fun setMaxConcurrentDownloads(max: Int) {}
    suspend fun resetAllSettings() {}
}
