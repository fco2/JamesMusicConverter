package com.chuka.jamesmusicconverter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing download history
 */
@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val url: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val localThumbnailPath: String?,  // Path to locally saved thumbnail
    val remoteThumbnailUrl: String?,  // Original thumbnail URL
    val platform: String,  // YouTube, TikTok, etc.
    val downloadMode: String,  // AUDIO or VIDEO
    val duration: Long,  // Duration in seconds
    val uploader: String?,
    val downloadedAt: Long,  // Timestamp
    val fileExists: Boolean = true,  // Track if file still exists
    // Carousel/Playlist tracking fields
    val isPartOfCarousel: Boolean = false,
    val carouselGroupId: String? = null,
    val carouselPosition: Int? = null,
    val carouselTotal: Int? = null
)
