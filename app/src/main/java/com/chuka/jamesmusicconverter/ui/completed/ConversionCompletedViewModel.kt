package com.chuka.jamesmusicconverter.ui.completed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chuka.jamesmusicconverter.domain.FileActionHandler
import com.chuka.jamesmusicconverter.domain.model.ConversionResult
import com.chuka.jamesmusicconverter.domain.repository.ConversionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val PLAY_DEBOUNCE_DELAY = 1000L // 1 second
/**
 * ViewModel for the Conversion Completed screen.
 * Context-independent - delegates all context-dependent operations to FileActionHandler.
 */
@HiltViewModel
class ConversionCompletedViewModel @Inject constructor(
    private val fileActionHandler: FileActionHandler,
    private val repository: ConversionRepository
) : ViewModel() {

    // Debounce mechanism to prevent rapid successive play calls
    private var lastPlayTime = 0L

    // Full conversion result with all videos (for playlists)
    private val _conversionResult = MutableStateFlow<ConversionResult?>(null)
    val conversionResult: StateFlow<ConversionResult?> = _conversionResult.asStateFlow()

    /**
     * Load the full conversion result from repository
     */
    fun loadResult(videoUrl: String) {
        if (videoUrl.isNotBlank()) {
            viewModelScope.launch {
                val result = repository.getConversionResult(videoUrl)
                result.fold(
                    onSuccess = { conversionResult ->
                        _conversionResult.value = conversionResult
                        android.util.Log.d("CHUKA_Completed", "Loaded result with ${conversionResult.getVideoCount()} videos")
                    },
                    onFailure = { exception ->
                        android.util.Log.e("CHUKA_Completed", "Failed to load result", exception)
                    }
                )
            }
        }
    }

    /**
     * Play the media file using the default player (video or audio)
     * Includes debounce to prevent flickering from rapid clicks
     */
    fun playFile(filePath: String, isVideo: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPlayTime > PLAY_DEBOUNCE_DELAY) {
            lastPlayTime = currentTime
            fileActionHandler.playFile(filePath, isVideo)
        }
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
