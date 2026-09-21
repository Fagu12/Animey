package com.example.features.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.logging.AppLogger
import com.example.core.result.AppResult
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.EpisodeRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.LibraryRepository
import com.example.domain.usecase.GetAnimeDetailsUseCase
import com.example.domain.usecase.GetEpisodesUseCase
import com.example.domain.usecase.GetPopularAnimeUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.domain.model.TrackingBinding
import com.example.domain.model.TrackingPlatform
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.repository.TrackingBindingRepository
import com.example.domain.usecase.tracking.AutoMapAnimeUseCase
import com.example.domain.usecase.tracking.BindingCandidate
import com.example.domain.usecase.tracking.SearchAniListMediaUseCase

data class AnimeDetailsUiState(
    val isLoading: Boolean = true,
    val isEpisodesLoading: Boolean = true,
    val error: String? = null,
    val anime: Anime? = null,
    val episodes: List<Episode> = emptyList(),
    val selectedSeason: Int = 1,
    val availableSeasons: List<Int> = listOf(1),
    val isFavorite: Boolean = false,
    val isInLibrary: Boolean = false,
    val lastWatchedEpisode: Episode? = null,
    val relatedAnime: List<Anime> = emptyList(),
    val recommendations: List<Anime> = emptyList(),
    val trackingBinding: TrackingBinding? = null,
    val autoCandidates: List<BindingCandidate> = emptyList(),
    val searchCandidates: List<AniListMediaEntry> = emptyList(),
    val isSearchingCandidates: Boolean = false,
    val isBindingDialogOpen: Boolean = false
)

class AnimeDetailsViewModel(
    private val localId: String,
    private val sourceId: String,
    private val sourceAnimeId: String,
    private val getAnimeDetailsUseCase: GetAnimeDetailsUseCase,
    private val getEpisodesUseCase: GetEpisodesUseCase,
    private val getPopularAnimeUseCase: GetPopularAnimeUseCase,
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
    private val libraryRepository: LibraryRepository,
    private val historyRepository: HistoryRepository,
    private val trackingBindingRepository: TrackingBindingRepository,
    private val autoMapAnimeUseCase: AutoMapAnimeUseCase,
    private val searchAniListMediaUseCase: SearchAniListMediaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeDetailsUiState())
    val uiState: StateFlow<AnimeDetailsUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        observeTrackingBinding()
    }

    private fun observeTrackingBinding() {
        viewModelScope.launch {
            trackingBindingRepository.getBinding(localId, TrackingPlatform.ANILIST).collect { binding ->
                _uiState.update { it.copy(trackingBinding = binding) }
            }
        }
    }

    fun loadAll() {
        loadDetails()
        loadEpisodes()
        loadRecommendations()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Check local cache first
            val localAnime = animeRepository.getAnimeByIdDirect(localId)
            if (localAnime != null) {
                _uiState.update { it.copy(anime = localAnime, isFavorite = localAnime.isFavorite) }
            }

            val effectiveSourceId = sourceId.ifBlank { localAnime?.sourceId ?: "" }
            val effectiveSourceAnimeId = sourceAnimeId.ifBlank { localAnime?.sourceAnimeId ?: "" }

            if (effectiveSourceId.isBlank() || effectiveSourceAnimeId.isBlank()) {
                // If pure local without source info, keep local
                if (localAnime != null) {
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Anime source identifier not found") }
                }
                return@launch
            }

            val detailsResult = getAnimeDetailsUseCase(
                sourceId = effectiveSourceId,
                sourceAnimeId = effectiveSourceAnimeId,
                localId = localId
            )

            when (detailsResult) {
                is AppResult.Success -> {
                    val anime = detailsResult.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            anime = anime,
                            isFavorite = anime.isFavorite
                        )
                    }
                }
                is AppResult.Error -> {
                    val errMsg = detailsResult.error.message
                    AppLogger.e("AnimeDetailsVM", "Failed loading details for $localId ($effectiveSourceId): $errMsg")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Source details unavailable: $errMsg"
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun loadEpisodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isEpisodesLoading = true) }

            val effectiveSourceId = sourceId.ifBlank { _uiState.value.anime?.sourceId ?: "" }
            val effectiveSourceAnimeId = sourceAnimeId.ifBlank { _uiState.value.anime?.sourceAnimeId ?: "" }

            val episodesResult = getEpisodesUseCase(
                sourceId = effectiveSourceId,
                sourceAnimeId = effectiveSourceAnimeId,
                animeLocalId = localId
            )

            when (episodesResult) {
                is AppResult.Success -> {
                    val allEpisodes = episodesResult.data.sortedBy { it.number }
                    val seasons = allEpisodes.map { it.seasonNumber }.distinct().sorted()
                    val activeSeasons = if (seasons.isEmpty()) listOf(1) else seasons
                    val initialSeason = activeSeasons.first()

                    // Check history for last watched episode
                    val lastWatched = allEpisodes.firstOrNull { it.lastPositionMs > 0 && !it.isWatched }
                        ?: allEpisodes.firstOrNull { it.isWatched }
                        ?: allEpisodes.firstOrNull()

                    AppLogger.i("Mangayomi", "[MANGAYOMI][DETAIL][UI] episodeCount=${allEpisodes.size}")

                    _uiState.update {
                        it.copy(
                            isEpisodesLoading = false,
                            episodes = allEpisodes,
                            availableSeasons = activeSeasons,
                            selectedSeason = initialSeason,
                            lastWatchedEpisode = lastWatched,
                            error = if (allEpisodes.isEmpty() && it.anime == null) "No episodes found for this title" else it.error
                        )
                    }
                }
                is AppResult.Error -> {
                    val errMsg = episodesResult.error.message
                    AppLogger.e("AnimeDetailsVM", "Failed loading episodes for $localId ($effectiveSourceId): $errMsg")
                    _uiState.update {
                        it.copy(
                            isEpisodesLoading = false,
                            error = it.error ?: "Failed loading episodes: $errMsg"
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            val popular = getPopularAnimeUseCase(page = 1)
            if (popular is AppResult.Success) {
                val filtered = popular.data.filterNot { it.localId == localId }
                _uiState.update {
                    it.copy(
                        relatedAnime = filtered.take(6),
                        recommendations = filtered.reversed().take(6)
                    )
                }
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        _uiState.update { it.copy(selectedSeason = seasonNumber) }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val current = _uiState.value.anime ?: return@launch
            val nextState = !current.isFavorite
            val updated = current.copy(isFavorite = nextState)
            animeRepository.saveAnime(updated)
            _uiState.update { it.copy(anime = updated, isFavorite = nextState) }
        }
    }

    fun toggleEpisodeWatched(episode: Episode) {
        viewModelScope.launch {
            val nextWatched = !episode.isWatched
            episodeRepository.updateProgress(
                episodeId = episode.id,
                positionMs = if (nextWatched) episode.durationMs else 0L,
                durationMs = episode.durationMs,
                isWatched = nextWatched
            )
            _uiState.update { state ->
                val updatedList = state.episodes.map {
                    if (it.id == episode.id) it.copy(isWatched = nextWatched, lastPositionMs = if (nextWatched) it.durationMs else 0L)
                    else it
                }
                state.copy(episodes = updatedList)
            }
        }
    }

    fun retry() {
        loadAll()
    }

    fun openBindingDialog() {
        _uiState.update { it.copy(isBindingDialogOpen = true) }
        autoMapAnime()
    }

    fun closeBindingDialog() {
        _uiState.update { it.copy(isBindingDialogOpen = false) }
    }

    fun autoMapAnime() {
        viewModelScope.launch {
            val title = _uiState.value.anime?.title ?: return@launch
            _uiState.update { it.copy(isSearchingCandidates = true) }
            val res = autoMapAnimeUseCase(title)
            if (res is AppResult.Success) {
                _uiState.update { it.copy(isSearchingCandidates = false, autoCandidates = res.data) }
            } else {
                _uiState.update { it.copy(isSearchingCandidates = false) }
            }
        }
    }

    fun searchAniListCandidates(query: String) {
        viewModelScope.launch {
            if (query.trim().isBlank()) return@launch
            _uiState.update { it.copy(isSearchingCandidates = true) }
            val res = searchAniListMediaUseCase(query)
            if (res is AppResult.Success) {
                _uiState.update { it.copy(isSearchingCandidates = false, searchCandidates = res.data) }
            } else {
                _uiState.update { it.copy(isSearchingCandidates = false) }
            }
        }
    }

    fun saveBinding(
        aniListId: Int,
        season: Int,
        title: String?,
        cover: String?,
        confidence: Float
    ) {
        viewModelScope.launch {
            val effSourceId = sourceId.ifBlank { _uiState.value.anime?.sourceId ?: "" }
            val effSourceAnimeId = sourceAnimeId.ifBlank { _uiState.value.anime?.sourceAnimeId ?: "" }

            val binding = TrackingBinding(
                localAnimeId = localId,
                sourceId = effSourceId,
                sourceAnimeId = effSourceAnimeId,
                platform = TrackingPlatform.ANILIST,
                aniListId = aniListId,
                season = season,
                mappingConfidence = confidence,
                aniListTitle = title,
                aniListCoverImage = cover
            )

            trackingBindingRepository.saveBinding(binding)
            closeBindingDialog()
        }
    }

    fun removeBinding() {
        viewModelScope.launch {
            trackingBindingRepository.deleteBinding(localId, TrackingPlatform.ANILIST)
            closeBindingDialog()
        }
    }

    companion object {
        fun provideFactory(
            localId: String,
            sourceId: String,
            sourceAnimeId: String,
            getAnimeDetailsUseCase: GetAnimeDetailsUseCase,
            getEpisodesUseCase: GetEpisodesUseCase,
            getPopularAnimeUseCase: GetPopularAnimeUseCase,
            animeRepository: AnimeRepository,
            episodeRepository: EpisodeRepository,
            libraryRepository: LibraryRepository,
            historyRepository: HistoryRepository,
            trackingBindingRepository: TrackingBindingRepository,
            autoMapAnimeUseCase: AutoMapAnimeUseCase,
            searchAniListMediaUseCase: SearchAniListMediaUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AnimeDetailsViewModel(
                    localId = localId,
                    sourceId = sourceId,
                    sourceAnimeId = sourceAnimeId,
                    getAnimeDetailsUseCase = getAnimeDetailsUseCase,
                    getEpisodesUseCase = getEpisodesUseCase,
                    getPopularAnimeUseCase = getPopularAnimeUseCase,
                    animeRepository = animeRepository,
                    episodeRepository = episodeRepository,
                    libraryRepository = libraryRepository,
                    historyRepository = historyRepository,
                    trackingBindingRepository = trackingBindingRepository,
                    autoMapAnimeUseCase = autoMapAnimeUseCase,
                    searchAniListMediaUseCase = searchAniListMediaUseCase
                ) as T
            }
        }
    }
}
