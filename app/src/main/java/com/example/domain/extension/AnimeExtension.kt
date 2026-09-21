package com.example.domain.extension

import com.example.core.result.AppResult
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionPreference
import com.example.domain.model.VideoSource

import com.example.domain.source.AnimeSource

/**
 * Base Source representation for an extension.
 */
interface ExtensionSource {
    val id: String
    val name: String
    val lang: String
    val iconUrl: String
    val manifest: ExtensionManifest
}

/**
 * Primary Anime Extension interface.
 * All extensions implementing anime scrapers / API bridges implement this contract.
 */
interface AnimeExtension : ExtensionSource, AnimeSource {
    override suspend fun getPreferences(): List<ExtensionPreference> = emptyList()

    override suspend fun setPreference(key: String, value: Any) {}
}
