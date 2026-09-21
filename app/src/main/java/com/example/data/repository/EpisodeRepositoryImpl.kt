package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.local.database.dao.EpisodeDao
import com.example.data.local.database.entity.EpisodeEntity
import com.example.domain.model.Episode
import com.example.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EpisodeRepositoryImpl(
    private val episodeDao: EpisodeDao
) : EpisodeRepository {

    override fun getEpisodeById(episodeId: String): Flow<Episode?> {
        return episodeDao.getEpisodeById(episodeId).map { it?.toDomain() }
    }

    override suspend fun getEpisodeByIdDirect(episodeId: String): Episode? {
        return episodeDao.getEpisodeByIdDirect(episodeId)?.toDomain()
    }

    override fun getEpisodesForAnime(animeId: String): Flow<List<Episode>> {
        return episodeDao.getEpisodesForAnime(animeId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getEpisodesForAnimeDirect(animeId: String): List<Episode> {
        return episodeDao.getEpisodesForAnimeDirect(animeId).map { it.toDomain() }
    }

    override fun getEpisodesForSeason(animeId: String, seasonNumber: Int): Flow<List<Episode>> {
        return episodeDao.getEpisodesForSeason(animeId, seasonNumber).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveEpisode(episode: Episode): AppResult<Unit> {
        return try {
            episodeDao.insertEpisode(EpisodeEntity.fromDomain(episode))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("EpisodeRepo", "Failed to save episode ${episode.id}", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to save episode", e))
        }
    }

    override suspend fun saveEpisodes(episodes: List<Episode>): AppResult<Unit> {
        return try {
            episodeDao.insertEpisodes(episodes.map { EpisodeEntity.fromDomain(it) })
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("EpisodeRepo", "Failed to save episodes list", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to save episodes list", e))
        }
    }

    override suspend fun updateProgress(
        episodeId: String,
        positionMs: Long,
        durationMs: Long,
        isWatched: Boolean
    ): AppResult<Unit> {
        return try {
            episodeDao.updateEpisodeProgress(episodeId, positionMs, durationMs, isWatched)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("EpisodeRepo", "Failed to update episode progress $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update progress", e))
        }
    }

    override suspend fun markWatched(episodeId: String, isWatched: Boolean): AppResult<Unit> {
        return try {
            episodeDao.markEpisodeWatched(episodeId, isWatched)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("EpisodeRepo", "Failed to mark episode watched $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to mark watched", e))
        }
    }

    override suspend fun markWatchedUpTo(animeId: String, upToNumber: Float, isWatched: Boolean): AppResult<Unit> {
        return try {
            episodeDao.markEpisodesWatchedUpTo(animeId, upToNumber, isWatched)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("EpisodeRepo", "Failed to mark episodes watched up to $upToNumber", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to mark episodes watched", e))
        }
    }

    override suspend fun deleteEpisode(episodeId: String): AppResult<Unit> {
        return try {
            episodeDao.deleteEpisodeById(episodeId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("EpisodeRepo", "Failed to delete episode $episodeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete episode", e))
        }
    }
}
