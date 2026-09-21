package com.example.data.source.adapter

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.extension.mangayomi.runtime.MangayomiExecutionContext
import com.example.data.source.mapping.AnimeSourceMapper
import com.example.domain.extension.runtime.ExtensionLifecycleState
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.SourceType
import com.example.domain.model.TrackInfo
import com.example.domain.model.VideoSource
import com.example.domain.extension.AnimeExtension
import com.example.domain.model.ExtensionManifest
import com.example.domain.source.AnimeSource
import com.example.domain.source.ExtensionCompatibilityStatus
import com.example.domain.source.SourceTypeInfo

/**
 * Adapter executing Mangayomi JavaScript-based Anime extensions in real sandboxed contexts.
 */
class MangayomiSourceAdapter(
    override val id: String,
    override val name: String,
    override val lang: String,
    override val baseUrl: String,
    override val iconUrl: String = "",
    override val lifecycleState: ExtensionLifecycleState = ExtensionLifecycleState.INSTALLED,
    override val compatibilityStatus: ExtensionCompatibilityStatus = ExtensionCompatibilityStatus.COMPATIBLE,
    val itemType: Int = 1, // 1 = Anime
    val executionContext: MangayomiExecutionContext? = null,
    val manifestOverride: ExtensionManifest? = null
) : AnimeSource, AnimeExtension {

    override val sourceType: SourceTypeInfo = SourceTypeInfo.MANGAYOMI_EXTENSION

    override val manifest: ExtensionManifest
        get() = manifestOverride ?: ExtensionManifest(
            id = id,
            name = name,
            version = executionContext?.manifest?.version ?: "1.0.0",
            versionCode = executionContext?.manifest?.versionCode ?: 1,
            language = lang,
            baseUrl = baseUrl,
            iconUrl = iconUrl,
            isInstalled = true,
            isEnabled = true,
            sourceCodeUrl = executionContext?.manifest?.sourceCodeUrl ?: ""
        )

    override suspend fun getEpisodeStreams(
        sourceEpisodeId: String,
        extra: Map<String, String>
    ): AppResult<List<VideoSource>> {
        val dummyEpisode = Episode(
            id = "ep_${id}_${sourceEpisodeId.hashCode()}",
            animeId = "",
            sourceId = id,
            sourceEpisodeId = sourceEpisodeId,
            number = 1f,
            title = ""
        )
        return getVideoSources(dummyEpisode, extra)
    }

    override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
        val ctx = executionContext ?: return AppResult.Error(
            AppError.ExtensionError(id, "Mangayomi execution context is not loaded for extension $id")
        )
        return try {
            val rawList = ctx.getPopular(page)
            val animeList = rawList.mapNotNull { mapRawItemToAnime(it) }
            AppResult.Success(animeList)
        } catch (e: Exception) {
            AppResult.Error(AppError.ExtensionError(id, "Mangayomi getPopular failed: ${e.message}", e))
        }
    }

    override suspend fun getLatestAnime(page: Int): AppResult<List<Anime>> {
        val ctx = executionContext ?: return AppResult.Error(
            AppError.ExtensionError(id, "Mangayomi execution context is not loaded for extension $id")
        )
        return try {
            val rawList = ctx.getLatestUpdates(page)
            val animeList = rawList.mapNotNull { mapRawItemToAnime(it) }
            AppResult.Success(animeList)
        } catch (e: Exception) {
            AppResult.Error(AppError.ExtensionError(id, "Mangayomi getLatestUpdates failed: ${e.message}", e))
        }
    }

    override suspend fun searchAnime(
        query: String,
        page: Int,
        filters: Map<String, Any>
    ): AppResult<List<Anime>> {
        val ctx = executionContext ?: return AppResult.Error(
            AppError.ExtensionError(id, "Mangayomi execution context is not loaded for extension $id")
        )
        return try {
            val rawList = ctx.search(query, page, filters)
            val animeList = rawList.mapNotNull { mapRawItemToAnime(it) }
            AppResult.Success(animeList)
        } catch (e: Exception) {
            AppResult.Error(AppError.ExtensionError(id, "Mangayomi search failed: ${e.message}", e))
        }
    }

    override suspend fun getAnimeDetails(sourceAnimeId: String): AppResult<Anime> {
        val ctx = executionContext ?: return AppResult.Error(
            AppError.ExtensionError(id, "Mangayomi execution context is not loaded for extension $id")
        )
        AppLogger.i("Mangayomi", "[MANGAYOMI][DETAIL][REQUEST] source=$id url=$sourceAnimeId")
        return try {
            val detailMap = ctx.getDetail(sourceAnimeId)
            AppLogger.i("Mangayomi", "[MANGAYOMI][DETAIL][RAW] $detailMap")
            val mapped = mapRawDetailToAnime(sourceAnimeId, detailMap)
            if (mapped != null) {
                AppLogger.i(
                    "Mangayomi",
                    "[MANGAYOMI][DETAIL][NORMALIZED] title=${mapped.title} imageUrl=${mapped.poster} description=${mapped.description.take(50)}... genres=${mapped.genres} status=${mapped.status}"
                )
                if (mapped.poster.isNotBlank()) {
                    AppLogger.i("Mangayomi", "[IMAGE] url=${mapped.poster} loadStarted=true")
                }
                AppResult.Success(mapped)
            } else {
                AppResult.Error(AppError.ExtensionError(id, "Invalid or empty anime detail payload returned by Mangayomi extension"))
            }
        } catch (e: Exception) {
            AppLogger.e("Mangayomi", "[MANGAYOMI][DETAIL][ERROR] $id getDetail failed: ${e.message}", e)
            AppResult.Error(AppError.ExtensionError(id, "Mangayomi getDetail failed: ${e.message}", e))
        }
    }

    override suspend fun getEpisodes(sourceAnimeId: String): AppResult<List<Episode>> {
        val ctx = executionContext ?: return AppResult.Error(
            AppError.ExtensionError(id, "Mangayomi execution context is not loaded for extension $id")
        )
        return try {
            val detailMap = ctx.getDetail(sourceAnimeId)
            val rawEpisodes = detailMap["episodes"]
                ?: detailMap["chapters"]
                ?: detailMap["episodeList"]
                ?: detailMap["chapterList"]
            val list = mutableListOf<Episode>()
            val effectiveAnimeId = "anime_${id}_${sourceAnimeId.hashCode()}"

            if (rawEpisodes is List<*>) {
                rawEpisodes.filterIsInstance<Map<String, Any?>>().forEachIndexed { index, epMap ->
                    val epTitle = epMap["name"]?.toString()
                        ?: epMap["title"]?.toString()
                        ?: epMap["nameEpisode"]?.toString()
                        ?: ""

                    var epNumber = epMap["number"]?.toString()?.toFloatOrNull()
                        ?: epMap["episodeNumber"]?.toString()?.toFloatOrNull()
                        ?: epMap["chapterNumber"]?.toString()?.toFloatOrNull()

                    if (epNumber == null && epTitle.isNotBlank()) {
                        epNumber = extractEpisodeNumberFromTitle(epTitle)
                    }
                    val finalEpNumber = epNumber ?: (rawEpisodes.size - index).toFloat()

                    val finalTitle = epTitle.ifBlank { "Episode ${finalEpNumber.toInt()}" }
                    val epUrl = epMap["url"]?.toString()
                        ?: epMap["link"]?.toString()
                        ?: epMap["href"]?.toString()
                        ?: ""

                    val uploadDate = epMap["dateUpload"]?.toString()
                        ?: epMap["releaseDate"]?.toString()
                        ?: epMap["date"]?.toString()
                        ?: ""

                    val scanlator = epMap["scanlator"]?.toString() ?: ""

                    if (epUrl.isNotBlank()) {
                        list.add(
                            AnimeSourceMapper.mapRawToEpisode(
                                sourceId = id,
                                animeId = effectiveAnimeId,
                                sourceEpisodeId = epUrl,
                                episodeNumber = finalEpNumber,
                                seasonNumber = 1,
                                title = if (scanlator.isNotBlank()) "$finalTitle [$scanlator]" else finalTitle,
                                releaseDate = uploadDate
                            )
                        )
                    }
                }
            }
            AppLogger.i("Mangayomi", "[MANGAYOMI][DETAIL][NORMALIZED] episodesCount=${list.size}")
            AppResult.Success(list)
        } catch (e: Exception) {
            AppLogger.e("Mangayomi", "[MANGAYOMI][EPISODES][ERROR] $id getEpisodes failed: ${e.message}", e)
            AppResult.Error(AppError.ExtensionError(id, "Mangayomi getEpisodes failed: ${e.message}", e))
        }
    }

    private fun extractEpisodeNumberFromTitle(title: String): Float? {
        val patterns = listOf(
            Regex("""(?i)(?:ep|episode|e)\s*[:.-]?\s*(\d+(?:\.\d+)?)"""),
            Regex("""(?i)\b(\d+(?:\.\d+)?)\b""")
        )
        for (pattern in patterns) {
            val match = pattern.find(title)
            if (match != null) {
                val numStr = match.groupValues[1]
                val num = numStr.toFloatOrNull()
                if (num != null) return num
            }
        }
        return null
    }

    private fun mapRawItemToAnime(itemMap: Map<String, Any?>): Anime? {
        val link = itemMap["link"]?.toString()
            ?: itemMap["url"]?.toString()
            ?: itemMap["id"]?.toString()
            ?: return null

        val title = itemMap["name"]?.toString()
            ?: itemMap["title"]?.toString()
            ?: "Unknown Anime"

        val image = itemMap["imageUrl"]?.toString()
            ?: itemMap["cover"]?.toString()
            ?: itemMap["coverUrl"]?.toString()
            ?: itemMap["image"]?.toString()
            ?: ""

        val desc = itemMap["description"]?.toString()
            ?: itemMap["summary"]?.toString()
            ?: ""

        val status = itemMap["status"]?.toString()

        com.example.core.logging.AppLogger.d("MangayomiAdapter", "SOURCE RESULT: title=$title, url=$link, imageUrl=$image")

        return AnimeSourceMapper.mapRawToAnime(
            sourceId = id,
            sourceAnimeId = link,
            title = title,
            description = desc,
            poster = image,
            statusString = status
        )
    }

    private fun mapRawDetailToAnime(sourceAnimeId: String, detailMap: Map<String, Any?>): Anime? {
        if (detailMap.isEmpty()) return null
        val title = detailMap["name"]?.toString() ?: detailMap["title"]?.toString() ?: "Unknown Anime"
        val image = detailMap["imageUrl"]?.toString()
            ?: detailMap["cover"]?.toString()
            ?: detailMap["coverUrl"]?.toString()
            ?: detailMap["image"]?.toString()
            ?: ""
        val desc = detailMap["description"]?.toString() ?: detailMap["synopsis"]?.toString() ?: ""
        val status = detailMap["status"]?.toString()
        val author = detailMap["author"]?.toString() ?: ""

        val rawGenres = detailMap["genre"] ?: detailMap["genres"]
        val genres = when (rawGenres) {
            is List<*> -> rawGenres.mapNotNull { it?.toString() }
            is String -> rawGenres.split(",").map { it.trim() }
            else -> emptyList()
        }

        return AnimeSourceMapper.mapRawToAnime(
            sourceId = id,
            sourceAnimeId = sourceAnimeId,
            title = title,
            description = if (author.isNotBlank()) "$desc\nAuthor: $author" else desc,
            poster = image,
            statusString = status,
            genres = genres
        )
    }

    override suspend fun getVideoSources(
        episode: Episode,
        extra: Map<String, String>
    ): AppResult<List<VideoSource>> {
        val ctx = executionContext ?: return AppResult.Error(
            AppError.ExtensionError(id, "Mangayomi execution context is not loaded for extension $id")
        )
        return try {
            val rawVideos = ctx.getVideoList(episode.sourceEpisodeId)
            val sources = mutableListOf<VideoSource>()

            rawVideos.forEach { videoMap ->
                val streamUrl = videoMap["url"]?.toString() ?: videoMap["originalUrl"]?.toString() ?: ""
                if (streamUrl.isNotBlank()) {
                    val quality = videoMap["quality"]?.toString() ?: "Auto"
                    val serverName = videoMap["server"]?.toString() ?: videoMap["originalUrl"]?.toString() ?: name

                    // Extract headers
                    val headers = mutableMapOf<String, String>()
                    val rawHeaders = videoMap["headers"]
                    if (rawHeaders is Map<*, *>) {
                        rawHeaders.forEach { (k, v) ->
                            if (k != null && v != null) {
                                headers[k.toString()] = v.toString()
                            }
                        }
                    }

                    // Extract subtitles
                    val subs = mutableListOf<TrackInfo>()
                    val rawSubs = videoMap["subtitles"]
                    if (rawSubs is List<*>) {
                        rawSubs.filterIsInstance<Map<String, Any?>>().forEachIndexed { subIndex, subMap ->
                            val subUrl = subMap["url"]?.toString() ?: subMap["file"]?.toString() ?: ""
                            val subLang = subMap["lang"]?.toString() ?: subMap["language"]?.toString() ?: "en"
                            val subLabel = subMap["label"]?.toString() ?: subMap["title"]?.toString() ?: subLang
                            if (subUrl.isNotBlank()) {
                                subs.add(
                                    TrackInfo(
                                        id = "sub_${id}_${subIndex}",
                                        label = subLabel,
                                        language = subLang,
                                        url = subUrl,
                                        isDefault = subIndex == 0
                                    )
                                )
                            }
                        }
                    }

                    // Extract audio tracks
                    val audios = mutableListOf<TrackInfo>()
                    val rawAudios = videoMap["audios"] ?: videoMap["audioTracks"]
                    if (rawAudios is List<*>) {
                        rawAudios.filterIsInstance<Map<String, Any?>>().forEachIndexed { audioIndex, audioMap ->
                            val audioUrl = audioMap["url"]?.toString() ?: ""
                            val audioLang = audioMap["lang"]?.toString() ?: audioMap["language"]?.toString() ?: "jp"
                            val audioLabel = audioMap["label"]?.toString() ?: audioMap["title"]?.toString() ?: audioLang
                            if (audioUrl.isNotBlank()) {
                                audios.add(
                                    TrackInfo(
                                        id = "audio_${id}_${audioIndex}",
                                        label = audioLabel,
                                        language = audioLang,
                                        url = audioUrl,
                                        isDefault = audioIndex == 0
                                    )
                                )
                            }
                        }
                    }

                    sources.add(
                        AnimeSourceMapper.mapRawToVideoSource(
                            providerId = id,
                            serverName = serverName,
                            url = streamUrl,
                            quality = quality,
                            type = SourceType.fromUrlOrMime(streamUrl),
                            headers = headers,
                            referer = headers["Referer"],
                            subtitles = subs,
                            audioTracks = audios,
                            isDefault = sources.isEmpty()
                        )
                    )
                }
            }

            if (sources.isNotEmpty()) {
                AppResult.Success(sources)
            } else {
                AppResult.Error(AppError.ExtensionError(id, "No playable video sources found for episode ${episode.number}"))
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.ExtensionError(id, "Mangayomi getVideoSources failed: ${e.message}", e))
        }
    }

    override suspend fun getPreferences(): List<ExtensionPreference> {
        val ctx = executionContext ?: return emptyList()
        return try {
            val rawPrefs = ctx.getPreferences()
            rawPrefs.mapNotNull { prefMap ->
                val key = prefMap["key"]?.toString() ?: return@mapNotNull null
                val title = prefMap["title"]?.toString() ?: key
                val summary = prefMap["summary"]?.toString() ?: ""
                val defaultVal = prefMap["defaultValue"]?.toString() ?: ""
                ExtensionPreference.StringPreference(
                    key = key,
                    title = title,
                    summary = summary,
                    defaultValue = defaultVal,
                    value = defaultVal
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun setPreference(key: String, value: Any) {
        executionContext?.setPreference(key, value)
    }
}
