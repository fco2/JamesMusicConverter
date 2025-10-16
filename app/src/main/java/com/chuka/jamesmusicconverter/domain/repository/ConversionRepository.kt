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

    // Store the last conversion result
    private var lastConversionResult: ConversionResult? = null

    /**
     * Converts video to MP3 by downloading and extracting audio
     */
    override fun convertVideoToMp3(
        videoUrl: String,
        username: String?,
        password: String?,
        cookiesFromBrowser: String?
    ): Flow<ConversionProgress> = flow {
        try {
            var downloadedFilePath: String? = null

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
                        lastConversionResult = ConversionResult(
                            videoTitle = extractTitleFromUrl(videoUrl),
                            thumbnailUrl = null,
                            fileName = outputFile.name,
                            fileSize = extractionProgress.fileSize,
                            filePath = extractionProgress.outputFilePath
                        )

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
        return try {
            if (lastConversionResult != null) {
                Result.success(lastConversionResult!!)
            } else {
                Result.failure(Exception("No conversion result available"))
            }
        } catch (e: Exception) {
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
