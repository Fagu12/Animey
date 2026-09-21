package com.example.data.extension.mangayomi.storage

import android.content.Context
import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Sandboxed local storage store for installed Mangayomi JavaScript extension files.
 */
interface InstalledExtensionStore {
    suspend fun saveScript(extensionId: String, scriptContent: String): AppResult<String>
    suspend fun readScript(extensionId: String): AppResult<String>
    suspend fun deleteScript(extensionId: String): AppResult<Unit>
    suspend fun hasScript(extensionId: String): Boolean
    suspend fun listInstalledExtensionIds(): List<String>
}

class DefaultInstalledExtensionStore(
    private val context: Context? = null,
    private val baseDirOverride: File? = null
) : InstalledExtensionStore {

    private val inMemoryFallback = ConcurrentHashMap<String, String>()

    private fun getStorageDir(): File? {
        val dir = baseDirOverride ?: context?.filesDir?.let { File(it, "extensions/mangayomi") }
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    override suspend fun saveScript(extensionId: String, scriptContent: String): AppResult<String> = withContext(Dispatchers.IO) {
        if (extensionId.isBlank() || scriptContent.isBlank()) {
            return@withContext AppResult.Error(AppError.ValidationError("Extension ID and script content must not be blank"))
        }

        try {
            val dir = getStorageDir()
            if (dir != null) {
                val scriptFile = File(dir, "${sanitizeFileName(extensionId)}.js")
                scriptFile.writeText(scriptContent, Charsets.UTF_8)
                inMemoryFallback[extensionId] = scriptContent
                AppLogger.d("InstalledExtensionStore", "Saved script for $extensionId to ${scriptFile.absolutePath}")
                AppResult.Success(scriptFile.absolutePath)
            } else {
                inMemoryFallback[extensionId] = scriptContent
                AppResult.Success("memory://$extensionId.js")
            }
        } catch (e: Exception) {
            AppLogger.e("InstalledExtensionStore", "Failed saving script for $extensionId", e)
            // Fallback to in-memory store
            inMemoryFallback[extensionId] = scriptContent
            AppResult.Success("memory://$extensionId.js")
        }
    }

    override suspend fun readScript(extensionId: String): AppResult<String> = withContext(Dispatchers.IO) {
        val inMem = inMemoryFallback[extensionId]
        if (inMem != null && inMem.isNotBlank()) {
            return@withContext AppResult.Success(inMem)
        }

        try {
            val dir = getStorageDir()
            if (dir != null) {
                val scriptFile = File(dir, "${sanitizeFileName(extensionId)}.js")
                if (scriptFile.exists()) {
                    val content = scriptFile.readText(Charsets.UTF_8)
                    inMemoryFallback[extensionId] = content
                    return@withContext AppResult.Success(content)
                }
            }
            AppResult.Error(AppError.NotFoundError("Script for extension $extensionId not found in local store"))
        } catch (e: Exception) {
            AppResult.Error(AppError.StorageError("Failed reading script for $extensionId", e))
        }
    }

    override suspend fun deleteScript(extensionId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        inMemoryFallback.remove(extensionId)
        try {
            val dir = getStorageDir()
            if (dir != null) {
                val scriptFile = File(dir, "${sanitizeFileName(extensionId)}.js")
                if (scriptFile.exists()) {
                    scriptFile.delete()
                }
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.StorageError("Failed deleting script for $extensionId", e))
        }
    }

    override suspend fun hasScript(extensionId: String): Boolean = withContext(Dispatchers.IO) {
        if (inMemoryFallback.containsKey(extensionId)) return@withContext true
        val dir = getStorageDir() ?: return@withContext false
        val scriptFile = File(dir, "${sanitizeFileName(extensionId)}.js")
        scriptFile.exists()
    }

    override suspend fun listInstalledExtensionIds(): List<String> = withContext(Dispatchers.IO) {
        val ids = mutableSetOf<String>()
        ids.addAll(inMemoryFallback.keys)
        val dir = getStorageDir()
        if (dir != null && dir.exists()) {
            dir.listFiles { file -> file.extension == "js" }?.forEach { file ->
                ids.add(file.nameWithoutExtension)
            }
        }
        ids.toList()
    }

    private fun sanitizeFileName(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
