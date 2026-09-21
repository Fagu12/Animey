package com.example.domain.model

/**
 * Normalized Domain Anime model.
 * Provider-agnostic representation for all anime metadata in the UI & database.
 */
data class Anime(
    val localId: String,
    val title: String,
    val alternativeTitles: List<String> = emptyList(),
    val description: String = "",
    val poster: String = "",
    val banner: String = "",
    val year: Int? = null,
    val season: String = "",
    val type: String = "TV", // TV, Movie, OVA, ONA, Special
    val status: String = "Ongoing", // Ongoing, Completed, Upcoming
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val rating: Double? = null,
    val episodeCount: Int? = null,
    val duration: String? = null,
    val sourceId: String = "",
    val sourceAnimeId: String = "",
    val isFavorite: Boolean = false
) {
    val altTitles: List<String>
        get() = alternativeTitles

    val totalEpisodes: Int?
        get() = episodeCount

    val studio: String
        get() = studios.firstOrNull() ?: ""

    val displayScore: String
        get() = rating?.let { String.format("%.1f", it) } ?: "N/A"
}
