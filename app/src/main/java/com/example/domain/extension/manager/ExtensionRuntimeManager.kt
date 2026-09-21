package com.example.domain.extension.manager

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.factory.DefaultExtensionRuntimeFactory
import com.example.domain.extension.factory.ExtensionRuntimeFactory
import com.example.domain.extension.loader.DefaultExtensionLoader
import com.example.domain.extension.loader.ExtensionLoader
import com.example.domain.extension.logging.DefaultExtensionLogger
import com.example.domain.extension.logging.ExtensionLogEntry
import com.example.domain.extension.logging.ExtensionLogger
import com.example.domain.extension.logging.ExtensionRuntimeStats
import com.example.domain.extension.runtime.BuiltInExtensionRuntime
import com.example.domain.extension.runtime.ExtensionRuntime
import com.example.domain.extension.runtime.ExtensionRuntimeStatus
import com.example.domain.extension.validator.DefaultExtensionValidator
import com.example.domain.extension.validator.ExtensionValidator
import com.example.domain.model.ExtensionManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * High-level coordinator managing extension runtimes, lifecycles, execution sandboxes,
 * structured logging, and failure isolation.
 */
interface ExtensionRuntimeManager {
    val loadedExtensions: StateFlow<Map<String, AnimeExtension>>
    val runtimeStatuses: StateFlow<Map<String, ExtensionRuntimeStatus>>
    val extensionLogs: StateFlow<Map<String, List<ExtensionLogEntry>>>

    fun registerRuntime(runtime: ExtensionRuntime)
    fun getRuntime(runtimeType: String): ExtensionRuntime?
    fun getAllRuntimes(): List<ExtensionRuntime>

    suspend fun load(manifest: ExtensionManifest, payload: ByteArray? = null): AppResult<AnimeExtension>
    suspend fun unload(extensionId: String): AppResult<Unit>
    suspend fun reload(manifest: ExtensionManifest, payload: ByteArray? = null): AppResult<AnimeExtension>
    suspend fun update(newManifest: ExtensionManifest, payload: ByteArray? = null): AppResult<AnimeExtension> = reload(newManifest, payload)

    fun getExtension(extensionId: String): AnimeExtension?
    fun isLoaded(extensionId: String): Boolean
    fun getLifecycleState(extensionId: String): com.example.domain.extension.runtime.ExtensionLifecycleState =
        if (isLoaded(extensionId)) com.example.domain.extension.runtime.ExtensionLifecycleState.ACTIVE else com.example.domain.extension.runtime.ExtensionLifecycleState.UNLOADED
    fun getStats(extensionId: String): ExtensionRuntimeStats?
    fun getLogs(extensionId: String): List<ExtensionLogEntry>
    fun clearLogs(extensionId: String)
    suspend fun shutdown(): AppResult<Unit>
    suspend fun getExtensionPreferences(extensionId: String): List<com.example.domain.model.ExtensionPreference> = emptyList()
    suspend fun setExtensionPreference(extensionId: String, key: String, value: Any): AppResult<Unit> = AppResult.Success(Unit)
}

class DefaultExtensionRuntimeManager(
    val builtInRuntime: BuiltInExtensionRuntime = BuiltInExtensionRuntime(),
    val sandboxedRuntime: com.example.domain.extension.runtime.SandboxedExtensionRuntime = com.example.domain.extension.runtime.SandboxedExtensionRuntime(),
    val runtimeFactory: ExtensionRuntimeFactory = DefaultExtensionRuntimeFactory(builtInRuntime, sandboxedRuntime),
    val validator: ExtensionValidator = DefaultExtensionValidator(),
    val logger: ExtensionLogger = DefaultExtensionLogger(),
    currentAppVersion: String = "1.0.0"
) : ExtensionRuntimeManager {

    private val loader: ExtensionLoader = DefaultExtensionLoader(
        validator = validator,
        runtimeFactory = runtimeFactory,
        logger = logger,
        currentAppVersion = currentAppVersion
    )

    private val instances = ConcurrentHashMap<String, AnimeExtension>()
    private val _loadedExtensions = MutableStateFlow<Map<String, AnimeExtension>>(emptyMap())
    override val loadedExtensions: StateFlow<Map<String, AnimeExtension>> = _loadedExtensions.asStateFlow()

    private val _runtimeStatuses = MutableStateFlow<Map<String, ExtensionRuntimeStatus>>(emptyMap())
    override val runtimeStatuses: StateFlow<Map<String, ExtensionRuntimeStatus>> = _runtimeStatuses.asStateFlow()

    override val extensionLogs: StateFlow<Map<String, List<ExtensionLogEntry>>> = logger.allLogs

    init {
        updateRuntimeStatuses()
    }

    private fun updateRuntimeStatuses() {
        val map = runtimeFactory.getAllRuntimes().associate { it.runtimeType to it.getStatus() }
        _runtimeStatuses.value = map
    }

    override fun registerRuntime(runtime: ExtensionRuntime) {
        runtimeFactory.registerRuntime(runtime)
        updateRuntimeStatuses()
        AppLogger.i("ExtensionRuntimeManager", "Registered runtime engine: ${runtime.runtimeType}")
    }

    override fun getRuntime(runtimeType: String): ExtensionRuntime? {
        return runtimeFactory.getRuntime(runtimeType)
    }

    override fun getAllRuntimes(): List<ExtensionRuntime> {
        return runtimeFactory.getAllRuntimes()
    }

    override suspend fun load(manifest: ExtensionManifest, payload: ByteArray?): AppResult<AnimeExtension> {
        val result = loader.loadExtension(manifest, payload)
        if (result is AppResult.Success) {
            instances[manifest.id] = result.data
            _loadedExtensions.value = instances.toMap()
            updateRuntimeStatuses()
        }
        return result
    }

    override suspend fun unload(extensionId: String): AppResult<Unit> {
        val result = loader.unloadExtension(extensionId)
        instances.remove(extensionId)
        _loadedExtensions.value = instances.toMap()
        updateRuntimeStatuses()
        return result
    }

    override suspend fun reload(manifest: ExtensionManifest, payload: ByteArray?): AppResult<AnimeExtension> {
        val result = loader.reloadExtension(manifest, payload)
        if (result is AppResult.Success) {
            instances[manifest.id] = result.data
            _loadedExtensions.value = instances.toMap()
            updateRuntimeStatuses()
        }
        return result
    }

    override suspend fun update(newManifest: ExtensionManifest, payload: ByteArray?): AppResult<AnimeExtension> {
        val extId = newManifest.id
        val existingExtension = instances[extId]

        // 1. Capture existing preferences to preserve across updates
        val existingPreferences = try {
            getExtensionPreferences(extId)
        } catch (e: Exception) {
            emptyList()
        }

        // 2. Load the updated extension (isolated in target runtime)
        val loadResult = loader.loadExtension(newManifest, payload)
        if (loadResult !is AppResult.Success) {
            // Rollback: Keep existing instance active, report failure
            AppLogger.e("ExtensionRuntimeManager", "Update failed for '$extId', rolling back: ${(loadResult as AppResult.Error).error.message}")
            return loadResult
        }

        val newInstance = loadResult.data

        // 3. Migrate preferences
        for (pref in existingPreferences) {
            try {
                val valueToMigrate = pref.preferenceValue ?: pref.preferenceDefault
                if (valueToMigrate != null) {
                    newInstance.setPreference(pref.key, valueToMigrate)
                }
            } catch (e: Exception) {
                // Non-fatal preference migration issue
            }
        }

        // 4. Atomically swap active reference
        instances[extId] = newInstance
        _loadedExtensions.value = instances.toMap()
        updateRuntimeStatuses()
        logger.i(extId, "Lifecycle", "Successfully updated extension to version ${newManifest.version}")

        return AppResult.Success(newInstance)
    }

    override fun getLifecycleState(extensionId: String): com.example.domain.extension.runtime.ExtensionLifecycleState {
        for (runtime in runtimeFactory.getAllRuntimes()) {
            if (runtime is com.example.domain.extension.runtime.SandboxedExtensionRuntime) {
                val state = runtime.getLifecycleState(extensionId)
                if (state != com.example.domain.extension.runtime.ExtensionLifecycleState.UNLOADED) return state
            }
        }
        return if (instances.containsKey(extensionId)) {
            com.example.domain.extension.runtime.ExtensionLifecycleState.ACTIVE
        } else {
            com.example.domain.extension.runtime.ExtensionLifecycleState.UNLOADED
        }
    }

    override fun getExtension(extensionId: String): AnimeExtension? {
        return instances[extensionId]
    }

    override fun isLoaded(extensionId: String): Boolean {
        return instances.containsKey(extensionId)
    }

    override fun getStats(extensionId: String): ExtensionRuntimeStats? {
        for (runtime in runtimeFactory.getAllRuntimes()) {
            val stats = runtime.getRuntimeStats(extensionId)
            if (stats != null) return stats
        }
        return null
    }

    override fun getLogs(extensionId: String): List<ExtensionLogEntry> {
        return logger.getLogs(extensionId)
    }

    override fun clearLogs(extensionId: String) {
        logger.clearLogs(extensionId)
    }

    override suspend fun shutdown(): AppResult<Unit> {
        instances.clear()
        _loadedExtensions.value = emptyMap()
        for (runtime in runtimeFactory.getAllRuntimes()) {
            runtime.shutdown()
        }
        updateRuntimeStatuses()
        return AppResult.Success(Unit)
    }

    override suspend fun getExtensionPreferences(extensionId: String): List<com.example.domain.model.ExtensionPreference> {
        for (runtime in runtimeFactory.getAllRuntimes()) {
            if (runtime.getLoadedExtensionIds().contains(extensionId)) {
                return runtime.getExtensionPreferences(extensionId)
            }
        }
        val ext = instances[extensionId] ?: builtInRuntime.getLoadedExtension(extensionId)
        return ext?.getPreferences() ?: emptyList()
    }

    override suspend fun setExtensionPreference(
        extensionId: String,
        key: String,
        value: Any
    ): AppResult<Unit> {
        for (runtime in runtimeFactory.getAllRuntimes()) {
            if (runtime.getLoadedExtensionIds().contains(extensionId)) {
                return runtime.setExtensionPreference(extensionId, key, value)
            }
        }
        return builtInRuntime.setExtensionPreference(extensionId, key, value)
    }
}
