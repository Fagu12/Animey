package com.example.data.extension.mangayomi.runtime

import com.example.core.logging.AppLogger
import com.example.data.extension.mangayomi.bridge.DomDocumentWrapper
import com.example.data.extension.mangayomi.bridge.MangayomiCryptoBridge
import com.example.data.extension.mangayomi.bridge.MangayomiStorageBridge
import com.example.data.extension.mangayomi.model.MangayomiExtensionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import app.cash.quickjs.QuickJs
import java.io.Closeable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

interface QuickJsHttpBridge {
    fun get(url: String, headersJson: String?): String
    fun post(url: String, headersJson: String?, body: String?): String
}

class MangayomiExecutionContext(
    val manifest: MangayomiExtensionManifest,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) : Closeable {

    private val isClosed = AtomicBoolean(false)
    private var quickJs: QuickJs? = null

    init {
        val qjs = QuickJs.create()

        val httpBridgeImpl = object : QuickJsHttpBridge {
            override fun get(url: String, headersJson: String?): String {
                val reqBuilder = Request.Builder().url(url)
                reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                if (!headersJson.isNullOrBlankJson()) {
                    parseJsonToMap(headersJson)?.forEach { (k, v) -> reqBuilder.header(k, v.toString()) }
                }
                val req = reqBuilder.build()
                return executeHttpAndFormatJson(okHttpClient, req)
            }

            override fun post(url: String, headersJson: String?, body: String?): String {
                val reqBuilder = Request.Builder().url(url)
                reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                val mediaType = "application/x-www-form-urlencoded".toMediaTypeOrNull()
                reqBuilder.post((body ?: "").toRequestBody(mediaType))
                if (!headersJson.isNullOrBlankJson()) {
                    parseJsonToMap(headersJson)?.forEach { (k, v) -> reqBuilder.header(k, v.toString()) }
                }
                return executeHttpAndFormatJson(okHttpClient, reqBuilder.build())
            }
        }

        qjs.set("NativeHttpBridge", QuickJsHttpBridge::class.java, httpBridgeImpl)

        val fullPreamble = """
            class Source {}
            class Extension {}
            class MProvider {}
            var exports = {};
            var module = { exports: exports };

            class Client {
                constructor() {}
                get(url, headers) {
                    var hJson = headers ? JSON.stringify(headers) : "{}";
                    var respStr = NativeHttpBridge.get(url, hJson);
                    return JSON.parse(respStr);
                }
                post(url, headers, body) {
                    var hJson = headers ? JSON.stringify(headers) : "{}";
                    var bStr = body ? (typeof body === 'string' ? body : JSON.stringify(body)) : "";
                    var respStr = NativeHttpBridge.post(url, hJson, bStr);
                    return JSON.parse(respStr);
                }
            }
            var http = new Client();

            function Document(html) {
                return {
                    select: function(selector) { return []; },
                    selectFirst: function(selector) { return null; },
                    querySelectorAll: function(selector) { return []; },
                    querySelector: function(selector) { return null; }
                };
            }
            function parseHtml(html) { return Document(html); }
            function DOMParser() {
                this.parseFromString = function(str, mime) { return Document(str); };
            }

            var console = {
                log: function() {},
                info: function() {},
                warn: function() {},
                error: function() {}
            };
        """.trimIndent()

        qjs.evaluate(fullPreamble)

        if (manifest.scriptContent.isNotBlank()) {
            qjs.evaluate(manifest.scriptContent)
        }

        qjs.evaluate("""
            var source = null;
            if (typeof DefaultExtension !== 'undefined') {
                source = new DefaultExtension();
            } else if (typeof mangayomiSources !== 'undefined' && Array.isArray(mangayomiSources) && mangayomiSources.length > 0) {
                var firstSrc = mangayomiSources[0];
                if (typeof firstSrc === 'function') {
                    try { source = new firstSrc(); } catch(e) { source = firstSrc; }
                } else { source = firstSrc; }
            }
        """.trimIndent())

        quickJs = qjs
        AppLogger.i("Mangayomi", "QuickJS ExecutionContext initialized for extension '${manifest.name}' (${manifest.id})")
    }

    suspend fun getPreferences(timeoutMs: Long = 5000L): List<Map<String, Any?>> {
        return try {
            invokeJsFunction("getSourcePreferences", emptyArray(), timeoutMs)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun setPreference(key: String, value: Any, timeoutMs: Long = 5000L) {
        try {
            invokeJsFunction("setPreference", arrayOf(key, value), timeoutMs)
        } catch (ignored: Exception) {}
    }

    suspend fun getPopular(page: Int, timeoutMs: Long = 15000L): List<Map<String, Any?>> {
        return invokeJsFunction("getPopular", arrayOf(page), timeoutMs)
    }

    suspend fun getLatestUpdates(page: Int, timeoutMs: Long = 15000L): List<Map<String, Any?>> {
        return invokeJsFunction("getLatestUpdates", arrayOf(page), timeoutMs)
    }

    suspend fun search(
        query: String,
        page: Int = 1,
        filters: Map<String, Any> = emptyMap(),
        timeoutMs: Long = 15000L
    ): List<Map<String, Any?>> {
        return invokeJsFunction("search", arrayOf(query, page, filters), timeoutMs)
    }

    suspend fun getDetail(url: String, timeoutMs: Long = 15000L): Map<String, Any?> {
        val list = invokeJsFunction("getDetail", arrayOf(url), timeoutMs)
        return list.firstOrNull() ?: emptyMap()
    }

    suspend fun getVideoList(url: String, timeoutMs: Long = 15000L): List<Map<String, Any?>> {
        return invokeJsFunction("getVideoList", arrayOf(url), timeoutMs)
    }

    private suspend fun invokeJsFunction(
        functionName: String,
        args: Array<Any?>,
        timeoutMs: Long
    ): List<Map<String, Any?>> = withContext(Dispatchers.IO) {
        if (isClosed.get()) {
            throw IllegalStateException("ExecutionContext for ${manifest.id} is closed")
        }
        val qjs = quickJs ?: throw IllegalStateException("QuickJS is uninitialized")

        withTimeout(timeoutMs) {
            val argsJson = args.joinToString(",") { arg ->
                when (arg) {
                    null -> "null"
                    is Number, is Boolean -> arg.toString()
                    is String -> "\"${escapeJson(arg)}\""
                    else -> "\"${escapeJson(arg.toString())}\""
                }
            }

            val script = """
                (function() {
                    try {
                        if (!source) return JSON.stringify({ error: "No source instance" });
                        var fn = source["$functionName"] || window["$functionName"];
                        if (typeof fn !== 'function') {
                            return JSON.stringify({ error: "Function '$functionName' not found" });
                        }
                        var res = fn.apply(source, [$argsJson]);
                        return JSON.stringify({ success: true, data: res });
                    } catch(e) {
                        return JSON.stringify({ error: String(e) });
                    }
                })()
            """.trimIndent()

            val rawResultStr = qjs.evaluate(script)?.toString() ?: "{}"
            val resMap = parseJsonToMap(rawResultStr)

            val data = resMap?.get("data")
            when (data) {
                is List<*> -> data.filterIsInstance<Map<String, Any?>>()
                is Map<*, *> -> listOf(data as Map<String, Any?>)
                else -> emptyList()
            }
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            quickJs?.close()
            quickJs = null
        }
    }

    companion object {
        private fun executeHttpAndFormatJson(client: OkHttpClient, request: Request): String {
            return try {
                client.newCall(request).execute().use { resp ->
                    val bodyText = resp.body?.string() ?: ""
                    AppLogger.i("Mangayomi", "[MANGAYOMI][HTTP] method=${request.method} url=${request.url} status=${resp.code} responseBytes=${bodyText.length}")
                    val headersObj = StringBuilder("{")
                    resp.headers.names().forEachIndexed { idx, name ->
                        if (idx > 0) headersObj.append(",")
                        headersObj.append("\"").append(escapeJson(name)).append("\":\"").append(escapeJson(resp.header(name) ?: "")).append("\"")
                    }
                    headersObj.append("}")

                    """{"body":"${escapeJson(bodyText)}","statusCode":${resp.code},"headers":$headersObj,"ok":${resp.isSuccessful}}"""
                }
            } catch (e: Exception) {
                AppLogger.e("Mangayomi", "[MANGAYOMI][HTTP] Request failed for ${request.url}: ${e.message}")
                """{"body":"","statusCode":0,"headers":{},"ok":false}"""
            }
        }

        private fun escapeJson(str: String): String {
            return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        private fun String?.isNullOrBlankJson(): Boolean {
            if (this == null || this.isBlank() || this == "{}" || this == "null") return true
            return false
        }

        private fun parseJsonToMap(json: String?): Map<String, Any?>? {
            if (json == null || json.isBlank() || json == "{}") return emptyMap()
            val result = mutableMapOf<String, Any?>()
            try {
                val trimmed = json.trim()
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    val body = trimmed.substring(1, trimmed.length - 1)
                    var key = ""
                    var inKey = false
                    var inVal = false
                    var valBuf = StringBuilder()
                    var keyBuf = StringBuilder()
                    var escape = false

                    var i = 0
                    while (i < body.length) {
                        val c = body[i]
                        if (escape) {
                            if (inKey) keyBuf.append(c) else if (inVal) valBuf.append(c)
                            escape = false
                            i++
                            continue
                        }
                        if (c == '\\') {
                            escape = true
                            i++
                            continue
                        }
                        if (c == '"') {
                            if (!inKey && !inVal && key.isEmpty()) {
                                inKey = true
                            } else if (inKey) {
                                inKey = false
                                key = keyBuf.toString()
                                keyBuf.clear()
                            } else if (!inVal) {
                                inVal = true
                            } else if (inVal) {
                                inVal = false
                                result[key] = valBuf.toString()
                                valBuf.clear()
                                key = ""
                            }
                            i++
                            continue
                        }
                        if (c == ':' && !inKey && !inVal) {
                            i++
                            continue
                        }
                        if (c == ',' && !inKey && !inVal) {
                            if (key.isNotEmpty() && valBuf.isNotEmpty()) {
                                result[key] = valBuf.toString()
                                valBuf.clear()
                                key = ""
                            }
                            i++
                            continue
                        }
                        if (inKey) keyBuf.append(c)
                        else if (inVal) valBuf.append(c)
                        i++
                    }
                    if (key.isNotEmpty() && valBuf.isNotEmpty()) {
                        result[key] = valBuf.toString()
                    }
                }
            } catch (ignored: Exception) {}
            return result
        }
    }
}
