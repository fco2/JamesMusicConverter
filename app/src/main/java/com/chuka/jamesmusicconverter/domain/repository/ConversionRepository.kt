package com.chuka.jamesmusicconverter.domain.repository

import android.content.Context
import com.chuka.jamesmusicconverter.data.service.AudioExtractor
import com.chuka.jamesmusicconverter.data.service.DownloadNotificationService
import com.chuka.jamesmusicconverter.data.service.VideoDownloader
import com.chuka.jamesmusicconverter.domain.model.ConversionProgress
import com.chuka.jamesmusicconverter.domain.model.ConversionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Repository for handling video-to-MP3 conversion operations
 */
interface ConversionRepository {
    fun convertVideoToMp3(
        videoUrl: String,
        username: String? = null,
        password: String? = null,
        cookiesFromBrowser: String? = null
    ): Flow<ConversionProgress>
    suspend fun getConversionResult(videoUrl: String): Result<ConversionResult>
}

class ConversionRepositoryImpl(
    private val videoDownloader: VideoDownloader,
    private val audioExtractor: AudioExtractor,
    private val notificationService: DownloadNotificationService
) : ConversionRepository {

    // Store conversion results by URL to prevent caching across different conversions
    // Use synchronized access to prevent race conditions
    private val conversionResults = mutableMapOf<String, ConversionResult>()
    private val lock = Any()

    /**
     * Converts video to MP3 by downloading and extracting audio
     */
    override fun convertVideoToMp3(
        videoUrl: String,
        username: String?,
        password: String?,
        cookiesFromBrowser: String?
    ): Flow<ConversionProgress> = flow {
        android.util.Log.d("CHUKA_Repository", "=== convertVideoToMp3 called ===")
        android.util.Log.d("CHUKA_Repository", "URL: $videoUrl")

        synchronized(lock) {
            android.util.Log.d("CHUKA_Repository", "Cached results before clear: ${conversionResults.keys}")
            // Clear any previous result for this URL to ensure fresh conversion
            conversionResults.remove(videoUrl)
            android.util.Log.d("CHUKA_Repository", "Cached results after clear: ${conversionResults.keys}")
        }

        try {
            var downloadedFilePath: String? = null
            var videoTitle: String? = null
            var thumbnailUrl: String? = null

            // Step 1: Download the video (0% - 80%)
            videoDownloader.downloadVideo(
                url = videoUrl,
                username = username,
                password = password,
                cookiesFromBrowser = cookiesFromBrowser
            ).collect { downloadProgress ->
                // Ensure progress is never negative
                val normalizedProgress = downloadProgress.progress.coerceAtLeast(0f)
                emit(ConversionProgress(
                    percentage = normalizedProgress * 0.8f,
                    statusMessage = downloadProgress.message
                ))

                if (downloadProgress.filePath != null) {
                    downloadedFilePath = downloadProgress.filePath
                }

                // Capture metadata when available
                if (downloadProgress.metadata != null) {
                    videoTitle = downloadProgress.metadata.title
                    thumbnailUrl = downloadProgress.metadata.thumbnail
                }
            }

            // Step 2: Extract audio and convert to MP3 (80% - 100%)
            if (downloadedFilePath != null) {
                audioExtractor.extractAudioToMp3(downloadedFilePath!!).collect { extractionProgress ->
                    // Ensure progress is never negative
                    val normalizedProgress = extractionProgress.progress.coerceAtLeast(0f)
                    emit(ConversionProgress(
                        percentage = 0.8f + (normalizedProgress * 0.2f),
                        statusMessage = extractionProgress.message
                    ))

                    // Store the result when extraction is complete
                    if (extractionProgress.outputFilePath != null) {
                        val outputFile = File(extractionProgress.outputFilePath)
                        val result = ConversionResult(
                            videoTitle = videoTitle ?: extractTitleFromUrl(videoUrl),
                            thumbnailUrl = thumbnailUrl,
                            fileName = outputFile.name,
                            fileSize = extractionProgress.fileSize,
                            filePath = extractionProgress.outputFilePath,
                            durationMillis = extractionProgress.durationMillis
                        )

                        // Store result with synchronization to prevent race conditions
                        synchronized(lock) {
                            android.util.Log.d("CHUKA_Repository", "Storing result for URL: $videoUrl -> ${result.fileName} (${result.durationMillis}ms)")
                            conversionResults[videoUrl] = result
                        }

                        // Show notification when conversion is complete
                        notificationService.showDownloadCompletedNotification(
                            fileName = outputFile.name,
                            filePath = extractionProgress.outputFilePath,
                            fileSize = extractionProgress.fileSize
                        )
                    }
                }
            } else {
                throw Exception("Failed to download video")
            }
        } catch (e: Exception) {
            throw Exception("Conversion failed: ${e.message}")
        }
    }

    /**
     * Gets the final conversion result
     */
    override suspend fun getConversionResult(videoUrl: String): Result<ConversionResult> {
        android.util.Log.d("CHUKA_Repository", "=== getConversionResult called ===")
        android.util.Log.d("CHUKA_Repository", "URL: $videoUrl")

        return try {
            val result = synchronized(lock) {
                android.util.Log.d("CHUKA_Repository", "All cached results: ${conversionResults.keys}")
                conversionResults[videoUrl]
            }

            if (result != null) {
                android.util.Log.d("CHUKA_Repository", "Found result: ${result.fileName} for URL: $videoUrl")
                Result.success(result)
            } else {
                android.util.Log.e("CHUKA_Repository", "No result found for URL: $videoUrl")
                Result.failure(Exception("No conversion result available"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CHUKA_Repository", "Exception: ${e.message}")
            Result.failure(e)
        }
    }

    private fun extractTitleFromUrl(url: String): String {
        // Simple title extraction from URL
        // In a real app, you would fetch this from the video platform's API
        return when {
            url.contains("youtube.com") || url.contains("youtu.be") -> "YouTube Video"
            url.contains("vimeo.com") -> "Vimeo Video"
            url.contains("dailymotion.com") -> "Dailymotion Video"
            else -> "Video"
        }
    }
}
