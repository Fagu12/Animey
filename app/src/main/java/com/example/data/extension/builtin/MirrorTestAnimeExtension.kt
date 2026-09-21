package com.example.data.extension.builtin

import com.example.core.result.AppResult
import com.example.domain.extension.AnimeExtension
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.ExtensionType
import com.example.domain.model.SourceType
import com.example.domain.model.TrackInfo
import com.example.domain.model.VideoSource

/**
 * Secondary built-in extension to verify multi-provider selection,
 * per-anime preferred provider overrides, and provider fallback behavior.
 */
class MirrorTestAnimeExtension : AnimeExtension {

    override val id: String = "builtin.mirror.anime"
    override val name: String = "Animey Mirror Provider"
    override val lang: String = "en"
    override val iconUrl: String = ""
    override val baseUrl: String = "https://mirror.animey.app"

    override val manifest: ExtensionManifest = ExtensionManifest(
        id = id,
        name = name,
        version = "1.0.0",
        language = lang,
        type = ExtensionType.ANIME,
        description = "High-speed secondary mirror provider for failover and multi-provider selection.",
        author = "Animey Core Team",
        isInstalled = true,
        isEnabled = true,
        isBuiltIn = true
    )

    private val delegate = TestAnimeExtension()

    override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
        return delegate.getPopularAnime(page)
    }

    override suspend fun getLatestAnime(page: Int): AppResult<List<Anime>> {
        return delegate.getLatestAnime(page)
    }

    override suspend fun searchAnime(
        query: String,
        page: Int,
        filters: Map<String, Any>
    ): AppResult<List<Anime>> {
        return delegate.searchAnime(query, page, filters)
    }

    override suspend fun getAnimeDetails(sourceAnimeId: String): AppResult<Anime> {
        return delegate.getAnimeDetails(sourceAnimeId)
    }

    override suspend fun getEpisodes(sourceAnimeId: String): AppResult<List<Episode>> {
        return delegate.getEpisodes(sourceAnimeId)
    }

    override suspend fun getEpisodeStreams(
        sourceEpisodeId: String,
        extra: Map<String, String>
    ): AppResult<List<VideoSource>> {
        // Multi-server and multi-quality streams for Mirror provider
        val streams = listOf(
            // Mirror Server A: Akamai Mirror
            VideoSource(
                id = "$sourceEpisodeId-mirror-akamai-1080p",
                providerId = id,
                serverName = "Akamai Mirror",
                url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                type = SourceType.HLS,
                quality = "1080p",
                subtitles = listOf(
                    TrackInfo(id = "sub-en", label = "English", language = "en", isDefault = true),
                    TrackInfo(id = "sub-fr", label = "Français", language = "fr")
                ),
                isDefault = true
            ),
            VideoSource(
                id = "$sourceEpisodeId-mirror-akamai-720p",
                providerId = id,
                serverName = "Akamai Mirror",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                type = SourceType.MP4,
                quality = "720p"
            ),
            VideoSource(
                id = "$sourceEpisodeId-mirror-akamai-480p",
                providerId = id,
                serverName = "Akamai Mirror",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                type = SourceType.MP4,
                quality = "480p"
            ),

            // Mirror Server B: Cloudflare Edge
            VideoSource(
                id = "$sourceEpisodeId-mirror-cf-1080p",
                providerId = id,
                serverName = "Cloudflare Edge",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                type = SourceType.MP4,
                quality = "1080p",
                subtitles = listOf(
                    TrackInfo(id = "sub-en", label = "English [CC]", language = "en", isDefault = true)
                )
            ),
            VideoSource(
                id = "$sourceEpisodeId-mirror-cf-720p",
                providerId = id,
                serverName = "Cloudflare Edge",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                type = SourceType.MP4,
                quality = "720p"
            ),
            VideoSource(
                id = "$sourceEpisodeId-mirror-cf-360p",
                providerId = id,
                serverName = "Cloudflare Edge",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                type = SourceType.MP4,
                quality = "360p"
            ),
            VideoSource(
                id = "$sourceEpisodeId-mirror-embed",
                providerId = id,
                serverName = "MirrorEmbed (Web)",
                url = "https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1",
                type = SourceType.EMBED,
                quality = "720p",
                headers = mapOf("Referer" to "https://mirror.animey.app"),
                referer = "https://mirror.animey.app"
            )
        )
        return AppResult.Success(streams)
    }

    override suspend fun getPreferences(): List<ExtensionPreference> = emptyList()

    override suspend fun setPreference(key: String, value: Any) {}
}
