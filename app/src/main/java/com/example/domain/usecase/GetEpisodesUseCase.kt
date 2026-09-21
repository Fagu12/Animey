package com.example.domain.usecase

import com.example.core.result.AppResult
import com.example.domain.model.Episode
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.EpisodeRepository

class GetEpisodesUseCase(
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository
) {
    suspend operator fun invoke(
        sourceId: String,
        sourceAnimeId: String,
        animeLocalId: String? = null
    ): AppResult<List<Episode>> {
        val effectiveAnimeId = animeLocalId ?: "$sourceId:$sourceAnimeId"
        val localEpisodes = episodeRepository.getEpisodesForAnimeDirect(effectiveAnimeId)
            .associateBy { it.sourceEpisodeId }

        val remoteResult = animeRepository.getEpisodes(
            sourceId = sourceId,
            sourceAnimeId = sourceAnimeId
        )

        return when (remoteResult) {
            is AppResult.Success -> {
                val merged = remoteResult.data.map { remoteEp ->
                    val local = localEpisodes[remoteEp.sourceEpisodeId]
                    if (local != null) {
                        remoteEp.copy(
                            id = local.id,
                            animeId = effectiveAnimeId,
                            lastPositionMs = local.lastPositionMs,
                            durationMs = if (remoteEp.durationMs > 0) remoteEp.durationMs else local.durationMs,
                            isWatched = local.isWatched,
                            isDownloaded = local.isDownloaded,
                            skipSegments = if (remoteEp.skipSegments.isNotEmpty()) remoteEp.skipSegments else local.skipSegments
                        )
                    } else {
                        remoteEp.copy(animeId = effectiveAnimeId)
                    }
                }
                // Cache episodes locally in Room
                episodeRepository.saveEpisodes(merged)
                AppResult.Success(merged)
            }
            is AppResult.Error -> {
                if (localEpisodes.isNotEmpty()) {
                    AppResult.Success(localEpisodes.values.sortedBy { it.number })
                } else {
                    remoteResult
                }
            }
            is AppResult.Loading -> remoteResult
        }
    }
}
