package com.example.domain.usecase

import com.example.core.result.AppResult
import com.example.domain.model.Anime
import com.example.domain.repository.AnimeRepository

class GetAnimeDetailsUseCase(
    private val animeRepository: AnimeRepository
) {
    suspend operator fun invoke(
        sourceId: String,
        sourceAnimeId: String,
        localId: String? = null
    ): AppResult<Anime> {
        // If we have a localId, check local DB first for existing favorite status and details
        val effectiveLocalId = localId ?: "$sourceId:$sourceAnimeId"
        val localCached = animeRepository.getAnimeByIdDirect(effectiveLocalId)

        val remoteResult = animeRepository.getAnimeDetails(
            sourceId = sourceId,
            sourceAnimeId = sourceAnimeId
        )

        return when (remoteResult) {
            is AppResult.Success -> {
                // Preserve local user state like isFavorite if present
                val mergedAnime = if (localCached != null) {
                    remoteResult.data.copy(
                        isFavorite = localCached.isFavorite
                    )
                } else {
                    remoteResult.data
                }
                animeRepository.saveAnime(mergedAnime)
                AppResult.Success(mergedAnime)
            }
            is AppResult.Error -> {
                if (localCached != null) {
                    AppResult.Success(localCached)
                } else {
                    remoteResult
                }
            }
            is AppResult.Loading -> remoteResult
        }
    }
}
