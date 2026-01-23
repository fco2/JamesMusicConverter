package com.chuka.jamesmusicconverter.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chuka.jamesmusicconverter.domain.model.QueueItem
import com.chuka.jamesmusicconverter.domain.model.QueueItemStatus
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import com.chuka.jamesmusicconverter.ui.components.CarouselIndicatorBadge
import java.io.File

/**
 * Card component displaying a single queue item with its status and actions
 */
@Composable
fun QueueItemCard(
    item: QueueItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail with status overlay and carousel badge
            ThumbnailWithStatus(
                thumbnailUrl = item.thumbnailUrl,
                localThumbnailPath = item.localThumbnailPath,
                status = item.status,
                progress = item.progress,
                downloadMode = item.downloadMode,
                carouselPositionText = item.carouselPositionText
            )

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Platform badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.platform,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Title
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Duration and mode info (show for all items when available)
                if (item.durationMillis > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Duration
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDuration(item.durationMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Mode indicator
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (item.downloadMode == DownloadMode.VIDEO) "Video" else "Audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status message
                Text(
                    text = item.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = getStatusColor(item.status),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Progress bar for active/pending items
                if (item.status == QueueItemStatus.ACTIVE || item.status == QueueItemStatus.PENDING) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    if (item.progress > 0) {
                        Text(
                            text = "${item.progressPercentage}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // File info for completed items (file size and name)
                if (item.status == QueueItemStatus.COMPLETED && item.filePath != null) {
                    // File size
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.downloadMode == DownloadMode.VIDEO)
                                Icons.Default.VideoFile
                            else
                                Icons.Default.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatFileSize(item.fileSize),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Error message for failed items
                if (item.status == QueueItemStatus.FAILED && item.errorMessage != null) {
                    Text(
                        text = item.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action buttons
                QueueItemActions(
                    item = item,
                    onCancel = onCancel,
                    onRetry = onRetry,
                    onRemove = onRemove,
                    onPlay = onPlay
                )
            }
        }
    }
}

/**
 * Thumbnail with status overlay and optional carousel badge
 */
@Composable
private fun ThumbnailWithStatus(
    thumbnailUrl: String?,
    localThumbnailPath: String?,
    status: QueueItemStatus,
    progress: Float,
    downloadMode: DownloadMode,
    carouselPositionText: String? = null
) {
    Box(
        modifier = Modifier.size(60.dp),  // Reduced from 80dp
        contentAlignment = Alignment.Center
    ) {
        // Thumbnail image
        val imageModel = when {
            localThumbnailPath != null && File(localThumbnailPath).exists() -> localThumbnailPath
            thumbnailUrl != null -> thumbnailUrl
            else -> null
        }

        if (imageModel != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageModel)
                    .crossfade(true)
                    .build(),
                contentDescription = "Thumbnail",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (downloadMode == DownloadMode.VIDEO)
                        Icons.Default.VideoLibrary
                    else
                        Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),  // Reduced from 32dp
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Status overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (status) {
                        QueueItemStatus.ACTIVE -> Color.Black.copy(alpha = 0.5f)
                        QueueItemStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.7f)
                        QueueItemStatus.FAILED -> Color(0xFFE53935).copy(alpha = 0.7f)
                        QueueItemStatus.CANCELLED -> Color(0xFFFF9800).copy(alpha = 0.7f)
                        QueueItemStatus.PENDING -> Color.Black.copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                QueueItemStatus.ACTIVE -> {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(28.dp),  // Reduced from 36dp
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
                QueueItemStatus.COMPLETED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)  // Reduced from 32dp
                    )
                }
                QueueItemStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)  // Reduced from 32dp
                    )
                }
                QueueItemStatus.CANCELLED -> {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Cancelled",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)  // Reduced from 32dp
                    )
                }
                QueueItemStatus.PENDING -> {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Pending",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)  // Reduced from 28dp
                    )
                }
            }
        }

        // Carousel position badge (top-right corner)
        if (carouselPositionText != null) {
            CarouselIndicatorBadge(
                position = carouselPositionText.substringBefore("/").toIntOrNull(),
                total = carouselPositionText.substringAfter("/").toIntOrNull(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            )
        }
    }
}

/**
 * Action buttons based on item status
 */
@Composable
private fun QueueItemActions(
    item: QueueItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        when (item.status) {
            QueueItemStatus.PENDING, QueueItemStatus.ACTIVE -> {
                // Cancel button
                TextButton(
                    onClick = onCancel,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel", style = MaterialTheme.typography.labelMedium)
                }
            }

            QueueItemStatus.COMPLETED -> {
                // Play button
                if (item.filePath != null) {
                    TextButton(
                        onClick = onPlay,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Remove button
                TextButton(
                    onClick = onRemove,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove", style = MaterialTheme.typography.labelMedium)
                }
            }

            QueueItemStatus.FAILED, QueueItemStatus.CANCELLED -> {
                // Retry button
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry", style = MaterialTheme.typography.labelMedium)
                }

                // Remove button
                TextButton(
                    onClick = onRemove,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Get color for status text
 */
@Composable
private fun getStatusColor(status: QueueItemStatus): Color {
    return when (status) {
        QueueItemStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        QueueItemStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        QueueItemStatus.COMPLETED -> Color(0xFF4CAF50)
        QueueItemStatus.FAILED -> MaterialTheme.colorScheme.error
        QueueItemStatus.CANCELLED -> Color(0xFFFF9800)
    }
}

/**
 * Format file size to human-readable string
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * Format duration to human-readable string (mm:ss or HH:mm:ss)
 */
private fun formatDuration(durationMillis: Long): String {
    if (durationMillis <= 0) return "Unknown"

    val totalSeconds = durationMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
