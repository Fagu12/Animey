package com.example.domain.model

data class HistoryEntry(
    val anime: Anime,
    val episode: Episode,
    val positionMs: Long,
    val durationMs: Long,
    val sourceId: String,
    val watchedAt: Long = System.currentTimeMillis()
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
