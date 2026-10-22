package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.model.AnimeItem
import com.example.model.ContinueWatchingItem
import com.example.model.NavTab
import com.example.ui.AnimeyUiState
import com.example.ui.components.AnimeCard
import com.example.ui.components.AnimePosterImage
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.HeroCarousel
import java.util.Calendar

@Composable
fun HomeScreen(
    uiState: AnimeyUiState,
    onAnimeClick: (AnimeItem) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onActiveHeroChanged: (Bitmap?, AnimeItem) -> Unit,
    onOpenCardStyleDialog: () -> Unit,
    onOpenHistoryCardStyleDialog: () -> Unit,
    onOpenCarouselStyleDialog: () -> Unit,
    onOpenNavBarStyleDialog: () -> Unit,
    onNavigateTab: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 5 || hour >= 23 -> "You're up late"
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    color = colorScheme.surfaceContainer.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Animey",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onNavigateTab(NavTab.SETTINGS) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsNone,
                                contentDescription = "Notifications & Settings",
                                tint = colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(colorScheme.primary, colorScheme.tertiary)
                                    )
                                )
                                .clickable { onNavigateTab(NavTab.SETTINGS) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "F4",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Hey ")
                        withStyle(SpanStyle(color = colorScheme.primary)) {
                            append("F4GU")
                        }
                        append(", what are we watching today?")
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Stream your favourite seasonal anime simulcasts, movies, and OVAs in crisp HD!",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionBannerCard(
                        subtitle = "${uiState.trendingAnime.size} items",
                        title = "ANIME LIST",
                        posterUrl = uiState.featuredAnime.firstOrNull()?.posterUrl.orEmpty(),
                        seedColor = colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(NavTab.LIBRARY) }
                    )
                    QuickActionBannerCard(
                        subtitle = "Simulcast & Top Airing",
                        title = "SEASONAL",
                        posterUrl = uiState.featuredAnime.getOrNull(1)?.posterUrl.orEmpty(),
                        seedColor = colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(NavTab.EXPLORE) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = onOpenCardStyleDialog,
                        label = { Text("Card: ${uiState.cardStyle.label}") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Style,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.testTag("chip_card_style")
                    )
                    AssistChip(
                        onClick = onOpenCarouselStyleDialog,
                        label = { Text("Carousel: ${uiState.carouselStyle.label}") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ViewCarousel,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.testTag("chip_carousel_style")
                    )
                    AssistChip(
                        onClick = onOpenNavBarStyleDialog,
                        label = { Text("Dock: ${uiState.navBarStyle.label}") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Dock,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.testTag("chip_nav_style")
                    )
                    AssistChip(
                        onClick = onOpenHistoryCardStyleDialog,
                        label = { Text("History: ${uiState.historyCardStyle.label}") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.testTag("chip_history_style")
                    )
                }
            }
        }

        item {
            HeroCarousel(
                items = uiState.featuredAnime,
                style = uiState.carouselStyle,
                onAnimeClick = onAnimeClick,
                onActiveAnimeChanged = onActiveHeroChanged
            )
        }

        uiState.featuredAnime.getOrNull(2)?.let { releaseAnime ->
            item {
                NewEpisodeReleaseCard(
                    anime = releaseAnime,
                    onClick = { onAnimeClick(releaseAnime) },
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeaderRow(
                    title = "Continue Watching",
                    actionText = uiState.historyCardStyle.label,
                    onActionClick = onOpenHistoryCardStyleDialog
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.continueWatching, key = { it.id }) { cwItem ->
                        ContinueWatchingCard(
                            item = cwItem,
                            style = uiState.historyCardStyle,
                            onClick = { onContinueWatchingClick(cwItem) }
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeaderRow(
                    title = "Recently Updated",
                    actionText = uiState.cardStyle.label,
                    onActionClick = onOpenCardStyleDialog
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.recentlyUpdatedAnime, key = { it.id }) { anime ->
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

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeaderRow(
                    title = "Trending Anime",
                    actionText = "Explore All",
                    onActionClick = { onNavigateTab(NavTab.EXPLORE) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.trendingAnime, key = { it.id }) { anime ->
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
    }
}

@Composable
private fun QuickActionBannerCard(
    subtitle: String,
    title: String,
    posterUrl: String,
    seedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1.25f)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxSize()
                ) {
                    AnimePosterImage(
                        imageUrl = posterUrl,
                        contentDescription = title,
                        fallbackColor = seedColor
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        colorScheme.surfaceContainerHigh,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun NewEpisodeReleaseCard(
    anime: AnimeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "New Episode Releases",
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(132.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AnimePosterImage(
                        imageUrl = anime.posterUrl,
                        contentDescription = anime.title,
                        fallbackColor = anime.seedColor
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = anime.title.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "NEW EPISODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = colorScheme.surfaceContainerHighest
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "EPISODE ${(anime.currentEpisode ?: 7) + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watched up to episode ${anime.currentEpisode ?: 7}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "1 episode behind",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Released Today",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = colorScheme.surfaceContainerHigh,
            modifier = Modifier.clickable(onClick = onActionClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
