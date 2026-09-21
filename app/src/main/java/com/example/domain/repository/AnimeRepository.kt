package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import kotlinx.coroutines.flow.Flow

interface AnimeRepository {
    // Local Room database queries
    fun getAnimeById(localId: String): Flow<Anime?>
    suspend fun getAnimeByIdDirect(localId: String): Anime?
    suspend fun getAnimeBySource(sourceId: String, sourceAnimeId: String): Anime?
    fun getAllAnime(): Flow<List<Anime>>
    fun getFavoriteAnime(): Flow<List<Anime>>
    fun searchLocalAnime(query: String): Flow<List<Anime>>
    suspend fun saveAnime(anime: Anime): AppResult<Unit>
    suspend fun saveAnimeList(animeList: List<Anime>): AppResult<Unit>
    suspend fun toggleFavorite(localId: String, isFavorite: Boolean): AppResult<Unit>
    suspend fun deleteAnime(localId: String): AppResult<Unit>

    // Remote Extension-backed browsing operations
    suspend fun getPopularAnime(extensionId: String? = null, page: Int = 1): AppResult<List<Anime>>
    suspend fun getLatestAnime(extensionId: String? = null, page: Int = 1): AppResult<List<Anime>>
    suspend fun searchAnime(
        query: String,
        extensionId: String? = null,
        page: Int = 1,
        filters: Map<String, Any> = emptyMap()
    ): AppResult<List<Anime>>
    suspend fun getAnimeDetails(sourceId: String, sourceAnimeId: String): AppResult<Anime>
    suspend fun getEpisodes(sourceId: String, sourceAnimeId: String): AppResult<List<Episode>>
}
