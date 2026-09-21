package com.example.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Anime

@Entity(tableName = "anime")
data class AnimeEntity(
    @PrimaryKey
    val localId: String,
    val sourceId: String,
    val sourceAnimeId: String,
    val title: String,
    val altTitles: List<String> = emptyList(),
    val poster: String = "",
    val banner: String = "",
    val description: String = "",
    val genres: List<String> = emptyList(),
    val status: String = "Ongoing",
    val type: String = "TV",
    val totalEpisodes: Int? = null,
    val season: String = "",
    val year: Int? = null,
    val studio: String = "",
    val rating: Double? = null,
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomain(): Anime = Anime(
        localId = localId,
        sourceId = sourceId,
        sourceAnimeId = sourceAnimeId,
        title = title,
        alternativeTitles = altTitles,
        poster = poster,
        banner = banner,
        description = description,
        genres = genres,
        status = status,
        type = type,
        episodeCount = totalEpisodes,
        season = season,
        year = year,
        studios = if (studio.isNotBlank()) listOf(studio) else emptyList(),
        rating = rating,
        isFavorite = isFavorite
    )

    companion object {
        fun fromDomain(domain: Anime): AnimeEntity = AnimeEntity(
            localId = domain.localId,
            sourceId = domain.sourceId,
            sourceAnimeId = domain.sourceAnimeId,
            title = domain.title,
            altTitles = domain.alternativeTitles,
            poster = domain.poster,
            banner = domain.banner,
            description = domain.description,
            genres = domain.genres,
            status = domain.status,
            type = domain.type,
            totalEpisodes = domain.episodeCount,
            season = domain.season,
            year = domain.year,
            studio = domain.studios.firstOrNull() ?: "",
            rating = domain.rating,
            isFavorite = domain.isFavorite,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
