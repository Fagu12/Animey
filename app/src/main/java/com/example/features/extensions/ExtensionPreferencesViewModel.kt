package com.example.features.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.logging.AppLogger
import com.example.core.result.AppResult
import com.example.domain.extension.ExtensionManager
import com.example.domain.extension.manager.ExtensionRuntimeManager
import com.example.domain.model.ExtensionInfo
import com.example.domain.model.ExtensionPreference
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExtensionPreferencesUiState(
    val extensionInfo: ExtensionInfo? = null,
    val preferences: List<ExtensionPreference> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = ""
)

sealed interface ExtensionPreferencesEvent {
    data class ShowMessage(val message: String) : ExtensionPreferencesEvent
}

class ExtensionPreferencesViewModel(
    val extensionId: String,
    private val extensionManager: ExtensionManager,
    private val runtimeManager: ExtensionRuntimeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtensionPreferencesUiState())
    val uiState: StateFlow<ExtensionPreferencesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExtensionPreferencesEvent>()
    val events: SharedFlow<ExtensionPreferencesEvent> = _events.asSharedFlow()

    init {
        loadPreferences()
    }

    fun loadPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val info = extensionManager.allExtensions.value.find { it.manifest.id == extensionId }
                val prefs = extensionManager.getExtensionPreferences(extensionId)
                _uiState.update {
                    it.copy(
                        extensionInfo = info,
                        preferences = prefs,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("ExtPrefVM", "Error loading preferences for $extensionId", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load preferences"
                    )
                }
            }
        }
    }

    fun updatePreference(key: String, value: Any) {
        viewModelScope.launch {
            // Route through ExtensionRuntime
            val result = runtimeManager.setExtensionPreference(extensionId, key, value)
            if (result is AppResult.Success) {
                val updatedPrefs = _uiState.value.preferences.map { pref ->
                    if (pref.key == key) pref.copyWithValue(value) else pref
                }
                _uiState.update { it.copy(preferences = updatedPrefs) }
                _events.emit(ExtensionPreferencesEvent.ShowMessage("Preference updated"))
            } else if (result is AppResult.Error) {
                _events.emit(ExtensionPreferencesEvent.ShowMessage(result.error.message))
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val current = _uiState.value.preferences
            for (pref in current) {
                val def = pref.preferenceDefault ?: continue
                runtimeManager.setExtensionPreference(extensionId, pref.key, def)
            }
            val refreshed = extensionManager.getExtensionPreferences(extensionId)
            _uiState.update { it.copy(preferences = refreshed) }
            _events.emit(ExtensionPreferencesEvent.ShowMessage("Preferences reset to defaults"))
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    companion object {
        fun provideFactory(
            extensionId: String,
            extensionManager: ExtensionManager,
            runtimeManager: ExtensionRuntimeManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExtensionPreferencesViewModel(
                    extensionId = extensionId,
                    extensionManager = extensionManager,
                    runtimeManager = runtimeManager
                ) as T
            }
        }
    }
}
