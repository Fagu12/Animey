package com.example.domain.source

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Default in-memory implementation of SourceRegistry.
 */
class DefaultSourceRegistry : SourceRegistry {

    private val sourcesMap = ConcurrentHashMap<String, AnimeSource>()
    private val disabledSourceIds = ConcurrentHashMap.newKeySet<String>()
    private var preferredId: String? = null

    private val _allSources = MutableStateFlow<List<AnimeSource>>(emptyList())
    override val allSources: StateFlow<List<AnimeSource>> = _allSources.asStateFlow()

    private val _enabledSources = MutableStateFlow<List<AnimeSource>>(emptyList())
    override val enabledSources: StateFlow<List<AnimeSource>> = _enabledSources.asStateFlow()

    private val _preferredSource = MutableStateFlow<AnimeSource?>(null)
    override val preferredSource: StateFlow<AnimeSource?> = _preferredSource.asStateFlow()

    @Synchronized
    override fun registerSource(source: AnimeSource) {
        sourcesMap[source.id] = source
        if (preferredId == null) {
            preferredId = source.id
        }
        updateFlows()
    }

    @Synchronized
    override fun unregisterSource(sourceId: String) {
        sourcesMap.remove(sourceId)
        disabledSourceIds.remove(sourceId)
        if (preferredId == sourceId) {
            preferredId = sourcesMap.keys.firstOrNull()
        }
        updateFlows()
    }

    override fun getSource(sourceId: String): AnimeSource? {
        return sourcesMap[sourceId]
    }

    @Synchronized
    override fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        if (enabled) {
            disabledSourceIds.remove(sourceId)
        } else {
            disabledSourceIds.add(sourceId)
        }
        updateFlows()
    }

    @Synchronized
    override fun setPreferredSource(sourceId: String) {
        if (sourcesMap.containsKey(sourceId)) {
            preferredId = sourceId
            updateFlows()
        }
    }

    override fun getSourcesByType(sourceType: SourceTypeInfo): List<AnimeSource> {
        return sourcesMap.values.filter { it.sourceType == sourceType }
    }

    private fun updateFlows() {
        val all = sourcesMap.values.toList()
        _allSources.value = all
        _enabledSources.value = all.filter { !disabledSourceIds.contains(it.id) }
        _preferredSource.value = preferredId?.let { sourcesMap[it] } ?: _enabledSources.value.firstOrNull()
    }
}
