package com.chuka.jamesmusicconverter.ui.queue

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.chuka.jamesmusicconverter.ui.theme.StatusSuccess
import com.chuka.jamesmusicconverter.ui.theme.StatusSuccessDark
import java.io.File

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
                ) { Text("Cancel All") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelAllDialog = false }) { Text("Keep") }
            }
        )
    }

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
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
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
                    if (uiState.activeCount + uiState.pendingCount > 0) {
                        IconButton(onClick = { showCancelAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel All"
                            )
                        }
                    }
                    if (uiState.completedCount + uiState.failedCount > 0) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear Finished"
                            )
                        }
                    }
                },
                colors = if (isSystemInDarkTheme()) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.queueItems.isEmpty()) {
            EmptyQueueState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Compact chip-style stats bar
                QueueStatsBar(
                    activeCount = uiState.activeCount,
                    pendingCount = uiState.pendingCount,
                    completedCount = uiState.completedCount,
                    failedCount = uiState.failedCount
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * Compact stats bar using small chips instead of large numbers
 */
@Composable
private fun QueueStatsBar(
    activeCount: Int,
    pendingCount: Int,
    completedCount: Int,
    failedCount: Int,
    modifier: Modifier = Modifier
) {
    val totalActive = activeCount + pendingCount
    val hasAny = totalActive > 0 || completedCount > 0 || failedCount > 0
    if (!hasAny) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (totalActive > 0) {
            StatChip(
                icon = Icons.Default.Download,
                label = "$totalActive active",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        if (completedCount > 0) {
            val successColor = if (isSystemInDarkTheme()) StatusSuccessDark else StatusSuccess
            StatChip(
                icon = Icons.Default.CheckCircle,
                label = "$completedCount done",
                containerColor = successColor.copy(alpha = 0.15f),
                contentColor = successColor
            )
        }

        if (failedCount > 0) {
            StatChip(
                icon = Icons.Default.Error,
                label = "$failedCount failed",
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

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
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "No downloads in queue",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Add videos from the home screen\nto start downloading",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

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
