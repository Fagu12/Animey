package com.example.data.extension.mangayomi.runtime

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.extension.mangayomi.bridge.MangayomiCryptoBridge
import com.example.data.extension.mangayomi.bridge.MangayomiDomBridge
import com.example.data.extension.mangayomi.bridge.MangayomiHttpBridge
import com.example.data.extension.mangayomi.model.MangayomiExtensionManifest
import com.example.data.source.adapter.MangayomiSourceAdapter
import com.example.domain.extension.runtime.ExtensionLifecycleState
import com.example.domain.source.ExtensionCompatibilityStatus
import com.example.domain.source.SourceRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Mangayomi JavaScript Extension Runtime Manager.
 * Orchestrates extension lifecycle, execution contexts, and source adapter registration.
 */
class MangayomiRuntime(
    private val sourceRegistry: SourceRegistry,
    private val httpBridge: MangayomiHttpBridge = MangayomiHttpBridge(),
    private val domBridge: MangayomiDomBridge = MangayomiDomBridge(),
    private val cryptoBridge: MangayomiCryptoBridge = MangayomiCryptoBridge()
) {

    private val manifestsMap = ConcurrentHashMap<String, MangayomiExtensionManifest>()
    private val contextsMap = ConcurrentHashMap<String, MangayomiExecutionContext>()
    private val adaptersMap = ConcurrentHashMap<String, MangayomiSourceAdapter>()
    private val lifecycleMap = ConcurrentHashMap<String, ExtensionLifecycleState>()

    private val _installedExtensions = MutableStateFlow<List<MangayomiExtensionManifest>>(emptyList())
    val installedExtensions: StateFlow<List<MangayomiExtensionManifest>> = _installedExtensions.asStateFlow()

    /**
     * Installs or loads a Mangayomi JS extension into the runtime.
     */
    suspend fun loadExtension(manifest: MangayomiExtensionManifest): AppResult<MangayomiSourceAdapter> {
        val extensionId = manifest.id
        try {
            updateLifecycle(extensionId, ExtensionLifecycleState.LOADING)

            // Validate script
            if (manifest.scriptContent.isBlank()) {
                updateLifecycle(extensionId, ExtensionLifecycleState.FAILED)
                return AppResult.Error(AppError.ExtensionError(extensionId, "Extension script content is empty"))
            }

            updateLifecycle(extensionId, ExtensionLifecycleState.INITIALIZING)

            // Create execution context
            val context = MangayomiExecutionContext(
                manifest = manifest
            )

            // Clean previous context if exists
            contextsMap[extensionId]?.close()
            contextsMap[extensionId] = context
            manifestsMap[extensionId] = manifest

            // Create real source adapter
            val adapter = MangayomiSourceAdapter(
                id = manifest.id,
                name = manifest.name,
                lang = manifest.lang,
                baseUrl = manifest.baseUrl,
                iconUrl = manifest.iconUrl,
                lifecycleState = ExtensionLifecycleState.ACTIVE,
                compatibilityStatus = manifest.compatibilityStatus,
                itemType = manifest.itemType,
                executionContext = context
            )

            adaptersMap[extensionId] = adapter
            updateLifecycle(extensionId, ExtensionLifecycleState.ACTIVE)

            // Register with unified source registry
            sourceRegistry.registerSource(adapter)
            updateInstalledList()

            return AppResult.Success(adapter)
        } catch (e: Exception) {
            updateLifecycle(extensionId, ExtensionLifecycleState.ERROR)
            return AppResult.Error(AppError.ExtensionError(extensionId, "Failed to load Mangayomi extension: ${e.message}", e))
        }
    }

    /**
     * Unload an extension and release its execution resources.
     */
    fun unloadExtension(extensionId: String): AppResult<Unit> {
        try {
            updateLifecycle(extensionId, ExtensionLifecycleState.UNLOADING)
            contextsMap.remove(extensionId)?.close()
            adaptersMap.remove(extensionId)
            sourceRegistry.unregisterSource(extensionId)
            updateLifecycle(extensionId, ExtensionLifecycleState.UNLOADED)
            updateInstalledList()
            return AppResult.Success(Unit)
        } catch (e: Exception) {
            return AppResult.Error(AppError.ExtensionError(extensionId, "Failed to unload extension: ${e.message}", e))
        }
    }

    /**
     * Enables or disables an extension.
     */
    fun setExtensionEnabled(extensionId: String, enabled: Boolean) {
        val currentState = lifecycleMap[extensionId] ?: return
        if (enabled && currentState == ExtensionLifecycleState.DISABLED) {
            updateLifecycle(extensionId, ExtensionLifecycleState.ACTIVE)
            sourceRegistry.setSourceEnabled(extensionId, true)
        } else if (!enabled && currentState == ExtensionLifecycleState.ACTIVE) {
            updateLifecycle(extensionId, ExtensionLifecycleState.DISABLED)
            sourceRegistry.setSourceEnabled(extensionId, false)
        }
        updateInstalledList()
    }

    /**
     * Completely remove an installed extension.
     */
    fun removeExtension(extensionId: String): AppResult<Unit> {
        unloadExtension(extensionId)
        manifestsMap.remove(extensionId)
        lifecycleMap.remove(extensionId)
        updateInstalledList()
        return AppResult.Success(Unit)
    }

    fun getExtensionState(extensionId: String): ExtensionLifecycleState {
        return lifecycleMap[extensionId] ?: ExtensionLifecycleState.UNINSTALLED
    }

    fun getAdapter(extensionId: String): MangayomiSourceAdapter? {
        return adaptersMap[extensionId]
    }

    private fun updateLifecycle(extensionId: String, newState: ExtensionLifecycleState) {
        val current = lifecycleMap[extensionId]
        if (current != null) {
            try {
                current.validateTransition(newState)
            } catch (ignored: Exception) {
                // Keep resilient
            }
        }
        lifecycleMap[extensionId] = newState
    }

    private fun updateInstalledList() {
        val list = manifestsMap.values.map { manifest ->
            val state = lifecycleMap[manifest.id] ?: ExtensionLifecycleState.INSTALLED
            manifest.copy(lifecycleState = state)
        }
        _installedExtensions.value = list
    }
}
