package com.example.data.extension.builtin

import com.example.core.result.AppResult
import com.example.domain.extension.AnimeExtension
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.ExtensionType
import com.example.domain.model.SkipSegment
import com.example.domain.model.SkipType
import com.example.domain.model.SourceType
import com.example.domain.model.TrackInfo
import com.example.domain.model.VideoSource

class TestAnimeExtension : AnimeExtension {

    override val id: String = "builtin.test.anime"
    override val name: String = "Animey Test Stream"
    override val lang: String = "en"
    override val iconUrl: String = ""
    override val baseUrl: String = "https://mock.animey.app"

    override val manifest: ExtensionManifest = ExtensionManifest(
        id = id,
        name = name,
        version = "1.0.0",
        language = lang,
        type = ExtensionType.ANIME,
        description = "Built-in verification and test stream provider with sample catalog and valid media streams.",
        author = "Animey Team",
        isInstalled = true,
        isEnabled = true,
        isBuiltIn = true
    )

    private var selectedServer: String = "FastCDN"
    private var enable4K: Boolean = false
    private var language: String = "English"
    private var enableHlsFastStart: Boolean = true
    private var customUserAgent: String = "Animey/1.0 (Built-in Test)"
    private var maxBufferSeconds: Int = 60
    private var playbackSpeed: Double = 1.0
    private var preferredQualities: List<String> = listOf("1080p", "720p")

    private val catalog = listOf(
        Anime(
            localId = "$id:frieren-beyond-journeys-end",
            sourceId = id,
            sourceAnimeId = "frieren-beyond-journeys-end",
            title = "Frieren: Beyond Journey's End",
            alternativeTitles = listOf("Sousou no Frieren", "葬送のフリーレン"),
            description = "The adventure is over but life goes on for an elf mage just beginning to learn what living is all about. An elf mage named Frieren embarks on a journey to the land where souls rest.",
            poster = "https://cdn.myanimelist.net/images/anime/1015/138025.jpg",
            banner = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1200",
            year = 2023,
            season = "Fall",
            type = "TV",
            status = "Completed",
            genres = listOf("Adventure", "Drama", "Fantasy"),
            studios = listOf("Madhouse"),
            rating = 9.38,
            episodeCount = 28
        ),
        Anime(
            localId = "$id:solo-leveling",
            sourceId = id,
            sourceAnimeId = "solo-leveling",
            title = "Solo Leveling",
            alternativeTitles = listOf("Ore dake Level Up na Ken", "俺だけレベルアップな件"),
            description = "They say whatever doesn't kill you makes you stronger, but that's not the case for the world's weakest hunter Sung Jinwoo until a mysterious quest grants him the ability to level up infinitely.",
            poster = "https://cdn.myanimelist.net/images/anime/1841/141018.jpg",
            banner = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1200",
            year = 2024,
            season = "Winter",
            type = "TV",
            status = "Completed",
            genres = listOf("Action", "Adventure", "Fantasy"),
            studios = listOf("A-1 Pictures"),
            rating = 8.42,
            episodeCount = 12
        ),
        Anime(
            localId = "$id:jujutsu-kaisen-s2",
            sourceId = id,
            sourceAnimeId = "jujutsu-kaisen-s2",
            title = "Jujutsu Kaisen Season 2",
            alternativeTitles = listOf("JJK S2", "呪術廻戦 懐玉・玉折／渋谷事変"),
            description = "The past comes to light when Gojo Satoru and Geto Suguru take on a mission to protect the Star Plasma Vessel. Later, the Shibuya Incident begins.",
            poster = "https://cdn.myanimelist.net/images/anime/1792/138022.jpg",
            banner = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200",
            year = 2023,
            season = "Summer",
            type = "TV",
            status = "Completed",
            genres = listOf("Action", "Supernatural", "Dark Fantasy"),
            studios = listOf("MAPPA"),
            rating = 8.85,
            episodeCount = 23
        ),
        Anime(
            localId = "$id:demon-slayer-hashira-training",
            sourceId = id,
            sourceAnimeId = "demon-slayer-hashira-training",
            title = "Demon Slayer: Hashira Training Arc",
            alternativeTitles = listOf("Kimetsu no Yaiba: Hashira Geiko-hen"),
            description = "Tanjiro visits the Stone Hashira, Himejima, who intends to prepare him for the battles to come. The training to become a Hashira is rigorous and demanding.",
            poster = "https://cdn.myanimelist.net/images/anime/1167/141697.jpg",
            banner = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=1200",
            year = 2024,
            season = "Spring",
            type = "TV",
            status = "Completed",
            genres = listOf("Action", "Historical", "Supernatural"),
            studios = listOf("ufotable"),
            rating = 8.56,
            episodeCount = 8
        ),
        Anime(
            localId = "$id:chainsaw-man",
            sourceId = id,
            sourceAnimeId = "chainsaw-man",
            title = "Chainsaw Man",
            alternativeTitles = listOf("CSM", "チェンソーマン"),
            description = "Denji is a young man living as a Devil Hunter with the 'Chainsaw Devil' Pochita. When betrayed, he is reborn as the powerful Chainsaw Man.",
            poster = "https://cdn.myanimelist.net/images/anime/1806/126216.jpg",
            banner = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=1200",
            year = 2022,
            season = "Fall",
            type = "TV",
            status = "Completed",
            genres = listOf("Action", "Gore", "Supernatural"),
            studios = listOf("MAPPA"),
            rating = 8.52,
            episodeCount = 12
        ),
        Anime(
            localId = "$id:spy-x-family",
            sourceId = id,
            sourceAnimeId = "spy-x-family",
            title = "Spy x Family",
            alternativeTitles = listOf("SPYxFAMILY", "スパイファミリー"),
            description = "A spy on an undercover mission gets married and adopts a child, unaware that his wife is an assassin and his daughter is a telepath.",
            poster = "https://cdn.myanimelist.net/images/anime/1441/122795.jpg",
            banner = "https://images.unsplash.com/photo-1563089145-599997674d42?w=1200",
            year = 2022,
            season = "Spring",
            type = "TV",
            status = "Completed",
            genres = listOf("Action", "Comedy", "Slice of Life"),
            studios = listOf("Wit Studio", "CloverWorks"),
            rating = 8.55,
            episodeCount = 25
        )
    )

    override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
        val pageSize = 10
        val startIndex = (page - 1) * pageSize
        if (startIndex >= catalog.size) return AppResult.Success(emptyList())
        val endIndex = (startIndex + pageSize).coerceAtMost(catalog.size)
        return AppResult.Success(catalog.subList(startIndex, endIndex))
    }

    override suspend fun getLatestAnime(page: Int): AppResult<List<Anime>> {
        val reversed = catalog.reversed()
        val pageSize = 10
        val startIndex = (page - 1) * pageSize
        if (startIndex >= reversed.size) return AppResult.Success(emptyList())
        val endIndex = (startIndex + pageSize).coerceAtMost(reversed.size)
        return AppResult.Success(reversed.subList(startIndex, endIndex))
    }

    override suspend fun searchAnime(
        query: String,
        page: Int,
        filters: Map<String, Any>
    ): AppResult<List<Anime>> {
        val trimmed = query.trim().lowercase()
        val results = if (trimmed.isBlank()) {
            catalog
        } else {
            catalog.filter { item ->
                item.title.lowercase().contains(trimmed) ||
                item.alternativeTitles.any { it.lowercase().contains(trimmed) } ||
                item.genres.any { it.lowercase().contains(trimmed) } ||
                item.description.lowercase().contains(trimmed)
            }
        }
        val pageSize = 10
        val startIndex = (page - 1) * pageSize
        if (startIndex >= results.size) return AppResult.Success(emptyList())
        val endIndex = (startIndex + pageSize).coerceAtMost(results.size)
        return AppResult.Success(results.subList(startIndex, endIndex))
    }

    override suspend fun getAnimeDetails(sourceAnimeId: String): AppResult<Anime> {
        val found = catalog.firstOrNull { it.sourceAnimeId == sourceAnimeId }
            ?: return AppResult.Error(com.example.core.result.AppError.NotFoundError("Anime not found in test source"))
        return AppResult.Success(found)
    }

    override suspend fun getEpisodes(sourceAnimeId: String): AppResult<List<Episode>> {
        val anime = catalog.firstOrNull { it.sourceAnimeId == sourceAnimeId }
            ?: return AppResult.Error(com.example.core.result.AppError.NotFoundError("Anime not found"))

        val total = anime.episodeCount ?: 12
        val episodes = (1..total).map { epNum ->
            val epId = "$id:$sourceAnimeId:ep$epNum"
            Episode(
                id = epId,
                animeId = anime.localId,
                sourceId = id,
                sourceEpisodeId = "ep$epNum",
                number = epNum.toFloat(),
                seasonNumber = 1,
                title = "Episode $epNum: The Beginning of Journey Part $epNum",
                description = "Tanjiro and friends encounter new trials in this thrilling chapter.",
                thumbnail = anime.banner.ifBlank { anime.poster },
                durationMs = 1440000L, // 24 minutes
                skipSegments = listOf(
                    SkipSegment(
                        type = SkipType.INTRO,
                        startMs = 90000L,   // 01:30
                        endMs = 180000L     // 03:00
                    ),
                    SkipSegment(
                        type = SkipType.OUTRO,
                        startMs = 1320000L, // 22:00
                        endMs = 1410000L    // 23:30
                    )
                )
            )
        }
        return AppResult.Success(episodes)
    }

    override suspend fun getEpisodeStreams(
        sourceEpisodeId: String,
        extra: Map<String, String>
    ): AppResult<List<VideoSource>> {
        // Multi-server and multi-quality streams for testing
        val streams = listOf(
            // Server 1: FastCDN (Global)
            VideoSource(
                id = "$sourceEpisodeId-fastcdn-1080p",
                providerId = id,
                serverName = "FastCDN (Global)",
                url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                type = SourceType.HLS,
                quality = "1080p",
                subtitles = listOf(
                    TrackInfo(id = "sub-en", label = "English [CC]", language = "en", isDefault = true),
                    TrackInfo(id = "sub-es", label = "Español", language = "es"),
                    TrackInfo(id = "sub-id", label = "Indonesian", language = "id")
                ),
                isDefault = true
            ),
            VideoSource(
                id = "$sourceEpisodeId-fastcdn-720p",
                providerId = id,
                serverName = "FastCDN (Global)",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                type = SourceType.MP4,
                quality = "720p",
                subtitles = listOf(
                    TrackInfo(id = "sub-en", label = "English", language = "en", isDefault = true)
                )
            ),
            VideoSource(
                id = "$sourceEpisodeId-fastcdn-480p",
                providerId = id,
                serverName = "FastCDN (Global)",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                type = SourceType.MP4,
                quality = "480p"
            ),
            VideoSource(
                id = "$sourceEpisodeId-fastcdn-360p",
                providerId = id,
                serverName = "FastCDN (Global)",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                type = SourceType.MP4,
                quality = "360p"
            ),

            // Server 2: Mirror 1 (Tokyo)
            VideoSource(
                id = "$sourceEpisodeId-tokyo-1080p",
                providerId = id,
                serverName = "Mirror 1 (Tokyo)",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                type = SourceType.MP4,
                quality = "1080p",
                subtitles = listOf(
                    TrackInfo(id = "sub-en", label = "English", language = "en", isDefault = true),
                    TrackInfo(id = "sub-ja", label = "Japanese", language = "ja")
                )
            ),
            VideoSource(
                id = "$sourceEpisodeId-tokyo-720p",
                providerId = id,
                serverName = "Mirror 1 (Tokyo)",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                type = SourceType.MP4,
                quality = "720p"
            ),

            // Server 3: Backup Cloud (Progressive File / MP4 with Referer and Headers)
            VideoSource(
                id = "$sourceEpisodeId-backup-720p",
                providerId = id,
                serverName = "Backup Cloud",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                type = SourceType.FILE,
                quality = "720p",
                headers = mapOf("X-Animey-Token" to "test-token-backup", "Accept" to "video/*"),
                referer = "https://backup.cloud.animey.app"
            ),
            VideoSource(
                id = "$sourceEpisodeId-backup-480p",
                providerId = id,
                serverName = "Backup Cloud",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
                type = SourceType.MP4,
                quality = "480p",
                headers = mapOf("X-Animey-Token" to "test-token-backup"),
                referer = "https://backup.cloud.animey.app"
            ),
            VideoSource(
                id = "$sourceEpisodeId-backup-360p",
                providerId = id,
                serverName = "Backup Cloud",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                type = SourceType.MP4,
                quality = "360p"
            ),

            // Server 4: Web Embed Player (EMBED - Never sent to ExoPlayer)
            VideoSource(
                id = "$sourceEpisodeId-embed-web",
                providerId = id,
                serverName = "StreamEmbed (Web)",
                url = "https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1",
                type = SourceType.EMBED,
                quality = "1080p",
                headers = mapOf("User-Agent" to "Mozilla/5.0 AnimeyEmbed/1.0"),
                referer = "https://embed.animey.app"
            )
        )
        val sortedStreams = if (selectedServer.isNotBlank()) {
            streams.sortedByDescending { it.serverName.contains(selectedServer, ignoreCase = true) }
        } else {
            streams
        }
        return AppResult.Success(sortedStreams)
    }

    override suspend fun getPreferences(): List<ExtensionPreference> {
        return listOf(
            ExtensionPreference.SelectPreference(
                key = "language",
                title = "Language",
                summary = "Preferred audio/content language",
                options = listOf("English", "Japanese", "Spanish", "German"),
                defaultValue = "English",
                value = language
            ),
            ExtensionPreference.BooleanPreference(
                key = "enable_4k",
                title = "Enable 4K Ultra HD Streams",
                summary = "Enable experimental 4K streams when available",
                defaultValue = false,
                value = enable4K
            ),
            ExtensionPreference.BooleanPreference(
                key = "enable_hls_fast_start",
                title = "Fast Start HLS",
                summary = "Pre-buffer media chunks for instant playback",
                defaultValue = true,
                value = enableHlsFastStart
            ),
            ExtensionPreference.StringPreference(
                key = "custom_user_agent",
                title = "Custom User-Agent",
                summary = "HTTP client header override",
                defaultValue = "Animey/1.0 (Built-in Test)",
                value = customUserAgent
            ),
            ExtensionPreference.IntegerPreference(
                key = "max_buffer_seconds",
                title = "Max Buffer Duration (seconds)",
                summary = "Video buffer capacity in seconds",
                defaultValue = 60,
                value = maxBufferSeconds
            ),
            ExtensionPreference.NumberPreference(
                key = "playback_speed",
                title = "Preferred Playback Speed",
                summary = "Default playback rate multiplier",
                defaultValue = 1.0,
                value = playbackSpeed
            ),
            ExtensionPreference.SelectPreference(
                key = "server_select",
                title = "Primary Streaming Server",
                summary = "Select primary content delivery node",
                options = listOf("FastCDN", "Mirror 1", "Mirror 2"),
                defaultValue = "FastCDN",
                value = selectedServer
            ),
            ExtensionPreference.MultiSelectPreference(
                key = "preferred_qualities",
                title = "Preferred Video Qualities",
                summary = "Qualities allowed during auto-selection",
                options = listOf("1080p", "720p", "480p", "360p"),
                defaultValue = listOf("1080p", "720p"),
                value = preferredQualities
            )
        )
    }

    override suspend fun setPreference(key: String, value: Any) {
        when (key) {
            "language" -> language = value.toString()
            "enable_4k" -> enable4K = (value as? Boolean) ?: value.toString().toBooleanStrictOrNull() ?: false
            "enable_hls_fast_start" -> enableHlsFastStart = (value as? Boolean) ?: value.toString().toBooleanStrictOrNull() ?: true
            "custom_user_agent" -> customUserAgent = value.toString()
            "max_buffer_seconds" -> maxBufferSeconds = (value as? Number)?.toInt() ?: value.toString().toIntOrNull() ?: 60
            "playback_speed" -> playbackSpeed = (value as? Number)?.toDouble() ?: value.toString().toDoubleOrNull() ?: 1.0
            "server_select" -> selectedServer = value.toString()
            "preferred_qualities" -> {
                preferredQualities = when (value) {
                    is List<*> -> value.mapNotNull { it?.toString() }
                    is Set<*> -> value.mapNotNull { it?.toString() }
                    else -> value.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
            }
        }
    }
}
