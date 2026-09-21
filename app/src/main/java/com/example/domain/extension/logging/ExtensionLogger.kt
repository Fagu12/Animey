package com.example.domain.extension.logging

import com.example.core.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

enum class ExtensionLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

data class ExtensionLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val extensionId: String,
    val level: ExtensionLogLevel,
    val tag: String,
    val message: String,
    val error: String? = null
)

enum class CircuitBreakerState {
    CLOSED,    // Healthy, normal operation
    HALF_OPEN, // Testing recovery after cooldown
    OPEN       // Failing repeatedly, blocking requests
}

data class ExtensionRuntimeStats(
    val extensionId: String,
    val totalRequests: Long = 0L,
    val successfulRequests: Long = 0L,
    val failedRequests: Long = 0L,
    val totalLatencyMs: Long = 0L,
    val circuitState: CircuitBreakerState = CircuitBreakerState.CLOSED,
    val consecutiveFailures: Int = 0,
    val lastFailureMessage: String? = null,
    val lastActivityTimestamp: Long = System.currentTimeMillis()
) {
    val averageLatencyMs: Long
        get() = if (totalRequests > 0) totalLatencyMs / totalRequests else 0L

    val successRate: Float
        get() = if (totalRequests > 0) (successfulRequests.toFloat() / totalRequests.toFloat()) * 100f else 100f
}

/**
 * Isolated logger and diagnostics buffer for extensions.
 */
interface ExtensionLogger {
    val allLogs: StateFlow<Map<String, List<ExtensionLogEntry>>>

    fun log(
        extensionId: String,
        level: ExtensionLogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null
    )

    fun d(extensionId: String, tag: String, message: String) =
        log(extensionId, ExtensionLogLevel.DEBUG, tag, message)

    fun i(extensionId: String, tag: String, message: String) =
        log(extensionId, ExtensionLogLevel.INFO, tag, message)

    fun w(extensionId: String, tag: String, message: String, throwable: Throwable? = null) =
        log(extensionId, ExtensionLogLevel.WARN, tag, message, throwable)

    fun e(extensionId: String, tag: String, message: String, throwable: Throwable? = null) =
        log(extensionId, ExtensionLogLevel.ERROR, tag, message, throwable)

    fun getLogs(extensionId: String): List<ExtensionLogEntry>
    fun clearLogs(extensionId: String)
}

class DefaultExtensionLogger(
    private val maxLogsPerExtension: Int = 100
) : ExtensionLogger {

    private val logStore = ConcurrentHashMap<String, ArrayDeque<ExtensionLogEntry>>()
    private val _allLogs = MutableStateFlow<Map<String, List<ExtensionLogEntry>>>(emptyMap())
    override val allLogs: StateFlow<Map<String, List<ExtensionLogEntry>>> = _allLogs.asStateFlow()

    override fun log(
        extensionId: String,
        level: ExtensionLogLevel,
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        val entry = ExtensionLogEntry(
            extensionId = extensionId,
            level = level,
            tag = tag,
            message = message,
            error = throwable?.message
        )

        val queue = logStore.computeIfAbsent(extensionId) { ArrayDeque() }
        synchronized(queue) {
            if (queue.size >= maxLogsPerExtension) {
                queue.removeFirst()
            }
            queue.addLast(entry)
        }

        // Update state flow snapshot
        _allLogs.value = logStore.mapValues { synchronized(it.value) { it.value.toList() } }

        // Also forward to AppLogger
        val formattedMsg = "[$extensionId][$tag] $message"
        when (level) {
            ExtensionLogLevel.DEBUG -> AppLogger.d("Ext-$extensionId", formattedMsg)
            ExtensionLogLevel.INFO -> AppLogger.i("Ext-$extensionId", formattedMsg)
            ExtensionLogLevel.WARN -> AppLogger.w("Ext-$extensionId", formattedMsg, throwable)
            ExtensionLogLevel.ERROR -> AppLogger.e("Ext-$extensionId", formattedMsg, throwable)
        }
    }

    override fun getLogs(extensionId: String): List<ExtensionLogEntry> {
        val queue = logStore[extensionId] ?: return emptyList()
        return synchronized(queue) { queue.toList() }
    }

    override fun clearLogs(extensionId: String) {
        logStore[extensionId]?.let { queue ->
            synchronized(queue) { queue.clear() }
        }
        _allLogs.value = logStore.mapValues { synchronized(it.value) { it.value.toList() } }
    }
}
