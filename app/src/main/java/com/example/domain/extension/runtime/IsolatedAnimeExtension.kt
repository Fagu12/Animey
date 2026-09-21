package com.example.domain.extension.runtime

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.extension.AnimeExtension
import com.example.domain.extension.logging.CircuitBreakerState
import com.example.domain.extension.logging.ExtensionLogger
import com.example.domain.extension.logging.ExtensionRuntimeStats
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.VideoSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

/**
 * Robust isolation wrapper around an AnimeExtension instance.
 * Features:
 * 1. Failure isolation & exception containment (prevents crashes from leaking into the core application).
 * 2. Timeout protection (default 20 seconds).
 * 3. Circuit breaker pattern: if an extension fails 5 times consecutively, subsequent calls are fast-failed
 *    for 30 seconds before attempting half-open recovery.
 * 4. Extension-scoped structured telemetry and logs.
 */
class IsolatedAnimeExtension(
    private val delegate: AnimeExtension,
    private val logger: ExtensionLogger? = null,
    private val timeoutMs: Long = 20_000L,
    private val maxConsecutiveFailures: Int = 5,
    private val circuitCooldownMs: Long = 30_000L
) : AnimeExtension {

    override val id: String = delegate.id
    override val name: String = delegate.name
    override val lang: String = delegate.lang
    override val iconUrl: String = delegate.iconUrl
    override val manifest: ExtensionManifest = delegate.manifest
    override val baseUrl: String = delegate.baseUrl

    @Volatile
    private var stats = ExtensionRuntimeStats(extensionId = delegate.id)

    @Volatile
    private var circuitOpenedAt: Long = 0L

    fun getStats(): ExtensionRuntimeStats = stats

    private fun checkCircuitBreaker(): AppResult<Unit>? {
        val currentStats = stats
        if (currentStats.circuitState == CircuitBreakerState.OPEN) {
            val now = System.currentTimeMillis()
            if (now - circuitOpenedAt > circuitCooldownMs) {
                // Cooldown elapsed, test with half-open
                stats = currentStats.copy(circuitState = CircuitBreakerState.HALF_OPEN)
                logger?.w(id, "CircuitBreaker", "Circuit entering HALF_OPEN state (attempting recovery)")
            } else {
                val remainingSeconds = (circuitCooldownMs - (now - circuitOpenedAt)) / 1000
                logger?.w(id, "CircuitBreaker", "Request rejected by OPEN circuit breaker ($remainingSeconds s cooldown left)")
                return AppResult.Error(
                    AppError.ProviderError(
                        provider = name,
                        message = "Extension '$name' is temporarily paused due to repeated failures. Cooldown remaining: ${remainingSeconds}s"
                    )
                )
            }
        }
        return null
    }

    private fun recordSuccess(durationMs: Long) {
        val s = stats
        stats = s.copy(
            totalRequests = s.totalRequests + 1,
            successfulRequests = s.successfulRequests + 1,
            totalLatencyMs = s.totalLatencyMs + durationMs,
            consecutiveFailures = 0,
            circuitState = CircuitBreakerState.CLOSED,
            lastActivityTimestamp = System.currentTimeMillis()
        )
    }

    private fun recordFailure(durationMs: Long, errorMessage: String) {
        val s = stats
        val newConsecutiveFailures = s.consecutiveFailures + 1
        val shouldOpen = newConsecutiveFailures >= maxConsecutiveFailures
        val newState = if (shouldOpen) CircuitBreakerState.OPEN else s.circuitState

        if (shouldOpen && s.circuitState != CircuitBreakerState.OPEN) {
            circuitOpenedAt = System.currentTimeMillis()
            logger?.e(id, "CircuitBreaker", "Circuit tripped to OPEN after $newConsecutiveFailures consecutive failures")
        }

        stats = s.copy(
            totalRequests = s.totalRequests + 1,
            failedRequests = s.failedRequests + 1,
            totalLatencyMs = s.totalLatencyMs + durationMs,
            consecutiveFailures = newConsecutiveFailures,
            circuitState = newState,
            lastFailureMessage = errorMessage,
            lastActivityTimestamp = System.currentTimeMillis()
        )
    }

    private suspend fun <T> executeSafely(
        actionName: String,
        block: suspend () -> AppResult<T>
    ): AppResult<T> {
        val circuitCheck = checkCircuitBreaker()
        if (circuitCheck != null) {
            @Suppress("UNCHECKED_CAST")
            return circuitCheck as AppResult<T>
        }

        val startTime = System.currentTimeMillis()
        logger?.d(id, "Execution", "Starting operation: $actionName")

        return try {
            val result = withTimeout(timeoutMs) {
                block()
            }
            val elapsed = System.currentTimeMillis() - startTime

            when (result) {
                is AppResult.Success -> {
                    recordSuccess(elapsed)
                    logger?.i(id, "Execution", "Completed $actionName successfully in ${elapsed}ms")
                    result
                }
                is AppResult.Error -> {
                    recordFailure(elapsed, result.error.message)
                    logger?.w(id, "Execution", "$actionName returned error in ${elapsed}ms: ${result.error.message}")
                    result
                }
                is AppResult.Loading -> result
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val elapsed = System.currentTimeMillis() - startTime
            recordFailure(elapsed, "Timeout after ${timeoutMs}ms")
            logger?.e(id, "Execution", "$actionName timed out after ${timeoutMs}ms", e)
            AppResult.Error(AppError.TimeoutError("Extension '$name' timed out during $actionName", e))
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val elapsed = System.currentTimeMillis() - startTime
            val msg = t.message ?: "Unknown crash in extension '$name'"
            recordFailure(elapsed, msg)
            logger?.e(id, "Execution", "$actionName crashed: $msg", t)
            AppResult.Error(
                AppError.ProviderError(
                    provider = name,
                    message = msg,
                    cause = t
                )
            )
        }
    }

    override suspend fun getPopularAnime(page: Int): AppResult<List<Anime>> {
        return executeSafely("getPopularAnime(page=$page)") {
            delegate.getPopularAnime(page)
        }
    }

    override suspend fun getLatestAnime(page: Int): AppResult<List<Anime>> {
        return executeSafely("getLatestAnime(page=$page)") {
            delegate.getLatestAnime(page)
        }
    }

    override suspend fun searchAnime(
        query: String,
        page: Int,
        filters: Map<String, Any>
    ): AppResult<List<Anime>> {
        return executeSafely("searchAnime(query='$query', page=$page)") {
            delegate.searchAnime(query, page, filters)
        }
    }

    override suspend fun getAnimeDetails(sourceAnimeId: String): AppResult<Anime> {
        return executeSafely("getAnimeDetails(sourceAnimeId='$sourceAnimeId')") {
            delegate.getAnimeDetails(sourceAnimeId)
        }
    }

    override suspend fun getEpisodes(sourceAnimeId: String): AppResult<List<Episode>> {
        return executeSafely("getEpisodes(sourceAnimeId='$sourceAnimeId')") {
            delegate.getEpisodes(sourceAnimeId)
        }
    }

    override suspend fun getEpisodeStreams(
        sourceEpisodeId: String,
        extra: Map<String, String>
    ): AppResult<List<VideoSource>> {
        return executeSafely("getEpisodeStreams(sourceEpisodeId='$sourceEpisodeId')") {
            delegate.getEpisodeStreams(sourceEpisodeId, extra)
        }
    }

    override suspend fun getPreferences(): List<ExtensionPreference> {
        return try {
            delegate.getPreferences()
        } catch (t: Throwable) {
            logger?.w(id, "Preferences", "Failed to retrieve preferences: ${t.message}", t)
            emptyList()
        }
    }

    override suspend fun setPreference(key: String, value: Any) {
        try {
            delegate.setPreference(key, value)
            logger?.d(id, "Preferences", "Updated preference '$key'")
        } catch (t: Throwable) {
            logger?.e(id, "Preferences", "Failed to set preference '$key': ${t.message}", t)
        }
    }
}
