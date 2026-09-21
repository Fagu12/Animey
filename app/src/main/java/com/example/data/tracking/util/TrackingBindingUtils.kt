package com.example.data.tracking.util

import kotlin.math.max

object TrackingBindingUtils {

    /**
     * Calculates a similarity score between 0.0f and 1.0f using token Jaccard similarity
     * combined with Levenshtein distance for precise matching.
     */
    fun calculateTitleSimilarity(title1: String, title2: String): Float {
        val norm1 = normalize(title1)
        val norm2 = normalize(title2)

        if (norm1 == norm2) return 1.0f
        if (norm1.isEmpty() || norm2.isEmpty()) return 0.0f

        val tokens1 = norm1.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        val tokens2 = norm2.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()

        val intersection = tokens1.intersect(tokens2).size.toFloat()
        val union = tokens1.union(tokens2).size.toFloat()
        val jaccard = if (union > 0) intersection / union else 0.0f

        val levDist = levenshteinDistance(norm1, norm2)
        val maxLen = max(norm1.length, norm2.length)
        val levSim = 1.0f - (levDist.toFloat() / maxLen.toFloat())

        // Weighted combination: 60% token overlap, 40% character edit distance
        val score = (0.6f * jaccard) + (0.4f * levSim)
        return score.coerceIn(0.0f, 1.0f)
    }

    /**
     * Extracts explicit season numbers from titles like "Season 2", "2nd Season", "S3", "Part 2"
     */
    fun extractSeasonNumber(title: String): Int {
        val lower = title.lowercase()
        val regexes = listOf(
            Regex("season\\s*(\\d+)"),
            Regex("(\\d+)(?:st|nd|rd|th)\\s*season"),
            Regex("\\bs(\\d+)\\b"),
            Regex("part\\s*(\\d+)")
        )

        for (regex in regexes) {
            val match = regex.find(lower)
            if (match != null) {
                return match.groupValues[1].toIntOrNull() ?: 1
            }
        }
        return 1
    }

    private fun normalize(str: String): String {
        return str.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
