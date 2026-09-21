package com.example.domain.repository

import com.example.domain.model.SkipSegment

interface SkipMetadataRepository {
    suspend fun getSkipSegments(
        animeId: String,
        episodeId: String,
        episodeNumber: Float,
        durationMs: Long = 0L
    ): List<SkipSegment>

    suspend fun saveSkipSegments(
        episodeId: String,
        segments: List<SkipSegment>
    )
}
