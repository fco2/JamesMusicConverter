package com.chuka.jamesmusicconverter.domain.model

import com.chuka.jamesmusicconverter.navigation.DownloadMode

/**
 * Status of a queue item
 */
enum class QueueItemStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Domain model for a download queue item
 */
data class QueueItem(
    val id: String,
    val url: String,
    val title: String?,
    val thumbnailUrl: String?,
    val localThumbnailPath: String?,
    val platform: String,
    val downloadMode: DownloadMode,
    val status: QueueItemStatus,
    val progress: Float,
    val statusMessage: String,
    val errorMessage: String?,
    val filePath: String?,
    val fileSize: Long,
    val durationMillis: Long = 0L,
    val addedAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val retryCount: Int,
    // Carousel/Playlist tracking fields
    val isPartOfCarousel: Boolean = false,
    val carouselGroupId: String? = null,
    val carouselPosition: Int? = null,
    val carouselTotal: Int? = null
) {
    /**
     * Check if this item is in a terminal state (completed, failed, or cancelled)
     */
    val isFinished: Boolean
        get() = status == QueueItemStatus.COMPLETED ||
                status == QueueItemStatus.FAILED ||
                status == QueueItemStatus.CANCELLED

    /**
     * Check if this item can be retried
     */
    val canRetry: Boolean
        get() = status == QueueItemStatus.FAILED || status == QueueItemStatus.CANCELLED

    /**
     * Check if this item can be cancelled
     */
    val canCancel: Boolean
        get() = status == QueueItemStatus.PENDING || status == QueueItemStatus.ACTIVE

    /**
     * Get a display-friendly title
     */
    val displayTitle: String
        get() = title ?: "Untitled Video"

    /**
     * Get formatted progress percentage
     */
    val progressPercentage: Int
        get() = (progress * 100).toInt()

    /**
     * Get human-friendly file name (without extension and path)
     */
    val friendlyFileName: String
        get() = title ?: filePath?.let { java.io.File(it).nameWithoutExtension } ?: "Unknown"

    /**
     * Get formatted carousel position (e.g., "2/5")
     */
    val carouselPositionText: String?
        get() = if (isPartOfCarousel && carouselPosition != null && carouselTotal != null) {
            "$carouselPosition/$carouselTotal"
        } else null
}
