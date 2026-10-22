package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AnimeRepository
import com.example.model.CardStyle
import com.example.model.CarouselStyle
import com.example.model.HistoryCardStyle
import com.example.model.NavBarStyle
import com.example.model.NavTab

@Composable
fun CardStylePreviewDialog(
    initialStyle: CardStyle,
    onDismiss: () -> Unit,
    onConfirm: (CardStyle) -> Unit
) {
    var tempStyle by remember(initialStyle) { mutableStateOf(initialStyle) }
    val sampleAnime = AnimeRepository.curatedFeaturedAnime.first()

    StyleSelectorDialogFrame(
        title = "Card Style",
        onCancel = onDismiss,
        onConfirm = { onConfirm(tempStyle) },
        previewContent = {
            AnimeCard(
                title = sampleAnime.title,
                posterUrl = sampleAnime.posterUrl,
                score = sampleAnime.score,
                cardStyle = tempStyle,
                currentEpisode = sampleAnime.currentEpisode,
                totalEpisodes = sampleAnime.totalEpisodes,
                seedColor = sampleAnime.seedColor,
                cardWidth = 148.dp
            )
        }
    ) {
        CardStyle.entries.forEachIndexed { index, style ->
            StyleRadioOptionRow(
                title = style.label,
                description = style.description,
                selected = tempStyle == style,
                onClick = { tempStyle = style }
            )
            if (index < CardStyle.entries.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryCardStylePreviewDialog(
    initialStyle: HistoryCardStyle,
    onDismiss: () -> Unit,
    onConfirm: (HistoryCardStyle) -> Unit
) {
    var tempStyle by remember(initialStyle) { mutableStateOf(initialStyle) }
    val sampleItem = AnimeRepository.initialContinueWatching.first()

    StyleSelectorDialogFrame(
        title = "Card Style",
        onCancel = onDismiss,
        onConfirm = { onConfirm(tempStyle) },
        previewContent = {
            ContinueWatchingCard(
                item = sampleItem,
                style = tempStyle
            )
        }
    ) {
        HistoryCardStyle.entries.forEachIndexed { index, style ->
            StyleRadioOptionRow(
                title = style.label,
                description = style.description,
                selected = tempStyle == style,
                onClick = { tempStyle = style }
            )
            if (index < HistoryCardStyle.entries.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        }
    }
}

@Composable
fun CarouselStylePreviewDialog(
    initialStyle: CarouselStyle,
    onDismiss: () -> Unit,
    onConfirm: (CarouselStyle) -> Unit
) {
    var tempStyle by remember(initialStyle) { mutableStateOf(initialStyle) }
    val sampleAnime = AnimeRepository.curatedFeaturedAnime.first()

    StyleSelectorDialogFrame(
        title = "Carousel Style",
        onCancel = onDismiss,
        onConfirm = { onConfirm(tempStyle) },
        previewContent = {
            if (tempStyle == CarouselStyle.CLASSIC) {
                ClassicCarouselSlide(
                    anime = sampleAnime,
                    onClick = {}
                )
            } else {
                Box(modifier = Modifier.width(205.dp)) {
                    PortraitCarouselSlide(
                        anime = sampleAnime,
                        isSelected = true,
                        onClick = {}
                    )
                }
            }
        }
    ) {
        CarouselStyle.entries.forEachIndexed { index, style ->
            StyleRadioOptionRow(
                title = style.label,
                description = style.description,
                selected = tempStyle == style,
                onClick = { tempStyle = style }
            )
            if (index < CarouselStyle.entries.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        }
    }
}

@Composable
fun NavBarStylePreviewDialog(
    initialStyle: NavBarStyle,
    onDismiss: () -> Unit,
    onConfirm: (NavBarStyle) -> Unit
) {
    var tempStyle by remember(initialStyle) { mutableStateOf(initialStyle) }
    var previewTab by remember { mutableStateOf(NavTab.HOME) }

    StyleSelectorDialogFrame(
        title = "Nav Bar Style",
        onCancel = onDismiss,
        onConfirm = { onConfirm(tempStyle) },
        previewContent = {
            FloatingGlassDock(
                selectedTab = previewTab,
                navBarStyle = tempStyle,
                onTabSelected = { previewTab = it },
                horizontalMargin = 8.dp,
                applySystemBarsPadding = false
            )
        }
    ) {
        NavBarStyle.entries.forEachIndexed { index, style ->
            StyleRadioOptionRow(
                title = style.label,
                description = style.description,
                selected = tempStyle == style,
                onClick = { tempStyle = style }
            )
            if (index < NavBarStyle.entries.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun StyleSelectorDialogFrame(
    title: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    previewContent: @Composable () -> Unit,
    optionsContent: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(30.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LIVE PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.6.sp,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        previewContent()
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        optionsContent()
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(
                            topStart = 24.dp,
                            bottomStart = 24.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.surfaceContainerHigh,
                            contentColor = colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 8.dp,
                            topEnd = 24.dp,
                            bottomEnd = 24.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Confirm",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleRadioOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorScheme.primary,
                unselectedColor = colorScheme.onSurfaceVariant
            )
        )
    }
}
