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
    private val okHttpClient: OkHttpClient = createDefaultOkHttpClient()
) : Closeable {

    private val isClosed = AtomicBoolean(false)
    private var quickJs: QuickJs? = null

    init {
        val qjs = QuickJs.create()

        val httpBridgeImpl = object : QuickJsHttpBridge {
            override fun get(url: String, headersJson: String?): String {
                return try {
                    val reqBuilder = Request.Builder().url(url)
                    reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    if (!headersJson.isNullOrBlankJson()) {
                        parseJsonToMap(headersJson)?.forEach { (k, v) -> reqBuilder.header(k, v.toString()) }
                    }
                    val req = reqBuilder.build()
                    executeHttpAndFormatJson(okHttpClient, req)
                } catch (e: Throwable) {
                    AppLogger.e("Mangayomi", "[MANGAYOMI][HTTP] NativeHttpBridge GET failed for $url: ${e.message}", e)
                    formatHttpExceptionJson(e)
                }
            }

            override fun post(url: String, headersJson: String?, body: String?): String {
                return try {
                    val reqBuilder = Request.Builder().url(url)
                    reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    val mediaType = "application/x-www-form-urlencoded".toMediaTypeOrNull()
                    reqBuilder.post((body ?: "").toRequestBody(mediaType))
                    if (!headersJson.isNullOrBlankJson()) {
                        parseJsonToMap(headersJson)?.forEach { (k, v) -> reqBuilder.header(k, v.toString()) }
                    }
                    val req = reqBuilder.build()
                    executeHttpAndFormatJson(okHttpClient, req)
                } catch (e: Throwable) {
                    AppLogger.e("Mangayomi", "[MANGAYOMI][HTTP] NativeHttpBridge POST failed for $url: ${e.message}", e)
                    formatHttpExceptionJson(e)
                }
            }
        }

        qjs.set("NativeHttpBridge", QuickJsHttpBridge::class.java, httpBridgeImpl)

        val fullPreamble = """
            class Source {}
            class Extension {}
            class MProvider {
                constructor(source) {
                    if (source) {
                        this.source = source;
                    } else if (typeof mangayomiSources !== 'undefined' && Array.isArray(mangayomiSources) && mangayomiSources.length > 0) {
                        this.source = mangayomiSources[0];
                    } else {
                        this.source = {};
                    }
                }
            }
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
            val argsJs = args.joinToString(",") { serializeArgToJs(it) }

            val script = """
                var _jsLog_function = "$functionName";
                var _jsLog_resultType = "undefined";
                var _jsLog_promiseResolved = false;
                var _jsLog_finalType = "undefined";
                var _jsLog_resultLength = 0;

                var _globalResultJson = null;
                var _globalError = null;
                var _globalFinished = false;

                (async function() {
                    try {
                        if (!source) {
                            _globalError = "No source instance in execution context";
                            return;
                        }
                        var fn = source["$functionName"] || (typeof window !== 'undefined' ? window["$functionName"] : null);
                        if (typeof fn !== 'function') {
                            _globalError = "Function '$functionName' not found on source or window";
                            return;
                        }
                        var rawRes = fn.apply(source, [$argsJs]);
                        _jsLog_resultType = (rawRes && typeof rawRes.then === 'function') ? "Promise" : typeof rawRes;

                        var res = (rawRes && typeof rawRes.then === 'function') ? await rawRes : rawRes;
                        _jsLog_promiseResolved = true;
                        _jsLog_finalType = typeof res;

                        if (typeof res === 'undefined') {
                            _globalResultJson = "null";
                            _jsLog_resultLength = 4;
                        } else {
                            _globalResultJson = JSON.stringify(res);
                            _jsLog_resultLength = _globalResultJson ? _globalResultJson.length : 0;
                        }
                    } catch(e) {
                        var msg = (e && e.message) ? e.message : String(e);
                        var stack = (e && e.stack) ? e.stack : "";
                        _globalError = msg + (stack ? "\n" + stack : "");
                    } finally {
                        _globalFinished = true;
                    }
                })();
            """.trimIndent()

            qjs.evaluate(script)

            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val isFinished = qjs.evaluate("_globalFinished")?.toString() == "true"
                if (isFinished) break
                qjs.evaluate("var _tick = 0;") // Flush QuickJS microtasks
                Thread.sleep(10)
            }

            val isFinished = qjs.evaluate("_globalFinished")?.toString() == "true"
            if (!isFinished) {
                throw RuntimeException("JS function '$functionName' timed out after ${timeoutMs}ms")
            }

            val globalError = qjs.evaluate("_globalError")?.toString()
            if (!globalError.isNullOrBlank() && globalError != "null") {
                AppLogger.e("Mangayomi", "[MANGAYOMI][JS] function=$functionName FAILED with error: $globalError")
                throw RuntimeException("JS function '$functionName' failed:\n$globalError")
            }

            val resType = qjs.evaluate("_jsLog_resultType")?.toString() ?: "unknown"
            val promResolved = qjs.evaluate("_jsLog_promiseResolved")?.toString() == "true"
            val finalType = qjs.evaluate("_jsLog_finalType")?.toString() ?: "unknown"
            val resLen = qjs.evaluate("_jsLog_resultLength")?.toString()?.toIntOrNull() ?: 0

            AppLogger.i("Mangayomi", "[MANGAYOMI][JS] function=$functionName")
            AppLogger.i("Mangayomi", "[MANGAYOMI][JS] resultType=$resType")
            AppLogger.i("Mangayomi", "[MANGAYOMI][JS] promiseResolved=$promResolved")
            AppLogger.i("Mangayomi", "[MANGAYOMI][JS] finalType=$finalType")
            AppLogger.i("Mangayomi", "[MANGAYOMI][JS] resultLength=$resLen")

            val rawJsonStr = qjs.evaluate("_globalResultJson")?.toString()
            val parsedData = parseJsonToNative(rawJsonStr)

            unpackResultToListOfMaps(parsedData)
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            quickJs?.close()
            quickJs = null
        }
    }

    companion object {
        private fun formatHttpExceptionJson(e: Throwable): String {
            val errObj = org.json.JSONObject()
            errObj.put("body", "")
            errObj.put("statusCode", 0)
            errObj.put("headers", org.json.JSONObject())
            errObj.put("ok", false)
            errObj.put("error", "${e.javaClass.name}: ${e.message}")
            errObj.put("errorClass", e.javaClass.name)
            errObj.put("errorMessage", e.message ?: "")
            val cause = e.cause
            if (cause != null) {
                errObj.put("rootCauseClass", cause.javaClass.name)
                errObj.put("rootCauseMessage", cause.message ?: "")
            }
            return errObj.toString()
        }

        private fun executeHttpAndFormatJson(client: OkHttpClient, request: Request): String {
            return try {
                client.newCall(request).execute().use { resp ->
                    val bodyText = resp.body?.string() ?: ""
                    AppLogger.i("Mangayomi", "[MANGAYOMI][HTTP] method=${request.method} url=${request.url} status=${resp.code} responseBytes=${bodyText.length}")

                    val respObj = org.json.JSONObject()
                    respObj.put("body", bodyText)
                    respObj.put("statusCode", resp.code)
                    val headersObj = org.json.JSONObject()
                    resp.headers.names().forEach { name ->
                        if (!name.equals("Set-Cookie", ignoreCase = true) && !name.equals("Authorization", ignoreCase = true)) {
                            headersObj.put(name, resp.header(name) ?: "")
                        }
                    }
                    respObj.put("headers", headersObj)
                    respObj.put("ok", resp.isSuccessful)
                    respObj.toString()
                }
            } catch (e: Throwable) {
                AppLogger.e("Mangayomi", "[MANGAYOMI][HTTP] Request failed for ${request.url}: ${e.message}", e)
                formatHttpExceptionJson(e)
            }
        }

        private fun serializeArgToJs(arg: Any?): String {
            return when (arg) {
                null -> "null"
                is Number, is Boolean -> arg.toString()
                is String -> "\"${escapeJson(arg)}\""
                is Map<*, *> -> {
                    val entries = arg.entries.joinToString(",") { (k, v) ->
                        "\"${escapeJson(k.toString())}\":${serializeArgToJs(v)}"
                    }
                    "{$entries}"
                }
                is List<*> -> {
                    val items = arg.joinToString(",") { serializeArgToJs(it) }
                    "[$items]"
                }
                is Array<*> -> {
                    val items = arg.joinToString(",") { serializeArgToJs(it) }
                    "[$items]"
                }
                else -> "\"${escapeJson(arg.toString())}\""
            }
        }

        private fun unpackResultToListOfMaps(data: Any?): List<Map<String, Any?>> {
            return when (data) {
                is List<*> -> {
                    data.filterIsInstance<Map<String, Any?>>()
                }
                is Map<*, *> -> {
                    val map = data as Map<String, Any?>
                    val innerList = map["list"]
                    if (innerList is List<*>) {
                        innerList.filterIsInstance<Map<String, Any?>>()
                    } else {
                        listOf(map)
                    }
                }
                else -> emptyList()
            }
        }

        fun createDefaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
                .build()
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

        private fun parseJsonToNative(jsonStr: String?): Any? {
            if (jsonStr.isNullOrBlank() || jsonStr == "null") return null
            val trimmed = jsonStr.trim()
            return try {
                val tokener = org.json.JSONTokener(trimmed)
                val nextValue = tokener.nextValue()
                convertJsonValue(nextValue)
            } catch (e: Exception) {
                AppLogger.e("Mangayomi", "[MANGAYOMI][JSON] Failed to parse JSON result: ${e.message}")
                null
            }
        }

        private fun convertJsonValue(value: Any?): Any? {
            return when (value) {
                null, org.json.JSONObject.NULL -> null
                is org.json.JSONObject -> {
                    val map = mutableMapOf<String, Any?>()
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        map[k] = convertJsonValue(value.opt(k))
                    }
                    map
                }
                is org.json.JSONArray -> {
                    val list = mutableListOf<Any?>()
                    for (i in 0 until value.length()) {
                        list.add(convertJsonValue(value.opt(i)))
                    }
                    list
                }
                else -> value
            }
        }

        private fun parseJsonToMap(json: String?): Map<String, Any?>? {
            return parseJsonToNative(json) as? Map<String, Any?>
        }
    }
}
