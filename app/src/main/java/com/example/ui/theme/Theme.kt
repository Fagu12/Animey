package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun AnimeyTheme(
    seedColor: Color = DefaultMonetSeed,
    pureOledMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val animatedPrimary by animateColorAsState(
        targetValue = seedColor,
        animationSpec = tween(durationMillis = 480),
        label = "monet_primary"
    )

    val bg = if (pureOledMode) OledDeepBackground else Color(0xFF101016)
    val surface = if (pureOledMode) OledSurface else Color(0xFF15151E)
    val primaryContainer = lerp(OledSurfaceHigh, animatedPrimary, 0.28f)
    val secondary = lerp(animatedPrimary, Color.White, 0.22f)
    val surfaceContainer = lerp(OledSurfaceContainer, animatedPrimary, 0.06f)
    val surfaceContainerHigh = lerp(OledSurfaceHigh, animatedPrimary, 0.10f)

    val colorScheme = darkColorScheme(
        primary = animatedPrimary,
        onPrimary = Color(0xFF110D24),
        primaryContainer = primaryContainer,
        onPrimaryContainer = Color(0xFFF3EEFF),
        secondary = secondary,
        onSecondary = Color(0xFF141126),
        secondaryContainer = lerp(OledSurfaceHigh, animatedPrimary, 0.20f),
        onSecondaryContainer = TextPrimaryDark,
        tertiary = lerp(animatedPrimary, Color(0xFF38BDF8), 0.35f),
        background = bg,
        onBackground = TextPrimaryDark,
        surface = surface,
        onSurface = TextPrimaryDark,
        surfaceVariant = surfaceContainerHigh,
        onSurfaceVariant = TextSecondaryDark,
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = OledSurfaceHighest,
        outline = lerp(OutlineSubtleDark, animatedPrimary, 0.38f),
        outlineVariant = OutlineSubtleDark
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
