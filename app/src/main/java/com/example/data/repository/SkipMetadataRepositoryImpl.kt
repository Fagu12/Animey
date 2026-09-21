package com.example.data.repository

import com.example.data.local.database.dao.EpisodeDao
import com.example.domain.model.SkipSegment
import com.example.domain.repository.SkipMetadataRepository

class SkipMetadataRepositoryImpl(
    private val episodeDao: EpisodeDao
) : SkipMetadataRepository {

    override suspend fun getSkipSegments(
        animeId: String,
        episodeId: String,
        episodeNumber: Float,
        durationMs: Long
    ): List<SkipSegment> {
        val episode = episodeDao.getEpisodeByIdDirect(episodeId)
        if (episode != null && episode.skipSegments.isNotEmpty()) {
            return episode.skipSegments.map { it.copy(source = it.source.ifBlank { "independent" }) }
        }
        return emptyList()
    }

    override suspend fun saveSkipSegments(
        episodeId: String,
        segments: List<SkipSegment>
    ) {
        val episode = episodeDao.getEpisodeByIdDirect(episodeId) ?: return
        val normalized = segments.map { it.copy(source = it.source.ifBlank { "independent" }) }
        episodeDao.updateEpisode(episode.copy(skipSegments = normalized))
    }
}
