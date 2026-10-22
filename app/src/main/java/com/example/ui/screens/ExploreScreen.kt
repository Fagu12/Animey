package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AnimeItem
import com.example.ui.AnimeyUiState
import com.example.ui.components.AnimeCard

@Composable
fun ExploreScreen(
    uiState: AnimeyUiState,
    onSearchQueryChange: (String) -> Unit,
    onGenreSelect: (String) -> Unit,
    onAnimeClick: (AnimeItem) -> Unit,
    onOpenCardStyleDialog: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val colorScheme = MaterialTheme.colorScheme
    val genres = listOf("All", "Action", "Fantasy", "Supernatural", "Adventure", "Sci-Fi", "Drama")

    val allAnime = remember(uiState.featuredAnime, uiState.trendingAnime, uiState.recentlyUpdatedAnime) {
        (uiState.featuredAnime + uiState.trendingAnime + uiState.recentlyUpdatedAnime)
            .distinctBy { it.id }
    }

    val filteredAnime = remember(allAnime, uiState.searchQuery, uiState.selectedGenre) {
        allAnime.filter { anime ->
            val matchesQuery = uiState.searchQuery.isBlank() ||
                anime.title.contains(uiState.searchQuery, ignoreCase = true) ||
                anime.studio.contains(uiState.searchQuery, ignoreCase = true)
            val matchesGenre = uiState.selectedGenre == "All" ||
                anime.genres.any { it.equals(uiState.selectedGenre, ignoreCase = true) }
            matchesQuery && matchesGenre
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp)
            .testTag("explore_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Explore Anime",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Discover seasonal simulcasts & top rated anime",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.surfaceContainerHigh
            ) {
                IconButton(onClick = onOpenCardStyleDialog) {
                    Icon(
                        imageVector = Icons.Rounded.Style,
                        contentDescription = "Change Card Style",
                        tint = colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .testTag("explore_search_input"),
            placeholder = { Text("Search anime title or studio...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surfaceContainer,
                unfocusedContainerColor = colorScheme.surfaceContainer,
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                val selected = genre == uiState.selectedGenre
                FilterChip(
                    selected = selected,
                    onClick = { onGenreSelect(genre) },
                    label = { Text(genre) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorScheme.primary,
                        selectedLabelColor = colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(99.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 4.dp,
                bottom = 120.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredAnime, key = { it.id }) { anime ->
                AnimeCard(
                    title = anime.title,
                    posterUrl = anime.posterUrl,
                    score = anime.score,
                    cardStyle = uiState.cardStyle,
                    currentEpisode = anime.currentEpisode,
                    totalEpisodes = anime.totalEpisodes,
                    seedColor = anime.seedColor,
                    onTap = { onAnimeClick(anime) }
                )
            }
        }
    }
}
