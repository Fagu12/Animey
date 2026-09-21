package com.example.domain.source

/**
 * Compatibility status reported by the extension engine.
 */
enum class ExtensionCompatibilityStatus {
    COMPATIBLE,
    COMPATIBLE_WITH_LIMITATIONS,
    UNSUPPORTED_API,
    MISSING_DEPENDENCY,
    INVALID_EXTENSION,
    RUNTIME_ERROR
}
