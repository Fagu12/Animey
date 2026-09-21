package com.example.features.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import com.example.domain.model.TrackingBinding
import com.example.ui.components.TrackingBindingDialog
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.AnimeyApplication
import com.example.R
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.ui.components.AnimeCard
import com.example.ui.components.EmptyState

@Composable
fun AnimeDetailsScreen(
    animeId: String,
    sourceId: String = "",
    sourceAnimeId: String = "",
    onNavigateBack: () -> Unit = {},
    onNavigateToPlayer: (String, String) -> Unit = { _, _ -> },
    onNavigateToAnime: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val container = (context.applicationContext as AnimeyApplication).container

    val viewModel: AnimeDetailsViewModel = viewModel(
        factory = AnimeDetailsViewModel.provideFactory(
            localId = animeId,
            sourceId = sourceId,
            sourceAnimeId = sourceAnimeId,
            getAnimeDetailsUseCase = container.getAnimeDetailsUseCase,
            getEpisodesUseCase = container.getEpisodesUseCase,
            getPopularAnimeUseCase = container.getPopularAnimeUseCase,
            animeRepository = container.animeRepository,
            episodeRepository = container.episodeRepository,
            libraryRepository = container.libraryRepository,
            historyRepository = container.historyRepository,
            trackingBindingRepository = container.trackingBindingRepository,
            autoMapAnimeUseCase = container.autoMapAnimeUseCase,
            searchAniListMediaUseCase = container.searchAniListMediaUseCase
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val isOnline by container.networkMonitor.isOnline.collectAsState(initial = container.networkMonitor.isCurrentlyOnline)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("anime_details_screen_root")
    ) {
        if (uiState.isLoading && uiState.anime == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("details_loading_indicator")
                )
            }
        } else if (uiState.error != null && uiState.anime == null) {
            if (!isOnline) {
                EmptyState(
                    title = "Offline",
                    description = "This anime is not cached locally. Connect to the internet to load its details and episodes.",
                    icon = Icons.Default.CloudOff,
                    actionLabel = "Retry",
                    onActionClick = { viewModel.retry() }
                )
            } else {
                EmptyState(
                    title = "Failed to load anime details",
                    description = uiState.error ?: "Unable to fetch details from extension",
                    icon = Icons.Outlined.Info,
                    actionLabel = "Retry",
                    onActionClick = { viewModel.retry() }
                )
            }
        } else {
            uiState.anime?.let { anime ->
                AnimeDetailsContent(
                    anime = anime,
                    uiState = uiState,
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onSelectSeason = { viewModel.selectSeason(it) },
                    onToggleEpisodeWatched = { viewModel.toggleEpisodeWatched(it) },
                    onPlayEpisode = { episode ->
                        onNavigateToPlayer(anime.localId, episode.id)
                    },
                    onNavigateToAnime = onNavigateToAnime,
                    onNavigateBack = onNavigateBack,
                    onOpenMappingDialog = { viewModel.openBindingDialog() }
                )
            }
        }

        // Dialog for AniList Mapping
        if (uiState.isBindingDialogOpen) {
            TrackingBindingDialog(
                animeTitle = uiState.anime?.title ?: "",
                currentBinding = uiState.trackingBinding,
                autoCandidates = uiState.autoCandidates,
                searchResults = uiState.searchCandidates,
                isSearching = uiState.isSearchingCandidates,
                onSearch = { viewModel.searchAniListCandidates(it) },
                onBind = { mediaId, season, title, cover, confidence ->
                    viewModel.saveBinding(mediaId, season, title, cover, confidence)
                },
                onRemoveBinding = { viewModel.removeBinding() },
                onDismiss = { viewModel.closeBindingDialog() }
            )
        }

        // Top Navigation Back & Action Bar Floating
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .testTag("details_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("details_favorite_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimeDetailsContent(
    anime: Anime,
    uiState: AnimeDetailsUiState,
    onToggleFavorite: () -> Unit,
    onSelectSeason: (Int) -> Unit,
    onToggleEpisodeWatched: (Episode) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onNavigateToAnime: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenMappingDialog: () -> Unit
) {
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    val filteredEpisodes = remember(uiState.episodes, uiState.selectedSeason) {
        uiState.episodes.filter { it.seasonNumber == uiState.selectedSeason }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Banner Hero Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
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

                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
            }
        }

        // Title and Poster Header Info
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Poster
                ElevatedCard(
                    modifier = Modifier
                        .width(115.dp)
                        .height(165.dp)
                        .testTag("details_poster_card"),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.elevatedCardElevation(8.dp)
                ) {
                    if (anime.poster.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(anime.poster)
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = anime.title.take(1),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Metadata Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = anime.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("details_anime_title")
                    )

                    if (anime.alternativeTitles.isNotEmpty()) {
                        Text(
                            text = anime.alternativeTitles.first(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Rating & Status Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (anime.rating != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = String.format("%.1f", anime.rating),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Type & Status Pill
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${anime.type} • ${anime.status}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Year, Season, Studio
                    val metaInfo = listOfNotNull(
                        anime.year?.toString(),
                        anime.season.ifBlank { null },
                        anime.studio.ifBlank { null }
                    ).joinToString(" • ")

                    if (metaInfo.isNotBlank()) {
                        Text(
                            text = metaInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    anime.episodeCount?.let { count ->
                        Text(
                            text = "$count Episodes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Action Buttons Row (Play / Resume + Favorite)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val resumeEpisode = uiState.lastWatchedEpisode ?: uiState.episodes.firstOrNull()
                Button(
                    onClick = {
                        resumeEpisode?.let { onPlayEpisode(it) }
                    },
                    enabled = resumeEpisode != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("details_play_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.lastWatchedEpisode != null && uiState.lastWatchedEpisode.lastPositionMs > 0)
                            "Resume Ep ${uiState.lastWatchedEpisode.number.toInt()}"
                        else "Watch Now",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("details_favorite_outline_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = null,
                        tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isFavorite) "Saved" else "Bookmark",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Genres Chips
        if (anime.genres.isNotEmpty()) {
            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    anime.genres.forEach { genre ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Description with Expand/Collapse
        if (anime.description.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                        .animateContentSize()
                ) {
                    Text(
                        text = anime.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 22.sp,
                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDescriptionExpanded) "Show Less" else "Read More",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (isDescriptionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Tracking Mapping Card
        item {
            TrackingMappingCard(
                binding = uiState.trackingBinding,
                onOpenMappingDialog = onOpenMappingDialog
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // EPISODES SECTION HEADER & SEASON SELECTOR
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Episodes (${uiState.episodes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (uiState.isEpisodesLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Season Selector Tabs if more than 1 season
                if (uiState.availableSeasons.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ScrollableTabRow(
                        selectedTabIndex = uiState.availableSeasons.indexOf(uiState.selectedSeason).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            val index = uiState.availableSeasons.indexOf(uiState.selectedSeason).coerceAtLeast(0)
                            if (index < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[index]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        uiState.availableSeasons.forEach { seasonNum ->
                            Tab(
                                selected = uiState.selectedSeason == seasonNum,
                                onClick = { onSelectSeason(seasonNum) },
                                text = {
                                    Text(
                                        text = "Season $seasonNum",
                                        fontWeight = if (uiState.selectedSeason == seasonNum) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // EPISODE ITEMS LIST
        if (filteredEpisodes.isEmpty() && !uiState.isEpisodesLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No episodes found for this season",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredEpisodes, key = { it.id }) { episode ->
                EpisodeItemRow(
                    episode = episode,
                    onPlay = { onPlayEpisode(episode) },
                    onToggleWatched = { onToggleEpisodeWatched(episode) }
                )
            }
        }

        // RELATED ANIME & RECOMMENDATIONS
        if (uiState.recommendations.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.recommendations, key = { it.localId }) { recAnime ->
                            AnimeCard(
                                anime = recAnime,
                                onClick = { onNavigateToAnime(recAnime.localId) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.relatedAnime.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Related Shows",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.relatedAnime, key = { it.localId }) { relAnime ->
                            AnimeCard(
                                anime = relAnime,
                                onClick = { onNavigateToAnime(relAnime.localId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeItemRow(
    episode: Episode,
    onPlay: () -> Unit,
    onToggleWatched: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onPlay() }
            .testTag("episode_item_${episode.number.toInt()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (episode.isWatched)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail or Episode Number Square
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (episode.thumbnail.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(episode.thumbnail)
                                .crossfade(true)
                                .build(),
                            contentDescription = episode.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Episode Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Episode ${if (episode.number % 1f == 0f) episode.number.toInt() else episode.number}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (episode.isWatched)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface
                        )

                        // Filler / Recap Badges
                        if (episode.isFiller) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "FILLER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (episode.isRecap) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "RECAP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (episode.title.isNotBlank()) {
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (episode.releaseDate.isNotBlank()) {
                        Text(
                            text = episode.releaseDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Watched toggle button
                IconButton(
                    onClick = onToggleWatched,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (episode.isWatched)
                            Icons.Filled.CheckCircle
                        else
                            Icons.Outlined.CheckCircleOutline,
                        contentDescription = "Toggle watched",
                        tint = if (episode.isWatched)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Watch progress bar if partially watched
            if (episode.lastPositionMs > 0 && !episode.isWatched) {
                LinearProgressIndicator(
                    progress = { episode.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun TrackingMappingCard(
    binding: TrackingBinding?,
    onOpenMappingDialog: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onOpenMappingDialog() }
            .testTag("tracking_mapping_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (binding != null) "AniList Mapped: ${binding.aniListTitle ?: "ID #${binding.aniListId}"}" else "AniList Tracking: Not Mapped",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (binding != null) "Season ${binding.season} • ${(binding.mappingConfidence * 100).toInt()}% Confidence Match" else "Tap to auto-map or search AniList entry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onOpenMappingDialog,
                modifier = Modifier.testTag("map_anilist_button")
            ) {
                Text(if (binding != null) "Edit" else "Map")
            }
        }
    }
}
