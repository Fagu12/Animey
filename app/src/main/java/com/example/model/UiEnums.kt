package com.example.model

import androidx.compose.ui.graphics.Color

enum class CardStyle(
    val id: String,
    val label: String,
    val description: String
) {
    DEFAULT_STYLE(
        id = "default_style",
        label = "Default",
        description = "Container card layout with poster, title, extension info, and progress count."
    ),
    SAIKOU(
        id = "saikou",
        label = "Saikou",
        description = "Clean vertical poster card with bottom-right badge rating."
    ),
    EXOTIC(
        id = "exotic",
        label = "Exotic",
        description = "Bordered card with glowing shadow and vibrant action banner."
    ),
    MODERN(
        id = "modern",
        label = "Modern",
        description = "Full cover card with dark gradient title overlay at the bottom."
    )
}

enum class HistoryCardStyle(
    val id: String,
    val label: String,
    val description: String
) {
    REGULAR(
        id = "regular",
        label = "Regular",
        description = "Classic card layout showing media title, progress, and date details."
    ),
    FROSTED_GLASS(
        id = "frosted_glass",
        label = "Frosted Glass",
        description = "Frosted glass container overlay with soft background color elements."
    ),
    BOOTIFUL(
        id = "bootiful",
        label = "Bootiful",
        description = "An immersive, premium poster-first card layout."
    )
}

enum class CarouselStyle(
    val id: String,
    val label: String,
    val description: String
) {
    CLASSIC(
        id = "classic",
        label = "Classic",
        description = "Full-width hero banner with compact details and quick synopsis."
    ),
    PORTRAIT(
        id = "portrait",
        label = "Portrait",
        description = "Classic carousel card layout featuring vertical poster cards."
    )
}

enum class NavBarStyle(
    val id: String,
    val label: String,
    val description: String
) {
    CLASSIC(
        id = "classic",
        label = "Classic",
        description = "Pill indicator with icon and label"
    ),
    DYNAMIC_PILL(
        id = "dynamic_pill",
        label = "Dynamic Pill",
        description = "Selected tab expands with label, others shrink to icon only"
    )
}

enum class NavTab(val label: String) {
    HOME("Home"),
    EXPLORE("Explore"),
    LIBRARY("Library"),
    SETTINGS("Settings")
}

enum class LibraryFilter(val label: String) {
    WATCHING("Watching"),
    PLANNING("Planning"),
    COMPLETED("Completed"),
    HISTORY("History")
}

data class EpisodeItem(
    val number: Int,
    val title: String,
    val duration: String,
    val airDate: String,
    val watchedProgress: Float = 0f,
    val isFiller: Boolean = false
)

data class StreamServer(
    val id: String,
    val name: String,
    val quality: String,
    val audioType: String,
    val latencyMs: Int
)

data class AnimeExtension(
    val id: String,
    val name: String,
    val version: String,
    val language: String,
    val sourceType: String,
    val accentColor: Color,
    val isInstalled: Boolean = true
)

data class AnimeItem(
    val id: String,
    val title: String,
    val japaneseTitle: String,
    val synopsis: String,
    val posterUrl: String,
    val bannerUrl: String,
    val score: Double,
    val studio: String,
    val status: String,
    val releaseYear: Int,
    val currentEpisode: Int?,
    val totalEpisodes: Int?,
    val genres: List<String>,
    val seedColor: Color,
    val libraryStatus: LibraryFilter = LibraryFilter.WATCHING,
    val episodes: List<EpisodeItem> = emptyList()
)

data class ContinueWatchingItem(
    val id: String,
    val animeId: String,
    val animeTitle: String,
    val episodeTitle: String,
    val episodeNumber: Int,
    val posterUrl: String,
    val bannerUrl: String,
    val updatedDate: String,
    val remainingTimeText: String,
    val progress: Float,
    val seedColor: Color
)
