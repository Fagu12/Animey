package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.database.entity.ExtensionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM extensions ORDER BY sortOrder ASC, name ASC")
    fun getAllInstalledExtensions(): Flow<List<ExtensionEntity>>

    @Query("SELECT * FROM extensions WHERE isEnabled = 1 ORDER BY sortOrder ASC, name ASC")
    fun getEnabledExtensions(): Flow<List<ExtensionEntity>>

    @Query("SELECT * FROM extensions WHERE hasUpdate = 1 ORDER BY sortOrder ASC, name ASC")
    fun getExtensionsWithUpdates(): Flow<List<ExtensionEntity>>

    @Query("SELECT * FROM extensions WHERE id = :id LIMIT 1")
    fun getExtensionById(id: String): Flow<ExtensionEntity?>

    @Query("SELECT * FROM extensions WHERE id = :id LIMIT 1")
    suspend fun getExtensionByIdDirect(id: String): ExtensionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: ExtensionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtensions(extensions: List<ExtensionEntity>)

    @Update
    suspend fun updateExtension(extension: ExtensionEntity)

    @Query("UPDATE extensions SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setExtensionEnabled(id: String, isEnabled: Boolean)

    @Query("UPDATE extensions SET hasUpdate = :hasUpdate WHERE id = :id")
    suspend fun setHasUpdate(id: String, hasUpdate: Boolean)

    @Query("UPDATE extensions SET sortOrder = :order WHERE id = :id")
    suspend fun setExtensionOrder(id: String, order: Int)

    @Query("DELETE FROM extensions WHERE id = :id")
    suspend fun deleteExtension(id: String)
}
