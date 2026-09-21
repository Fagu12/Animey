package com.example.core.cache

import android.content.Context
import coil.Coil
import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.local.database.dao.AnimeDao
import com.example.data.local.database.dao.EpisodeDao
import com.example.domain.model.StorageUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Breakdown of current application cache sizes.
 */
data class CacheBreakdown(
    val imageCacheBytes: Long = 0L,
    val metadataAnimeCount: Int = 0,
    val metadataEpisodeCount: Int = 0,
    val estimatedMetadataBytes: Long = 0L,
    val otherCacheBytes: Long = 0L,
    val totalCacheBytes: Long = 0L
) {
    val imageSizeFormatted: String get() = StorageUsage.formatBytes(imageCacheBytes)
    val metadataSizeFormatted: String get() = StorageUsage.formatBytes(estimatedMetadataBytes)
    val totalSizeFormatted: String get() = StorageUsage.formatBytes(totalCacheBytes)
    val metadataSummary: String get() = "$metadataAnimeCount anime, $metadataEpisodeCount episodes"
}

/**
 * Manages image caches, temporary network response caches, and non-essential Room metadata caches.
 * Preserves user library, watch history, and downloaded episodes at all times.
 */
class CacheManager(
    private val context: Context,
    private val animeDao: AnimeDao,
    private val episodeDao: EpisodeDao
) {
    companion object {
        private const val TAG = "CacheManager"
        private const val ESTIMATED_BYTES_PER_ANIME = 2048L
        private const val ESTIMATED_BYTES_PER_EPISODE = 512L
    }

    /**
     * Calculates the current size of all application caches.
     */
    suspend fun getCacheBreakdown(): CacheBreakdown = withContext(Dispatchers.IO) {
        try {
            // 1. Image cache size
            var imageBytes = 0L
            try {
                val coilLoader = Coil.imageLoader(context)
                imageBytes = coilLoader.diskCache?.size ?: 0L
            } catch (e: Exception) {
                AppLogger.w(TAG, "Could not query Coil disk cache size: ${e.message}")
            }

            val imageCacheDir = File(context.cacheDir, "image_cache")
            if (imageCacheDir.exists()) {
                val directoryBytes = getDirectorySize(imageCacheDir)
                if (directoryBytes > imageBytes) {
                    imageBytes = directoryBytes
                }
            }

            // 2. Non-essential metadata count
            val nonEssentialAnime = animeDao.getNonEssentialAnimeCount()
            val nonEssentialEpisodes = episodeDao.getNonEssentialEpisodeCount()
            val estimatedMetaBytes = (nonEssentialAnime * ESTIMATED_BYTES_PER_ANIME) +
                (nonEssentialEpisodes * ESTIMATED_BYTES_PER_EPISODE)

            // 3. Other temporary files in cache directory
            var otherBytes = 0L
            val cacheFiles = context.cacheDir.listFiles()
            if (cacheFiles != null) {
                for (file in cacheFiles) {
                    if (file.name != "image_cache") {
                        otherBytes += if (file.isDirectory) getDirectorySize(file) else file.length()
                    }
                }
            }

            val total = imageBytes + estimatedMetaBytes + otherBytes

            CacheBreakdown(
                imageCacheBytes = imageBytes,
                metadataAnimeCount = nonEssentialAnime,
                metadataEpisodeCount = nonEssentialEpisodes,
                estimatedMetadataBytes = estimatedMetaBytes,
                otherCacheBytes = otherBytes,
                totalCacheBytes = total
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to calculate cache size", e)
            CacheBreakdown()
        }
    }

    /**
     * Clears Coil memory and disk image caches.
     */
    suspend fun clearImages(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            AppLogger.d(TAG, "Clearing image caches...")
            try {
                val coilLoader = Coil.imageLoader(context)
                coilLoader.memoryCache?.clear()
                coilLoader.diskCache?.clear()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error clearing Coil caches: ${e.message}")
            }

            val imageCacheDir = File(context.cacheDir, "image_cache")
            if (imageCacheDir.exists()) {
                deleteRecursively(imageCacheDir)
            }

            AppLogger.d(TAG, "Image caches successfully cleared")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to clear images", e)
            AppResult.Error(AppError.StorageError("Failed to clear images: ${e.message}", e))
        }
    }

    /**
     * Clears non-essential anime and episode metadata records.
     * Preserves library items, watch history, and downloaded episodes.
     */
    suspend fun clearMetadata(): AppResult<Int> = withContext(Dispatchers.IO) {
        try {
            AppLogger.d(TAG, "Clearing non-essential metadata...")
            val deletedEpisodes = episodeDao.deleteNonEssentialEpisodes()
            val deletedAnime = animeDao.deleteNonEssentialAnime()
            val totalDeleted = deletedAnime + deletedEpisodes

            AppLogger.d(TAG, "Cleared $deletedAnime anime and $deletedEpisodes episode metadata records")
            AppResult.Success(totalDeleted)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to clear metadata", e)
            AppResult.Error(AppError.DatabaseError("Failed to clear metadata: ${e.message}", e))
        }
    }

    /**
     * Clears all temporary caches (images, other temp cache files, and non-essential metadata).
     */
    suspend fun clearAllCache(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            AppLogger.d(TAG, "Clearing all application caches...")
            // Clear images
            clearImages()

            // Clear metadata
            clearMetadata()

            // Clear temporary cache directory files (except downloads directory)
            val cacheFiles = context.cacheDir.listFiles()
            if (cacheFiles != null) {
                for (file in cacheFiles) {
                    deleteRecursively(file)
                }
            }

            AppLogger.d(TAG, "All application caches successfully cleared")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to clear all cache", e)
            AppResult.Error(AppError.StorageError("Failed to clear all cache: ${e.message}", e))
        }
    }

    private fun getDirectorySize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) {
                getDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    private fun deleteRecursively(fileOrDir: File) {
        if (fileOrDir.isDirectory) {
            val children = fileOrDir.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteRecursively(child)
                }
            }
        }
        fileOrDir.delete()
    }
}
