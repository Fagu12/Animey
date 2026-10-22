package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.NavBarStyle
import com.example.model.NavTab

@Composable
fun FloatingGlassDock(
    selectedTab: NavTab,
    navBarStyle: NavBarStyle,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    translucent: Boolean = true,
    horizontalMargin: Dp = 24.dp,
    applySystemBarsPadding: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val dockShape = RoundedCornerShape(999.dp)

    val containerModifier = if (applySystemBarsPadding) {
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = horizontalMargin, vertical = 12.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalMargin)
    }

    Box(
        modifier = containerModifier
            .height(68.dp)
            .shadow(
                elevation = 20.dp,
                shape = dockShape,
                ambientColor = Color.Black,
                spotColor = colorScheme.primary.copy(alpha = 0.35f)
            )
            .clip(dockShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        colorScheme.surfaceContainer.copy(alpha = if (translucent) 0.88f else 0.98f),
                        colorScheme.surfaceContainerHigh.copy(alpha = if (translucent) 0.90f else 0.98f)
                    )
                )
            )
            .border(
                width = 1.1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = 0.42f),
                        colorScheme.outlineVariant.copy(alpha = 0.65f),
                        colorScheme.primary.copy(alpha = 0.28f)
                    )
                ),
                shape = dockShape
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("floating_glass_dock")
    ) {
        when (navBarStyle) {
            NavBarStyle.CLASSIC -> ClassicDockContent(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            NavBarStyle.DYNAMIC_PILL -> DynamicPillDockContent(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    }
}

@Composable
private fun ClassicDockContent(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val pillBg by animateColorAsState(
                targetValue = if (isSelected) {
                    colorScheme.primaryContainer.copy(alpha = 0.72f)
                } else {
                    Color.Transparent
                },
                label = "classic_pill_bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                label = "classic_content_color"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(pillBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(tab) }
                    )
                    .minimumInteractiveComponentSize()
                    .testTag("nav_tab_${tab.name.lowercase()}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = getTabIcon(tab, isSelected),
                    contentDescription = tab.label,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DynamicPillDockContent(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) colorScheme.primary else Color.Transparent,
                label = "dynamic_pill_bg"
            )
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                label = "dynamic_pill_icon"
            )

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(tab) }
                    )
                    .padding(
                        horizontal = if (isSelected) 20.dp else 14.dp,
                        vertical = 8.dp
                    )
                    .minimumInteractiveComponentSize()
                    .testTag("nav_tab_${tab.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = getTabIcon(tab, isSelected),
                    contentDescription = tab.label,
                    tint = iconColor,
                    modifier = Modifier.size(21.dp)
                )
                AnimatedVisibility(
                    visible = isSelected,
                    enter = expandHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = shrinkHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun getTabIcon(tab: NavTab, selected: Boolean): ImageVector {
    return when (tab) {
        NavTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
        NavTab.EXPLORE -> if (selected) Icons.Rounded.Explore else Icons.Outlined.Explore
        NavTab.LIBRARY -> if (selected) Icons.Rounded.VideoLibrary else Icons.Outlined.VideoLibrary
        NavTab.SETTINGS -> if (selected) Icons.Rounded.Tune else Icons.Outlined.Tune
    }
}
