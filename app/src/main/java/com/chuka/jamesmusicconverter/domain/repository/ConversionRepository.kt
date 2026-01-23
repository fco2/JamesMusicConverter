package com.chuka.jamesmusicconverter.domain.repository

import android.content.Context
import com.chuka.jamesmusicconverter.data.service.AudioExtractor
import com.chuka.jamesmusicconverter.data.service.DownloadForegroundService
import com.chuka.jamesmusicconverter.data.service.DownloadNotificationService
import com.chuka.jamesmusicconverter.data.service.VideoDownloader
import com.chuka.jamesmusicconverter.domain.model.ConversionProgress
import com.chuka.jamesmusicconverter.domain.model.ConversionResult
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import java.io.File

/**
 * Repository for handling video-to-MP3 conversion and video download operations
 */
// group video: https://www.instagram.com/p/DS8oWxXj4PV/?utm_source=ig_web_copy_link&igsh=MzRlODBiNWFlZA==

interface ConversionRepository {
    fun convertVideoToMp3(
        videoUrl: String,
        username: String? = null,
        password: String? = null,
        cookiesFromBrowser: String? = null
    ): Flow<ConversionProgress>

    fun downloadMedia(
        videoUrl: String,
        username: String? = null,
        password: String? = null,
        cookiesFromBrowser: String? = null,
        downloadMode: DownloadMode = DownloadMode.AUDIO
    ): Flow<ConversionProgress>

    suspend fun getConversionResult(videoUrl: String): Result<ConversionResult>
}

class ConversionRepositoryImpl(
    private val context: Context,
    private val videoDownloader: VideoDownloader,
    private val audioExtractor: AudioExtractor,
    private val notificationService: DownloadNotificationService,
    private val downloadHistoryRepository: DownloadHistoryRepository,
    private val thumbnailManager: com.chuka.jamesmusicconverter.data.util.ThumbnailManager
) : ConversionRepository {

    // Store conversion results by URL to prevent caching across different conversions
    // Use synchronized access to prevent race conditions
    private val conversionResults = mutableMapOf<String, ConversionResult>()
    private val lock = Any()

    /**
     * Downloads media (video or audio) based on the download mode
     * For AUDIO mode: downloads and converts to MP3
     * For VIDEO mode: downloads video as MP4
     */
    override fun downloadMedia(
        videoUrl: String,
        username: String?,
        password: String?,
        cookiesFromBrowser: String?,
        downloadMode: DownloadMode
    ): Flow<ConversionProgress> = flow {
        android.util.Log.d("CHUKA_Repository", "=== downloadMedia called ===")
        android.util.Log.d("CHUKA_Repository", "URL: $videoUrl, Mode: $downloadMode")

        synchronized(lock) {
            android.util.Log.d("CHUKA_Repository", "Cached results before clear: ${conversionResults.keys}")
            // Clear any previous result for this URL to ensure fresh conversion
            conversionResults.remove(videoUrl)
            android.util.Log.d("CHUKA_Repository", "Cached results after clear: ${conversionResults.keys}")
        }

        try {
            // Start foreground service to keep process alive during download
            DownloadForegroundService.startService(context)
            var downloadedFilePath: String? = null
            var videoTitle: String? = null
            var thumbnailUrl: String? = null

            when (downloadMode) {
                DownloadMode.AUDIO -> {
                    // Step 1: Download the video (0% - 80%)
                    videoDownloader.downloadVideo(
                        url = videoUrl,
                        username = username,
                        password = password,
                        cookiesFromBrowser = cookiesFromBrowser,
                        downloadMode = DownloadMode.AUDIO
                    ).collect { downloadProgress ->
                        // Ensure progress is never negative
                        val normalizedProgress = downloadProgress.progress.coerceAtLeast(0f)
                        val progress = ConversionProgress(
                            percentage = normalizedProgress * 0.8f,
                            statusMessage = downloadProgress.message
                        )

                        // Update foreground service notification
                        DownloadForegroundService.updateProgress(
                            context,
                            downloadProgress.message,
                            normalizedProgress * 0.8f
                        )

                        emit(progress)

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
                            val progress = ConversionProgress(
                                percentage = 0.8f + (normalizedProgress * 0.2f),
                                statusMessage = extractionProgress.message
                            )

                            // Update foreground service notification
                            DownloadForegroundService.updateProgress(
                                context,
                                extractionProgress.message,
                                0.8f + (normalizedProgress * 0.2f)
                            )

                            emit(progress)

                            // Store the result when extraction is complete
                            if (extractionProgress.outputFilePath != null) {
                                val outputFile = File(extractionProgress.outputFilePath)
                                val result = ConversionResult(
                                    videoTitle = videoTitle ?: extractTitleFromUrl(videoUrl),
                                    thumbnailUrl = thumbnailUrl,
                                    fileName = outputFile.name,
                                    fileSize = extractionProgress.fileSize,
                                    filePath = extractionProgress.outputFilePath,
                                    durationMillis = extractionProgress.durationMillis,
                                    isVideo = false
                                )

                                // Store result with synchronization to prevent race conditions
                                synchronized(lock) {
                                    android.util.Log.d("CHUKA_Repository", "Storing audio result for URL: $videoUrl -> ${result.fileName}")
                                    conversionResults[videoUrl] = result
                                }

                                // Save to history
                                saveToHistory(videoUrl, result, DownloadMode.AUDIO)

                                // Show notification when conversion is complete
                                notificationService.showDownloadCompletedNotification(
                                    fileName = outputFile.name,
                                    filePath = extractionProgress.outputFilePath,
                                    fileSize = extractionProgress.fileSize
                                )
                            }
                        }
                    } else {
                        throw Exception("Failed to download audio")
                    }
                }

                DownloadMode.VIDEO -> {
                    // Download video as MP4 (0% - 100%)
                    var downloadedVideos: List<com.chuka.jamesmusicconverter.domain.model.VideoItem> = emptyList()

                    videoDownloader.downloadVideo(
                        url = videoUrl,
                        username = username,
                        password = password,
                        cookiesFromBrowser = cookiesFromBrowser,
                        downloadMode = DownloadMode.VIDEO
                    ).collect { downloadProgress ->
                        // Ensure progress is never negative
                        val normalizedProgress = downloadProgress.progress.coerceAtLeast(0f)
                        val progress = ConversionProgress(
                            percentage = normalizedProgress,
                            statusMessage = downloadProgress.message
                        )

                        // Update foreground service notification
                        DownloadForegroundService.updateProgress(
                            context,
                            downloadProgress.message,
                            normalizedProgress
                        )

                        emit(progress)

                        if (downloadProgress.filePath != null) {
                            downloadedFilePath = downloadProgress.filePath
                        }

                        // Capture metadata when available
                        if (downloadProgress.metadata != null) {
                            videoTitle = downloadProgress.metadata.title
                            thumbnailUrl = downloadProgress.metadata.thumbnail
                        }

                        // Capture all downloaded files (for playlists)
                        if (downloadProgress.downloadedFiles.isNotEmpty()) {
                            downloadedVideos = downloadProgress.downloadedFiles
                            android.util.Log.d("CHUKA_Repository", "Captured ${downloadedVideos.size} videos from download")
                        }

                        // Store the result when download is complete
                        if (downloadProgress.progress >= 1.0f && downloadedFilePath != null) {
                            val outputFile = File(downloadedFilePath!!)
                            val result = ConversionResult(
                                videoTitle = videoTitle ?: extractTitleFromUrl(videoUrl),
                                thumbnailUrl = thumbnailUrl,
                                fileName = outputFile.name,
                                fileSize = outputFile.length(),
                                filePath = downloadedFilePath!!,
                                durationMillis = 0L,
                                isVideo = true,
                                videos = downloadedVideos  // Include all downloaded videos
                            )

                            // Store result with synchronization to prevent race conditions
                            synchronized(lock) {
                                android.util.Log.d("CHUKA_Repository", "Storing video result for URL: $videoUrl -> ${result.fileName} (${result.getVideoCount()} videos)")
                                conversionResults[videoUrl] = result
                            }

                            // Save to history
                            saveToHistory(videoUrl, result, DownloadMode.VIDEO)

                            // Show notification when download is complete
                            val notificationMessage = if (downloadedVideos.size > 1) {
                                "Downloaded ${downloadedVideos.size} videos"
                            } else {
                                outputFile.name
                            }
                            notificationService.showDownloadCompletedNotification(
                                fileName = notificationMessage,
                                filePath = downloadedFilePath!!,
                                fileSize = result.getTotalSize()
                            )
                        }
                    }
                }
            }

            // Stop foreground service when download completes successfully
            DownloadForegroundService.stopService(context)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Stop foreground service on cancellation
            DownloadForegroundService.stopService(context)
            // Re-throw CancellationException as-is so it can be properly handled by the ViewModel
            throw e
        } catch (e: Exception) {
            // Stop foreground service on error
            DownloadForegroundService.stopService(context)
            throw Exception("Download failed: ${e.message}")
        }
    }

    /**
     * Converts video to MP3 by downloading and extracting audio (legacy method)
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
            // Start foreground service to keep process alive during download
            DownloadForegroundService.startService(context)
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
                val progress = ConversionProgress(
                    percentage = normalizedProgress * 0.8f,
                    statusMessage = downloadProgress.message
                )

                // Update foreground service notification
                DownloadForegroundService.updateProgress(
                    context,
                    downloadProgress.message,
                    normalizedProgress * 0.8f
                )

                emit(progress)

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
                    val progress = ConversionProgress(
                        percentage = 0.8f + (normalizedProgress * 0.2f),
                        statusMessage = extractionProgress.message
                    )

                    // Update foreground service notification
                    DownloadForegroundService.updateProgress(
                        context,
                        extractionProgress.message,
                        0.8f + (normalizedProgress * 0.2f)
                    )

                    emit(progress)

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

                        // Save to history
                        saveToHistory(videoUrl, result, DownloadMode.AUDIO)

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

            // Stop foreground service when conversion completes successfully
            DownloadForegroundService.stopService(context)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Stop foreground service on cancellation
            DownloadForegroundService.stopService(context)
            // Re-throw CancellationException as-is so it can be properly handled by the ViewModel
            throw e
        } catch (e: Exception) {
            // Stop foreground service on error
            DownloadForegroundService.stopService(context)
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
            url.contains("dailymotion.com") -> "Dailymotion Video"
            else -> "Video"
        }
    }

    /**
     * Detect platform from URL
     */
    private fun detectPlatform(url: String): String {
        return when {
            url.contains("youtube.com", ignoreCase = true) ||
                    url.contains("youtu.be", ignoreCase = true) -> "YouTube"

            url.contains("tiktok.com", ignoreCase = true) -> "TikTok"
            url.contains("instagram.com", ignoreCase = true) -> "Instagram"
            url.contains("twitter.com", ignoreCase = true) ||
                    url.contains("x.com", ignoreCase = true) -> "Twitter"

            url.contains("facebook.com", ignoreCase = true) ||
                    url.contains("fb.watch", ignoreCase = true) -> "Facebook"

            url.contains("dailymotion.com", ignoreCase = true) -> "Dailymotion"
            else -> "Other"
        }
    }

    /**
     * Save download to history
     */
    private suspend fun saveToHistory(
        videoUrl: String,
        result: ConversionResult,
        downloadMode: DownloadMode
    ) {
        try {
            // Download and save thumbnail if available
            val localThumbnailPath = if (result.thumbnailUrl != null) {
                thumbnailManager.downloadAndSaveThumbnail(
                    result.thumbnailUrl,
                    result.fileName
                )
            } else null

            // Add to history
            downloadHistoryRepository.addDownload(
                title = result.videoTitle,
                url = videoUrl,
                fileName = result.fileName,
                filePath = result.filePath,
                fileSize = result.fileSize,
                localThumbnailPath = localThumbnailPath,
                remoteThumbnailUrl = result.thumbnailUrl,
                platform = detectPlatform(videoUrl),
                downloadMode = downloadMode,
                duration = result.durationMillis / 1000, // Convert to seconds
                uploader = null // We don't have uploader info in ConversionResult
            )

            android.util.Log.d("CHUKA_Repository", "Saved to history: ${result.fileName}")
        } catch (e: Exception) {
            android.util.Log.e("CHUKA_Repository", "Failed to save to history", e)
            // Don't fail the download if history save fails
        }
    }
}
