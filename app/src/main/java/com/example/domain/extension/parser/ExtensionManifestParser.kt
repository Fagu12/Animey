package com.example.domain.extension.parser

import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionType
import com.example.domain.model.RepositoryIndex
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Interface for safely parsing and serializing Extension Manifests and Repository Indexes.
 */
interface ExtensionManifestParser {
    fun parseManifest(json: String): AppResult<ExtensionManifest>
    fun parseRepositoryIndex(json: String): AppResult<RepositoryIndex>
    fun serializeManifest(manifest: ExtensionManifest): String
    fun serializeRepositoryIndex(index: RepositoryIndex): String
}

/**
 * Pure Kotlin & Moshi-based manifest parser with validation and flexible property fallbacks.
 * Runs seamlessly on JVM Unit Tests, Robolectric, and Android Runtime.
 */
class DefaultExtensionManifestParser(
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
) : ExtensionManifestParser {

    private val mapAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    override fun parseManifest(json: String): AppResult<ExtensionManifest> {
        return try {
            val map = mapAdapter.fromJson(json)
                ?: return AppResult.Error(AppError.GeneralError("Failed to parse JSON map"))
            val manifest = parseMapToManifest(map)
            AppResult.Success(manifest)
        } catch (e: Exception) {
            AppResult.Error(AppError.GeneralError("Failed to parse extension manifest JSON: ${e.message}", e))
        }
    }

    override fun parseRepositoryIndex(json: String): AppResult<RepositoryIndex> {
        return try {
            val root = mapAdapter.fromJson(json)
                ?: return AppResult.Error(AppError.GeneralError("Failed to parse repository JSON map"))

            val repoName = (root["name"] as? String)?.ifBlank { "Community Repository" } ?: "Community Repository"
            val repoVersion = (root["version"] as? Number)?.toInt() ?: 1

            @Suppress("UNCHECKED_CAST")
            val rawList = root["extensions"] as? List<Map<String, Any?>> ?: emptyList()
            val manifests = mutableListOf<ExtensionManifest>()

            for (itemMap in rawList) {
                try {
                    val manifest = parseMapToManifest(itemMap)
                    manifests.add(manifest)
                } catch (e: Exception) {
                    // Skip invalid manifest item rather than failing whole repository index
                }
            }

            AppResult.Success(
                RepositoryIndex(
                    name = repoName,
                    version = repoVersion,
                    extensions = manifests
                )
            )
        } catch (e: Exception) {
            AppResult.Error(AppError.GeneralError("Failed to parse repository index JSON: ${e.message}", e))
        }
    }

    override fun serializeManifest(manifest: ExtensionManifest): String {
        val map = mutableMapOf<String, Any?>(
            "id" to manifest.id,
            "name" to manifest.name,
            "version" to manifest.version,
            "language" to manifest.language,
            "type" to manifest.type.name.lowercase(),
            "iconUrl" to manifest.iconUrl,
            "description" to manifest.description,
            "downloadUrl" to manifest.downloadUrl,
            "minAppVersion" to manifest.minAppVersion,
            "author" to manifest.author,
            "isInstalled" to manifest.isInstalled,
            "isEnabled" to manifest.isEnabled,
            "hasUpdate" to manifest.hasUpdate,
            "order" to manifest.order,
            "isBuiltIn" to manifest.isBuiltIn,
            "apiVersion" to manifest.apiVersion,
            "minApiVersion" to manifest.minApiVersion,
            "targetApiVersion" to manifest.targetApiVersion,
            "capabilities" to manifest.capabilities,
            "allowedDomains" to manifest.allowedDomains,
            "nsfw" to manifest.nsfw
        )
        if (manifest.checksum != null) map["checksum"] = manifest.checksum
        if (manifest.sourceHash.isNotBlank()) map["sourceHash"] = manifest.sourceHash
        return mapAdapter.indent("  ").toJson(map)
    }

    override fun serializeRepositoryIndex(index: RepositoryIndex): String {
        val extMaps = index.extensions.map { ext ->
            val map = mutableMapOf<String, Any?>(
                "id" to ext.id,
                "name" to ext.name,
                "version" to ext.version,
                "language" to ext.language,
                "type" to ext.type.name.lowercase(),
                "iconUrl" to ext.iconUrl,
                "description" to ext.description,
                "downloadUrl" to ext.downloadUrl,
                "minAppVersion" to ext.minAppVersion,
                "author" to ext.author,
                "isInstalled" to ext.isInstalled,
                "isEnabled" to ext.isEnabled,
                "hasUpdate" to ext.hasUpdate,
                "order" to ext.order,
                "isBuiltIn" to ext.isBuiltIn,
                "apiVersion" to ext.apiVersion,
                "minApiVersion" to ext.minApiVersion,
                "targetApiVersion" to ext.targetApiVersion,
                "capabilities" to ext.capabilities,
                "allowedDomains" to ext.allowedDomains,
                "nsfw" to ext.nsfw
            )
            if (ext.checksum != null) map["checksum"] = ext.checksum
            if (ext.sourceHash.isNotBlank()) map["sourceHash"] = ext.sourceHash
            map
        }
        val rootMap = mapOf(
            "name" to index.name,
            "version" to index.version,
            "extensions" to extMaps
        )
        return mapAdapter.indent("  ").toJson(rootMap)
    }

    private fun parseMapToManifest(map: Map<String, Any?>): ExtensionManifest {
        val id = (map["id"] as? String)?.trim()
            ?: throw IllegalArgumentException("Missing required 'id' field in manifest")
        val name = (map["name"] as? String)?.trim()
            ?: throw IllegalArgumentException("Missing required 'name' field in manifest")
        val version = (map["version"] as? String)?.trim() ?: "1.0.0"

        val language = when {
            map.containsKey("language") && map["language"] is String -> map["language"] as String
            map.containsKey("lang") && map["lang"] is String -> map["lang"] as String
            else -> "all"
        }.trim()

        val typeStr = (map["type"] as? String)?.lowercase() ?: "anime"
        val extensionType = when (typeStr) {
            "manga" -> ExtensionType.MANGA
            "novel" -> ExtensionType.NOVEL
            else -> ExtensionType.ANIME
        }

        val apiVersion = (map["apiVersion"] as? Number)?.toInt() ?: ExtensionManifest.CURRENT_HOST_EXTENSION_API_VERSION
        val minApiVersion = (map["minApiVersion"] as? Number)?.toInt() ?: 1
        val targetApiVersion = (map["targetApiVersion"] as? Number)?.toInt() ?: apiVersion

        @Suppress("UNCHECKED_CAST")
        val capabilities = (map["capabilities"] as? List<*>)?.mapNotNull { it?.toString() }
            ?: listOf("NETWORK", "PREFERENCES")

        @Suppress("UNCHECKED_CAST")
        val allowedDomains = (map["allowedDomains"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val checksum = (map["checksum"] as? String)?.trim()
        val nsfw = (map["nsfw"] as? Boolean) ?: false
        val sourceHash = (map["sourceHash"] as? String)?.trim().orEmpty()

        return ExtensionManifest(
            id = id,
            name = name,
            version = version,
            language = language,
            type = extensionType,
            iconUrl = (map["iconUrl"] as? String)?.trim().orEmpty(),
            description = (map["description"] as? String)?.trim().orEmpty(),
            downloadUrl = (map["downloadUrl"] as? String)?.trim().orEmpty(),
            minAppVersion = (map["minAppVersion"] as? String)?.trim() ?: "1.0.0",
            author = (map["author"] as? String)?.trim() ?: "Community",
            isInstalled = (map["isInstalled"] as? Boolean) ?: false,
            isEnabled = (map["isEnabled"] as? Boolean) ?: true,
            hasUpdate = (map["hasUpdate"] as? Boolean) ?: false,
            order = (map["order"] as? Number)?.toInt() ?: 0,
            isBuiltIn = (map["isBuiltIn"] as? Boolean) ?: false,
            apiVersion = apiVersion,
            minApiVersion = minApiVersion,
            targetApiVersion = targetApiVersion,
            capabilities = capabilities,
            allowedDomains = allowedDomains,
            checksum = checksum,
            nsfw = nsfw,
            sourceHash = sourceHash
        )
    }
}
