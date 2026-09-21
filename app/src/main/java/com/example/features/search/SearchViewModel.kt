package com.example.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.result.AppResult
import com.example.domain.extension.ExtensionManager
import com.example.domain.model.Anime
import com.example.domain.model.ExtensionInfo
import com.example.domain.usecase.AnimeSearchUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val results: List<Anime> = emptyList(),
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val searchHistory: List<String> = listOf("Frieren", "Solo Leveling", "Jujutsu Kaisen", "Demon Slayer"),
    val selectedSourceId: String? = null,
    val availableSources: List<ExtensionInfo> = emptyList(),
    val selectedGenreFilter: String? = null,
    val selectedStatusFilter: String? = null
)

class SearchViewModel(
    private val animeSearchUseCase: AnimeSearchUseCase,
    private val extensionManager: ExtensionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeSources()
    }

    private fun observeSources() {
        viewModelScope.launch {
            extensionManager.allExtensions.collect { extensions ->
                val installed = extensions.filter { it.manifest.isInstalled && it.manifest.isEnabled }
                _uiState.update { state ->
                    state.copy(
                        availableSources = installed,
                        selectedSourceId = state.selectedSourceId ?: installed.firstOrNull()?.manifest?.id
                    )
                }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false, error = null) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // Debounce typing
            performSearch(query = newQuery, page = 1, isNewSearch = true)
        }
    }

    fun onSearchSubmit(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        // Add to history
        addSearchHistory(trimmed)
        searchJob?.cancel()
        performSearch(query = trimmed, page = 1, isNewSearch = true)
    }

    fun selectSource(sourceId: String?) {
        _uiState.update { it.copy(selectedSourceId = sourceId) }
        if (_uiState.value.query.isNotBlank()) {
            performSearch(query = _uiState.value.query, page = 1, isNewSearch = true)
        }
    }

    fun selectGenreFilter(genre: String?) {
        val next = if (_uiState.value.selectedGenreFilter == genre) null else genre
        _uiState.update { it.copy(selectedGenreFilter = next) }
        if (_uiState.value.query.isNotBlank()) {
            performSearch(query = _uiState.value.query, page = 1, isNewSearch = true)
        }
    }

    fun selectStatusFilter(status: String?) {
        val next = if (_uiState.value.selectedStatusFilter == status) null else status
        _uiState.update { it.copy(selectedStatusFilter = next) }
        if (_uiState.value.query.isNotBlank()) {
            performSearch(query = _uiState.value.query, page = 1, isNewSearch = true)
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isSearching || state.isLoadingMore || !state.hasMorePages || state.query.isBlank()) return

        val nextPage = state.currentPage + 1
        performSearch(query = state.query, page = nextPage, isNewSearch = false)
    }

    private fun performSearch(query: String, page: Int, isNewSearch: Boolean) {
        viewModelScope.launch {
            if (isNewSearch) {
                _uiState.update { it.copy(isSearching = true, error = null, currentPage = 1) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val filters = mutableMapOf<String, Any>()
            _uiState.value.selectedGenreFilter?.let { filters["genre"] = it }
            _uiState.value.selectedStatusFilter?.let { filters["status"] = it }

            val result = animeSearchUseCase(
                query = query,
                extensionId = _uiState.value.selectedSourceId,
                page = page,
                filters = filters
            )

            when (result) {
                is AppResult.Success -> {
                    val newItems = result.data
                    _uiState.update { state ->
                        val combined = if (isNewSearch) newItems else (state.results + newItems).distinctBy { it.localId }
                        state.copy(
                            isSearching = false,
                            isLoadingMore = false,
                            error = null,
                            results = combined,
                            currentPage = page,
                            hasMorePages = newItems.isNotEmpty()
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isSearching = false,
                            isLoadingMore = false,
                            error = if (isNewSearch) result.error.message else null
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _uiState.update { state ->
            val updated = (listOf(trimmed) + state.searchHistory.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(15)
            state.copy(searchHistory = updated)
        }
    }

    fun removeSearchHistory(query: String) {
        _uiState.update { state ->
            state.copy(searchHistory = state.searchHistory.filterNot { it == query })
        }
    }

    fun clearSearchHistory() {
        _uiState.update { it.copy(searchHistory = emptyList()) }
    }

    fun retry() {
        if (_uiState.value.query.isNotBlank()) {
            performSearch(query = _uiState.value.query, page = _uiState.value.currentPage, isNewSearch = true)
        }
    }

    companion object {
        fun provideFactory(
            animeSearchUseCase: AnimeSearchUseCase,
            extensionManager: ExtensionManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(
                    animeSearchUseCase = animeSearchUseCase,
                    extensionManager = extensionManager
                ) as T
            }
        }
    }
}
