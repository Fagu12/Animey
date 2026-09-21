package com.example.domain.usecase.tracking

import com.example.core.result.AppResult
import com.example.domain.repository.AniListRepository

class AniListLogoutUseCase(
    private val aniListRepository: AniListRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return aniListRepository.logout()
    }
}
