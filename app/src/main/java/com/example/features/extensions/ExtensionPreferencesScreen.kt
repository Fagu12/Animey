package com.example.features.extensions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.AnimeyApplication
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.PreferenceType

/**
 * Universal, metadata-driven Extension Preferences Screen.
 * Automatically generates configuration controls for extensions based on declared types:
 * - BOOLEAN
 * - STRING
 * - INTEGER
 * - NUMBER
 * - SELECT
 * - MULTI_SELECT
 *
 * All mutations are transmitted strictly through ExtensionRuntime back to the extension.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionPreferencesScreen(
    extensionId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = (context.applicationContext as AnimeyApplication).container

    val viewModel: ExtensionPreferencesViewModel = viewModel(
        factory = ExtensionPreferencesViewModel.provideFactory(
            extensionId = extensionId,
            extensionManager = container.extensionManager,
            runtimeManager = container.extensionRuntimeManager
        ),
        key = "ext_pref_$extensionId"
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showResetDialog by remember { mutableStateOf(false) }
    var activePreferenceDialog by remember { mutableStateOf<ExtensionPreference?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExtensionPreferencesEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("extension_preferences_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.extensionInfo?.manifest?.name ?: "Extension Settings",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ext_pref_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    if (uiState.preferences.isNotEmpty()) {
                        IconButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.testTag("ext_pref_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset all preferences to default"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("ext_pref_loading"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Failed to load preferences",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FilledTonalButton(
                            onClick = { viewModel.loadPreferences() },
                            modifier = Modifier.testTag("ext_pref_retry_button")
                        ) {
                            Text("Retry")
                        }
                    }
                }
                uiState.preferences.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("ext_pref_empty_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Preferences Available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This extension does not provide any configurable options.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val filteredPreferences = remember(uiState.preferences, uiState.searchQuery) {
                        if (uiState.searchQuery.isBlank()) {
                            uiState.preferences
                        } else {
                            val q = uiState.searchQuery.trim().lowercase()
                            uiState.preferences.filter {
                                it.title.lowercase().contains(q) ||
                                    it.summary.lowercase().contains(q) ||
                                    it.key.lowercase().contains(q)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Extension summary card
                        item(key = "ext_info_card") {
                            uiState.extensionInfo?.manifest?.let { manifest ->
                                ExtensionInfoCard(
                                    name = manifest.name,
                                    version = manifest.version,
                                    author = manifest.author,
                                    language = manifest.language,
                                    description = manifest.description
                                )
                            }
                        }

                        // Search box if more than 3 preferences
                        if (uiState.preferences.size > 3) {
                            item(key = "search_field") {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ext_pref_search_field"),
                                    placeholder = { Text("Search preferences...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search icon"
                                        )
                                    },
                                    trailingIcon = {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Clear search"
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // Automatically generated preference rows
                        items(
                            items = filteredPreferences,
                            key = { it.key }
                        ) { pref ->
                            DynamicPreferenceItem(
                                preference = pref,
                                onValueChange = { newValue ->
                                    viewModel.updatePreference(pref.key, newValue)
                                },
                                onRequestEdit = {
                                    activePreferenceDialog = pref
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Preferences") },
            text = { Text("Are you sure you want to reset all preferences for this extension to their factory defaults?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetToDefaults()
                    },
                    modifier = Modifier.testTag("confirm_reset_preferences_button")
                ) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Preference Edit Dialog for non-boolean types
    activePreferenceDialog?.let { pref ->
        when (pref.preferenceType) {
            PreferenceType.STRING -> {
                EditStringDialog(
                    preference = pref,
                    onDismiss = { activePreferenceDialog = null },
                    onConfirm = { newValue ->
                        viewModel.updatePreference(pref.key, newValue)
                        activePreferenceDialog = null
                    }
                )
            }
            PreferenceType.INTEGER -> {
                EditIntegerDialog(
                    preference = pref,
                    onDismiss = { activePreferenceDialog = null },
                    onConfirm = { newValue ->
                        viewModel.updatePreference(pref.key, newValue)
                        activePreferenceDialog = null
                    }
                )
            }
            PreferenceType.NUMBER -> {
                EditNumberDialog(
                    preference = pref,
                    onDismiss = { activePreferenceDialog = null },
                    onConfirm = { newValue ->
                        viewModel.updatePreference(pref.key, newValue)
                        activePreferenceDialog = null
                    }
                )
            }
            PreferenceType.SELECT -> {
                EditSelectDialog(
                    preference = pref,
                    onDismiss = { activePreferenceDialog = null },
                    onConfirm = { newValue ->
                        viewModel.updatePreference(pref.key, newValue)
                        activePreferenceDialog = null
                    }
                )
            }
            PreferenceType.MULTI_SELECT -> {
                EditMultiSelectDialog(
                    preference = pref,
                    onDismiss = { activePreferenceDialog = null },
                    onConfirm = { newValue ->
                        viewModel.updatePreference(pref.key, newValue)
                        activePreferenceDialog = null
                    }
                )
            }
            PreferenceType.BOOLEAN -> {
                // Handled inline via Switch toggle
                activePreferenceDialog = null
            }
        }
    }
}

@Composable
private fun ExtensionInfoCard(
    name: String,
    version: String,
    author: String,
    language: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ext_pref_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v$version • by $author • $language",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicPreferenceItem(
    preference: ExtensionPreference,
    onValueChange: (Any) -> Unit,
    onRequestEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ext_pref_row_${preference.key}")
            .clickable(
                onClick = {
                    if (preference.preferenceType == PreferenceType.BOOLEAN) {
                        onValueChange(!preference.asBoolean())
                    } else {
                        onRequestEdit()
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preference.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (preference.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preference.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Subtitle/Value preview for non-boolean types
                when (preference.preferenceType) {
                    PreferenceType.BOOLEAN -> Unit
                    PreferenceType.STRING -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = preference.asString().ifEmpty { "None" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    PreferenceType.INTEGER -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = preference.asInt().toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    PreferenceType.NUMBER -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = preference.asDouble().toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    PreferenceType.SELECT -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = preference.asString().ifEmpty { "Default" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    PreferenceType.MULTI_SELECT -> {
                        val selectedList = preference.asMultiSelect()
                        Spacer(modifier = Modifier.height(6.dp))
                        if (selectedList.isEmpty()) {
                            Text(
                                text = "None selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                selectedList.take(4).forEach { item ->
                                    SuggestionChip(
                                        onClick = onRequestEdit,
                                        label = { Text(item, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                if (selectedList.size > 4) {
                                    SuggestionChip(
                                        onClick = onRequestEdit,
                                        label = { Text("+${selectedList.size - 4}", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Trailing action / toggle control
            if (preference.preferenceType == PreferenceType.BOOLEAN) {
                Switch(
                    checked = preference.asBoolean(),
                    onCheckedChange = { onValueChange(it) },
                    modifier = Modifier.testTag("ext_pref_switch_${preference.key}")
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Dialogs for Dynamic Types: STRING, INTEGER, NUMBER, SELECT, MULTI_SELECT
// -------------------------------------------------------------------------------------------------

@Composable
private fun EditStringDialog(
    preference: ExtensionPreference,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(preference.asString()) }

    AlertDialog(
        modifier = Modifier.testTag("ext_pref_dialog_${preference.key}"),
        onDismissRequest = onDismiss,
        title = { Text(preference.title) },
        text = {
            Column {
                if (preference.summary.isNotBlank()) {
                    Text(
                        text = preference.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ext_pref_text_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(textValue) },
                modifier = Modifier.testTag("ext_pref_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("ext_pref_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditIntegerDialog(
    preference: ExtensionPreference,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(preference.asInt().toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.testTag("ext_pref_dialog_${preference.key}"),
        onDismissRequest = onDismiss,
        title = { Text(preference.title) },
        text = {
            Column {
                if (preference.summary.isNotBlank()) {
                    Text(
                        text = preference.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            val current = textValue.toIntOrNull() ?: preference.asInt()
                            textValue = (current - 1).toString()
                            isError = false
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrement")
                    }

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            isError = it.toIntOrNull() == null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ext_pref_integer_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = isError,
                        shape = RoundedCornerShape(8.dp)
                    )

                    IconButton(
                        onClick = {
                            val current = textValue.toIntOrNull() ?: preference.asInt()
                            textValue = (current + 1).toString()
                            isError = false
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increment")
                    }
                }

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please enter a valid integer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = textValue.toIntOrNull()
                    if (parsed != null) {
                        onConfirm(parsed)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("ext_pref_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("ext_pref_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditNumberDialog(
    preference: ExtensionPreference,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var textValue by remember { mutableStateOf(preference.asDouble().toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.testTag("ext_pref_dialog_${preference.key}"),
        onDismissRequest = onDismiss,
        title = { Text(preference.title) },
        text = {
            Column {
                if (preference.summary.isNotBlank()) {
                    Text(
                        text = preference.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        isError = it.toDoubleOrNull() == null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ext_pref_number_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = isError,
                    shape = RoundedCornerShape(8.dp)
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please enter a valid decimal number",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = textValue.toDoubleOrNull()
                    if (parsed != null) {
                        onConfirm(parsed)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("ext_pref_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("ext_pref_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditSelectDialog(
    preference: ExtensionPreference,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedOption by remember { mutableStateOf(preference.asString()) }

    AlertDialog(
        modifier = Modifier.testTag("ext_pref_dialog_${preference.key}"),
        onDismissRequest = onDismiss,
        title = { Text(preference.title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                if (preference.summary.isNotBlank()) {
                    item {
                        Text(
                            text = preference.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                items(preference.options) { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedOption = option }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("ext_pref_option_$option"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedOption = option }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedOption) },
                modifier = Modifier.testTag("ext_pref_save_button")
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("ext_pref_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditMultiSelectDialog(
    preference: ExtensionPreference,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val initialSelection = remember { preference.asMultiSelect().toSet() }
    var selectedSet by remember { mutableStateOf(initialSelection) }

    AlertDialog(
        modifier = Modifier.testTag("ext_pref_dialog_${preference.key}"),
        onDismissRequest = onDismiss,
        title = { Text(preference.title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                if (preference.summary.isNotBlank()) {
                    item {
                        Text(
                            text = preference.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                items(preference.options) { option ->
                    val isChecked = selectedSet.contains(option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedSet = if (isChecked) {
                                    selectedSet - option
                                } else {
                                    selectedSet + option
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("ext_pref_checkbox_$option"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedSet = if (checked) {
                                    selectedSet + option
                                } else {
                                    selectedSet - option
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedSet.toList()) },
                modifier = Modifier.testTag("ext_pref_save_button")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("ext_pref_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
