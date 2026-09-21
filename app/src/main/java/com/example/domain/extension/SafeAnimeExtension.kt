package com.example.domain.extension

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.VideoSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

/**
 * Safety decorator for AnimeExtension that guarantees:
 * 1. Exception containment (No crash can escape into UI or calling coroutines).
 * 2. Execution timeout protection (prevents runaway requests).
 * 3. Consistent AppResult contract.
 */
class SafeAnimeExtension(
    private val delegate: AnimeExtension,
    private val timeoutMs: Long = 20_000L
) : AnimeExtension {

    override val id: String = delegate.id
    override val name: String = delegate.name
    override val lang: String = delegate.lang
    override val iconUrl: String = delegate.iconUrl
    override val manifest: ExtensionManifest = delegate.manifest
    override val baseUrl: String = delegate.baseUrl

    private suspend fun <T> runSafely(
        actionName: String,
        block: suspend () -> AppResult<T>
    ): AppResult<T> {
        return try {
            withTimeout(timeoutMs) {
                block()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            AppLogger.w("SafeExtension", "[$name] Action $actionName timed out after ${timeoutMs}ms")
            AppResult.Error(AppError.TimeoutError("Extension '$name' timed out during $actionName", e))
        } catch (t: Throwable) {
            AppLogger.e("SafeExtension", "[$name] Action $actionName failed with exception", t)
            AppResult.Error(
                AppError.ProviderError(
                    provider = name,
                    message = t.message ?: "Unknown error in extension '$name' during $actionName",
                    cause = t
                )
            )
        }
    }

    override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
        return runSafely("getPopularAnime(page=$page)") {
            delegate.getPopularAnime(page)
        }
    }

    override suspend fun getLatestAnime(page: Int): AppResult<List<Anime>> {
        return runSafely("getLatestAnime(page=$page)") {
            delegate.getLatestAnime(page)
        }
    }

    override suspend fun searchAnime(
        query: String,
        page: Int,
        filters: Map<String, Any>
    ): AppResult<List<Anime>> {
        return runSafely("searchAnime(query=$query, page=$page)") {
            delegate.searchAnime(query, page, filters)
        }
    }

    override suspend fun getAnimeDetails(sourceAnimeId: String): AppResult<Anime> {
        return runSafely("getAnimeDetails(id=$sourceAnimeId)") {
            delegate.getAnimeDetails(sourceAnimeId)
        }
    }

    override suspend fun getEpisodes(sourceAnimeId: String): AppResult<List<Episode>> {
        return runSafely("getEpisodes(id=$sourceAnimeId)") {
            delegate.getEpisodes(sourceAnimeId)
        }
    }

    override suspend fun getEpisodeStreams(
        sourceEpisodeId: String,
        extra: Map<String, String>
    ): AppResult<List<VideoSource>> {
        return runSafely("getEpisodeStreams(id=$sourceEpisodeId)") {
            delegate.getEpisodeStreams(sourceEpisodeId, extra)
        }
    }

    override suspend fun getPreferences(): List<ExtensionPreference> {
        return try {
            delegate.getPreferences()
        } catch (t: Throwable) {
            AppLogger.w("SafeExtension", "Failed to load preferences for $name: ${t.message}")
            emptyList()
        }
    }

    override suspend fun setPreference(key: String, value: Any) {
        try {
            delegate.setPreference(key, value)
        } catch (t: Throwable) {
            AppLogger.w("SafeExtension", "Failed to set preference $key for $name: ${t.message}")
        }
    }
}
