package com.example.data.tracking.anilist

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.model.tracking.AniListMediaEntry
import com.example.domain.model.tracking.AniListUser
import com.example.domain.model.tracking.TrackStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AniListService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val graphqlUrl = "https://graphql.anilist.co"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchViewerProfile(accessToken: String): AppResult<AniListUser> = withContext(Dispatchers.IO) {
        AppLogger.d("AniListService", "Fetching viewer profile from AniList API...")
        val query = """
            query {
              Viewer {
                id
                name
                avatar { large medium }
                bannerImage
                unreadNotificationCount
                statistics {
                  anime {
                    count
                    episodesWatched
                    meanScore
                    minutesWatched
                  }
                }
              }
            }
        """.trimIndent()

        val responseResult = executeGraphQL(query, JSONObject(), accessToken)
        return@withContext when (responseResult) {
            is AppResult.Error -> responseResult
            is AppResult.Success -> {
                try {
                    val dataObj = responseResult.data.optJSONObject("data")
                    val viewerObj = dataObj?.optJSONObject("Viewer")
                        ?: return@withContext AppResult.Error(AppError.NetworkError("User not found in response"))

                    val statsObj = viewerObj.optJSONObject("statistics")?.optJSONObject("anime")

                    val user = AniListUser(
                        id = viewerObj.getInt("id"),
                        name = viewerObj.getString("name"),
                        avatarUrl = viewerObj.optJSONObject("avatar")?.optString("large"),
                        bannerUrl = viewerObj.optString("bannerImage", null),
                        unreadNotificationCount = viewerObj.optInt("unreadNotificationCount", 0),
                        animeCount = statsObj?.optInt("count", 0) ?: 0,
                        episodesWatched = statsObj?.optInt("episodesWatched", 0) ?: 0,
                        meanScore = statsObj?.optDouble("meanScore", 0.0) ?: 0.0,
                        minutesWatched = statsObj?.optInt("minutesWatched", 0) ?: 0
                    )
                    AppLogger.d("AniListService", "Successfully fetched user profile for ${user.name}")
                    AppResult.Success(user)
                } catch (e: Exception) {
                    AppLogger.e("AniListService", "Failed to parse viewer response", e)
                    AppResult.Error(AppError.ValidationError("Failed to parse AniList user profile", e))
                }
            }
            else -> AppResult.Error(AppError.GeneralError("Unexpected state during viewer profile request"))
        }
    }

    suspend fun saveMediaListEntry(
        accessToken: String,
        mediaId: Int,
        status: TrackStatus,
        progress: Int,
        score: Double?
    ): AppResult<AniListMediaEntry> = withContext(Dispatchers.IO) {
        AppLogger.d("AniListService", "Saving media list entry: mediaId=$mediaId, status=${status.aniListStatus}, progress=$progress")
        val mutation = """
            mutation (${"$"}mediaId: Int, ${"$"}status: MediaListStatus, ${"$"}progress: Int, ${"$"}score: Float) {
              SaveMediaListEntry (mediaId: ${"$"}mediaId, status: ${"$"}status, progress: ${"$"}progress, score: ${"$"}score) {
                id
                mediaId
                status
                progress
                score
                repeat
                updatedAt
                media {
                  id
                  title { userPreferred english romaji }
                  coverImage { large }
                  episodes
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("mediaId", mediaId)
            put("status", status.aniListStatus)
            put("progress", progress)
            if (score != null && score > 0) {
                put("score", score)
            }
        }

        val responseResult = executeGraphQL(mutation, variables, accessToken)
        return@withContext when (responseResult) {
            is AppResult.Error -> responseResult
            is AppResult.Success -> {
                try {
                    val entryObj = responseResult.data.optJSONObject("data")?.optJSONObject("SaveMediaListEntry")
                        ?: return@withContext AppResult.Error(AppError.NetworkError("Failed to update AniList media entry"))

                    val entry = parseMediaListEntry(entryObj)
                    AppLogger.d("AniListService", "Successfully saved media entry for mediaId=$mediaId")
                    AppResult.Success(entry)
                } catch (e: Exception) {
                    AppLogger.e("AniListService", "Failed to parse save entry response", e)
                    AppResult.Error(AppError.ValidationError("Failed to parse saved entry response", e))
                }
            }
            else -> AppResult.Error(AppError.GeneralError("Unexpected state during save media list entry"))
        }
    }

    suspend fun fetchMediaListEntry(
        accessToken: String,
        mediaId: Int
    ): AppResult<AniListMediaEntry?> = withContext(Dispatchers.IO) {
        val query = """
            query (${"$"}mediaId: Int) {
              MediaList (mediaId: ${"$"}mediaId) {
                id
                mediaId
                status
                progress
                score
                repeat
                updatedAt
                media {
                  id
                  title { userPreferred english romaji }
                  coverImage { large }
                  episodes
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("mediaId", mediaId) }
        val responseResult = executeGraphQL(query, variables, accessToken)

        return@withContext when (responseResult) {
            is AppResult.Error -> AppResult.Success(null) // Non-fatal if entry doesn't exist
            is AppResult.Success -> {
                try {
                    val entryObj = responseResult.data.optJSONObject("data")?.optJSONObject("MediaList")
                    if (entryObj == null) {
                        AppResult.Success(null)
                    } else {
                        AppResult.Success(parseMediaListEntry(entryObj))
                    }
                } catch (e: Exception) {
                    AppResult.Success(null)
                }
            }
            else -> AppResult.Success(null)
        }
    }

    suspend fun fetchUserMediaList(
        accessToken: String,
        userId: Int,
        status: TrackStatus?
    ): AppResult<List<AniListMediaEntry>> = withContext(Dispatchers.IO) {
        val query = """
            query (${"$"}userId: Int, ${"$"}status: MediaListStatus) {
              MediaListCollection (userId: ${"$"}userId, type: ANIME, status: ${"$"}status) {
                lists {
                  entries {
                    id
                    mediaId
                    status
                    progress
                    score
                    repeat
                    updatedAt
                    media {
                      id
                      title { userPreferred english romaji }
                      coverImage { large }
                      episodes
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("userId", userId)
            if (status != null) {
                put("status", status.aniListStatus)
            }
        }

        val responseResult = executeGraphQL(query, variables, accessToken)
        return@withContext when (responseResult) {
            is AppResult.Error -> responseResult
            is AppResult.Success -> {
                try {
                    val listsArray = responseResult.data
                        .optJSONObject("data")
                        ?.optJSONObject("MediaListCollection")
                        ?.optJSONArray("lists")

                    val entries = mutableListOf<AniListMediaEntry>()
                    if (listsArray != null) {
                        for (i in 0 until listsArray.length()) {
                            val listObj = listsArray.getJSONObject(i)
                            val entriesArray = listObj.optJSONArray("entries") ?: JSONArray()
                            for (j in 0 until entriesArray.length()) {
                                val entryObj = entriesArray.getJSONObject(j)
                                entries.add(parseMediaListEntry(entryObj))
                            }
                        }
                    }
                    AppResult.Success(entries)
                } catch (e: Exception) {
                    AppLogger.e("AniListService", "Failed to parse user media list", e)
                    AppResult.Error(AppError.ValidationError("Failed to parse media list", e))
                }
            }
            else -> AppResult.Error(AppError.GeneralError("Unexpected state during fetch user media list"))
        }
    }

    suspend fun searchAniListMedia(
        queryStr: String
    ): AppResult<List<AniListMediaEntry>> = withContext(Dispatchers.IO) {
        val query = """
            query (${"$"}search: String) {
              Page (page: 1, perPage: 10) {
                media (search: ${"$"}search, type: ANIME) {
                  id
                  title { userPreferred english romaji }
                  coverImage { large }
                  episodes
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("search", queryStr) }
        val responseResult = executeGraphQL(query, variables, accessToken = null)

        return@withContext when (responseResult) {
            is AppResult.Error -> responseResult
            is AppResult.Success -> {
                try {
                    val mediaArray = responseResult.data
                        .optJSONObject("data")
                        ?.optJSONObject("Page")
                        ?.optJSONArray("media") ?: JSONArray()

                    val list = mutableListOf<AniListMediaEntry>()
                    for (i in 0 until mediaArray.length()) {
                        val mObj = mediaArray.getJSONObject(i)
                        val mediaId = mObj.getInt("id")
                        val titleObj = mObj.optJSONObject("title")
                        val title = titleObj?.optString("english")
                            ?: titleObj?.optString("userPreferred")
                            ?: titleObj?.optString("romaji")
                            ?: "Anime $mediaId"
                        val cover = mObj.optJSONObject("coverImage")?.optString("large")
                        val epCount = if (mObj.has("episodes") && !mObj.isNull("episodes")) mObj.getInt("episodes") else null

                        list.add(
                            AniListMediaEntry(
                                mediaId = mediaId,
                                title = title,
                                coverImage = cover,
                                totalEpisodes = epCount
                            )
                        )
                    }
                    AppResult.Success(list)
                } catch (e: Exception) {
                    AppResult.Error(AppError.ValidationError("Failed to parse search results", e))
                }
            }
            else -> AppResult.Error(AppError.GeneralError("Unexpected state during search media"))
        }
    }

    private fun executeGraphQL(
        query: String,
        variables: JSONObject,
        accessToken: String?
    ): AppResult<JSONObject> {
        val payload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val requestBuilder = Request.Builder()
            .url(graphqlUrl)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("Accept", "application/json")

        if (!accessToken.isNull_or_blank_safe()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        return try {
            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                AppLogger.e("AniListService", "GraphQL call failed with status ${response.code}")
                AppResult.Error(AppError.NetworkError("AniList server returned HTTP ${response.code}", statusCode = response.code))
            } else {
                val json = JSONObject(responseBody)
                if (json.has("errors")) {
                    val errorsArr = json.getJSONArray("errors")
                    val errMsg = if (errorsArr.length() > 0) errorsArr.getJSONObject(0).optString("message", "Unknown GraphQL error") else "GraphQL error"
                    AppLogger.e("AniListService", "GraphQL error: $errMsg")
                    AppResult.Error(AppError.NetworkError(errMsg))
                } else {
                    AppResult.Success(json)
                }
            }
        } catch (e: IOException) {
            AppLogger.e("AniListService", "Network IO error during GraphQL call", e)
            AppResult.Error(AppError.NetworkError("Network error connecting to AniList", cause = e))
        } catch (e: Exception) {
            AppLogger.e("AniListService", "Unexpected exception during GraphQL call", e)
            AppResult.Error(AppError.UnknownError("AniList error: ${e.message}", e))
        }
    }

    private fun parseMediaListEntry(obj: JSONObject): AniListMediaEntry {
        val mediaId = obj.getInt("mediaId")
        val statusStr = obj.optString("status", "CURRENT")
        val progress = obj.optInt("progress", 0)
        val score = obj.optDouble("score", 0.0)
        val repeat = obj.optInt("repeat", 0)
        val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis() / 1000) * 1000

        val mediaObj = obj.optJSONObject("media")
        val titleObj = mediaObj?.optJSONObject("title")
        val title = titleObj?.optString("english")
            ?: titleObj?.optString("userPreferred")
            ?: titleObj?.optString("romaji")
            ?: "Media $mediaId"
        val cover = mediaObj?.optJSONObject("coverImage")?.optString("large")
        val epCount = if (mediaObj != null && mediaObj.has("episodes") && !mediaObj.isNull("episodes")) mediaObj.getInt("episodes") else null

        return AniListMediaEntry(
            mediaId = mediaId,
            title = title,
            coverImage = cover,
            status = TrackStatus.fromAniListStatus(statusStr),
            progress = progress,
            totalEpisodes = epCount,
            score = score,
            repeatCount = repeat,
            updatedAt = updatedAt
        )
    }

    private fun String?.isNull_or_blank_safe(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
