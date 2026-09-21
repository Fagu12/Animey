package com.example.domain.usecase.tracking

import com.example.domain.model.tracking.AniListUser
import com.example.domain.repository.AniListRepository
import kotlinx.coroutines.flow.Flow

class GetAniListUserUseCase(
    private val aniListRepository: AniListRepository
) {
    operator fun invoke(): Flow<AniListUser?> {
        return aniListRepository.getStoredUser()
    }
}
