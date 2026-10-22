package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AnimeItem
import com.example.model.ContinueWatchingItem
import com.example.model.LibraryFilter
import com.example.ui.AnimeyUiState
import com.example.ui.components.AnimeCard
import com.example.ui.components.ContinueWatchingCard

@Composable
fun LibraryScreen(
    uiState: AnimeyUiState,
    onSelectFilter: (LibraryFilter) -> Unit,
    onAnimeClick: (AnimeItem) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onOpenCardStyleDialog: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val colorScheme = MaterialTheme.colorScheme
    val allAnime = remember(uiState.featuredAnime, uiState.trendingAnime, uiState.recentlyUpdatedAnime) {
        (uiState.featuredAnime + uiState.trendingAnime + uiState.recentlyUpdatedAnime)
            .distinctBy { it.id }
    }

    val filteredList = remember(allAnime, uiState.libraryFilter) {
        if (uiState.libraryFilter == LibraryFilter.HISTORY) {
            allAnime
        } else {
            allAnime.filter { it.libraryStatus == uiState.libraryFilter }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp)
            .testTag("library_screen")
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(26.dp),
            color = colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MovieFilter,
                            contentDescription = null,
                            tint = colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Anime Library",
                            style = MaterialTheme.typography.titleLarge,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your synced anime watchlist & episode progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenCardStyleDialog) {
                        Icon(
                            imageVector = Icons.Rounded.Style,
                            contentDescription = "Customize Card Style",
                            tint = colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LibraryFilter.entries) { filter ->
                        val isSelected = uiState.libraryFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectFilter(filter) },
                            leadingIcon = if (filter == LibraryFilter.HISTORY) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = filter.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.primary,
                                selectedLabelColor = colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(99.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.libraryFilter == LibraryFilter.HISTORY) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.continueWatching.forEach { item ->
                    ContinueWatchingCard(
                        item = item,
                        style = uiState.historyCardStyle,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onContinueWatchingClick(item) }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    bottom = 120.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { anime ->
                    AnimeCard(
                        title = anime.title,
                        posterUrl = anime.posterUrl,
                        score = anime.score,
                        cardStyle = uiState.cardStyle,
                        currentEpisode = anime.currentEpisode,
                        totalEpisodes = anime.totalEpisodes,
                        seedColor = anime.seedColor,
                        showEpisodeInBanner = true,
                        onTap = { onAnimeClick(anime) }
                    )
                }
            }
        }
    }
}
