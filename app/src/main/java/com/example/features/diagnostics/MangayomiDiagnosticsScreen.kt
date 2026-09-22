package com.example.features.diagnostics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangayomiDiagnosticsScreen(
    viewModel: MangayomiDiagnosticsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.liveTestResult == null && !uiState.isLiveTesting) {
            viewModel.runLiveExtensionTest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mangayomi Runtime Diagnostics") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("diag_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runSelfTest() },
                        modifier = Modifier.testTag("diag_refresh_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "MANGAYOMI RUNTIME DIAGNOSTICS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "QuickJS Engine & Mangayomi Extension Test Suite",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SINGLE Overall Diagnostic Status Banner
            val suiteStatus = when {
                uiState.isLiveTesting -> "RUNNING"
                uiState.liveTestResult == null -> "READY"
                uiState.liveTestResult?.let { live ->
                    live.rawError == null && live.stages.isNotEmpty() && live.stages.all { it.pass }
                } == true -> "PASSED"
                else -> "FAILED"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (suiteStatus) {
                        "PASSED" -> Color(0xFFE8F5E9)
                        "FAILED" -> Color(0xFFFFEBEE)
                        "RUNNING" -> Color(0xFFE3F2FD)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (suiteStatus) {
                        "RUNNING" -> CircularProgressIndicator(modifier = Modifier.height(24.dp).width(24.dp), strokeWidth = 2.dp)
                        "PASSED" -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                        "FAILED" -> Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFC62828))
                        else -> Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Diagnostic Status: $suiteStatus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (suiteStatus) {
                                "PASSED" -> Color(0xFF2E7D32)
                                "FAILED" -> Color(0xFFC62828)
                                "RUNNING" -> Color(0xFF1565C0)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = "ENGINE = QuickJS 0.9.2 | ABI = android",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        if (uiState.isLiveTesting) {
                            Text(
                                text = "LIVE TEST STATUS: STARTING / RUNNING...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Self Test Results
            uiState.selfTestResult?.let { res ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "1. Engine Environment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("Engine", res.engineName)
                        DiagRow("Version", res.engineVersion)
                        DiagRow("Legacy Rhino Proof", res.rhinoErrorSummary)

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "2. QuickJS ES2020 Capability Check",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagTestRow("CLASS test", res.classTest)
                        DiagTestRow("CLASS EXPRESSION test", res.classExprTest)
                        DiagTestRow("PROMISE test", res.promiseTest)
                        DiagTestRow("ASYNC test", res.asyncTest)
                        DiagTestRow("PREAMBLE test", res.preambleTest)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button for Live Extension Test
            Button(
                onClick = { viewModel.runLiveExtensionTest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("run_live_test_btn"),
                enabled = !uiState.isLiveTesting
            ) {
                if (uiState.isLiveTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing Diagnostic Suite...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RUN LIVE JUST4ANIME TEST")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Test Results Section
            uiState.liveTestResult?.let { live ->
                // STAGE-BY-STAGE EXECUTION LOG CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "STAGE-BY-STAGE PIPELINE LOG",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("TEST_EXECUTED", "${live.testExecuted}")
                        DiagRow("TEST_STARTED_AT", live.testStartedAt)
                        DiagRow("TEST_FINISHED_AT", live.testFinishedAt)

                        Spacer(modifier = Modifier.height(8.dp))
                        live.stages.forEach { stage ->
                            DiagTestRow("STAGE ${stage.stageNumber}: ${stage.stageName}", if (stage.pass) "PASS" else "FAIL")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // FAILURE DETAILS CARD (If failed)
                if (live.rawError != null || live.testFailedStage.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFC62828))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FAILURE DETAILED BREAKDOWN",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            DiagRow("TEST_FAILED_STAGE", live.testFailedStage.ifBlank { "NONE" })
                            DiagRow("TEST_ERROR_TYPE", live.testErrorType.ifBlank { "NONE" })
                            DiagRow("TEST_ERROR_MESSAGE", live.testErrorMessage.ifBlank { live.rawError ?: "NONE" })

                            if (live.testErrorStacktrace.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("TEST_ERROR_STACKTRACE:", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = live.testErrorStacktrace,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFB71C1C)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // RAW HTTP RESULT CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1. RAW Client.get() & JSON.parse DIAGNOSTICS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("HTTP_METHOD", live.httpMethod)
                        DiagRow("HTTP_URL", live.httpUrl)
                        DiagRow("HTTP_STATUS", "${live.httpStatus}")
                        DiagRow("HTTP_CONTENT_TYPE", live.httpContentType)
                        DiagRow("HTTP_HEADERS", live.httpHeadersStr)

                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("RAW_RESPONSE_JS_TYPE", live.rawResponseJsType)
                        DiagRow("RAW_BODY_JS_TYPE", live.rawBodyJsType)
                        DiagRow("RAW_BODY_IS_STRING", "${live.rawBodyIsString}")
                        DiagRow("RAW_BODY_LENGTH", "${live.httpBodyLength}")
                        DiagRow("RESPONSE_IS_NOT_JSON", "${live.responseIsNotJson}")

                        Spacer(modifier = Modifier.height(8.dp))
                        DiagTestRow("RAW_CLIENT_GET", if (live.rawHttpPass) "PASS" else "FAIL")
                        DiagTestRow("JSON_PARSE", if (live.jsonParsePass) "PASS" else "FAIL")

                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("JSON_SHAPE", live.jsonShape)
                        if (!live.jsonParsePass) {
                            DiagRow("JSON_ERROR_TYPE", live.jsonErrorType)
                            DiagRow("JSON_ERROR_MESSAGE", live.jsonErrorMessage)
                            if (live.errorPosition.isNotBlank()) {
                                DiagRow("ERROR_POSITION", live.errorPosition)
                            }
                            if (live.jsonErrorStack.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("JSON_ERROR_STACK:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = live.jsonErrorStack,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("BODY_FIRST_CHAR_CODE", "${live.bodyFirstCharCode}")
                        DiagRow("BODY_LAST_CHAR_CODE", "${live.bodyLastCharCode}")
                        DiagRow("BODY_CHAR_CODES", live.bodyCharCodes)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("RAW_BODY_PREFIX_1000:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = live.rawBodyPrefix1000.ifBlank { live.httpBodyPrefix },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("RAW_BODY_SUFFIX_300:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = live.rawBodySuffix300,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("PARSED_TYPE", live.parsedType)
                        DiagRow("PARSED_KEYS", live.parsedKeys)
                        DiagRow("PARSED_JSON_PREFIX", live.parsedJsonPrefix)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1B. DIRECT NATIVE OKHTTP URL VARIANT TESTS CARD
                // 1B. DIRECT NATIVE OKHTTP URL VARIANT TESTS (Tasks 1, 2, 3, 7, 8)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1B. NATIVE OKHTTP URL VARIANT & CONTROL TESTS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        // TASK 7: OkHttp Timeouts
                        DiagRow("CONNECT_TIMEOUT_MS", "${live.task7_connectTimeoutMs}ms")
                        DiagRow("READ_TIMEOUT_MS", "${live.task7_readTimeoutMs}ms")
                        DiagRow("WRITE_TIMEOUT_MS", "${live.task7_writeTimeoutMs}ms")
                        DiagRow("CALL_TIMEOUT_MS", "${live.task7_callTimeoutMs}ms")
                        Spacer(modifier = Modifier.height(8.dp))

                        // TEST A
                        DiagTestRow("TEST A (page=1&perPage=5)", if (live.testA.status in 200..299) "PASS (${live.testA.status}) ${live.testA.elapsedMs}ms" else "FAIL (${live.testA.status}) [${live.testA.timeoutPhase}]")
                        if (live.testA.exceptionClass.isNotBlank()) {
                            DiagRow("TEST_A_EXCEPTION", "${live.testA.exceptionClass}: ${live.testA.exceptionMessage}")
                        }

                        // TEST B
                        DiagTestRow("TEST B (page=1&perPage=20)", if (live.testB.status in 200..299) "PASS (${live.testB.status}) ${live.testB.elapsedMs}ms" else "FAIL (${live.testB.status}) [${live.testB.timeoutPhase}]")
                        if (live.testB.exceptionClass.isNotBlank()) {
                            DiagRow("TEST_B_EXCEPTION", "${live.testB.exceptionClass}: ${live.testB.exceptionMessage}")
                        }

                        // TEST C
                        DiagTestRow("TEST C (page=1&perPage=5&sort)", if (live.testC.status in 200..299) "PASS (${live.testC.status}) ${live.testC.elapsedMs}ms" else "FAIL (${live.testC.status}) [${live.testC.timeoutPhase}]")
                        if (live.testC.exceptionClass.isNotBlank()) {
                            DiagRow("TEST_C_EXCEPTION", "${live.testC.exceptionClass}: ${live.testC.exceptionMessage}")
                        }

                        // TEST D
                        DiagTestRow("TEST D (page=1&perPage=20&sort)", if (live.testD.status in 200..299) "PASS (${live.testD.status}) ${live.testD.elapsedMs}ms" else "FAIL (${live.testD.status}) [${live.testD.timeoutPhase}]")
                        if (live.testD.exceptionClass.isNotBlank()) {
                            DiagRow("TEST_D_TIMEOUT_PHASE", live.testD.timeoutPhase)
                            DiagRow("TEST_D_EXCEPTION", "${live.testD.exceptionClass}: ${live.testD.exceptionMessage}")
                            DiagRow("TEST_D_ROOT_CAUSE", "${live.testD.rootCauseClass}: ${live.testD.rootCauseMessage}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("TASK 8: CONTROL COMPARISON (TEST D)", fontWeight = FontWeight.Bold)
                        DiagTestRow("STANDALONE_OKHTTP", if (live.testD_Standalone.status in 200..299) "PASS (${live.testD_Standalone.status}) ${live.testD_Standalone.elapsedMs}ms" else "FAIL (${live.testD_Standalone.status}) [${live.testD_Standalone.timeoutPhase}]")
                        DiagTestRow("QUICKJS_BRIDGE_OKHTTP", if (live.testD_Bridge.status in 200..299) "PASS (${live.testD_Bridge.status}) ${live.testD_Bridge.elapsedMs}ms" else "FAIL (${live.testD_Bridge.status}) [${live.testD_Bridge.timeoutPhase}]")
                        if (live.testD_Bridge.exceptionClass.isNotBlank()) {
                            DiagRow("BRIDGE_EXCEPTION", "${live.testD_Bridge.exceptionClass}: ${live.testD_Bridge.exceptionMessage}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1C. URL CONSTRUCTION & ENCODING VERIFICATION (Tasks 4, 5, 6)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1C. URL ENCODING & BRIDGE VERIFICATION (TASKS 4, 5, 6)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        // TASK 4
                        Text("TASK 4: HttpUrl.Builder", fontWeight = FontWeight.Bold)
                        DiagRow("TASK4_BUILT_URL", live.task4_builtUrl)
                        DiagRow("TASK4_EXPECTED_URL", live.task4_expectedUrl)
                        DiagTestRow("TASK4_URLS_MATCH", if (live.task4_matchesExpected) "PASS" else "FAIL")

                        Spacer(modifier = Modifier.height(8.dp))

                        // TASK 5
                        Text("TASK 5: QuickJS Encoding", fontWeight = FontWeight.Bold)
                        DiagRow("TASK5_STRINGIFY", live.task5_stringify)
                        DiagRow("TASK5_ENCODED_1", live.task5_encoded1)
                        DiagRow("TASK5_ENCODED_2", live.task5_encoded2)
                        DiagTestRow("TASK5_ENCODING_MATCHES", if (live.task5_matchesExpected) "PASS" else "FAIL")

                        Spacer(modifier = Modifier.height(8.dp))

                        // TASK 6
                        Text("TASK 6: Direct vs Extension Bridge Request", fontWeight = FontWeight.Bold)
                        DiagRow("TASK6_DIRECT_URL", live.task6_directUrl)
                        DiagRow("TASK6_EXTENSION_URL", live.task6_extensionUrl)
                        DiagTestRow("TASK6_URLS_MATCH", if (live.task6_urlsMatch) "PASS" else "FAIL")
                        DiagRow("TASK6_DIRECT_HEADERS", live.task6_directSafeHeaders)
                        DiagRow("TASK6_EXTENSION_HEADERS", live.task6_extensionSafeHeaders)

                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("LAST_BRIDGE_URL", live.lastBridgeUrl)
                        DiagRow("LAST_BRIDGE_HEADERS", live.lastBridgeHeadersNames)
                        if (live.lastBridgeError.isNotBlank()) {
                            DiagRow("LAST_BRIDGE_ERROR", live.lastBridgeError)
                            DiagRow("LAST_BRIDGE_ERR_CLASS", live.lastBridgeErrorClass)
                            DiagRow("LAST_BRIDGE_ERR_MSG", live.lastBridgeErrorMessage)
                            DiagRow("LAST_BRIDGE_ROOT_CAUSE_CLASS", live.lastBridgeRootCauseClass)
                            DiagRow("LAST_BRIDGE_ROOT_CAUSE_MSG", live.lastBridgeRootCauseMessage)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1C. RUNTIME OBJECT INTROSPECTION CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1C. RUNTIME OBJECT INTROSPECTION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("DEFAULT_EXTENSION_TYPE", live.defaultExtensionType)
                        DiagRow("DEFAULT_EXTENSION_PROTO", live.defaultExtensionProto)
                        DiagRow("DEFAULT_EXTENSION_METHODS", live.defaultExtensionMethods)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("SOURCE_TYPE", live.sourceType)
                        DiagRow("SOURCE_PROTO", live.sourceProto)
                        DiagRow("SOURCE_METHODS", live.sourceMethods)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("EXTENSION_TYPE", live.extensionType)
                        DiagRow("EXTENSION_PROTO", live.extensionProto)
                        DiagRow("EXTENSION_METHODS", live.extensionMethods)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("SOURCE_EQUALS_EXTENSION", "${live.sourceEqualsExtension}")
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("SOURCE_SEARCH_TYPE", live.sourceSearchType)
                        DiagRow("SOURCE_DETAIL_TYPE", live.sourceDetailType)
                        DiagRow("SOURCE_VIDEO_TYPE", live.sourceVideoType)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("EXTENSION_SEARCH_TYPE", live.extensionSearchType)
                        DiagRow("EXTENSION_DETAIL_TYPE", live.extensionDetailType)
                        DiagRow("EXTENSION_VIDEO_TYPE", live.extensionVideoType)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SEARCH RESULT CARD & DEEP INSPECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. REAL SEARCH TEST & DEEP INSPECTION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagTestRow("SEARCH_STATUS", live.searchStatus)
                        if (live.searchFailureReason.isNotBlank()) {
                            DiagRow("SEARCH_FAILURE_REASON", live.searchFailureReason)
                        }
                        DiagRow("SEARCH_METHOD_TYPE", live.searchMethodType)
                        DiagRow("SEARCH_METHOD_STRING", live.searchMethodString)
                        DiagRow("PROTOTYPE_TYPE", live.prototypeType)
                        DiagRow("PROTOTYPE_KEYS", live.prototypeKeys)
                        DiagRow("SEARCH_CALL_EXCEPTION", live.searchCallException)
                        DiagRow("SEARCH_RESPONSE_TYPE", live.searchResponseType)
                        DiagRow("SEARCH_RESPONSE_STRING", live.searchResponseString)
                        DiagRow("SEARCH_RESPONSE_JSON", live.searchResponseJson)
                        DiagRow("SEARCH_OWN_KEYS", live.searchOwnKeys)
                        DiagRow("SEARCH_OBJECT_KEYS", live.searchObjectKeys)
                        DiagRow("SEARCH_LIST_TYPE", live.searchListType)
                        DiagRow("SEARCH_LIST_IS_ARRAY", "${live.searchListIsArray}")
                        DiagRow("SEARCH_LIST_LENGTH", "${live.searchListLength}")
                        DiagRow("SEARCH_HAS_NEXT_TYPE", live.searchHasNextType)
                        DiagRow("SEARCH_HAS_NEXT_VALUE", live.searchHasNextValue)
                        DiagRow("SEARCH_RESULT_COUNT", "${live.searchResultCount}")
                        DiagRow("SEARCH_HAS_NEXT_PAGE", "${live.searchHasNextPage}")

                        if (live.searchErrorType != null) {
                            DiagRow("SEARCH_ERROR_TYPE", live.searchErrorType)
                            DiagRow("SEARCH_ERROR_MESSAGE", live.searchErrorMessage ?: "")
                        }

                        if (live.searchItems.isEmpty()) {
                            DiagRow("SEARCH_RETURNED_ZERO", "True")
                        } else {
                            live.searchItems.forEach { item ->
                                DiagRow("RESULT_${item.index}_NAME", item.name)
                                DiagRow("RESULT_${item.index}_LINK", item.link)
                                DiagRow("RESULT_${item.index}_IMAGE", item.image)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DIRECT searchApi INSPECTION CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2B. DIRECT searchApi INSPECTION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("SEARCH_API_TYPE", live.searchApiType)
                        DiagRow("DIRECT_SEARCH_API_TYPE", live.directSearchApiType)
                        DiagRow("DIRECT_SEARCH_API_STRING", live.directSearchApiString)
                        DiagRow("DIRECT_SEARCH_API_JSON", live.directSearchApiJson)
                        DiagRow("DIRECT_SEARCH_API_KEYS", live.directSearchApiKeys)
                        DiagRow("DIRECT_SEARCH_API_LIST_TYPE", live.directSearchApiListType)
                        DiagRow("DIRECT_SEARCH_API_LIST_LENGTH", "${live.directSearchApiListLength}")
                        DiagRow("DIRECT_SEARCH_API_HAS_NEXT", live.directSearchApiHasNext)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DETAIL RESULT CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("3. REAL DETAIL TEST", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("DETAIL_INPUT_LINK", live.detailInputLink)
                        DiagRow("DETAIL_STATUS", live.detailStatus)
                        DiagRow("DETAIL_NAME", live.detailName)
                        DiagRow("DETAIL_IMAGE_URL", live.detailImageUrl)
                        DiagRow("DETAIL_DESCRIPTION_LENGTH", "${live.detailDescriptionLength}")
                        DiagRow("DETAIL_GENRES", live.detailGenres)
                        DiagRow("DETAIL_STATUS_VALUE", live.detailStatusValue)
                        DiagRow("DETAIL_CHAPTER_COUNT", "${live.detailChapterCount}")
                        DiagRow("CHAPTER_0_NAME", live.chapter0Name)
                        DiagRow("CHAPTER_0_URL", live.chapter0Url)
                        DiagRow("CHAPTER_LAST_NAME", live.chapterLastName)
                        DiagRow("CHAPTER_LAST_URL", live.chapterLastUrl)

                        if (live.detailErrorType != null) {
                            DiagRow("DETAIL_ERROR_TYPE", live.detailErrorType)
                            DiagRow("DETAIL_ERROR_MESSAGE", live.detailErrorMessage ?: "")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // VIDEO RESULT CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("4. REAL VIDEO TEST", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("VIDEO_INPUT_URL", live.videoInputUrl)
                        DiagRow("VIDEO_STATUS", live.videoStatus)
                        DiagRow("VIDEO_COUNT", "${live.videoCount}")

                        if (live.videoErrorType != null) {
                            DiagRow("VIDEO_ERROR_TYPE", live.videoErrorType)
                            DiagRow("VIDEO_ERROR_MESSAGE", live.videoErrorMessage ?: "")
                        }

                        live.videoItems.forEach { v ->
                            DiagRow("VIDEO_${v.index}_QUALITY", v.quality)
                            DiagRow("VIDEO_${v.index}_URL", v.url)
                            DiagRow("VIDEO_${v.index}_ORIGINAL_URL", v.originalUrl)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // NETWORK TRACE CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("5. NETWORK INSTRUMENTATION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("REQUEST_COUNT_AFTER_RAW_GET", "${live.requestCountAfterRawGet}")
                        DiagRow("REQUEST_COUNT_AFTER_SEARCH", "${live.requestCountAfterSearch}")
                        DiagRow("TOTAL_REQUESTS_LOGGED", "${live.networkTraces.size}")
                        Spacer(modifier = Modifier.height(8.dp))
                        if (live.networkTraces.isEmpty()) {
                            DiagRow("NETWORK_TRACES", "None recorded")
                        } else {
                            live.networkTraces.forEach { trace ->
                                Text("REQUEST #${trace.requestNumber}", fontWeight = FontWeight.Bold)
                                DiagRow("METHOD", trace.method)
                                DiagRow("URL", trace.url)
                                DiagRow("STATUS", "${trace.httpStatus}")
                                DiagRow("CONTENT_TYPE", trace.contentType)
                                DiagRow("BYTES", "${trace.responseBytes}")
                                DiagRow("BODY_PREFIX_300", trace.bodyPrefix300)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PIPELINE TRACING CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("6. PIPELINE VERIFICATION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        DiagRow("RAW_IMAGE_URL", live.rawImageUrl)
                        DiagRow("NORMALIZED_POSTER_URL", live.normalizedPosterUrl)
                        DiagRow("DATABASE_POSTER_URL", live.databasePosterUrl)
                        DiagRow("UI_POSTER_URL", live.uiPosterUrl)
                        DiagRow("RAW_CHAPTER_COUNT", "${live.rawChapterCount}")
                        DiagRow("NORMALIZED_EPISODE_COUNT", "${live.normalizedEpisodeCount}")
                        DiagRow("DATABASE_EPISODE_COUNT", "${live.databaseEpisodeCount}")
                        DiagRow("UI_EPISODE_COUNT", "${live.uiEpisodeCount}")
                        DiagRow("FIRST_REAL_TITLE", live.firstRealTitle)
                        DiagRow("FIRST_REAL_LINK", live.firstRealLink)
                        DiagRow("FIRST_REAL_IMAGE", live.firstRealImage)
                        DiagRow("REAL_EPISODE_COUNT", "${live.realEpisodeCount}")
                        DiagRow("FIRST_REAL_VIDEO", live.firstRealVideo)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(160.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DiagTestRow(label: String, result: String) {
    val isOk = result == "PASS" || result.contains("OK")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(180.dp)
        )
        Text(
            text = result,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isOk) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
    }
}
