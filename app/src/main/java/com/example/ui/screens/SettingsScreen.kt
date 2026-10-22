package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AnimeyUiState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: AnimeyUiState,
    onOpenCardStyleDialog: () -> Unit,
    onOpenHistoryCardStyleDialog: () -> Unit,
    onOpenCarouselStyleDialog: () -> Unit,
    onOpenNavBarStyleDialog: () -> Unit,
    onSelectSeedColor: (Color) -> Unit,
    onTogglePureOled: (Boolean) -> Unit,
    onToggleTranslucentNav: (Boolean) -> Unit,
    onToggleCarouselAnimation: (Boolean) -> Unit,
    onUpdateNavMargin: (Int) -> Unit,
    onSelectExtension: (String) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val colorScheme = MaterialTheme.colorScheme
    val seedPalette = listOf(
        Color(0xFF9E86FF),
        Color(0xFF56B4FC),
        Color(0xFFF46B84),
        Color(0xFF4ADE80),
        Color(0xFFFBBF24),
        Color(0xFFC084FC),
        Color(0xFF2DD4BF)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 12.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    text = "UI & Appearance Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize Dynamic Material You theming, card layouts, carousels & dock",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsGroupHeader("LAYOUT & STYLES")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceContainer
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                Column {
                    SettingsNavigationRow(
                        icon = Icons.Rounded.Style,
                        title = "Card Style",
                        subtitle = "Active: ${uiState.cardStyle.label} • Customize media card presentation",
                        onClick = onOpenCardStyleDialog,
                        testTag = "settings_row_card_style"
                    )
                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsNavigationRow(
                        icon = Icons.Rounded.History,
                        title = "History Card Style",
                        subtitle = "Active: ${uiState.historyCardStyle.label} • Customize Continue Watching cards",
                        onClick = onOpenHistoryCardStyleDialog,
                        testTag = "settings_row_history_card_style"
                    )
                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsNavigationRow(
                        icon = Icons.Rounded.ViewCarousel,
                        title = "Carousel Style",
                        subtitle = "Active: ${uiState.carouselStyle.label} • Switch between Classic banner & Portrait",
                        onClick = onOpenCarouselStyleDialog,
                        testTag = "settings_row_carousel_style"
                    )
                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsNavigationRow(
                        icon = Icons.Rounded.Dock,
                        title = "Nav Bar Style",
                        subtitle = "Active: ${uiState.navBarStyle.label} • Floating Glassmorphic Pill Dock",
                        onClick = onOpenNavBarStyleDialog,
                        testTag = "settings_row_nav_bar_style"
                    )
                }
            }
        }

        item {
            SettingsGroupHeader("DYNAMIC MONET / MATERIAL YOU THEME")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceContainer
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Monet Accent Seed Color",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically extracted from active anime poster or pick manually",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(seedPalette) { color ->
                            val isSelected = color == uiState.seedColor
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectSeedColor(color) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected Seed Color",
                                        tint = Color(0xFF100C20),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsToggleRow(
                        icon = Icons.Rounded.Brush,
                        title = "Pure OLED Deep Dark (#0A0A0C)",
                        subtitle = "High-contrast true black background for OLED displays",
                        checked = uiState.pureOledDark,
                        onCheckedChange = onTogglePureOled
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Animation,
                        title = "Enable Carousel Animation",
                        subtitle = "Smooth motion & palette extraction on hero carousels",
                        checked = uiState.enableCarouselAnimation,
                        onCheckedChange = onToggleCarouselAnimation
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.Dock,
                        title = "Translucent Glassmorphic Nav",
                        subtitle = "Enable translucent frosted glass bottom navigation dock",
                        checked = uiState.translucentNav,
                        onCheckedChange = onToggleTranslucentNav
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bottom Nav Bar Margin",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "${uiState.bottomNavHorizontalMarginDp} dp",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Slider(
                        value = uiState.bottomNavHorizontalMarginDp.toFloat(),
                        onValueChange = { onUpdateNavMargin(it.roundToInt()) },
                        valueRange = 12f..48f
                    )
                }
            }
        }

        item {
            SettingsGroupHeader("INSTALLED ANIME EXTENSIONS")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceContainer
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    uiState.extensions.forEachIndexed { idx, ext ->
                        val isSelected = ext.name == uiState.selectedExtensionName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectExtension(ext.name) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ext.accentColor.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Extension,
                                    contentDescription = null,
                                    tint = ext.accentColor
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ext.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = ext.language,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = ext.version,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Active Extension",
                                        tint = colorScheme.onPrimary,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                        if (idx < uiState.extensions.lastIndex) {
                            HorizontalDivider(
                                color = colorScheme.outlineVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.4.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = colorScheme.surfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
