package com.example.domain.model

enum class LibraryStatus(val displayName: String) {
    WATCHING("Watching"),
    COMPLETED("Completed"),
    PLANNING("Planning"),
    PAUSED("Paused"),
    DROPPED("Dropped"),
    FAVORITES("Favorites")
}

data class LibraryEntry(
    val anime: Anime,
    val status: LibraryStatus,
    val customCategory: String = "",
    val score: Double? = null,
    val userScore: Double? = score,
    val lastWatchedEpisode: Float? = null,
    val totalEpisodesWatched: Int = 0,
    val totalWatchedEpisodes: Int = totalEpisodesWatched,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
