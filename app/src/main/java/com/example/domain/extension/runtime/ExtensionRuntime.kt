package com.example.domain.extension.runtime

import com.example.core.result.AppResult
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.logging.ExtensionRuntimeStats
import com.example.domain.model.ExtensionManifest

enum class ExtensionRuntimeState {
    UNINITIALIZED,
    READY,
    ERROR,
    SHUTDOWN
}

data class ExtensionRuntimeStatus(
    val runtimeType: String,
    val version: String,
    val state: ExtensionRuntimeState,
    val loadedCount: Int = 0,
    val isAvailable: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Isolated Extension Runtime contract.
 *
 * All execution of extensions is sandboxed and managed behind this interface.
 * The core application interacts exclusively with the normalized [AnimeExtension] contract,
 * without knowing any provider-specific or runtime-specific execution details.
 */
interface ExtensionRuntime {
    /**
     * Unique identifier for this runtime engine (e.g., "builtin", "isolated-sandbox").
     */
    val runtimeType: String

    /**
     * Version of this runtime implementation.
     */
    val version: String

    /**
     * Whether this runtime is healthy and ready to process requests.
     */
    val isAvailable: Boolean

    /**
     * Inspects whether this runtime can execute the given manifest.
     */
    fun canHandle(manifest: ExtensionManifest): Boolean

    /**
     * Initializes any runtime dependencies or isolation containers.
     */
    suspend fun initialize(): AppResult<Unit>

    /**
     * Loads and initializes an [AnimeExtension] instance within this runtime's isolation sandbox.
     */
    suspend fun loadExtension(
        manifest: ExtensionManifest,
        payload: ByteArray? = null
    ): AppResult<AnimeExtension>

    /**
     * Unloads and cleans up all memory/resources associated with an extension.
     */
    suspend fun unloadExtension(extensionId: String): AppResult<Unit>

    /**
     * Returns an active loaded extension instance if present.
     */
    suspend fun getLoadedExtension(extensionId: String): AnimeExtension?

    /**
     * Returns the set of all extension IDs currently loaded into this runtime.
     */
    fun getLoadedExtensionIds(): Set<String>

    /**
     * Returns runtime execution and health statistics for a specific extension.
     */
    fun getRuntimeStats(extensionId: String): ExtensionRuntimeStats?

    /**
     * Gets the current operational status of this runtime.
     */
    fun getStatus(): ExtensionRuntimeStatus

    /**
     * Gracefully tears down the runtime and all hosted extensions.
     */
    suspend fun shutdown(): AppResult<Unit>

    /**
     * Retrieves dynamic preferences exposed by an extension hosted in this runtime.
     */
    suspend fun getExtensionPreferences(extensionId: String): List<com.example.domain.model.ExtensionPreference> = emptyList()

    /**
     * Sends dynamic preference changes back to the extension hosted in this runtime.
     */
    suspend fun setExtensionPreference(
        extensionId: String,
        key: String,
        value: Any
    ): AppResult<Unit> = AppResult.Success(Unit)
}
