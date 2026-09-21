package com.example.data.repository

import com.example.data.local.datastore.SettingsDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.settingsFlow

    override suspend fun setThemeMode(mode: AppThemeMode) = dataStore.setThemeMode(mode)

    override suspend fun setAmoledMode(enabled: Boolean) = dataStore.setAmoledMode(enabled)

    override suspend fun setDynamicColor(enabled: Boolean) = dataStore.setDynamicColor(enabled)

    override suspend fun setAccentColor(accent: String) = dataStore.setAccentColor(accent)

    override suspend fun setAutoPlayNext(enabled: Boolean) = dataStore.setAutoPlayNext(enabled)

    override suspend fun setAutoSkipIntro(enabled: Boolean) = dataStore.setAutoSkipIntro(enabled)

    override suspend fun setAutoSkipOutro(enabled: Boolean) = dataStore.setAutoSkipOutro(enabled)

    override suspend fun setAutoSkipRecap(enabled: Boolean) = dataStore.setAutoSkipRecap(enabled)

    override suspend fun setSkipOnce(enabled: Boolean) = dataStore.setSkipOnce(enabled)

    override suspend fun setManualSkip(enabled: Boolean) = dataStore.setManualSkip(enabled)

    override suspend fun setDefaultQuality(quality: String) = dataStore.setDefaultQuality(quality)

    override suspend fun setDoubleTapSeekSeconds(seconds: Int) = dataStore.setDoubleTapSeekSeconds(seconds)

    override suspend fun setDoubleTapSeek(enabled: Boolean) = dataStore.setDoubleTapSeek(enabled)

    override suspend fun setSwipeSeek(enabled: Boolean) = dataStore.setSwipeSeek(enabled)

    override suspend fun setBrightnessGesture(enabled: Boolean) = dataStore.setBrightnessGesture(enabled)

    override suspend fun setVolumeGesture(enabled: Boolean) = dataStore.setVolumeGesture(enabled)

    override suspend fun setLongPressSpeed(enabled: Boolean) = dataStore.setLongPressSpeed(enabled)

    override suspend fun setLongPressSpeedMultiplier(multiplier: Float) = dataStore.setLongPressSpeedMultiplier(multiplier)

    override suspend fun setKeepScreenAwake(enabled: Boolean) = dataStore.setKeepScreenAwake(enabled)

    override suspend fun setVideoFitMode(fitMode: String) = dataStore.setVideoFitMode(fitMode)

    override suspend fun setPreferredAudioLanguage(language: String) = dataStore.setPreferredAudioLanguage(language)

    override suspend fun setSubtitleLanguage(language: String) = dataStore.setSubtitleLanguage(language)

    override suspend fun setSubtitleDelayMs(delayMs: Long) = dataStore.setSubtitleDelayMs(delayMs)

    override suspend fun setSubtitleFontSize(sizeSp: Int) = dataStore.setSubtitleFontSize(sizeSp)

    override suspend fun setSubtitleFontStyle(fontStyle: String) = dataStore.setSubtitleFontStyle(fontStyle)

    override suspend fun setSubtitleTextColor(colorHex: String) = dataStore.setSubtitleTextColor(colorHex)

    override suspend fun setSubtitleTextOpacity(opacity: Float) = dataStore.setSubtitleTextOpacity(opacity)

    override suspend fun setSubtitleBackgroundColor(colorHex: String) = dataStore.setSubtitleBackgroundColor(colorHex)

    override suspend fun setSubtitleBackgroundOpacity(opacity: Float) = dataStore.setSubtitleBackgroundOpacity(opacity)

    override suspend fun setSubtitleOutlineEnabled(enabled: Boolean) = dataStore.setSubtitleOutlineEnabled(enabled)

    override suspend fun setSubtitleOutlineColor(colorHex: String) = dataStore.setSubtitleOutlineColor(colorHex)

    override suspend fun setSubtitleBottomMarginDp(marginDp: Int) = dataStore.setSubtitleBottomMarginDp(marginDp)

    override suspend fun setDefaultExtensionId(extensionId: String) = dataStore.setDefaultExtensionId(extensionId)

    override suspend fun setAutoUpdateAniList(enabled: Boolean) = dataStore.setAutoUpdateAniList(enabled)

    override suspend fun setCompletionPercentage(percentage: Int) = dataStore.setCompletionPercentage(percentage)

    override suspend fun setSyncProgress(enabled: Boolean) = dataStore.setSyncProgress(enabled)

    override suspend fun setAutoMarkCompleted(enabled: Boolean) = dataStore.setAutoMarkCompleted(enabled)
    override suspend fun setIncognitoMode(enabled: Boolean) = dataStore.setIncognitoMode(enabled)
    override suspend fun setDownloadOverWifiOnly(enabled: Boolean) = dataStore.setDownloadOverWifiOnly(enabled)
    override suspend fun setMaxConcurrentDownloads(max: Int) = dataStore.setMaxConcurrentDownloads(max)

    override suspend fun resetAllSettings() = dataStore.resetAllSettings()
}
