package com.example.data.source.adapter

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.extension.runtime.ExtensionLifecycleState
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.VideoSource
import com.example.domain.source.AnimeSource
import com.example.domain.source.ExtensionCompatibilityStatus
import com.example.domain.source.SourceTypeInfo

/**
 * Adapter contract and stub for Aniyomi APK-based Anime extensions.
 * Bytecode execution, ClassLoader, and HostShim will be connected in Phase 4.
 */
class AniyomiSourceAdapter(
    override val id: String,
    override val name: String,
    override val lang: String,
    override val baseUrl: String,
    override val iconUrl: String = "",
    override val lifecycleState: ExtensionLifecycleState = ExtensionLifecycleState.INSTALLED,
    override val compatibilityStatus: ExtensionCompatibilityStatus = ExtensionCompatibilityStatus.COMPATIBLE,
    val libVersion: Int = 17
) : AnimeSource {

    override val sourceType: SourceTypeInfo = SourceTypeInfo.ANIYOMI_EXTENSION

    override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
        // Phase 4: Will invoke source.popularAnimeRequest / fetchPopularAnime via AniyomiHostShim
        return AppResult.Error(AppError.ExtensionError("Aniyomi runtime bytecode execution is scheduled for Phase 4"))
    }

    override suspend fun getLatestAnime(page: Int): AppResult<List<Anime>> {
        // Phase 4: Will invoke source.latestUpdatesRequest / fetchLatestUpdates via AniyomiHostShim
        return AppResult.Error(AppError.ExtensionError("Aniyomi runtime bytecode execution is scheduled for Phase 4"))
    }

    override suspend fun searchAnime(
        query: String,
        page: Int,
        filters: Map<String, Any>
    ): AppResult<List<Anime>> {
        // Phase 4: Will invoke source.searchAnimeRequest / fetchSearchAnime via AniyomiHostShim
        return AppResult.Error(AppError.ExtensionError("Aniyomi runtime bytecode execution is scheduled for Phase 4"))
    }

    override suspend fun getAnimeDetails(sourceAnimeId: String): AppResult<Anime> {
        // Phase 4: Will invoke source.getAnimeDetails / fetchAnimeDetails via AniyomiHostShim
        return AppResult.Error(AppError.ExtensionError("Aniyomi runtime bytecode execution is scheduled for Phase 4"))
    }

    override suspend fun getEpisodes(sourceAnimeId: String): AppResult<List<Episode>> {
        // Phase 4: Will invoke source.getEpisodeList / fetchEpisodeList via AniyomiHostShim
        return AppResult.Error(AppError.ExtensionError("Aniyomi runtime bytecode execution is scheduled for Phase 4"))
    }

    override suspend fun getVideoSources(
        episode: Episode,
        extra: Map<String, String>
    ): AppResult<List<VideoSource>> {
        // Phase 4: Will invoke source.getVideoList / fetchVideoList via AniyomiHostShim
        return AppResult.Error(AppError.ExtensionError("Aniyomi runtime bytecode execution is scheduled for Phase 4"))
    }

    override suspend fun getPreferences(): List<ExtensionPreference> {
        return emptyList()
    }

    override suspend fun setPreference(key: String, value: Any) {}
}
