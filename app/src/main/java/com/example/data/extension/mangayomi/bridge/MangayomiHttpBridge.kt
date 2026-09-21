package com.example.data.extension.mangayomi.bridge

import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * JS-accessible HTTP response object.
 */
class HttpResponse(
    val body: String,
    val statusCode: Int,
    val headers: Map<String, String>,
    val ok: Boolean
) {
    fun text(): String = body
    fun json(): Any? = null
}

/**
 * HTTP bridge exposing standard HTTP operations to Mangayomi JavaScript extensions.
 * Reuses OkHttpClient with proper timeout, redirect, and header handling.
 */
class MangayomiHttpBridge(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    companion object {
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }

    /**
     * Perform HTTP GET request.
     */
    fun get(url: String, headersMap: Map<String, Any>? = null): HttpResponse {
        val requestBuilder = Request.Builder().url(url)
        val finalHeaders = mutableMapOf("User-Agent" to DEFAULT_USER_AGENT)

        headersMap?.forEach { (k, v) ->
            finalHeaders[k] = v.toString()
        }
        finalHeaders.forEach { (k, v) ->
            requestBuilder.header(k, v)
        }

        return executeRequest(requestBuilder.build())
    }

    /**
     * Perform HTTP POST request.
     */
    fun post(url: String, headersMap: Map<String, Any>? = null, bodyPayload: Any? = null): HttpResponse {
        val finalHeaders = mutableMapOf("User-Agent" to DEFAULT_USER_AGENT)
        headersMap?.forEach { (k, v) ->
            finalHeaders[k] = v.toString()
        }

        val contentType = finalHeaders["Content-Type"] ?: "application/x-www-form-urlencoded"
        val bodyString = when (bodyPayload) {
            is String -> bodyPayload
            is Map<*, *> -> {
                bodyPayload.entries.joinToString("&") { (k, v) ->
                    "${k.toString()}=${v.toString()}"
                }
            }
            null -> ""
            else -> bodyPayload.toString()
        }

        val mediaType = contentType.toMediaTypeOrNull()
        val requestBody = bodyString.toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)

        finalHeaders.forEach { (k, v) ->
            requestBuilder.header(k, v)
        }

        return executeRequest(requestBuilder.build())
    }

    private fun executeRequest(request: Request): HttpResponse {
        try {
            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string() ?: ""
                val headersMap = mutableMapOf<String, String>()
                for (name in response.headers.names()) {
                    headersMap[name] = response.header(name) ?: ""
                }
                return HttpResponse(
                    body = bodyText,
                    statusCode = response.code,
                    headers = headersMap,
                    ok = response.isSuccessful
                )
            }
        } catch (e: Exception) {
            return HttpResponse(
                body = "",
                statusCode = 0,
                headers = emptyMap(),
                ok = false
            )
        }
    }
}
