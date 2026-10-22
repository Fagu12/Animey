package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.CardStyle
import com.example.model.ContinueWatchingItem
import com.example.model.HistoryCardStyle
import java.util.Locale

@Composable
fun AnimeCard(
    title: String,
    posterUrl: String,
    score: Double,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    currentEpisode: Int? = null,
    totalEpisodes: Int? = null,
    seedColor: Color = MaterialTheme.colorScheme.primary,
    cardWidth: Dp = 148.dp,
    showEpisodeInBanner: Boolean = false,
    onTap: () -> Unit = {}
) {
    val formattedScore = String.format(Locale.US, "%.1f", score)
    val episodeText = when {
        currentEpisode != null && totalEpisodes != null -> "EP $currentEpisode/$totalEpisodes"
        currentEpisode != null -> "Episode $currentEpisode"
        totalEpisodes != null -> "$totalEpisodes EPS"
        else -> ""
    }

    Box(
        modifier = modifier
            .width(cardWidth)
            .testTag("anime_card_${title.lowercase(Locale.ROOT).replace(" ", "_")}")
    ) {
        when (cardStyle) {
            CardStyle.DEFAULT_STYLE -> DefaultAnimeCardContent(
                title = title,
                posterUrl = posterUrl,
                formattedScore = formattedScore,
                episodeText = episodeText,
                seedColor = seedColor,
                onTap = onTap
            )

            CardStyle.SAIKOU -> SaikouAnimeCardContent(
                title = title,
                posterUrl = posterUrl,
                formattedScore = formattedScore,
                episodeText = episodeText,
                seedColor = seedColor,
                onTap = onTap
            )

            CardStyle.MODERN -> ModernAnimeCardContent(
                title = title,
                posterUrl = posterUrl,
                formattedScore = formattedScore,
                episodeText = episodeText,
                seedColor = seedColor,
                onTap = onTap
            )

            CardStyle.EXOTIC -> ExoticAnimeCardContent(
                title = title,
                posterUrl = posterUrl,
                formattedScore = formattedScore,
                currentEpisode = currentEpisode,
                showEpisodeInBanner = showEpisodeInBanner,
                seedColor = seedColor,
                onTap = onTap
            )
        }
    }
}

@Composable
private fun DefaultAnimeCardContent(
    title: String,
    posterUrl: String,
    formattedScore: String,
    episodeText: String,
    seedColor: Color,
    onTap: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(13.dp))
            ) {
                AnimePosterImage(
                    imageUrl = posterUrl,
                    contentDescription = title,
                    fallbackColor = seedColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formattedScore,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (episodeText.isNotEmpty()) {
                    Text(
                        text = episodeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SaikouAnimeCardContent(
    title: String,
    posterUrl: String,
    formattedScore: String,
    episodeText: String,
    seedColor: Color,
    onTap: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onTap)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AnimePosterImage(
                imageUrl = posterUrl,
                contentDescription = title,
                fallbackColor = seedColor
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.primary.copy(alpha = 0.94f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formattedScore,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        if (episodeText.isNotEmpty()) {
            Text(
                text = episodeText,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun ModernAnimeCardContent(
    title: String,
    posterUrl: String,
    formattedScore: String,
    episodeText: String,
    seedColor: Color,
    onTap: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onTap)
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
                    Brush.verticalGradient(
                        0.32f to Color.Transparent,
                        0.68f to Color.Black.copy(alpha = 0.25f),
                        1.0f to Color.Black.copy(alpha = 0.90f)
                    )
                )
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            shape = RoundedCornerShape(99.dp),
            color = colorScheme.primary.copy(alpha = 0.92f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = formattedScore,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (episodeText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episodeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun ExoticAnimeCardContent(
    title: String,
    posterUrl: String,
    formattedScore: String,
    currentEpisode: Int?,
    showEpisodeInBanner: Boolean,
    seedColor: Color,
    onTap: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = colorScheme.primary.copy(alpha = 0.45f),
                spotColor = colorScheme.primary.copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(colorScheme.surfaceContainer)
            .border(
                width = 1.3.dp,
                color = colorScheme.primary.copy(alpha = 0.52f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onTap)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.73f)
                .clip(RoundedCornerShape(14.dp))
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
                        Brush.verticalGradient(
                            0.48f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )
            if (showEpisodeInBanner) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(99.dp),
                    color = colorScheme.primary.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formattedScore,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            shape = RoundedCornerShape(11.dp),
            color = colorScheme.primary.copy(alpha = 0.88f)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (showEpisodeInBanner && currentEpisode != null) {
                    Text(
                        text = "Episode $currentEpisode",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedScore,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    style: HistoryCardStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    when (style) {
        HistoryCardStyle.REGULAR -> {
            Card(
                modifier = modifier
                    .width(310.dp)
                    .height(132.dp)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                ContinueWatchingRowBody(item = item)
            }
        }

        HistoryCardStyle.FROSTED_GLASS -> {
            Box(
                modifier = modifier
                    .width(310.dp)
                    .height(132.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colorScheme.surfaceContainerHigh,
                                colorScheme.primary.copy(alpha = 0.26f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = colorScheme.primary.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable(onClick = onClick)
            ) {
                ContinueWatchingRowBody(item = item)
            }
        }

        HistoryCardStyle.BOOTIFUL -> {
            Card(
                modifier = modifier
                    .width(300.dp)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        AnimePosterImage(
                            imageUrl = item.bannerUrl,
                            contentDescription = item.animeTitle,
                            fallbackColor = item.seedColor
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.primary
                        ) {
                            Text(
                                text = "Episode ${item.episodeNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = item.updatedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.episodeTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.animeTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(99.dp)),
                            color = colorScheme.primary,
                            trackColor = colorScheme.surfaceContainerHighest,
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.remainingTimeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingRowBody(item: ContinueWatchingItem) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(82.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(13.dp))
        ) {
            AnimePosterImage(
                imageUrl = item.posterUrl,
                contentDescription = item.animeTitle,
                fallbackColor = item.seedColor
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colorScheme.primary
            ) {
                Text(
                    text = "Episode ${item.episodeNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
            Column {
                Text(
                    text = item.episodeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.animeTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.updatedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.remainingTimeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = colorScheme.primary,
                    trackColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun AnimePosterImage(
    imageUrl: String,
    contentDescription: String,
    fallbackColor: Color,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .allowHardware(false)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                fallbackColor.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    )
            )
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                fallbackColor.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MovieFilter,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    )
}
