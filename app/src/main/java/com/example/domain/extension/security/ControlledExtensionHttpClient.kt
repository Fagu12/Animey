package com.example.domain.extension.security

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.model.ExtensionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Sandboxed, security-hardened HTTP client for extension network operations.
 */
interface ControlledExtensionHttpClient {
    suspend fun get(
        manifest: ExtensionManifest,
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 15_000L
    ): AppResult<String>

    suspend fun post(
        manifest: ExtensionManifest,
        url: String,
        body: String,
        contentType: String = "application/json",
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 15_000L
    ): AppResult<String>
}

class DefaultControlledExtensionHttpClient(
    private val policyEnforcer: ExtensionNetworkPolicyEnforcer = DefaultExtensionNetworkPolicyEnforcer(),
    private val baseOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) : ControlledExtensionHttpClient {

    // Sensitive headers that extensions must NEVER inject or override
    private val forbiddenHeaders = setOf(
        "authorization", "proxy-authorization", "cookie", "host"
    )

    override suspend fun get(
        manifest: ExtensionManifest,
        url: String,
        headers: Map<String, String>,
        timeoutMs: Long
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val policyDecision = policyEnforcer.evaluateRequest(manifest, url)
        if (policyDecision is NetworkPolicyDecision.Denied) {
            return@withContext AppResult.Error(
                AppError.SecurityViolation(policyDecision.reason)
            )
        }

        try {
            withTimeout(timeoutMs) {
                val sanitizedHeaders = sanitizeHeaders(manifest.id, headers)
                val request = Request.Builder()
                    .url(url)
                    .headers(sanitizedHeaders)
                    .get()
                    .build()

                baseOkHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withTimeout AppResult.Error(
                            AppError.NetworkError("HTTP ${response.code}: ${response.message}")
                        )
                    }
                    val bodyString = response.body?.string().orEmpty()
                    AppResult.Success(bodyString)
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            AppResult.Error(AppError.TimeoutError("Request to '$url' timed out after ${timeoutMs}ms", e))
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(message = "Network call failed: ${e.message}", cause = e))
        }
    }

    override suspend fun post(
        manifest: ExtensionManifest,
        url: String,
        body: String,
        contentType: String,
        headers: Map<String, String>,
        timeoutMs: Long
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val policyDecision = policyEnforcer.evaluateRequest(manifest, url)
        if (policyDecision is NetworkPolicyDecision.Denied) {
            return@withContext AppResult.Error(
                AppError.SecurityViolation(policyDecision.reason)
            )
        }

        try {
            withTimeout(timeoutMs) {
                val sanitizedHeaders = sanitizeHeaders(manifest.id, headers)
                val mediaType = contentType.toMediaTypeOrNull()
                val requestBody = body.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .headers(sanitizedHeaders)
                    .post(requestBody)
                    .build()

                baseOkHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withTimeout AppResult.Error(
                            AppError.NetworkError("HTTP ${response.code}: ${response.message}")
                        )
                    }
                    val bodyString = response.body?.string().orEmpty()
                    AppResult.Success(bodyString)
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            AppResult.Error(AppError.TimeoutError("POST request to '$url' timed out after ${timeoutMs}ms", e))
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(message = "Network POST failed: ${e.message}", cause = e))
        }
    }

    private fun sanitizeHeaders(extensionId: String, headers: Map<String, String>): Headers {
        val builder = Headers.Builder()
        builder.add("User-Agent", "Animey-Extension-Sandbox/1.0 (extension-id: $extensionId)")
        for ((key, value) in headers) {
            if (key.lowercase() !in forbiddenHeaders) {
                builder.add(key, value)
            }
        }
        return builder.build()
    }
}
