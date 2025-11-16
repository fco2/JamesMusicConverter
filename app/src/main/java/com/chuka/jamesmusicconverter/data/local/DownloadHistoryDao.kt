package com.chuka.jamesmusicconverter.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for download history
 */
@Dao
interface DownloadHistoryDao {

    /**
     * Insert a new download entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadHistoryEntity): Long

    /**
     * Get all downloads ordered by most recent first
     */
    @Query("SELECT * FROM download_history ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadHistoryEntity>>

    /**
     * Get downloads by platform
     */
    @Query("SELECT * FROM download_history WHERE platform = :platform ORDER BY downloadedAt DESC")
    fun getDownloadsByPlatform(platform: String): Flow<List<DownloadHistoryEntity>>

    /**
     * Get downloads by mode (AUDIO or VIDEO)
     */
    @Query("SELECT * FROM download_history WHERE downloadMode = :mode ORDER BY downloadedAt DESC")
    fun getDownloadsByMode(mode: String): Flow<List<DownloadHistoryEntity>>

    /**
     * Search downloads by title
     */
    @Query("SELECT * FROM download_history WHERE title LIKE '%' || :query || '%' ORDER BY downloadedAt DESC")
    fun searchDownloads(query: String): Flow<List<DownloadHistoryEntity>>

    /**
     * Get a specific download by ID
     */
    @Query("SELECT * FROM download_history WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadHistoryEntity?

    /**
     * Update file exists status
     */
    @Query("UPDATE download_history SET fileExists = :exists WHERE id = :id")
    suspend fun updateFileExists(id: Long, exists: Boolean)

    /**
     * Delete a download entry
     */
    @Delete
    suspend fun deleteDownload(download: DownloadHistoryEntity)

    /**
     * Delete download by ID
     */
    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    /**
     * Clear all history
     */
    @Query("DELETE FROM download_history")
    suspend fun clearAllHistory()

    /**
     * Get total download count
     */
    @Query("SELECT COUNT(*) FROM download_history")
    suspend fun getTotalDownloadCount(): Int

    /**
     * Get total file size
     */
    @Query("SELECT SUM(fileSize) FROM download_history")
    suspend fun getTotalFileSize(): Long?

    /**
     * Get recent downloads (last 10)
     */
    @Query("SELECT * FROM download_history ORDER BY downloadedAt DESC LIMIT :limit")
    fun getRecentDownloads(limit: Int = 10): Flow<List<DownloadHistoryEntity>>
}
