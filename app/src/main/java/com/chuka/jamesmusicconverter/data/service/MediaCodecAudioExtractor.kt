package com.chuka.jamesmusicconverter.data.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.nio.ByteBuffer

/**
 * Audio extractor using Android's native MediaCodec API
 *
 * This is a pure Android solution that doesn't require FFmpeg binaries.
 * It can extract audio from video files and remux to supported formats.
 *
 * Pros:
 * - No external dependencies
 * - Native Android support
 * - Hardware acceleration available
 *
 * Cons:
 * - Limited to formats supported by device
 * - Cannot transcode to MP3 (need AAC, OGG, etc.)
 * - May vary by device/Android version
 */
class MediaCodecAudioExtractor(private val context: Context) {

    private val TAG = "MediaCodecExtractor"

    /**
     * Extract audio from video and save as AAC/M4A
     * (MP3 encoding is not natively supported by MediaCodec)
     */
    fun extractAudioToAAC(
        videoFilePath: String,
        outputFileName: String = "converted_${System.currentTimeMillis()}.m4a"
    ): Flow<ExtractionProgress> = flow {
        emit(ExtractionProgress(0f, "Preparing audio extraction..."))

        try {
            // Create output directory
            val musicDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "JamesMusicConverter"
            )
            musicDir.mkdirs()

            val outputFile = File(musicDir, outputFileName)

            emit(ExtractionProgress(0.1f, "Reading source file..."))

            val extractor = MediaExtractor()
            extractor.setDataSource(videoFilePath)

            // Find audio track
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)

                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw Exception("No audio track found in video")
            }

            // Check if the format is supported by MediaMuxer
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
            Log.d(TAG, "Audio format detected: $mime")

            // Opus and Vorbis cannot be directly muxed into MP4 container
            if (mime == "audio/opus" || mime == "audio/vorbis") {
                Log.w(TAG, "Unsupported format for direct remuxing: $mime")
                throw Exception(
                    "Audio format $mime is not supported for MP4 output. " +
                    "This video needs transcoding which is not yet implemented. " +
                    "Try a different video or format."
                )
            }

            emit(ExtractionProgress(0.2f, "Extracting audio track..."))

            extractor.selectTrack(audioTrackIndex)

            // Create muxer for output
            val muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            val muxerTrackIndex = try {
                muxer.addTrack(audioFormat)
            } catch (e: Exception) {
                muxer.release()
                extractor.release()
                throw Exception(
                    "Unsupported audio format ($mime) for MP4 container. " +
                    "This video uses a codec that cannot be directly converted. " +
                    "Original error: ${e.message}"
                )
            }
            muxer.start()

            // Extract and write samples
            val bufferInfo = MediaCodec.BufferInfo()
            val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer

            var sampleCount = 0
            val duration = audioFormat.getLong(MediaFormat.KEY_DURATION)

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)

                if (sampleSize < 0) {
                    // End of stream
                    break
                }

                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                bufferInfo.size = sampleSize
                bufferInfo.offset = 0

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

                extractor.advance()
                sampleCount++

                // Update progress based on timestamp
                if (duration > 0) {
                    val progress = 0.2f + (0.7f * (bufferInfo.presentationTimeUs.toFloat() / duration))
                    if (sampleCount % 100 == 0) { // Update every 100 samples
                        emit(ExtractionProgress(progress, "Extracting audio..."))
                    }
                }
            }

            emit(ExtractionProgress(0.9f, "Finalizing..."))

            // Clean up
            muxer.stop()
            muxer.release()
            extractor.release()

            // Delete temp video file
            try {
                File(videoFilePath).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete temp video file", e)
            }

            emit(ExtractionProgress(
                1f,
                "Extraction complete",
                outputFile.absolutePath,
                outputFile.length()
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Audio extraction failed", e)
            throw Exception("Audio extraction failed: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Check supported codecs on this device
     */
    fun getSupportedAudioFormats(): List<String> {
        val formats = mutableListOf<String>()

        try {
            // Common audio MIME types
            val mimeTypes = listOf(
                "audio/mp4a-latm", // AAC
                "audio/3gpp",      // AMR
                "audio/amr-wb",    // AMR-WB
                "audio/flac",      // FLAC
                "audio/vorbis",    // Vorbis
                "audio/opus",      // Opus
                "audio/raw"        // PCM
            )

            for (mime in mimeTypes) {
                try {
                    val codec = MediaCodec.createEncoderByType(mime)
                    formats.add(mime)
                    codec.release()
                } catch (e: Exception) {
                    // Codec not supported
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking supported formats", e)
        }

        return formats
    }

    /**
     * Transcode audio to AAC (most widely supported)
     * This requires decoding original audio and re-encoding to AAC
     */
    fun transcodeToAAC(
        inputFilePath: String,
        outputFileName: String = "transcoded_${System.currentTimeMillis()}.m4a",
        bitrate: Int = 192000 // 192 kbps
    ): Flow<ExtractionProgress> = flow {
        emit(ExtractionProgress(0f, "Preparing transcoding..."))

        try {
            val musicDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "JamesMusicConverter"
            )
            musicDir.mkdirs()

            val outputFile = File(musicDir, outputFileName)

            // TODO: Implement full transcode pipeline:
            // 1. MediaExtractor to read input
            // 2. MediaCodec decoder to decode original audio
            // 3. MediaCodec encoder to encode to AAC
            // 4. MediaMuxer to write output
            //
            // This is complex and requires managing codec buffers,
            // so for now we'll just copy the audio track

            emit(ExtractionProgress(0.5f, "Transcoding..."))

            // Simplified: just extract (same as above)
            extractAudioToAAC(inputFilePath, outputFileName).collect {
                emit(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transcoding failed", e)
            throw Exception("Transcoding failed: ${e.message}")
        }
    }
}
