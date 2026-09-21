package com.example.domain.model

/**
 * UI representation of an available provider/extension for an episode.
 */
data class ProviderOption(
    val id: String,
    val name: String,
    val lang: String = "en",
    val iconUrl: String = "",
    val isPreferred: Boolean = false,
    val isSelected: Boolean = false,
    val isBuiltIn: Boolean = false
)

/**
 * UI representation of a streaming server under a provider.
 */
data class ServerOption(
    val name: String,
    val isPreferred: Boolean = false,
    val isSelected: Boolean = false,
    val sourceCount: Int = 1,
    val qualities: List<String> = emptyList(),
    val hasFailed: Boolean = false
)

/**
 * UI representation of a video quality stream option.
 */
data class QualityOption(
    val quality: String,
    val label: String,
    val isPreferred: Boolean = false,
    val isSelected: Boolean = false,
    val isAvailable: Boolean = true
)

/**
 * Immutable state tracking the active source selection hierarchy:
 * Episode -> ExtensionManager -> Available Providers -> VideoSource list (grouped by Server) -> Quality
 */
data class SourceSelectionState(
    val isLoading: Boolean = false,
    val animeId: String = "",
    val episode: Episode? = null,
    val availableProviders: List<ProviderOption> = emptyList(),
    val selectedProviderId: String? = null,
    val rawSources: List<VideoSource> = emptyList(),
    val availableServers: List<ServerOption> = emptyList(),
    val selectedServerName: String? = null,
    val availableQualities: List<QualityOption> = emptyList(),
    val selectedQuality: String? = null,
    val activeSource: VideoSource? = null,
    val failedSourceIds: Set<String> = emptySet(),
    val isAutoFallbackActive: Boolean = false,
    val error: String? = null
) {
    val hasSources: Boolean
        get() = rawSources.isNotEmpty()

    val hasFallbackAvailable: Boolean
        get() = rawSources.any { !failedSourceIds.contains(it.id) && it.id != activeSource?.id }
}
