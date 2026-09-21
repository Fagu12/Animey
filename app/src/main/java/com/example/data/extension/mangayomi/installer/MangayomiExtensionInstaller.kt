package com.example.data.extension.mangayomi.installer

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.extension.mangayomi.model.MangayomiExtensionManifest
import com.example.data.extension.mangayomi.repository.DefaultMangayomiRepositoryClient
import com.example.data.extension.mangayomi.repository.MangayomiRepositoryClient
import com.example.data.extension.mangayomi.runtime.MangayomiRuntime
import com.example.data.extension.mangayomi.storage.DefaultInstalledExtensionStore
import com.example.data.extension.mangayomi.storage.InstalledExtensionStore
import com.example.data.source.adapter.MangayomiSourceAdapter
import com.example.domain.model.ExtensionManifest
import com.example.domain.repository.ExtensionStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Orchestrates the full lifecycle of installing, updating, persisting, and initializing
 * real Mangayomi JavaScript anime extensions.
 */
interface MangayomiExtensionInstaller {
    suspend fun installExtension(
        manifest: MangayomiExtensionManifest,
        scriptOverride: String? = null
    ): AppResult<MangayomiSourceAdapter>

    suspend fun updateExtension(
        manifest: MangayomiExtensionManifest,
        scriptOverride: String? = null
    ): AppResult<MangayomiSourceAdapter>

    suspend fun uninstallExtension(extensionId: String): AppResult<Unit>

    suspend fun loadSavedExtensions(): AppResult<Int>
}

class DefaultMangayomiExtensionInstaller(
    private val runtime: MangayomiRuntime,
    private val storageRepository: ExtensionStorageRepository,
    private val extensionStore: InstalledExtensionStore = DefaultInstalledExtensionStore(),
    private val repositoryClient: MangayomiRepositoryClient = DefaultMangayomiRepositoryClient()
) : MangayomiExtensionInstaller {

    companion object {
        private const val TAG = "MangayomiInstaller"
    }

    override suspend fun installExtension(
        manifest: MangayomiExtensionManifest,
        scriptOverride: String?
    ): AppResult<MangayomiSourceAdapter> = withContext(Dispatchers.IO) {
        AppLogger.i(TAG, "Starting installation of extension ${manifest.id} (${manifest.name})")

        // 1. Validate manifest contract
        val validationErr = validateManifest(manifest)
        if (validationErr != null) {
            return@withContext AppResult.Error(validationErr)
        }

        // 2. Fetch or obtain JavaScript source
        val scriptContent = if (!scriptOverride.isNullOrBlank()) {
            scriptOverride
        } else if (manifest.scriptContent.isNotBlank()) {
            manifest.scriptContent
        } else if (manifest.sourceCodeUrl.isNotBlank()) {
            when (val downloadResult = repositoryClient.downloadExtensionSource(manifest.sourceCodeUrl)) {
                is AppResult.Success -> downloadResult.data
                is AppResult.Error -> return@withContext AppResult.Error(
                    AppError.NetworkError("Failed downloading extension script: ${downloadResult.error.message}")
                )
                is AppResult.Loading -> return@withContext AppResult.Error(AppError.GeneralError("Unexpected loading state"))
            }
        } else {
            return@withContext AppResult.Error(
                AppError.ValidationError("No sourceCodeUrl or scriptContent provided for extension ${manifest.id}")
            )
        }

        if (scriptContent.isBlank()) {
            return@withContext AppResult.Error(AppError.ValidationError("Extension script content is empty"))
        }

        // 3. Prepare updated manifest with script content
        val updatedManifest = manifest.copy(
            scriptContent = scriptContent,
            installedAt = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis(),
            isEnabled = true
        )

        // 4. Test load extension in Mangayomi JS runtime FIRST
        when (val loadResult = runtime.loadExtension(updatedManifest)) {
            is AppResult.Success -> {
                val adapter = loadResult.data
                // 5. Smoke Test
                AppLogger.i(TAG, "Running smoke test on extension ${manifest.id}")
                val smokeTestRes = adapter.getPopularAnime(1)
                if (smokeTestRes is AppResult.Success || (smokeTestRes is AppResult.Error && !smokeTestRes.error.message.orEmpty().contains("SyntaxError"))) {
                    // 6. Smoke test passed -> Persist script file and save to Room DB
                    val saveScriptResult = extensionStore.saveScript(manifest.id, scriptContent)
                    val installedPath = if (saveScriptResult is AppResult.Success) saveScriptResult.data else ""

                    val domainManifest = ExtensionManifest(
                        id = manifest.id,
                        name = manifest.name,
                        version = manifest.version,
                        language = manifest.lang,
                        author = manifest.author.ifBlank { "Community" },
                        description = manifest.description.ifBlank { "Mangayomi Anime Source" },
                        iconUrl = manifest.iconUrl,
                        downloadUrl = manifest.sourceCodeUrl,
                        isInstalled = true,
                        isEnabled = true,
                        hasUpdate = false,
                        order = 0,
                        isBuiltIn = false
                    )
                    storageRepository.saveInstalledExtension(domainManifest)

                    AppLogger.i(TAG, "Successfully installed and verified extension ${manifest.id}")
                    AppResult.Success(adapter)
                } else {
                    val errorMsg = if (smokeTestRes is AppResult.Error) smokeTestRes.error.message else "Smoke test failed"
                    AppLogger.e(TAG, "Smoke test failed for ${manifest.id}: $errorMsg. Rolling back.")
                    runtime.removeExtension(manifest.id)
                    AppResult.Error(AppError.ExtensionError(manifest.id, "Extension smoke test failed: $errorMsg"))
                }
            }
            is AppResult.Error -> {
                AppLogger.e(TAG, "Extension ${manifest.id} runtime initialization failed: ${loadResult.error.message}")
                runtime.removeExtension(manifest.id)
                AppResult.Error(loadResult.error)
            }
            is AppResult.Loading -> AppResult.Error(AppError.GeneralError("Unexpected loading state"))
        }
    }

    override suspend fun updateExtension(
        manifest: MangayomiExtensionManifest,
        scriptOverride: String?
    ): AppResult<MangayomiSourceAdapter> {
        return installExtension(manifest, scriptOverride)
    }

    override suspend fun uninstallExtension(extensionId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        AppLogger.i(TAG, "Uninstalling extension $extensionId")
        runtime.removeExtension(extensionId)
        extensionStore.deleteScript(extensionId)
        storageRepository.deleteExtension(extensionId)
        AppResult.Success(Unit)
    }

    override suspend fun loadSavedExtensions(): AppResult<Int> = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val installedIds = extensionStore.listInstalledExtensionIds()
            val dbManifests = storageRepository.getInstalledExtensions().first().associateBy { it.id }

            for (id in installedIds) {
                when (val scriptRes = extensionStore.readScript(id)) {
                    is AppResult.Success -> {
                        val dbManifest = dbManifests[id]
                        val manifest = MangayomiExtensionManifest(
                            id = id,
                            name = dbManifest?.name ?: id.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                            lang = dbManifest?.language ?: "en",
                            version = dbManifest?.version ?: "1.0.0",
                            baseUrl = "",
                            iconUrl = dbManifest?.iconUrl ?: "",
                            description = dbManifest?.description ?: "",
                            author = dbManifest?.author ?: "Community",
                            sourceCodeUrl = dbManifest?.downloadUrl ?: "",
                            scriptContent = scriptRes.data,
                            isEnabled = dbManifest?.isEnabled ?: true
                        )
                        if (runtime.loadExtension(manifest) is AppResult.Success) {
                            count++
                        }
                    }
                    else -> Unit
                }
            }
            AppResult.Success(count)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error loading saved extensions", e)
            AppResult.Error(AppError.StorageError("Failed restoring saved extensions", e))
        }
    }

    private fun validateManifest(manifest: MangayomiExtensionManifest): AppError? {
        if (manifest.id.isBlank()) {
            return AppError.ValidationError("Extension ID cannot be blank")
        }
        if (!manifest.id.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
            return AppError.ValidationError("Extension ID contains invalid characters: ${manifest.id}")
        }
        if (manifest.name.isBlank()) {
            return AppError.ValidationError("Extension name cannot be blank")
        }
        if (manifest.itemType != 1 && manifest.itemType != 0 && manifest.itemType != 2) {
            return AppError.ValidationError("Unsupported extension itemType: ${manifest.itemType}")
        }
        return null
    }
}
