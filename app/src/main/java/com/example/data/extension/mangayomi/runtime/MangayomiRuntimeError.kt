package com.example.data.extension.mangayomi.runtime

enum class MangayomiPhase {
    DOWNLOAD,
    PREAMBLE,
    SOURCE_PARSE,
    SOURCE_INIT,
    METHOD_CALL,
    PROMISE,
    RESULT_MAPPING
}

data class MangayomiRuntimeError(
    val phase: MangayomiPhase,
    val extensionId: String,
    val extensionName: String,
    val method: String? = null,
    override val message: String,
    val stack: String? = null,
    override val cause: Throwable? = null
) : Exception("[$phase][${extensionName.ifBlank { extensionId }}]${method?.let { " [$it]" } ?: ""} $message", cause)
