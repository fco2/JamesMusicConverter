package com.chuka.jamesmusicconverter.data.service

import android.content.Context
import android.os.Environment
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * YouTube and platform video downloader using youtubedl-android library
 *
 * This uses the youtubedl-android library which bundles yt-dlp with Python runtime,
 * eliminating permission issues and binary extraction problems.
 *
 * Supported platforms: YouTube, Vimeo, TikTok, Instagram, Twitter/X, and 1000+ more
 */
class YtDlpDownloader(private val context: Context) {

    private val TAG = "YtDlpDownloader"

    @Volatile
    private var isInitialized = false

    @Volatile
    private var initializationFailed = false

    @Volatile
    private var initializationError: String? = null

    /**
     * Initialize YoutubeDL library with enhanced error handling
     * This must be called before any other operations
     * Should be called on a background thread as it can be slow
     */
    private fun ensureInitialized() {
        if (isInitialized) return

        if (initializationFailed) {
            throw Exception("YoutubeDL initialization previously failed: $initializationError")
        }

        synchronized(this) {
            if (isInitialized) return

            if (initializationFailed) {
                throw Exception("YoutubeDL initialization previously failed: $initializationError")
            }

            try {
                Log.d(TAG, "Initializing YoutubeDL library...")
                Log.d(TAG, "Native library dir: ${context.applicationInfo.nativeLibraryDir}")
                Log.d(TAG, "App data dir: ${context.filesDir.absolutePath}")

                val ytdl = YoutubeDL.getInstance()

                // Initialize with custom application directory
                // The library will handle loading its own native libraries
                ytdl.init(context)

                // Initialize FFmpeg for audio extraction
                try {
                    FFmpeg.getInstance().init(context)
                    Log.d(TAG, "FFmpeg initialized successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "FFmpeg initialization failed: ${e.message}")
                    // Continue anyway - audio extraction may not work but downloads will
                }

                isInitialized = true
                Log.d(TAG, "YoutubeDL initialized successfully")

                // Try to get version to verify it's working
                try {
                    val version = ytdl.version(context)
                    Log.d(TAG, "YoutubeDL version: $version")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get YoutubeDL version: ${e.message}")
                }

            } catch (e: Exception) {
                initializationFailed = true
                initializationError = e.message ?: "Unknown error"
                Log.e(TAG, "Failed to initialize YoutubeDL", e)
                Log.e(TAG, "Error details: ${e.javaClass.simpleName}: ${e.message}")
                e.printStackTrace()
                throw Exception(
                    "Failed to initialize YoutubeDL library. " +
                            "This may be due to incompatible device architecture or missing native libraries. " +
                            "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Download video using yt-dlp with progress tracking
     * Uses video title as filename if available
     */
    fun downloadVideo(
        url: String,
        outputFileName: String? = null
    ): Flow<DownloadProgress> = callbackFlow {
        trySend(DownloadProgress(0f, "Initializing yt-dlp..."))

        try {
            // Ensure YoutubeDL is initialized
            try {
                ensureInitialized()
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
                close(
                    Exception(
                        "Cannot initialize yt-dlp downloader. " +
                                "This might be due to device compatibility issues. " +
                                "Please try using a direct video URL instead. " +
                                "Technical details: ${e.message}"
                    )
                )
                return@callbackFlow
            }

            trySend(DownloadProgress(0.05f, "Fetching video information..."))

            // Create output directory in public Downloads folder
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "JamesMusicConverter"
            )
            downloadsDir.mkdirs()

            // Use video title as filename, or fallback to provided name
            val fileTemplate = outputFileName ?: "%(title)s"
            val outputTemplate = File(downloadsDir, "$fileTemplate.%(ext)s").absolutePath

            trySend(DownloadProgress(0.1f, "Starting download..."))

            // Create download request
            val request = YoutubeDLRequest(url)
            request.addOption("-o", outputTemplate)
            // Flexible format selection - try best audio, fallback to best overall
            // yt-dlp will automatically choose the best available format
            request.addOption("-f", "bestaudio/best")
            request.addOption("--no-playlist")
            // Add socket timeout to handle network issues
            request.addOption("--socket-timeout", "30")
            // Add retries for network issues
            request.addOption("--retries", "3")

            Log.d(TAG, "Starting download for: $url")
            Log.d(TAG, "Output template: $outputTemplate")

            // Execute download with progress tracking
            val response = YoutubeDL.getInstance().execute(request) { progress, _, line ->
                Log.d(TAG, "Progress: $progress% - $line")

                // Send progress updates (0.1 to 0.9 range)
                val normalizedProgress = 0.1f + (progress / 100f * 0.8f)
                trySend(DownloadProgress(normalizedProgress, "Downloading... $progress%"))
            }

            Log.d(TAG, "Download completed. Exit code: ${response.exitCode}")

            if (response.exitCode == 0) {
                // Find the downloaded file - if we used video title template, search for most recent file
                val downloadedFile = if (outputFileName != null) {
                    findDownloadedFile(downloadsDir, outputFileName)
                } else {
                    // Find most recently downloaded file
                    downloadsDir.listFiles()?.maxByOrNull { it.lastModified() }
                }

                if (downloadedFile != null && downloadedFile.exists()) {
                    val fileSizeMB = downloadedFile.length() / (1024 * 1024)
                    Log.d(TAG, "Downloaded file: ${downloadedFile.absolutePath} ($fileSizeMB MB)")
                    trySend(DownloadProgress(1f, "Download complete", downloadedFile.absolutePath))
                } else {
                    Log.e(TAG, "Download completed but file not found in ${downloadsDir.absolutePath}")
                    close(Exception("Download completed but file not found"))
                }
            } else {
                val errorMsg = response.err ?: "Unknown error"
                Log.e(TAG, "Download failed: $errorMsg")
                Log.e(TAG, "Full error output: ${response.out}")
                close(Exception("Download failed: $errorMsg"))
            }

            close()

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            close(Exception("Download failed: ${e.message}"))
        }

        awaitClose { Log.d(TAG, "Download flow closed") }
    }.flowOn(Dispatchers.IO)

    /**
     * Download audio only (faster, smaller file)
     * Extracts best quality audio from the video and converts to MP3
     *
     * @param url The video URL to download
     * @param outputFileName Optional custom filename (without extension)
     * @param username Optional username for authentication
     * @param password Optional password for authentication
     * @param cookiesFromBrowser Optional browser to extract cookies from (e.g., "chrome", "firefox", "edge")
     */
    fun downloadAudioOnly(
        url: String,
        outputFileName: String? = null,
        username: String? = null,
        password: String? = null,
        cookiesFromBrowser: String? = null
    ): Flow<DownloadProgress> = callbackFlow {
        trySend(DownloadProgress(0f, "Initializing yt-dlp..."))

        var videoMetadata: VideoInfo? = null

        try {
            // Ensure YoutubeDL is initialized
            try {
                ensureInitialized()
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
                close(
                    Exception(
                        "Cannot initialize yt-dlp downloader. " +
                                "This might be due to device compatibility issues. " +
                                "Technical details: ${e.message}"
                    )
                )
                return@callbackFlow
            }

            trySend(DownloadProgress(0.02f, "Fetching video information..."))

            // Try to get video metadata first
            try {
                videoMetadata = getVideoInfo(url)
                if (videoMetadata != null) {
                    Log.d(TAG, "Retrieved metadata: ${videoMetadata.title}")
                    trySend(DownloadProgress(
                        0.05f,
                        "Found: ${videoMetadata.title}",
                        metadata = videoMetadata
                    ))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch video info, continuing anyway: ${e.message}")
            }

            // Create output directory in public Downloads folder
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "JamesMusicConverter"
            )
            downloadsDir.mkdirs()

            // Use video title as filename, or fallback to provided name
            val fileTemplate = outputFileName ?: "%(title)s"
            val outputTemplate = File(downloadsDir, "$fileTemplate.%(ext)s").absolutePath

            trySend(DownloadProgress(0.1f, "Starting download and conversion..."))

            // Create download request for audio only with conversion
            val request = YoutubeDLRequest(url)
            request.addOption("-o", outputTemplate)
            // Extract audio and convert to MP3 using ffmpeg
            request.addOption("-x")  // Extract audio
            request.addOption("--audio-format", "mp3")  // Convert to MP3
            request.addOption("--audio-quality", "0")  // Best quality (320kbps for MP3)
            request.addOption("--no-playlist")
            request.addOption("--socket-timeout", "30")
            // Add retries for network issues
            request.addOption("--retries", "3")
            // Try to avoid geo-blocking issues
            request.addOption("--geo-bypass")

            // Add authentication if provided
            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                Log.d(TAG, "Adding username/password authentication")
                request.addOption("--username", username)
                request.addOption("--password", password)
            }

            // Add cookies from browser if specified
            if (!cookiesFromBrowser.isNullOrBlank()) {
                Log.d(TAG, "Extracting cookies from browser: $cookiesFromBrowser")
                request.addOption("--cookies-from-browser", cookiesFromBrowser)
            }

            Log.d(TAG, "Starting audio-only download for: $url")

            // Execute download with progress tracking
            val response = YoutubeDL.getInstance().execute(request) { progress, _, line ->
                Log.d(TAG, "Audio progress: $progress% - $line")

                val normalizedProgress = 0.1f + (progress / 100f * 0.9f)
                trySend(DownloadProgress(normalizedProgress, "Downloading audio... $progress%"))
            }

            if (response.exitCode == 0) {
                // Find the downloaded file - if we used video title template, search for most recent MP3
                val downloadedFile = if (outputFileName != null) {
                    findDownloadedFile(downloadsDir, outputFileName)
                } else {
                    // Find most recently downloaded MP3 file
                    downloadsDir.listFiles { file -> file.extension == "mp3" }?.maxByOrNull { it.lastModified() }
                }

                if (downloadedFile != null && downloadedFile.exists()) {
                    val fileSizeMB = downloadedFile.length() / (1024 * 1024)
                    Log.d(TAG, "Downloaded audio: ${downloadedFile.absolutePath} ($fileSizeMB MB)")
                    trySend(DownloadProgress(
                        1f,
                        "Audio ready",
                        downloadedFile.absolutePath,
                        metadata = videoMetadata
                    ))
                } else {
                    close(Exception("Audio download completed but file not found"))
                }
            } else {
                val errorMsg = response.err ?: "Unknown error"
                Log.e(TAG, "Audio download error output: ${response.out}")
                close(Exception("Audio download failed: $errorMsg"))
            }

            close()

        } catch (e: Exception) {
            Log.e(TAG, "Audio download failed", e)
            close(Exception("Audio download failed: ${e.message}"))
        }

        awaitClose { Log.d(TAG, "Audio download flow closed") }
    }.flowOn(Dispatchers.IO)

    /**
     * Get video information without downloading
     * Returns metadata like title, duration, thumbnail, etc.
     */
    suspend fun getVideoInfo(url: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()

            Log.d(TAG, "Fetching video info for: $url")

            val request = YoutubeDLRequest(url)
            request.addOption("--dump-json")
            request.addOption("--no-playlist")
            request.addOption("--skip-download")

            val response = YoutubeDL.getInstance().execute(request)

            if (response.exitCode == 0) {
                // Parse JSON output (response.out contains the JSON)
                Log.d(TAG, "Video info retrieved successfully")

                val jsonOutput = response.out

                // Extract metadata using regex patterns (simpler than full JSON parsing)
                val title = extractFromJson(jsonOutput, "title") ?: "Unknown Title"
                val thumbnail = extractFromJson(jsonOutput, "thumbnail")
                val uploader = extractFromJson(jsonOutput, "uploader")
                val durationStr = extractFromJson(jsonOutput, "duration")
                val duration = durationStr?.toLongOrNull() ?: 0L

                Log.d(TAG, "Extracted - Title: $title, Thumbnail: $thumbnail")

                VideoInfo(
                    title = title,
                    duration = duration,
                    thumbnail = thumbnail,
                    uploader = uploader
                )
            } else {
                Log.e(TAG, "Failed to get video info: ${response.err}")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video info", e)
            null
        }
    }

    /**
     * Simple JSON field extractor using regex
     */
    private fun extractFromJson(json: String, fieldName: String): String? {
        return try {
            val pattern = """"$fieldName":\s*"([^"]*)"""".toRegex()
            pattern.find(json)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if YoutubeDL is properly initialized and available
     * This should be called from a background thread as initialization can be slow
     */
    fun isAvailable(): Boolean {
        return try {
            // Try to initialize if not already done
            ensureInitialized()

            // If we got here without exception, initialization succeeded
            Log.d(TAG, "YoutubeDL is available (initialized: $isInitialized)")
            isInitialized
        } catch (e: Exception) {
            Log.e(TAG, "YoutubeDL not available: ${e.message}", e)
            false
        }
    }

    /**
     * Get detailed error information if initialization failed
     */
    fun getInitializationError(): String? {
        return if (initializationFailed) {
            initializationError
        } else {
            null
        }
    }

    /**
     * Get yt-dlp version
     */
    suspend fun getVersion(): String? = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            YoutubeDL.getInstance().version(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting YoutubeDL version", e)
            null
        }
    }

    /**
     * Update yt-dlp to the latest version
     */
    suspend fun updateYtDlp(): Boolean = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()

            Log.d(TAG, "Updating yt-dlp...")
            val status = YoutubeDL.getInstance().updateYoutubeDL(context)
            Log.d(TAG, "Update status: ${status?.name}")
            status == YoutubeDL.UpdateStatus.DONE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update yt-dlp", e)
            false
        }
    }

    /**
     * Find the downloaded file (yt-dlp may change extension)
     */
    private fun findDownloadedFile(directory: File, baseName: String): File? {
        val files = directory.listFiles { file ->
            file.name.startsWith(baseName)
        }

        return files?.maxByOrNull { it.lastModified() }
    }
}

/**
 * Video information from yt-dlp
 */
data class VideoInfo(
    val title: String,
    val duration: Long,
    val thumbnail: String?,
    val uploader: String?
)
