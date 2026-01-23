package com.chuka.jamesmusicconverter.domain.repository

import com.chuka.jamesmusicconverter.data.local.DownloadQueueDao
import com.chuka.jamesmusicconverter.data.local.DownloadQueueEntity
import com.chuka.jamesmusicconverter.domain.model.QueueItem
import com.chuka.jamesmusicconverter.domain.model.QueueItemStatus
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing download queue operations
 */
@Singleton
class DownloadQueueRepository @Inject constructor(
    private val dao: DownloadQueueDao
) {

    /**
     * Get all queue items
     */
    fun getAllQueueItems(): Flow<List<QueueItem>> {
        return dao.getAllQueueItems().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Get active queue (pending and active items)
     */
    fun getActiveQueue(): Flow<List<QueueItem>> {
        return dao.getActiveQueue().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Get queue item by ID
     */
    suspend fun getQueueItemById(id: String): QueueItem? {
        return dao.getQueueItemById(id)?.toDomainModel()
    }

    /**
     * Get queue item by ID as Flow
     */
    fun getQueueItemByIdFlow(id: String): Flow<QueueItem?> {
        return dao.getQueueItemByIdFlow(id).map { it?.toDomainModel() }
    }

    /**
     * Add a new item to the queue
     * @return The generated queue item ID
     */
    suspend fun addToQueue(
        url: String,
        title: String?,
        thumbnailUrl: String?,
        localThumbnailPath: String?,
        platform: String,
        downloadMode: DownloadMode,
        durationMillis: Long = 0L,
        isPartOfCarousel: Boolean = false,
        carouselGroupId: String? = null,
        carouselPosition: Int? = null,
        carouselTotal: Int? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val entity = DownloadQueueEntity(
            id = id,
            url = url,
            title = title,
            thumbnailUrl = thumbnailUrl,
            localThumbnailPath = localThumbnailPath,
            platform = platform,
            downloadMode = downloadMode.name,
            status = QueueItemStatus.PENDING.name,
            progress = 0f,
            statusMessage = "Waiting...",
            errorMessage = null,
            filePath = null,
            fileSize = 0L,
            durationMillis = durationMillis,
            addedAt = System.currentTimeMillis(),
            startedAt = null,
            completedAt = null,
            retryCount = 0,
            isPartOfCarousel = isPartOfCarousel,
            carouselGroupId = carouselGroupId,
            carouselPosition = carouselPosition,
            carouselTotal = carouselTotal
        )
        dao.insertItem(entity)
        return id
    }

    /**
     * Update progress for a queue item
     */
    suspend fun updateProgress(id: String, progress: Float, statusMessage: String) {
        dao.updateProgress(id, progress, statusMessage)
    }

    /**
     * Mark item as started
     */
    suspend fun markAsStarted(id: String, statusMessage: String = "Starting...") {
        dao.markAsStarted(id, System.currentTimeMillis(), statusMessage)
    }

    /**
     * Mark item as completed
     */
    suspend fun markAsCompleted(
        id: String,
        filePath: String,
        fileSize: Long,
        title: String?,
        durationMillis: Long = 0L
    ) {
        dao.markAsCompleted(
            id = id,
            completedAt = System.currentTimeMillis(),
            statusMessage = "Completed",
            filePath = filePath,
            fileSize = fileSize,
            title = title
        )
        
        // Update duration if provided
        if (durationMillis > 0) {
            val entity = dao.getQueueItemById(id)
            if (entity != null) {
                dao.updateItem(entity.copy(durationMillis = durationMillis))
            }
        }
    }

    /**
     * Mark item as failed
     */
    suspend fun markAsFailed(id: String, errorMessage: String) {
        dao.markAsFailed(id, errorMessage, "Failed")
    }

    /**
     * Cancel an item
     */
    suspend fun cancelItem(id: String) {
        dao.cancelItem(id)
    }

    /**
     * Retry a failed or cancelled item
     */
    suspend fun retryItem(id: String) {
        dao.resetForRetry(id)
    }

    /**
     * Remove an item from the queue
     */
    suspend fun removeItem(id: String) {
        dao.deleteItem(id)
    }

    /**
     * Clear all completed items
     */
    suspend fun clearCompleted() {
        dao.clearCompleted()
    }

    /**
     * Clear all finished items (completed, cancelled, failed)
     */
    suspend fun clearFinished() {
        dao.clearFinished()
    }

    /**
     * Get count of active items
     */
    suspend fun getActiveCount(): Int {
        return dao.getActiveCount()
    }

    /**
     * Get count of active items as Flow
     */
    fun getActiveCountFlow(): Flow<Int> {
        return dao.getActiveCountFlow()
    }

    /**
     * Check if URL is already in active queue
     */
    suspend fun isUrlInActiveQueue(url: String): Boolean {
        return dao.isUrlInActiveQueue(url)
    }

    /**
     * Get next pending item
     */
    suspend fun getNextPendingItem(): QueueItem? {
        return dao.getNextPendingItem()?.toDomainModel()
    }

    /**
     * Update thumbnail path for an item
     */
    suspend fun updateThumbnailPath(id: String, localThumbnailPath: String) {
        val entity = dao.getQueueItemById(id)
        if (entity != null) {
            dao.updateItem(entity.copy(localThumbnailPath = localThumbnailPath))
        }
    }

    /**
     * Extension function to convert entity to domain model
     */
    private fun DownloadQueueEntity.toDomainModel(): QueueItem {
        return QueueItem(
            id = id,
            url = url,
            title = title,
            thumbnailUrl = thumbnailUrl,
            localThumbnailPath = localThumbnailPath,
            platform = platform,
            downloadMode = DownloadMode.valueOf(downloadMode),
            status = QueueItemStatus.valueOf(status),
            progress = progress,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
            filePath = filePath,
            fileSize = fileSize,
            durationMillis = durationMillis,
            addedAt = addedAt,
            startedAt = startedAt,
            completedAt = completedAt,
            retryCount = retryCount,
            isPartOfCarousel = isPartOfCarousel,
            carouselGroupId = carouselGroupId,
            carouselPosition = carouselPosition,
            carouselTotal = carouselTotal
        )
    }
}
