package com.example.data.local.database

import androidx.room.TypeConverter
import com.example.domain.model.DownloadStatus
import com.example.domain.model.ExtensionType
import com.example.domain.model.LibraryStatus
import com.example.domain.model.SkipSegment
import com.example.domain.model.SkipType
import com.example.domain.model.TrackingPlatform
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    private val skipSegmentListType = Types.newParameterizedType(List::class.java, SkipSegment::class.java)
    private val skipSegmentListAdapter = moshi.adapter<List<SkipSegment>>(skipSegmentListType)

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return list?.let { stringListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            stringListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromSkipSegmentList(list: List<SkipSegment>?): String {
        return list?.let { skipSegmentListAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toSkipSegmentList(json: String?): List<SkipSegment> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            skipSegmentListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromLibraryStatus(status: LibraryStatus?): String {
        return status?.name ?: LibraryStatus.PLANNING.name
    }

    @TypeConverter
    fun toLibraryStatus(name: String?): LibraryStatus {
        return try {
            name?.let { LibraryStatus.valueOf(it) } ?: LibraryStatus.PLANNING
        } catch (_: Exception) {
            LibraryStatus.PLANNING
        }
    }

    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus?): String {
        return status?.name ?: DownloadStatus.QUEUED.name
    }

    @TypeConverter
    fun toDownloadStatus(name: String?): DownloadStatus {
        return try {
            name?.let { DownloadStatus.valueOf(it) } ?: DownloadStatus.QUEUED
        } catch (_: Exception) {
            DownloadStatus.QUEUED
        }
    }

    @TypeConverter
    fun fromExtensionType(type: ExtensionType?): String {
        return type?.name ?: ExtensionType.ANIME.name
    }

    @TypeConverter
    fun toExtensionType(name: String?): ExtensionType {
        return try {
            name?.let { ExtensionType.valueOf(it) } ?: ExtensionType.ANIME
        } catch (_: Exception) {
            ExtensionType.ANIME
        }
    }

    @TypeConverter
    fun fromTrackingPlatform(platform: TrackingPlatform?): String {
        return platform?.name ?: TrackingPlatform.ANILIST.name
    }

    @TypeConverter
    fun toTrackingPlatform(name: String?): TrackingPlatform {
        return try {
            name?.let { TrackingPlatform.valueOf(it) } ?: TrackingPlatform.ANILIST
        } catch (_: Exception) {
            TrackingPlatform.ANILIST
        }
    }

    @TypeConverter
    fun fromSkipType(type: SkipType?): String {
        return type?.name ?: SkipType.INTRO.name
    }

    @TypeConverter
    fun toSkipType(name: String?): SkipType {
        return try {
            name?.let { SkipType.valueOf(it) } ?: SkipType.INTRO
        } catch (_: Exception) {
            SkipType.INTRO
        }
    }
}
