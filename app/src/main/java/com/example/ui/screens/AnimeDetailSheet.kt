package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AnimeRepository
import com.example.model.AnimeItem
import com.example.model.EpisodeItem
import com.example.model.LibraryFilter
import com.example.ui.components.AnimePosterImage

@Composable
fun AnimeDetailOverlay(
    anime: AnimeItem,
    activeEpisodeNumber: Int?,
    selectedExtensionName: String,
    onClose: () -> Unit,
    onStartEpisode: (Int) -> Unit,
    onStopPlayer: () -> Unit,
    onUpdateLibraryStatus: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = {
        if (activeEpisodeNumber != null) {
            onStopPlayer()
        } else {
            onClose()
        }
    })

    val colorScheme = MaterialTheme.colorScheme
    var selectedServer by remember { mutableStateOf(AnimeRepository.streamServers.first()) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackPosition by remember { mutableFloatStateOf(0.36f) }

    val episodes = remember(anime) {
        if (anime.episodes.isNotEmpty()) {
            anime.episodes
        } else {
            val count = (anime.totalEpisodes ?: 12).coerceAtMost(12)
            (1..count).map { ep ->
                EpisodeItem(
                    number = ep,
                    title = "Episode $ep — ${anime.title}",
                    duration = "23:40",
                    airDate = "2025-10-${(ep + 10).coerceAtMost(28)}",
                    watchedProgress = if (ep <= (anime.currentEpisode ?: 1)) 1f else 0f
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("anime_detail_overlay")
    ) {
        AnimePosterImage(
            imageUrl = anime.bannerUrl,
            contentDescription = anime.title,
            fallbackColor = anime.seedColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to colorScheme.background.copy(alpha = 0.55f),
                        0.32f to colorScheme.background.copy(alpha = 0.92f),
                        0.55f to colorScheme.background
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 12.dp,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close details",
                                tint = colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = colorScheme.primaryContainer.copy(alpha = 0.88f)
                    ) {
                        Text(
                            text = "$selectedExtensionName • 1080P SIMULCAST",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (activeEpisodeNumber != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        border = BorderStroke(1.2.dp, colorScheme.primary.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = episodes.firstOrNull { it.number == activeEpisodeNumber }?.title
                                            ?: "Episode $activeEpisodeNumber",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = anime.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colorScheme.surfaceContainerHighest
                                        ) {
                                            Text(
                                                text = "Episode $activeEpisodeNumber • ${selectedServer.quality}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "HW+",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (activeEpisodeNumber > 1) onStartEpisode(activeEpisodeNumber - 1)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous Episode",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.primary,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clickable { isPlaying = !isPlaying }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = colorScheme.onPrimary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onStartEpisode(activeEpisodeNumber + 1) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next Episode",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        playbackPosition = (playbackPosition + 0.18f).coerceAtMost(0.98f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FastForward,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+85s OP Skip",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Slider(
                                value = playbackPosition,
                                onValueChange = { playbackPosition = it }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "08:32 / 23:40",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Hd,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ClosedCaption,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(118.dp)
                            .height(168.dp)
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        AnimePosterImage(
                            imageUrl = anime.posterUrl,
                            contentDescription = anime.title,
                            fallbackColor = anime.seedColor
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = anime.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = anime.studio,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = anime.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val next = when (anime.libraryStatus) {
                                LibraryFilter.WATCHING -> LibraryFilter.COMPLETED
                                LibraryFilter.COMPLETED -> LibraryFilter.PLANNING
                                else -> LibraryFilter.WATCHING
                            }
                            onUpdateLibraryStatus(next)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = anime.libraryStatus.label.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { onStartEpisode(anime.currentEpisode ?: 1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watch Ep ${anime.currentEpisode ?: 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdded,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Stream Server Selection",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        AnimeRepository.streamServers.forEachIndexed { index, srv ->
                            val isSelected = srv.id == selectedServer.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedServer = srv }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(34.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerHigh
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isSelected) colorScheme.onPrimary else colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = srv.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "CC • ${srv.audioType} • ${srv.latencyMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                                )
                            }
                            if (index < AnimeRepository.streamServers.lastIndex) {
                                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.35f))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Episodes (${episodes.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            items(episodes, key = { it.number }) { ep ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartEpisode(ep.number) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeEpisodeNumber == ep.number) {
                            colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            colorScheme.surfaceContainer
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (activeEpisodeNumber == ep.number) {
                            colorScheme.primary
                        } else {
                            colorScheme.outlineVariant.copy(alpha = 0.55f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.primary
                            ) {
                                Text(
                                    text = "EP ${ep.number}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ep.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ep.airDate} • ${ep.duration}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Stream Episode ${ep.number}",
                                tint = colorScheme.primary
                            )
                        }
                        if (ep.watchedProgress > 0f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { ep.watchedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(99.dp)),
                                color = colorScheme.primary,
                                trackColor = colorScheme.surfaceContainerHighest
                            )
                        }
                    }
                }
            }
        }
    }
}
