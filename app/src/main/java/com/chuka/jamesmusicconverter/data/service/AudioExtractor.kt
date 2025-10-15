package com.chuka.jamesmusicconverter.data.service

import android.content.Context
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
     * Handles audio extraction/conversion
     *
     * Since yt-dlp now does the conversion using ffmpeg, this just verifies
     * the file exists and moves it to the final location if needed.
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

            // Create output directory
            val musicDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "JamesMusicConverter"
            )
            musicDir.mkdirs()

            // Use the original filename or provided name
            val finalFileName = outputFileName ?: sourceFile.name
            val outputFile = File(musicDir, finalFileName)

            emit(ExtractionProgress(0.3f, "Moving audio file..."))

            // Move/copy file to music directory
            if (sourceFile.renameTo(outputFile)) {
                Log.d(TAG, "Audio file moved successfully")
            } else {
                // If rename fails, try copy
                Log.d(TAG, "Rename failed, copying file instead")
                sourceFile.copyTo(outputFile, overwrite = true)
                sourceFile.delete()
            }

            val fileSizeMB = outputFile.length() / (1024 * 1024)
            Log.d(TAG, "Audio file ready: ${outputFile.absolutePath} ($fileSizeMB MB)")

            emit(ExtractionProgress(
                1f,
                "Audio ready",
                outputFile.absolutePath,
                outputFile.length()
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
            val musicDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
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
    val fileSize: Long = 0
)
