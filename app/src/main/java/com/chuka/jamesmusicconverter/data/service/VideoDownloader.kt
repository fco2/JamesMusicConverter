package com.chuka.jamesmusicconverter.data.service

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Service for downloading videos from URLs
 *
 * Supports:
 * - Direct video URLs (via OkHttp)
 * - YouTube, Vimeo, etc. (via yt-dlp)
 */
class VideoDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val ytDlpDownloader = YtDlpDownloader(context)

    /**
     * Downloads a video from the given URL with progress updates
     * Automatically detects if yt-dlp is needed (YouTube, Vimeo, etc.)
     * or if it's a direct video URL
     */
    fun downloadVideo(url: String): Flow<DownloadProgress> = flow {
        emit(DownloadProgress(0f, "Preparing download..."))

        try {
            // Check if URL requires yt-dlp (YouTube, Vimeo, etc.)
            if (requiresYtDlp(url)) {
                Log.d("VideoDownloader", "URL requires yt-dlp: $url")
                emit(DownloadProgress(0.01f, "Checking yt-dlp availability..."))

                // Use yt-dlp for platform-specific URLs
                // Note: isAvailable() may be slow on first call (initializes library)
                val ytDlpAvailable = try {
                    ytDlpDownloader.isAvailable()
                } catch (e: Exception) {
                    Log.e("VideoDownloader", "Failed to check yt-dlp availability", e)
                    false
                }

                if (ytDlpAvailable) {
                    Log.d("VideoDownloader", "Using yt-dlp for: $url")
                    // Use downloadAudioOnly which converts to MP3 using ffmpeg
                    ytDlpDownloader.downloadAudioOnly(url).collect { progress ->
                        emit(progress)
                    }
                    return@flow
                } else {
                    // Get detailed error information
                    val errorDetails = ytDlpDownloader.getInitializationError() ?: "Unknown error"
                    Log.e("VideoDownloader", "yt-dlp not available: $errorDetails")

                    throw Exception(
                        "Cannot download from this platform. " +
                                "The yt-dlp downloader could not be initialized on your device. " +
                                "This may be due to:\n" +
                                "• Incompatible device architecture\n" +
                                "• Missing native libraries\n" +
                                "• Device security restrictions\n\n" +
                                "Please try:\n" +
                                "1. Using a direct video URL (ending in .mp4, .webm, etc.)\n" +
                                "2. Downloading the video separately and converting the file\n\n" +
                                "Technical details: $errorDetails"
                    )
                }
            }

            // Direct video URL - use OkHttp
            Log.d("VideoDownloader", "Using direct download for: $url")
            val request = Request.Builder()
                .url(url)
                .build()

            emit(DownloadProgress(0.1f, "Connecting to server..."))

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Failed to download: HTTP ${response.code}. " +
                            "This URL may not be a direct video link. " +
                            "For YouTube/Vimeo URLs, please use a direct video URL for now.")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                val contentType = response.header("Content-Type")

                Log.d("VideoDownloader", "Content-Type: $contentType")
                Log.d("VideoDownloader", "Content-Length: $contentLength")

                // Validate it's actually a video
                if (contentType != null && !contentType.startsWith("video/") && !contentType.startsWith("application/octet-stream")) {
                    Log.w("VideoDownloader", "Content-Type is not video: $contentType")
                    // Still continue, but log warning
                }

                if (contentLength <= 0) {
                    throw Exception("Invalid content length. This might not be a direct video URL.")
                }

                // Create downloads directory
                val downloadsDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "JamesMusicConverter"
                )
                downloadsDir.mkdirs()

                val outputFile = File(downloadsDir, "temp_video_${System.currentTimeMillis()}.mp4")

                emit(DownloadProgress(0.2f, "Downloading video... (${formatBytes(contentLength)})"))

                body.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            if (contentLength > 0) {
                                val progress = 0.2f + (0.6f * (totalBytesRead.toFloat() / contentLength))
                                val downloaded = formatBytes(totalBytesRead)
                                val total = formatBytes(contentLength)
                                emit(DownloadProgress(
                                    progress,
                                    "Downloading... $downloaded / $total (${(progress * 100).toInt()}%)"
                                ))
                            }
                        }

                        output.flush()
                    }
                }

                // Verify file was actually written
                if (outputFile.length() == 0L) {
                    outputFile.delete()
                    throw Exception("Download failed: File is empty. This URL may not be a direct video link.")
                }

                Log.d("VideoDownloader", "Downloaded ${formatBytes(outputFile.length())} to ${outputFile.absolutePath}")
                emit(DownloadProgress(0.8f, "Download complete", outputFile.absolutePath))
            }
        } catch (e: Exception) {
            Log.e("VideoDownloader", "Download failed", e)
            throw Exception("Download failed: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Check if URL requires yt-dlp (platform-specific URLs)
     */
    private fun requiresYtDlp(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("youtube.com") ||
                lowerUrl.contains("youtu.be") ||
                lowerUrl.contains("vimeo.com") ||
                lowerUrl.contains("dailymotion.com") ||
                lowerUrl.contains("twitch.tv") ||
                lowerUrl.contains("facebook.com") ||
                lowerUrl.contains("instagram.com") ||
                lowerUrl.contains("tiktok.com") ||
                lowerUrl.contains("twitter.com") ||
                lowerUrl.contains("x.com")
    }

    /**
     * Format bytes to human-readable string
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * For YouTube and other platforms, we would use yt-dlp
     * This is a stub showing how it would work
     */
    fun downloadFromYouTube(url: String): Flow<DownloadProgress> = flow {
        emit(DownloadProgress(0f, "Fetching video information..."))

        // In a real implementation, you would:
        // 1. Use yt-dlp binary (via ProcessBuilder) or
        // 2. Use NewPipe Extractor library or
        // 3. Call a backend service that handles the download

        // For now, simulate the process
        emit(DownloadProgress(0.2f, "Downloading from YouTube..."))
        kotlinx.coroutines.delay(2000)

        // This is where you'd actually call yt-dlp or similar
        // Example command: yt-dlp -f bestaudio -o output.mp3 URL

        emit(DownloadProgress(0.8f, "Download complete"))
    }
}

data class DownloadProgress(
    val progress: Float,
    val message: String,
    val filePath: String? = null
)
