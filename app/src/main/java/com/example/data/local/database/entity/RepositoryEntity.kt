package com.example.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ExtensionRepository

@Entity(tableName = "extension_repositories")
data class RepositoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val version: Int = 1,
    val isEnabled: Boolean = true,
    val extensionCount: Int = 0,
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ExtensionRepository = ExtensionRepository(
        id = id,
        name = name,
        url = url,
        version = version,
        isEnabled = isEnabled,
        extensionCount = extensionCount
    )

    companion object {
        fun fromDomain(domain: ExtensionRepository): RepositoryEntity = RepositoryEntity(
            id = domain.id,
            name = domain.name,
            url = domain.url,
            version = domain.version,
            isEnabled = domain.isEnabled,
            extensionCount = domain.extensionCount,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
}
