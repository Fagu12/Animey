package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AnimeRepository
import com.example.model.AnimeExtension
import com.example.model.AnimeItem
import com.example.model.CardStyle
import com.example.model.CarouselStyle
import com.example.model.ContinueWatchingItem
import com.example.model.HistoryCardStyle
import com.example.model.LibraryFilter
import com.example.model.NavBarStyle
import com.example.model.NavTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnimeyUiState(
    val activeTab: NavTab = NavTab.HOME,
    val cardStyle: CardStyle = CardStyle.EXOTIC,
    val historyCardStyle: HistoryCardStyle = HistoryCardStyle.FROSTED_GLASS,
    val carouselStyle: CarouselStyle = CarouselStyle.CLASSIC,
    val navBarStyle: NavBarStyle = NavBarStyle.DYNAMIC_PILL,
    val seedColor: Color = AnimeRepository.defaultSeedColor,
    val pureOledDark: Boolean = true,
    val enableCarouselAnimation: Boolean = true,
    val translucentNav: Boolean = true,
    val bottomNavHorizontalMarginDp: Int = 24,
    val featuredAnime: List<AnimeItem> = AnimeRepository.curatedFeaturedAnime,
    val trendingAnime: List<AnimeItem> = AnimeRepository.curatedTrendingAnime,
    val recentlyUpdatedAnime: List<AnimeItem> = AnimeRepository.curatedRecentlyUpdated,
    val continueWatching: List<ContinueWatchingItem> = AnimeRepository.initialContinueWatching,
    val libraryFilter: LibraryFilter = LibraryFilter.WATCHING,
    val searchQuery: String = "",
    val selectedGenre: String = "All",
    val selectedAnimeForDetail: AnimeItem? = null,
    val activeEpisodeForPlayer: Int? = null,
    val selectedExtensionName: String = "HiAnime",
    val extensions: List<AnimeExtension> = AnimeRepository.animeExtensions,
    val showCardStyleDialog: Boolean = false,
    val showHistoryCardStyleDialog: Boolean = false,
    val showCarouselStyleDialog: Boolean = false,
    val showNavBarStyleDialog: Boolean = false
)

class AnimeyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeyUiState())
    val uiState: StateFlow<AnimeyUiState> = _uiState.asStateFlow()

    init {
        refreshLiveAnimeCatalog()
    }

    fun refreshLiveAnimeCatalog() {
        viewModelScope.launch {
            val liveList = AnimeRepository.fetchLiveTopAnime()
            if (liveList.isNotEmpty()) {
                _uiState.update { state ->
                    val mergedTrending = (state.trendingAnime + liveList).distinctBy { it.title }
                    state.copy(trendingAnime = mergedTrending)
                }
            }
        }
    }

    fun selectTab(tab: NavTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setCardStyle(style: CardStyle) {
        _uiState.update { it.copy(cardStyle = style, showCardStyleDialog = false) }
    }

    fun setHistoryCardStyle(style: HistoryCardStyle) {
        _uiState.update { it.copy(historyCardStyle = style, showHistoryCardStyleDialog = false) }
    }

    fun setCarouselStyle(style: CarouselStyle) {
        _uiState.update { it.copy(carouselStyle = style, showCarouselStyleDialog = false) }
    }

    fun setNavBarStyle(style: NavBarStyle) {
        _uiState.update { it.copy(navBarStyle = style, showNavBarStyleDialog = false) }
    }

    fun updateSeedColor(color: Color) {
        _uiState.update { it.copy(seedColor = color) }
    }

    fun updateSeedColorFromBitmap(bitmap: Bitmap, fallbackColor: Color) {
        val extracted = AnimeRepository.extractSeedColorFromBitmap(bitmap) ?: fallbackColor
        _uiState.update { it.copy(seedColor = extracted) }
    }

    fun setPureOledDark(enabled: Boolean) {
        _uiState.update { it.copy(pureOledDark = enabled) }
    }

    fun setTranslucentNav(enabled: Boolean) {
        _uiState.update { it.copy(translucentNav = enabled) }
    }

    fun setEnableCarouselAnimation(enabled: Boolean) {
        _uiState.update { it.copy(enableCarouselAnimation = enabled) }
    }

    fun setBottomNavMargin(marginDp: Int) {
        _uiState.update { it.copy(bottomNavHorizontalMarginDp = marginDp.coerceIn(12, 48)) }
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(libraryFilter = filter) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectGenre(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre) }
    }

    fun openAnimeDetail(anime: AnimeItem) {
        _uiState.update {
            it.copy(
                selectedAnimeForDetail = anime,
                seedColor = anime.seedColor,
                activeEpisodeForPlayer = null
            )
        }
    }

    fun closeAnimeDetail() {
        _uiState.update {
            it.copy(
                selectedAnimeForDetail = null,
                activeEpisodeForPlayer = null
            )
        }
    }

    fun startEpisodePlayback(anime: AnimeItem, episodeNumber: Int) {
        _uiState.update { state ->
            val updatedEntry = ContinueWatchingItem(
                id = "cw_${anime.id}",
                animeId = anime.id,
                animeTitle = anime.title,
                episodeTitle = anime.episodes.firstOrNull { it.number == episodeNumber }?.title
                    ?: "Episode $episodeNumber",
                episodeNumber = episodeNumber,
                posterUrl = anime.posterUrl,
                bannerUrl = anime.bannerUrl,
                updatedDate = "2026-10-22",
                remainingTimeText = "16:20 left",
                progress = 0.42f,
                seedColor = anime.seedColor
            )
            val filtered = state.continueWatching.filterNot { it.animeId == anime.id }
            state.copy(
                selectedAnimeForDetail = anime,
                activeEpisodeForPlayer = episodeNumber,
                seedColor = anime.seedColor,
                continueWatching = listOf(updatedEntry) + filtered
            )
        }
    }

    fun stopEpisodePlayback() {
        _uiState.update { it.copy(activeEpisodeForPlayer = null) }
    }

    fun updateAnimeLibraryStatus(animeId: String, newStatus: LibraryFilter) {
        _uiState.update { state ->
            val updateItem: (AnimeItem) -> AnimeItem = { item ->
                if (item.id == animeId) item.copy(libraryStatus = newStatus) else item
            }
            state.copy(
                featuredAnime = state.featuredAnime.map(updateItem),
                trendingAnime = state.trendingAnime.map(updateItem),
                recentlyUpdatedAnime = state.recentlyUpdatedAnime.map(updateItem),
                selectedAnimeForDetail = state.selectedAnimeForDetail?.let(updateItem)
            )
        }
    }

    fun selectExtension(name: String) {
        _uiState.update { it.copy(selectedExtensionName = name) }
    }

    fun setShowCardStyleDialog(show: Boolean) {
        _uiState.update { it.copy(showCardStyleDialog = show) }
    }

    fun setShowHistoryCardStyleDialog(show: Boolean) {
        _uiState.update { it.copy(showHistoryCardStyleDialog = show) }
    }

    fun setShowCarouselStyleDialog(show: Boolean) {
        _uiState.update { it.copy(showCarouselStyleDialog = show) }
    }

    fun setShowNavBarStyleDialog(show: Boolean) {
        _uiState.update { it.copy(showNavBarStyleDialog = show) }
    }
}
