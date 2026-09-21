package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Brand - Electric Indigo / Neon Violet
val AnimeyPrimary = Color(0xFF6366F1)
val AnimeyPrimaryVariant = Color(0xFF4F46E5)
val AnimeySecondary = Color(0xFF06B6D4)
val AnimeyTertiary = Color(0xFFEC4899)
val AnimeyAccent = Color(0xFFF43F5E)

// Dark Surfaces (Obsidian & Deep Slate)
val AnimeyDarkBackground = Color(0xFF090A0F)
val AnimeyDarkSurface = Color(0xFF111420)
val AnimeyDarkSurfaceVariant = Color(0xFF191E2E)
val AnimeyDarkCard = Color(0xFF1E2438)
val AnimeyDarkBorder = Color(0xFF2E3852)

// AMOLED Surfaces (Pure Pitch Black)
val AnimeyAmoledBackground = Color(0xFF000000)
val AnimeyAmoledSurface = Color(0xFF000000)
val AnimeyAmoledSurfaceVariant = Color(0xFF0C0D12)
val AnimeyAmoledCard = Color(0xFF08090D)
val AnimeyAmoledBorder = Color(0xFF1C1E26)

// Text & Content (Dark Mode)
val AnimeyTextPrimary = Color(0xFFF8FAFC)
val AnimeyTextSecondary = Color(0xFF94A3B8)
val AnimeyTextMuted = Color(0xFF64748B)

// Light Theme equivalents
val AnimeyLightBackground = Color(0xFFF8FAFC)
val AnimeyLightSurface = Color(0xFFFFFFFF)
val AnimeyLightSurfaceVariant = Color(0xFFF1F5F9)
val AnimeyLightCard = Color(0xFFFFFFFF)
val AnimeyLightBorder = Color(0xFFE2E8F0)
val AnimeyLightTextPrimary = Color(0xFF0F172A)
val AnimeyLightTextSecondary = Color(0xFF475569)
val AnimeyLightTextMuted = Color(0xFF94A3B8)

enum class AccentColor(
    val id: String,
    val displayName: String,
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val tertiary: Color,
    val previewColor: Color
) {
    INDIGO(
        id = "INDIGO",
        displayName = "Electric Indigo",
        primary = Color(0xFF6366F1),
        primaryVariant = Color(0xFF4F46E5),
        secondary = Color(0xFF06B6D4),
        tertiary = Color(0xFFEC4899),
        previewColor = Color(0xFF6366F1)
    ),
    CYAN(
        id = "CYAN",
        displayName = "Cyber Cyan",
        primary = Color(0xFF06B6D4),
        primaryVariant = Color(0xFF0891B2),
        secondary = Color(0xFF3B82F6),
        tertiary = Color(0xFF10B981),
        previewColor = Color(0xFF06B6D4)
    ),
    SAKURA(
        id = "SAKURA",
        displayName = "Sakura Pink",
        primary = Color(0xFFEC4899),
        primaryVariant = Color(0xFFDB2777),
        secondary = Color(0xFFF43F5E),
        tertiary = Color(0xFFA855F7),
        previewColor = Color(0xFFEC4899)
    ),
    EMERALD(
        id = "EMERALD",
        displayName = "Jade Emerald",
        primary = Color(0xFF10B981),
        primaryVariant = Color(0xFF059669),
        secondary = Color(0xFF14B8A6),
        tertiary = Color(0xFF3B82F6),
        previewColor = Color(0xFF10B981)
    ),
    AMBER(
        id = "AMBER",
        displayName = "Solar Amber",
        primary = Color(0xFFF59E0B),
        primaryVariant = Color(0xFFD97706),
        secondary = Color(0xFFEF4444),
        tertiary = Color(0xFF8B5CF6),
        previewColor = Color(0xFFF59E0B)
    ),
    CRIMSON(
        id = "CRIMSON",
        displayName = "Crimson Red",
        primary = Color(0xFFEF4444),
        primaryVariant = Color(0xFFDC2626),
        secondary = Color(0xFFF97316),
        tertiary = Color(0xFFEC4899),
        previewColor = Color(0xFFEF4444)
    ),
    PURPLE(
        id = "PURPLE",
        displayName = "Royal Amethyst",
        primary = Color(0xFF8B5CF6),
        primaryVariant = Color(0xFF7C3AED),
        secondary = Color(0xFFEC4899),
        tertiary = Color(0xFF06B6D4),
        previewColor = Color(0xFF8B5CF6)
    );

    companion object {
        fun fromId(id: String?): AccentColor =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: INDIGO
    }
}
