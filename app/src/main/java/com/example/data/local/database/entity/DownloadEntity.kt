package com.example.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.DownloadInfo
import com.example.domain.model.DownloadStatus

@Entity(
    tableName = "downloads",
    indices = [
        Index("animeId"),
        Index("episodeId"),
        Index("status")
    ]
)
data class DownloadEntity(
    @PrimaryKey
    val episodeId: String,
    val animeId: String,
    val sourceId: String = "",
    val quality: String = "1080p",
    val localFilePath: String = "",
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val downloadUrl: String = "",
    val animeTitle: String = "",
    val episodeTitle: String = "",
    val episodeNumber: Float = 1f,
    val thumbnail: String = ""
) {
    fun toDomain(): DownloadInfo = DownloadInfo(
        episodeId = episodeId,
        animeId = animeId,
        sourceId = sourceId,
        quality = quality,
        localFilePath = localFilePath,
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        status = status,
        error = error,
        createdAt = createdAt,
        downloadUrl = downloadUrl,
        animeTitle = animeTitle,
        episodeTitle = episodeTitle,
        episodeNumber = episodeNumber,
        thumbnail = thumbnail
    )

    companion object {
        fun fromDomain(domain: DownloadInfo): DownloadEntity = DownloadEntity(
            episodeId = domain.episodeId,
            animeId = domain.animeId,
            sourceId = domain.sourceId,
            quality = domain.quality,
            localFilePath = domain.localFilePath,
            totalBytes = domain.totalBytes,
            downloadedBytes = domain.downloadedBytes,
            status = domain.status,
            error = domain.error,
            createdAt = domain.createdAt,
            downloadUrl = domain.downloadUrl,
            animeTitle = domain.animeTitle,
            episodeTitle = domain.episodeTitle,
            episodeNumber = domain.episodeNumber,
            thumbnail = domain.thumbnail
        )
    }
}
