package com.example.data.source.mapping

import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.SourceType
import com.example.domain.model.TrackInfo
import com.example.domain.model.VideoSource
import java.util.UUID

/**
 * Normalization layer converting raw source responses (Aniyomi, Mangayomi, API) into domain models.
 */
object AnimeSourceMapper {

    /**
     * Map raw metadata to Domain Anime model.
     */
    fun mapRawToAnime(
        sourceId: String,
        sourceAnimeId: String,
        title: String,
        description: String = "",
        poster: String = "",
        banner: String = "",
        statusString: String? = null,
        genres: List<String> = emptyList(),
        rating: Double? = null,
        episodeCount: Int? = null,
        year: Int? = null,
        type: String = "TV"
    ): Anime {
        val resolvedStatus = when (statusString?.lowercase()?.trim()) {
            "ongoing", "airing", "releasing", "1" -> "Ongoing"
            "completed", "finished", "ended", "2" -> "Completed"
            "upcoming", "not yet aired", "3" -> "Upcoming"
            else -> statusString?.ifBlank { "Ongoing" } ?: "Ongoing"
        }

        return Anime(
            localId = "anime_${sourceId}_${sourceAnimeId.hashCode()}",
            title = title.ifBlank { "Unknown Anime" },
            description = description,
            poster = poster,
            banner = banner.ifBlank { poster },
            status = resolvedStatus,
            genres = genres,
            episodeCount = episodeCount,
            rating = rating,
            year = year,
            type = type,
            sourceId = sourceId,
            sourceAnimeId = sourceAnimeId
        )
    }

    /**
     * Map raw episode metadata to Domain Episode model.
     */
    fun mapRawToEpisode(
        sourceId: String,
        animeId: String,
        sourceEpisodeId: String,
        episodeNumber: Float,
        seasonNumber: Int = 1,
        title: String = "",
        thumbnail: String = "",
        durationMs: Long = 0L,
        releaseDate: String = ""
    ): Episode {
        return Episode(
            id = "ep_${sourceId}_${animeId.hashCode()}_${episodeNumber}",
            animeId = animeId,
            sourceId = sourceId,
            sourceEpisodeId = sourceEpisodeId,
            number = episodeNumber,
            seasonNumber = seasonNumber,
            title = title,
            thumbnail = thumbnail,
            durationMs = durationMs,
            releaseDate = releaseDate
        )
    }

    /**
     * Map raw stream metadata to Domain VideoSource model.
     */
    fun mapRawToVideoSource(
        providerId: String,
        serverName: String,
        url: String,
        quality: String = "Auto",
        type: SourceType? = null,
        headers: Map<String, String> = emptyMap(),
        referer: String? = null,
        subtitles: List<TrackInfo> = emptyList(),
        audioTracks: List<TrackInfo> = emptyList(),
        isDefault: Boolean = false
    ): VideoSource {
        val resolvedType = type ?: SourceType.fromUrlOrMime(url)
        val finalHeaders = headers.toMutableMap()
        if (referer != null && !finalHeaders.containsKey("Referer")) {
            finalHeaders["Referer"] = referer
        }

        return VideoSource(
            id = "src_${providerId}_${serverName.hashCode()}_${UUID.randomUUID().toString().take(6)}",
            providerId = providerId,
            serverName = serverName.ifBlank { "Default Server" },
            url = url,
            type = resolvedType,
            quality = quality.ifBlank { "Auto" },
            headers = finalHeaders,
            referer = referer ?: finalHeaders["Referer"],
            subtitles = subtitles,
            audioTracks = audioTracks,
            isDefault = isDefault
        )
    }
}
