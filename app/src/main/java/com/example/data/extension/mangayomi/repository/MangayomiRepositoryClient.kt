package com.example.data.extension.mangayomi.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.extension.mangayomi.model.MangayomiExtensionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client capable of fetching real Mangayomi extension repository indices and downloading JavaScript extension sources.
 */
interface MangayomiRepositoryClient {
    suspend fun fetchRepositoryIndex(repoUrl: String): AppResult<List<MangayomiExtensionManifest>>
    suspend fun downloadExtensionSource(sourceCodeUrl: String): AppResult<String>
}

class DefaultMangayomiRepositoryClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) : MangayomiRepositoryClient {

    companion object {
        private const val TAG = "MangayomiRepoClient"
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) Animey/1.0 MangayomiClient"
    }

    override suspend fun fetchRepositoryIndex(repoUrl: String): AppResult<List<MangayomiExtensionManifest>> = withContext(Dispatchers.IO) {
        val normalizedUrl = repoUrl.trim()
        if (normalizedUrl.isBlank()) {
            return@withContext AppResult.Error(AppError.ValidationError("Repository URL cannot be blank"))
        }

        try {
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AppResult.Error(
                        AppError.NetworkError("Failed to fetch repository index: HTTP ${response.code}")
                    )
                }

                val bodyString = response.body?.string() ?: ""
                if (bodyString.isBlank()) {
                    return@withContext AppResult.Error(AppError.NetworkError("Empty repository response"))
                }

                val manifests = parseIndexJson(bodyString, normalizedUrl)
                AppResult.Success(manifests)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed fetching repo from $normalizedUrl", e)
            AppResult.Error(AppError.NetworkError("Repository index fetch failed: ${e.message}", cause = e))
        }
    }

    override suspend fun downloadExtensionSource(sourceCodeUrl: String): AppResult<String> = withContext(Dispatchers.IO) {
        val trimmedUrl = sourceCodeUrl.trim()
        if (trimmedUrl.isBlank()) {
            return@withContext AppResult.Error(AppError.ValidationError("Source code URL cannot be blank"))
        }

        try {
            val request = Request.Builder()
                .url(trimmedUrl)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "text/javascript, application/javascript, text/plain, */*")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AppResult.Error(
                        AppError.NetworkError("Failed to download extension source: HTTP ${response.code}", statusCode = response.code)
                    )
                }

                val code = response.body?.string() ?: ""
                if (code.isBlank()) {
                    return@withContext AppResult.Error(AppError.ValidationError("Downloaded extension script is empty"))
                }

                AppResult.Success(code)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed downloading extension script from $trimmedUrl", e)
            AppResult.Error(AppError.NetworkError("Failed downloading extension script: ${e.message}", cause = e))
        }
    }

    private fun parseIndexJson(jsonString: String, baseRepoUrl: String): List<MangayomiExtensionManifest> {
        val results = mutableListOf<MangayomiExtensionManifest>()
        val trimmed = jsonString.trim()

        try {
            val jsonArray = if (trimmed.startsWith("[")) {
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

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i) ?: continue
                val id = item.optString("id").ifBlank { item.optString("pkg") }
                val name = item.optString("name")
                if (id.isBlank() || name.isBlank()) continue

                val itemType = parseItemType(item)
                // Filter Anime extensions (itemType == 1 or explicit anime declaration)
                if (itemType != 1 && item.optBoolean("isManga", false) && !item.optBoolean("isAnime", false)) {
                    continue
                }

                val version = item.optString("version", "1.0.0")
                val versionCode = item.optInt("versionCode", 1)
                val lang = item.optString("lang", "en")
                val baseUrl = item.optString("baseUrl", "")
                val iconUrl = item.optString("iconUrl", item.optString("icon", ""))
                val description = item.optString("description", "")
                val author = item.optString("author", "")
                var sourceCodeUrl = item.optString("sourceCodeUrl", item.optString("sourceUrl", item.optString("url", "")))
                val sourceCode = item.optString("sourceCode", "")
                val hasCloudflare = item.optBoolean("hasCloudflare", false)
                val appMinVerReq = item.optString("appMinVerReq", "0.1.0")
                val sourceCodeLanguage = item.optInt("sourceCodeLanguage", 0)
                val notes = item.optString("notes", "")

                // Resolve relative source code URL against repository base
                if (sourceCodeUrl.isNotBlank() && !sourceCodeUrl.startsWith("http://") && !sourceCodeUrl.startsWith("https://")) {
                    val repoBase = baseRepoUrl.substringBeforeLast('/')
                    sourceCodeUrl = "$repoBase/$sourceCodeUrl"
                }

                results.add(
                    MangayomiExtensionManifest(
                        id = id,
                        name = name,
                        lang = lang,
                        version = version,
                        versionCode = versionCode,
                        baseUrl = baseUrl,
                        iconUrl = iconUrl,
                        description = description,
                        author = author,
                        itemType = itemType,
                        sourceCodeUrl = sourceCodeUrl,
                        sourceCodeLanguage = sourceCodeLanguage,
                        appMinVerReq = appMinVerReq,
                        notes = notes,
                        hasCloudflare = hasCloudflare,
                        scriptContent = sourceCode
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed parsing index JSON", e)
        }

        return results
    }

    private fun parseItemType(item: JSONObject): Int {
        if (item.has("itemType")) {
            return when (val type = item.get("itemType")) {
                is Number -> type.toInt()
                is String -> when (type.lowercase()) {
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
        return 1 // Default to 1 (Anime)
    }
}
