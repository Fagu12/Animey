package com.example.domain.player.selector

import com.example.domain.model.ServerOption
import com.example.domain.model.VideoSource
import com.example.domain.repository.SourcePreferenceRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Responsible for grouping, ranking, and selecting streaming servers.
 * Server selection is isolated from provider extraction and video quality.
 */
class ServerSelector(
    private val sourcePreferenceRepository: SourcePreferenceRepository
) {

    /**
     * Extracts distinct servers from the raw [VideoSource] list,
     * tracking qualities, failure status, and preference tags.
     */
    suspend fun extractServers(
        sources: List<VideoSource>,
        animeId: String = "",
        selectedServerName: String? = null,
        failedSourceIds: Set<String> = emptySet()
    ): List<ServerOption> {
        if (sources.isEmpty()) return emptyList()

        val perAnimePref = if (animeId.isNotBlank()) {
            sourcePreferenceRepository.getPreferredServerForAnime(animeId).firstOrNull()
        } else null
        val globalPref = sourcePreferenceRepository.preferredServer.firstOrNull()
        val preferredName = perAnimePref ?: globalPref

        // Group sources by their serverName
        val serverGroups = sources.groupBy { normalizeServerName(it.serverName) }

        return serverGroups.map { (serverName, serverSources) ->
            val qualities = serverSources.map { it.quality }.distinct()
            val allFailed = serverSources.isNotEmpty() && serverSources.all { failedSourceIds.contains(it.id) }
            val isPreferred = serverName.equals(preferredName, ignoreCase = true) ||
                    (preferredName != null && serverName.contains(preferredName, ignoreCase = true))

            val isSelected = if (selectedServerName != null) {
                normalizeServerName(selectedServerName).equals(serverName, ignoreCase = true)
            } else {
                isPreferred || serverSources.any { it.isDefault }
            }

            ServerOption(
                name = serverName,
                isPreferred = isPreferred,
                isSelected = isSelected,
                sourceCount = serverSources.size,
                qualities = qualities,
                hasFailed = allFailed
            )
        }
    }

    /**
     * Resolves the best server name to use for stream playback.
     * Hierarchy:
     * 1. Explicit override (if not failed and exists)
     * 2. Per-anime preferred server
     * 3. Global preferred server
     * 4. Server with isDefault = true
     * 5. First server with healthy (non-failed) sources
     */
    suspend fun resolveBestServer(
        sources: List<VideoSource>,
        animeId: String = "",
        selectedServerOverride: String? = null,
        failedSourceIds: Set<String> = emptySet()
    ): String? {
        if (sources.isEmpty()) return null

        val groups = sources.groupBy { normalizeServerName(it.serverName) }
        val perAnimePref = if (animeId.isNotBlank()) {
            sourcePreferenceRepository.getPreferredServerForAnime(animeId).firstOrNull()
        } else null
        val globalPref = sourcePreferenceRepository.preferredServer.firstOrNull()

        // 1. Explicit override
        if (!selectedServerOverride.isNullOrBlank()) {
            val normalized = normalizeServerName(selectedServerOverride)
            val matchedGroup = groups[normalized]
            if (matchedGroup != null && matchedGroup.any { !failedSourceIds.contains(it.id) }) {
                return normalized
            }
        }

        // 2. Per-anime preference
        if (!perAnimePref.isNullOrBlank()) {
            val matchedKey = groups.keys.find { it.contains(perAnimePref, ignoreCase = true) }
            if (matchedKey != null && groups[matchedKey]?.any { !failedSourceIds.contains(it.id) } == true) {
                return matchedKey
            }
        }

        // 3. Global preference
        if (!globalPref.isNullOrBlank()) {
            val matchedKey = groups.keys.find { it.contains(globalPref, ignoreCase = true) }
            if (matchedKey != null && groups[matchedKey]?.any { !failedSourceIds.contains(it.id) } == true) {
                return matchedKey
            }
        }

        // 4. Default source tag
        val defaultServer = sources.find { it.isDefault && !failedSourceIds.contains(it.id) }?.serverName
        if (defaultServer != null) {
            return normalizeServerName(defaultServer)
        }

        // 5. First non-failed server
        for ((serverName, serverSources) in groups) {
            if (serverSources.any { !failedSourceIds.contains(it.id) }) {
                return serverName
            }
        }

        // Fallback: First group regardless of failure
        return groups.keys.firstOrNull()
    }

    /**
     * Filters sources belonging to a specified server.
     */
    fun filterSourcesForServer(
        sources: List<VideoSource>,
        serverName: String?
    ): List<VideoSource> {
        if (serverName.isNullOrBlank()) return sources
        val target = normalizeServerName(serverName)
        val filtered = sources.filter { normalizeServerName(it.serverName).equals(target, ignoreCase = true) }
        return if (filtered.isNotEmpty()) filtered else sources
    }

    /**
     * Finds the next alternative healthy server when current server fails.
     */
    fun findNextFallbackServer(
        sources: List<VideoSource>,
        currentServer: String?,
        failedSourceIds: Set<String>
    ): String? {
        val groups = sources.groupBy { normalizeServerName(it.serverName) }
        val currentNormalized = currentServer?.let { normalizeServerName(it) }

        for ((serverName, serverSources) in groups) {
            if (serverName != currentNormalized && serverSources.any { !failedSourceIds.contains(it.id) }) {
                return serverName
            }
        }
        return null
    }

    /**
     * Saves user preferred server.
     */
    suspend fun setPreferredServer(
        animeId: String?,
        serverName: String?,
        isGlobal: Boolean = false
    ) {
        if (isGlobal || animeId.isNullOrBlank()) {
            sourcePreferenceRepository.setPreferredServer(serverName)
        } else {
            sourcePreferenceRepository.setPreferredServerForAnime(animeId, serverName)
        }
    }

    private fun normalizeServerName(rawName: String): String {
        return rawName.trim()
    }
}
