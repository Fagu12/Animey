package com.example.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Episode
import com.example.domain.model.SkipSegment

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["localId"],
            childColumns = ["animeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("animeId"),
        Index("sourceId", "sourceEpisodeId")
    ]
)
data class EpisodeEntity(
    @PrimaryKey
    val id: String,
    val animeId: String,
    val sourceId: String,
    val sourceEpisodeId: String,
    val number: Float,
    val title: String = "",
    val thumbnail: String = "",
    val description: String = "",
    val durationMs: Long = 0L,
    val lastPositionMs: Long = 0L,
    val isWatched: Boolean = false,
    val isDownloaded: Boolean = false,
    val seasonNumber: Int = 1,
    val skipSegments: List<SkipSegment> = emptyList(),
    val releaseDate: String = ""
) {
    fun toDomain(): Episode = Episode(
        id = id,
        animeId = animeId,
        sourceId = sourceId,
        sourceEpisodeId = sourceEpisodeId,
        number = number,
        title = title,
        thumbnail = thumbnail,
        description = description,
        durationMs = durationMs,
        lastPositionMs = lastPositionMs,
        isWatched = isWatched,
        isDownloaded = isDownloaded,
        seasonNumber = seasonNumber,
        skipSegments = skipSegments,
        releaseDate = releaseDate
    )

    companion object {
        fun fromDomain(domain: Episode): EpisodeEntity = EpisodeEntity(
            id = domain.id,
            animeId = domain.animeId,
            sourceId = domain.sourceId,
            sourceEpisodeId = domain.sourceEpisodeId,
            number = domain.number,
            title = domain.title,
            thumbnail = domain.thumbnail,
            description = domain.description,
            durationMs = domain.durationMs,
            lastPositionMs = domain.lastPositionMs,
            isWatched = domain.isWatched,
            isDownloaded = domain.isDownloaded,
            seasonNumber = domain.seasonNumber,
            skipSegments = domain.skipSegments,
            releaseDate = domain.releaseDate
        )
    }
}
