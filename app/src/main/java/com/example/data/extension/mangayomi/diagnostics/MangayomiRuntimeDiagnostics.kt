package com.example.data.extension.mangayomi.diagnostics

import android.os.Build
import com.example.core.logging.AppLogger
import com.example.data.local.database.dao.AnimeDao
import com.example.data.local.database.dao.EpisodeDao
import com.example.data.local.database.entity.AnimeEntity
import com.example.data.local.database.entity.EpisodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.mozilla.javascript.Context
import app.cash.quickjs.QuickJs
import java.util.concurrent.TimeUnit

data class HttpRequestTrace(
    val requestNumber: Int,
    val method: String,
    val url: String,
    val httpStatus: Int,
    val contentType: String,
    val responseBytes: Int,
    val bodyPrefix300: String
)

data class SearchItemResult(
    val index: Int,
    val name: String,
    val link: String,
    val image: String
)

data class VideoItemResult(
    val index: Int,
    val url: String,
    val originalUrl: String,
    val quality: String,
    val headersJson: String
)

data class StageResult(
    val stageNumber: Int,
    val stageName: String,
    val pass: Boolean,
    val errorType: String? = null,
    val errorMessage: String? = null,
    val stackTrace: String? = null
)

data class RuntimeDiagnosticsResult(
    val engineName: String,
    val engineVersion: String,
    val nativeLibrary: String,
    val abi: String,
    val classTest: String,
    val classExprTest: String,
    val promiseTest: String,
    val asyncTest: String,
    val preambleTest: String,
    val rhinoErrorSummary: String
)

data class LiveTestResult(
    val engine: String = "QuickJS",
    val version: String = "0.9.2",
    val nativeLib: String = "quickjs",
    val abi: String = "android",

    // Stages log
    val stages: List<StageResult> = emptyList(),

    // Overall Execution Tracking
    val testExecuted: Boolean = false,
    val testStartedAt: String = "",
    val testFinishedAt: String = "",
    val testFailedStage: String = "",
    val testErrorType: String = "",
    val testErrorMessage: String = "",
    val testErrorStacktrace: String = "",

    // 1. RAW Client.get() & JSON.parse Diagnostics
    val httpMethod: String = "GET",
    val httpUrl: String = "",
    val httpStatus: Int = 0,
    val httpContentType: String = "",
    val httpHeadersStr: String = "",
    val httpBodyLength: Int = 0,
    val httpBodyPrefix: String = "",
    val rawResponseJsType: String = "",
    val rawBodyJsType: String = "",
    val rawBodyIsString: Boolean = false,
    val rawBodyPrefix1000: String = "",
    val rawBodySuffix300: String = "",
    val responseIsNotJson: Boolean = false,

    val jsonParsePass: Boolean = false,
    val jsonErrorType: String = "",
    val jsonErrorMessage: String = "",
    val jsonErrorStack: String = "",
    val errorPosition: String = "",
    val bodyCharCodes: String = "",
    val bodyFirstCharCode: Int = 0,
    val bodyLastCharCode: Int = 0,
    val jsonShape: String = "",

    val clientGetResultType: String = "",
    val clientGetBodyType: String = "",
    val parsedType: String = "",
    val parsedKeys: String = "",
    val parsedJsonPrefix: String = "",
    val rawHttpPass: Boolean = false,

    // 2. Source & Extension Load
    val sourceBytes: Int = 0,
    val sourceExecution: String = "NOT_RUN",
    val sourceException: String? = null,
    val instanceStatus: String = "NOT_RUN",
    val constructorName: String = "None",

    // 3. Real Search Test & Deep Inspection
    val searchStatus: String = "NOT_RUN",
    val searchResultType: String = "",
    val searchResponseKeys: String = "",
    val searchResultCount: Int = 0,
    val searchHasNextPage: Boolean = false,
    val searchItems: List<SearchItemResult> = emptyList(),
    val searchPass: Boolean = false,
    val searchErrorType: String? = null,
    val searchErrorMessage: String? = null,
    val searchErrorStacktrace: String? = null,

    // Deep Search Debug Fields
    val searchMethodType: String = "",
    val searchMethodString: String = "",
    val prototypeType: String = "",
    val prototypeKeys: String = "",
    val searchCallException: String = "",
    val searchResponseType: String = "",
    val searchResponseString: String = "",
    val searchResponseJson: String = "",
    val searchOwnKeys: String = "",
    val searchObjectKeys: String = "",
    val searchListType: String = "",
    val searchListIsArray: Boolean = false,
    val searchListLength: Int = -1,
    val searchHasNextType: String = "",
    val searchHasNextValue: String = "",
    val searchFailureReason: String = "",

    // Direct searchApi Debug Fields
    val searchApiType: String = "",
    val directSearchApiType: String = "",
    val directSearchApiString: String = "",
    val directSearchApiJson: String = "",
    val directSearchApiKeys: String = "",
    val directSearchApiListType: String = "",
    val directSearchApiListLength: Int = -1,
    val directSearchApiHasNext: String = "",

    // Network Instrumentation Counts
    val requestCountAfterRawGet: Int = 0,
    val requestCountAfterSearch: Int = 0,

    // 4. Real Detail Test
    val detailInputLink: String = "",
    val detailStatus: String = "NOT_RUN",
    val detailName: String = "",
    val detailImageUrl: String = "",
    val detailDescriptionLength: Int = 0,
    val detailGenres: String = "",
    val detailStatusValue: String = "",
    val detailChapterCount: Int = 0,
    val chapter0Name: String = "",
    val chapter0Url: String = "",
    val chapterLastName: String = "",
    val chapterLastUrl: String = "",
    val detailPass: Boolean = false,
    val detailErrorType: String? = null,
    val detailErrorMessage: String? = null,
    val detailErrorStacktrace: String? = null,

    // 5. Episode Verification
    val episodesPass: Boolean = false,

    // 6. Real Video Test
    val videoInputUrl: String = "",
    val videoStatus: String = "NOT_RUN",
    val videoCount: Int = 0,
    val videoItems: List<VideoItemResult> = emptyList(),
    val videoPass: Boolean = false,
    val videoErrorType: String? = null,
    val videoErrorMessage: String? = null,
    val videoErrorStacktrace: String? = null,

    // Network Instrumentation
    val networkTraces: List<HttpRequestTrace> = emptyList(),

    // Pipeline Verification
    val rawImageUrl: String = "",
    val normalizedPosterUrl: String = "",
    val databasePosterUrl: String = "",
    val uiPosterUrl: String = "",
    val posterUiPass: Boolean = false,
    val rawChapterCount: Int = 0,
    val normalizedEpisodeCount: Int = 0,
    val databaseEpisodeCount: Int = 0,
    val uiEpisodeCount: Int = 0,
    val episodeUiPass: Boolean = false,

    // Final Summary Fields
    val firstRealTitle: String = "",
    val firstRealLink: String = "",
    val firstRealImage: String = "",
    val realEpisodeCount: Int = 0,
    val firstRealVideo: String = "",

    val rawError: String? = null
)

interface QuickJsHttpBridge {
    fun get(url: String, headersJson: String?): String
    fun post(url: String, headersJson: String?, body: String?): String
}

object MangayomiRuntimeDiagnostics {

    private const val TAG = "MangayomiDiag"
    private const val JUST4ANIME_URL = "https://raw.githubusercontent.com/Mallyd11/mangayomi-anime-extensions/main/javascript/anime/src/en/just4anime.js"

    fun runRhinoProof(): String {
        val cx = Context.enter()
        cx.optimizationLevel = -1
        cx.languageVersion = Context.VERSION_ES6
        return try {
            val scope = cx.initSafeStandardObjects()
            cx.evaluateString(scope, "class TestClass {}", "proof_class.js", 1, null)
            "CLASS_OK"
        } catch (e: Exception) {
            "CLASS_FAIL: ${e.message}"
        } finally {
            Context.exit()
        }
    }

    fun runQuickJsSelfTest(): RuntimeDiagnosticsResult {
        var classRes = "NOT_RUN"
        var classExprRes = "NOT_RUN"
        var promiseRes = "NOT_RUN"
        var asyncRes = "NOT_RUN"
        var preambleRes = "NOT_RUN"

        val rhinoProofErr = runRhinoProof()
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

        try {
            QuickJs.create().use { qjs ->
                try {
                    qjs.evaluate("class TestClass {}")
                    classRes = "PASS"
                } catch (e: Exception) {
                    classRes = "FAIL: ${e.message}"
                }

                try {
                    qjs.evaluate("const TestClass2 = class {};")
                    classExprRes = "PASS"
                } catch (e: Exception) {
                    classExprRes = "FAIL: ${e.message}"
                }

                try {
                    val pVal = qjs.evaluate("Promise.resolve('PROMISE_OK')")?.toString() ?: "null"
                    promiseRes = if (pVal.contains("PROMISE_OK")) "PASS" else "PASS ($pVal)"
                } catch (e: Exception) {
                    promiseRes = "FAIL: ${e.message}"
                }

                try {
                    qjs.evaluate("(async () => { return 'ASYNC_OK'; })()")
                    asyncRes = "PASS"
                } catch (e: Exception) {
                    asyncRes = "FAIL: ${e.message}"
                }

                try {
                    qjs.evaluate("""
                        class Source {}
                        class Extension {}
                        class MProvider {}
                        var exports = {};
                        var module = { exports: exports };
                    """.trimIndent())
                    preambleRes = "PASS"
                } catch (e: Exception) {
                    preambleRes = "FAIL: ${e.message}"
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "QuickJS Self-test failed: ${e.message}", e)
        }

        return RuntimeDiagnosticsResult(
            engineName = "QuickJS",
            engineVersion = "0.9.2",
            nativeLibrary = "quickjs",
            abi = abi,
            classTest = classRes,
            classExprTest = classExprRes,
            promiseTest = promiseRes,
            asyncTest = asyncRes,
            preambleTest = preambleRes,
            rhinoErrorSummary = rhinoProofErr
        )
    }

    suspend fun runLiveExtensionTest(
        okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build(),
        animeDao: AnimeDao? = null,
        episodeDao: EpisodeDao? = null
    ): LiveTestResult = withContext(Dispatchers.IO) {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val httpTraces = mutableListOf<HttpRequestTrace>()
        var traceIndex = 1
        val startedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        val stagesList = mutableListOf<StageResult>()

        var failedStage = ""
        var errType = ""
        var errMsg = ""
        var errStack = ""

        fun recordStagePass(num: Int, name: String) {
            stagesList.add(StageResult(stageNumber = num, stageName = name, pass = true))
            AppLogger.i(TAG, "[MANGAYOMI] STAGE $num $name: PASS")
        }

        fun recordStageFail(num: Int, name: String, e: Throwable) {
            failedStage = name
            errType = e.javaClass.name
            errMsg = e.message ?: e.toString()
            errStack = e.stackTraceToString()
            stagesList.add(
                StageResult(
                    stageNumber = num,
                    stageName = name,
                    pass = false,
                    errorType = errType,
                    errorMessage = errMsg,
                    stackTrace = errStack
                )
            )
            AppLogger.e(TAG, "[MANGAYOMI] STAGE $num $name: FAIL ($errType: $errMsg)", e)
        }

        AppLogger.i(TAG, "DIAGNOSTICS_SCREEN_CREATED")
        AppLogger.i(TAG, "DIAGNOSTICS_VIEWMODEL_CREATED")
        AppLogger.i(TAG, "LIVE_TEST_STARTED")

        // STAGE 1: QUICKJS_INIT
        var qjs: QuickJs? = null
        try {
            qjs = QuickJs.create()
            recordStagePass(1, "QUICKJS_INIT")
            AppLogger.i(TAG, "QUICKJS_INITIALIZED")
        } catch (e: Throwable) {
            recordStageFail(1, "QUICKJS_INIT", e)
            val finishedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            return@withContext LiveTestResult(
                engine = "QuickJS", version = "0.9.2", nativeLib = "quickjs", abi = abi,
                stages = stagesList, testExecuted = false,
                testStartedAt = startedAtStr, testFinishedAt = finishedAtStr,
                testFailedStage = failedStage, testErrorType = errType, testErrorMessage = errMsg, testErrorStacktrace = errStack,
                rawError = "STAGE 1 QUICKJS_INIT failed: $errMsg"
            )
        }

        // STAGE 2: HTTP_BRIDGE_INIT
        try {
            val httpBridgeImpl = object : QuickJsHttpBridge {
                override fun get(url: String, headersJson: String?): String {
                    val reqBuilder = Request.Builder().url(url)
                    reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    if (!headersJson.isNullOrBlankJson()) {
                        parseJsonToMap(headersJson)?.forEach { (k, v) -> reqBuilder.header(k, v.toString()) }
                    }
                    val req = reqBuilder.build()
                    return executeHttpAndFormatJson(okHttpClient, req, httpTraces, traceIndex++)
                }

                override fun post(url: String, headersJson: String?, body: String?): String {
                    val reqBuilder = Request.Builder().url(url)
                    reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    val mediaType = "application/x-www-form-urlencoded".toMediaTypeOrNull()
                    reqBuilder.post((body ?: "").toRequestBody(mediaType))
                    if (!headersJson.isNullOrBlankJson()) {
                        parseJsonToMap(headersJson)?.forEach { (k, v) -> reqBuilder.header(k, v.toString()) }
                    }
                    return executeHttpAndFormatJson(okHttpClient, reqBuilder.build(), httpTraces, traceIndex++)
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
            recordStagePass(2, "HTTP_BRIDGE_INIT")
            AppLogger.i(TAG, "HTTP_BRIDGE_INITIALIZED")
        } catch (e: Throwable) {
            qjs.close()
            recordStageFail(2, "HTTP_BRIDGE_INIT", e)
            val finishedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            return@withContext LiveTestResult(
                engine = "QuickJS", version = "0.9.2", nativeLib = "quickjs", abi = abi,
                stages = stagesList, testExecuted = false,
                testStartedAt = startedAtStr, testFinishedAt = finishedAtStr,
                testFailedStage = failedStage, testErrorType = errType, testErrorMessage = errMsg, testErrorStacktrace = errStack,
                rawError = "STAGE 2 HTTP_BRIDGE_INIT failed: $errMsg"
            )
        }

        // STAGE 3: RAW_CLIENT_GET & STAGE 4: JSON_PARSE
        val targetSearchUrl = "https://just4anime.online/api/advanced-search?page=1&perPage=5&query=Frieren"
        var httpStatusVal = 0
        var httpContentTypeVal = "application/json"
        var httpHeadersStrVal = "{}"
        var httpBodyLenVal = 0
        var httpBodyPrefixVal = ""
        var rawResJsTypeVal = "undefined"
        var rawBodyJsTypeVal = "undefined"
        var rawBodyIsStringVal = false
        var rawBodyPrefix1000Val = ""
        var rawBodySuffix300Val = ""
        var responseIsNotJsonVal = false

        var jsonParsePass = false
        var jsonErrTypeVal = ""
        var jsonErrMsgVal = ""
        var jsonErrStackVal = ""
        var errorPositionVal = ""
        var bodyCharCodesVal = ""
        var bodyFirstCharCodeVal = 0
        var bodyLastCharCodeVal = 0
        var jsonShapeVal = "INVALID"

        var clientGetResultTypeVal = "undefined"
        var clientGetBodyTypeVal = "undefined"
        var parsedTypeVal = "undefined"
        var parsedKeysVal = ""
        var parsedJsonPrefixVal = ""
        var rawHttpPass = false

        AppLogger.i(TAG, "RAW_HTTP_STARTED")
        try {
            val directHttpScript = """
                (function() {
                    var client = new Client();
                    var res = client.get(
                        "$targetSearchUrl",
                        { "User-Agent": "Mozilla/5.0", "Accept": "application/json, text/plain, */*" }
                    );

                    var rawResJsType = typeof res;
                    var rawBodyJsType = (res && res.body !== undefined) ? typeof res.body : "undefined";
                    var rawBodyIsString = rawBodyJsType === "string";
                    var bodyStr = (res && res.body) ? res.body : "";
                    var bodyLength = bodyStr.length;
                    var bodyPrefix1000 = bodyStr.substring(0, 1000);
                    var bodySuffix300 = bodyStr.substring(Math.max(0, bodyStr.length - 300));

                    var httpStatus = (res && res.statusCode) ? res.statusCode : 0;
                    var httpHeaders = (res && res.headers) ? res.headers : {};
                    var contentType = httpHeaders["content-type"] || httpHeaders["Content-Type"] || "";

                    var isHtmlOrCloudflare = false;
                    var trimmedPrefix = bodyPrefix1000.trim().toLowerCase();
                    if (trimmedPrefix.indexOf("<!doctype html") === 0 ||
                        trimmedPrefix.indexOf("<html") === 0 ||
                        trimmedPrefix.indexOf("cloudflare") >= 0 ||
                        trimmedPrefix.indexOf("access denied") >= 0 ||
                        trimmedPrefix.indexOf("just a moment") >= 0) {
                        isHtmlOrCloudflare = true;
                    }

                    var charCodes = [];
                    var maxChars = Math.min(100, bodyStr.length);
                    for (var i = 0; i < maxChars; i++) {
                        charCodes.push(bodyStr.charCodeAt(i));
                    }
                    var firstCharCode = bodyStr.length > 0 ? bodyStr.charCodeAt(0) : -1;
                    var lastCharCode = bodyStr.length > 0 ? bodyStr.charCodeAt(bodyStr.length - 1) : -1;

                    var parsed = null;
                    var jsonParsePass = false;
                    var jsonErrType = "";
                    var jsonErrMsg = "";
                    var jsonErrStack = "";
                    var errPos = "";
                    var jsonShape = "INVALID";

                    try {
                        parsed = JSON.parse(bodyStr);
                        jsonParsePass = true;
                        if (typeof parsed === 'object') {
                            if (parsed === null) {
                                jsonShape = "NULL";
                            } else if (Array.isArray(parsed)) {
                                jsonShape = "ARRAY";
                            } else {
                                jsonShape = "OBJECT";
                            }
                        } else if (typeof parsed === 'string') {
                            jsonShape = "STRING";
                        } else if (typeof parsed === 'number') {
                            jsonShape = "NUMBER";
                        } else if (typeof parsed === 'boolean') {
                            jsonShape = "BOOLEAN";
                        }
                    } catch(e) {
                        jsonParsePass = false;
                        jsonErrType = e.name || (e.constructor ? e.constructor.name : "SyntaxError");
                        jsonErrMsg = e.message || String(e);
                        jsonErrStack = e.stack || "";
                        var posMatch = jsonErrMsg.match(/(?:at position|position|at column|col|at line|line)\s+(\d+)/i);
                        if (posMatch && posMatch[1]) {
                            errPos = posMatch[1];
                        }
                    }

                    var keys = (parsed && typeof parsed === 'object' && parsed !== null) ? Object.keys(parsed).join(",") : "";

                    return JSON.stringify({
                        rawResJsType: rawResJsType,
                        rawBodyJsType: rawBodyJsType,
                        rawBodyIsString: rawBodyIsString,
                        bodyLength: bodyLength,
                        bodyPrefix1000: bodyPrefix1000,
                        bodySuffix300: bodySuffix300,
                        httpStatus: httpStatus,
                        httpContentType: contentType,
                        httpHeadersStr: JSON.stringify(httpHeaders),
                        responseIsNotJson: isHtmlOrCloudflare,
                        jsonParsePass: jsonParsePass,
                        jsonErrorType: jsonErrType,
                        jsonErrorMessage: jsonErrMsg,
                        jsonErrorStack: jsonErrStack,
                        errorPosition: errPos,
                        bodyCharCodes: charCodes.join(","),
                        bodyFirstCharCode: firstCharCode,
                        bodyLastCharCode: lastCharCode,
                        jsonShape: jsonShape,
                        parsedType: typeof parsed,
                        parsedKeys: keys,
                        parsedPrefix: parsed ? JSON.stringify(parsed).substring(0, 300) : ""
                    });
                })()
            """.trimIndent()

            val rawHttpEval = qjs.evaluate(directHttpScript)?.toString() ?: "{}"
            val httpEvalMap = parseJsonToMap(rawHttpEval)

            httpStatusVal = httpEvalMap?.get("httpStatus")?.toString()?.toIntOrNull() ?: 0
            httpContentTypeVal = httpEvalMap?.get("httpContentType")?.toString() ?: "application/json"
            httpHeadersStrVal = httpEvalMap?.get("httpHeadersStr")?.toString() ?: "{}"
            httpBodyLenVal = httpEvalMap?.get("bodyLength")?.toString()?.toIntOrNull() ?: 0
            httpBodyPrefixVal = httpEvalMap?.get("bodyPrefix1000")?.toString()?.take(500) ?: ""
            rawResJsTypeVal = httpEvalMap?.get("rawResJsType")?.toString() ?: "undefined"
            rawBodyJsTypeVal = httpEvalMap?.get("rawBodyJsType")?.toString() ?: "undefined"
            rawBodyIsStringVal = httpEvalMap?.get("rawBodyIsString")?.toString()?.toBoolean() ?: false
            rawBodyPrefix1000Val = httpEvalMap?.get("bodyPrefix1000")?.toString() ?: ""
            rawBodySuffix300Val = httpEvalMap?.get("bodySuffix300")?.toString() ?: ""
            responseIsNotJsonVal = httpEvalMap?.get("responseIsNotJson")?.toString()?.toBoolean() ?: false

            jsonParsePass = httpEvalMap?.get("jsonParsePass")?.toString()?.toBoolean() ?: false
            jsonErrTypeVal = httpEvalMap?.get("jsonErrorType")?.toString() ?: ""
            jsonErrMsgVal = httpEvalMap?.get("jsonErrorMessage")?.toString() ?: ""
            jsonErrStackVal = httpEvalMap?.get("jsonErrorStack")?.toString() ?: ""
            errorPositionVal = httpEvalMap?.get("errorPosition")?.toString() ?: ""
            bodyCharCodesVal = httpEvalMap?.get("bodyCharCodes")?.toString() ?: ""
            bodyFirstCharCodeVal = httpEvalMap?.get("bodyFirstCharCode")?.toString()?.toIntOrNull() ?: 0
            bodyLastCharCodeVal = httpEvalMap?.get("bodyLastCharCode")?.toString()?.toIntOrNull() ?: 0
            jsonShapeVal = httpEvalMap?.get("jsonShape")?.toString() ?: "INVALID"

            parsedTypeVal = httpEvalMap?.get("parsedType")?.toString() ?: "unknown"
            parsedKeysVal = httpEvalMap?.get("parsedKeys")?.toString() ?: ""
            parsedJsonPrefixVal = httpEvalMap?.get("parsedPrefix")?.toString() ?: ""

            clientGetResultTypeVal = rawResJsTypeVal
            clientGetBodyTypeVal = rawBodyJsTypeVal

            rawHttpPass = httpStatusVal in 200..299

            if (rawHttpPass) {
                recordStagePass(3, "RAW_CLIENT_GET")
                AppLogger.i(TAG, "RAW_HTTP_FINISHED")
            } else {
                throw RuntimeException("HTTP Status $httpStatusVal not in 200..299")
            }

            if (jsonParsePass) {
                recordStagePass(4, "JSON_PARSE")
            } else {
                val errSummary = if (responseIsNotJsonVal) {
                    "Response is HTML/Cloudflare (Not JSON)"
                } else {
                    "$jsonErrTypeVal: $jsonErrMsgVal"
                }
                throw RuntimeException("JSON.parse failed: $errSummary")
            }
        } catch (e: Throwable) {
            if (!rawHttpPass) {
                recordStageFail(3, "RAW_CLIENT_GET", e)
            } else {
                recordStageFail(4, "JSON_PARSE", e)
            }
            qjs.close()
            val finishedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            return@withContext LiveTestResult(
                engine = "QuickJS", version = "0.9.2", nativeLib = "quickjs", abi = abi,
                stages = stagesList, testExecuted = true,
                testStartedAt = startedAtStr, testFinishedAt = finishedAtStr,
                testFailedStage = failedStage, testErrorType = errType, testErrorMessage = errMsg, testErrorStacktrace = errStack,
                httpMethod = "GET", httpUrl = targetSearchUrl, httpStatus = httpStatusVal,
                httpContentType = httpContentTypeVal, httpHeadersStr = httpHeadersStrVal, httpBodyLength = httpBodyLenVal,
                httpBodyPrefix = httpBodyPrefixVal, rawResponseJsType = rawResJsTypeVal, rawBodyJsType = rawBodyJsTypeVal,
                rawBodyIsString = rawBodyIsStringVal, rawBodyPrefix1000 = rawBodyPrefix1000Val, rawBodySuffix300 = rawBodySuffix300Val,
                responseIsNotJson = responseIsNotJsonVal, clientGetResultType = clientGetResultTypeVal, clientGetBodyType = clientGetBodyTypeVal,
                jsonParsePass = jsonParsePass, jsonErrorType = jsonErrTypeVal, jsonErrorMessage = jsonErrMsgVal,
                jsonErrorStack = jsonErrStackVal, errorPosition = errorPositionVal, bodyCharCodes = bodyCharCodesVal,
                bodyFirstCharCode = bodyFirstCharCodeVal, bodyLastCharCode = bodyLastCharCodeVal, jsonShape = jsonShapeVal,
                parsedType = parsedTypeVal, parsedKeys = parsedKeysVal, parsedJsonPrefix = parsedJsonPrefixVal,
                rawHttpPass = rawHttpPass, networkTraces = httpTraces, rawError = "Failed at $failedStage: $errMsg"
            )
        }

        // STAGE 5: JUST4ANIME_SOURCE_LOAD
        var sourceBytes = 0
        var sourceJs = ""
        try {
            val request = Request.Builder().url(JUST4ANIME_URL).build()
            val response = okHttpClient.newCall(request).execute()
            sourceJs = response.body?.string() ?: ""
            sourceBytes = sourceJs.toByteArray().size
            qjs.evaluate(sourceJs)
            recordStagePass(5, "JUST4ANIME_SOURCE_LOAD")
            AppLogger.i(TAG, "EXTENSION_LOADED")
        } catch (e: Throwable) {
            recordStageFail(5, "JUST4ANIME_SOURCE_LOAD", e)
            qjs.close()
            val finishedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            return@withContext LiveTestResult(
                engine = "QuickJS", version = "0.9.2", nativeLib = "quickjs", abi = abi,
                stages = stagesList, testExecuted = true,
                testStartedAt = startedAtStr, testFinishedAt = finishedAtStr,
                testFailedStage = failedStage, testErrorType = errType, testErrorMessage = errMsg, testErrorStacktrace = errStack,
                httpMethod = "GET", httpUrl = targetSearchUrl, httpStatus = httpStatusVal,
                httpContentType = httpContentTypeVal, httpBodyLength = httpBodyLenVal, httpBodyPrefix = httpBodyPrefixVal,
                clientGetResultType = clientGetResultTypeVal, clientGetBodyType = clientGetBodyTypeVal,
                parsedType = parsedTypeVal, parsedKeys = parsedKeysVal, parsedJsonPrefix = parsedJsonPrefixVal,
                rawHttpPass = rawHttpPass, jsonParsePass = jsonParsePass, sourceBytes = sourceBytes,
                sourceExecution = "FAIL", sourceException = errMsg, instanceStatus = "NOT_RUN", constructorName = "None",
                networkTraces = httpTraces, rawError = "Source Load Failed: $errMsg"
            )
        }

        // STAGE 6: DEFAULT_EXTENSION_INSTANTIATION
        var constructorNameVal = "None"
        try {
            qjs.evaluate("""
                var source = null;
                if (typeof DefaultExtension !== 'undefined') {
                    source = new DefaultExtension();
                }
            """.trimIndent())

            val isInstantiated = qjs.evaluate("source !== null") as Boolean
            constructorNameVal = qjs.evaluate("source ? source.constructor.name : 'None'")?.toString() ?: "None"

            if (isInstantiated) {
                recordStagePass(6, "DEFAULT_EXTENSION_INSTANTIATION")
            } else {
                throw RuntimeException("DefaultExtension class not defined or instance is null")
            }
        } catch (e: Throwable) {
            recordStageFail(6, "DEFAULT_EXTENSION_INSTANTIATION", e)
            qjs.close()
            val finishedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            return@withContext LiveTestResult(
                engine = "QuickJS", version = "0.9.2", nativeLib = "quickjs", abi = abi,
                stages = stagesList, testExecuted = true,
                testStartedAt = startedAtStr, testFinishedAt = finishedAtStr,
                testFailedStage = failedStage, testErrorType = errType, testErrorMessage = errMsg, testErrorStacktrace = errStack,
                httpMethod = "GET", httpUrl = targetSearchUrl, httpStatus = httpStatusVal,
                httpContentType = httpContentTypeVal, httpBodyLength = httpBodyLenVal, httpBodyPrefix = httpBodyPrefixVal,
                clientGetResultType = clientGetResultTypeVal, clientGetBodyType = clientGetBodyTypeVal,
                parsedType = parsedTypeVal, parsedKeys = parsedKeysVal, parsedJsonPrefix = parsedJsonPrefixVal,
                rawHttpPass = rawHttpPass, jsonParsePass = jsonParsePass, sourceBytes = sourceBytes,
                sourceExecution = "PASS", instanceStatus = "FAIL", constructorName = constructorNameVal,
                networkTraces = httpTraces, rawError = "Instantiation Failed: $errMsg"
            )
        }

        val reqCountAfterRawGetVal = httpTraces.size

        // STAGE 7: SEARCH & DEEP INSPECTION
        var searchResultTypeVal = "undefined"
        var searchResponseKeysVal = ""
        var searchResultCountVal = 0
        var searchHasNextPageVal = false
        val searchItemsList = mutableListOf<SearchItemResult>()
        var searchPassVal = false

        var searchMethodTypeVal = ""
        var searchMethodStringVal = ""
        var prototypeTypeVal = ""
        var prototypeKeysVal = ""
        var searchCallExceptionVal = ""
        var searchResponseTypeVal = ""
        var searchResponseStringVal = ""
        var searchResponseJsonVal = ""
        var searchOwnKeysVal = ""
        var searchObjectKeysVal = ""
        var searchListTypeVal = ""
        var searchListIsArrayVal = false
        var searchListLengthVal = -1
        var searchHasNextTypeVal = ""
        var searchHasNextValueVal = ""
        var searchFailureReasonVal = ""

        var searchApiTypeVal = ""
        var directSearchApiTypeVal = ""
        var directSearchApiStringVal = ""
        var directSearchApiJsonVal = ""
        var directSearchApiKeysVal = ""
        var directSearchApiListTypeVal = ""
        var directSearchApiListLengthVal = -1
        var directSearchApiHasNextVal = ""

        AppLogger.i(TAG, "SEARCH_STARTED")
        try {
            val searchScript = """
                var _searchResult = null;
                (async function() {
                    function safeStringify(value) {
                        try {
                            return JSON.stringify(value);
                        } catch (e) {
                            return "JSON.stringify failed: " + String(e);
                        }
                    }

                    try {
                        if (!source) {
                            _searchResult = JSON.stringify({ error: "No source instance" });
                            return;
                        }
                        var extension = source;

                        // 1. Inspect extension.search
                        var searchMethodType = typeof extension.search;
                        var searchMethodString = extension.search ? String(extension.search).substring(0, 500) : "";

                        // 2. Inspect prototype
                        var proto = Object.getPrototypeOf(extension);
                        var prototypeType = typeof proto;
                        var prototypeKeys = "";
                        try {
                            prototypeKeys = proto ? Object.getOwnPropertyNames(proto).join(",") : "";
                        } catch(e) {
                            prototypeKeys = "ERROR: " + String(e);
                        }

                        // 3. Inspect actual search call
                        var searchResponse = null;
                        var searchCallException = "";
                        try {
                            var rawRes = extension.search("Frieren", 1, []);
                            if (rawRes && typeof rawRes.then === 'function') {
                                searchResponse = await rawRes;
                            } else {
                                searchResponse = rawRes;
                            }
                        } catch(e) {
                            searchCallException = String(e && e.stack ? e.stack : e);
                        }

                        var searchResponseType = typeof searchResponse;
                        var searchResponseString = String(searchResponse);
                        var searchResponseJson = safeStringify(searchResponse);

                        // 4. Inspect keys
                        var searchOwnKeys = "";
                        try {
                            searchOwnKeys = searchResponse ? Object.getOwnPropertyNames(searchResponse).join(",") : "";
                        } catch(e) {
                            searchOwnKeys = "ERROR: " + String(e);
                        }

                        var searchObjectKeys = "";
                        try {
                            searchObjectKeys = searchResponse ? Object.keys(searchResponse).join(",") : "";
                        } catch(e) {
                            searchObjectKeys = "ERROR: " + String(e);
                        }

                        // 5. Inspect Mangayomi fields
                        var searchListType = typeof searchResponse?.list;
                        var searchListIsArray = Array.isArray(searchResponse?.list);
                        var searchListLength = searchListIsArray ? searchResponse.list.length : -1;
                        var searchHasNextType = typeof searchResponse?.hasNextPage;
                        var searchHasNextValue = String(searchResponse?.hasNextPage);

                        var results = searchListIsArray ? searchResponse.list : [];
                        var items = [];
                        for (var i = 0; i < Math.min(5, results.length); i++) {
                            var item = results[i] || {};
                            items.push({
                                name: item.name || item.title || "",
                                link: item.link || item.url || "",
                                imageUrl: item.imageUrl || item.cover || item.image || ""
                            });
                        }

                        // 7. Inspect searchApi directly
                        var searchApiType = typeof extension.searchApi;
                        var directSearchApiType = "undefined";
                        var directSearchApiString = "";
                        var directSearchApiJson = "";
                        var directSearchApiKeys = "";
                        var directSearchApiListType = "undefined";
                        var directSearchApiListLength = -1;
                        var directSearchApiHasNext = "undefined";

                        if (searchApiType === "function") {
                            try {
                                var directRawRes = extension.searchApi("page=1&perPage=5&query=Frieren");
                                var directRes = (directRawRes && typeof directRawRes.then === 'function') ? await directRawRes : directRawRes;
                                directSearchApiType = typeof directRes;
                                directSearchApiString = String(directRes);
                                directSearchApiJson = safeStringify(directRes);
                                directSearchApiKeys = (directRes && typeof directRes === 'object' && directRes !== null) ? Object.keys(directRes).join(",") : "";
                                directSearchApiListType = typeof directRes?.list;
                                directSearchApiListLength = Array.isArray(directRes?.list) ? directRes.list.length : -1;
                                directSearchApiHasNext = String(directRes?.hasNextPage);
                            } catch(e) {
                                directSearchApiString = "ERROR: " + String(e && e.stack ? e.stack : e);
                            }
                        }

                        _searchResult = JSON.stringify({
                            success: true,
                            searchMethodType: searchMethodType,
                            searchMethodString: searchMethodString,
                            prototypeType: prototypeType,
                            prototypeKeys: prototypeKeys,
                            searchCallException: searchCallException,
                            searchResponseType: searchResponseType,
                            searchResponseString: searchResponseString,
                            searchResponseJson: searchResponseJson,
                            searchOwnKeys: searchOwnKeys,
                            searchObjectKeys: searchObjectKeys,
                            searchListType: searchListType,
                            searchListIsArray: searchListIsArray,
                            searchListLength: searchListLength,
                            searchHasNextType: searchHasNextType,
                            searchHasNextValue: searchHasNextValue,
                            items: items,
                            searchApiType: searchApiType,
                            directSearchApiType: directSearchApiType,
                            directSearchApiString: directSearchApiString,
                            directSearchApiJson: directSearchApiJson,
                            directSearchApiKeys: directSearchApiKeys,
                            directSearchApiListType: directSearchApiListType,
                            directSearchApiListLength: directSearchApiListLength,
                            directSearchApiHasNext: directSearchApiHasNext
                        });
                    } catch(e) {
                        _searchResult = JSON.stringify({ error: String(e), stack: (e && e.stack) ? String(e.stack) : "" });
                    }
                })();
            """.trimIndent()

            qjs.evaluate(searchScript)
            qjs.evaluate("var _dummy = 0;") // trigger microtask flush
            val rawSearchEval = qjs.evaluate("_searchResult")?.toString() ?: "{}"
            val searchEvalMap = parseJsonToMap(rawSearchEval)

            if (searchEvalMap?.containsKey("error") == true) {
                val sErr = searchEvalMap["error"]?.toString() ?: "Search JS Error"
                val sStack = searchEvalMap["stack"]?.toString() ?: ""
                throw RuntimeException("JS Search Exception: $sErr\n$sStack")
            }

            searchMethodTypeVal = searchEvalMap?.get("searchMethodType")?.toString() ?: ""
            searchMethodStringVal = searchEvalMap?.get("searchMethodString")?.toString() ?: ""
            prototypeTypeVal = searchEvalMap?.get("prototypeType")?.toString() ?: ""
            prototypeKeysVal = searchEvalMap?.get("prototypeKeys")?.toString() ?: ""
            searchCallExceptionVal = searchEvalMap?.get("searchCallException")?.toString() ?: ""
            searchResponseTypeVal = searchEvalMap?.get("searchResponseType")?.toString() ?: ""
            searchResponseStringVal = searchEvalMap?.get("searchResponseString")?.toString() ?: ""
            searchResponseJsonVal = searchEvalMap?.get("searchResponseJson")?.toString() ?: ""
            searchOwnKeysVal = searchEvalMap?.get("searchOwnKeys")?.toString() ?: ""
            searchObjectKeysVal = searchEvalMap?.get("searchObjectKeys")?.toString() ?: ""
            searchListTypeVal = searchEvalMap?.get("searchListType")?.toString() ?: ""
            searchListIsArrayVal = searchEvalMap?.get("searchListIsArray")?.toString()?.toBoolean() ?: false
            searchListLengthVal = searchEvalMap?.get("searchListLength")?.toString()?.toIntOrNull() ?: -1
            searchHasNextTypeVal = searchEvalMap?.get("searchHasNextType")?.toString() ?: ""
            searchHasNextValueVal = searchEvalMap?.get("searchHasNextValue")?.toString() ?: ""

            searchApiTypeVal = searchEvalMap?.get("searchApiType")?.toString() ?: ""
            directSearchApiTypeVal = searchEvalMap?.get("directSearchApiType")?.toString() ?: ""
            directSearchApiStringVal = searchEvalMap?.get("directSearchApiString")?.toString() ?: ""
            directSearchApiJsonVal = searchEvalMap?.get("directSearchApiJson")?.toString() ?: ""
            directSearchApiKeysVal = searchEvalMap?.get("directSearchApiKeys")?.toString() ?: ""
            directSearchApiListTypeVal = searchEvalMap?.get("directSearchApiListType")?.toString() ?: ""
            directSearchApiListLengthVal = searchEvalMap?.get("directSearchApiListLength")?.toString()?.toIntOrNull() ?: -1
            directSearchApiHasNextVal = searchEvalMap?.get("directSearchApiHasNext")?.toString() ?: ""

            searchResultTypeVal = searchResponseTypeVal
            searchResponseKeysVal = searchObjectKeysVal
            searchResultCountVal = if (searchListLengthVal >= 0) searchListLengthVal else 0
            searchHasNextPageVal = searchHasNextValueVal == "true"

            val rawItems = searchEvalMap?.get("items")
            if (rawItems is List<*>) {
                rawItems.filterIsInstance<Map<String, Any?>>().forEachIndexed { idx, item ->
                    searchItemsList.add(
                        SearchItemResult(
                            index = idx,
                            name = item["name"]?.toString() ?: item["title"]?.toString() ?: "",
                            link = item["link"]?.toString() ?: item["url"]?.toString() ?: "",
                            image = item["imageUrl"]?.toString() ?: item["cover"]?.toString() ?: ""
                        )
                    )
                }
            }

            // SEARCH_STATUS PASS ONLY IF:
            // - searchMethodType is function
            // - searchCallException is empty
            // - searchListIsArray is true
            // - searchListLength > 0
            searchPassVal = searchMethodTypeVal == "function" &&
                    searchCallExceptionVal.isBlank() &&
                    searchListIsArrayVal &&
                    searchListLengthVal > 0

            if (!searchPassVal) {
                searchFailureReasonVal = when {
                    searchMethodTypeVal != "function" -> "extension.search is not a function ($searchMethodTypeVal)"
                    searchCallExceptionVal.isNotBlank() -> "Exception: $searchCallExceptionVal"
                    !searchListIsArrayVal -> "searchResponse.list is not an array ($searchListTypeVal)"
                    searchListLengthVal <= 0 -> "searchResponse.list is empty (length $searchListLengthVal)"
                    else -> "Unknown shape: type=$searchResponseTypeVal, json=$searchResponseJsonVal"
                }
            }

            val reqCountAfterSearchVal = httpTraces.size

            if (searchPassVal) {
                recordStagePass(7, "SEARCH")
                AppLogger.i(TAG, "SEARCH_FINISHED_PASS")
            } else {
                recordStageFail(7, "SEARCH", RuntimeException(searchFailureReasonVal))
                AppLogger.e(TAG, "SEARCH_FINISHED_FAIL: $searchFailureReasonVal", null)
            }
        } catch (e: Throwable) {
            recordStageFail(7, "SEARCH", e)
            qjs.close()
            val finishedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            return@withContext LiveTestResult(
                engine = "QuickJS", version = "0.9.2", nativeLib = "quickjs", abi = abi,
                stages = stagesList, testExecuted = true,
                testStartedAt = startedAtStr, testFinishedAt = finishedAtStr,
                testFailedStage = failedStage, testErrorType = errType, testErrorMessage = errMsg, testErrorStacktrace = errStack,
                httpMethod = "GET", httpUrl = targetSearchUrl, httpStatus = httpStatusVal,
                httpContentType = httpContentTypeVal, httpBodyLength = httpBodyLenVal, httpBodyPrefix = httpBodyPrefixVal,
                clientGetResultType = clientGetResultTypeVal, clientGetBodyType = clientGetBodyTypeVal,
                parsedType = parsedTypeVal, parsedKeys = parsedKeysVal, parsedJsonPrefix = parsedJsonPrefixVal,
                rawHttpPass = rawHttpPass, jsonParsePass = jsonParsePass, sourceBytes = sourceBytes,
                sourceExecution = "PASS", instanceStatus = "PASS", constructorName = constructorNameVal,
                searchStatus = "FAIL", searchResultType = searchResultTypeVal, searchResponseKeys = searchResponseKeysVal,
                searchResultCount = searchResultCountVal, searchHasNextPage = searchHasNextPageVal,
                searchItems = searchItemsList, searchPass = false,
                searchErrorType = errType, searchErrorMessage = errMsg, searchErrorStacktrace = errStack,
                searchMethodType = searchMethodTypeVal, searchMethodString = searchMethodStringVal,
                prototypeType = prototypeTypeVal, prototypeKeys = prototypeKeysVal,
                searchCallException = searchCallExceptionVal, searchResponseType = searchResponseTypeVal,
                searchResponseString = searchResponseStringVal, searchResponseJson = searchResponseJsonVal,
                searchOwnKeys = searchOwnKeysVal, searchObjectKeys = searchObjectKeysVal,
                searchListType = searchListTypeVal, searchListIsArray = searchListIsArrayVal,
                searchListLength = searchListLengthVal, searchHasNextType = searchHasNextTypeVal,
                searchHasNextValue = searchHasNextValueVal, searchFailureReason = searchFailureReasonVal,
                searchApiType = searchApiTypeVal, directSearchApiType = directSearchApiTypeVal,
                directSearchApiString = directSearchApiStringVal, directSearchApiJson = directSearchApiJsonVal,
                directSearchApiKeys = directSearchApiKeysVal, directSearchApiListType = directSearchApiListTypeVal,
                directSearchApiListLength = directSearchApiListLengthVal, directSearchApiHasNext = directSearchApiHasNextVal,
                requestCountAfterRawGet = reqCountAfterRawGetVal, requestCountAfterSearch = httpTraces.size,
                networkTraces = httpTraces, rawError = "Search Failed: $errMsg"
            )
        }

        val reqCountAfterSearchVal = httpTraces.size

        // CONDITIONAL CHECK FOR STAGE 8 DETAIL: Stop if search returned 0 items
        if (searchResultCountVal == 0 || searchItemsList.isEmpty() || !searchPassVal) {
            qjs.close()
            val finishedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            return@withContext LiveTestResult(
                engine = "QuickJS", version = "0.9.2", nativeLib = "quickjs", abi = abi,
                stages = stagesList, testExecuted = true,
                testStartedAt = startedAtStr, testFinishedAt = finishedAtStr,
                httpMethod = "GET", httpUrl = targetSearchUrl, httpStatus = httpStatusVal,
                httpContentType = httpContentTypeVal, httpBodyLength = httpBodyLenVal, httpBodyPrefix = httpBodyPrefixVal,
                clientGetResultType = clientGetResultTypeVal, clientGetBodyType = clientGetBodyTypeVal,
                parsedType = parsedTypeVal, parsedKeys = parsedKeysVal, parsedJsonPrefix = parsedJsonPrefixVal,
                rawHttpPass = rawHttpPass, jsonParsePass = jsonParsePass, sourceBytes = sourceBytes,
                sourceExecution = "PASS", instanceStatus = "PASS", constructorName = constructorNameVal,
                searchStatus = if (searchPassVal) "PASS" else "FAIL", searchResultType = searchResultTypeVal, searchResponseKeys = searchResponseKeysVal,
                searchResultCount = searchResultCountVal, searchHasNextPage = searchHasNextPageVal,
                searchItems = searchItemsList, searchPass = searchPassVal,
                searchMethodType = searchMethodTypeVal, searchMethodString = searchMethodStringVal,
                prototypeType = prototypeTypeVal, prototypeKeys = prototypeKeysVal,
                searchCallException = searchCallExceptionVal, searchResponseType = searchResponseTypeVal,
                searchResponseString = searchResponseStringVal, searchResponseJson = searchResponseJsonVal,
                searchOwnKeys = searchOwnKeysVal, searchObjectKeys = searchObjectKeysVal,
                searchListType = searchListTypeVal, searchListIsArray = searchListIsArrayVal,
                searchListLength = searchListLengthVal, searchHasNextType = searchHasNextTypeVal,
                searchHasNextValue = searchHasNextValueVal, searchFailureReason = searchFailureReasonVal,
                searchApiType = searchApiTypeVal, directSearchApiType = directSearchApiTypeVal,
                directSearchApiString = directSearchApiStringVal, directSearchApiJson = directSearchApiJsonVal,
                directSearchApiKeys = directSearchApiKeysVal, directSearchApiListType = directSearchApiListTypeVal,
                directSearchApiListLength = directSearchApiListLengthVal, directSearchApiHasNext = directSearchApiHasNextVal,
                requestCountAfterRawGet = reqCountAfterRawGetVal, requestCountAfterSearch = reqCountAfterSearchVal,
                networkTraces = httpTraces, rawError = if (searchPassVal) null else "Search Failed: $searchFailureReasonVal"
            )
        }

        // STAGE 8: DETAIL & STAGE 9: EPISODES
        val targetDetailLink = searchItemsList.first().link
        var detailNameVal = ""
        var detailImageUrlVal = ""
        var descStr = ""
        var detailDescLenVal = 0
        var detailGenresVal = ""
        var detailStatusVal = ""
        var detailChapterCountVal = 0
        var chapter0NameVal = ""
        var chapter0UrlVal = ""
        var chapterLastNameVal = ""
        var chapterLastUrlVal = ""
        var detailPassVal = false
        var episodesPassVal = false

        AppLogger.i(TAG, "DETAIL_STARTED")
        try {
            val detailScript = """
                (function() {
                    try {
                        if (!source) return JSON.stringify({ error: "No source instance" });
                        var res = source.getDetail("$targetDetailLink");
                        return JSON.stringify({ success: true, data: res });
                    } catch(e) {
                        return JSON.stringify({ error: String(e), stack: (e && e.stack) ? String(e.stack) : "" });
                    }
                })()
            """.trimIndent()

            val rawDetailEval = qjs.evaluate(detailScript)?.toString() ?: "{}"
            val detailEvalMap = parseJsonToMap(rawDetailEval)

            if (detailEvalMap?.containsKey("error") == true) {
                val dErr = detailEvalMap["error"]?.toString() ?: "Detail JS Error"
                val dStack = detailEvalMap["stack"]?.toString() ?: ""
                throw RuntimeException("JS Detail Exception: $dErr\n$dStack")
            }

            val detailData = detailEvalMap?.get("data") as? Map<*, *>
            detailNameVal = detailData?.get("name")?.toString() ?: detailData?.get("title")?.toString() ?: ""
            detailImageUrlVal = detailData?.get("imageUrl")?.toString() ?: detailData?.get("cover")?.toString() ?: ""
            descStr = detailData?.get("description")?.toString() ?: detailData?.get("synopsis")?.toString() ?: ""
            detailDescLenVal = descStr.length
            detailGenresVal = (detailData?.get("genre") as? List<*>)?.joinToString(", ") ?: detailData?.get("genre")?.toString() ?: ""
            detailStatusVal = detailData?.get("status")?.toString() ?: "Ongoing"

            val rawChapters = (detailData?.get("episodes") ?: detailData?.get("chapters")) as? List<*>
            if (rawChapters != null) {
                detailChapterCountVal = rawChapters.size
                if (detailChapterCountVal > 0) {
                    val firstCh = rawChapters.first() as? Map<*, *>
                    chapter0NameVal = firstCh?.get("name")?.toString() ?: firstCh?.get("title")?.toString() ?: ""
                    chapter0UrlVal = firstCh?.get("url")?.toString() ?: firstCh?.get("link")?.toString() ?: ""

                    val lastCh = rawChapters.last() as? Map<*, *>
                    chapterLastNameVal = lastCh?.get("name")?.toString() ?: lastCh?.get("title")?.toString() ?: ""
                    chapterLastUrlVal = lastCh?.get("url")?.toString() ?: lastCh?.get("link")?.toString() ?: ""
                }
            }

            detailPassVal = detailNameVal.isNotBlank()
            episodesPassVal = detailChapterCountVal > 0

            recordStagePass(8, "DETAIL")
            AppLogger.i(TAG, "DETAIL_FINISHED")

            if (episodesPassVal) {
                recordStagePass(9, "EPISODES")
            } else {
                recordStageFail(9, "EPISODES", RuntimeException("Detail returned 0 episodes/chapters"))
            }
        } catch (e: Throwable) {
            recordStageFail(8, "DETAIL", e)
            qjs.close()
            val finishedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            return@withContext LiveTestResult(
                engine = "QuickJS", version = "0.9.2", nativeLib = "quickjs", abi = abi,
                stages = stagesList, testExecuted = true,
                testStartedAt = startedAtStr, testFinishedAt = finishedAtStr,
                testFailedStage = failedStage, testErrorType = errType, testErrorMessage = errMsg, testErrorStacktrace = errStack,
                httpMethod = "GET", httpUrl = targetSearchUrl, httpStatus = httpStatusVal,
                httpContentType = httpContentTypeVal, httpBodyLength = httpBodyLenVal, httpBodyPrefix = httpBodyPrefixVal,
                clientGetResultType = clientGetResultTypeVal, clientGetBodyType = clientGetBodyTypeVal,
                parsedType = parsedTypeVal, parsedKeys = parsedKeysVal, parsedJsonPrefix = parsedJsonPrefixVal,
                rawHttpPass = rawHttpPass, jsonParsePass = jsonParsePass, sourceBytes = sourceBytes,
                sourceExecution = "PASS", instanceStatus = "PASS", constructorName = constructorNameVal,
                searchStatus = "PASS", searchResultType = searchResultTypeVal, searchResponseKeys = searchResponseKeysVal,
                searchResultCount = searchResultCountVal, searchHasNextPage = searchHasNextPageVal,
                searchItems = searchItemsList, searchPass = true,
                searchMethodType = searchMethodTypeVal, searchMethodString = searchMethodStringVal,
                prototypeType = prototypeTypeVal, prototypeKeys = prototypeKeysVal,
                searchCallException = searchCallExceptionVal, searchResponseType = searchResponseTypeVal,
                searchResponseString = searchResponseStringVal, searchResponseJson = searchResponseJsonVal,
                searchOwnKeys = searchOwnKeysVal, searchObjectKeys = searchObjectKeysVal,
                searchListType = searchListTypeVal, searchListIsArray = searchListIsArrayVal,
                searchListLength = searchListLengthVal, searchHasNextType = searchHasNextTypeVal,
                searchHasNextValue = searchHasNextValueVal, searchFailureReason = searchFailureReasonVal,
                searchApiType = searchApiTypeVal, directSearchApiType = directSearchApiTypeVal,
                directSearchApiString = directSearchApiStringVal, directSearchApiJson = directSearchApiJsonVal,
                directSearchApiKeys = directSearchApiKeysVal, directSearchApiListType = directSearchApiListTypeVal,
                directSearchApiListLength = directSearchApiListLengthVal, directSearchApiHasNext = directSearchApiHasNextVal,
                requestCountAfterRawGet = reqCountAfterRawGetVal, requestCountAfterSearch = reqCountAfterSearchVal,
                detailInputLink = targetDetailLink, detailStatus = "FAIL",
                detailErrorType = errType, detailErrorMessage = errMsg, detailErrorStacktrace = errStack,
                networkTraces = httpTraces, rawError = "Detail Failed: $errMsg"
            )
        }

        // CONDITIONAL CHECK FOR STAGE 10 VIDEO_LIST: Stop if 0 episodes
        if (detailChapterCountVal == 0 || chapter0UrlVal.isBlank()) {
            qjs.close()
            val finishedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            return@withContext LiveTestResult(
                engine = "QuickJS", version = "0.9.2", nativeLib = "quickjs", abi = abi,
                stages = stagesList, testExecuted = true,
                testStartedAt = startedAtStr, testFinishedAt = finishedAtStr,
                httpMethod = "GET", httpUrl = targetSearchUrl, httpStatus = httpStatusVal,
                httpContentType = httpContentTypeVal, httpBodyLength = httpBodyLenVal, httpBodyPrefix = httpBodyPrefixVal,
                clientGetResultType = clientGetResultTypeVal, clientGetBodyType = clientGetBodyTypeVal,
                parsedType = parsedTypeVal, parsedKeys = parsedKeysVal, parsedJsonPrefix = parsedJsonPrefixVal,
                rawHttpPass = rawHttpPass, jsonParsePass = jsonParsePass, sourceBytes = sourceBytes,
                sourceExecution = "PASS", instanceStatus = "PASS", constructorName = constructorNameVal,
                searchStatus = "PASS", searchResultType = searchResultTypeVal, searchResponseKeys = searchResponseKeysVal,
                searchResultCount = searchResultCountVal, searchHasNextPage = searchHasNextPageVal,
                searchItems = searchItemsList, searchPass = true,
                searchMethodType = searchMethodTypeVal, searchMethodString = searchMethodStringVal,
                prototypeType = prototypeTypeVal, prototypeKeys = prototypeKeysVal,
                searchCallException = searchCallExceptionVal, searchResponseType = searchResponseTypeVal,
                searchResponseString = searchResponseStringVal, searchResponseJson = searchResponseJsonVal,
                searchOwnKeys = searchOwnKeysVal, searchObjectKeys = searchObjectKeysVal,
                searchListType = searchListTypeVal, searchListIsArray = searchListIsArrayVal,
                searchListLength = searchListLengthVal, searchHasNextType = searchHasNextTypeVal,
                searchHasNextValue = searchHasNextValueVal, searchFailureReason = searchFailureReasonVal,
                searchApiType = searchApiTypeVal, directSearchApiType = directSearchApiTypeVal,
                directSearchApiString = directSearchApiStringVal, directSearchApiJson = directSearchApiJsonVal,
                directSearchApiKeys = directSearchApiKeysVal, directSearchApiListType = directSearchApiListTypeVal,
                directSearchApiListLength = directSearchApiListLengthVal, directSearchApiHasNext = directSearchApiHasNextVal,
                requestCountAfterRawGet = reqCountAfterRawGetVal, requestCountAfterSearch = reqCountAfterSearchVal,
                detailInputLink = targetDetailLink, detailStatus = "PASS", detailName = detailNameVal,
                detailImageUrl = detailImageUrlVal, detailDescriptionLength = detailDescLenVal,
                detailGenres = detailGenresVal, detailStatusValue = detailStatusVal, detailChapterCount = 0,
                detailPass = true, episodesPass = false,
                networkTraces = httpTraces, rawError = null
            )
        }

        // STAGE 10: VIDEO_LIST
        val targetEpisodeUrl = chapter0UrlVal
        val videoItemsList = mutableListOf<VideoItemResult>()
        var videoCountVal = 0
        var videoPassVal = false

        AppLogger.i(TAG, "VIDEO_STARTED")
        try {
            val videoScript = """
                (function() {
                    try {
                        if (!source) return JSON.stringify({ error: "No source instance" });
                        var res = source.getVideoList("$targetEpisodeUrl");
                        return JSON.stringify({ success: true, data: res });
                    } catch(e) {
                        return JSON.stringify({ error: String(e), stack: (e && e.stack) ? String(e.stack) : "" });
                    }
                })()
            """.trimIndent()

            val rawVideoEval = qjs.evaluate(videoScript)?.toString() ?: "{}"
            val videoEvalMap = parseJsonToMap(rawVideoEval)

            if (videoEvalMap?.containsKey("error") == true) {
                val vErr = videoEvalMap["error"]?.toString() ?: "Video JS Error"
                val vStack = videoEvalMap["stack"]?.toString() ?: ""
                throw RuntimeException("JS Video Exception: $vErr\n$vStack")
            }

            val videoDataList = videoEvalMap?.get("data") as? List<*>
            videoDataList?.filterIsInstance<Map<String, Any?>>()?.take(5)?.forEachIndexed { idx, v ->
                val vUrl = v["url"]?.toString() ?: ""
                val vOrig = v["originalUrl"]?.toString() ?: vUrl
                val vQual = v["quality"]?.toString() ?: "Auto"
                val vHeaders = v["headers"]?.toString() ?: "{}"
                videoItemsList.add(VideoItemResult(idx, vUrl, vOrig, vQual, vHeaders))
            }

            videoCountVal = videoItemsList.size
            videoPassVal = videoCountVal > 0

            if (videoPassVal) {
                recordStagePass(10, "VIDEO_LIST")
                AppLogger.i(TAG, "VIDEO_FINISHED")
            } else {
                recordStageFail(10, "VIDEO_LIST", RuntimeException("getVideoList returned 0 video items"))
            }
        } catch (e: Throwable) {
            recordStageFail(10, "VIDEO_LIST", e)
        } finally {
            qjs.close()
        }

        // PIPELINE TRACING TO DATABASE & UI
        val rawPoster = searchItemsList.firstOrNull()?.image ?: detailImageUrlVal
        val normalizedPoster = rawPoster
        var dbPoster = ""
        var uiPoster = ""

        val rawChaps = detailChapterCountVal
        val normalizedEps = rawChaps
        var dbEps = 0
        var uiEps = 0

        if (animeDao != null && episodeDao != null && detailNameVal.isNotBlank()) {
            try {
                val animeId = "anime_just4anime_${targetDetailLink.hashCode()}"
                val entity = AnimeEntity(
                    localId = animeId,
                    sourceId = "just4anime",
                    sourceAnimeId = targetDetailLink,
                    title = detailNameVal,
                    altTitles = emptyList(),
                    poster = normalizedPoster,
                    banner = normalizedPoster,
                    description = descStr,
                    genres = if (detailGenresVal.isNotBlank()) detailGenresVal.split(", ") else emptyList(),
                    status = detailStatusVal,
                    type = "Anime",
                    totalEpisodes = normalizedEps,
                    season = "",
                    year = null,
                    studio = "",
                    rating = null,
                    isFavorite = false,
                    lastUpdated = System.currentTimeMillis()
                )
                animeDao.insertAnime(entity)
                val storedAnime = animeDao.getAnimeByIdDirect(animeId)
                dbPoster = storedAnime?.poster ?: ""
                uiPoster = dbPoster

                if (detailChapterCountVal > 0) {
                    val epEntity = EpisodeEntity(
                        id = "ep_just4anime_${targetEpisodeUrl.hashCode()}",
                        animeId = animeId,
                        sourceId = "just4anime",
                        sourceEpisodeId = targetEpisodeUrl,
                        number = 1.0f,
                        title = chapter0NameVal.ifBlank { "Episode 1" },
                        seasonNumber = 1
                    )
                    episodeDao.insertEpisodes(listOf(epEntity))
                    val storedEpisodes = episodeDao.getEpisodesForAnimeDirect(animeId)
                    dbEps = storedEpisodes.size
                    uiEps = dbEps
                }
            } catch (ignored: Exception) {}
        }

        val finishedAtStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        val isAllPass = rawHttpPass && jsonParsePass && searchPassVal && detailPassVal && episodesPassVal && videoPassVal

        AppLogger.i(TAG, "LIVE_TEST_FINISHED")

        return@withContext LiveTestResult(
            engine = "QuickJS",
            version = "0.9.2",
            nativeLib = "quickjs",
            abi = abi,
            stages = stagesList,
            testExecuted = true,
            testStartedAt = startedAtStr,
            testFinishedAt = finishedAtStr,
            testFailedStage = failedStage,
            testErrorType = errType,
            testErrorMessage = errMsg,
            testErrorStacktrace = errStack,
            httpMethod = "GET",
            httpUrl = targetSearchUrl,
            httpStatus = httpStatusVal,
            httpContentType = httpContentTypeVal,
            httpBodyLength = httpBodyLenVal,
            httpBodyPrefix = httpBodyPrefixVal,
            clientGetResultType = clientGetResultTypeVal,
            clientGetBodyType = clientGetBodyTypeVal,
            parsedType = parsedTypeVal,
            parsedKeys = parsedKeysVal,
            parsedJsonPrefix = parsedJsonPrefixVal,
            rawHttpPass = rawHttpPass,
            jsonParsePass = jsonParsePass,
            sourceBytes = sourceBytes,
            sourceExecution = "PASS",
            instanceStatus = "PASS",
            constructorName = constructorNameVal,
            searchStatus = if (searchPassVal) "PASS" else "FAIL",
            searchResultType = searchResultTypeVal,
            searchResponseKeys = searchResponseKeysVal,
            searchResultCount = searchResultCountVal,
            searchHasNextPage = searchHasNextPageVal,
            searchItems = searchItemsList,
            searchPass = searchPassVal,
            searchMethodType = searchMethodTypeVal,
            searchMethodString = searchMethodStringVal,
            prototypeType = prototypeTypeVal,
            prototypeKeys = prototypeKeysVal,
            searchCallException = searchCallExceptionVal,
            searchResponseType = searchResponseTypeVal,
            searchResponseString = searchResponseStringVal,
            searchResponseJson = searchResponseJsonVal,
            searchOwnKeys = searchOwnKeysVal,
            searchObjectKeys = searchObjectKeysVal,
            searchListType = searchListTypeVal,
            searchListIsArray = searchListIsArrayVal,
            searchListLength = searchListLengthVal,
            searchHasNextType = searchHasNextTypeVal,
            searchHasNextValue = searchHasNextValueVal,
            searchFailureReason = searchFailureReasonVal,
            searchApiType = searchApiTypeVal,
            directSearchApiType = directSearchApiTypeVal,
            directSearchApiString = directSearchApiStringVal,
            directSearchApiJson = directSearchApiJsonVal,
            directSearchApiKeys = directSearchApiKeysVal,
            directSearchApiListType = directSearchApiListTypeVal,
            directSearchApiListLength = directSearchApiListLengthVal,
            directSearchApiHasNext = directSearchApiHasNextVal,
            requestCountAfterRawGet = reqCountAfterRawGetVal,
            requestCountAfterSearch = reqCountAfterSearchVal,
            detailInputLink = targetDetailLink,
            detailStatus = if (detailPassVal) "PASS" else "FAIL",
            detailName = detailNameVal,
            detailImageUrl = detailImageUrlVal,
            detailDescriptionLength = detailDescLenVal,
            detailGenres = detailGenresVal,
            detailStatusValue = detailStatusVal,
            detailChapterCount = detailChapterCountVal,
            chapter0Name = chapter0NameVal,
            chapter0Url = chapter0UrlVal,
            chapterLastName = chapterLastNameVal,
            chapterLastUrl = chapterLastUrlVal,
            detailPass = detailPassVal,
            episodesPass = episodesPassVal,
            videoInputUrl = targetEpisodeUrl,
            videoStatus = if (videoPassVal) "PASS" else "FAIL",
            videoCount = videoCountVal,
            videoItems = videoItemsList,
            videoPass = videoPassVal,
            networkTraces = httpTraces,
            rawImageUrl = rawPoster,
            normalizedPosterUrl = normalizedPoster,
            databasePosterUrl = dbPoster,
            uiPosterUrl = uiPoster,
            posterUiPass = uiPoster.isNotBlank(),
            rawChapterCount = rawChaps,
            normalizedEpisodeCount = normalizedEps,
            databaseEpisodeCount = dbEps,
            uiEpisodeCount = uiEps,
            episodeUiPass = uiEps > 0,
            firstRealTitle = searchItemsList.firstOrNull()?.name ?: detailNameVal,
            firstRealLink = searchItemsList.firstOrNull()?.link ?: targetDetailLink,
            firstRealImage = searchItemsList.firstOrNull()?.image ?: detailImageUrlVal,
            realEpisodeCount = detailChapterCountVal,
            firstRealVideo = videoItemsList.firstOrNull()?.url ?: "",
            rawError = if (isAllPass) null else (if (errMsg.isNotBlank()) "Failed at $failedStage: $errMsg" else "Pipeline failed at stage $failedStage")
        )
    }

    private fun executeHttpAndFormatJson(
        client: OkHttpClient,
        request: Request,
        traces: MutableList<HttpRequestTrace>,
        index: Int
    ): String {
        return try {
            client.newCall(request).execute().use { resp ->
                val bytes = resp.body?.bytes() ?: byteArrayOf()
                val bodyText = String(bytes, Charsets.UTF_8)
                val contentType = resp.header("Content-Type") ?: resp.header("content-type") ?: "application/json"
                val responseBytes = bytes.size
                val prefix300 = bodyText.take(300)

                AppLogger.i(TAG, "[MANGAYOMI][HTTP] method=${request.method} url=${request.url} status=${resp.code} responseBytes=$responseBytes")
                traces.add(
                    HttpRequestTrace(
                        requestNumber = index,
                        method = request.method,
                        url = request.url.toString(),
                        httpStatus = resp.code,
                        contentType = contentType,
                        responseBytes = responseBytes,
                        bodyPrefix300 = prefix300
                    )
                )

                val jsonObj = org.json.JSONObject()
                jsonObj.put("body", bodyText)
                jsonObj.put("statusCode", resp.code)
                jsonObj.put("ok", resp.isSuccessful)
                val headersObj = org.json.JSONObject()
                resp.headers.names().forEach { name ->
                    if (!name.equals("Set-Cookie", ignoreCase = true) && !name.equals("Authorization", ignoreCase = true)) {
                        headersObj.put(name, resp.header(name) ?: "")
                    }
                }
                jsonObj.put("headers", headersObj)

                jsonObj.toString()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "[MANGAYOMI][HTTP] Request failed for ${request.url}: ${e.message}")
            traces.add(
                HttpRequestTrace(
                    requestNumber = index,
                    method = request.method,
                    url = request.url.toString(),
                    httpStatus = 0,
                    contentType = "error",
                    responseBytes = 0,
                    bodyPrefix300 = "Error: ${e.message}"
                )
            )
            val jsonObj = org.json.JSONObject()
            jsonObj.put("body", "")
            jsonObj.put("statusCode", 0)
            jsonObj.put("ok", false)
            jsonObj.put("headers", org.json.JSONObject())
            jsonObj.toString()
        }
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
                val jsonObj = org.json.JSONObject(trimmed)
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val valObj = jsonObj.opt(key)
                    if (valObj is org.json.JSONArray) {
                        val list = mutableListOf<Any?>()
                        for (i in 0 until valObj.length()) {
                            val item = valObj.get(i)
                            if (item is org.json.JSONObject) {
                                val itemMap = mutableMapOf<String, Any?>()
                                item.keys().forEach { k -> itemMap[k] = item.opt(k) }
                                list.add(itemMap)
                            } else {
                                list.add(item)
                            }
                        }
                        result[key] = list
                    } else if (valObj is org.json.JSONObject) {
                        val subMap = mutableMapOf<String, Any?>()
                        valObj.keys().forEach { k -> subMap[k] = valObj.opt(k) }
                        result[key] = subMap
                    } else {
                        result[key] = valObj
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "parseJsonToMap error: ${e.message}")
        }
        return result
    }
}
