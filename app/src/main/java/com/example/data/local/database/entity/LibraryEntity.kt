package com.example.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.LibraryStatus

@Entity(
    tableName = "library",
    foreignKeys = [
        ForeignKey(
            entity = AnimeEntity::class,
            parentColumns = ["localId"],
            childColumns = ["animeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("animeId"),
        Index("status")
    ]
)
data class LibraryEntity(
    @PrimaryKey
    val animeId: String,
    val status: LibraryStatus = LibraryStatus.PLANNING,
    val customCategory: String = "",
    val score: Double? = null,
    val totalEpisodesWatched: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
