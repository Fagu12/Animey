package com.example.domain.extension.factory

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.extension.runtime.BuiltInExtensionRuntime
import com.example.domain.extension.runtime.ExtensionRuntime
import com.example.domain.model.ExtensionManifest
import java.util.concurrent.ConcurrentHashMap

/**
 * Factory for creating, retrieving, and resolving appropriate [ExtensionRuntime] engines.
 */
interface ExtensionRuntimeFactory {
    /**
     * Registers a runtime engine with the factory.
     */
    fun registerRuntime(runtime: ExtensionRuntime)

    /**
     * Retrieves a runtime instance by its unique type name (e.g. "builtin").
     */
    fun getRuntime(runtimeType: String): ExtensionRuntime?

    /**
     * Resolves the most appropriate runtime capable of executing the given [manifest].
     */
    fun getRuntimeForManifest(manifest: ExtensionManifest): ExtensionRuntime?

    /**
     * Returns all registered runtime engines.
     */
    fun getAllRuntimes(): List<ExtensionRuntime>

    /**
     * Returns the list of all supported runtime type identifiers.
     */
    fun getAvailableRuntimeTypes(): List<String>
}

class DefaultExtensionRuntimeFactory(
    private val builtInRuntime: BuiltInExtensionRuntime = BuiltInExtensionRuntime(),
    private val sandboxedRuntime: com.example.domain.extension.runtime.SandboxedExtensionRuntime = com.example.domain.extension.runtime.SandboxedExtensionRuntime()
) : ExtensionRuntimeFactory {

    private val runtimes = ConcurrentHashMap<String, ExtensionRuntime>()

    init {
        registerRuntime(builtInRuntime)
        registerRuntime(sandboxedRuntime)
    }

    override fun registerRuntime(runtime: ExtensionRuntime) {
        runtimes[runtime.runtimeType] = runtime
    }

    override fun getRuntime(runtimeType: String): ExtensionRuntime? {
        return runtimes[runtimeType]
    }

    override fun getRuntimeForManifest(manifest: ExtensionManifest): ExtensionRuntime? {
        // Built-ins or registered catalog entries match first
        if (builtInRuntime.canHandle(manifest)) {
            return builtInRuntime
        }
        // Otherwise search registered runtimes
        return runtimes.values.firstOrNull { it.isAvailable && it.canHandle(manifest) }
    }

    override fun getAllRuntimes(): List<ExtensionRuntime> {
        return runtimes.values.toList()
    }

    override fun getAvailableRuntimeTypes(): List<String> {
        return runtimes.keys.toList()
    }
}
