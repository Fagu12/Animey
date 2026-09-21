package com.example.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.result.AppResult
import com.example.domain.model.HistoryEntry
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val historyItems: List<HistoryEntry> = emptyList(),
    val filteredItems: List<HistoryEntry> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val statusMessage: String? = null
)

class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val episodeRepository: EpisodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            combine(
                historyRepository.getAllHistory(),
                _searchQuery
            ) { allHistory, query ->
                val filtered = if (query.isBlank()) {
                    allHistory
                } else {
                    allHistory.filter { entry ->
                        entry.anime.title.contains(query, ignoreCase = true) ||
                                entry.episode.title.contains(query, ignoreCase = true) ||
                                entry.sourceId.contains(query, ignoreCase = true) ||
                                "Ep ${entry.episode.number}".contains(query, ignoreCase = true)
                    }
                }

                HistoryUiState(
                    historyItems = allHistory,
                    filteredItems = filtered,
                    searchQuery = query,
                    isLoading = false,
                    statusMessage = _uiState.value.statusMessage
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, statusMessage = e.message) }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            when (val result = historyRepository.clearAllHistory()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "Watch history cleared") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(statusMessage = "Failed to clear history: ${result.error.message}") }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun removeHistoryItem(episodeId: String) {
        viewModelScope.launch {
            when (val result = historyRepository.removeHistoryForEpisode(episodeId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "Item removed from history") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(statusMessage = "Failed to remove item: ${result.error.message}") }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun markWatched(animeId: String, episodeId: String) {
        viewModelScope.launch {
            when (val result = episodeRepository.markWatched(episodeId, true)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "Marked as watched") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(statusMessage = "Failed to update: ${result.error.message}") }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun markUnwatched(animeId: String, episodeId: String) {
        viewModelScope.launch {
            // Update episode watched status to false and reset progress to 0
            episodeRepository.updateProgress(episodeId, positionMs = 0L, durationMs = 0L, isWatched = false)
            when (val result = episodeRepository.markWatched(episodeId, false)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "Marked as unwatched") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(statusMessage = "Failed to update: ${result.error.message}") }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun startOver(animeId: String, episodeId: String, sourceId: String, durationMs: Long) {
        viewModelScope.launch {
            episodeRepository.updateProgress(episodeId, positionMs = 0L, durationMs = durationMs, isWatched = false)
            historyRepository.recordHistory(animeId, episodeId, sourceId, positionMs = 0L, durationMs = durationMs)
            _uiState.update { it.copy(statusMessage = "Progress reset to beginning") }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    companion object {
        fun provideFactory(
            historyRepository: HistoryRepository,
            episodeRepository: EpisodeRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(
                    historyRepository = historyRepository,
                    episodeRepository = episodeRepository
                ) as T
            }
        }
    }
}
