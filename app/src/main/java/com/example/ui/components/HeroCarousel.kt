package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.model.AnimeItem
import com.example.model.CarouselStyle
import java.util.Locale

@Composable
fun HeroCarousel(
    items: List<AnimeItem>,
    style: CarouselStyle,
    onAnimeClick: (AnimeItem) -> Unit,
    onActiveAnimeChanged: (Bitmap?, AnimeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size }
    )
    val context = LocalContext.current

    LaunchedEffect(pagerState.currentPage, items) {
        val currentAnime = items.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        onActiveAnimeChanged(null, currentAnime)
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(currentAnime.posterUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    onActiveAnimeChanged(bmp, currentAnime)
                }
            }
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_carousel"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (style) {
            CarouselStyle.CLASSIC -> {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val anime = items[page]
                    ClassicCarouselSlide(
                        anime = anime,
                        onClick = { onAnimeClick(anime) }
                    )
                }
            }

            CarouselStyle.PORTRAIT -> {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 76.dp),
                    pageSpacing = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val anime = items[page]
                    val isSelected = page == pagerState.currentPage
                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.88f,
                        animationSpec = tween(280),
                        label = "portrait_scale"
                    )
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.55f,
                        animationSpec = tween(280),
                        label = "portrait_alpha"
                    )
                    PortraitCarouselSlide(
                        anime = anime,
                        isSelected = isSelected,
                        modifier = Modifier
                            .scale(animatedScale)
                            .graphicsLayer { alpha = animatedAlpha },
                        onClick = { onAnimeClick(anime) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, _ ->
                val isSelected = index == pagerState.currentPage
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 7.dp,
                    animationSpec = tween(240),
                    label = "carousel_dot"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.5.dp)
                        .width(dotWidth)
                        .height(7.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun ClassicCarouselSlide(
    anime: AnimeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                AnimePosterImage(
                    imageUrl = anime.bannerUrl,
                    contentDescription = anime.title,
                    fallbackColor = anime.seedColor
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.35f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.72f)
                            )
                        )
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(99.dp),
                    color = Color.Black.copy(alpha = 0.62f)
                ) {
                    Text(
                        text = "${anime.studio} • ${anime.status}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                CarouselScoreBadge(score = anime.score)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = anime.synopsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Rounded.NorthEast,
                        contentDescription = "Open Anime",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PortraitCarouselSlide(
    anime: AnimeItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(
                    width = if (isSelected) 1.5.dp else 0.dp,
                    color = if (isSelected) colorScheme.primary.copy(alpha = 0.65f) else Color.Transparent,
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            AnimePosterImage(
                imageUrl = anime.posterUrl,
                contentDescription = anime.title,
                fallbackColor = anime.seedColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = anime.title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            CarouselScoreBadge(score = anime.score)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = anime.synopsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.NorthEast,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun CarouselScoreBadge(score: Double) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = colorScheme.primaryContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                text = String.format(Locale.US, "%.1f", score),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
