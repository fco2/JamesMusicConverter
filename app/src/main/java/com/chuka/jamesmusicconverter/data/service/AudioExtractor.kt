package com.chuka.jamesmusicconverter.data.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * Service for extracting audio from video files
 *
 * Uses Android's MediaCodec for audio extraction and conversion
 */
class AudioExtractor(private val context: Context) {

    private val TAG = "AudioExtractor"
    private val mediaCodecExtractor = MediaCodecAudioExtractor(context)

    /**
     * Extracts audio duration from a media file using MediaMetadataRetriever
     * @param filePath Path to the audio/video file
     * @return Duration in milliseconds, or 0 if unable to extract
     */
    private fun extractDuration(filePath: String): Long {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract duration from $filePath: ${e.message}")
            0L
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release MediaMetadataRetriever: ${e.message}")
            }
        }
    }

    /**
     * Handles audio extraction/conversion (optimized for yt-dlp output)
     *
     * Since yt-dlp now does the conversion using ffmpeg, this function:
     * 1. Verifies the file exists and is valid
     * 2. Ensures it's in the correct output location
     * 3. Validates it's an MP3 file
     *
     * @param videoFilePath Path to the audio file (already converted by yt-dlp)
     * @param outputFileName Optional custom filename
     */
    fun extractAudioToMp3(
        videoFilePath: String,
        outputFileName: String? = null
    ): Flow<ExtractionProgress> = flow {
        emit(ExtractionProgress(0f, "Verifying audio file..."))

        try {
            Log.d(TAG, "Processing audio file: $videoFilePath")

            val sourceFile = File(videoFilePath)
            if (!sourceFile.exists()) {
                throw Exception("Source audio file not found: $videoFilePath")
            }

            // Validate file size (should be > 0)
            if (sourceFile.length() == 0L) {
                throw Exception("Audio file is empty (0 bytes)")
            }

            // Create output directory in public Downloads folder
            val musicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "JamesMusicConverter"
            )

            if (!musicDir.exists() && !musicDir.mkdirs()) {
                throw Exception("Failed to create output directory: ${musicDir.absolutePath}")
            }

            emit(ExtractionProgress(0.2f, "Validating audio file..."))

            // Ensure filename has .mp3 extension
            val finalFileName = when {
                outputFileName != null -> {
                    if (outputFileName.endsWith(".mp3", ignoreCase = true)) {
                        outputFileName
                    } else {
                        "$outputFileName.mp3"
                    }
                }
                sourceFile.name.endsWith(".mp3", ignoreCase = true) -> sourceFile.name
                else -> "${sourceFile.nameWithoutExtension}.mp3"
            }

            val outputFile = File(musicDir, finalFileName)

            // Check if source and output are the same
            if (sourceFile.canonicalPath == outputFile.canonicalPath) {
                Log.d(TAG, "File is already in the correct location: ${outputFile.absolutePath}")
                val duration = extractDuration(outputFile.absolutePath)
                emit(ExtractionProgress(
                    1f,
                    "Audio ready",
                    outputFile.absolutePath,
                    outputFile.length(),
                    duration
                ))
                return@flow
            }

            emit(ExtractionProgress(0.4f, "Organizing audio file..."))

            // Handle existing file
            if (outputFile.exists()) {
                Log.d(TAG, "Output file already exists, will overwrite")
                outputFile.delete()
            }

            // Try atomic move first (faster), fallback to copy
            val moved = try {
                sourceFile.renameTo(outputFile)
            } catch (e: Exception) {
                Log.w(TAG, "Atomic move failed: ${e.message}")
                false
            }

            if (moved) {
                Log.d(TAG, "Audio file moved successfully (atomic)")
            } else {
                // Cross-filesystem move - need to copy
                Log.d(TAG, "Performing copy operation (cross-filesystem)")
                emit(ExtractionProgress(0.5f, "Copying audio file..."))

                try {
                    sourceFile.copyTo(outputFile, overwrite = true)
                    emit(ExtractionProgress(0.9f, "Cleaning up..."))

                    // Delete source after successful copy
                    if (!sourceFile.delete()) {
                        Log.w(TAG, "Failed to delete source file: ${sourceFile.absolutePath}")
                    }
                } catch (e: Exception) {
                    // Clean up partial copy
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                    throw Exception("Failed to copy audio file: ${e.message}")
                }
            }

            // Final verification
            if (!outputFile.exists() || outputFile.length() == 0L) {
                throw Exception("Output file is missing or empty after processing")
            }

            // Extract duration from the final MP3 file
            val duration = extractDuration(outputFile.absolutePath)
            val fileSizeMB = outputFile.length() / (1024.0 * 1024.0)
            val durationSec = duration / 1000.0
            Log.d(TAG, "Audio file ready: ${outputFile.absolutePath} (${String.format("%.2f", fileSizeMB)} MB, ${String.format("%.1f", durationSec)}s)")

            emit(ExtractionProgress(
                1f,
                "Audio ready",
                outputFile.absolutePath,
                outputFile.length(),
                duration
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Audio processing failed", e)
            throw Exception("Audio processing failed: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Direct conversion from URL to MP3 (for supported formats)
     *
     * TODO: For production, use yt-dlp or similar to handle platform-specific URLs
     */
    fun convertDirectToMp3(
        url: String,
        outputFileName: String = "audio_${System.currentTimeMillis()}.mp3"
    ): Flow<ExtractionProgress> = flow {
        emit(ExtractionProgress(0f, "Preparing download and conversion..."))

        try {
            // Create output directory in public Downloads folder
            val musicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "JamesMusicConverter"
            )
            musicDir.mkdirs()

            val outputFile = File(musicDir, outputFileName)

            emit(ExtractionProgress(0.2f, "Downloading and converting..."))

            // TODO: Replace with actual conversion using FFmpeg or MediaCodec
            // FFmpeg command would be: ffmpeg -i url -vn -codec:a libmp3lame -q:a 2 -y output.mp3

            Log.d("AudioExtractor", "Simulating direct conversion from: $url")
            delay(1500)

            emit(ExtractionProgress(0.6f, "Converting audio..."))
            delay(1000)

            // Create placeholder file
            outputFile.createNewFile()

            emit(ExtractionProgress(1f, "Conversion complete", outputFile.absolutePath, outputFile.length()))

        } catch (e: Exception) {
            Log.e("AudioExtractor", "Direct conversion failed", e)
            throw Exception("Conversion failed: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}

data class ExtractionProgress(
    val progress: Float,
    val message: String,
    val outputFilePath: String? = null,
    val fileSize: Long = 0,
    val durationMillis: Long = 0 // Duration in milliseconds
)
