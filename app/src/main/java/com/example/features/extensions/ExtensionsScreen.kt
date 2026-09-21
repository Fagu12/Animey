package com.example.features.extensions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.ExtensionOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.AnimeyApplication
import com.example.domain.model.ExtensionType
import com.example.features.extensions.components.AddRepositoryDialog
import com.example.features.extensions.components.AvailableExtensionCard
import com.example.features.extensions.components.ConfirmDeleteRepoDialog
import com.example.features.extensions.components.ConfirmUninstallDialog
import com.example.features.extensions.components.ExtensionLogsBottomSheet
import com.example.features.extensions.components.ExtensionPreferencesBottomSheet
import com.example.features.extensions.components.InstalledExtensionCard
import com.example.features.extensions.components.RepositoryCard
import com.example.features.extensions.components.UpdateExtensionCard
import com.example.ui.components.EmptyState
import com.example.ui.components.AnimeyTopBar

@Composable
fun ExtensionsScreen(
    onNavigateToHistory: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPreferences: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val container = (context.applicationContext as AnimeyApplication).container
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: ExtensionsViewModel = viewModel(
        factory = ExtensionsViewModel.provideFactory(
            extensionManager = container.extensionManager,
            settingsRepository = container.settingsRepository
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    // Handle user snackbars
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val tabs = listOf(
        "Installed (${uiState.installedList.size})",
        "Available (${uiState.availableList.size})",
        "Updates",
        "Repositories (${uiState.repositoriesList.size})"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimeyTopBar(
                title = "Extensions",
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("extensions_screen_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search & Filter Header
            ExtensionsHeader(
                searchQuery = uiState.searchQuery,
                onSearchChange = viewModel::setSearchQuery,
                selectedLanguage = uiState.selectedLanguage,
                availableLanguages = uiState.availableLanguages,
                onLanguageChange = viewModel::setLanguageFilter,
                selectedType = uiState.selectedType,
                onTypeChange = viewModel::setTypeFilter,
                selectedSort = uiState.sortOption,
                onSortChange = viewModel::setSortOption,
                onClearSearch = {
                    viewModel.setSearchQuery("")
                    keyboardController?.hide()
                }
            )

            // Tabs Row
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("extensions_tab_row")
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.setTab(index) },
                        text = {
                            if (index == 2 && uiState.updateList.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ) {
                                            Text(
                                                text = "${uiState.updateList.size}",
                                                modifier = Modifier.testTag("extensions_update_badge")
                                            )
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Updates",
                                        fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = title,
                                    fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        modifier = Modifier.testTag("extensions_tab_$index")
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (uiState.selectedTab) {
                    0 -> InstalledTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToPreferences = onNavigateToPreferences
                    )
                    1 -> AvailableTabContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    2 -> UpdatesTabContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    3 -> RepositoriesTabContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Dialogs & Sheets
    if (uiState.showAddRepoDialog) {
        AddRepositoryDialog(
            onDismiss = viewModel::dismissAddRepoDialog,
            onConfirm = viewModel::addRepository
        )
    }

    uiState.uninstallTarget?.let { target ->
        ConfirmUninstallDialog(
            extensionInfo = target,
            onDismiss = viewModel::dismissUninstallDialog,
            onConfirm = viewModel::confirmUninstall
        )
    }

    uiState.deleteRepoTarget?.let { target ->
        ConfirmDeleteRepoDialog(
            repository = target,
            onDismiss = viewModel::dismissDeleteRepoDialog,
            onConfirm = viewModel::confirmDeleteRepo
        )
    }

    uiState.activePreferencesSheet?.let { (info, prefs) ->
        ExtensionPreferencesBottomSheet(
            extensionInfo = info,
            preferences = prefs,
            onPreferenceChange = { key, value ->
                viewModel.setPreferenceValue(info.manifest.id, key, value)
            },
            onDismiss = viewModel::dismissPreferencesSheet
        )
    }

    uiState.activeLogsSheet?.let { (info, logs) ->
        ExtensionLogsBottomSheet(
            extensionInfo = info,
            logs = logs,
            onDismiss = viewModel::dismissLogsSheet
        )
    }
}

@Composable
private fun ExtensionsHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedLanguage: String,
    availableLanguages: List<String>,
    onLanguageChange: (String) -> Unit,
    selectedType: ExtensionType?,
    onTypeChange: (ExtensionType?) -> Unit,
    selectedSort: ExtensionSortOption,
    onSortChange: (ExtensionSortOption) -> Unit,
    onClearSearch: () -> Unit
) {
    var langMenuExpanded by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search extensions, providers, authors...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = onClearSearch,
                        modifier = Modifier.testTag("extensions_clear_search")
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("extensions_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter & Sorting Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language Filter
            Box {
                FilterChip(
                    selected = selectedLanguage != "ALL",
                    onClick = { langMenuExpanded = true },
                    label = { Text("Lang: $selectedLanguage") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_chip_language")
                )

                DropdownMenu(
                    expanded = langMenuExpanded,
                    onDismissRequest = { langMenuExpanded = false }
                ) {
                    availableLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(if (lang == "ALL") "All Languages" else lang) },
                            onClick = {
                                onLanguageChange(lang)
                                langMenuExpanded = false
                            },
                            modifier = Modifier.testTag("filter_lang_option_$lang")
                        )
                    }
                }
            }

            // Type Filter
            Box {
                FilterChip(
                    selected = selectedType != null,
                    onClick = { typeMenuExpanded = true },
                    label = { Text("Type: ${selectedType?.name ?: "All"}") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("filter_chip_type")
                )

                DropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Types") },
                        onClick = {
                            onTypeChange(null)
                            typeMenuExpanded = false
                        },
                        modifier = Modifier.testTag("filter_type_option_all")
                    )
                    ExtensionType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                onTypeChange(type)
                                typeMenuExpanded = false
                            },
                            modifier = Modifier.testTag("filter_type_option_${type.name}")
                        )
                    }
                }
            }

            // Sorting Mode
            Box {
                FilterChip(
                    selected = selectedSort != ExtensionSortOption.ORDER,
                    onClick = { sortMenuExpanded = true },
                    label = { Text("Sort: ${selectedSort.displayName}") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.testTag("filter_chip_sort")
                )

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    ExtensionSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                onSortChange(option)
                                sortMenuExpanded = false
                            },
                            modifier = Modifier.testTag("sort_option_${option.name}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledTabContent(
    uiState: ExtensionsUiState,
    viewModel: ExtensionsViewModel,
    onNavigateToPreferences: (String) -> Unit = {}
) {
    if (uiState.installedList.isEmpty()) {
        EmptyState(
            title = "No Installed Extensions",
            description = if (uiState.searchQuery.isNotBlank() || uiState.selectedLanguage != "ALL" || uiState.selectedType != null) {
                "No installed extensions match your search query or filters."
            } else {
                "Browse the Available tab to install anime providers and media extensions."
            },
            icon = Icons.Outlined.ExtensionOff
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = uiState.installedList,
                key = { _, item -> item.manifest.id }
            ) { index, extensionInfo ->
                val isPreferred = extensionInfo.manifest.id == uiState.preferredExtensionId
                val canMoveUp = index > 0
                val canMoveDown = index < uiState.installedList.size - 1

                InstalledExtensionCard(
                    extensionInfo = extensionInfo,
                    isPreferred = isPreferred,
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    isUpdating = uiState.updatingIds.contains(extensionInfo.manifest.id),
                    isUninstalling = uiState.uninstallingIds.contains(extensionInfo.manifest.id),
                    onToggleEnabled = { isEnabled ->
                        viewModel.toggleExtensionEnabled(extensionInfo.manifest.id, isEnabled)
                    },
                    onSetPreferred = {
                        viewModel.setPreferredExtension(extensionInfo.manifest.id)
                    },
                    onMoveUp = {
                        viewModel.moveExtensionUp(extensionInfo.manifest.id)
                    },
                    onMoveDown = {
                        viewModel.moveExtensionDown(extensionInfo.manifest.id)
                    },
                    onOpenSettings = {
                        onNavigateToPreferences(extensionInfo.manifest.id)
                        viewModel.openPreferences(extensionInfo)
                    },
                    onOpenLogs = {
                        viewModel.openLogs(extensionInfo)
                    },
                    onUpdate = {
                        viewModel.updateExtension(extensionInfo.manifest)
                    },
                    onUninstall = {
                        viewModel.requestUninstall(extensionInfo)
                    }
                )
            }
        }
    }
}

@Composable
private fun AvailableTabContent(
    uiState: ExtensionsUiState,
    viewModel: ExtensionsViewModel
) {
    if (uiState.availableList.isEmpty()) {
        EmptyState(
            title = "No Available Extensions",
            description = if (uiState.repositoriesList.isEmpty()) {
                "No repositories configured. Add a repository in the Repositories tab to discover extensions."
            } else {
                "All extensions from active repositories are already installed or filtered out."
            },
            icon = Icons.Default.Extension
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = uiState.availableList,
                key = { _, item -> item.manifest.id }
            ) { _, extensionInfo ->
                AvailableExtensionCard(
                    extensionInfo = extensionInfo,
                    isInstalling = uiState.installingIds.contains(extensionInfo.manifest.id),
                    onInstall = {
                        viewModel.installExtension(extensionInfo.manifest)
                    }
                )
            }
        }
    }
}

@Composable
private fun UpdatesTabContent(
    uiState: ExtensionsUiState,
    viewModel: ExtensionsViewModel
) {
    if (uiState.updateList.isEmpty()) {
        EmptyState(
            title = "All Extensions Up to Date",
            description = "All installed anime providers and video extension modules are running the latest releases.",
            icon = Icons.Default.CheckCircle
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.updateList.size} Update${if (uiState.updateList.size > 1) "s" else ""} Available",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = viewModel::updateAllExtensions,
                        enabled = uiState.updatingIds.isEmpty(),
                        modifier = Modifier.testTag("extensions_update_all_button")
                    ) {
                        if (uiState.updatingIds.isNotEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update All")
                        }
                    }
                }
            }

            itemsIndexed(
                items = uiState.updateList,
                key = { _, item -> item.manifest.id }
            ) { _, extensionInfo ->
                UpdateExtensionCard(
                    extensionInfo = extensionInfo,
                    isUpdating = uiState.updatingIds.contains(extensionInfo.manifest.id),
                    onUpdate = {
                        viewModel.updateExtension(extensionInfo.manifest)
                    }
                )
            }
        }
    }
}

@Composable
private fun RepositoriesTabContent(
    uiState: ExtensionsUiState,
    viewModel: ExtensionsViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extension Repositories",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = viewModel::syncRepositories,
                        enabled = !uiState.isSyncingRepos,
                        modifier = Modifier.testTag("extensions_sync_all_button")
                    ) {
                        if (uiState.isSyncingRepos) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync All Repositories",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Button(
                        onClick = viewModel::showAddRepoDialog,
                        modifier = Modifier.testTag("extensions_add_repo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Repo")
                    }
                }
            }
        }

        if (uiState.repositoriesList.isEmpty()) {
            item {
                EmptyState(
                    title = "No Repositories Configured",
                    description = "Add an extension repository to fetch and discover community anime providers.",
                    icon = Icons.Default.Storage
                )
            }
        } else {
            itemsIndexed(
                items = uiState.repositoriesList,
                key = { _, repo -> repo.id }
            ) { _, repo ->
                RepositoryCard(
                    repository = repo,
                    onToggleEnabled = { isEnabled ->
                        viewModel.toggleRepository(repo.id, isEnabled)
                    },
                    onRefresh = viewModel::syncRepositories,
                    onDelete = {
                        viewModel.requestDeleteRepo(repo)
                    }
                )
            }
        }
    }
}
