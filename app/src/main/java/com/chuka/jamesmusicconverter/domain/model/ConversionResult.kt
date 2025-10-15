package com.chuka.jamesmusicconverter.domain.model

/**
 * Represents the result of a video-to-MP3 conversion
 */
data class ConversionResult(
    val videoTitle: String,
    val thumbnailUrl: String?,
    val fileName: String,
    val fileSize: Long,
    val filePath: String
)
