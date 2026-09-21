package com.example.domain.usecase

import com.example.core.result.AppResult
import com.example.domain.model.Anime
import com.example.domain.repository.AnimeRepository

class AnimeSearchUseCase(
    private val animeRepository: AnimeRepository
) {
    suspend operator fun invoke(
        query: String,
        extensionId: String? = null,
        page: Int = 1,
        filters: Map<String, Any> = emptyMap()
    ): AppResult<List<Anime>> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return AppResult.Success(emptyList())
        }
        return animeRepository.searchAnime(
            query = trimmed,
            extensionId = extensionId,
            page = page,
            filters = filters
        )
    }
}
