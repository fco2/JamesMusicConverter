package com.chuka.jamesmusicconverter.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.chuka.jamesmusicconverter.MainActivity
import com.chuka.jamesmusicconverter.R
import com.chuka.jamesmusicconverter.data.service.AudioExtractor
import com.chuka.jamesmusicconverter.data.service.DownloadNotificationService
import com.chuka.jamesmusicconverter.data.service.VideoDownloader
import com.chuka.jamesmusicconverter.data.util.ThumbnailManager
import com.chuka.jamesmusicconverter.domain.repository.DownloadHistoryRepository
import com.chuka.jamesmusicconverter.domain.repository.DownloadQueueRepository
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * WorkManager worker for background video downloads
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val videoDownloader: VideoDownloader,
    private val audioExtractor: AudioExtractor,
    private val queueRepository: DownloadQueueRepository,
    private val historyRepository: DownloadHistoryRepository,
    private val notificationService: DownloadNotificationService,
    private val thumbnailManager: ThumbnailManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "DownloadWorker"
        private const val CHANNEL_ID = "download_worker_channel"
        private const val CHANNEL_NAME = "Download Progress"

        // Input keys
        const val KEY_QUEUE_ID = "queue_id"
        const val KEY_URL = "url"
        const val KEY_DOWNLOAD_MODE = "download_mode"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_COOKIES_BROWSER = "cookies_browser"

        // Output keys
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS_MESSAGE = "status_message"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_FILE_SIZE = "file_size"
        const val KEY_TITLE = "title"
        const val KEY_ERROR = "error"
    }

    private val notificationId: Int
        get() = inputData.getString(KEY_QUEUE_ID)?.hashCode() ?: id.hashCode()

    override suspend fun doWork(): Result {
        val queueId = inputData.getString(KEY_QUEUE_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val downloadModeStr = inputData.getString(KEY_DOWNLOAD_MODE) ?: DownloadMode.AUDIO.name
        val downloadMode = DownloadMode.valueOf(downloadModeStr)
        val username = inputData.getString(KEY_USERNAME)
        val password = inputData.getString(KEY_PASSWORD)
        val cookiesFromBrowser = inputData.getString(KEY_COOKIES_BROWSER)

        Log.d(TAG, "Starting download for queue ID: $queueId, URL: $url, mode: $downloadMode")

        try {
            // Set foreground with notification
            setForeground(createForegroundInfo("Initializing download..."))

            // Mark as started
            queueRepository.markAsStarted(queueId, "Starting download...")

            var downloadedFilePath: String? = null
            var videoTitle: String? = null
            var thumbnailUrl: String? = null
            var fileSize: Long = 0
            var videoDurationMillis: Long = 0L

            when (downloadMode) {
                DownloadMode.AUDIO -> {
                    // Download audio and convert to MP3
                    videoDownloader.downloadVideo(
                        url = url,
                        username = username,
                        password = password,
                        cookiesFromBrowser = cookiesFromBrowser,
                        downloadMode = DownloadMode.AUDIO
                    ).collect { downloadProgress ->
                        coroutineContext.ensureActive()

                        val normalizedProgress = downloadProgress.progress.coerceAtLeast(0f)
                        val progress = normalizedProgress * 0.8f

                        // Update queue progress
                        queueRepository.updateProgress(queueId, progress, downloadProgress.message)

                        // Update notification
                        setForeground(createForegroundInfo(downloadProgress.message, (progress * 100).toInt()))

                        // Update WorkManager progress data
                        setProgress(workDataOf(
                            KEY_PROGRESS to progress,
                            KEY_STATUS_MESSAGE to downloadProgress.message
                        ))

                        if (downloadProgress.filePath != null) {
                            downloadedFilePath = downloadProgress.filePath
                        }

                        if (downloadProgress.metadata != null) {
                            videoTitle = downloadProgress.metadata.title
                            thumbnailUrl = downloadProgress.metadata.thumbnail
                            videoDurationMillis = downloadProgress.metadata.duration * 1000  // Convert seconds to millis
                        }
                    }

                    // Extract audio to MP3
                    if (downloadedFilePath != null) {
                        audioExtractor.extractAudioToMp3(downloadedFilePath!!).collect { extractionProgress ->
                            coroutineContext.ensureActive()

                            val normalizedProgress = extractionProgress.progress.coerceAtLeast(0f)
                            val progress = 0.8f + (normalizedProgress * 0.2f)

                            queueRepository.updateProgress(queueId, progress, extractionProgress.message)
                            setForeground(createForegroundInfo(extractionProgress.message, (progress * 100).toInt()))
                            setProgress(workDataOf(
                                KEY_PROGRESS to progress,
                                KEY_STATUS_MESSAGE to extractionProgress.message
                            ))

                            if (extractionProgress.outputFilePath != null) {
                                downloadedFilePath = extractionProgress.outputFilePath
                                fileSize = extractionProgress.fileSize
                            }
                        }
                    } else {
                        throw Exception("Failed to download audio")
                    }
                }

                DownloadMode.VIDEO -> {
                    // Download video as MP4
                    videoDownloader.downloadVideo(
                        url = url,
                        username = username,
                        password = password,
                        cookiesFromBrowser = cookiesFromBrowser,
                        downloadMode = DownloadMode.VIDEO
                    ).collect { downloadProgress ->
                        coroutineContext.ensureActive()

                        val normalizedProgress = downloadProgress.progress.coerceAtLeast(0f)

                        queueRepository.updateProgress(queueId, normalizedProgress, downloadProgress.message)
                        setForeground(createForegroundInfo(downloadProgress.message, (normalizedProgress * 100).toInt()))
                        setProgress(workDataOf(
                            KEY_PROGRESS to normalizedProgress,
                            KEY_STATUS_MESSAGE to downloadProgress.message
                        ))

                        if (downloadProgress.filePath != null) {
                            downloadedFilePath = downloadProgress.filePath
                        }

                        if (downloadProgress.metadata != null) {
                            videoTitle = downloadProgress.metadata.title
                            thumbnailUrl = downloadProgress.metadata.thumbnail
                            videoDurationMillis = downloadProgress.metadata.duration * 1000  // Convert seconds to millis
                        }

                        if (downloadProgress.progress >= 1.0f && downloadedFilePath != null) {
                            val outputFile = File(downloadedFilePath!!)
                            fileSize = outputFile.length()
                        }
                    }
                }
            }

            // Complete successfully
            if (downloadedFilePath != null) {
                val outputFile = File(downloadedFilePath!!)
                if (fileSize == 0L) {
                    fileSize = outputFile.length()
                }

                // Mark as completed in queue
                queueRepository.markAsCompleted(
                    id = queueId,
                    filePath = downloadedFilePath!!,
                    fileSize = fileSize,
                    title = videoTitle,
                    durationMillis = videoDurationMillis
                )

                // Capture thumbnailUrl in local val for smart cast
                val currentThumbnailUrl = thumbnailUrl

                // Save thumbnail if available
                if (currentThumbnailUrl != null) {
                    try {
                        val localThumbnailPath = thumbnailManager.downloadAndSaveThumbnail(
                            currentThumbnailUrl,
                            outputFile.name
                        )
                        if (localThumbnailPath != null) {
                            queueRepository.updateThumbnailPath(queueId, localThumbnailPath)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save thumbnail", e)
                    }
                }

                // Save to history with carousel info from queue item
                try {
                    val localThumbnailPath = if (currentThumbnailUrl != null) {
                        thumbnailManager.downloadAndSaveThumbnail(currentThumbnailUrl, outputFile.name)
                    } else null

                    // Get carousel info from queue item
                    val queueItem = queueRepository.getQueueItemById(queueId)

                    historyRepository.addDownload(
                        title = videoTitle ?: outputFile.nameWithoutExtension,
                        url = url,
                        fileName = outputFile.name,
                        filePath = downloadedFilePath!!,
                        fileSize = fileSize,
                        localThumbnailPath = localThumbnailPath,
                        remoteThumbnailUrl = currentThumbnailUrl,
                        platform = detectPlatform(url),
                        downloadMode = downloadMode,
                        duration = videoDurationMillis,
                        uploader = null,
                        isPartOfCarousel = queueItem?.isPartOfCarousel ?: false,
                        carouselGroupId = queueItem?.carouselGroupId,
                        carouselPosition = queueItem?.carouselPosition,
                        carouselTotal = queueItem?.carouselTotal
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save to history", e)
                }

                // Show completion notification
                notificationService.showDownloadCompletedNotification(
                    fileName = outputFile.name,
                    filePath = downloadedFilePath!!,
                    fileSize = fileSize
                )

                Log.d(TAG, "Download completed: $downloadedFilePath")

                return Result.success(workDataOf(
                    KEY_FILE_PATH to downloadedFilePath,
                    KEY_FILE_SIZE to fileSize,
                    KEY_TITLE to videoTitle
                ))
            } else {
                throw Exception("Download failed: No output file")
            }

        } catch (e: CancellationException) {
            Log.d(TAG, "Download cancelled for queue ID: $queueId")
            queueRepository.cancelItem(queueId)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for queue ID: $queueId", e)
            queueRepository.markAsFailed(queueId, e.message ?: "Unknown error")
            return Result.failure(workDataOf(KEY_ERROR to e.message))
        }
    }

    private fun createForegroundInfo(message: String, progress: Int = 0): ForegroundInfo {
        createNotificationChannel()

        val intent = Intent(appContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Downloading Media")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setColor(0xFF607D8B.toInt())
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSound(null)
            .build()

        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress"
                setSound(null, null)
            }

            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

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
}
