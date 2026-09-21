package com.example.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.AnimeyApplication
import com.example.domain.model.Anime
import com.example.domain.model.HistoryEntry
import com.example.ui.components.AnimeCard
import com.example.ui.components.AnimeRowSkeleton
import com.example.ui.components.AnimeyTopBar
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorStateView
import com.example.ui.components.HeroBannerSkeleton

@Composable
fun HomeScreen(
    onNavigateToAnime: (String) -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToExtensions: () -> Unit = {},
    onNavigateToPlayer: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val container = (context.applicationContext as AnimeyApplication).container

    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            getPopularAnimeUseCase = container.getPopularAnimeUseCase,
            getLatestAnimeUseCase = container.getLatestAnimeUseCase,
            historyRepository = container.historyRepository,
            episodeRepository = container.episodeRepository
        )
    )

    val isOnline by container.networkMonitor.isOnline.collectAsState(initial = container.networkMonitor.isCurrentlyOnline)
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("All", "Trending", "Popular", "Latest", "Recently Updated")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen_root")
    ) {
        AnimeyTopBar(
            title = "Animey",
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToDownloads = onNavigateToDownloads,
            onNavigateToSettings = onNavigateToSettings
        )

        if (uiState.isLoading && uiState.trending.isEmpty() && uiState.popular.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_loading_indicator")
            ) {
                HeroBannerSkeleton()
                Spacer(modifier = Modifier.height(16.dp))
                AnimeRowSkeleton(count = 4)
            }
        } else if (uiState.error != null && uiState.trending.isEmpty() && uiState.popular.isEmpty()) {
            if (!isOnline) {
                EmptyState(
                    title = "Offline",
                    description = "You are currently offline. Your Library, Watch History, and Downloaded episodes remain fully accessible.",
                    icon = Icons.Default.CloudOff,
                    actionLabel = "View Downloads",
                    onActionClick = onNavigateToDownloads
                )
            } else {
                ErrorStateView(
                    title = "Couldn't load feed",
                    message = uiState.error ?: "Failed to fetch catalog from extensions",
                    retryLabel = "Retry",
                    onRetry = { viewModel.refresh() }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Extension shortcut banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("extension_status_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Extension,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Extension Engine Ready",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Modular stream providers connected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = onNavigateToExtensions,
                                modifier = Modifier.testTag("home_extensions_shortcut_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Manage Extensions",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Category Filter Chips
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = uiState.selectedCategory == category,
                                onClick = { viewModel.selectCategory(category) },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("home_filter_chip_$category")
                            )
                        }
                    }
                }

                // Featured Hero Banner (Top Trending anime)
                val featuredAnime = uiState.trending.firstOrNull() ?: uiState.popular.firstOrNull()
                if (featuredAnime != null && (uiState.selectedCategory == "All" || uiState.selectedCategory == "Trending")) {
                    item {
                        FeaturedHeroBanner(
                            anime = featuredAnime,
                            onClick = { onNavigateToAnime(featuredAnime.localId) }
                        )
                    }
                }

                // Continue Watching Section (if user has watch history)
                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        ContinueWatchingSection(
                            historyList = uiState.continueWatching,
                            onAnimeClick = onNavigateToAnime,
                            onResumeClick = { animeId, epId -> onNavigateToPlayer(animeId, epId) },
                            onStartOverClick = { entry ->
                                viewModel.startOver(
                                    animeId = entry.anime.localId,
                                    episodeId = entry.episode.id,
                                    sourceId = entry.sourceId,
                                    durationMs = entry.durationMs
                                )
                                onNavigateToPlayer(entry.anime.localId, entry.episode.id)
                            },
                            onMarkWatchedClick = { entry -> viewModel.markWatched(entry.episode.id) },
                            onMarkUnwatchedClick = { entry -> viewModel.markUnwatched(entry.episode.id) },
                            onRemoveClick = { entry -> viewModel.removeHistory(entry.episode.id) },
                            onSeeAllClick = onNavigateToHistory
                        )
                    }
                }

                // Trending Section
                if (uiState.trending.isNotEmpty() && (uiState.selectedCategory == "All" || uiState.selectedCategory == "Trending")) {
                    item {
                        SectionHeader(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            title = "Trending Now",
                            tag = "home_trending_header"
                        )
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(uiState.trending, key = { _, anime -> anime.localId }) { index, anime ->
                                Box {
                                    AnimeCard(
                                        anime = anime,
                                        onClick = { onNavigateToAnime(anime.localId) },
                                        modifier = Modifier.width(140.dp)
                                    )
                                    // Rank badge #1, #2...
                                    Surface(
                                        color = if (index < 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(2.dp)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index < 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Popular Section
                if (uiState.popular.isNotEmpty() && (uiState.selectedCategory == "All" || uiState.selectedCategory == "Popular")) {
                    item {
                        SectionHeader(
                            icon = Icons.Default.LocalFireDepartment,
                            title = "Popular Anime",
                            tag = "home_popular_header"
                        )
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.popular, key = { it.localId }) { anime ->
                                AnimeCard(
                                    anime = anime,
                                    onClick = { onNavigateToAnime(anime.localId) },
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Latest Section
                if (uiState.latest.isNotEmpty() && (uiState.selectedCategory == "All" || uiState.selectedCategory == "Latest")) {
                    item {
                        SectionHeader(
                            icon = Icons.Default.NewReleases,
                            title = "Latest Releases",
                            tag = "home_latest_header"
                        )
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.latest, key = { it.localId }) { anime ->
                                AnimeCard(
                                    anime = anime,
                                    onClick = { onNavigateToAnime(anime.localId) },
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Recently Updated Section
                if (uiState.recentlyUpdated.isNotEmpty() && (uiState.selectedCategory == "All" || uiState.selectedCategory == "Recently Updated")) {
                    item {
                        SectionHeader(
                            icon = Icons.Default.Update,
                            title = "Recently Updated",
                            tag = "home_recently_updated_header"
                        )
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.recentlyUpdated.reversed(), key = { it.localId }) { anime ->
                                AnimeCard(
                                    anime = anime,
                                    onClick = { onNavigateToAnime(anime.localId) },
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun FeaturedHeroBanner(
    anime: Anime,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("home_hero_featured_card"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (anime.banner.isNotBlank() || anime.poster.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(anime.banner.ifBlank { anime.poster })
                        .crossfade(true)
                        .build(),
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // Content inside hero
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "FEATURED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (anime.rating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", anime.rating),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = "${anime.type} • ${anime.genres.take(2).joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingSection(
    historyList: List<HistoryEntry>,
    onAnimeClick: (String) -> Unit,
    onResumeClick: (String, String) -> Unit,
    onStartOverClick: (HistoryEntry) -> Unit,
    onMarkWatchedClick: (HistoryEntry) -> Unit,
    onMarkUnwatchedClick: (HistoryEntry) -> Unit,
    onRemoveClick: (HistoryEntry) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Continue Watching",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "History",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(historyList, key = { it.episode.id }) { entry ->
                var menuExpanded by remember { mutableStateOf(false) }

                ElevatedCard(
                    modifier = Modifier
                        .width(260.dp)
                        .clickable { onResumeClick(entry.anime.localId, entry.episode.id) }
                        .testTag("continue_watching_card_${entry.episode.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        // Poster / Thumbnail with overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(105.dp)
                        ) {
                            val imageUrl = entry.episode.thumbnail.ifBlank { entry.anime.poster.ifBlank { entry.anime.banner } }
                            if (imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = entry.anime.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Season badge top start
                            val seasonNum = entry.episode.seasonNumber.takeIf { it > 0 } ?: 1
                            Surface(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 8.dp),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = "Season $seasonNum",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp
                                )
                            }

                            // Menu button top end
                            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Resume Episode") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onResumeClick(entry.anime.localId, entry.episode.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Start Over") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onStartOverClick(entry)
                                        }
                                    )
                                    if (!entry.episode.isWatched) {
                                        DropdownMenuItem(
                                            text = { Text("Mark Watched") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onMarkWatchedClick(entry)
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("Mark Unwatched") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onMarkUnwatchedClick(entry)
                                            }
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = { Text("Remove from History", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onRemoveClick(entry)
                                        }
                                    )
                                }
                            }
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { entry.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )

                        // Info Column
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = entry.anime.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Episode ${entry.episode.number.toInt()}${if (entry.episode.title.isNotBlank()) ": ${entry.episode.title}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Resume Button & Start Over Button Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { onResumeClick(entry.anime.localId, entry.episode.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .testTag("continue_watching_resume_btn_${entry.episode.id}"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onStartOverClick(entry) },
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("continue_watching_start_over_btn_${entry.episode.id}"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Start Over", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
