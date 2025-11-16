package com.chuka.jamesmusicconverter.data.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
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
import java.io.FileOutputStream

/**
 * YouTube and platform video downloader using youtubedl-android library
 *
 * This uses the youtubedl-android library which bundles yt-dlp with Python runtime,
 * eliminating permission issues and binary extraction problems.
 *
 * Supported platforms: YouTube, TikTok, Instagram, Twitter/X, and 1000+ more
 */
class YtDlpDownloader(private val context: Context) {

    private val TAG = "CHUKA_NEW_YtDlpDownloader"

    @Volatile
    private var isInitialized = false

    @Volatile
    private var initializationFailed = false

    @Volatile
    private var initializationError: String? = null

    // Track active download sessions for cancellation support
    private val activeSessions = mutableMapOf<String, String>()
    private val sessionsLock = Any()

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
     * Check if URL is a playlist or contains multiple videos
     * Works for YouTube playlists, Instagram carousels, etc.
     */
    suspend fun isPlaylist(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()

            // For Instagram, always treat as potential multi-post
            // yt-dlp will handle single vs multi internally
            if (url.contains("instagram.com")) {
                Log.d(TAG, "Instagram URL detected - checking for carousel/multi-post")

                val request = YoutubeDLRequest(url)
                request.addOption("--flat-playlist")
                request.addOption("--dump-json")
                // Don't skip download check for Instagram

                val response = YoutubeDL.getInstance().execute(request)

                // Count JSON entries - Instagram carousels will have multiple
                val jsonLines = response.out.lines().filter { it.trim().startsWith("{") }
                val jsonCount = jsonLines.size
                val isMultiPost = jsonCount > 1

                Log.d(TAG, "Instagram: Found $jsonCount item(s) - ${if (isMultiPost) "carousel/multi-post" else "single post"}")
                return@withContext isMultiPost
            }

            // For other platforms (YouTube, etc.)
            val request = YoutubeDLRequest(url)
            request.addOption("--flat-playlist")
            request.addOption("--dump-json")

            val response = YoutubeDL.getInstance().execute(request)

            // If there are multiple JSON objects, it's a playlist
            val jsonCount = response.out.lines().count { it.trim().startsWith("{") }
            val isPlaylist = jsonCount > 1

            Log.d(TAG, "URL $url is ${if (isPlaylist) "a playlist with $jsonCount videos" else "a single video"}")
            isPlaylist
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if URL is playlist", e)
            // For Instagram, default to allowing playlist mode to capture all videos
            url.contains("instagram.com")
        }
    }

    /**
     * Download video using yt-dlp with progress tracking (downloads full video with audio as MP4)
     * Uses video title as filename if available
     *
     * @param url The video URL to download
     * @param outputFileName Optional custom filename (without extension)
     * @param username Optional username for authentication
     * @param password Optional password for authentication
     * @param cookiesFromBrowser Optional browser to extract cookies from (e.g., "chrome", "firefox", "edge")
     * @param allowPlaylist Whether to download playlists (default: false, single video only)
     */
    fun downloadVideo(
        url: String,
        outputFileName: String? = null,
        username: String? = null,
        password: String? = null,
        cookiesFromBrowser: String? = null,
        allowPlaylist: Boolean = false
    ): Flow<DownloadProgress> = callbackFlow {
        trySend(DownloadProgress(0f, "Initializing yt-dlp..."))

        var videoMetadata: VideoInfo? = null
        val sessionId = java.util.UUID.randomUUID().toString()

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

            // Track download start time to identify new/updated files
            val downloadStartTime = System.currentTimeMillis()

            // Track existing files with their modification times
            val existingFilesMap = if (allowPlaylist) {
                downloadsDir.listFiles { file -> file.extension == "mp4" }
                    ?.associateWith { it.lastModified() } ?: emptyMap()
            } else {
                emptyMap()
            }
            Log.d(TAG, "Existing MP4 files before download: ${existingFilesMap.size}")

            // Use video title as filename, or fallback to provided name
            val fileTemplate = outputFileName ?: "%(title)s"
            val outputTemplate = File(downloadsDir, "$fileTemplate.%(ext)s").absolutePath

            trySend(DownloadProgress(0.1f, "Starting video download..."))

            // Create download request for video with audio (best quality MP4)
            val request = YoutubeDLRequest(url)
            request.addOption("-o", outputTemplate)
            // Download best video+audio, merge into MP4 if needed
            // This format selector ensures we get both video and audio
            request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
            // Merge into MP4 format
            request.addOption("--merge-output-format", "mp4")
            // Force overwrite existing files (allows re-downloading same video)
            request.addOption("--force-overwrites")

            // Playlist handling
            if (allowPlaylist) {
                request.addOption("--yes-playlist")
                Log.d(TAG, "Playlist download enabled")
            } else {
                request.addOption("--no-playlist")
            }

            request.addOption("--socket-timeout", "30")
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

            Log.d(TAG, "Starting video download for: $url")
            Log.d(TAG, "Output template: $outputTemplate")

            // Register session for cancellation support
            synchronized(sessionsLock) {
                activeSessions[sessionId] = url
            }

            // Execute download with progress tracking
            val response = YoutubeDL.getInstance().execute(request) { progress, _, line ->
                //Log.d(TAG, "Video progress: $progress% - $line")

                // yt-dlp reports -1 when it doesn't know total size yet
                val actualProgress = if (progress < 0) 0f else progress
                val normalizedProgress = 0.1f + (actualProgress / 100f * 0.9f)
                val statusMessage = if (progress < 0) {
                    "Downloading video..."
                } else {
                    "Downloading video... $progress%"
                }
                trySend(DownloadProgress(normalizedProgress, statusMessage))
            }

            // Unregister session after completion
            synchronized(sessionsLock) {
                activeSessions.remove(sessionId)
            }

            Log.d(TAG, "Download completed. Exit code: ${response.exitCode}")

            if (response.exitCode == 0) {
                if (allowPlaylist) {
                    // For playlists, find files that are NEW or were UPDATED during download
                    val allMp4Files = downloadsDir.listFiles { file ->
                        file.extension == "mp4"
                    } ?: emptyArray()

                    // Find files that are either:
                    // 1. New (didn't exist before), OR
                    // 2. Were modified/updated after download started (re-downloaded/replaced)
                    val downloadedFiles = allMp4Files.filter { file ->
                        val wasNew = !existingFilesMap.containsKey(file)
                        val wasUpdated = existingFilesMap[file]?.let { oldModTime ->
                            file.lastModified() > oldModTime
                        } ?: false
                        val wasDownloadedNow = file.lastModified() >= downloadStartTime - 1000 // 1s buffer

                        wasNew || wasUpdated || wasDownloadedNow
                    }.sortedByDescending { it.lastModified() }

                    Log.d(TAG, "Found ${downloadedFiles.size} downloaded/updated video(s) (total MP4s: ${allMp4Files.size}, previously existing: ${existingFilesMap.size})")

                    if (downloadedFiles.isNotEmpty()) {
                        Log.d(TAG, "Downloaded ${downloadedFiles.size} videos from playlist")

                        // Create VideoItem for each downloaded file with thumbnail extraction
                        val videoItems = downloadedFiles.map { file ->
                            // Extract title from filename (remove extension)
                            val title = file.nameWithoutExtension

                            // Extract thumbnail from video file
                            val thumbnailPath = extractThumbnailFromVideo(file)

                            com.chuka.jamesmusicconverter.domain.model.VideoItem(
                                title = title,
                                fileName = file.name,
                                fileSize = file.length(),
                                filePath = file.absolutePath,
                                thumbnailUrl = thumbnailPath,  // Local file path to extracted thumbnail
                                durationMillis = 0L
                            )
                        }

                        // Return primary video info with all videos
                        val downloadedFile = downloadedFiles.first()
                        val fileSizeMB = downloadedFile.length() / (1024 * 1024)
                        Log.d(TAG, "Primary video: ${downloadedFile.absolutePath} ($fileSizeMB MB)")
                        trySend(DownloadProgress(
                            1f,
                            "Downloaded ${downloadedFiles.size} video${if (downloadedFiles.size > 1) "s" else ""}",
                            downloadedFile.absolutePath,
                            metadata = videoMetadata,
                            downloadedFiles = videoItems
                        ))
                    } else {
                        Log.e(TAG, "Playlist download completed but no files found")
                        close(Exception("Download completed but no video files were found in the output directory."))
                    }
                } else {
                    // Single video download
                    val downloadedFile = if (outputFileName != null) {
                        findDownloadedFile(downloadsDir, outputFileName)
                    } else {
                        // Find most recently downloaded MP4 file
                        downloadsDir.listFiles { file -> file.extension == "mp4" }?.maxByOrNull { it.lastModified() }
                    }

                    if (downloadedFile != null && downloadedFile.exists()) {
                        val fileSizeMB = downloadedFile.length() / (1024 * 1024)
                        Log.d(TAG, "Downloaded video: ${downloadedFile.absolutePath} ($fileSizeMB MB)")
                        trySend(DownloadProgress(
                            1f,
                            "Video ready",
                            downloadedFile.absolutePath,
                            metadata = videoMetadata
                        ))
                    } else {
                        Log.e(TAG, "Download completed but file not found in ${downloadsDir.absolutePath}")
                        close(Exception("Video download completed but file not found"))
                    }
                }
            } else {
                val errorMsg = response.err ?: "Unknown error"
                Log.e(TAG, "Download failed: $errorMsg")
                Log.e(TAG, "Full error output: ${response.out}")
                close(Exception("Video download failed: $errorMsg"))
            }

            close()

        } catch (e: Exception) {
            Log.e(TAG, "Video download failed", e)
            close(Exception("Video download failed: ${e.message}"))
        } finally {
            // Always unregister session on completion/error/cancellation
            synchronized(sessionsLock) {
                activeSessions.remove(sessionId)
            }
        }

        awaitClose {
            Log.d(TAG, "Video download flow closed for session: $sessionId")
            // Mark session as cancelled (cleanup)
            synchronized(sessionsLock) {
                val wasCancelled = activeSessions.remove(sessionId) != null
                if (wasCancelled) {
                    Log.d(TAG, "Marked video download session as cancelled: $sessionId")
                }
            }
        }
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
        val sessionId = java.util.UUID.randomUUID().toString()

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
            // Force overwrite existing files (allows re-downloading same audio)
            request.addOption("--force-overwrites")
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

            // Register session for cancellation support
            synchronized(sessionsLock) {
                activeSessions[sessionId] = url
            }

            // Execute download with progress tracking
            val response = YoutubeDL.getInstance().execute(request) { progress, _, line ->
                //Log.d(TAG, "Audio progress: $progress% - $line")

                // yt-dlp reports -1 when it doesn't know total size yet
                val actualProgress = if (progress < 0) 0f else progress
                val normalizedProgress = 0.1f + (actualProgress / 100f * 0.9f)
                val statusMessage = if (progress < 0) {
                    "Downloading audio..."
                } else {
                    "Downloading audio... $progress%"
                }
                trySend(DownloadProgress(normalizedProgress, statusMessage))
            }

            // Unregister session after completion
            synchronized(sessionsLock) {
                activeSessions.remove(sessionId)
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
        } finally {
            // Always unregister session on completion/error/cancellation
            synchronized(sessionsLock) {
                activeSessions.remove(sessionId)
            }
        }

        awaitClose {
            Log.d(TAG, "Audio download flow closed for session: $sessionId")
            // Mark session as cancelled (cleanup)
            synchronized(sessionsLock) {
                val wasCancelled = activeSessions.remove(sessionId) != null
                if (wasCancelled) {
                    Log.d(TAG, "Marked download session as cancelled: $sessionId")
                }
            }
            // Note: YoutubeDL doesn't provide direct process cancellation.
            // The download may continue in background, but results won't be processed.
        }
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
     * Extract thumbnail from video file
     * Returns the path to the saved thumbnail image, or null if extraction fails
     */
    private fun extractThumbnailFromVideo(videoFile: File): String? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)

            // Extract frame at 1 second (or first frame if video is shorter)
            val bitmap = retriever.getFrameAtTime(1_000_000) // 1 second in microseconds
            retriever.release()

            if (bitmap != null) {
                // Create thumbnails directory
                val thumbnailsDir = File(videoFile.parent, ".thumbnails")
                thumbnailsDir.mkdirs()

                // Save thumbnail as JPEG
                val thumbnailFile = File(thumbnailsDir, "${videoFile.nameWithoutExtension}_thumb.jpg")
                FileOutputStream(thumbnailFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()

                Log.d(TAG, "Extracted thumbnail for ${videoFile.name}: ${thumbnailFile.absolutePath}")
                thumbnailFile.absolutePath
            } else {
                Log.w(TAG, "Failed to extract thumbnail bitmap for ${videoFile.name}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting thumbnail from ${videoFile.name}", e)
            null
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

    /**
     * Get list of active download URLs
     */
    fun getActiveDownloads(): List<String> {
        synchronized(sessionsLock) {
            return activeSessions.values.toList()
        }
    }

    /**
     * Cancel all active downloads
     * Note: Due to YoutubeDL library limitations, this marks sessions as cancelled
     * but the underlying process may continue. New results won't be processed.
     */
    fun cancelAllDownloads() {
        synchronized(sessionsLock) {
            val count = activeSessions.size
            activeSessions.clear()
            Log.d(TAG, "Marked $count download session(s) as cancelled")
        }
    }

    /**
     * Check if there are any active downloads
     */
    fun hasActiveDownloads(): Boolean {
        synchronized(sessionsLock) {
            return activeSessions.isNotEmpty()
        }
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
