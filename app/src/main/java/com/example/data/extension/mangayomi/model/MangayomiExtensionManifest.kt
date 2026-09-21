package com.example.data.extension.mangayomi.model

import com.example.domain.extension.runtime.ExtensionLifecycleState
import com.example.domain.source.ExtensionCompatibilityStatus

/**
 * Manifest metadata for a Mangayomi JavaScript Anime extension adhering to the official Mangayomi specification.
 */
data class MangayomiExtensionManifest(
    val id: String,
    val name: String,
    val lang: String = "en",
    val version: String = "1.0.0",
    val versionCode: Int = 1,
    val baseUrl: String,
    val iconUrl: String = "",
    val description: String = "",
    val author: String = "",
    val itemType: Int = 1, // 0 = Manga, 1 = Anime, 2 = Novel
    val sourceCodeUrl: String = "",
    val sourceCodeLanguage: Int = 0, // 0 = JavaScript
    val appMinVerReq: String = "0.1.0",
    val additionalParams: String = "",
    val notes: String = "",
    val hasCloudflare: Boolean = false,
    val scriptContent: String = "",
    val installedPath: String = "",
    val installedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true,
    val lifecycleState: ExtensionLifecycleState = ExtensionLifecycleState.INSTALLED,
    val compatibilityStatus: ExtensionCompatibilityStatus = ExtensionCompatibilityStatus.COMPATIBLE
)

