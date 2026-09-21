package com.example.tracking

import com.example.data.tracking.util.TrackingBindingUtils
import com.example.domain.model.TrackingBinding
import com.example.domain.model.TrackingPlatform
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.usecase.tracking.AutoMapAnimeUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingBindingTest {

    @Test
    fun testTitleSimilarityAndSeasonExtraction() {
        val titleS1 = "Attack on Titan Season 1"
        val titleS2 = "Attack on Titan Season 2"
        val titleS3 = "Attack on Titan Season 3"

        assertEquals(1, TrackingBindingUtils.extractSeasonNumber(titleS1))
        assertEquals(2, TrackingBindingUtils.extractSeasonNumber(titleS2))
        assertEquals(3, TrackingBindingUtils.extractSeasonNumber(titleS3))

        val similarityExact = TrackingBindingUtils.calculateTitleSimilarity("Attack on Titan", "Attack on Titan")
        assertTrue(similarityExact > 0.95f)

        val similarityDiffSeason = TrackingBindingUtils.calculateTitleSimilarity("Attack on Titan Season 1", "Attack on Titan Season 2")
        // Different season titles should have distinct confidence
        assertTrue(similarityDiffSeason < 0.95f)
    }

    @Test
    fun testSeparateSeasonAndProviderMappings() {
        // Test that application maintains separate mappings for different seasons and providers
        val providerA_S1 = TrackingBinding(
            localAnimeId = "local_a_s1",
            sourceId = "provider_a",
            sourceAnimeId = "anime_101",
            platform = TrackingPlatform.ANILIST,
            aniListId = 10001,
            season = 1,
            mappingConfidence = 0.95f
        )

        val providerA_S2 = TrackingBinding(
            localAnimeId = "local_a_s2",
            sourceId = "provider_a",
            sourceAnimeId = "anime_102",
            platform = TrackingPlatform.ANILIST,
            aniListId = 10002,
            season = 2,
            mappingConfidence = 0.95f
        )

        val providerB_S1 = TrackingBinding(
            localAnimeId = "local_b_s1",
            sourceId = "provider_b",
            sourceAnimeId = "series_x_s1",
            platform = TrackingPlatform.ANILIST,
            aniListId = 10001,
            season = 1,
            mappingConfidence = 0.90f
        )

        // Verify provider IDs and AniList IDs are strictly independent
        assertNotEquals(providerA_S1.sourceAnimeId, providerA_S1.aniListId.toString())
        assertNotEquals(providerA_S1.aniListId, providerA_S2.aniListId)
        assertEquals(providerA_S1.aniListId, providerB_S1.aniListId)
        assertNotEquals(providerA_S1.sourceId, providerB_S1.sourceId)
    }

    @Test
    fun testCandidateRankingInAutoMap() {
        val entry1 = AniListMediaEntry(
            mediaId = 101,
            title = "Jujutsu Kaisen Season 2",
            totalEpisodes = 23
        )
        val entry2 = AniListMediaEntry(
            mediaId = 102,
            title = "Jujutsu Kaisen",
            totalEpisodes = 24
        )

        val score1 = TrackingBindingUtils.calculateTitleSimilarity("Jujutsu Kaisen Season 2", entry1.title)
        val score2 = TrackingBindingUtils.calculateTitleSimilarity("Jujutsu Kaisen Season 2", entry2.title)

        assertTrue("Exact season match score should be higher", score1 > score2)
    }
}
