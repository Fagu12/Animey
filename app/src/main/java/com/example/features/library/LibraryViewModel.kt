package com.example.features.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.result.AppResult
import com.example.domain.model.LibraryEntry
import com.example.domain.model.LibraryStatus
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val selectedStatus: LibraryStatus = LibraryStatus.WATCHING,
    val searchQuery: String = "",
    val isGridView: Boolean = true,
    val allEntries: List<LibraryEntry> = emptyList(),
    val filteredEntries: List<LibraryEntry> = emptyList(),
    val categoryCounts: Map<LibraryStatus, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val statusMessage: String? = null
)

class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val episodeRepository: EpisodeRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow(LibraryStatus.WATCHING)

    init {
        observeLibrary()
    }

    private fun observeLibrary() {
        viewModelScope.launch {
            combine(
                libraryRepository.getAllLibraryEntries(),
                _selectedStatus,
                _searchQuery
            ) { allEntries, status, query ->
                val counts = LibraryStatus.entries.associateWith { cat ->
                    allEntries.count { it.status == cat }
                }

                val currentCategoryEntries = allEntries.filter { it.status == status }
                val filtered = if (query.isBlank()) {
                    currentCategoryEntries
                } else {
                    currentCategoryEntries.filter {
                        it.anime.title.contains(query, ignoreCase = true) ||
                                it.anime.altTitles.any { alt -> alt.contains(query, ignoreCase = true) } ||
                                it.anime.genres.any { g -> g.contains(query, ignoreCase = true) }
                    }
                }

                LibraryUiState(
                    selectedStatus = status,
                    searchQuery = query,
                    isGridView = _uiState.value.isGridView,
                    allEntries = allEntries,
                    filteredEntries = filtered,
                    categoryCounts = counts,
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

    fun selectStatus(status: LibraryStatus) {
        _selectedStatus.value = status
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun updateStatus(animeId: String, newStatus: LibraryStatus) {
        viewModelScope.launch {
            when (val result = libraryRepository.updateStatus(animeId, newStatus)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "Moved to ${newStatus.displayName}") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(statusMessage = "Failed to update: ${result.error.message}") }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun removeFromLibrary(animeId: String) {
        viewModelScope.launch {
            when (val result = libraryRepository.removeFromLibrary(animeId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "Removed from Library") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(statusMessage = "Failed to remove: ${result.error.message}") }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    companion object {
        fun provideFactory(
            libraryRepository: LibraryRepository,
            episodeRepository: EpisodeRepository,
            historyRepository: HistoryRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(
                    libraryRepository = libraryRepository,
                    episodeRepository = episodeRepository,
                    historyRepository = historyRepository
                ) as T
            }
        }
    }
}
