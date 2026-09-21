package com.example.domain.model.tracking

enum class TrackStatus(val displayName: String, val aniListStatus: String) {
    WATCHING("Watching", "CURRENT"),
    COMPLETED("Completed", "COMPLETED"),
    PLANNING("Planning", "PLANNING"),
    PAUSED("Paused", "PAUSED"),
    DROPPED("Dropped", "DROPPED"),
    REWATCHING("Rewatching", "REPEATING");

    companion object {
        fun fromAniListStatus(status: String?): TrackStatus {
            return entries.find { it.aniListStatus.equals(status, ignoreCase = true) } ?: WATCHING
        }
    }
}

data class AniListUser(
    val id: Int,
    val name: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val unreadNotificationCount: Int = 0,
    val animeCount: Int = 0,
    val episodesWatched: Int = 0,
    val meanScore: Double = 0.0,
    val minutesWatched: Int = 0
)

data class AniListMediaEntry(
    val mediaId: Int,
    val title: String,
    val coverImage: String? = null,
    val status: TrackStatus = TrackStatus.WATCHING,
    val progress: Int = 0,
    val totalEpisodes: Int? = null,
    val score: Double = 0.0,
    val repeatCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR,
    NOT_LOGGED_IN
}
