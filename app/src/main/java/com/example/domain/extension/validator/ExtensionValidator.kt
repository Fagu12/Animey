package com.example.domain.extension.validator

import com.example.domain.model.ExtensionManifest
import com.example.domain.util.VersionComparator

/**
 * Result of validating an extension manifest.
 */
sealed class ExtensionValidationResult {
    object Valid : ExtensionValidationResult()
    data class Warning(val warnings: List<String>) : ExtensionValidationResult()
    data class Incompatible(val reason: String) : ExtensionValidationResult()
    data class Invalid(val errors: List<String>) : ExtensionValidationResult()

    val isValid: Boolean get() = this is Valid || this is Warning
}

/**
 * Validates extension manifests for structural correctness, app compatibility,
 * and security constraints without executing untrusted code.
 */
interface ExtensionValidator {
    fun validate(
        manifest: ExtensionManifest,
        currentAppVersion: String = "1.0.0",
        currentApiVersion: Int = ExtensionManifest.CURRENT_HOST_EXTENSION_API_VERSION
    ): ExtensionValidationResult

    fun checkCompatibility(
        minAppVersion: String,
        currentAppVersion: String = "1.0.0"
    ): Boolean

    fun checkApiCompatibility(
        minApiVersion: Int,
        currentApiVersion: Int = ExtensionManifest.CURRENT_HOST_EXTENSION_API_VERSION
    ): Boolean
}

class DefaultExtensionValidator : ExtensionValidator {

    private val idRegex = Regex("^[a-zA-Z0-9_.-]{1,128}$")
    private val domainRegex = Regex("^[a-zA-Z0-9.-]+$")

    override fun validate(
        manifest: ExtensionManifest,
        currentAppVersion: String,
        currentApiVersion: Int
    ): ExtensionValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Structure validation
        if (manifest.id.isBlank()) {
            errors.add("Extension ID cannot be blank")
        } else if (!idRegex.matches(manifest.id)) {
            errors.add("Extension ID '${manifest.id}' must be 3-64 characters and contain only letters, numbers, dots, underscores, or hyphens")
        }

        if (manifest.name.isBlank()) {
            errors.add("Extension name cannot be blank")
        }

        if (manifest.version.isBlank()) {
            errors.add("Extension version cannot be blank")
        }

        // 2. Capabilities validation
        for (cap in manifest.capabilities) {
            val parsed = com.example.domain.model.ExtensionCapability.fromString(cap)
            if (parsed == null) {
                warnings.add("Unrecognized capability '$cap' requested by extension '${manifest.id}'")
            }
        }

        // 3. Allowed domains validation
        for (domain in manifest.allowedDomains) {
            if (domain.isBlank() || !domainRegex.matches(domain) || domain.contains("/")) {
                errors.add("Invalid allowed domain format '$domain'. Must be a clean hostname without protocol or path.")
            }
        }

        // 4. Checksum format validation (if provided)
        if (!manifest.checksum.isNullOrBlank()) {
            val isHex = manifest.checksum.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
            if (!isHex || (manifest.checksum.length != 64 && manifest.checksum.length != 32 && manifest.checksum.length != 40)) {
                warnings.add("Suspicious checksum format: expected 32, 40, or 64 hex characters")
            }
        }

        // 5. Security validation (Safe URLs)
        if (manifest.downloadUrl.isNotBlank()) {
            val url = manifest.downloadUrl.lowercase()
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                errors.add("Security violation: downloadUrl must use http or https protocol")
            }
            if (url.startsWith("file:") || url.startsWith("javascript:") || url.startsWith("content:")) {
                errors.add("Security violation: Forbidden protocol in downloadUrl")
            }
        }

        if (manifest.iconUrl.isNotBlank()) {
            val iconUrl = manifest.iconUrl.lowercase()
            if (iconUrl.startsWith("javascript:") || iconUrl.startsWith("file:")) {
                warnings.add("Warning: suspicious iconUrl protocol")
            }
        }

        if (errors.isNotEmpty()) {
            return ExtensionValidationResult.Invalid(errors)
        }

        // 6. API Version Compatibility check
        if (!checkApiCompatibility(manifest.minApiVersion, currentApiVersion)) {
            return ExtensionValidationResult.Incompatible(
                "Extension requires minimum API version ${manifest.minApiVersion}, but current host API version is $currentApiVersion"
            )
        }

        // 7. App Version Compatibility check
        val isCompatible = checkCompatibility(manifest.minAppVersion, currentAppVersion)
        if (!isCompatible) {
            return ExtensionValidationResult.Incompatible(
                "Extension requires minimum app version '${manifest.minAppVersion}', but current app version is '$currentAppVersion'"
            )
        }

        return if (warnings.isNotEmpty()) {
            ExtensionValidationResult.Warning(warnings)
        } else {
            ExtensionValidationResult.Valid
        }
    }

    override fun checkCompatibility(minAppVersion: String, currentAppVersion: String): Boolean {
        if (minAppVersion.isBlank()) return true
        return VersionComparator.compare(currentAppVersion, minAppVersion) >= 0
    }

    override fun checkApiCompatibility(minApiVersion: Int, currentApiVersion: Int): Boolean {
        return currentApiVersion >= minApiVersion
    }
}
