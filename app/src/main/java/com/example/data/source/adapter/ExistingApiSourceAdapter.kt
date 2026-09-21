package com.example.data.source.adapter

import com.example.core.result.AppResult
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.runtime.ExtensionLifecycleState
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.VideoSource
import com.example.domain.source.AnimeSource
import com.example.domain.source.ExtensionCompatibilityStatus
import com.example.domain.source.SourceTypeInfo

/**
 * Adapter bridging existing AnimeExtension implementations to the unified AnimeSource interface.
 * Ensures complete backwards compatibility with all existing app features and tests.
 */
class ExistingApiSourceAdapter(
    val delegate: AnimeExtension
) : AnimeSource {

    override val id: String = delegate.id
    override val name: String = delegate.name
    override val lang: String = delegate.lang
    override val baseUrl: String = delegate.baseUrl
    override val iconUrl: String = delegate.iconUrl
    override val sourceType: SourceTypeInfo = SourceTypeInfo.BUILTIN_API
    override val lifecycleState: ExtensionLifecycleState = ExtensionLifecycleState.ACTIVE
    override val compatibilityStatus: ExtensionCompatibilityStatus = ExtensionCompatibilityStatus.COMPATIBLE

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

    override suspend fun getVideoSources(
        episode: Episode,
        extra: Map<String, String>
    ): AppResult<List<VideoSource>> {
        return delegate.getEpisodeStreams(episode.sourceEpisodeId, extra)
    }

    override suspend fun getPreferences(): List<ExtensionPreference> {
        return delegate.getPreferences()
    }

    override suspend fun setPreference(key: String, value: Any) {
        delegate.setPreference(key, value)
    }
}
