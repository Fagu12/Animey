package com.example.core.logging

import android.util.Log

/**
 * Centralized, safe logger.
 * Automatically redacts bearer tokens, api keys, and passwords.
 */
object AppLogger {
    private const val DEFAULT_TAG = "Animey"

    private val SENSITIVE_PATTERNS = listOf(
        Regex("(?i)(bearer\\s+)[A-Za-z0-9-_.]+"),
        Regex("(?i)(token=)[A-Za-z0-9-_.]+"),
        Regex("(?i)(password=)[^&\\s]+"),
        Regex("(?i)(key=)[A-Za-z0-9-_.]+")
    )

    private fun sanitize(message: String): String {
        var sanitized = message
        for (pattern in SENSITIVE_PATTERNS) {
            sanitized = pattern.replace(sanitized, "$1[REDACTED]")
        }
        return sanitized
    }

    fun d(tag: String = DEFAULT_TAG, message: String) {
        val sanitized = sanitize(message)
        try {
            Log.d(tag, sanitized)
        } catch (_: Throwable) {
            println("D/$tag: $sanitized")
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        val sanitized = sanitize(message)
        try {
            Log.i(tag, sanitized)
        } catch (_: Throwable) {
            println("I/$tag: $sanitized")
        }
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        val sanitized = sanitize(message)
        try {
            Log.w(tag, sanitized, throwable)
        } catch (_: Throwable) {
            println("W/$tag: $sanitized ${throwable?.message ?: ""}")
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        val sanitized = sanitize(message)
        try {
            Log.e(tag, sanitized, throwable)
        } catch (_: Throwable) {
            println("E/$tag: $sanitized ${throwable?.message ?: ""}")
        }
    }

    /**
     * Extension-specific isolated audit logging
     */
    fun logExtensionOperation(
        extensionId: String,
        operation: String,
        durationMs: Long,
        success: Boolean,
        errorType: String? = null
    ) {
        val status = if (success) "SUCCESS" else "FAILURE"
        val errorSuffix = if (errorType != null) " | Error: $errorType" else ""
        i("ExtensionLogger", "[$extensionId] op=$operation | status=$status | duration=${durationMs}ms$errorSuffix")
    }
}
