package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository managing user preferences for:
 * - Preferred Provider (Global & Per-Anime)
 * - Preferred Server (Global & Per-Anime)
 * - Preferred Quality (Global & Per-Anime)
 */
interface SourcePreferenceRepository {

    // ==================== Global Preferences ====================

    /** Global preferred extension/provider ID (nullable if default/none set) */
    val globalPreferredProvider: Flow<String?>

    suspend fun setGlobalPreferredProvider(providerId: String?)

    /** Global preferred server name or pattern (e.g., "FastCDN", "Mirror 1") */
    val preferredServer: Flow<String?>

    suspend fun setPreferredServer(serverName: String?)

    /** Global preferred stream quality (e.g., "1080p", "720p", "480p", "360p") */
    val preferredQuality: Flow<String>

    suspend fun setPreferredQuality(quality: String)

    // ==================== Per-Anime Preferences ====================

    /** Gets preferred provider ID specifically configured for an anime */
    fun getPreferredProviderForAnime(animeId: String): Flow<String?>

    suspend fun setPreferredProviderForAnime(animeId: String, providerId: String?)

    /** Gets preferred server name specifically configured for an anime */
    fun getPreferredServerForAnime(animeId: String): Flow<String?>

    suspend fun setPreferredServerForAnime(animeId: String, serverName: String?)

    /** Gets preferred quality specifically configured for an anime */
    fun getPreferredQualityForAnime(animeId: String): Flow<String?>

    suspend fun setPreferredQualityForAnime(animeId: String, quality: String?)

    /** Clears all custom source preferences for a given anime */
    suspend fun clearAnimePreferences(animeId: String)
}
