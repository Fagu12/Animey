package com.example.domain.usecase

import com.example.core.result.AppResult
import com.example.domain.model.Anime
import com.example.domain.repository.AnimeRepository

class GetPopularAnimeUseCase(
    private val animeRepository: AnimeRepository
) {
    suspend operator fun invoke(
        extensionId: String? = null,
        page: Int = 1
    ): AppResult<List<Anime>> {
        return animeRepository.getPopularAnime(
            extensionId = extensionId,
            page = page
        )
    }
}
