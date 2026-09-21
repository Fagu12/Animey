package com.example.domain.model

import java.util.Locale

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadInfo(
    val episodeId: String,
    val animeId: String,
    val sourceId: String,
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
    val progressPercent: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val isFinished: Boolean
        get() = status == DownloadStatus.COMPLETED || status == DownloadStatus.FAILED || status == DownloadStatus.CANCELLED

    val isDownloading: Boolean
        get() = status == DownloadStatus.DOWNLOADING

    val isPaused: Boolean
        get() = status == DownloadStatus.PAUSED

    val isCompleted: Boolean
        get() = status == DownloadStatus.COMPLETED
}

data class StorageUsage(
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val totalBytes: Long = 0L
) {
    val usedFormatted: String
        get() = formatBytes(usedBytes)
    val freeFormatted: String
        get() = formatBytes(freeBytes)
    val totalFormatted: String
        get() = formatBytes(totalBytes)
    val usagePercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }
}
