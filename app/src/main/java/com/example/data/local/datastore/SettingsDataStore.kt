package com.example.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.PlayerGestureSettings
import com.example.domain.model.StartPage
import com.example.domain.model.SubtitleSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "animey_user_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val START_PAGE = stringPreferencesKey("start_page")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val AUTO_PLAY_COUNTDOWN_SEC = intPreferencesKey("auto_play_countdown_sec")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val AUTO_SKIP_INTRO = booleanPreferencesKey("auto_skip_intro")
        val AUTO_SKIP_OUTRO = booleanPreferencesKey("auto_skip_outro")
        val AUTO_SKIP_RECAP = booleanPreferencesKey("auto_skip_recap")
        val SKIP_ONCE = booleanPreferencesKey("skip_once")
        val MANUAL_SKIP = booleanPreferencesKey("manual_skip")
        val PREFERRED_AUDIO_LANG = stringPreferencesKey("preferred_audio_lang")
        val DEFAULT_EXTENSION_ID = stringPreferencesKey("default_extension_id")
        val PREFERRED_SERVER = stringPreferencesKey("preferred_server")

        // Display & Controls
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val VIDEO_FIT_MODE = stringPreferencesKey("video_fit_mode")

        // Gestures
        val GESTURE_DOUBLE_TAP = booleanPreferencesKey("gesture_double_tap")
        val GESTURE_SEEK_SECONDS = intPreferencesKey("gesture_seek_seconds")
        val GESTURE_SWIPE_SEEK = booleanPreferencesKey("gesture_swipe_seek")
        val GESTURE_BRIGHTNESS = booleanPreferencesKey("gesture_brightness")
        val GESTURE_VOLUME = booleanPreferencesKey("gesture_volume")
        val GESTURE_LONG_PRESS_SPEED = booleanPreferencesKey("gesture_long_press_speed")
        val GESTURE_SPEED_MULTIPLIER = floatPreferencesKey("gesture_speed_multiplier")

        // Subtitles
        val SUBTITLE_LANG = stringPreferencesKey("subtitle_lang")
        val SUBTITLE_DELAY_MS = longPreferencesKey("subtitle_delay_ms")
        val SUBTITLE_FONT_SIZE = intPreferencesKey("subtitle_font_size")
        val SUBTITLE_FONT_STYLE = stringPreferencesKey("subtitle_font_style")
        val SUBTITLE_TEXT_COLOR = stringPreferencesKey("subtitle_text_color")
        val SUBTITLE_TEXT_OPACITY = floatPreferencesKey("subtitle_text_opacity")
        val SUBTITLE_BG_COLOR = stringPreferencesKey("subtitle_bg_color")
        val SUBTITLE_BG_OPACITY = floatPreferencesKey("subtitle_bg_opacity")
        val SUBTITLE_OUTLINE = booleanPreferencesKey("subtitle_outline")
        val SUBTITLE_OUTLINE_COLOR = stringPreferencesKey("subtitle_outline_color")
        val SUBTITLE_BOTTOM_MARGIN = intPreferencesKey("subtitle_bottom_margin")

        // Tracking / AniList
        val TRACKING_AUTO_UPDATE_ANILIST = booleanPreferencesKey("tracking_auto_update_anilist")
        val TRACKING_COMPLETION_PERCENTAGE = intPreferencesKey("tracking_completion_percentage")
        val TRACKING_SYNC_PROGRESS = booleanPreferencesKey("tracking_sync_progress")
        val TRACKING_AUTO_MARK_COMPLETED = booleanPreferencesKey("tracking_auto_mark_completed")
        val ANILIST_ACCESS_TOKEN = stringPreferencesKey("anilist_access_token")
        val ANILIST_USER_DATA = stringPreferencesKey("anilist_user_data")

        // Privacy & Downloads
        val INCOGNITO_MODE = booleanPreferencesKey("incognito_mode")
        val DOWNLOAD_OVER_WIFI_ONLY = booleanPreferencesKey("download_over_wifi_only")
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeModeName = prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.name
            val themeMode = try {
                AppThemeMode.valueOf(themeModeName)
            } catch (_: Exception) {
                AppThemeMode.DARK
            }

            val startPageName = prefs[Keys.START_PAGE] ?: StartPage.HOME.name
            val startPage = try {
                StartPage.valueOf(startPageName)
            } catch (_: Exception) {
                StartPage.HOME
            }

            val gestures = PlayerGestureSettings(
                enableDoubleTapSeek = prefs[Keys.GESTURE_DOUBLE_TAP] ?: true,
                doubleTapSeekSeconds = prefs[Keys.GESTURE_SEEK_SECONDS] ?: 10,
                enableSwipeSeek = prefs[Keys.GESTURE_SWIPE_SEEK] ?: true,
                enableBrightnessGesture = prefs[Keys.GESTURE_BRIGHTNESS] ?: true,
                enableVolumeGesture = prefs[Keys.GESTURE_VOLUME] ?: true,
                enableLongPressSpeed = prefs[Keys.GESTURE_LONG_PRESS_SPEED] ?: true,
                longPressSpeedMultiplier = prefs[Keys.GESTURE_SPEED_MULTIPLIER] ?: 2.0f
            )

            val subtitles = SubtitleSettings(
                preferredLanguage = prefs[Keys.SUBTITLE_LANG] ?: "en",
                delayMs = prefs[Keys.SUBTITLE_DELAY_MS] ?: 0L,
                fontSizeSp = prefs[Keys.SUBTITLE_FONT_SIZE] ?: 18,
                fontStyle = prefs[Keys.SUBTITLE_FONT_STYLE] ?: "NORMAL",
                textColorHex = prefs[Keys.SUBTITLE_TEXT_COLOR] ?: "#FFFFFF",
                textOpacity = prefs[Keys.SUBTITLE_TEXT_OPACITY] ?: 1.0f,
                backgroundColorHex = prefs[Keys.SUBTITLE_BG_COLOR] ?: "#000000",
                backgroundOpacity = prefs[Keys.SUBTITLE_BG_OPACITY] ?: 0.5f,
                outlineEnabled = prefs[Keys.SUBTITLE_OUTLINE] ?: true,
                outlineColorHex = prefs[Keys.SUBTITLE_OUTLINE_COLOR] ?: "#000000",
                bottomMarginDp = prefs[Keys.SUBTITLE_BOTTOM_MARGIN] ?: 24
            )

            val tracking = com.example.domain.model.TrackingSettings(
                autoUpdateAniList = prefs[Keys.TRACKING_AUTO_UPDATE_ANILIST] ?: true,
                completionPercentage = prefs[Keys.TRACKING_COMPLETION_PERCENTAGE] ?: 85,
                syncProgress = prefs[Keys.TRACKING_SYNC_PROGRESS] ?: true,
                autoMarkCompleted = prefs[Keys.TRACKING_AUTO_MARK_COMPLETED] ?: true
            )

            AppSettings(
                themeMode = themeMode,
                amoledMode = prefs[Keys.AMOLED_MODE] ?: false,
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
                accentColor = prefs[Keys.ACCENT_COLOR] ?: "INDIGO",
                startPage = startPage,
                defaultQuality = prefs[Keys.DEFAULT_QUALITY] ?: "1080p",
                autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
                autoPlayCountdownSec = prefs[Keys.AUTO_PLAY_COUNTDOWN_SEC] ?: 5,
                resumePlayback = prefs[Keys.RESUME_PLAYBACK] ?: true,
                defaultPlaybackSpeed = prefs[Keys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
                autoSkipIntro = prefs[Keys.AUTO_SKIP_INTRO] ?: false,
                autoSkipOutro = prefs[Keys.AUTO_SKIP_OUTRO] ?: false,
                autoSkipRecap = prefs[Keys.AUTO_SKIP_RECAP] ?: false,
                skipOnce = prefs[Keys.SKIP_ONCE] ?: true,
                manualSkip = prefs[Keys.MANUAL_SKIP] ?: true,
                keepScreenAwake = prefs[Keys.KEEP_SCREEN_AWAKE] ?: true,
                videoFitMode = prefs[Keys.VIDEO_FIT_MODE] ?: "FIT",
                gestures = gestures,
                subtitles = subtitles,
                tracking = tracking,
                preferredAudioLanguage = prefs[Keys.PREFERRED_AUDIO_LANG] ?: "jp",
                defaultExtensionId = prefs[Keys.DEFAULT_EXTENSION_ID] ?: "",
                incognitoMode = prefs[Keys.INCOGNITO_MODE] ?: false,
                downloads = com.example.domain.model.DownloadSettings(
                    downloadOverWifiOnly = prefs[Keys.DOWNLOAD_OVER_WIFI_ONLY] ?: true,
                    maxConcurrentDownloads = prefs[Keys.MAX_CONCURRENT_DOWNLOADS] ?: 2
                )
            )
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AMOLED_MODE] = enabled }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAccentColor(accent: String) {
        context.settingsDataStore.edit { it[Keys.ACCENT_COLOR] = accent }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setAutoSkipIntro(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_SKIP_INTRO] = enabled }
    }

    suspend fun setAutoSkipOutro(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_SKIP_OUTRO] = enabled }
    }

    suspend fun setAutoSkipRecap(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_SKIP_RECAP] = enabled }
    }

    suspend fun setSkipOnce(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SKIP_ONCE] = enabled }
    }

    suspend fun setManualSkip(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MANUAL_SKIP] = enabled }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_QUALITY] = quality }
    }

    suspend fun setDoubleTapSeek(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GESTURE_DOUBLE_TAP] = enabled }
    }

    suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        context.settingsDataStore.edit { it[Keys.GESTURE_SEEK_SECONDS] = seconds }
    }

    suspend fun setSwipeSeek(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GESTURE_SWIPE_SEEK] = enabled }
    }

    suspend fun setBrightnessGesture(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GESTURE_BRIGHTNESS] = enabled }
    }

    suspend fun setVolumeGesture(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GESTURE_VOLUME] = enabled }
    }

    suspend fun setLongPressSpeed(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GESTURE_LONG_PRESS_SPEED] = enabled }
    }

    suspend fun setLongPressSpeedMultiplier(multiplier: Float) {
        context.settingsDataStore.edit { it[Keys.GESTURE_SPEED_MULTIPLIER] = multiplier }
    }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.KEEP_SCREEN_AWAKE] = enabled }
    }

    suspend fun setVideoFitMode(fitMode: String) {
        context.settingsDataStore.edit { it[Keys.VIDEO_FIT_MODE] = fitMode }
    }

    suspend fun setPreferredAudioLanguage(language: String) {
        context.settingsDataStore.edit { it[Keys.PREFERRED_AUDIO_LANG] = language }
    }

    suspend fun setSubtitleLanguage(language: String) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_LANG] = language }
    }

    suspend fun setSubtitleDelayMs(delayMs: Long) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_DELAY_MS] = delayMs }
    }

    suspend fun setSubtitleFontSize(sizeSp: Int) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_FONT_SIZE] = sizeSp }
    }

    suspend fun setSubtitleFontStyle(fontStyle: String) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_FONT_STYLE] = fontStyle }
    }

    suspend fun setSubtitleTextColor(colorHex: String) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_TEXT_COLOR] = colorHex }
    }

    suspend fun setSubtitleTextOpacity(opacity: Float) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_TEXT_OPACITY] = opacity }
    }

    suspend fun setSubtitleBackgroundColor(colorHex: String) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_BG_COLOR] = colorHex }
    }

    suspend fun setSubtitleBackgroundOpacity(opacity: Float) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_BG_OPACITY] = opacity }
    }

    suspend fun setSubtitleOutlineEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_OUTLINE] = enabled }
    }

    suspend fun setSubtitleOutlineColor(colorHex: String) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_OUTLINE_COLOR] = colorHex }
    }

    suspend fun setSubtitleBottomMarginDp(marginDp: Int) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_BOTTOM_MARGIN] = marginDp }
    }

    suspend fun setDefaultExtensionId(extensionId: String) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_EXTENSION_ID] = extensionId }
    }

    suspend fun setAutoUpdateAniList(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TRACKING_AUTO_UPDATE_ANILIST] = enabled }
    }

    suspend fun setCompletionPercentage(percentage: Int) {
        context.settingsDataStore.edit { it[Keys.TRACKING_COMPLETION_PERCENTAGE] = percentage.coerceIn(1, 100) }
    }

    suspend fun setSyncProgress(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TRACKING_SYNC_PROGRESS] = enabled }
    }

    suspend fun setAutoMarkCompleted(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TRACKING_AUTO_MARK_COMPLETED] = enabled }
    }

    val preferredServerFlow: Flow<String?> = context.settingsDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.PREFERRED_SERVER] }

    suspend fun setPreferredServer(server: String?) {
        context.settingsDataStore.edit { prefs ->
            if (server == null) {
                prefs.remove(Keys.PREFERRED_SERVER)
            } else {
                prefs[Keys.PREFERRED_SERVER] = server
            }
        }
    }

    fun getAnimePreference(animeId: String, keyType: String): Flow<String?> {
        val sanitizedId = animeId.replace(":", "_").replace("/", "_")
        val key = stringPreferencesKey("anime_${keyType}_$sanitizedId")
        return context.settingsDataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { prefs -> prefs[key] }
    }

    suspend fun setAnimePreference(animeId: String, keyType: String, value: String?) {
        val sanitizedId = animeId.replace(":", "_").replace("/", "_")
        val key = stringPreferencesKey("anime_${keyType}_$sanitizedId")
        context.settingsDataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(key)
            } else {
                prefs[key] = value
            }
        }
    }

    suspend fun clearAnimePreferences(animeId: String) {
        val sanitizedId = animeId.replace(":", "_").replace("/", "_")
        context.settingsDataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("anime_provider_$sanitizedId"))
            prefs.remove(stringPreferencesKey("anime_server_$sanitizedId"))
            prefs.remove(stringPreferencesKey("anime_quality_$sanitizedId"))
        }
    }

    val aniListAccessTokenFlow: Flow<String?> = context.settingsDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.ANILIST_ACCESS_TOKEN] }

    suspend fun setAniListAccessToken(token: String?) {
        context.settingsDataStore.edit { prefs ->
            if (token == null) {
                prefs.remove(Keys.ANILIST_ACCESS_TOKEN)
            } else {
                prefs[Keys.ANILIST_ACCESS_TOKEN] = token
            }
        }
    }

    val aniListUserDataFlow: Flow<String?> = context.settingsDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.ANILIST_USER_DATA] }

    suspend fun setAniListUserData(jsonString: String?) {
        context.settingsDataStore.edit { prefs ->
            if (jsonString == null) {
                prefs.remove(Keys.ANILIST_USER_DATA)
            } else {
                prefs[Keys.ANILIST_USER_DATA] = jsonString
            }
        }
    }

    suspend fun setIncognitoMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.INCOGNITO_MODE] = enabled }
    }

    suspend fun setDownloadOverWifiOnly(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DOWNLOAD_OVER_WIFI_ONLY] = enabled }
    }

    suspend fun setMaxConcurrentDownloads(max: Int) {
        context.settingsDataStore.edit { it[Keys.MAX_CONCURRENT_DOWNLOADS] = max }
    }

    suspend fun resetAllSettings() {
        context.settingsDataStore.edit { prefs ->
            val savedToken = prefs[Keys.ANILIST_ACCESS_TOKEN]
            val savedUser = prefs[Keys.ANILIST_USER_DATA]
            prefs.clear()
            if (savedToken != null) prefs[Keys.ANILIST_ACCESS_TOKEN] = savedToken
            if (savedUser != null) prefs[Keys.ANILIST_USER_DATA] = savedUser
        }
    }
}
