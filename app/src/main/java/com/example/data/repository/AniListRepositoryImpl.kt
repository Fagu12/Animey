package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.local.datastore.SettingsDataStore
import com.example.data.tracking.anilist.AniListService
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.model.tracking.AniListUser
import com.example.domain.model.tracking.TrackStatus
import com.example.domain.repository.AniListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class AniListRepositoryImpl(
    private val aniListService: AniListService,
    private val settingsDataStore: SettingsDataStore
) : AniListRepository {

    override fun getAccessToken(): Flow<String?> {
        return settingsDataStore.aniListAccessTokenFlow
    }

    override fun getStoredUser(): Flow<AniListUser?> {
        return settingsDataStore.aniListUserDataFlow.map { jsonString ->
            parseUserFromJson(jsonString)
        }
    }

    override suspend fun loginWithToken(token: String): AppResult<AniListUser> {
        val sanitizedToken = token.trim()
        if (sanitizedToken.isEmpty()) {
            return AppResult.Error(AppError.ValidationError("Access token cannot be empty"))
        }

        AppLogger.d("AniListRepository", "Attempting AniList login with access token")
        val result = aniListService.fetchViewerProfile(sanitizedToken)

        if (result is AppResult.Success) {
            val user = result.data
            settingsDataStore.setAniListAccessToken(sanitizedToken)
            settingsDataStore.setAniListUserData(serializeUserToJson(user))
            AppLogger.d("AniListRepository", "Successfully logged in as ${user.name}")
        } else if (result is AppResult.Error) {
            AppLogger.e("AniListRepository", "AniList login failed")
        }

        return result
    }

    override suspend fun logout(): AppResult<Unit> {
        AppLogger.d("AniListRepository", "Logging out of AniList")
        settingsDataStore.setAniListAccessToken(null)
        settingsDataStore.setAniListUserData(null)
        return AppResult.Success(Unit)
    }

    override suspend fun fetchCurrentUserProfile(): AppResult<AniListUser> {
        val token = getAccessToken().first()
            ?: return AppResult.Error(AppError.AuthenticationError("Not logged into AniList"))

        val result = aniListService.fetchViewerProfile(token)
        if (result is AppResult.Success) {
            settingsDataStore.setAniListUserData(serializeUserToJson(result.data))
        }
        return result
    }

    override suspend fun updateMediaProgress(
        mediaId: Int,
        status: TrackStatus,
        progress: Int,
        score: Double?
    ): AppResult<AniListMediaEntry> {
        val token = getAccessToken().first()
            ?: return AppResult.Error(AppError.AuthenticationError("Not logged into AniList"))

        return aniListService.saveMediaListEntry(token, mediaId, status, progress, score)
    }

    override suspend fun getMediaEntry(mediaId: Int): AppResult<AniListMediaEntry?> {
        val token = getAccessToken().first()
            ?: return AppResult.Error(AppError.AuthenticationError("Not logged into AniList"))

        return aniListService.fetchMediaListEntry(token, mediaId)
    }

    override suspend fun getUserMediaList(status: TrackStatus?): AppResult<List<AniListMediaEntry>> {
        val token = getAccessToken().first()
            ?: return AppResult.Error(AppError.AuthenticationError("Not logged into AniList"))

        val userJson = settingsDataStore.aniListUserDataFlow.first()
        val user = parseUserFromJson(userJson)

        val userId = user?.id ?: run {
            val profileRes = fetchCurrentUserProfile()
            if (profileRes is AppResult.Success) profileRes.data.id else null
        } ?: return AppResult.Error(AppError.AuthenticationError("Failed to identify AniList user ID"))

        return aniListService.fetchUserMediaList(token, userId, status)
    }

    override suspend fun searchAniListMedia(query: String): AppResult<List<AniListMediaEntry>> {
        return aniListService.searchAniListMedia(query)
    }

    private fun serializeUserToJson(user: AniListUser): String {
        return JSONObject().apply {
            put("id", user.id)
            put("name", user.name)
            put("avatarUrl", user.avatarUrl)
            put("bannerUrl", user.bannerUrl)
            put("unreadNotificationCount", user.unreadNotificationCount)
            put("animeCount", user.animeCount)
            put("episodesWatched", user.episodesWatched)
            put("meanScore", user.meanScore)
            put("minutesWatched", user.minutesWatched)
        }.toString()
    }

    private fun parseUserFromJson(jsonStr: String?): AniListUser? {
        if (jsonStr.isNull_or_blank_safe()) return null
        return try {
            val json = JSONObject(jsonStr!!)
            AniListUser(
                id = json.getInt("id"),
                name = json.getString("name"),
                avatarUrl = json.optString("avatarUrl", null),
                bannerUrl = json.optString("bannerUrl", null),
                unreadNotificationCount = json.optInt("unreadNotificationCount", 0),
                animeCount = json.optInt("animeCount", 0),
                episodesWatched = json.optInt("episodesWatched", 0),
                meanScore = json.optDouble("meanScore", 0.0),
                minutesWatched = json.optInt("minutesWatched", 0)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun String?.isNull_or_blank_safe(): Boolean = this == null || this.trim().isEmpty()
}
