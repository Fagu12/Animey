package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.local.database.dao.AnimeDao
import com.example.data.local.database.dao.EpisodeDao
import com.example.data.local.database.entity.AnimeEntity
import com.example.data.local.database.entity.EpisodeEntity
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.ExtensionManager
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.repository.AnimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnimeRepositoryImpl(
    private val animeDao: AnimeDao,
    private val episodeDao: EpisodeDao? = null,
    private val extensionManager: ExtensionManager? = null
) : AnimeRepository {

    private fun resolveExtension(extensionId: String?): AnimeExtension? {
        val manager = extensionManager ?: return null
        if (!extensionId.isNullOrBlank()) {
            val direct = manager.getExtension(extensionId)
            if (direct != null) return direct
        }
        val preferred = manager.preferredExtension.value
        if (preferred != null) return preferred

        val enabled = manager.enabledExtensions.value.firstOrNull()
        if (enabled != null) return enabled

        // Fallback to any available extension if StateFlows are pending async dispatch
        for (info in manager.allExtensions.value) {
            val ext = manager.getExtension(info.manifest.id)
            if (ext != null) return ext
        }

        return manager.getExtension("builtin.test.anime")
    }

    override fun getAnimeById(localId: String): Flow<Anime?> {
        return animeDao.getAnimeById(localId).map { it?.toDomain() }
    }

    override suspend fun getAnimeByIdDirect(localId: String): Anime? {
        return animeDao.getAnimeByIdDirect(localId)?.toDomain()
    }

    override suspend fun getAnimeBySource(sourceId: String, sourceAnimeId: String): Anime? {
        return animeDao.getAnimeBySourceId(sourceId, sourceAnimeId)?.toDomain()
    }

    override fun getAllAnime(): Flow<List<Anime>> {
        return animeDao.getAllAnime().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getFavoriteAnime(): Flow<List<Anime>> {
        return animeDao.getFavoriteAnime().map { entities -> entities.map { it.toDomain() } }
    }

    override fun searchLocalAnime(query: String): Flow<List<Anime>> {
        return animeDao.searchLocalAnime(query).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun saveAnime(anime: Anime): AppResult<Unit> {
        return try {
            animeDao.insertAnime(AnimeEntity.fromDomain(anime))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("AnimeRepo", "Failed to save anime ${anime.localId}", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to save anime", e))
        }
    }

    override suspend fun saveAnimeList(animeList: List<Anime>): AppResult<Unit> {
        return try {
            animeDao.insertAnimeList(animeList.map { AnimeEntity.fromDomain(it) })
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("AnimeRepo", "Failed to save anime list", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to save anime list", e))
        }
    }

    override suspend fun toggleFavorite(localId: String, isFavorite: Boolean): AppResult<Unit> {
        return try {
            animeDao.updateFavoriteStatus(localId, isFavorite)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("AnimeRepo", "Failed to toggle favorite for $localId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to toggle favorite", e))
        }
    }

    override suspend fun deleteAnime(localId: String): AppResult<Unit> {
        return try {
            animeDao.deleteAnimeById(localId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("AnimeRepo", "Failed to delete anime $localId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete anime", e))
        }
    }

    // Remote Extension-backed browsing operations
    override suspend fun getPopularAnime(extensionId: String?, page: Int): AppResult<List<Anime>> {
        val extension = resolveExtension(extensionId)
            ?: return AppResult.Error(AppError.NotFoundError("No active extension found for browsing"))

        return when (val result = extension.getPopularAnime(page)) {
            is AppResult.Success -> {
                try {
                    animeDao.insertAnimeList(result.data.map { AnimeEntity.fromDomain(it) })
                } catch (e: Exception) {
                    AppLogger.w("AnimeRepo", "Could not cache popular anime into DB: ${e.message}")
                }
                result
            }
            is AppResult.Error -> result
            is AppResult.Loading -> result
        }
    }

    override suspend fun getLatestAnime(extensionId: String?, page: Int): AppResult<List<Anime>> {
        val extension = resolveExtension(extensionId)
            ?: return AppResult.Error(AppError.NotFoundError("No active extension found for browsing"))

        return when (val result = extension.getLatestAnime(page)) {
            is AppResult.Success -> {
                try {
                    animeDao.insertAnimeList(result.data.map { AnimeEntity.fromDomain(it) })
                } catch (e: Exception) {
                    AppLogger.w("AnimeRepo", "Could not cache latest anime into DB: ${e.message}")
                }
                result
            }
            is AppResult.Error -> result
            is AppResult.Loading -> result
        }
    }

    override suspend fun searchAnime(
        query: String,
        extensionId: String?,
        page: Int,
        filters: Map<String, Any>
    ): AppResult<List<Anime>> {
        val extension = resolveExtension(extensionId)
            ?: return AppResult.Error(AppError.NotFoundError("No active extension found for search"))

        return when (val result = extension.searchAnime(query = query, page = page, filters = filters)) {
            is AppResult.Success -> {
                try {
                    animeDao.insertAnimeList(result.data.map { AnimeEntity.fromDomain(it) })
                } catch (e: Exception) {
                    AppLogger.w("AnimeRepo", "Could not cache search anime results into DB: ${e.message}")
                }
                result
            }
            is AppResult.Error -> result
            is AppResult.Loading -> result
        }
    }

    override suspend fun getAnimeDetails(sourceId: String, sourceAnimeId: String): AppResult<Anime> {
        val effectiveAnimeId = "anime_${sourceId}_${sourceAnimeId.hashCode()}"
        val extension = resolveExtension(sourceId)
        val cached = animeDao.getAnimeBySourceId(sourceId, sourceAnimeId)
            ?: animeDao.getAnimeByIdDirect(effectiveAnimeId)
            ?: animeDao.getAnimeByIdDirect("$sourceId:$sourceAnimeId")

        if (extension == null) {
            return if (cached != null) {
                AppResult.Success(cached.toDomain())
            } else {
                AppResult.Error(AppError.NotFoundError("Extension '$sourceId' not found and no cached data exists"))
            }
        }

        return when (val result = extension.getAnimeDetails(sourceAnimeId)) {
            is AppResult.Success -> {
                val fetched = result.data
                val merged = if (cached != null) {
                    val cachedDomain = cached.toDomain()
                    fetched.copy(
                        localId = effectiveAnimeId,
                        poster = fetched.poster.ifBlank { cachedDomain.poster },
                        banner = fetched.banner.ifBlank { cachedDomain.banner.ifBlank { cachedDomain.poster } },
                        description = fetched.description.ifBlank { cachedDomain.description },
                        status = if (fetched.status == "Ongoing" && cachedDomain.status != "Ongoing") cachedDomain.status else fetched.status,
                        genres = fetched.genres.ifEmpty { cachedDomain.genres },
                        isFavorite = cachedDomain.isFavorite
                    )
                } else {
                    fetched.copy(localId = effectiveAnimeId)
                }

                try {
                    animeDao.insertAnime(AnimeEntity.fromDomain(merged))
                    val epResult = extension.getEpisodes(sourceAnimeId)
                    if (epResult is AppResult.Success && epResult.data.isNotEmpty()) {
                        val epEntities = epResult.data.map { ep ->
                            EpisodeEntity.fromDomain(ep.copy(animeId = effectiveAnimeId))
                        }
                        episodeDao?.insertEpisodes(epEntities)
                        AppLogger.i("Mangayomi", "[MANGAYOMI][DETAIL][DATABASE] episodeCount=${epEntities.size}")
                    }
                } catch (e: Exception) {
                    AppLogger.w("AnimeRepo", "Could not cache anime details or episodes: ${e.message}")
                }
                AppResult.Success(merged)
            }
            is AppResult.Error -> {
                val cachedEpisodes = episodeDao?.getEpisodesForAnimeDirect(effectiveAnimeId)
                if (cached != null && !cachedEpisodes.isNullOrEmpty()) {
                    AppResult.Success(cached.toDomain())
                } else {
                    result
                }
            }
            is AppResult.Loading -> result
        }
    }

    override suspend fun getEpisodes(sourceId: String, sourceAnimeId: String): AppResult<List<Episode>> {
        val effectiveAnimeId = "anime_${sourceId}_${sourceAnimeId.hashCode()}"
        val extension = resolveExtension(sourceId)
        if (extension == null) {
            val cachedEpisodes = episodeDao?.getEpisodesForAnimeDirect(effectiveAnimeId)
            return if (!cachedEpisodes.isNullOrEmpty()) {
                AppResult.Success(cachedEpisodes.map { it.toDomain() })
            } else {
                AppResult.Error(AppError.NotFoundError("Extension '$sourceId' not found and no cached episodes exist"))
            }
        }

        return when (val result = extension.getEpisodes(sourceAnimeId)) {
            is AppResult.Success -> {
                val eps = result.data.map { it.copy(animeId = effectiveAnimeId) }
                try {
                    episodeDao?.insertEpisodes(eps.map { EpisodeEntity.fromDomain(it) })
                    AppLogger.i("Mangayomi", "[MANGAYOMI][DETAIL][DATABASE] episodeCount=${eps.size}")
                } catch (e: Exception) {
                    AppLogger.w("AnimeRepo", "Could not cache episodes into DB: ${e.message}")
                }
                AppResult.Success(eps)
            }
            is AppResult.Error -> {
                val cachedEpisodes = episodeDao?.getEpisodesForAnimeDirect(effectiveAnimeId)
                if (!cachedEpisodes.isNullOrEmpty()) {
                    AppResult.Success(cachedEpisodes.map { it.toDomain() })
                } else {
                    result
                }
            }
            is AppResult.Loading -> result
        }
    }
}
