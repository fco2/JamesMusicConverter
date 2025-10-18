package com.chuka.jamesmusicconverter.domain.model

/**
 * Represents the result of a video-to-MP3 conversion or video download
 */
data class ConversionResult(
    val videoTitle: String,
    val thumbnailUrl: String?,
    val fileName: String,
    val fileSize: Long,
    val filePath: String,
    val durationMillis: Long = 0, // Duration in milliseconds
    val isVideo: Boolean = false   // true if video file (MP4), false if audio (MP3)
)
