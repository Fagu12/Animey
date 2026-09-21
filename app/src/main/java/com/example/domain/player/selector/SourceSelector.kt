package com.example.domain.player.selector

import com.example.core.result.AppResult
import com.example.domain.extension.ExtensionManager
import com.example.domain.model.Episode
import com.example.domain.model.QualityOption
import com.example.domain.model.SourceSelectionState
import com.example.domain.model.VideoSource
import com.example.domain.repository.SourcePreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update

/**
 * High-level coordinator for the Provider -> Server -> Quality video source pipeline.
 *
 * Episode
 *   ↓
 * ExtensionManager
 *   ↓
 * Available extensions/providers (ProviderSelector)
 *   ↓
 * VideoSource list (grouped by ServerSelector)
 *   ↓
 * Quality Selection (1080p, 720p, 480p, 360p)
 *
 * Keeps provider selection strictly separated from video quality selection.
 * Handles graceful automatic and manual fallback when a source fails.
 */
class SourceSelector(
    private val extensionManager: ExtensionManager,
    private val providerSelector: ProviderSelector,
    private val serverSelector: ServerSelector,
    private val sourcePreferenceRepository: SourcePreferenceRepository
) {

    private val _state = MutableStateFlow(SourceSelectionState())
    val state: StateFlow<SourceSelectionState> = _state.asStateFlow()

    companion object {
        val STANDARD_QUALITIES = listOf("1080p", "720p", "480p", "360p")
    }

    /**
     * Entry point: Loads video sources for an episode through the provider resolution pipeline.
     */
    suspend fun loadSourcesForEpisode(
        animeId: String,
        episode: Episode,
        forcedProviderId: String? = null,
        forcedServerName: String? = null,
        forcedQuality: String? = null
    ): SourceSelectionState {
        _state.update {
            it.copy(
                isLoading = true,
                animeId = animeId,
                episode = episode,
                error = null,
                isAutoFallbackActive = false
            )
        }

        // 1. Discover available providers
        val providers = providerSelector.getAvailableProviders(
            animeId = animeId,
            selectedProviderId = forcedProviderId ?: episode.sourceId
        )

        // 2. Resolve target provider extension
        val targetProvider = providerSelector.resolveBestProvider(
            animeId = animeId,
            preferredOverrideId = forcedProviderId ?: if (episode.sourceId.isNotBlank()) episode.sourceId else null
        )

        if (targetProvider == null) {
            val errorState = _state.value.copy(
                isLoading = false,
                availableProviders = providers,
                error = "No active video provider/extension found."
            )
            _state.value = errorState
            return errorState
        }

        val effectiveProviderId = targetProvider.id

        // 3. Extract stream sources from the provider
        val sourceEpisodeId = episode.sourceEpisodeId.ifBlank { episode.id }
        val streamsResult = targetProvider.getEpisodeStreams(sourceEpisodeId)

        val rawSources = when (streamsResult) {
            is AppResult.Success -> streamsResult.data
            is AppResult.Error -> {
                // Fallback attempt: query via ExtensionManager fallback
                when (val managerResult = extensionManager.getStreams(effectiveProviderId, sourceEpisodeId)) {
                    is AppResult.Success -> managerResult.data
                    is AppResult.Error, is AppResult.Loading -> emptyList()
                }
            }
            is AppResult.Loading -> emptyList()
        }

        if (rawSources.isEmpty()) {
            val emptyState = _state.value.copy(
                isLoading = false,
                availableProviders = providers.map { it.copy(isSelected = it.id == effectiveProviderId) },
                selectedProviderId = effectiveProviderId,
                rawSources = emptyList(),
                availableServers = emptyList(),
                availableQualities = emptyList(),
                activeSource = null,
                error = "No video streams returned by provider '${targetProvider.name}'."
            )
            _state.value = emptyState
            return emptyState
        }

        // 4. Resolve Server
        val failedIds = _state.value.failedSourceIds
        val selectedServer = serverSelector.resolveBestServer(
            sources = rawSources,
            animeId = animeId,
            selectedServerOverride = forcedServerName,
            failedSourceIds = failedIds
        )

        val servers = serverSelector.extractServers(
            sources = rawSources,
            animeId = animeId,
            selectedServerName = selectedServer,
            failedSourceIds = failedIds
        )

        // 5. Filter sources for active server & resolve quality
        val serverSources = serverSelector.filterSourcesForServer(rawSources, selectedServer)
        val selectedQuality = resolveBestQuality(
            sources = serverSources,
            animeId = animeId,
            forcedQuality = forcedQuality
        )

        val qualities = extractQualityOptions(
            allSources = rawSources,
            serverSources = serverSources,
            animeId = animeId,
            selectedQuality = selectedQuality
        )

        // 6. Select the active VideoSource
        val activeSource = serverSources.find { it.quality.equals(selectedQuality, ignoreCase = true) }
            ?: serverSources.firstOrNull { !failedIds.contains(it.id) }
            ?: serverSources.firstOrNull()

        val newState = _state.value.copy(
            isLoading = false,
            availableProviders = providers.map { it.copy(isSelected = it.id == effectiveProviderId) },
            selectedProviderId = effectiveProviderId,
            rawSources = rawSources,
            availableServers = servers,
            selectedServerName = selectedServer,
            availableQualities = qualities,
            selectedQuality = selectedQuality,
            activeSource = activeSource,
            error = null
        )

        _state.value = newState
        return newState
    }

    /**
     * User explicitly selects a different Provider.
     */
    suspend fun selectProvider(providerId: String) {
        val current = _state.value
        val ep = current.episode ?: return
        loadSourcesForEpisode(
            animeId = current.animeId,
            episode = ep,
            forcedProviderId = providerId
        )
    }

    /**
     * User explicitly selects a different Server under the current provider.
     */
    suspend fun selectServer(serverName: String) {
        val current = _state.value
        if (current.rawSources.isEmpty()) return

        val serverSources = serverSelector.filterSourcesForServer(current.rawSources, serverName)
        val selectedQuality = resolveBestQuality(
            sources = serverSources,
            animeId = current.animeId,
            forcedQuality = current.selectedQuality
        )

        val updatedServers = serverSelector.extractServers(
            sources = current.rawSources,
            animeId = current.animeId,
            selectedServerName = serverName,
            failedSourceIds = current.failedSourceIds
        )

        val updatedQualities = extractQualityOptions(
            allSources = current.rawSources,
            serverSources = serverSources,
            animeId = current.animeId,
            selectedQuality = selectedQuality
        )

        val activeSource = serverSources.find { it.quality.equals(selectedQuality, ignoreCase = true) }
            ?: serverSources.firstOrNull { !current.failedSourceIds.contains(it.id) }
            ?: serverSources.firstOrNull()

        _state.update {
            it.copy(
                availableServers = updatedServers,
                selectedServerName = serverName,
                availableQualities = updatedQualities,
                selectedQuality = selectedQuality,
                activeSource = activeSource,
                error = null
            )
        }
    }

    /**
     * User explicitly selects a different Quality (e.g., 1080p, 720p, 480p, 360p).
     */
    fun selectQuality(quality: String) {
        val current = _state.value
        if (current.rawSources.isEmpty()) return

        val serverSources = serverSelector.filterSourcesForServer(current.rawSources, current.selectedServerName)
        val matchedSource = serverSources.find { it.quality.equals(quality, ignoreCase = true) }
            ?: current.rawSources.find { it.quality.equals(quality, ignoreCase = true) }

        if (matchedSource != null) {
            val updatedQualities = current.availableQualities.map {
                it.copy(isSelected = it.quality.equals(quality, ignoreCase = true))
            }

            _state.update {
                it.copy(
                    availableQualities = updatedQualities,
                    selectedQuality = quality,
                    activeSource = matchedSource,
                    selectedServerName = matchedSource.serverName
                )
            }
        }
    }

    /**
     * Marks a source as failed (e.g. ExoPlayer playback error or HTTP 403/404),
     * and attempts to seamlessly switch to the next fallback source.
     */
    suspend fun markSourceFailed(sourceId: String): VideoSource? {
        val current = _state.value
        val updatedFailed = current.failedSourceIds + sourceId

        _state.update { it.copy(failedSourceIds = updatedFailed) }

        return selectNextFallbackSource()
    }

    /**
     * Fallback resolution sequence:
     * 1. Next quality on current server
     * 2. Next healthy server under current provider
     * 3. Next available provider
     */
    suspend fun selectNextFallbackSource(): VideoSource? {
        val current = _state.value
        val failedIds = current.failedSourceIds

        // Strategy 1: Find next non-failed source on current server
        val currentServerSources = serverSelector.filterSourcesForServer(current.rawSources, current.selectedServerName)
        val alternativeOnServer = currentServerSources.firstOrNull { !failedIds.contains(it.id) }
        if (alternativeOnServer != null) {
            _state.update {
                it.copy(
                    activeSource = alternativeOnServer,
                    selectedQuality = alternativeOnServer.quality,
                    isAutoFallbackActive = true
                )
            }
            return alternativeOnServer
        }

        // Strategy 2: Find next fallback server on current provider
        val nextServer = serverSelector.findNextFallbackServer(
            sources = current.rawSources,
            currentServer = current.selectedServerName,
            failedSourceIds = failedIds
        )
        if (nextServer != null) {
            selectServer(nextServer)
            _state.update { it.copy(isAutoFallbackActive = true) }
            return _state.value.activeSource
        }

        // Strategy 3: Switch to next provider
        val currentProviderId = current.selectedProviderId
        val nextProvider = current.availableProviders.firstOrNull { it.id != currentProviderId }
        if (nextProvider != null && current.episode != null) {
            loadSourcesForEpisode(
                animeId = current.animeId,
                episode = current.episode,
                forcedProviderId = nextProvider.id
            )
            _state.update { it.copy(isAutoFallbackActive = true) }
            return _state.value.activeSource
        }

        return null
    }

    // ==================== Persistence Helpers ====================

    suspend fun setPreferredProvider(animeId: String?, providerId: String, isGlobal: Boolean = false) {
        providerSelector.setPreferredProvider(animeId, providerId, isGlobal)
        _state.update { state ->
            state.copy(
                availableProviders = state.availableProviders.map {
                    it.copy(isPreferred = it.id == providerId)
                }
            )
        }
    }

    suspend fun setPreferredServer(animeId: String?, serverName: String, isGlobal: Boolean = false) {
        serverSelector.setPreferredServer(animeId, serverName, isGlobal)
        _state.update { state ->
            state.copy(
                availableServers = state.availableServers.map {
                    it.copy(isPreferred = it.name.equals(serverName, ignoreCase = true))
                }
            )
        }
    }

    suspend fun setPreferredQuality(animeId: String?, quality: String, isGlobal: Boolean = false) {
        if (isGlobal || animeId.isNullOrBlank()) {
            sourcePreferenceRepository.setPreferredQuality(quality)
        } else {
            sourcePreferenceRepository.setPreferredQualityForAnime(animeId, quality)
        }
        _state.update { state ->
            state.copy(
                availableQualities = state.availableQualities.map {
                    it.copy(isPreferred = it.quality.equals(quality, ignoreCase = true))
                }
            )
        }
    }

    // ==================== Internal Helpers ====================

    private suspend fun resolveBestQuality(
        sources: List<VideoSource>,
        animeId: String = "",
        forcedQuality: String? = null
    ): String {
        if (sources.isEmpty()) return "1080p"

        val perAnimeQuality = if (animeId.isNotBlank()) {
            sourcePreferenceRepository.getPreferredQualityForAnime(animeId).firstOrNull()
        } else null
        val globalQuality = sourcePreferenceRepository.preferredQuality.firstOrNull() ?: "1080p"

        val targetQuality = forcedQuality ?: perAnimeQuality ?: globalQuality

        // Check if target quality is directly available in the sources
        val match = sources.find { it.quality.equals(targetQuality, ignoreCase = true) }
        if (match != null) return match.quality

        // Fallback: Pick highest resolution among standard qualities
        for (q in STANDARD_QUALITIES) {
            if (sources.any { it.quality.equals(q, ignoreCase = true) }) {
                return q
            }
        }

        return sources.first().quality
    }

    private suspend fun extractQualityOptions(
        allSources: List<VideoSource>,
        serverSources: List<VideoSource>,
        animeId: String = "",
        selectedQuality: String
    ): List<QualityOption> {
        val perAnimePref = if (animeId.isNotBlank()) {
            sourcePreferenceRepository.getPreferredQualityForAnime(animeId).firstOrNull()
        } else null
        val globalPref = sourcePreferenceRepository.preferredQuality.firstOrNull() ?: "1080p"
        val preferredQuality = perAnimePref ?: globalPref

        val serverQualities = serverSources.map { it.quality.lowercase() }.toSet()
        val allQualities = (STANDARD_QUALITIES + allSources.map { it.quality }).distinct()

        return allQualities.map { q ->
            val label = when (q.lowercase()) {
                "1080p" -> "1080p (FHD)"
                "720p" -> "720p (HD)"
                "480p" -> "480p (SD)"
                "360p" -> "360p (Data Saver)"
                else -> q
            }

            QualityOption(
                quality = q,
                label = label,
                isPreferred = q.equals(preferredQuality, ignoreCase = true),
                isSelected = q.equals(selectedQuality, ignoreCase = true),
                isAvailable = serverQualities.contains(q.lowercase())
            )
        }
    }
}
