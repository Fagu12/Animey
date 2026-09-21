package com.example.domain.extension.loader

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.factory.ExtensionRuntimeFactory
import com.example.domain.extension.logging.DefaultExtensionLogger
import com.example.domain.extension.logging.ExtensionLogger
import com.example.domain.extension.validator.DefaultExtensionValidator
import com.example.domain.extension.validator.ExtensionValidationResult
import com.example.domain.extension.validator.ExtensionValidator
import com.example.domain.model.ExtensionManifest

/**
 * Interface responsible for validating and orchestrating the loading lifecycle of extensions.
 */
interface ExtensionLoader {
    /**
     * Validates and loads an extension manifest into its designated runtime.
     */
    suspend fun loadExtension(
        manifest: ExtensionManifest,
        payload: ByteArray? = null
    ): AppResult<AnimeExtension>

    /**
     * Unloads an extension by ID from whichever runtime currently hosts it.
     */
    suspend fun unloadExtension(extensionId: String): AppResult<Unit>

    /**
     * Reloads an extension with updated manifest or configuration.
     */
    suspend fun reloadExtension(
        manifest: ExtensionManifest,
        payload: ByteArray? = null
    ): AppResult<AnimeExtension>

    /**
     * Checks if an extension is currently loaded in any active runtime.
     */
    fun isLoaded(extensionId: String): Boolean
}

class DefaultExtensionLoader(
    private val validator: ExtensionValidator = DefaultExtensionValidator(),
    private val runtimeFactory: ExtensionRuntimeFactory,
    private val logger: ExtensionLogger = DefaultExtensionLogger(),
    private val currentAppVersion: String = "1.0.0"
) : ExtensionLoader {

    override suspend fun loadExtension(
        manifest: ExtensionManifest,
        payload: ByteArray?
    ): AppResult<AnimeExtension> {
        // 1. Validate manifest and safety rules
        val validationResult = validator.validate(manifest, currentAppVersion)
        when (validationResult) {
            is ExtensionValidationResult.Invalid -> {
                val errorMsg = "Manifest validation failed: ${validationResult.errors.joinToString("; ")}"
                logger.e(manifest.id, "Loader", errorMsg)
                return AppResult.Error(AppError.ValidationError(errorMsg))
            }
            is ExtensionValidationResult.Incompatible -> {
                val errorMsg = "Compatibility violation: ${validationResult.reason}"
                logger.e(manifest.id, "Loader", errorMsg)
                return AppResult.Error(AppError.GeneralError(errorMsg))
            }
            is ExtensionValidationResult.Warning -> {
                logger.w(manifest.id, "Loader", "Validation warnings: ${validationResult.warnings.joinToString("; ")}")
            }
            is ExtensionValidationResult.Valid -> Unit
        }

        // 2. Resolve runtime
        val runtime = runtimeFactory.getRuntimeForManifest(manifest)
            ?: return AppResult.Error(
                AppError.NotFoundError("No compatible ExtensionRuntime found for extension '${manifest.id}'")
            )

        // 3. Delegate to runtime
        logger.i(manifest.id, "Loader", "Loading '${manifest.name}' via runtime '${runtime.runtimeType}'")
        return runtime.loadExtension(manifest, payload)
    }

    override suspend fun unloadExtension(extensionId: String): AppResult<Unit> {
        for (runtime in runtimeFactory.getAllRuntimes()) {
            if (runtime.getLoadedExtensionIds().contains(extensionId)) {
                runtime.unloadExtension(extensionId)
                logger.i(extensionId, "Loader", "Unloaded from runtime '${runtime.runtimeType}'")
            }
        }
        return AppResult.Success(Unit)
    }

    override suspend fun reloadExtension(
        manifest: ExtensionManifest,
        payload: ByteArray?
    ): AppResult<AnimeExtension> {
        unloadExtension(manifest.id)
        return loadExtension(manifest, payload)
    }

    override fun isLoaded(extensionId: String): Boolean {
        return runtimeFactory.getAllRuntimes().any { it.getLoadedExtensionIds().contains(extensionId) }
    }
}
