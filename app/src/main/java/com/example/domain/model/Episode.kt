package com.example.domain.model

/**
 * Normalized Domain Episode model.
 * Season-aware and source-independent.
 */
data class Episode(
    val id: String,
    val animeId: String,
    val sourceId: String,
    val sourceEpisodeId: String,
    val number: Float,
    val seasonNumber: Int = 1,
    val title: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val durationMs: Long = 0L,
    val lastPositionMs: Long = 0L,
    val isFiller: Boolean = false,
    val isRecap: Boolean = false,
    val isWatched: Boolean = false,
    val isDownloaded: Boolean = false,
    val skipSegments: List<SkipSegment> = emptyList(),
    val releaseDate: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayTitle: String
        get() = if (title.isNotBlank()) "Ep $number: $title" else "Episode $number"

    val progressPercent: Float
        get() = if (durationMs > 0) (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
