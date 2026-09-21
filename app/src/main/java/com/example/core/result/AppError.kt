package com.example.core.result

/**
 * Standardized application error hierarchy.
 * All errors presented to the UI or logged must conform to this type.
 * Never leaks raw stack traces or internal secrets to user UI.
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class NetworkError(
        override val message: String = "Network connection failed. Please check your internet connection.",
        val statusCode: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class TimeoutError(
        override val message: String = "The request timed out. Please try again.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class ExtensionError(
        val extensionId: String,
        override val message: String = "Extension '$extensionId' encountered an issue.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class SourceNotFoundError(
        val episodeId: String? = null,
        override val message: String = "No playable video source was found for this episode.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class InvalidSourceError(
        val sourceUrl: String? = null,
        override val message: String = "The video source could not be played or is unsupported.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class PlayerError(
        val errorCode: Int? = null,
        override val message: String = "Playback error occurred.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class AuthenticationError(
        val service: String = "Account",
        override val message: String = "Authentication failed for $service.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class DatabaseError(
        override val message: String = "Failed to access local database.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class RepositoryError(
        val repoUrl: String? = null,
        override val message: String = "Failed to load extension repository.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class ProviderError(
        val provider: String,
        override val message: String = "Provider '$provider' encountered an error.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class NotFoundError(
        override val message: String = "Requested item was not found.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class StorageError(
        override val message: String = "Storage operation failed.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class ValidationError(
        override val message: String = "Validation failed.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class SecurityViolation(
        override val message: String = "Security policy violation.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class GeneralError(
        override val message: String = "An error occurred.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class UnknownError(
        override val message: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
