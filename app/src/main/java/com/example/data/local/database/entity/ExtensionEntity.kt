package com.example.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionType

@Entity(tableName = "extensions")
data class ExtensionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val version: String,
    val language: String,
    val type: ExtensionType = ExtensionType.ANIME,
    val iconUrl: String = "",
    val description: String = "",
    val downloadUrl: String = "",
    val minAppVersion: String = "1.0.0",
    val author: String = "Community",
    val isInstalled: Boolean = true,
    val isEnabled: Boolean = true,
    val hasUpdate: Boolean = false,
    val sortOrder: Int = 0,
    val installedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ExtensionManifest = ExtensionManifest(
        id = id,
        name = name,
        version = version,
        language = language,
        type = type,
        iconUrl = iconUrl,
        description = description,
        downloadUrl = downloadUrl,
        minAppVersion = minAppVersion,
        author = author,
        isInstalled = isInstalled,
        isEnabled = isEnabled,
        hasUpdate = hasUpdate,
        order = sortOrder
    )

    companion object {
        fun fromDomain(domain: ExtensionManifest): ExtensionEntity = ExtensionEntity(
            id = domain.id,
            name = domain.name,
            version = domain.version,
            language = domain.language,
            type = domain.type,
            iconUrl = domain.iconUrl,
            description = domain.description,
            downloadUrl = domain.downloadUrl,
            minAppVersion = domain.minAppVersion,
            author = domain.author,
            isInstalled = domain.isInstalled,
            isEnabled = domain.isEnabled,
            hasUpdate = domain.hasUpdate,
            sortOrder = domain.order
        )
    }
}
