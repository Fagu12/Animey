package com.example.domain.player.selector

import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.ExtensionManager
import com.example.domain.model.ProviderOption
import com.example.domain.repository.SourcePreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

/**
 * Responsible for discovering, evaluating, and selecting video providers (extensions).
 * Decoupled from player engine and ExoPlayer internals.
 */
class ProviderSelector(
    private val extensionManager: ExtensionManager,
    private val sourcePreferenceRepository: SourcePreferenceRepository
) {

    /**
     * Returns the list of enabled providers converted to [ProviderOption],
     * with preference flags resolved against per-anime and global settings.
     */
    suspend fun getAvailableProviders(
        animeId: String = "",
        selectedProviderId: String? = null
    ): List<ProviderOption> {
        val enabledExtensions = extensionManager.enabledExtensions.value
        val perAnimePref = if (animeId.isNotBlank()) {
            sourcePreferenceRepository.getPreferredProviderForAnime(animeId).firstOrNull()
        } else null
        val globalPref = sourcePreferenceRepository.globalPreferredProvider.firstOrNull()
        val preferredId = perAnimePref ?: globalPref

        return enabledExtensions.map { extension ->
            val isPreferred = extension.id == preferredId
            val isSelected = if (selectedProviderId != null) {
                extension.id == selectedProviderId
            } else {
                isPreferred || (preferredId == null && extension == enabledExtensions.firstOrNull())
            }

            ProviderOption(
                id = extension.id,
                name = extension.name,
                lang = extension.lang,
                iconUrl = extension.iconUrl,
                isPreferred = isPreferred,
                isSelected = isSelected,
                isBuiltIn = extension.manifest.isBuiltIn
            )
        }
    }

    /**
     * Resolves the primary [AnimeExtension] instance to query for streams.
     * Hierarchy:
     * 1. Explicit override (if provided & valid)
     * 2. Per-anime preferred provider
     * 3. Global preferred provider
     * 4. First enabled extension from ExtensionManager
     */
    suspend fun resolveBestProvider(
        animeId: String = "",
        preferredOverrideId: String? = null
    ): AnimeExtension? {
        val enabled = extensionManager.enabledExtensions.value
        if (enabled.isEmpty()) return null

        // 1. Explicit override
        if (!preferredOverrideId.isNullOrBlank()) {
            val matched = enabled.find { it.id == preferredOverrideId }
            if (matched != null) return matched
        }

        // 2. Per-anime preference
        if (animeId.isNotBlank()) {
            val animePref = sourcePreferenceRepository.getPreferredProviderForAnime(animeId).firstOrNull()
            if (!animePref.isNullOrBlank()) {
                val matched = enabled.find { it.id == animePref }
                if (matched != null) return matched
            }
        }

        // 3. Global preference
        val globalPref = sourcePreferenceRepository.globalPreferredProvider.firstOrNull()
        if (!globalPref.isNullOrBlank()) {
            val matched = enabled.find { it.id == globalPref }
            if (matched != null) return matched
        }

        // 4. Default fallback: first enabled extension
        return enabled.firstOrNull()
    }

    /**
     * Retrieves an extension instance by ID.
     */
    fun getProvider(providerId: String): AnimeExtension? {
        return extensionManager.getExtension(providerId)
    }

    /**
     * Updates preference for provider.
     */
    suspend fun setPreferredProvider(
        animeId: String?,
        providerId: String?,
        isGlobal: Boolean = false
    ) {
        if (isGlobal || animeId.isNullOrBlank()) {
            sourcePreferenceRepository.setGlobalPreferredProvider(providerId)
        } else {
            sourcePreferenceRepository.setPreferredProviderForAnime(animeId, providerId)
        }
    }
}
