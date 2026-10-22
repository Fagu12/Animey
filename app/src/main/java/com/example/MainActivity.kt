package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.NavTab
import com.example.ui.AnimeyViewModel
import com.example.ui.components.CardStylePreviewDialog
import com.example.ui.components.CarouselStylePreviewDialog
import com.example.ui.components.FloatingGlassDock
import com.example.ui.components.HistoryCardStylePreviewDialog
import com.example.ui.components.NavBarStylePreviewDialog
import com.example.ui.screens.AnimeDetailOverlay
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AnimeyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AnimeyViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AnimeyTheme(
                seedColor = uiState.seedColor,
                pureOledMode = uiState.pureOledDark
            ) {
                AnimeyRootApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AnimeyRootApp(viewModel: AnimeyViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.20f),
                            colorScheme.background,
                            colorScheme.background
                        ),
                        radius = 1100f
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Crossfade(
                    targetState = uiState.activeTab,
                    label = "main_tab_crossfade"
                ) { tab ->
                    when (tab) {
                        NavTab.HOME -> HomeScreen(
                            uiState = uiState,
                            onAnimeClick = { viewModel.openAnimeDetail(it) },
                            onContinueWatchingClick = { cw ->
                                val matched = (uiState.featuredAnime + uiState.trendingAnime + uiState.recentlyUpdatedAnime)
                                    .firstOrNull { it.id == cw.animeId }
                                    ?: uiState.featuredAnime.first()
                                viewModel.startEpisodePlayback(matched, cw.episodeNumber)
                            },
                            onActiveHeroChanged = { bmp, anime ->
                                if (bmp != null) {
                                    viewModel.updateSeedColorFromBitmap(bmp, anime.seedColor)
                                } else {
                                    viewModel.updateSeedColor(anime.seedColor)
                                }
                            },
                            onOpenCardStyleDialog = { viewModel.setShowCardStyleDialog(true) },
                            onOpenHistoryCardStyleDialog = { viewModel.setShowHistoryCardStyleDialog(true) },
                            onOpenCarouselStyleDialog = { viewModel.setShowCarouselStyleDialog(true) },
                            onOpenNavBarStyleDialog = { viewModel.setShowNavBarStyleDialog(true) },
                            onNavigateTab = { viewModel.selectTab(it) }
                        )

                        NavTab.EXPLORE -> ExploreScreen(
                            uiState = uiState,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onGenreSelect = { viewModel.selectGenre(it) },
                            onAnimeClick = { viewModel.openAnimeDetail(it) },
                            onOpenCardStyleDialog = { viewModel.setShowCardStyleDialog(true) },
                            onBackToHome = { viewModel.selectTab(NavTab.HOME) }
                        )

                        NavTab.LIBRARY -> LibraryScreen(
                            uiState = uiState,
                            onSelectFilter = { viewModel.setLibraryFilter(it) },
                            onAnimeClick = { viewModel.openAnimeDetail(it) },
                            onContinueWatchingClick = { cw ->
                                val matched = (uiState.featuredAnime + uiState.trendingAnime + uiState.recentlyUpdatedAnime)
                                    .firstOrNull { it.id == cw.animeId }
                                    ?: uiState.featuredAnime.first()
                                viewModel.startEpisodePlayback(matched, cw.episodeNumber)
                            },
                            onOpenCardStyleDialog = { viewModel.setShowCardStyleDialog(true) },
                            onBackToHome = { viewModel.selectTab(NavTab.HOME) }
                        )

                        NavTab.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            onOpenCardStyleDialog = { viewModel.setShowCardStyleDialog(true) },
                            onOpenHistoryCardStyleDialog = { viewModel.setShowHistoryCardStyleDialog(true) },
                            onOpenCarouselStyleDialog = { viewModel.setShowCarouselStyleDialog(true) },
                            onOpenNavBarStyleDialog = { viewModel.setShowNavBarStyleDialog(true) },
                            onSelectSeedColor = { viewModel.updateSeedColor(it) },
                            onTogglePureOled = { viewModel.setPureOledDark(it) },
                            onToggleTranslucentNav = { viewModel.setTranslucentNav(it) },
                            onToggleCarouselAnimation = { viewModel.setEnableCarouselAnimation(it) },
                            onUpdateNavMargin = { viewModel.setBottomNavMargin(it) },
                            onSelectExtension = { viewModel.selectExtension(it) },
                            onBackToHome = { viewModel.selectTab(NavTab.HOME) }
                        )
                    }
                }
            }

            if (uiState.selectedAnimeForDetail == null) {
                FloatingGlassDock(
                    selectedTab = uiState.activeTab,
                    navBarStyle = uiState.navBarStyle,
                    onTabSelected = { viewModel.selectTab(it) },
                    translucent = uiState.translucentNav,
                    horizontalMargin = uiState.bottomNavHorizontalMarginDp.dp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            AnimatedVisibility(
                visible = uiState.selectedAnimeForDetail != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.selectedAnimeForDetail?.let { selectedAnime ->
                    AnimeDetailOverlay(
                        anime = selectedAnime,
                        activeEpisodeNumber = uiState.activeEpisodeForPlayer,
                        selectedExtensionName = uiState.selectedExtensionName,
                        onClose = { viewModel.closeAnimeDetail() },
                        onStartEpisode = { epNum ->
                            viewModel.startEpisodePlayback(selectedAnime, epNum)
                        },
                        onStopPlayer = { viewModel.stopEpisodePlayback() },
                        onUpdateLibraryStatus = { filter ->
                            viewModel.updateAnimeLibraryStatus(selectedAnime.id, filter)
                        }
                    )
                }
            }
        }
    }

    if (uiState.showCardStyleDialog) {
        CardStylePreviewDialog(
            initialStyle = uiState.cardStyle,
            onDismiss = { viewModel.setShowCardStyleDialog(false) },
            onConfirm = { viewModel.setCardStyle(it) }
        )
    }

    if (uiState.showHistoryCardStyleDialog) {
        HistoryCardStylePreviewDialog(
            initialStyle = uiState.historyCardStyle,
            onDismiss = { viewModel.setShowHistoryCardStyleDialog(false) },
            onConfirm = { viewModel.setHistoryCardStyle(it) }
        )
    }

    if (uiState.showCarouselStyleDialog) {
        CarouselStylePreviewDialog(
            initialStyle = uiState.carouselStyle,
            onDismiss = { viewModel.setShowCarouselStyleDialog(false) },
            onConfirm = { viewModel.setCarouselStyle(it) }
        )
    }

    if (uiState.showNavBarStyleDialog) {
        NavBarStylePreviewDialog(
            initialStyle = uiState.navBarStyle,
            onDismiss = { viewModel.setShowNavBarStyleDialog(false) },
            onConfirm = { viewModel.setNavBarStyle(it) }
        )
    }
}
