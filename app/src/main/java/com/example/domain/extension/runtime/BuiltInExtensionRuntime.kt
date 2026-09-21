package com.example.domain.extension.runtime

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.logging.DefaultExtensionLogger
import com.example.domain.extension.logging.ExtensionLogger
import com.example.domain.extension.logging.ExtensionRuntimeStats
import com.example.domain.model.ExtensionManifest
import java.util.concurrent.ConcurrentHashMap

/**
 * Production built-in extension runtime.
 * Safely wraps registered in-app extensions with [IsolatedAnimeExtension] to provide:
 * - Runtime sandboxing
 * - Circuit breaking & failure isolation
 * - Per-extension telemetry and logging
 */
class BuiltInExtensionRuntime(
    private val logger: ExtensionLogger = DefaultExtensionLogger()
) : ExtensionRuntime {

    override val runtimeType: String = "builtin"
    override val version: String = "1.0.0"
    override val isAvailable: Boolean = true

    @Volatile
    private var state: ExtensionRuntimeState = ExtensionRuntimeState.READY

    // In-memory catalog of available built-in extension suppliers
    private val rawCatalog = ConcurrentHashMap<String, AnimeExtension>()
    // Active isolated extension instances
    private val loadedInstances = ConcurrentHashMap<String, IsolatedAnimeExtension>()

    /**
     * Registers a built-in compiled extension into the runtime catalog.
     */
    fun register(extension: AnimeExtension) {
        rawCatalog[extension.id] = extension
        logger.i(extension.id, "Runtime", "Registered built-in extension: ${extension.id} (${extension.name})")
    }

    override fun canHandle(manifest: ExtensionManifest): Boolean {
        return manifest.isBuiltIn || rawCatalog.containsKey(manifest.id)
    }

    override suspend fun initialize(): AppResult<Unit> {
        state = ExtensionRuntimeState.READY
        return AppResult.Success(Unit)
    }

    override suspend fun loadExtension(
        manifest: ExtensionManifest,
        payload: ByteArray?
    ): AppResult<AnimeExtension> {
        if (state != ExtensionRuntimeState.READY) {
            return AppResult.Error(AppError.GeneralError("Runtime '$runtimeType' is not ready (state: $state)"))
        }

        // Return already loaded instance if exists
        loadedInstances[manifest.id]?.let {
            return AppResult.Success(it)
        }

        val raw = rawCatalog[manifest.id]
            ?: return AppResult.Error(
                AppError.NotFoundError("Built-in extension '${manifest.id}' not found in runtime catalog")
            )

        val isolated = IsolatedAnimeExtension(
            delegate = raw,
            logger = logger
        )
        loadedInstances[manifest.id] = isolated
        logger.i(manifest.id, "Runtime", "Loaded extension '${manifest.name}' into built-in sandbox")

        return AppResult.Success(isolated)
    }

    override suspend fun unloadExtension(extensionId: String): AppResult<Unit> {
        loadedInstances.remove(extensionId)
        logger.i(extensionId, "Runtime", "Unloaded extension '$extensionId' from built-in sandbox")
        return AppResult.Success(Unit)
    }

    override suspend fun getLoadedExtension(extensionId: String): AnimeExtension? {
        return loadedInstances[extensionId]
    }

    override fun getLoadedExtensionIds(): Set<String> {
        return loadedInstances.keys.toSet()
    }

    override fun getRuntimeStats(extensionId: String): ExtensionRuntimeStats? {
        return loadedInstances[extensionId]?.getStats()
    }

    override fun getStatus(): ExtensionRuntimeStatus {
        return ExtensionRuntimeStatus(
            runtimeType = runtimeType,
            version = version,
            state = state,
            loadedCount = loadedInstances.size,
            isAvailable = isAvailable
        )
    }

    override suspend fun shutdown(): AppResult<Unit> {
        loadedInstances.clear()
        state = ExtensionRuntimeState.SHUTDOWN
        AppLogger.i("BuiltInRuntime", "Shutdown completed")
        return AppResult.Success(Unit)
    }

    override suspend fun getExtensionPreferences(extensionId: String): List<com.example.domain.model.ExtensionPreference> {
        val ext = loadedInstances[extensionId] ?: rawCatalog[extensionId] ?: return emptyList()
        return ext.getPreferences()
    }

    override suspend fun setExtensionPreference(
        extensionId: String,
        key: String,
        value: Any
    ): AppResult<Unit> {
        val ext = loadedInstances[extensionId] ?: rawCatalog[extensionId]
            ?: return AppResult.Error(AppError.NotFoundError("Extension '$extensionId' not found in built-in runtime"))
        ext.setPreference(key, value)
        logger.d(extensionId, "Runtime", "Forwarded preference '$key' change to extension through runtime")
        return AppResult.Success(Unit)
    }
}
