package com.example.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.result.AppResult
import com.example.domain.model.Anime
import com.example.domain.model.HistoryEntry
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.usecase.GetLatestAnimeUseCase
import com.example.domain.usecase.GetPopularAnimeUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val trending: List<Anime> = emptyList(),
    val popular: List<Anime> = emptyList(),
    val latest: List<Anime> = emptyList(),
    val recentlyUpdated: List<Anime> = emptyList(),
    val continueWatching: List<HistoryEntry> = emptyList(),
    val selectedCategory: String = "All"
)

class HomeViewModel(
    private val getPopularAnimeUseCase: GetPopularAnimeUseCase,
    private val getLatestAnimeUseCase: GetLatestAnimeUseCase,
    private val historyRepository: HistoryRepository,
    private val episodeRepository: EpisodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
        loadHomeData(isRefresh = false)
    }

    private fun observeHistory() {
        viewModelScope.launch {
            historyRepository.getAllHistory()
                .catch { emit(emptyList()) }
                .collect { historyList ->
                    _uiState.update { it.copy(continueWatching = historyList.take(10)) }
                }
        }
    }

    fun loadHomeData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            try {
                val popularDeferred = async { getPopularAnimeUseCase(page = 1) }
                val latestDeferred = async { getLatestAnimeUseCase(page = 1) }

                val popularResult = popularDeferred.await()
                val latestResult = latestDeferred.await()

                val popularList = when (popularResult) {
                    is AppResult.Success -> popularResult.data
                    is AppResult.Error -> emptyList()
                    is AppResult.Loading -> emptyList()
                }

                val latestList = when (latestResult) {
                    is AppResult.Success -> latestResult.data
                    is AppResult.Error -> emptyList()
                    is AppResult.Loading -> emptyList()
                }

                // Trending is prioritized by top rating / popularity from the catalog
                val trendingList = (popularList + latestList)
                    .distinctBy { it.localId }
                    .sortedByDescending { it.rating ?: 0.0 }
                    .take(10)

                val recentlyUpdatedList = latestList.take(10)

                val hasError = popularList.isEmpty() && latestList.isEmpty() &&
                        (popularResult is AppResult.Error || latestResult is AppResult.Error)

                val errorMessage = if (hasError) {
                    (popularResult as? AppResult.Error)?.error?.message
                        ?: (latestResult as? AppResult.Error)?.error?.message
                        ?: "Failed to load anime feed"
                } else null

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = errorMessage,
                        popular = popularList,
                        latest = latestList,
                        trending = trendingList,
                        recentlyUpdated = recentlyUpdatedList
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun startOver(animeId: String, episodeId: String, sourceId: String, durationMs: Long) {
        viewModelScope.launch {
            episodeRepository.updateProgress(episodeId, positionMs = 0L, durationMs = durationMs, isWatched = false)
            historyRepository.recordHistory(animeId, episodeId, sourceId, positionMs = 0L, durationMs = durationMs)
        }
    }

    fun markWatched(episodeId: String) {
        viewModelScope.launch {
            episodeRepository.markWatched(episodeId, true)
        }
    }

    fun markUnwatched(episodeId: String) {
        viewModelScope.launch {
            episodeRepository.updateProgress(episodeId, positionMs = 0L, durationMs = 0L, isWatched = false)
            episodeRepository.markWatched(episodeId, false)
        }
    }

    fun removeHistory(episodeId: String) {
        viewModelScope.launch {
            historyRepository.removeHistoryForEpisode(episodeId)
        }
    }

    fun refresh() {
        loadHomeData(isRefresh = true)
    }

    companion object {
        fun provideFactory(
            getPopularAnimeUseCase: GetPopularAnimeUseCase,
            getLatestAnimeUseCase: GetLatestAnimeUseCase,
            historyRepository: HistoryRepository,
            episodeRepository: EpisodeRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    getPopularAnimeUseCase = getPopularAnimeUseCase,
                    getLatestAnimeUseCase = getLatestAnimeUseCase,
                    historyRepository = historyRepository,
                    episodeRepository = episodeRepository
                ) as T
            }
        }
    }
}
