package com.example.domain.model

enum class TrackingPlatform {
    ANILIST,
    MAL,
    SIMKL
}

data class TrackingBinding(
    val localAnimeId: String,
    val sourceId: String,
    val sourceAnimeId: String,
    val platform: TrackingPlatform = TrackingPlatform.ANILIST,
    val aniListId: Int,
    val season: Int = 1,
    val mappingConfidence: Float = 1.0f, // 0.0f to 1.0f (e.g. 0.95 = 95%)
    val currentProgress: Int = 0,
    val totalEpisodes: Int? = null,
    val score: Double? = null,
    val aniListTitle: String? = null,
    val aniListCoverImage: String? = null,
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    val platformAnimeId: String
        get() = aniListId.toString()
}
