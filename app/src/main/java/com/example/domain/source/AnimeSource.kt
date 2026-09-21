package com.example.domain.source

import com.example.core.result.AppResult
import com.example.domain.extension.runtime.ExtensionLifecycleState
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.VideoSource

/**
 * Unified internal interface for all anime sources (Built-in APIs, Aniyomi extensions, Mangayomi extensions).
 * Keeps UI, repositories, and player completely decoupled from runtime-specific details.
 */
interface AnimeSource {
    val id: String
    val name: String
    val lang: String
    val baseUrl: String
    val iconUrl: String
    val sourceType: SourceTypeInfo
        get() = SourceTypeInfo.MANGAYOMI_EXTENSION
    val lifecycleState: ExtensionLifecycleState
        get() = ExtensionLifecycleState.ACTIVE
    val compatibilityStatus: ExtensionCompatibilityStatus
        get() = ExtensionCompatibilityStatus.COMPATIBLE

    /**
     * Fetch popular / trending anime page.
     */
    suspend fun getPopularAnime(page: Int = 1): AppResult<List<Anime>>

    /**
     * Fetch latest updated anime page.
     */
    suspend fun getLatestAnime(page: Int = 1): AppResult<List<Anime>>

    /**
     * Search anime with query string and optional filter parameters.
     */
    suspend fun searchAnime(
        query: String,
        page: Int = 1,
        filters: Map<String, Any> = emptyMap()
    ): AppResult<List<Anime>>

    /**
     * Fetch complete anime details (synopsis, genres, score, status, banners).
     */
    suspend fun getAnimeDetails(sourceAnimeId: String): AppResult<Anime>

    /**
     * Fetch episode list for the specified anime.
     */
    suspend fun getEpisodes(sourceAnimeId: String): AppResult<List<Episode>>

    /**
     * Resolve playable video streams for the episode ID.
     */
    suspend fun getEpisodeStreams(
        sourceEpisodeId: String,
        extra: Map<String, String> = emptyMap()
    ): AppResult<List<VideoSource>> = getVideoSources(
        Episode(
            id = "ep_$sourceEpisodeId",
            animeId = "",
            sourceId = id,
            sourceEpisodeId = sourceEpisodeId,
            number = 1f,
            title = ""
        ),
        extra
    )

    /**
     * Resolve playable video streams (HLS/MP4/DASH/Embed) for the episode.
     */
    suspend fun getVideoSources(
        episode: Episode,
        extra: Map<String, String> = emptyMap()
    ): AppResult<List<VideoSource>> = getEpisodeStreams(episode.sourceEpisodeId, extra)

    /**
     * Query source-specific preferences.
     */
    suspend fun getPreferences(): List<ExtensionPreference> = emptyList()

    /**
     * Update source-specific preference value.
     */
    suspend fun setPreference(key: String, value: Any) {}
}
