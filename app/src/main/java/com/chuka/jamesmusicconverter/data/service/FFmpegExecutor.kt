package com.chuka.jamesmusicconverter.data.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Executor for FFmpeg commands using bundled binary
 *
 * This implementation invokes FFmpeg as an external process, similar to Jaffree
 * but adapted for Android. The FFmpeg binary needs to be:
 * 1. Built for Android architectures (arm64-v8a, armeabi-v7a, x86, x86_64)
 * 2. Placed in src/main/jniLibs/{arch}/libffmpeg.so
 * 3. Or downloaded on first run from a CDN/server
 *
 * Alternative: Use MediaCodec API for native Android support without external binaries
 */
class FFmpegExecutor(private val context: Context) {

    private val TAG = "FFmpegExecutor"

    /**
     * Execute FFmpeg command
     * @param command Array of FFmpeg arguments (without the 'ffmpeg' prefix)
     * @param onProgress Callback for progress updates
     * @return Exit code (0 = success)
     */
    suspend fun execute(
        command: Array<String>,
        onProgress: ((String) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        try {
            val ffmpegPath = getFFmpegPath()

            if (ffmpegPath == null) {
                Log.e(TAG, "FFmpeg binary not found")
                return@withContext -1
            }

            // Build the full command
            val fullCommand = mutableListOf(ffmpegPath).apply {
                addAll(command)
            }

            Log.d(TAG, "Executing: ${fullCommand.joinToString(" ")}")

            val processBuilder = ProcessBuilder(fullCommand)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()

            // Read output for progress/errors
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.use {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { output ->
                        Log.d(TAG, output)
                        onProgress?.invoke(output)
                    }
                }
            }

            // Wait for completion
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Log.d(TAG, "FFmpeg command completed successfully")
            } else {
                Log.e(TAG, "FFmpeg command failed with exit code: $exitCode")
            }

            exitCode
        } catch (e: Exception) {
            Log.e(TAG, "Error executing FFmpeg", e)
            -1
        }
    }

    /**
     * Get the path to the FFmpeg binary
     *
     * Implementation options:
     * 1. Extract from assets on first run
     * 2. Load from jniLibs (if packaged as .so)
     * 3. Download from server on demand
     * 4. Use system FFmpeg if available (rare on Android)
     */
    private fun getFFmpegPath(): String? {
        // Option 1: Check if already extracted
        val extractedPath = File(context.filesDir, "ffmpeg").absolutePath
        if (File(extractedPath).exists() && File(extractedPath).canExecute()) {
            return extractedPath
        }

        // Option 2: Try to extract from assets
        try {
            val assetManager = context.assets
            val ffmpegAsset = "ffmpeg" // Should be in src/main/assets/ffmpeg

            assetManager.open(ffmpegAsset).use { input ->
                File(extractedPath).outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Make executable
            File(extractedPath).setExecutable(true)

            Log.d(TAG, "FFmpeg extracted to: $extractedPath")
            return extractedPath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract FFmpeg from assets", e)
        }

        // Option 3: Load from jniLibs (if packaged as libffmpeg.so)
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val libFFmpegPath = File(nativeLibDir, "libffmpeg.so")
        if (libFFmpegPath.exists()) {
            return libFFmpegPath.absolutePath
        }

        Log.w(TAG, "FFmpeg binary not found. Please bundle FFmpeg or use MediaCodec alternative.")
        return null
    }

    /**
     * Check if FFmpeg is available
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        getFFmpegPath() != null
    }

    /**
     * Get FFmpeg version info
     */
    suspend fun getVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val ffmpegPath = getFFmpegPath() ?: return@withContext null

            val process = ProcessBuilder(ffmpegPath, "-version").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            reader.use {
                it.readLine() // First line contains version
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FFmpeg version", e)
            null
        }
    }
}
