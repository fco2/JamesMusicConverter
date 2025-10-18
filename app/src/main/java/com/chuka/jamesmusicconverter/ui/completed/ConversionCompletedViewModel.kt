package com.chuka.jamesmusicconverter.ui.completed

import androidx.lifecycle.ViewModel
import com.chuka.jamesmusicconverter.domain.FileActionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the Conversion Completed screen.
 * Context-independent - delegates all context-dependent operations to FileActionHandler.
 */
@HiltViewModel
class ConversionCompletedViewModel @Inject constructor(
    private val fileActionHandler: FileActionHandler
) : ViewModel() {

    /**
     * Play the MP3 file using the default music player
     */
    fun playFile(filePath: String) {
        fileActionHandler.playFile(filePath)
    }

    /**
     * Open the file location in file manager
     */
    fun openFileLocation(filePath: String) {
        fileActionHandler.openFileLocation(filePath)
    }

    /**
     * Handles file download notification
     */
    fun downloadFile(fileName: String) {
        fileActionHandler.showDownloadLocation(fileName)
    }

    /**
     * Handles file sharing
     */
    fun shareFile(filePath: String, fileName: String) {
        fileActionHandler.shareFile(filePath, fileName)
    }

    /**
     * Formats file size in human-readable format using device locale
     */
    fun formatFileSize(bytes: Long): String {
        val locale = Locale.getDefault()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(locale, "%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(locale, "%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(locale, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Formats duration in human-readable format (mm:ss or HH:mm:ss)
     */
    fun formatDuration(durationMillis: Long): String {
        if (durationMillis <= 0) return "Unknown"

        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }
}
