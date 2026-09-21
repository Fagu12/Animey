package com.example.data.extension.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionType
import com.example.domain.model.RepositoryIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface RepositoryFetcher {
    suspend fun fetchRepositoryIndex(url: String): AppResult<RepositoryIndex>
    fun parseIndexJson(json: String, baseRepoUrl: String = ""): AppResult<RepositoryIndex>
}

class DefaultRepositoryFetcher(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) : RepositoryFetcher {

    companion object {
        private const val TAG = "RepositoryFetcher"
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) Animey/1.0"
    }

    override suspend fun fetchRepositoryIndex(url: String): AppResult<RepositoryIndex> = withContext(Dispatchers.IO) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            return@withContext AppResult.Error(AppError.ValidationError("Repository URL cannot be blank"))
        }

        try {
            val request = Request.Builder()
                .url(trimmedUrl)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AppResult.Error(
                        AppError.NetworkError("HTTP ${response.code}: ${response.message}")
                    )
                }

                val body = response.body?.string()
                    ?: return@withContext AppResult.Error(AppError.NetworkError("Empty repository response"))

                parseIndexJson(body, trimmedUrl)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed fetching repository index from $trimmedUrl", e)
            AppResult.Error(AppError.NetworkError("Failed to fetch repository index from $trimmedUrl: ${e.message}", cause = e))
        }
    }

    override fun parseIndexJson(json: String, baseRepoUrl: String): AppResult<RepositoryIndex> {
        val trimmed = json.trim()
        if (trimmed.isBlank()) {
            return AppResult.Error(AppError.ValidationError("Empty repository JSON payload"))
        }

        return try {
            val jsonArray: JSONArray = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("extensions")
                    ?: obj.optJSONArray("anime")
                    ?: obj.optJSONArray("sources")
                    ?: JSONArray()
            } else {
                JSONArray()
            }

            val totalEntries = jsonArray.length()
            val extensions = mutableListOf<ExtensionManifest>()

            for (i in 0 until totalEntries) {
                val item = jsonArray.optJSONObject(i) ?: continue

                // Extract stable ID (string or number, e.g. 1183439094 or "hianime" or "pkg")
                val rawId = item.opt("id") ?: item.opt("pkg") ?: continue
                val idStr = rawId.toString().trim()

                val name = item.optString("name", "").trim()
                val baseUrl = item.optString("baseUrl", "").trim()
                val lang = item.optString("lang", item.optString("language", "en")).trim()
                val version = item.optString("version", "1.0.0").trim()

                var sourceCodeUrl = item.optString("sourceCodeUrl",
                    item.optString("downloadUrl",
                        item.optString("sourceUrl",
                            item.optString("url", "")
                        )
                    )
                ).trim()

                // Resolve relative source URL against repository base URL
                if (sourceCodeUrl.isNotBlank() && !sourceCodeUrl.startsWith("http://") && !sourceCodeUrl.startsWith("https://")) {
                    if (baseRepoUrl.isNotBlank()) {
                        val repoBase = baseRepoUrl.substringBeforeLast('/')
                        sourceCodeUrl = "$repoBase/$sourceCodeUrl"
                    }
                }

                val isManga = item.optBoolean("isManga", false)
                val itemType = parseItemType(item)

                // Required fields check: name, id, baseUrl, lang, version, sourceCodeUrl
                if (idStr.isBlank() || name.isBlank() || sourceCodeUrl.isBlank()) {
                    continue
                }

                // Filter rule: isManga == false OR itemType == 1 (Anime)
                if (isManga && itemType != 1) {
                    continue
                }

                val iconUrl = item.optString("iconUrl", item.optString("icon", "")).trim()
                val description = item.optString("description", item.optString("notes", "")).trim()
                val author = item.optString("author", "Community").trim()
                val appMinVerReq = item.optString("appMinVerReq", item.optString("minAppVersion", "0.1.0")).trim()
                val isNsfw = item.optBoolean("isNsfw", false)

                extensions.add(
                    ExtensionManifest(
                        id = idStr,
                        name = name,
                        version = version,
                        language = lang,
                        type = ExtensionType.ANIME,
                        iconUrl = iconUrl,
                        description = description,
                        downloadUrl = sourceCodeUrl,
                        sourceCodeUrl = sourceCodeUrl,
                        baseUrl = baseUrl,
                        minAppVersion = appMinVerReq,
                        author = author,
                        isInstalled = false,
                        isEnabled = true,
                        hasUpdate = false,
                        isBuiltIn = false,
                        nsfw = isNsfw
                    )
                )
            }

            AppLogger.d(TAG, "Parsed repository: total=$totalEntries, validAnime=${extensions.size}")

            AppResult.Success(
                RepositoryIndex(
                    name = "Mallyd11 Mangayomi Anime Repository",
                    version = 1,
                    extensions = extensions
                )
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Invalid repository index JSON format", e)
            AppResult.Error(AppError.GeneralError("Invalid repository index format: ${e.message}", e))
        }
    }

    private fun parseItemType(item: JSONObject): Int {
        if (item.has("itemType")) {
            return when (val type = item.get("itemType")) {
                is Number -> type.toInt()
                is String -> when (type.lowercase().trim()) {
                    "anime" -> 1
                    "manga" -> 0
                    "novel" -> 2
                    else -> 1
                }
                else -> 1
            }
        }
        if (item.optBoolean("isAnime", false)) return 1
        if (item.optBoolean("isManga", false)) return 0
        return 1
    }
}

