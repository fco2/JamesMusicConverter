package com.chuka.jamesmusicconverter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing download queue items
 */
@Entity(tableName = "download_queue")
data class DownloadQueueEntity(
    @PrimaryKey
    val id: String,  // UUID string

    val url: String,
    val title: String?,
    val thumbnailUrl: String?,
    val localThumbnailPath: String?,
    val platform: String,
    val downloadMode: String,  // AUDIO or VIDEO
    val status: String,  // PENDING, ACTIVE, COMPLETED, FAILED, CANCELLED
    val progress: Float,
    val statusMessage: String,
    val errorMessage: String?,
    val filePath: String?,
    val fileSize: Long,
    val durationMillis: Long = 0L,  // Duration in milliseconds
    val addedAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val retryCount: Int,
    // Carousel/Playlist tracking fields
    val isPartOfCarousel: Boolean = false,
    val carouselGroupId: String? = null,
    val carouselPosition: Int? = null,
    val carouselTotal: Int? = null
)
