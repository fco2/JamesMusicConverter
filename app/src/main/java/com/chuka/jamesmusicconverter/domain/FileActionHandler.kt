package com.chuka.jamesmusicconverter.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
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

    private var lastIntentTime = 0L
    private val INTENT_DEBOUNCE_DELAY = 500L // 500ms to prevent overlapping intents

    /**
     * Play the media file using the default player (video or audio)
     */
    fun playFile(filePath: String, isVideo: Boolean = false) {
        // Additional debounce at the handler level
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastIntentTime < INTENT_DEBOUNCE_DELAY) {
            android.util.Log.d("CHUKA_VIDEO", "Ignoring rapid play request (debounced)")
            return
        }
        lastIntentTime = currentTime

        try {
            val file = File(filePath)
            if (!file.exists()) {
                showToast("File not found: $filePath")
                return
            }

            val uri = getFileUri(file)
            val mimeType = if (isVideo) "video/*" else "audio/mpeg"

            android.util.Log.d("CHUKA_VIDEO", "Playing ${if (isVideo) "video" else "audio"}: $filePath")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Don't use SINGLE_TOP or CLEAR_TOP - they can cause issues with video players
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("CHUKA_VIDEO", "Error opening file", e)
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

            val parentDir = file.parentFile
            if (parentDir == null || !parentDir.exists()) {
                showToast("Directory not found")
                return
            }

            android.util.Log.d("FileActionHandler", "Opening folder: ${parentDir.absolutePath}")

            var success = false

            // Approach 1: Use DocumentsContract for Android 10+ (API 29+)
            // This is the most reliable way on modern Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    // Build the proper DocumentsContract URI for the JamesMusicConverter folder
                    val treeUri = DocumentsContract.buildTreeDocumentUri(
                        "com.android.externalstorage.documents",
                        "primary:Download/JamesMusicConverter"
                    )

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(treeUri, DocumentsContract.Document.MIME_TYPE_DIR)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                    success = true
                    android.util.Log.d("FileActionHandler", "Opened folder using DocumentsContract")
                } catch (e: Exception) {
                    android.util.Log.w("FileActionHandler", "DocumentsContract approach failed: ${e.message}")
                }
            }

            // Approach 2: Use DocumentsUI to open the specific folder
            // This works on most Android devices with the built-in file manager
            if (!success) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        // Use the DocumentsUI to show the Downloads folder
                        setDataAndType(
                            Uri.parse("content://com.android.externalstorage.documents/document/primary:Download%2FJamesMusicConverter"),
                            "vnd.android.document/directory"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                    success = true
                    android.util.Log.d("FileActionHandler", "Opened folder using DocumentsUI")
                } catch (e: Exception) {
                    android.util.Log.w("FileActionHandler", "DocumentsUI approach failed: ${e.message}")
                }
            }

            // Approach 3: Try opening Files app with tree URI
            if (!success) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse("content://com.android.externalstorage.documents/tree/primary:Download%2FJamesMusicConverter"),
                            "vnd.android.document/directory")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                    success = true
                    android.util.Log.d("FileActionHandler", "Opened folder using Files app")
                } catch (e: Exception) {
                    android.util.Log.w("FileActionHandler", "Files app approach failed: ${e.message}")
                }
            }

            // Approach 4: Open parent Downloads folder (fallback)
            if (!success) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(
                            Uri.parse("content://com.android.externalstorage.documents/document/primary:Download"),
                            "vnd.android.document/directory"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    showToast("Look for 'JamesMusicConverter' folder")
                    success = true
                    android.util.Log.d("FileActionHandler", "Opened Downloads folder (parent)")
                } catch (e: Exception) {
                    android.util.Log.w("FileActionHandler", "Downloads folder approach failed: ${e.message}")
                }
            }

            // Approach 5: Try with file:// URI (works on older Android)
            if (!success) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.fromFile(parentDir), "resource/folder")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    success = true
                } catch (e: Exception) {
                    android.util.Log.w("FileActionHandler", "File URI approach failed: ${e.message}")
                }
            }

            // Fallback: Show helpful message with exact path
            if (!success) {
                val readablePath = "Download/JamesMusicConverter"
                showToast("Files saved in: $readablePath\n\nOpen your file manager and navigate to this folder.", 1)
                android.util.Log.w("FileActionHandler", "All approaches failed, showing path to user")
            }

        } catch (e: Exception) {
            android.util.Log.e("FileActionHandler", "Error opening file location", e)
            showToast("Error opening folder. Files are in Download/JamesMusicConverter", 1)
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

            // Detect file type from extension
            val isVideo = fileName.endsWith(".mp4", ignoreCase = true) ||
                         fileName.endsWith(".webm", ignoreCase = true)
            val mimeType = if (isVideo) "video/*" else "audio/mpeg"
            val mediaType = if (isVideo) "video" else "MP3"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Check out this $mediaType")
                putExtra(Intent.EXTRA_TEXT, "Sharing: $fileName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share $mediaType").apply {
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
