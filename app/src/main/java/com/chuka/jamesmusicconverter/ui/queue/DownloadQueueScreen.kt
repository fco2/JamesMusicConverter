package com.chuka.jamesmusicconverter.ui.queue

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.chuka.jamesmusicconverter.domain.model.QueueItem
import com.chuka.jamesmusicconverter.domain.model.QueueItemStatus
import com.chuka.jamesmusicconverter.ui.components.SnackbarController
import com.chuka.jamesmusicconverter.ui.components.SnackbarViewModel
import java.io.File

/**
 * Main screen for viewing and managing the download queue
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadQueueViewModel = hiltViewModel(),
    snackbarViewModel: SnackbarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarController = snackbarViewModel.snackbarController
    var showCancelAllDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Cancel all confirmation dialog
    if (showCancelAllDialog) {
        AlertDialog(
            onDismissRequest = { showCancelAllDialog = false },
            title = { Text("Cancel All Downloads?") },
            text = { Text("This will cancel all pending and active downloads. Completed downloads will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelAll()
                        showCancelAllDialog = false
                    }
                ) {
                    Text("Cancel All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelAllDialog = false }) {
                    Text("Keep")
                }
            }
        )
    }

    // Clear finished confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Finished Downloads?") },
            text = { Text("This will remove all completed, failed, and cancelled items from the queue. Downloaded files will not be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearFinished()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Download Queue",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Cancel all button (only show if there are active items)
                    if (uiState.activeCount + uiState.pendingCount > 0) {
                        IconButton(onClick = { showCancelAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel All"
                            )
                        }
                    }

                    // Clear finished button (only show if there are finished items)
                    if (uiState.completedCount + uiState.failedCount > 0) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear Finished"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.queueItems.isEmpty()) {
            // Empty state
            EmptyQueueState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            // Queue items
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Stats row (simplified)
                QueueStatsRow(
                    activeCount = uiState.activeCount,
                    pendingCount = uiState.pendingCount,
                    completedCount = uiState.completedCount,
                    failedCount = uiState.failedCount
                )

                // Queue items list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.queueItems,
                        key = { it.id }
                    ) { item ->
                        QueueItemCard(
                            item = item,
                            onCancel = { viewModel.cancelItem(item) },
                            onRetry = { viewModel.retryItem(item) },
                            onRemove = { viewModel.removeItem(item) },
                            onPlay = { playFile(context, item, snackbarController) }
                        )
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Minimal stats row showing queue summary
 */
@Composable
private fun QueueStatsRow(
    activeCount: Int,
    pendingCount: Int,
    completedCount: Int,
    failedCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Active count
        if (activeCount + pendingCount > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${activeCount + pendingCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Completed count
        if (completedCount > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                )
                Text(
                    text = "done",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Failed count
        if (failedCount > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$failedCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Empty state when no items in queue
 */
@Composable
private fun EmptyQueueState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Queue,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No downloads in queue",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add videos from the home screen\nto start downloading",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Play a downloaded file
 */
private fun playFile(
    context: android.content.Context,
    item: QueueItem,
    snackbarController: SnackbarController
) {
    if (item.filePath == null) {
        snackbarController.showMessage("File not found")
        return
    }

    val file = File(item.filePath)
    if (!file.exists()) {
        snackbarController.showMessage("File no longer exists")
        return
    }

    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mimeType = when {
            item.filePath.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            item.filePath.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            item.filePath.endsWith(".m4a", ignoreCase = true) -> "audio/m4a"
            item.filePath.endsWith(".webm", ignoreCase = true) -> "video/webm"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        snackbarController.showMessage("Unable to open file")
    }
}
