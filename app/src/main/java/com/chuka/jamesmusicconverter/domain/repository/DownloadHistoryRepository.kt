package com.chuka.jamesmusicconverter.domain.repository

import com.chuka.jamesmusicconverter.data.local.DownloadHistoryDao
import com.chuka.jamesmusicconverter.data.local.DownloadHistoryEntity
import com.chuka.jamesmusicconverter.domain.model.DownloadHistory
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing download history
 */
@Singleton
class DownloadHistoryRepository @Inject constructor(
    private val downloadHistoryDao: DownloadHistoryDao
) {

    /**
     * Add a download to history
     */
    suspend fun addDownload(
        title: String,
        url: String,
        fileName: String,
        filePath: String,
        fileSize: Long,
        localThumbnailPath: String?,
        remoteThumbnailUrl: String?,
        platform: String,
        downloadMode: DownloadMode,
        duration: Long,
        uploader: String?,
        isPartOfCarousel: Boolean = false,
        carouselGroupId: String? = null,
        carouselPosition: Int? = null,
        carouselTotal: Int? = null
    ): Long {
        val entity = DownloadHistoryEntity(
            title = title,
            url = url,
            fileName = fileName,
            filePath = filePath,
            fileSize = fileSize,
            localThumbnailPath = localThumbnailPath,
            remoteThumbnailUrl = remoteThumbnailUrl,
            platform = platform,
            downloadMode = downloadMode.name,
            duration = duration,
            uploader = uploader,
            downloadedAt = System.currentTimeMillis(),
            fileExists = true,
            isPartOfCarousel = isPartOfCarousel,
            carouselGroupId = carouselGroupId,
            carouselPosition = carouselPosition,
            carouselTotal = carouselTotal
        )
        return downloadHistoryDao.insertDownload(entity)
    }

    /**
     * Get all downloads
     */
    fun getAllDownloads(): Flow<List<DownloadHistory>> {
        return downloadHistoryDao.getAllDownloads().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get recent downloads
     */
    fun getRecentDownloads(limit: Int = 10): Flow<List<DownloadHistory>> {
        return downloadHistoryDao.getRecentDownloads(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Search downloads
     */
    fun searchDownloads(query: String): Flow<List<DownloadHistory>> {
        return downloadHistoryDao.searchDownloads(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get downloads by platform
     */
    fun getDownloadsByPlatform(platform: String): Flow<List<DownloadHistory>> {
        return downloadHistoryDao.getDownloadsByPlatform(platform).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get downloads by mode
     */
    fun getDownloadsByMode(mode: DownloadMode): Flow<List<DownloadHistory>> {
        return downloadHistoryDao.getDownloadsByMode(mode.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Delete a download
     */
    suspend fun deleteDownload(id: Long) {
        downloadHistoryDao.deleteDownloadById(id)
    }

    /**
     * Clear all history
     */
    suspend fun clearAllHistory() {
        downloadHistoryDao.clearAllHistory()
    }

    /**
     * Update file exists status
     */
    suspend fun updateFileExists(id: Long, exists: Boolean) {
        downloadHistoryDao.updateFileExists(id, exists)
    }

    /**
     * Get total download count
     */
    suspend fun getTotalDownloadCount(): Int {
        return downloadHistoryDao.getTotalDownloadCount()
    }

    /**
     * Get total file size
     */
    suspend fun getTotalFileSize(): Long {
        return downloadHistoryDao.getTotalFileSize() ?: 0L
    }

    /**
     * Convert entity to domain model
     */
    private fun DownloadHistoryEntity.toDomain(): DownloadHistory {
        return DownloadHistory(
            id = id,
            title = title,
            url = url,
            fileName = fileName,
            filePath = filePath,
            fileSize = fileSize,
            localThumbnailPath = localThumbnailPath,
            remoteThumbnailUrl = remoteThumbnailUrl,
            platform = platform,
            downloadMode = DownloadMode.valueOf(downloadMode),
            duration = duration,
            uploader = uploader,
            downloadedAt = downloadedAt,
            fileExists = fileExists,
            isPartOfCarousel = isPartOfCarousel,
            carouselGroupId = carouselGroupId,
            carouselPosition = carouselPosition,
            carouselTotal = carouselTotal
        )
    }
}
