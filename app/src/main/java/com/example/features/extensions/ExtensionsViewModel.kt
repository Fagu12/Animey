package com.example.features.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.logging.AppLogger
import com.example.core.result.AppResult
import com.example.domain.extension.ExtensionManager
import com.example.domain.extension.logging.ExtensionLogEntry
import com.example.domain.model.ExtensionInfo
import com.example.domain.model.ExtensionInstallState
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.ExtensionRepository
import com.example.domain.model.ExtensionType
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExtensionSortOption(val displayName: String) {
    ORDER("Custom Order"),
    NAME("Name (A-Z)"),
    VERSION("Version"),
    LANGUAGE("Language")
}

data class ExtensionsUiState(
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val selectedLanguage: String = "ALL",
    val selectedType: ExtensionType? = null,
    val sortOption: ExtensionSortOption = ExtensionSortOption.ORDER,
    val installedList: List<ExtensionInfo> = emptyList(),
    val availableList: List<ExtensionInfo> = emptyList(),
    val updateList: List<ExtensionInfo> = emptyList(),
    val repositoriesList: List<ExtensionRepository> = emptyList(),
    val availableLanguages: List<String> = listOf("ALL"),
    val preferredExtensionId: String = "",
    val isInitialLoading: Boolean = false,
    val isSyncingRepos: Boolean = false,
    val installingIds: Set<String> = emptySet(),
    val updatingIds: Set<String> = emptySet(),
    val uninstallingIds: Set<String> = emptySet(),
    val userMessage: String? = null,
    val showAddRepoDialog: Boolean = false,
    val deleteRepoTarget: ExtensionRepository? = null,
    val uninstallTarget: ExtensionInfo? = null,
    val activePreferencesSheet: Pair<ExtensionInfo, List<ExtensionPreference>>? = null,
    val activeLogsSheet: Pair<ExtensionInfo, List<ExtensionLogEntry>>? = null
)

class ExtensionsViewModel(
    private val extensionManager: ExtensionManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedLanguage = MutableStateFlow("ALL")
    private val _selectedType = MutableStateFlow<ExtensionType?>(null)
    private val _sortOption = MutableStateFlow(ExtensionSortOption.ORDER)

    private val _isSyncingRepos = MutableStateFlow(false)
    private val _installingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _updatingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _uninstallingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _userMessage = MutableStateFlow<String?>(null)

    private val _showAddRepoDialog = MutableStateFlow(false)
    private val _deleteRepoTarget = MutableStateFlow<ExtensionRepository?>(null)
    private val _uninstallTarget = MutableStateFlow<ExtensionInfo?>(null)
    private val _activePreferencesSheet = MutableStateFlow<Pair<ExtensionInfo, List<ExtensionPreference>>?>(null)
    private val _activeLogsSheet = MutableStateFlow<Pair<ExtensionInfo, List<ExtensionLogEntry>>?>(null)

    init {
        // Automatically sync repositories on launch if empty or out of date
        viewModelScope.launch {
            extensionManager.syncRepositories()
        }
    }

    val uiState: StateFlow<ExtensionsUiState> = combine(
        combine(
            _selectedTab,
            _searchQuery,
            _selectedLanguage,
            _selectedType,
            _sortOption
        ) { tab, search, lang, type, sort ->
            FilterParams(tab, search, lang, type, sort)
        },
        combine(
            extensionManager.allExtensions,
            extensionManager.availableRepositoryExtensions,
            extensionManager.repositories,
            settingsRepository.settings
        ) { allInstalled, availableRemote, repos, settings ->
            DataBundle(allInstalled, availableRemote, repos, settings.defaultExtensionId)
        },
        combine(
            _isSyncingRepos,
            _installingIds,
            _updatingIds,
            _uninstallingIds,
            _userMessage
        ) { isSyncing, installing, updating, uninstalling, msg ->
            ActionStates(isSyncing, installing, updating, uninstalling, msg)
        },
        combine(
            _showAddRepoDialog,
            _deleteRepoTarget,
            _uninstallTarget,
            _activePreferencesSheet,
            _activeLogsSheet
        ) { addRepo, delRepo, uninst, pref, logs ->
            DialogStates(addRepo, delRepo, uninst, pref, logs)
        }
    ) { filter, data, actions, dialogs ->
        val installedOnly = data.allInstalled.filter { it.manifest.isInstalled }
        val updatesOnly = data.allInstalled.filter { it.manifest.isInstalled && it.manifest.hasUpdate }

        // Aggregate unique languages across installed and remote
        val allLangs = (installedOnly.map { it.manifest.language } + data.availableRemote.map { it.manifest.language })
            .filter { it.isNotBlank() }
            .map { it.uppercase() }
            .distinct()
            .sorted()
        val languageOptions = listOf("ALL") + allLangs

        // Filter and sort collections
        val filteredInstalled = filterAndSort(
            list = installedOnly,
            query = filter.search,
            lang = filter.lang,
            type = filter.type,
            sort = filter.sort
        )

        val filteredAvailable = filterAndSort(
            list = data.availableRemote.filter { !it.manifest.isInstalled },
            query = filter.search,
            lang = filter.lang,
            type = filter.type,
            sort = filter.sort
        )

        val filteredUpdates = filterAndSort(
            list = updatesOnly,
            query = filter.search,
            lang = filter.lang,
            type = filter.type,
            sort = filter.sort
        )

        ExtensionsUiState(
            selectedTab = filter.tab,
            searchQuery = filter.search,
            selectedLanguage = filter.lang,
            selectedType = filter.type,
            sortOption = filter.sort,
            installedList = filteredInstalled,
            availableList = filteredAvailable,
            updateList = filteredUpdates,
            repositoriesList = data.repos,
            availableLanguages = languageOptions,
            preferredExtensionId = data.preferredId,
            isSyncingRepos = actions.isSyncing,
            installingIds = actions.installing,
            updatingIds = actions.updating,
            uninstallingIds = actions.uninstalling,
            userMessage = actions.userMessage,
            showAddRepoDialog = dialogs.showAddRepo,
            deleteRepoTarget = dialogs.deleteRepo,
            uninstallTarget = dialogs.uninstall,
            activePreferencesSheet = dialogs.preferences,
            activeLogsSheet = dialogs.logs
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ExtensionsUiState(isInitialLoading = true)
    )

    private fun filterAndSort(
        list: List<ExtensionInfo>,
        query: String,
        lang: String,
        type: ExtensionType?,
        sort: ExtensionSortOption
    ): List<ExtensionInfo> {
        val q = query.trim().lowercase()
        return list.filter { info ->
            val matchQuery = if (q.isBlank()) true else {
                info.manifest.name.lowercase().contains(q) ||
                    info.manifest.description.lowercase().contains(q) ||
                    info.manifest.author.lowercase().contains(q) ||
                    info.manifest.id.lowercase().contains(q)
            }
            val matchLang = if (lang == "ALL") true else {
                info.manifest.language.equals(lang, ignoreCase = true)
            }
            val matchType = if (type == null) true else {
                info.manifest.type == type
            }
            matchQuery && matchLang && matchType
        }.let { filtered ->
            when (sort) {
                ExtensionSortOption.ORDER -> filtered.sortedBy { it.manifest.order }
                ExtensionSortOption.NAME -> filtered.sortedBy { it.manifest.name.lowercase() }
                ExtensionSortOption.VERSION -> filtered.sortedByDescending { it.manifest.version }
                ExtensionSortOption.LANGUAGE -> filtered.sortedBy { it.manifest.language }
            }
        }
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLanguageFilter(language: String) {
        _selectedLanguage.value = language
    }

    fun setTypeFilter(type: ExtensionType?) {
        _selectedType.value = type
    }

    fun setSortOption(sort: ExtensionSortOption) {
        _sortOption.value = sort
    }

    fun toggleExtensionEnabled(extensionId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            when (val result = extensionManager.setExtensionEnabled(extensionId, isEnabled)) {
                is AppResult.Success -> {
                    _userMessage.value = if (isEnabled) "Extension enabled" else "Extension disabled"
                }
                is AppResult.Error -> {
                    _userMessage.value = "Failed: ${result.error.message}"
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun setPreferredExtension(extensionId: String) {
        viewModelScope.launch {
            when (val result = extensionManager.setPreferredExtension(extensionId)) {
                is AppResult.Success -> {
                    _userMessage.value = "Default source set successfully"
                }
                is AppResult.Error -> {
                    _userMessage.value = "Failed to set default: ${result.error.message}"
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun moveExtensionUp(extensionId: String) {
        viewModelScope.launch {
            val installed = extensionManager.allExtensions.value.filter { it.manifest.isInstalled }.sortedBy { it.manifest.order }
            val index = installed.indexOfFirst { it.manifest.id == extensionId }
            if (index > 0) {
                val current = installed[index]
                val previous = installed[index - 1]
                extensionManager.setExtensionOrder(current.manifest.id, previous.manifest.order)
                extensionManager.setExtensionOrder(previous.manifest.id, current.manifest.order)
            }
        }
    }

    fun moveExtensionDown(extensionId: String) {
        viewModelScope.launch {
            val installed = extensionManager.allExtensions.value.filter { it.manifest.isInstalled }.sortedBy { it.manifest.order }
            val index = installed.indexOfFirst { it.manifest.id == extensionId }
            if (index >= 0 && index < installed.size - 1) {
                val current = installed[index]
                val next = installed[index + 1]
                extensionManager.setExtensionOrder(current.manifest.id, next.manifest.order)
                extensionManager.setExtensionOrder(next.manifest.id, current.manifest.order)
            }
        }
    }

    fun installExtension(manifest: ExtensionManifest) {
        viewModelScope.launch {
            _installingIds.update { it + manifest.id }
            when (val result = extensionManager.installExtension(manifest)) {
                is AppResult.Success -> {
                    _userMessage.value = "Installed ${manifest.name}"
                }
                is AppResult.Error -> {
                    _userMessage.value = "Installation failed: ${result.error.message}"
                }
                is AppResult.Loading -> Unit
            }
            _installingIds.update { it - manifest.id }
        }
    }

    fun updateExtension(manifest: ExtensionManifest) {
        viewModelScope.launch {
            _updatingIds.update { it + manifest.id }
            when (val result = extensionManager.updateExtension(manifest)) {
                is AppResult.Success -> {
                    _userMessage.value = "Updated ${manifest.name} to v${manifest.version}"
                }
                is AppResult.Error -> {
                    _userMessage.value = "Update failed: ${result.error.message}"
                }
                is AppResult.Loading -> Unit
            }
            _updatingIds.update { it - manifest.id }
        }
    }

    fun updateAllExtensions() {
        viewModelScope.launch {
            val pending = extensionManager.allExtensions.value.filter { it.manifest.isInstalled && it.manifest.hasUpdate }
            if (pending.isEmpty()) {
                _userMessage.value = "All extensions are already up to date"
                return@launch
            }

            var successCount = 0
            pending.forEach { info ->
                _updatingIds.update { it + info.manifest.id }
                val result = extensionManager.updateExtension(info.manifest)
                if (result is AppResult.Success) {
                    successCount++
                }
                _updatingIds.update { it - info.manifest.id }
            }
            _userMessage.value = "Updated $successCount of ${pending.size} extensions"
        }
    }

    fun requestUninstall(info: ExtensionInfo) {
        _uninstallTarget.value = info
    }

    fun dismissUninstallDialog() {
        _uninstallTarget.value = null
    }

    fun confirmUninstall() {
        val target = _uninstallTarget.value ?: return
        _uninstallTarget.value = null
        viewModelScope.launch {
            _uninstallingIds.update { it + target.manifest.id }
            when (val result = extensionManager.uninstallExtension(target.manifest.id)) {
                is AppResult.Success -> {
                    _userMessage.value = "Uninstalled ${target.manifest.name}"
                }
                is AppResult.Error -> {
                    _userMessage.value = "Uninstall failed: ${result.error.message}"
                }
                is AppResult.Loading -> Unit
            }
            _uninstallingIds.update { it - target.manifest.id }
        }
    }

    fun openPreferences(info: ExtensionInfo) {
        viewModelScope.launch {
            val prefs = extensionManager.getExtensionPreferences(info.manifest.id)
            _activePreferencesSheet.value = Pair(info, prefs)
        }
    }

    fun dismissPreferencesSheet() {
        _activePreferencesSheet.value = null
    }

    fun setPreferenceValue(extensionId: String, key: String, value: Any) {
        viewModelScope.launch {
            extensionManager.setExtensionPreference(extensionId, key, value)
            // Refresh active preferences sheet
            val target = _activePreferencesSheet.value?.first
            if (target != null && target.manifest.id == extensionId) {
                val updatedPrefs = extensionManager.getExtensionPreferences(extensionId)
                _activePreferencesSheet.value = Pair(target, updatedPrefs)
            }
        }
    }

    fun openLogs(info: ExtensionInfo) {
        val logs = extensionManager.getExtensionLogs(info.manifest.id)
        _activeLogsSheet.value = Pair(info, logs)
    }

    fun dismissLogsSheet() {
        _activeLogsSheet.value = null
    }

    fun showAddRepoDialog() {
        _showAddRepoDialog.value = true
    }

    fun dismissAddRepoDialog() {
        _showAddRepoDialog.value = false
    }

    fun addRepository(name: String, url: String) {
        _showAddRepoDialog.value = false
        if (url.isBlank()) {
            _userMessage.value = "Repository URL cannot be empty"
            return
        }
        viewModelScope.launch {
            _isSyncingRepos.value = true
            when (val result = extensionManager.addRepository(name, url)) {
                is AppResult.Success -> {
                    _userMessage.value = "Added repository: ${result.data.name}"
                }
                is AppResult.Error -> {
                    _userMessage.value = "Failed to add repo: ${result.error.message}"
                }
                is AppResult.Loading -> Unit
            }
            _isSyncingRepos.value = false
        }
    }

    fun toggleRepository(repoId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            extensionManager.setRepositoryEnabled(repoId, isEnabled)
            syncRepositories()
        }
    }

    fun syncRepositories() {
        viewModelScope.launch {
            _isSyncingRepos.value = true
            when (val result = extensionManager.syncRepositories()) {
                is AppResult.Success -> {
                    _userMessage.value = "Repositories synced successfully"
                }
                is AppResult.Error -> {
                    _userMessage.value = "Sync error: ${result.error.message}"
                }
                is AppResult.Loading -> Unit
            }
            _isSyncingRepos.value = false
        }
    }

    fun requestDeleteRepo(repo: ExtensionRepository) {
        _deleteRepoTarget.value = repo
    }

    fun dismissDeleteRepoDialog() {
        _deleteRepoTarget.value = null
    }

    fun confirmDeleteRepo() {
        val target = _deleteRepoTarget.value ?: return
        _deleteRepoTarget.value = null
        viewModelScope.launch {
            when (val result = extensionManager.removeRepository(target.id)) {
                is AppResult.Success -> {
                    _userMessage.value = "Removed repository: ${target.name}"
                    extensionManager.syncRepositories()
                }
                is AppResult.Error -> {
                    _userMessage.value = "Failed to remove repo: ${result.error.message}"
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    companion object {
        fun provideFactory(
            extensionManager: ExtensionManager,
            settingsRepository: SettingsRepository
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExtensionsViewModel(
                    extensionManager = extensionManager,
                    settingsRepository = settingsRepository
                ) as T
            }
        }
    }

    private data class FilterParams(
        val tab: Int,
        val search: String,
        val lang: String,
        val type: ExtensionType?,
        val sort: ExtensionSortOption
    )

    private data class DataBundle(
        val allInstalled: List<ExtensionInfo>,
        val availableRemote: List<ExtensionInfo>,
        val repos: List<ExtensionRepository>,
        val preferredId: String
    )

    private data class ActionStates(
        val isSyncing: Boolean,
        val installing: Set<String>,
        val updating: Set<String>,
        val uninstalling: Set<String>,
        val userMessage: String?
    )

    private data class DialogStates(
        val showAddRepo: Boolean,
        val deleteRepo: ExtensionRepository?,
        val uninstall: ExtensionInfo?,
        val preferences: Pair<ExtensionInfo, List<ExtensionPreference>>?,
        val logs: Pair<ExtensionInfo, List<ExtensionLogEntry>>?
    )
}
