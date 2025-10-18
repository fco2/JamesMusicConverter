package com.chuka.jamesmusicconverter.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import androidx.core.content.FileProvider
import com.chuka.jamesmusicconverter.ui.components.SnackbarController
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles file-related actions (play, share, open location) with proper context management.
 * Injected with ApplicationContext to avoid lifecycle issues.
 */
@Singleton
class FileActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val snackbarController: SnackbarController
) {

    /**
     * Play the MP3 file using the default music player
     */
    fun playFile(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                showToast("File not found: $filePath")
                return
            }

            val uri = getFileUri(file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "audio/mpeg")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Play MP3").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            showToast("Error opening file: ${e.message}")
        }
    }

    /**
     * Open the file location in file manager
     */
    fun openFileLocation(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                showToast("File not found: $filePath")
                return
            }

            val uri = getFileUri(file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback: show file path
                showToast("File location: ${file.parent}", 1) // 1 = LENGTH_LONG
            }
        } catch (e: Exception) {
            showToast("Error: ${e.message}")
        }
    }

    /**
     * Handles file download notification
     */
    fun showDownloadLocation(fileName: String) {
        try {
            showToast(
                "File saved to: Music/JamesMusicConverter/$fileName",
                1 // 1 = LENGTH_LONG
            )
        } catch (e: Exception) {
            showToast("Error: ${e.message}")
        }
    }

    /**
     * Handles file sharing
     */
    fun shareFile(filePath: String, fileName: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                showToast("File not found")
                return
            }

            val uri = getFileUri(file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Check out this MP3")
                putExtra(Intent.EXTRA_TEXT, "Sharing: $fileName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share MP3").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            showToast("Error sharing file: ${e.message}")
        }
    }

    /**
     * Gets a content URI for a file using FileProvider
     */
    private fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Shows a snackbar message
     */
    private fun showToast(message: String, duration: Int = 0) {
        val snackbarDuration = if (duration == 1) { // Toast.LENGTH_LONG
            SnackbarDuration.Long
        } else {
            SnackbarDuration.Short
        }
        snackbarController.showMessage(message, snackbarDuration)
    }
}
