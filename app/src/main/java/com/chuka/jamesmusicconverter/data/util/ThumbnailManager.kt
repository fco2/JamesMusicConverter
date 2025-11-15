package com.chuka.jamesmusicconverter.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages thumbnail storage and retrieval
 */
@Singleton
class ThumbnailManager @Inject constructor(
    private val context: Context
) {

    private val TAG = "ThumbnailManager"

    /**
     * Get the thumbnails directory
     */
    private fun getThumbnailsDir(): File {
        val dir = File(context.filesDir, "thumbnails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Download and save thumbnail from URL
     * Returns the local file path if successful
     */
    suspend fun downloadAndSaveThumbnail(
        thumbnailUrl: String,
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading thumbnail from: $thumbnailUrl")

            // Download image from URL
            val url = URL(thumbnailUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap != null) {
                // Save bitmap to local storage
                val thumbnailsDir = getThumbnailsDir()
                val sanitizedFileName = fileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                val thumbnailFile = File(thumbnailsDir, "${sanitizedFileName}.jpg")

                FileOutputStream(thumbnailFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()

                Log.d(TAG, "Thumbnail saved to: ${thumbnailFile.absolutePath}")
                thumbnailFile.absolutePath
            } else {
                Log.w(TAG, "Failed to decode bitmap from URL")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading thumbnail", e)
            null
        }
    }

    /**
     * Save an existing bitmap as thumbnail
     */
    suspend fun saveBitmap(
        bitmap: Bitmap,
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val thumbnailsDir = getThumbnailsDir()
            val sanitizedFileName = fileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val thumbnailFile = File(thumbnailsDir, "${sanitizedFileName}.jpg")

            FileOutputStream(thumbnailFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            Log.d(TAG, "Bitmap saved to: ${thumbnailFile.absolutePath}")
            thumbnailFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap", e)
            null
        }
    }

    /**
     * Delete a thumbnail file
     */
    fun deleteThumbnail(thumbnailPath: String?): Boolean {
        return try {
            if (thumbnailPath != null) {
                val file = File(thumbnailPath)
                if (file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting thumbnail", e)
            false
        }
    }

    /**
     * Clear all thumbnails
     */
    fun clearAllThumbnails() {
        try {
            val thumbnailsDir = getThumbnailsDir()
            thumbnailsDir.listFiles()?.forEach { file ->
                file.delete()
            }
            Log.d(TAG, "All thumbnails cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing thumbnails", e)
        }
    }

    /**
     * Get total size of all thumbnails
     */
    fun getTotalThumbnailSize(): Long {
        return try {
            val thumbnailsDir = getThumbnailsDir()
            thumbnailsDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating thumbnail size", e)
            0L
        }
    }
}
