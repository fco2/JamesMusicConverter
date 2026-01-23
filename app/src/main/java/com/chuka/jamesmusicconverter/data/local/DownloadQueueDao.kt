package com.chuka.jamesmusicconverter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for download queue
 */
@Dao
interface DownloadQueueDao {

    /**
     * Get all queue items ordered by added time
     */
    @Query("SELECT * FROM download_queue ORDER BY addedAt ASC")
    fun getAllQueueItems(): Flow<List<DownloadQueueEntity>>

    /**
     * Get active queue (pending and active items)
     */
    @Query("SELECT * FROM download_queue WHERE status IN ('PENDING', 'ACTIVE') ORDER BY addedAt ASC")
    fun getActiveQueue(): Flow<List<DownloadQueueEntity>>

    /**
     * Get queue item by ID
     */
    @Query("SELECT * FROM download_queue WHERE id = :id")
    suspend fun getQueueItemById(id: String): DownloadQueueEntity?

    /**
     * Get queue item by ID as Flow
     */
    @Query("SELECT * FROM download_queue WHERE id = :id")
    fun getQueueItemByIdFlow(id: String): Flow<DownloadQueueEntity?>

    /**
     * Insert a new queue item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: DownloadQueueEntity)

    /**
     * Update a queue item
     */
    @Update
    suspend fun updateItem(item: DownloadQueueEntity)

    /**
     * Update progress for an item
     */
    @Query("UPDATE download_queue SET progress = :progress, statusMessage = :statusMessage WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, statusMessage: String)

    /**
     * Update status for an item
     */
    @Query("UPDATE download_queue SET status = :status, statusMessage = :statusMessage WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, statusMessage: String)

    /**
     * Mark item as started
     */
    @Query("UPDATE download_queue SET status = 'ACTIVE', startedAt = :startedAt, statusMessage = :statusMessage WHERE id = :id")
    suspend fun markAsStarted(id: String, startedAt: Long, statusMessage: String)

    /**
     * Mark item as completed
     */
    @Query("UPDATE download_queue SET status = 'COMPLETED', progress = 1.0, completedAt = :completedAt, statusMessage = :statusMessage, filePath = :filePath, fileSize = :fileSize, title = :title WHERE id = :id")
    suspend fun markAsCompleted(
        id: String,
        completedAt: Long,
        statusMessage: String,
        filePath: String,
        fileSize: Long,
        title: String?
    )

    /**
     * Mark item as failed
     */
    @Query("UPDATE download_queue SET status = 'FAILED', errorMessage = :errorMessage, statusMessage = :statusMessage WHERE id = :id")
    suspend fun markAsFailed(id: String, errorMessage: String, statusMessage: String)

    /**
     * Cancel an item
     */
    @Query("UPDATE download_queue SET status = 'CANCELLED', statusMessage = 'Cancelled' WHERE id = :id")
    suspend fun cancelItem(id: String)

    /**
     * Reset item for retry (increment retry count)
     */
    @Query("UPDATE download_queue SET status = 'PENDING', progress = 0, errorMessage = null, statusMessage = 'Waiting...', retryCount = retryCount + 1, startedAt = null, completedAt = null WHERE id = :id")
    suspend fun resetForRetry(id: String)

    /**
     * Delete an item
     */
    @Query("DELETE FROM download_queue WHERE id = :id")
    suspend fun deleteItem(id: String)

    /**
     * Clear all completed items
     */
    @Query("DELETE FROM download_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()

    /**
     * Clear all cancelled items
     */
    @Query("DELETE FROM download_queue WHERE status = 'CANCELLED'")
    suspend fun clearCancelled()

    /**
     * Clear all failed items
     */
    @Query("DELETE FROM download_queue WHERE status = 'FAILED'")
    suspend fun clearFailed()

    /**
     * Clear all finished items (completed, cancelled, failed)
     */
    @Query("DELETE FROM download_queue WHERE status IN ('COMPLETED', 'CANCELLED', 'FAILED')")
    suspend fun clearFinished()

    /**
     * Get count of active items (pending + active)
     */
    @Query("SELECT COUNT(*) FROM download_queue WHERE status IN ('PENDING', 'ACTIVE')")
    suspend fun getActiveCount(): Int

    /**
     * Get count of active items as Flow
     */
    @Query("SELECT COUNT(*) FROM download_queue WHERE status IN ('PENDING', 'ACTIVE')")
    fun getActiveCountFlow(): Flow<Int>

    /**
     * Get count of pending items
     */
    @Query("SELECT COUNT(*) FROM download_queue WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    /**
     * Get count of completed items
     */
    @Query("SELECT COUNT(*) FROM download_queue WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int

    /**
     * Get next pending item (oldest first)
     */
    @Query("SELECT * FROM download_queue WHERE status = 'PENDING' ORDER BY addedAt ASC LIMIT 1")
    suspend fun getNextPendingItem(): DownloadQueueEntity?

    /**
     * Check if URL already exists in active queue
     */
    @Query("SELECT EXISTS(SELECT 1 FROM download_queue WHERE url = :url AND status IN ('PENDING', 'ACTIVE'))")
    suspend fun isUrlInActiveQueue(url: String): Boolean
}
