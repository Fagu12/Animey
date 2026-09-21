package com.example.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.TrackingBinding
import com.example.domain.model.TrackingPlatform

@Entity(
    tableName = "tracking_bindings",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["localId"],
            childColumns = ["localAnimeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("localAnimeId"),
        Index("platform", "platformAnimeId")
    ]
)
data class TrackingBindingEntity(
    @PrimaryKey
    val localAnimeId: String,
    val sourceId: String,
    val sourceAnimeId: String,
    val platform: TrackingPlatform,
    val platformAnimeId: String = "",
    val aniListId: Int,
    val season: Int = 1,
    val mappingConfidence: Float = 1.0f,
    val currentProgress: Int = 0,
    val totalEpisodes: Int? = null,
    val score: Double? = null,
    val aniListTitle: String? = null,
    val aniListCoverImage: String? = null,
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): TrackingBinding = TrackingBinding(
        localAnimeId = localAnimeId,
        sourceId = sourceId,
        sourceAnimeId = sourceAnimeId,
        platform = platform,
        aniListId = if (aniListId != 0) aniListId else (platformAnimeId.toIntOrNull() ?: 0),
        season = season,
        mappingConfidence = mappingConfidence,
        currentProgress = currentProgress,
        totalEpisodes = totalEpisodes,
        score = score,
        aniListTitle = aniListTitle,
        aniListCoverImage = aniListCoverImage,
        lastSyncedAt = lastSyncedAt
    )

    companion object {
        fun fromDomain(domain: TrackingBinding): TrackingBindingEntity = TrackingBindingEntity(
            localAnimeId = domain.localAnimeId,
            sourceId = domain.sourceId,
            sourceAnimeId = domain.sourceAnimeId,
            platform = domain.platform,
            platformAnimeId = domain.aniListId.toString(),
            aniListId = domain.aniListId,
            season = domain.season,
            mappingConfidence = domain.mappingConfidence,
            currentProgress = domain.currentProgress,
            totalEpisodes = domain.totalEpisodes,
            score = domain.score,
            aniListTitle = domain.aniListTitle,
            aniListCoverImage = domain.aniListCoverImage,
            lastSyncedAt = domain.lastSyncedAt
        )
    }
}
