package com.example.domain.extension

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.extension.repository.DefaultRepositoryFetcher
import com.example.data.extension.repository.RepositoryFetcher
import com.example.domain.extension.logging.DefaultExtensionLogger
import com.example.domain.extension.logging.ExtensionLogEntry
import com.example.domain.extension.logging.ExtensionLogger
import com.example.domain.extension.logging.ExtensionRuntimeStats
import com.example.domain.extension.manager.DefaultExtensionRuntimeManager
import com.example.domain.extension.manager.ExtensionRuntimeManager
import com.example.domain.extension.parser.DefaultExtensionManifestParser
import com.example.domain.extension.parser.ExtensionManifestParser
import com.example.domain.extension.runtime.BuiltInExtensionRuntime
import com.example.domain.extension.runtime.ExtensionRuntime
import com.example.domain.extension.runtime.ExtensionRuntimeStatus
import com.example.domain.extension.validator.DefaultExtensionValidator
import com.example.domain.extension.validator.ExtensionValidationResult
import com.example.domain.extension.validator.ExtensionValidator
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionInfo
import com.example.domain.model.ExtensionInstallState
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.ExtensionRepository
import com.example.domain.model.RepositoryIndex
import com.example.domain.model.VideoSource
import com.example.domain.repository.ExtensionStorageRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.util.VersionComparator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

interface ExtensionManager {
    val allExtensions: StateFlow<List<ExtensionInfo>>
    val enabledExtensions: StateFlow<List<AnimeExtension>>
    val preferredExtension: StateFlow<AnimeExtension?>
    val availableRepositoryExtensions: StateFlow<List<ExtensionInfo>>
    val repositories: Flow<List<ExtensionRepository>>
    val runtimeStatuses: StateFlow<Map<String, ExtensionRuntimeStatus>>

    fun registerExtension(extension: AnimeExtension)
    fun registerRuntime(runtime: ExtensionRuntime)
    fun getExtension(extensionId: String): AnimeExtension?
    fun getExtensionStats(extensionId: String): ExtensionRuntimeStats?
    fun getExtensionLogs(extensionId: String): List<ExtensionLogEntry>

    // Repository Management
    suspend fun addRepository(name: String, url: String): AppResult<ExtensionRepository>
    suspend fun removeRepository(repositoryId: String): AppResult<Unit>
    suspend fun setRepositoryEnabled(repositoryId: String, isEnabled: Boolean): AppResult<Unit>
    suspend fun syncRepositories(): AppResult<Unit>
    suspend fun checkUpdates(): AppResult<List<ExtensionManifest>>

    // Extension Management
    suspend fun setExtensionEnabled(extensionId: String, isEnabled: Boolean): AppResult<Unit>
    suspend fun setPreferredExtension(extensionId: String): AppResult<Unit>
    suspend fun setExtensionOrder(extensionId: String, order: Int): AppResult<Unit>
    suspend fun reorderExtensions(orderedIds: List<String>): AppResult<Unit>
    suspend fun installExtension(manifest: ExtensionManifest): AppResult<Unit>
    suspend fun updateExtension(manifest: ExtensionManifest): AppResult<Unit>
    suspend fun uninstallExtension(extensionId: String): AppResult<Unit>
    suspend fun getExtensionPreferences(extensionId: String): List<ExtensionPreference>
    suspend fun setExtensionPreference(extensionId: String, key: String, value: Any): AppResult<Unit>

    // Content Operations
    suspend fun searchAll(query: String): List<Anime>
    suspend fun getAnimeDetails(sourceId: String, sourceAnimeId: String): AppResult<Anime>
    suspend fun getEpisodes(sourceId: String, sourceAnimeId: String): AppResult<List<Episode>>
    suspend fun getStreams(sourceId: String, sourceEpisodeId: String): AppResult<List<VideoSource>>
}

class DefaultExtensionManager(
    private val storageRepository: ExtensionStorageRepository,
    private val settingsRepository: SettingsRepository,
    private val repositoryFetcher: RepositoryFetcher = DefaultRepositoryFetcher(),
    val runtimeManager: ExtensionRuntimeManager = DefaultExtensionRuntimeManager(),
    private val validator: ExtensionValidator = DefaultExtensionValidator(),
    private val manifestParser: ExtensionManifestParser = DefaultExtensionManifestParser(),
    private val mangayomiInstaller: com.example.data.extension.mangayomi.installer.MangayomiExtensionInstaller? = null,
    private val sourceRegistry: com.example.domain.source.SourceRegistry? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ExtensionManager {

    private val _registeredExtensions = MutableStateFlow<Map<String, AnimeExtension>>(emptyMap())
    private val _remoteManifests = MutableStateFlow<List<ExtensionManifest>>(emptyList())

    override val runtimeStatuses: StateFlow<Map<String, ExtensionRuntimeStatus>> =
        runtimeManager.runtimeStatuses

    init {
        scope.launch {
            mangayomiInstaller?.loadSavedExtensions()
        }
        scope.launch {
            // Keep database synced with registered built-in extensions
            _registeredExtensions.collect { registeredMap ->
                registeredMap.values.forEach { ext ->
                    storageRepository.saveInstalledExtension(ext.manifest)
                }
            }
        }
    }

    override val repositories: Flow<List<ExtensionRepository>> = storageRepository.getAllRepositories()

    override val allExtensions: StateFlow<List<ExtensionInfo>> = combine(
        storageRepository.getInstalledExtensions(),
        _registeredExtensions,
        _remoteManifests
    ) { dbManifests, registeredMap, remoteList ->
        val mergedMap = mutableMapOf<String, ExtensionManifest>()

        // Add registered in-memory extensions
        registeredMap.values.forEach { ext ->
            mergedMap[ext.id] = ext.manifest
        }

        // Merge stored DB manifests
        dbManifests.forEach { dbItem ->
            val existing = mergedMap[dbItem.id]
            val remote = remoteList.firstOrNull { it.id == dbItem.id }
            val hasUpdate = if (remote != null) {
                VersionComparator.isUpdateAvailable(dbItem.version, remote.version)
            } else {
                dbItem.hasUpdate
            }

            mergedMap[dbItem.id] = dbItem.copy(
                isBuiltIn = existing?.isBuiltIn ?: false,
                hasUpdate = hasUpdate
            )
        }

        mergedMap.values.map { manifest ->
            val isLoaded = runtimeManager.isLoaded(manifest.id)
            val installState = when {
                !manifest.isInstalled -> ExtensionInstallState.NOT_INSTALLED
                manifest.hasUpdate -> ExtensionInstallState.UPDATE_AVAILABLE
                else -> ExtensionInstallState.INSTALLED
            }
            ExtensionInfo(
                manifest = manifest,
                isLoaded = isLoaded,
                installState = installState
            )
        }.sortedBy { it.manifest.order }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val availableRepositoryExtensions: StateFlow<List<ExtensionInfo>> = combine(
        allExtensions,
        _remoteManifests
    ) { installedList, remoteList ->
        val installedMap = installedList.associateBy { it.manifest.id }

        remoteList.map { remoteManifest ->
            val installed = installedMap[remoteManifest.id]
            if (installed != null) {
                val hasUpdate = VersionComparator.isUpdateAvailable(installed.manifest.version, remoteManifest.version)
                val state = if (hasUpdate) ExtensionInstallState.UPDATE_AVAILABLE else ExtensionInstallState.INSTALLED
                ExtensionInfo(
                    manifest = remoteManifest.copy(
                        isInstalled = true,
                        hasUpdate = hasUpdate,
                        isEnabled = installed.manifest.isEnabled
                    ),
                    isLoaded = installed.isLoaded,
                    installState = state
                )
            } else {
                ExtensionInfo(
                    manifest = remoteManifest.copy(isInstalled = false),
                    isLoaded = false,
                    installState = ExtensionInstallState.NOT_INSTALLED
                )
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val enabledExtensions: StateFlow<List<AnimeExtension>> = combine(
        allExtensions,
        _registeredExtensions,
        runtimeManager.loadedExtensions
    ) { infos, registered, loaded ->
        val activeIds = infos.filter { it.manifest.isEnabled && it.manifest.isInstalled }.map { it.manifest.id }.toSet()
        val result = if (activeIds.isNotEmpty()) {
            activeIds.mapNotNull { loaded[it] ?: registered[it] }
        } else {
            registered.values.filter { it.manifest.isEnabled && it.manifest.isInstalled }
        }
        result.ifEmpty { registered.values.filter { it.manifest.isEnabled && it.manifest.isInstalled } }
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        _registeredExtensions.value.values.filter { it.manifest.isEnabled && it.manifest.isInstalled }
    )

    override val preferredExtension: StateFlow<AnimeExtension?> = combine(
        settingsRepository.settings,
        enabledExtensions,
        _registeredExtensions
    ) { settings, extensions, registered ->
        val list = extensions.ifEmpty { registered.values.filter { it.manifest.isEnabled && it.manifest.isInstalled } }
        val preferredId = settings.defaultExtensionId
        if (preferredId.isNotBlank()) {
            list.firstOrNull { it.id == preferredId } ?: list.firstOrNull()
        } else {
            list.firstOrNull()
        }
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        _registeredExtensions.value.values.firstOrNull { it.manifest.isEnabled && it.manifest.isInstalled }
    )

    override fun registerRuntime(runtime: ExtensionRuntime) {
        runtimeManager.registerRuntime(runtime)
    }

    override fun registerExtension(extension: AnimeExtension) {
        // Register in built-in runtime
        if (runtimeManager is DefaultExtensionRuntimeManager) {
            runtimeManager.builtInRuntime.register(extension)
        }
        scope.launch {
            runtimeManager.load(extension.manifest)
        }
        val safeWrapper = SafeAnimeExtension(extension)
        val currentMap = _registeredExtensions.value.toMutableMap()
        currentMap[extension.id] = safeWrapper
        _registeredExtensions.value = currentMap
        AppLogger.d("ExtensionManager", "Registered extension: ${extension.id} (${extension.name})")
    }

    override fun getExtension(extensionId: String): AnimeExtension? {
        val runtimeExt = runtimeManager.getExtension(extensionId)
        if (runtimeExt != null) return runtimeExt

        val registeredExt = _registeredExtensions.value[extensionId]
        if (registeredExt != null) return registeredExt

        val sourceExt = sourceRegistry?.getSource(extensionId)
        if (sourceExt is AnimeExtension) return sourceExt

        return null
    }

    override fun getExtensionStats(extensionId: String): ExtensionRuntimeStats? {
        return runtimeManager.getStats(extensionId)
    }

    override fun getExtensionLogs(extensionId: String): List<ExtensionLogEntry> {
        return runtimeManager.getLogs(extensionId)
    }

    override suspend fun addRepository(name: String, url: String): AppResult<ExtensionRepository> {
        val trimmedUrl = url.trim()
        val repo = ExtensionRepository(
            id = "repo_${System.currentTimeMillis()}_${trimmedUrl.hashCode()}",
            name = name.ifBlank { "Community Repo" },
            url = trimmedUrl,
            version = 1,
            isEnabled = true,
            extensionCount = 0
        )
        val saveResult = storageRepository.saveRepository(repo)
        if (saveResult is AppResult.Error) return saveResult

        // Sync repository immediately
        syncRepositories()
        return AppResult.Success(repo)
    }

    override suspend fun removeRepository(repositoryId: String): AppResult<Unit> {
        return storageRepository.deleteRepository(repositoryId)
    }

    override suspend fun setRepositoryEnabled(repositoryId: String, isEnabled: Boolean): AppResult<Unit> {
        return storageRepository.setRepositoryEnabled(repositoryId, isEnabled)
    }

    override suspend fun syncRepositories(): AppResult<Unit> {
        return try {
            val allRepos = mutableListOf<ExtensionManifest>()
            var activeRepos = storageRepository.getEnabledRepositories().first()

            if (activeRepos.isEmpty()) {
                val defaultRepo = ExtensionRepository(
                    id = "repo_mallyd11",
                    name = "Mallyd11 Mangayomi Extensions",
                    url = "https://raw.githubusercontent.com/Mallyd11/mangayomi-anime-extensions/main/anime_index.json",
                    isEnabled = true
                )
                storageRepository.saveRepository(defaultRepo)
                activeRepos = listOf(defaultRepo)
            }

            for (repo in activeRepos) {
                when (val result = repositoryFetcher.fetchRepositoryIndex(repo.url)) {
                    is AppResult.Success -> {
                        val index = result.data
                        val validManifests = index.extensions.filter { manifest ->
                            validator.validate(manifest).isValid
                        }
                        allRepos.addAll(validManifests)
                        storageRepository.updateRepositorySyncInfo(repo.id, validManifests.size)
                    }
                    is AppResult.Error -> {
                        AppLogger.w("ExtensionManager", "Failed to sync repo ${repo.name}: ${result.error.message}")
                    }
                    is AppResult.Loading -> Unit
                }
            }

            _remoteManifests.value = allRepos
            checkUpdates()
            mangayomiInstaller?.loadSavedExtensions()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionManager", "Failed syncing repositories", e)
            AppResult.Error(AppError.NetworkError("Failed syncing repositories", cause = e))
        }
    }

    override suspend fun checkUpdates(): AppResult<List<ExtensionManifest>> {
        val updates = mutableListOf<ExtensionManifest>()
        val installedDb = storageRepository.getInstalledExtensions().first()
        val installedList = if (installedDb.isNotEmpty()) installedDb else _registeredExtensions.value.values.map { it.manifest }
        val remotes = _remoteManifests.value.associateBy { it.id }

        for (manifest in installedList) {
            val remote = remotes[manifest.id]
            if (remote != null && VersionComparator.isUpdateAvailable(manifest.version, remote.version)) {
                storageRepository.setHasUpdate(manifest.id, true)
                updates.add(remote)
            } else if (manifest.hasUpdate) {
                storageRepository.setHasUpdate(manifest.id, false)
            }
        }
        return AppResult.Success(updates)
    }

    override suspend fun setExtensionEnabled(extensionId: String, isEnabled: Boolean): AppResult<Unit> {
        val result = storageRepository.setExtensionEnabled(extensionId, isEnabled)
        if (result is AppResult.Success) {
            val current = getExtension(extensionId)
            if (current != null) {
                val updated = current.manifest.copy(isEnabled = isEnabled)
                storageRepository.saveInstalledExtension(updated)
            }
        }
        return result
    }

    override suspend fun setPreferredExtension(extensionId: String): AppResult<Unit> {
        return try {
            settingsRepository.setDefaultExtensionId(extensionId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionManager", "Failed to set preferred extension: $extensionId", e)
            AppResult.Error(AppError.StorageError("Failed to set preferred extension", e))
        }
    }

    override suspend fun setExtensionOrder(extensionId: String, order: Int): AppResult<Unit> {
        return storageRepository.setExtensionOrder(extensionId, order)
    }

    override suspend fun reorderExtensions(orderedIds: List<String>): AppResult<Unit> {
        return storageRepository.reorderExtensions(orderedIds)
    }

    override suspend fun installExtension(manifest: ExtensionManifest): AppResult<Unit> {
        return try {
            val validation = validator.validate(manifest)
            if (!validation.isValid) {
                return when (validation) {
                    is ExtensionValidationResult.Invalid -> AppResult.Error(AppError.ValidationError(validation.errors.joinToString("; ")))
                    is ExtensionValidationResult.Incompatible -> AppResult.Error(AppError.GeneralError(validation.reason))
                    else -> AppResult.Error(AppError.GeneralError("Extension validation failed"))
                }
            }

            if (mangayomiInstaller != null && manifest.sourceCodeUrl.isNotBlank()) {
                val mangayomiManifest = com.example.data.extension.mangayomi.model.MangayomiExtensionManifest(
                    id = manifest.id,
                    name = manifest.name,
                    version = manifest.version,
                    versionCode = manifest.versionCode,
                    lang = manifest.language,
                    baseUrl = manifest.baseUrl,
                    iconUrl = manifest.iconUrl,
                    sourceCodeUrl = manifest.sourceCodeUrl,
                    itemType = 1,
                    appMinVerReq = "0.1.0"
                )
                val installRes = mangayomiInstaller.installExtension(mangayomiManifest)
                if (installRes is AppResult.Success) {
                    val adapter = installRes.data
                    val safeWrapper = SafeAnimeExtension(adapter)
                    val currentMap = _registeredExtensions.value.toMutableMap()
                    currentMap[manifest.id] = safeWrapper
                    _registeredExtensions.value = currentMap
                    val installedManifest = manifest.copy(
                        isInstalled = true,
                        isEnabled = true,
                        hasUpdate = false
                    )
                    storageRepository.saveInstalledExtension(installedManifest)
                    return AppResult.Success(Unit)
                } else if (installRes is AppResult.Error) {
                    return installRes
                }
            }

            when (val loadResult = runtimeManager.load(manifest)) {
                is AppResult.Success -> {
                    val installedManifest = manifest.copy(
                        isInstalled = true,
                        isEnabled = true,
                        hasUpdate = false
                    )
                    storageRepository.saveInstalledExtension(installedManifest)
                    AppResult.Success(Unit)
                }
                is AppResult.Error -> {
                    val installedManifest = manifest.copy(
                        isInstalled = true,
                        isEnabled = true,
                        hasUpdate = false
                    )
                    storageRepository.saveInstalledExtension(installedManifest)
                    AppResult.Success(Unit)
                }
                is AppResult.Loading -> AppResult.Success(Unit)
            }
        } catch (e: Exception) {
            AppLogger.e("ExtensionManager", "Failed to install extension ${manifest.id}", e)
            AppResult.Error(AppError.StorageError("Failed to install extension", e))
        }
    }

    override suspend fun updateExtension(manifest: ExtensionManifest): AppResult<Unit> {
        return installExtension(manifest)
    }

    override suspend fun uninstallExtension(extensionId: String): AppResult<Unit> {
        val ext = getExtension(extensionId)
        if (ext?.manifest?.isBuiltIn == true) {
            return AppResult.Error(AppError.GeneralError("Cannot uninstall built-in extension"))
        }

        mangayomiInstaller?.uninstallExtension(extensionId)
        runtimeManager.unload(extensionId)
        val currentMap = _registeredExtensions.value.toMutableMap()
        currentMap.remove(extensionId)
        _registeredExtensions.value = currentMap
        return storageRepository.deleteExtension(extensionId)
    }

    override suspend fun getExtensionPreferences(extensionId: String): List<ExtensionPreference> {
        val runtimePrefs = runtimeManager.getExtensionPreferences(extensionId)
        if (runtimePrefs.isNotEmpty()) return runtimePrefs
        val ext = getExtension(extensionId) ?: return emptyList()
        return ext.getPreferences()
    }

    override suspend fun setExtensionPreference(
        extensionId: String,
        key: String,
        value: Any
    ): AppResult<Unit> {
        // Send change back to the extension through ExtensionRuntime
        val runtimeResult = runtimeManager.setExtensionPreference(extensionId, key, value)
        if (runtimeResult is AppResult.Success) {
            return runtimeResult
        }
        val ext = getExtension(extensionId)
            ?: return AppResult.Error(AppError.NotFoundError("Extension '$extensionId' not found"))
        ext.setPreference(key, value)
        return AppResult.Success(Unit)
    }

    override suspend fun searchAll(query: String): List<Anime> {
        val activeExtensions = enabledExtensions.value.ifEmpty {
            _registeredExtensions.value.values.filter { it.manifest.isEnabled && it.manifest.isInstalled }
        }
        if (activeExtensions.isEmpty()) return emptyList()

        return activeExtensions.map { ext ->
            scope.async<List<Anime>> {
                when (val result = ext.searchAnime(query)) {
                    is AppResult.Success -> result.data
                    is AppResult.Error -> {
                        AppLogger.w("ExtensionManager", "Search failed on extension ${ext.name}: ${result.error.message}")
                        emptyList()
                    }
                    is AppResult.Loading -> emptyList()
                }
            }
        }.awaitAll().flatten()
    }

    override suspend fun getAnimeDetails(sourceId: String, sourceAnimeId: String): AppResult<Anime> {
        val ext = getExtension(sourceId)
            ?: return AppResult.Error(AppError.NotFoundError("Extension '$sourceId' not found or disabled"))
        return ext.getAnimeDetails(sourceAnimeId)
    }

    override suspend fun getEpisodes(sourceId: String, sourceAnimeId: String): AppResult<List<Episode>> {
        val ext = getExtension(sourceId)
            ?: return AppResult.Error(AppError.NotFoundError("Extension '$sourceId' not found or disabled"))
        return ext.getEpisodes(sourceAnimeId)
    }

    override suspend fun getStreams(sourceId: String, sourceEpisodeId: String): AppResult<List<VideoSource>> {
        val ext = getExtension(sourceId)
            ?: return AppResult.Error(AppError.NotFoundError("Extension '$sourceId' not found or disabled"))
        return ext.getEpisodeStreams(sourceEpisodeId)
    }
}
