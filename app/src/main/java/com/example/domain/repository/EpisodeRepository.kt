package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.Episode
import kotlinx.coroutines.flow.Flow

interface EpisodeRepository {
    fun getEpisodeById(episodeId: String): Flow<Episode?>
    suspend fun getEpisodeByIdDirect(episodeId: String): Episode?
    fun getEpisodesForAnime(animeId: String): Flow<List<Episode>>
    suspend fun getEpisodesForAnimeDirect(animeId: String): List<Episode>
    fun getEpisodesForSeason(animeId: String, seasonNumber: Int): Flow<List<Episode>>
    suspend fun saveEpisode(episode: Episode): AppResult<Unit>
    suspend fun saveEpisodes(episodes: List<Episode>): AppResult<Unit>
    suspend fun updateProgress(episodeId: String, positionMs: Long, durationMs: Long, isWatched: Boolean): AppResult<Unit>
    suspend fun markWatched(episodeId: String, isWatched: Boolean): AppResult<Unit>
    suspend fun markWatchedUpTo(animeId: String, upToNumber: Float, isWatched: Boolean): AppResult<Unit>
    suspend fun deleteEpisode(episodeId: String): AppResult<Unit>
}
