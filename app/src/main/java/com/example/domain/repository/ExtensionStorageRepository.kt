package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionRepository
import kotlinx.coroutines.flow.Flow

interface ExtensionStorageRepository {
    fun getInstalledExtensions(): Flow<List<ExtensionManifest>>
    fun getEnabledExtensions(): Flow<List<ExtensionManifest>>
    fun getExtensionsWithUpdates(): Flow<List<ExtensionManifest>>
    fun getExtensionById(id: String): Flow<ExtensionManifest?>
    suspend fun saveInstalledExtension(manifest: ExtensionManifest): AppResult<Unit>
    suspend fun setExtensionEnabled(id: String, isEnabled: Boolean): AppResult<Unit>
    suspend fun setHasUpdate(id: String, hasUpdate: Boolean): AppResult<Unit>
    suspend fun setExtensionOrder(id: String, order: Int): AppResult<Unit>
    suspend fun reorderExtensions(orderedIds: List<String>): AppResult<Unit>
    suspend fun deleteExtension(id: String): AppResult<Unit>

    fun getAllRepositories(): Flow<List<ExtensionRepository>>
    fun getEnabledRepositories(): Flow<List<ExtensionRepository>>
    suspend fun saveRepository(repo: ExtensionRepository): AppResult<Unit>
    suspend fun setRepositoryEnabled(id: String, isEnabled: Boolean): AppResult<Unit>
    suspend fun updateRepositorySyncInfo(id: String, count: Int): AppResult<Unit>
    suspend fun deleteRepository(id: String): AppResult<Unit>
}
