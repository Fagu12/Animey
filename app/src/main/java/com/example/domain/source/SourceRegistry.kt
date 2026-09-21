package com.example.domain.source

import kotlinx.coroutines.flow.StateFlow

/**
 * Unified Source Registry managing active, registered, and preferred sources.
 */
interface SourceRegistry {
    val allSources: StateFlow<List<AnimeSource>>
    val enabledSources: StateFlow<List<AnimeSource>>
    val preferredSource: StateFlow<AnimeSource?>

    fun registerSource(source: AnimeSource)
    fun unregisterSource(sourceId: String)
    fun getSource(sourceId: String): AnimeSource?
    fun setSourceEnabled(sourceId: String, enabled: Boolean)
    fun setPreferredSource(sourceId: String)
    fun getSourcesByType(sourceType: SourceTypeInfo): List<AnimeSource>
}
