package com.example.features.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.extension.mangayomi.diagnostics.LiveTestResult
import com.example.data.extension.mangayomi.diagnostics.MangayomiRuntimeDiagnostics
import com.example.data.extension.mangayomi.diagnostics.RuntimeDiagnosticsResult
import com.example.data.local.database.dao.AnimeDao
import com.example.data.local.database.dao.EpisodeDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

data class DiagnosticsUiState(
    val isLoading: Boolean = false,
    val selfTestResult: RuntimeDiagnosticsResult? = null,
    val isLiveTesting: Boolean = false,
    val liveTestResult: LiveTestResult? = null
)

class MangayomiDiagnosticsViewModel(
    private val animeDao: AnimeDao? = null,
    private val episodeDao: EpisodeDao? = null,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        runSelfTest()
        runLiveExtensionTest()
    }

    fun runSelfTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = MangayomiRuntimeDiagnostics.runQuickJsSelfTest()
            _uiState.update { it.copy(isLoading = false, selfTestResult = res) }
        }
    }

    fun runLiveExtensionTest() {
        viewModelScope.launch {
            // Clear previous results immediately at start of test run
            _uiState.update { it.copy(isLiveTesting = true, liveTestResult = null) }
            val liveRes = MangayomiRuntimeDiagnostics.runLiveExtensionTest(
                okHttpClient = okHttpClient,
                animeDao = animeDao,
                episodeDao = episodeDao
            )
            _uiState.update { it.copy(isLiveTesting = false, liveTestResult = liveRes) }
        }
    }
}
