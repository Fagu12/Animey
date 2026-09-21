package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.database.dao.AnimeDao
import com.example.data.local.database.dao.DownloadDao
import com.example.data.local.database.dao.EpisodeDao
import com.example.data.local.database.dao.ExtensionDao
import com.example.data.local.database.dao.HistoryDao
import com.example.data.local.database.dao.LibraryDao
import com.example.data.local.database.dao.RepositoryDao
import com.example.data.local.database.dao.TrackingBindingDao
import com.example.data.local.database.entity.AnimeEntity
import com.example.data.local.database.entity.DownloadEntity
import com.example.data.local.database.entity.EpisodeEntity
import com.example.data.local.database.entity.ExtensionEntity
import com.example.data.local.database.entity.HistoryEntity
import com.example.data.local.database.entity.LibraryEntity
import com.example.data.local.database.entity.RepositoryEntity
import com.example.data.local.database.entity.TrackingBindingEntity

@Database(
    entities = [
        AnimeEntity::class,
        EpisodeEntity::class,
        HistoryEntity::class,
        LibraryEntity::class,
        DownloadEntity::class,
        ExtensionEntity::class,
        RepositoryEntity::class,
        TrackingBindingEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AnimeyDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun historyDao(): HistoryDao
    abstract fun libraryDao(): LibraryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun extensionDao(): ExtensionDao
    abstract fun repositoryDao(): RepositoryDao
    abstract fun trackingBindingDao(): TrackingBindingDao

    companion object {
        private const val DATABASE_NAME = "animey_database.db"

        @Volatile
        private var instance: AnimeyDatabase? = null

        fun getInstance(context: Context): AnimeyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnimeyDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration(true)
                .build()
                .also { instance = it }
            }
        }
    }
}
