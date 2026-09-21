package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

fun buildDarkColorScheme(accent: AccentColor, amoled: Boolean = false): ColorScheme {
    val bg = if (amoled) AnimeyAmoledBackground else AnimeyDarkBackground
    val surface = if (amoled) AnimeyAmoledSurface else AnimeyDarkSurface
    val surfaceVariant = if (amoled) AnimeyAmoledSurfaceVariant else AnimeyDarkSurfaceVariant
    val outline = if (amoled) AnimeyAmoledBorder else AnimeyDarkBorder

    return darkColorScheme(
        primary = accent.primary,
        onPrimary = Color.White,
        primaryContainer = accent.primaryVariant,
        onPrimaryContainer = Color.White,
        secondary = accent.secondary,
        onSecondary = Color.Black,
        secondaryContainer = accent.secondary.copy(alpha = 0.2f),
        onSecondaryContainer = Color.White,
        tertiary = accent.tertiary,
        onTertiary = Color.White,
        tertiaryContainer = accent.tertiary.copy(alpha = 0.2f),
        onTertiaryContainer = Color.White,
        background = bg,
        onBackground = AnimeyTextPrimary,
        surface = surface,
        onSurface = AnimeyTextPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = AnimeyTextSecondary,
        outline = outline
    )
}

fun buildLightColorScheme(accent: AccentColor): ColorScheme {
    return lightColorScheme(
        primary = accent.primaryVariant,
        onPrimary = Color.White,
        primaryContainer = accent.primary.copy(alpha = 0.15f),
        onPrimaryContainer = accent.primaryVariant,
        secondary = accent.secondary,
        onSecondary = Color.White,
        secondaryContainer = accent.secondary.copy(alpha = 0.15f),
        onSecondaryContainer = Color.Black,
        tertiary = accent.tertiary,
        onTertiary = Color.White,
        tertiaryContainer = accent.tertiary.copy(alpha = 0.15f),
        onTertiaryContainer = Color.Black,
        background = AnimeyLightBackground,
        onBackground = AnimeyLightTextPrimary,
        surface = AnimeyLightSurface,
        onSurface = AnimeyLightTextPrimary,
        surfaceVariant = AnimeyLightSurfaceVariant,
        onSurfaceVariant = AnimeyLightTextSecondary,
        outline = AnimeyLightBorder
    )
}

@Composable
fun AnimeyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    amoled: Boolean = false,
    accentColor: String = "INDIGO",
    content: @Composable () -> Unit
) {
    val selectedAccent = AccentColor.fromId(accentColor)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && amoled) {
                base.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = AnimeyAmoledSurfaceVariant,
                    outline = AnimeyAmoledBorder
                )
            } else {
                base
            }
        }
        darkTheme -> buildDarkColorScheme(selectedAccent, amoled = amoled)
        else -> buildLightColorScheme(selectedAccent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AnimeyTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

