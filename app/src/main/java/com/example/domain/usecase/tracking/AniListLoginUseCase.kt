package com.example.domain.usecase.tracking

import com.example.core.result.AppResult
import com.example.domain.model.tracking.AniListUser
import com.example.domain.repository.AniListRepository

class AniListLoginUseCase(
    private val aniListRepository: AniListRepository
) {
    suspend operator fun invoke(token: String): AppResult<AniListUser> {
        return aniListRepository.loginWithToken(token)
    }
}
