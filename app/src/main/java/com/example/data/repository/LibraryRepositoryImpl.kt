package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.result.AppError
import com.example.core.result.AppResult
import com.example.data.local.database.dao.LibraryDao
import com.example.data.local.database.entity.LibraryEntity
import com.example.domain.model.Anime
import com.example.domain.model.LibraryEntry
import com.example.domain.model.LibraryStatus
import com.example.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryRepositoryImpl(
    private val libraryDao: LibraryDao
) : LibraryRepository {

    override fun getAllLibraryEntries(): Flow<List<LibraryEntry>> {
        return libraryDao.getAllLibraryEntries().map { list ->
            list.mapNotNull { item ->
                val anime = item.anime?.toDomain() ?: Anime(localId = item.library.animeId, title = "Unknown")
                LibraryEntry(
                    anime = anime,
                    status = item.library.status,
                    customCategory = item.library.customCategory,
                    score = item.library.score,
                    totalEpisodesWatched = item.library.totalEpisodesWatched,
                    addedAt = item.library.addedAt,
                    updatedAt = item.library.updatedAt
                )
            }
        }
    }

    override fun getLibraryEntriesByStatus(status: LibraryStatus): Flow<List<LibraryEntry>> {
        return libraryDao.getLibraryEntriesByStatus(status).map { list ->
            list.mapNotNull { item ->
                val anime = item.anime?.toDomain() ?: Anime(localId = item.library.animeId, title = "Unknown")
                LibraryEntry(
                    anime = anime,
                    status = item.library.status,
                    customCategory = item.library.customCategory,
                    score = item.library.score,
                    totalEpisodesWatched = item.library.totalEpisodesWatched,
                    addedAt = item.library.addedAt,
                    updatedAt = item.library.updatedAt
                )
            }
        }
    }

    override fun getLibraryEntryForAnime(animeId: String): Flow<LibraryEntry?> {
        return libraryDao.getLibraryEntryForAnime(animeId).map { item ->
            item?.let {
                val anime = it.anime?.toDomain() ?: Anime(localId = it.library.animeId, title = "Unknown")
                LibraryEntry(
                    anime = anime,
                    status = it.library.status,
                    customCategory = it.library.customCategory,
                    score = it.library.score,
                    totalEpisodesWatched = it.library.totalEpisodesWatched,
                    addedAt = it.library.addedAt,
                    updatedAt = it.library.updatedAt
                )
            }
        }
    }

    override fun isAnimeInLibrary(animeId: String): Flow<Boolean> {
        return libraryDao.isAnimeInLibrary(animeId)
    }

    override suspend fun addToLibrary(
        animeId: String,
        status: LibraryStatus,
        customCategory: String,
        score: Double?
    ): AppResult<Unit> {
        return try {
            val entity = LibraryEntity(
                animeId = animeId,
                status = status,
                customCategory = customCategory,
                score = score,
                addedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            libraryDao.upsertLibraryEntry(entity)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("LibraryRepo", "Failed to add anime $animeId to library", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to add to library", e))
        }
    }

    override suspend fun updateStatus(animeId: String, status: LibraryStatus): AppResult<Unit> {
        return try {
            libraryDao.updateStatus(animeId, status)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("LibraryRepo", "Failed to update library status for $animeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update status", e))
        }
    }

    override suspend fun updateProgress(animeId: String, watchedCount: Int): AppResult<Unit> {
        return try {
            libraryDao.updateProgress(animeId, watchedCount)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("LibraryRepo", "Failed to update watched count for $animeId", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update progress", e))
        }
    }

    override suspend fun removeFromLibrary(animeId: String): AppResult<Unit> {
        return try {
            libraryDao.deleteLibraryEntry(animeId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("LibraryRepo", "Failed to remove anime $animeId from library", e)
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to remove from library", e))
        }
    }
}
