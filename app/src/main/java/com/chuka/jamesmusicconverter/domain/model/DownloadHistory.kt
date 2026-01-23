package com.chuka.jamesmusicconverter.domain.model

import com.chuka.jamesmusicconverter.navigation.DownloadMode

/**
 * Domain model for download history
 */
data class DownloadHistory(
    val id: Long,
    val title: String,
    val url: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val localThumbnailPath: String?,
    val remoteThumbnailUrl: String?,
    val platform: String,
    val downloadMode: DownloadMode,
    val duration: Long,
    val uploader: String?,
    val downloadedAt: Long,
    val fileExists: Boolean,
    // Carousel/Playlist tracking fields
    val isPartOfCarousel: Boolean = false,
    val carouselGroupId: String? = null,
    val carouselPosition: Int? = null,
    val carouselTotal: Int? = null
) {
    /**
     * Format file size to human readable string
     */
    fun getFormattedFileSize(): String {
        val kb = fileSize / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$fileSize bytes"
        }
    }

    /**
     * Format duration to human readable string
     */
    fun getFormattedDuration(): String {
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * Get time ago string
     */
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - downloadedAt

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> if (days == 1L) "1 day ago" else "$days days ago"
            hours > 0 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
            minutes > 0 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
            else -> "Just now"
        }
    }

    /**
     * Get thumbnail to display (local first, then remote)
     */
    fun getThumbnailToDisplay(): String? {
        return localThumbnailPath ?: remoteThumbnailUrl
    }

    /**
     * Get formatted carousel position (e.g., "2/5")
     */
    fun getCarouselPositionText(): String? {
        return if (isPartOfCarousel && carouselPosition != null && carouselTotal != null) {
            "$carouselPosition/$carouselTotal"
        } else null
    }
}
