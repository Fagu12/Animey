package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.local.database.dao.ExtensionDao
import com.example.data.local.database.dao.RepositoryDao
import com.example.data.local.database.entity.ExtensionEntity
import com.example.data.local.database.entity.RepositoryEntity
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.ExtensionRepository
import com.example.domain.repository.ExtensionStorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExtensionStorageRepositoryImpl(
    private val extensionDao: ExtensionDao,
    private val repositoryDao: RepositoryDao
) : ExtensionStorageRepository {

    override fun getInstalledExtensions(): Flow<List<ExtensionManifest>> {
        return extensionDao.getAllInstalledExtensions().map { list -> list.map { it.toDomain() } }
    }

    override fun getEnabledExtensions(): Flow<List<ExtensionManifest>> {
        return extensionDao.getEnabledExtensions().map { list -> list.map { it.toDomain() } }
    }

    override fun getExtensionsWithUpdates(): Flow<List<ExtensionManifest>> {
        return extensionDao.getExtensionsWithUpdates().map { list -> list.map { it.toDomain() } }
    }

    override fun getExtensionById(id: String): Flow<ExtensionManifest?> {
        return extensionDao.getExtensionById(id).map { it?.toDomain() }
    }

    override suspend fun saveInstalledExtension(manifest: ExtensionManifest): AppResult<Unit> {
        return try {
            extensionDao.insertExtension(ExtensionEntity.fromDomain(manifest))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionStorage", "Failed to save extension ${manifest.id}", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to save extension", e))
        }
    }

    override suspend fun setExtensionEnabled(id: String, isEnabled: Boolean): AppResult<Unit> {
        return try {
            extensionDao.setExtensionEnabled(id, isEnabled)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionStorage", "Failed to set extension enabled $id", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update extension", e))
        }
    }

    override suspend fun setHasUpdate(id: String, hasUpdate: Boolean): AppResult<Unit> {
        return try {
            extensionDao.setHasUpdate(id, hasUpdate)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionStorage", "Failed to set extension update status $id", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update extension", e))
        }
    }

    override suspend fun setExtensionOrder(id: String, order: Int): AppResult<Unit> {
        return try {
            extensionDao.setExtensionOrder(id, order)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionStorage", "Failed to set extension order $id", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to set extension order", e))
        }
    }

    override suspend fun reorderExtensions(orderedIds: List<String>): AppResult<Unit> {
        return try {
            orderedIds.forEachIndexed { index, id ->
                extensionDao.setExtensionOrder(id, index)
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionStorage", "Failed to reorder extensions", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to reorder extensions", e))
        }
    }

    override suspend fun deleteExtension(id: String): AppResult<Unit> {
        return try {
            extensionDao.deleteExtension(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionStorage", "Failed to delete extension $id", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete extension", e))
        }
    }

    override fun getAllRepositories(): Flow<List<ExtensionRepository>> {
        return repositoryDao.getAllRepositories().map { list -> list.map { it.toDomain() } }
    }

    override fun getEnabledRepositories(): Flow<List<ExtensionRepository>> {
        return repositoryDao.getEnabledRepositories().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveRepository(repo: ExtensionRepository): AppResult<Unit> {
        return try {
            repositoryDao.insertRepository(RepositoryEntity.fromDomain(repo))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionStorage", "Failed to save repository ${repo.id}", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to save repository", e))
        }
    }

    override suspend fun setRepositoryEnabled(id: String, isEnabled: Boolean): AppResult<Unit> {
        return try {
            repositoryDao.setRepositoryEnabled(id, isEnabled)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionStorage", "Failed to toggle repository enabled $id", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to toggle repository", e))
        }
    }

    override suspend fun updateRepositorySyncInfo(id: String, count: Int): AppResult<Unit> {
        return try {
            repositoryDao.updateSyncInfo(id, count)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionStorage", "Failed to update sync info $id", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update sync info", e))
        }
    }

    override suspend fun deleteRepository(id: String): AppResult<Unit> {
        return try {
            repositoryDao.deleteRepository(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ExtensionStorage", "Failed to delete repository $id", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete repository", e))
        }
    }
}
