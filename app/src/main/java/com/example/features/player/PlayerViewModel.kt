package com.example.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.SourceSelectionState
import com.example.domain.model.TrackInfo
import com.example.domain.model.VideoSource
import com.example.domain.player.PlaybackState
import com.example.domain.player.PlayerEngine
import com.example.domain.player.PlayerState
import com.example.domain.player.VideoTrack
import com.example.domain.player.selector.SourceSelector
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.DownloadRepository
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SkipMetadataRepository
import com.example.domain.repository.TrackingRepository
import com.example.domain.model.DownloadStatus
import com.example.player.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isLoading: Boolean = true,
    val anime: Anime? = null,
    val currentEpisode: Episode? = null,
    val allEpisodes: List<Episode> = emptyList(),
    val appSettings: com.example.domain.model.AppSettings = com.example.domain.model.AppSettings(),
    val isControlsVisible: Boolean = true,
    val isSourceSheetVisible: Boolean = false,
    val isEpisodesDrawerVisible: Boolean = false,
    val isSettingsSheetVisible: Boolean = false,
    val isSubtitleSettingsSheetVisible: Boolean = false,
    val statusMessage: String? = null,
    val error: String? = null
)

class PlayerViewModel(
    private val animeId: String,
    private val initialEpisodeId: String,
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    val sourceSelector: SourceSelector,
    private val playerEngine: PlayerEngine,
    private val trackingRepository: TrackingRepository? = null,
    private val skipMetadataRepository: SkipMetadataRepository? = null,
    private val downloadRepository: DownloadRepository? = null
) : ViewModel() {

    val playerController = PlayerController(
        playerEngine = playerEngine,
        historyRepository = historyRepository,
        episodeRepository = episodeRepository,
        settingsRepository = settingsRepository,
        trackingRepository = trackingRepository,
        onAutoPlayNextEpisode = { playNextEpisode() },
        scope = viewModelScope
    )

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = playerEngine.state
    val sourceSelectionState: StateFlow<SourceSelectionState> = sourceSelector.state

    init {
        loadAnimeAndEpisode(initialEpisodeId)
        observePlayerErrorsAndState()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(appSettings = settings) }
            }
        }
    }

    fun loadAnimeAndEpisode(episodeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = null) }

            val anime = animeRepository.getAnimeByIdDirect(animeId)
            val episodes = episodeRepository.getEpisodesForAnimeDirect(animeId)
            val targetEpisode = episodes.find { it.id == episodeId } ?: episodes.firstOrNull()

            _uiState.update {
                it.copy(
                    anime = anime,
                    currentEpisode = targetEpisode,
                    allEpisodes = episodes
                )
            }

            if (targetEpisode != null) {
                loadSourcesForEpisode(targetEpisode)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Episode not found") }
            }
        }
    }

    private fun loadSourcesForEpisode(episode: Episode) {
        viewModelScope.launch {
            // Check if episode is downloaded locally for instant offline playback
            if (downloadRepository != null) {
                val download = downloadRepository.getDownloadDirect(episode.id)
                if (download != null && download.status == DownloadStatus.COMPLETED &&
                    download.localFilePath.isNotBlank() && java.io.File(download.localFilePath).exists()) {
                    val offlineSource = VideoSource(
                        id = "download_${episode.id}",
                        providerId = "offline",
                        serverName = "Downloaded (Offline)",
                        quality = download.quality,
                        url = download.localFilePath
                    )
                    playSource(offlineSource)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
            }

            val selectionState = sourceSelector.loadSourcesForEpisode(
                animeId = animeId,
                episode = episode
            )

            if (selectionState.activeSource != null) {
                playSource(selectionState.activeSource)
                _uiState.update { it.copy(isLoading = false) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = selectionState.error ?: "No valid streams found for episode"
                    )
                }
            }
        }
    }

    private fun playSource(source: VideoSource) {
        val episode = _uiState.value.currentEpisode ?: return
        viewModelScope.launch {
            val finalEpisode = if (episode.skipSegments.isEmpty() && skipMetadataRepository != null) {
                val independentSegments = skipMetadataRepository.getSkipSegments(
                    animeId = animeId,
                    episodeId = episode.id,
                    episodeNumber = episode.number,
                    durationMs = episode.durationMs
                )
                if (independentSegments.isNotEmpty()) {
                    episode.copy(skipSegments = independentSegments)
                } else episode
            } else {
                episode
            }
            playerController.playEpisode(
                animeId = animeId,
                episode = finalEpisode,
                source = source,
                autoPlay = true
            )
        }
    }

    private fun observePlayerErrorsAndState() {
        viewModelScope.launch {
            playerEngine.state.collectLatest { state ->
                if (state.playbackState == PlaybackState.ERROR) {
                    val currentSource = state.currentSource
                    if (currentSource != null) {
                        handleSourceFailure(currentSource)
                    }
                }
            }
        }
    }

    private fun handleSourceFailure(failedSource: VideoSource) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    statusMessage = "Source '${failedSource.serverName}' failed. Switching to alternative source..."
                )
            }

            val fallbackSource = sourceSelector.markSourceFailed(failedSource.id)
            if (fallbackSource != null) {
                playSource(fallbackSource)
                _uiState.update {
                    it.copy(
                        statusMessage = "Switched to ${fallbackSource.serverName} (${fallbackSource.quality})"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = "All available stream sources failed. Please select another provider or server.",
                        isSourceSheetVisible = true
                    )
                }
            }
        }
    }

    // ==================== User Actions ====================

    fun togglePlayPause() {
        if (playerState.value.isPlaying) {
            playerEngine.pause()
        } else {
            playerEngine.play()
        }
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
    }

    fun seekRelative(seconds: Int) {
        val current = playerState.value.currentPositionMs
        val target = (current + (seconds * 1000L)).coerceAtLeast(0L)
        playerEngine.seekTo(target)
    }

    fun skipCurrentSegment() {
        playerController.skipCurrentSegment()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerEngine.setPlaybackSpeed(speed)
    }

    fun selectSubtitleTrack(track: TrackInfo?) {
        playerEngine.selectSubtitle(track?.id)
    }

    fun selectAudioTrack(track: TrackInfo?) {
        playerEngine.selectAudioTrack(track?.id)
    }

    fun setPreferredAudioLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setPreferredAudioLanguage(language)
            val matchingTrack = playerState.value.availableAudioTracks.find {
                it.language.equals(language, ignoreCase = true) || it.label.lowercase().contains(language.lowercase())
            }
            if (matchingTrack != null) {
                playerEngine.selectAudioTrack(matchingTrack.id)
            }
        }
    }

    fun selectVideoTrack(trackId: String?) {
        playerEngine.selectVideoTrack(trackId)
    }

    fun setSubtitleVisible(isVisible: Boolean) {
        playerEngine.setSubtitleVisible(isVisible)
    }

    fun setFullscreen(isFullscreen: Boolean) {
        playerEngine.setFullscreen(isFullscreen)
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isSettingsSheetVisible = visible) }
    }

    fun retryPlayback() {
        val currentSource = sourceSelectionState.value.activeSource
        if (currentSource != null) {
            playSource(currentSource)
        } else {
            val currentEp = _uiState.value.currentEpisode
            if (currentEp != null) {
                loadSourcesForEpisode(currentEp)
            }
        }
    }

    fun playNextEpisode() {
        val current = _uiState.value.currentEpisode ?: return
        val episodes = _uiState.value.allEpisodes
        val currentIndex = episodes.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex + 1 < episodes.size) {
            selectEpisode(episodes[currentIndex + 1])
        }
    }

    fun playPreviousEpisode() {
        val current = _uiState.value.currentEpisode ?: return
        val episodes = _uiState.value.allEpisodes
        val currentIndex = episodes.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            selectEpisode(episodes[currentIndex - 1])
        }
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(isControlsVisible = visible) }
    }

    fun setSourceSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isSourceSheetVisible = visible) }
    }

    fun setEpisodesDrawerVisible(visible: Boolean) {
        _uiState.update { it.copy(isEpisodesDrawerVisible = visible) }
    }

    fun setSubtitleSettingsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isSubtitleSettingsSheetVisible = visible) }
    }

    fun selectEpisode(episode: Episode) {
        _uiState.update { it.copy(isEpisodesDrawerVisible = false) }
        loadAnimeAndEpisode(episode.id)
    }

    fun selectProvider(providerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }
            sourceSelector.selectProvider(providerId)
            val active = sourceSelector.state.value.activeSource
            if (active != null) {
                playSource(active)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectServer(serverName: String) {
        viewModelScope.launch {
            sourceSelector.selectServer(serverName)
            val active = sourceSelector.state.value.activeSource
            if (active != null) {
                playSource(active)
            }
        }
    }

    fun selectQuality(quality: String) {
        sourceSelector.selectQuality(quality)
        val active = sourceSelector.state.value.activeSource
        if (active != null) {
            playSource(active)
        }
    }

    fun setPreferredProvider(providerId: String, isGlobal: Boolean) {
        viewModelScope.launch {
            sourceSelector.setPreferredProvider(animeId, providerId, isGlobal)
        }
    }

    fun setPreferredServer(serverName: String, isGlobal: Boolean) {
        viewModelScope.launch {
            sourceSelector.setPreferredServer(animeId, serverName, isGlobal)
        }
    }

    fun setPreferredQuality(quality: String, isGlobal: Boolean) {
        viewModelScope.launch {
            sourceSelector.setPreferredQuality(animeId, quality, isGlobal)
        }
    }

    fun switchFallbackManually() {
        viewModelScope.launch {
            val next = sourceSelector.selectNextFallbackSource()
            if (next != null) {
                playSource(next)
                _uiState.update {
                    it.copy(
                        statusMessage = "Switched to ${next.serverName} (${next.quality})",
                        error = null
                    )
                }
            } else {
                _uiState.update { it.copy(error = "No alternative sources available.") }
            }
        }
    }

    fun startOver() {
        playerEngine.seekTo(0L)
    }

    fun setDoubleTapSeek(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDoubleTapSeek(enabled) }
    }

    fun setDoubleTapSeekSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setDoubleTapSeekSeconds(seconds) }
    }

    fun setSwipeSeek(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSwipeSeek(enabled) }
    }

    fun setBrightnessGesture(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBrightnessGesture(enabled) }
    }

    fun setVolumeGesture(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVolumeGesture(enabled) }
    }

    fun setLongPressSpeed(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLongPressSpeed(enabled) }
    }

    fun setLongPressSpeedMultiplier(multiplier: Float) {
        viewModelScope.launch { settingsRepository.setLongPressSpeedMultiplier(multiplier) }
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenAwake(enabled) }
    }

    fun setVideoFitMode(mode: String) {
        viewModelScope.launch { settingsRepository.setVideoFitMode(mode) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPlayNext(enabled) }
    }

    fun setAutoSkipIntro(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSkipIntro(enabled) }
    }

    fun setAutoSkipOutro(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSkipOutro(enabled) }
    }

    fun setAutoSkipRecap(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSkipRecap(enabled) }
    }

    fun setSkipOnce(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSkipOnce(enabled) }
    }

    fun setManualSkip(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setManualSkip(enabled) }
    }

    fun setSubtitleDelayMs(delayMs: Long) {
        playerEngine.setSubtitleDelay(delayMs)
        viewModelScope.launch { settingsRepository.setSubtitleDelayMs(delayMs) }
    }

    fun setSubtitleLanguage(language: String) {
        viewModelScope.launch { settingsRepository.setSubtitleLanguage(language) }
    }

    fun setSubtitleFontSize(sizeSp: Int) {
        viewModelScope.launch { settingsRepository.setSubtitleFontSize(sizeSp) }
    }

    fun setSubtitleFontStyle(fontStyle: String) {
        viewModelScope.launch { settingsRepository.setSubtitleFontStyle(fontStyle) }
    }

    fun setSubtitleTextColor(colorHex: String) {
        viewModelScope.launch { settingsRepository.setSubtitleTextColor(colorHex) }
    }

    fun setSubtitleTextOpacity(opacity: Float) {
        viewModelScope.launch { settingsRepository.setSubtitleTextOpacity(opacity) }
    }

    fun setSubtitleBackgroundColor(colorHex: String) {
        viewModelScope.launch { settingsRepository.setSubtitleBackgroundColor(colorHex) }
    }

    fun setSubtitleBackgroundOpacity(opacity: Float) {
        viewModelScope.launch { settingsRepository.setSubtitleBackgroundOpacity(opacity) }
    }

    fun setSubtitleOutlineEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSubtitleOutlineEnabled(enabled) }
    }

    fun setSubtitleOutlineColor(colorHex: String) {
        viewModelScope.launch { settingsRepository.setSubtitleOutlineColor(colorHex) }
    }

    fun setSubtitleBottomMarginDp(marginDp: Int) {
        viewModelScope.launch { settingsRepository.setSubtitleBottomMarginDp(marginDp) }
    }

    fun setAutoUpdateAniList(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoUpdateAniList(enabled) }
    }

    fun setCompletionPercentage(percentage: Int) {
        viewModelScope.launch { settingsRepository.setCompletionPercentage(percentage) }
    }

    fun setSyncProgress(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSyncProgress(enabled) }
    }

    fun setAutoMarkCompleted(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoMarkCompleted(enabled) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.release()
    }

    companion object {
        fun provideFactory(
            animeId: String,
            episodeId: String,
            animeRepository: AnimeRepository,
            episodeRepository: EpisodeRepository,
            historyRepository: HistoryRepository,
            settingsRepository: SettingsRepository,
            sourceSelector: SourceSelector,
            playerEngine: PlayerEngine,
            trackingRepository: TrackingRepository? = null,
            skipMetadataRepository: SkipMetadataRepository? = null,
            downloadRepository: DownloadRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(
                    animeId = animeId,
                    initialEpisodeId = episodeId,
                    animeRepository = animeRepository,
                    episodeRepository = episodeRepository,
                    historyRepository = historyRepository,
                    settingsRepository = settingsRepository,
                    sourceSelector = sourceSelector,
                    playerEngine = playerEngine,
                    trackingRepository = trackingRepository,
                    skipMetadataRepository = skipMetadataRepository,
                    downloadRepository = downloadRepository
                ) as T
            }
        }
    }
}
