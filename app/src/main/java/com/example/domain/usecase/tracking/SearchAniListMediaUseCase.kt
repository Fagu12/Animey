package com.example.domain.usecase.tracking

import com.example.core.result.AppResult
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.repository.AniListRepository

class SearchAniListMediaUseCase(
    private val aniListRepository: AniListRepository
) {
    suspend operator fun invoke(query: String): AppResult<List<AniListMediaEntry>> {
        return aniListRepository.searchAniListMedia(query)
    }
}
