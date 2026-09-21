package com.example.domain.model

enum class SourceType {
    HLS,
    MP4,
    DASH,
    FILE,
    EMBED,
    UNKNOWN;

    companion object {
        fun fromUrlOrMime(url: String, mimeType: String? = null): SourceType {
            if (mimeType != null) {
                val lowerMime = mimeType.lowercase()
                when {
                    lowerMime.contains("mpegurl") || lowerMime.contains("hls") -> return HLS
                    lowerMime.contains("mp4") || lowerMime.contains("video/mp4") -> return MP4
                    lowerMime.contains("dash") || lowerMime.contains("mpd") -> return DASH
                }
            }
            val lowerUrl = url.lowercase()
            return when {
                lowerUrl.startsWith("file://") || lowerUrl.startsWith("/storage/") || lowerUrl.startsWith("/data/") -> FILE
                lowerUrl.endsWith(".m3u8") || lowerUrl.contains(".m3u8?") -> HLS
                lowerUrl.endsWith(".mp4") || lowerUrl.contains(".mp4?") -> MP4
                lowerUrl.endsWith(".mpd") || lowerUrl.contains(".mpd?") -> DASH
                lowerUrl.contains("embed") || lowerUrl.contains("player") -> EMBED
                else -> UNKNOWN
            }
        }
    }
}

data class TrackInfo(
    val id: String,
    val label: String,
    val language: String,
    val url: String = "",
    val isDefault: Boolean = false
)

/**
 * Normalized Video Source.
 * The player checks sourceType before passing to ExoPlayer or WebView Embed Player.
 */
data class VideoSource(
    val id: String,
    val providerId: String,
    val serverName: String,
    val url: String,
    val type: SourceType = SourceType.HLS,
    val quality: String = "1080p",
    val headers: Map<String, String> = emptyMap(),
    val referer: String? = null,
    val subtitles: List<TrackInfo> = emptyList(),
    val audioTracks: List<TrackInfo> = emptyList(),
    val isDefault: Boolean = false
)
