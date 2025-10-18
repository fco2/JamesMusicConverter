package com.chuka.jamesmusicconverter.domain.model

/**
 * Represents a single downloaded video or audio file
 */
data class VideoItem(
    val title: String,
    val fileName: String,
    val fileSize: Long,
    val filePath: String,
    val thumbnailUrl: String? = null,
    val durationMillis: Long = 0
)

/**
 * Represents the result of a video-to-MP3 conversion or video download
 * Supports both single and multiple videos (playlists)
 */
data class ConversionResult(
    val videoTitle: String,           // Primary/playlist title
    val thumbnailUrl: String?,        // Primary thumbnail
    val fileName: String,             // Primary filename
    val fileSize: Long,               // Primary file size
    val filePath: String,             // Primary file path
    val durationMillis: Long = 0,     // Primary duration
    val isVideo: Boolean = false,     // true if video file (MP4), false if audio (MP3)
    val videos: List<VideoItem> = emptyList()  // For playlists: all downloaded videos
) {
    /**
     * Returns true if this result contains multiple videos (playlist)
     */
    fun isPlaylist(): Boolean = videos.size > 1

    /**
     * Returns the total size of all videos
     */
    fun getTotalSize(): Long = if (videos.isNotEmpty()) {
        videos.sumOf { it.fileSize }
    } else {
        fileSize
    }

    /**
     * Returns the number of videos
     */
    fun getVideoCount(): Int = if (videos.isNotEmpty()) videos.size else 1
}
