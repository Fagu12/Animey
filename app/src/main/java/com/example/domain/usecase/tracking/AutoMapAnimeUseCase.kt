package com.example.domain.usecase.tracking

import com.example.core.result.AppResult
import com.example.data.tracking.util.TrackingBindingUtils
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.repository.AniListRepository

data class BindingCandidate(
    val mediaEntry: AniListMediaEntry,
    val confidence: Float,
    val detectedSeason: Int
)

class AutoMapAnimeUseCase(
    private val aniListRepository: AniListRepository
) {
    suspend operator fun invoke(
        animeTitle: String
    ): AppResult<List<BindingCandidate>> {
        val detectedSeason = TrackingBindingUtils.extractSeasonNumber(animeTitle)
        
        // Clean title for search (remove season/part designations)
        val cleanTitle = animeTitle
            .replace(Regex("(?i)season\\s*\\d+"), "")
            .replace(Regex("(?i)\\d+(st|nd|rd|th)\\s*season"), "")
            .replace(Regex("(?i)\\bs\\d+\\b"), "")
            .replace(Regex("(?i)part\\s*\\d+"), "")
            .trim()

        val searchQuery = if (cleanTitle.length >= 2) cleanTitle else animeTitle

        return when (val searchRes = aniListRepository.searchAniListMedia(searchQuery)) {
            is AppResult.Success -> {
                val candidates = searchRes.data.map { media ->
                    val titleSim = TrackingBindingUtils.calculateTitleSimilarity(animeTitle, media.title)
                    val candidateSeason = TrackingBindingUtils.extractSeasonNumber(media.title)
                    
                    var confidence = titleSim
                    if (candidateSeason == detectedSeason && detectedSeason > 1) {
                        confidence = (confidence + 0.2f).coerceAtMost(1.0f)
                    }

                    BindingCandidate(
                        mediaEntry = media,
                        confidence = confidence,
                        detectedSeason = detectedSeason
                    )
                }.sortedByDescending { it.confidence }

                AppResult.Success(candidates)
            }
            is AppResult.Error -> searchRes
            else -> AppResult.Error(com.example.core.result.AppError.GeneralError("Failed to auto map anime"))
        }
    }
}
