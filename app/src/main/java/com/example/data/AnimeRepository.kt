package com.example.data

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.model.AnimeExtension
import com.example.model.AnimeItem
import com.example.model.ContinueWatchingItem
import com.example.model.EpisodeItem
import com.example.model.LibraryFilter
import com.example.model.StreamServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AnimeRepository {

    val defaultSeedColor = Color(0xFF8B75FF)

    val curatedFeaturedAnime: List<AnimeItem> = listOf(
        AnimeItem(
            id = "dandadan",
            title = "DAN DA DAN",
            japaneseTitle = "ダンダダン",
            synopsis = "This is a story about Momo, a high school girl who comes from a family of spirit mediums, and her classmate Okarun, an occult fanatic. Together they face overwhelming paranormal entities and chaotic alien battles.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1584/143719l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1584/143719l.jpg",
            score = 8.4,
            studio = "Science SARU",
            status = "AIRING",
            releaseYear = 2024,
            currentEpisode = 12,
            totalEpisodes = 12,
            genres = listOf("Action", "Supernatural", "Sci-Fi"),
            seedColor = Color(0xFF9E86FF),
            libraryStatus = LibraryFilter.WATCHING,
            episodes = listOf(
                EpisodeItem(1, "That's How Love Starts, Ya Know!", "24:00", "2024-10-04", 1.0f),
                EpisodeItem(2, "That's a Space Alien, Ain't It?!", "24:00", "2024-10-11", 1.0f),
                EpisodeItem(3, "Granny vs. Granny Clash!", "24:00", "2024-10-18", 1.0f),
                EpisodeItem(4, "Kicking Turbo Granny", "24:00", "2024-10-25", 0.65f),
                EpisodeItem(5, "Like, Where Are Your Balls?!", "24:00", "2024-11-01", 0.0f),
                EpisodeItem(6, "A Dangerous Woman Arrives", "24:00", "2024-11-08", 0.0f)
            )
        ),
        AnimeItem(
            id = "solo_leveling",
            title = "Solo Leveling",
            japaneseTitle = "俺だけレベルアップな件",
            synopsis = "In a world where hunters must battle deadly monsters to protect humanity, Sung Jinwoo—known as the weakest hunter of all mankind—awakens a secret leveling system.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1801/142390l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1801/142390l.jpg",
            score = 8.6,
            studio = "A-1 Pictures",
            status = "FINISHED",
            releaseYear = 2024,
            currentEpisode = 8,
            totalEpisodes = 12,
            genres = listOf("Action", "Fantasy", "Adventure"),
            seedColor = Color(0xFF56B4FC),
            libraryStatus = LibraryFilter.WATCHING,
            episodes = listOf(
                EpisodeItem(1, "I'm Used to It", "23:40", "2024-01-07", 1.0f),
                EpisodeItem(2, "If I Had One More Chance", "23:40", "2024-01-14", 1.0f),
                EpisodeItem(7, "Let's See How Far I Can Go", "23:40", "2024-02-18", 1.0f),
                EpisodeItem(8, "This Is Frustrating", "23:40", "2024-03-03", 0.68f),
                EpisodeItem(9, "You've Been Hiding Your Skills", "23:40", "2024-03-10", 0.0f),
                EpisodeItem(10, "What Is This, a Picnic?", "23:40", "2024-03-17", 0.0f)
            )
        ),
        AnimeItem(
            id = "bleach_tybw",
            title = "BLEACH: Thousand-Year Blood War - The Calamity",
            japaneseTitle = "BLEACH 千年血戦篇-相剋譚-",
            synopsis = "Ichigo Kurosaki and the Soul Reapers enter the final confrontation against Yhwach and the Schutzstaffel across the shattered realms of Wahrwelt.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1908/135431l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1908/135431l.jpg",
            score = 8.9,
            studio = "Pierrot Films",
            status = "AIRING",
            releaseYear = 2024,
            currentEpisode = 7,
            totalEpisodes = 14,
            genres = listOf("Action", "Supernatural"),
            seedColor = Color(0xFFF46B84),
            libraryStatus = LibraryFilter.WATCHING,
            episodes = listOf(
                EpisodeItem(6, "The Holy Newborn", "24:10", "2024-11-09", 1.0f),
                EpisodeItem(7, "Gate of the Sun", "24:10", "2024-11-16", 1.0f),
                EpisodeItem(8, "Baby, Hold Your Hand", "24:10", "2024-11-23", 0.25f)
            )
        ),
        AnimeItem(
            id = "mushoku_tensei_s3",
            title = "Mushoku Tensei: Jobless Reincarnation Season 3",
            japaneseTitle = "無職転生 ～異世界行ったら本気だす～",
            synopsis = "The third season of Mushoku Tensei: Isekai Ittara Honki Dasu following Rudeus Greyrat as monumental fates converge.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1895/141256l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1895/141256l.jpg",
            score = 8.4,
            studio = "Studio Bind",
            status = "UPCOMING",
            releaseYear = 2026,
            currentEpisode = 1,
            totalEpisodes = 24,
            genres = listOf("Drama", "Fantasy", "Adventure"),
            seedColor = Color(0xFFE9965B),
            libraryStatus = LibraryFilter.PLANNING,
            episodes = listOf(
                EpisodeItem(1, "Turning Point 4", "24:00", "2026-04-12", 0.0f)
            )
        ),
        AnimeItem(
            id = "frieren",
            title = "Frieren: Beyond Journey's End",
            japaneseTitle = "葬送のフリーレン",
            synopsis = "After a ten-year quest to defeat the Demon King, the elven mage Frieren embarks on a new journey to understand the hearts of humans.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1015/138006l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1015/138006l.jpg",
            score = 9.3,
            studio = "Madhouse",
            status = "FINISHED",
            releaseYear = 2023,
            currentEpisode = 28,
            totalEpisodes = 28,
            genres = listOf("Adventure", "Drama", "Fantasy"),
            seedColor = Color(0xFF4ADE80),
            libraryStatus = LibraryFilter.COMPLETED,
            episodes = listOf(
                EpisodeItem(1, "The Journey's End", "24:00", "2023-09-29", 1.0f),
                EpisodeItem(28, "It Would Be Embarrassing When We Meet Again", "24:00", "2024-03-22", 1.0f)
            )
        )
    )

    val curatedTrendingAnime: List<AnimeItem> = listOf(
        curatedFeaturedAnime[0],
        curatedFeaturedAnime[1],
        curatedFeaturedAnime[2],
        AnimeItem(
            id = "demon_slayer",
            title = "Demon Slayer: Kimetsu no Yaiba",
            japaneseTitle = "鬼滅の刃",
            synopsis = "Tanjiro Kamado joins the Demon Slayer Corps to cure his sister Nezuko and avenge his family.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1286/99889l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1286/99889l.jpg",
            score = 8.5,
            studio = "ufotable",
            status = "FINISHED",
            releaseYear = 2024,
            currentEpisode = 3,
            totalEpisodes = 26,
            genres = listOf("Action", "Supernatural"),
            seedColor = Color(0xFF38BDF8),
            libraryStatus = LibraryFilter.WATCHING,
            episodes = listOf(
                EpisodeItem(1, "Cruelty", "23:40", "2025-03-01", 1.0f),
                EpisodeItem(2, "Trainer Sakonji Urokodaki", "23:40", "2025-03-06", 1.0f),
                EpisodeItem(3, "Sabito and Makomo", "23:40", "2025-03-13", 0.34f)
            )
        ),
        AnimeItem(
            id = "one_piece",
            title = "ONE PIECE",
            japaneseTitle = "ワンピース",
            synopsis = "Monkey D. Luffy and the Straw Hat Pirates set sail for Egghead Island in search of the ultimate treasure.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1244/138851l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1244/138851l.jpg",
            score = 8.7,
            studio = "Toei Animation",
            status = "AIRING",
            releaseYear = 2024,
            currentEpisode = 1122,
            totalEpisodes = 1122,
            genres = listOf("Action", "Adventure", "Fantasy"),
            seedColor = Color(0xFFFBBF24),
            libraryStatus = LibraryFilter.WATCHING
        ),
        AnimeItem(
            id = "jujutsu_kaisen",
            title = "Jujutsu Kaisen Season 2",
            japaneseTitle = "呪術廻戦 懐玉・玉折／渋谷事変",
            synopsis = "Satoru Gojo's past and the catastrophic Shibuya Incident shake jujutsu society to its core.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1792/138022l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1792/138022l.jpg",
            score = 8.8,
            studio = "MAPPA",
            status = "FINISHED",
            releaseYear = 2023,
            currentEpisode = 23,
            totalEpisodes = 23,
            genres = listOf("Action", "Supernatural"),
            seedColor = Color(0xFFC084FC),
            libraryStatus = LibraryFilter.COMPLETED
        )
    )

    val curatedRecentlyUpdated: List<AnimeItem> = listOf(
        AnimeItem(
            id = "re_zero_s3",
            title = "Re:ZERO -Starting Life in Another World- Season 3",
            japaneseTitle = "Re:ゼロから始める異世界生活",
            synopsis = "Subaru Natsuki and Emilia face the Sin Archbishops of the Witch Cult in the watergate city of Priestella.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1438/142038l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1438/142038l.jpg",
            score = 8.7,
            studio = "White Fox",
            status = "AIRING",
            releaseYear = 2024,
            currentEpisode = 8,
            totalEpisodes = 16,
            genres = listOf("Drama", "Fantasy", "Suspense"),
            seedColor = Color(0xFFA78BFA),
            libraryStatus = LibraryFilter.WATCHING
        ),
        AnimeItem(
            id = "tokyo_revengers",
            title = "Tokyo Revengers",
            japaneseTitle = "東京リベンジャーズ",
            synopsis = "Takemichi Hanagaki leaps through time to save his friends and rewrite the destiny of the Tokyo Manji Gang.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1839/122012l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1839/122012l.jpg",
            score = 7.7,
            studio = "LIDENFILMS",
            status = "FINISHED",
            releaseYear = 2023,
            currentEpisode = 1,
            totalEpisodes = 13,
            genres = listOf("Action", "Drama"),
            seedColor = Color(0xFFFB7185),
            libraryStatus = LibraryFilter.WATCHING
        ),
        AnimeItem(
            id = "kaiju_no_8",
            title = "Kaiju No. 8",
            japaneseTitle = "怪獣8号",
            synopsis = "Kafka Hibino gains the ability to transform into a humanoid Kaiju while striving to join the Defense Force.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1370/140362l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1370/140362l.jpg",
            score = 8.3,
            studio = "Production I.G",
            status = "FINISHED",
            releaseYear = 2024,
            currentEpisode = 12,
            totalEpisodes = 12,
            genres = listOf("Action", "Sci-Fi"),
            seedColor = Color(0xFF2DD4BF),
            libraryStatus = LibraryFilter.COMPLETED
        ),
        AnimeItem(
            id = "chainsaw_man",
            title = "Chainsaw Man",
            japaneseTitle = "チェンソーマン",
            synopsis = "Denji merges with his chainsaw devil companion Pochita and becomes a Public Safety Devil Hunter.",
            posterUrl = "https://cdn.myanimelist.net/images/anime/1806/126216l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1806/126216l.jpg",
            score = 8.5,
            studio = "MAPPA",
            status = "FINISHED",
            releaseYear = 2022,
            currentEpisode = 12,
            totalEpisodes = 12,
            genres = listOf("Action", "Supernatural"),
            seedColor = Color(0xFFFB923C),
            libraryStatus = LibraryFilter.PLANNING
        )
    )

    val initialContinueWatching: List<ContinueWatchingItem> = listOf(
        ContinueWatchingItem(
            id = "cw_demon_slayer",
            animeId = "demon_slayer",
            animeTitle = "Demon Slayer: Kimetsu no Yaiba",
            episodeTitle = "Sabito and Makomo",
            episodeNumber = 3,
            posterUrl = "https://cdn.myanimelist.net/images/anime/1286/99889l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1286/99889l.jpg",
            updatedDate = "2025-03-13",
            remainingTimeText = "19:37 left",
            progress = 0.28f,
            seedColor = Color(0xFF8B75FF)
        ),
        ContinueWatchingItem(
            id = "cw_solo_leveling",
            animeId = "solo_leveling",
            animeTitle = "Solo Leveling",
            episodeTitle = "This Is Frustrating",
            episodeNumber = 8,
            posterUrl = "https://cdn.myanimelist.net/images/anime/1801/142390l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1801/142390l.jpg",
            updatedDate = "2025-03-15",
            remainingTimeText = "07:45 left",
            progress = 0.68f,
            seedColor = Color(0xFF56B4FC)
        ),
        ContinueWatchingItem(
            id = "cw_bleach",
            animeId = "bleach_tybw",
            animeTitle = "BLEACH: Thousand-Year Blood War",
            episodeTitle = "The Calamity",
            episodeNumber = 8,
            posterUrl = "https://cdn.myanimelist.net/images/anime/1908/135431l.jpg",
            bannerUrl = "https://cdn.myanimelist.net/images/anime/1908/135431l.jpg",
            updatedDate = "2025-03-16",
            remainingTimeText = "04:12 left",
            progress = 0.82f,
            seedColor = Color(0xFFF46B84)
        )
    )

    val streamServers: List<StreamServer> = listOf(
        StreamServer("srv_1", "ANIMEGG 1080P [SUB]", "1080p", "SUB", 24),
        StreamServer("srv_2", "ANIMEGG 720P [SUB]", "720p", "SUB", 19),
        StreamServer("srv_3", "ANIMEGG 480P [SUB]", "480p", "SUB", 16),
        StreamServer("srv_4", "ANIMEGG 1080P [DUB]", "1080p", "DUB", 28)
    )

    val animeExtensions: List<AnimeExtension> = listOf(
        AnimeExtension("ext_hianime", "HiAnime", "V14.12", "ENGLISH", "Aniyomi", Color(0xFF9E86FF), true),
        AnimeExtension("ext_anikoto", "Anikoto", "V14.6", "ENGLISH", "Aniyomi", Color(0xFF38BDF8), true),
        AnimeExtension("ext_anineko", "AniNeko", "V16.4", "ENGLISH", "Aniyomi", Color(0xFFC084FC), true),
        AnimeExtension("ext_animepahe", "AnimePahe", "V16.54", "ENGLISH", "Aniyomi", Color(0xFFF43F5E), true),
        AnimeExtension("ext_123anime", "123Anime", "V14.2", "ENGLISH", "Cloudstream", Color(0xFFA855F7), true)
    )

    suspend fun fetchLiveTopAnime(): List<AnimeItem> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.jikan.moe/v4/top/anime?filter=airing&limit=8")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val dataArray = root.optJSONArray("data") ?: return@withContext emptyList()
                val parsed = mutableListOf<AnimeItem>()
                val paletteColors = listOf(
                    Color(0xFF9E86FF),
                    Color(0xFF56B4FC),
                    Color(0xFFF46B84),
                    Color(0xFF4ADE80),
                    Color(0xFFFBBF24),
                    Color(0xFFC084FC)
                )
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.optJSONObject(i) ?: continue
                    val malId = item.optInt("mal_id", i + 100).toString()
                    val title = item.optString("title_english").takeIf { it.isNotBlank() && it != "null" }
                        ?: item.optString("title", "Anime Title")
                    val japanese = item.optString("title_japanese", "")
                    val synopsis = item.optString("synopsis", "Streaming now in HD simulcast.")
                    val score = item.optDouble("score", 8.4).let { if (it.isNaN()) 8.4 else it }
                    val episodesCount = item.optInt("episodes", 12).let { if (it <= 0) 12 else it }
                    val year = item.optInt("year", 2025).let { if (it <= 0) 2025 else it }
                    val images = item.optJSONObject("images")?.optJSONObject("jpg")
                    val largeImageUrl = images?.optString("large_image_url").orEmpty()
                    if (largeImageUrl.isNotBlank()) {
                        parsed.add(
                            AnimeItem(
                                id = "mal_$malId",
                                title = title,
                                japaneseTitle = japanese,
                                synopsis = synopsis,
                                posterUrl = largeImageUrl,
                                bannerUrl = largeImageUrl,
                                score = score,
                                studio = "Simulcast HD",
                                status = "AIRING",
                                releaseYear = year,
                                currentEpisode = episodesCount,
                                totalEpisodes = episodesCount,
                                genres = listOf("Anime", "Simulcast"),
                                seedColor = paletteColors[i % paletteColors.size]
                            )
                        )
                    }
                }
                parsed
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun extractSeedColorFromBitmap(bitmap: Bitmap): Color? {
        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, false)
            var bestColorInt: Int? = null
            var bestScore = -1f
            val hsl = FloatArray(3)

            for (y in 0 until scaled.height) {
                for (x in 0 until scaled.width) {
                    val pixel = scaled.getPixel(x, y)
                    ColorUtils.colorToHSL(pixel, hsl)
                    val saturation = hsl[1]
                    val lightness = hsl[2]
                    if (lightness in 0.22f..0.82f && saturation > 0.25f) {
                        val vibrancyScore = saturation * (1f - kotlin.math.abs(lightness - 0.55f))
                        if (vibrancyScore > bestScore) {
                            bestScore = vibrancyScore
                            bestColorInt = pixel
                        }
                    }
                }
            }

            bestColorInt?.let { rawColor ->
                val outHsl = FloatArray(3)
                ColorUtils.colorToHSL(rawColor, outHsl)
                outHsl[1] = outHsl[1].coerceIn(0.55f, 0.92f)
                outHsl[2] = outHsl[2].coerceIn(0.62f, 0.76f)
                Color(ColorUtils.HSLToColor(outHsl))
            }
        } catch (_: Exception) {
            null
        }
    }
}
