package com.example.domain.model

enum class SkipType {
    INTRO,
    OUTRO,
    RECAP
}

data class SkipSegment(
    val type: SkipType,
    val startTime: Long,
    val endTime: Long,
    val source: String = "independent"
) {
    val startMs: Long get() = startTime
    val endMs: Long get() = endTime

    // Secondary constructor for backward compatibility with startMs/endMs
    constructor(type: SkipType, startMs: Long, endMs: Long) : this(
        type = type,
        startTime = startMs,
        endTime = endMs,
        source = "independent"
    )
}
