package com.example.domain.repository

import com.example.core.result.AppResult
import com.example.domain.model.LibraryEntry
import com.example.domain.model.LibraryStatus
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun getAllLibraryEntries(): Flow<List<LibraryEntry>>
    fun getLibraryEntriesByStatus(status: LibraryStatus): Flow<List<LibraryEntry>>
    fun getLibraryEntryForAnime(animeId: String): Flow<LibraryEntry?>
    fun isAnimeInLibrary(animeId: String): Flow<Boolean>
    suspend fun addToLibrary(animeId: String, status: LibraryStatus, customCategory: String = "", score: Double? = null): AppResult<Unit>
    suspend fun updateStatus(animeId: String, status: LibraryStatus): AppResult<Unit>
    suspend fun updateProgress(animeId: String, watchedCount: Int): AppResult<Unit>
    suspend fun removeFromLibrary(animeId: String): AppResult<Unit>
}
