package com.example.data.repository

import com.example.data.local.datastore.SettingsDataStore
import com.example.domain.repository.SourcePreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SourcePreferenceRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : SourcePreferenceRepository {

    override val globalPreferredProvider: Flow<String?> =
        settingsDataStore.settingsFlow.map { it.defaultExtensionId.ifBlank { null } }

    override suspend fun setGlobalPreferredProvider(providerId: String?) {
        settingsDataStore.setDefaultExtensionId(providerId ?: "")
    }

    override val preferredServer: Flow<String?> =
        settingsDataStore.preferredServerFlow

    override suspend fun setPreferredServer(serverName: String?) {
        settingsDataStore.setPreferredServer(serverName)
    }

    override val preferredQuality: Flow<String> =
        settingsDataStore.settingsFlow.map { it.defaultQuality }

    override suspend fun setPreferredQuality(quality: String) {
        settingsDataStore.setDefaultQuality(quality)
    }

    override fun getPreferredProviderForAnime(animeId: String): Flow<String?> =
        settingsDataStore.getAnimePreference(animeId, "provider")

    override suspend fun setPreferredProviderForAnime(animeId: String, providerId: String?) {
        settingsDataStore.setAnimePreference(animeId, "provider", providerId)
    }

    override fun getPreferredServerForAnime(animeId: String): Flow<String?> =
        settingsDataStore.getAnimePreference(animeId, "server")

    override suspend fun setPreferredServerForAnime(animeId: String, serverName: String?) {
        settingsDataStore.setAnimePreference(animeId, "server", serverName)
    }

    override fun getPreferredQualityForAnime(animeId: String): Flow<String?> =
        settingsDataStore.getAnimePreference(animeId, "quality")

    override suspend fun setPreferredQualityForAnime(animeId: String, quality: String?) {
        settingsDataStore.setAnimePreference(animeId, "quality", quality)
    }

    override suspend fun clearAnimePreferences(animeId: String) {
        settingsDataStore.clearAnimePreferences(animeId)
    }
}
