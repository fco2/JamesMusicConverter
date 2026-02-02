package com.chuka.jamesmusicconverter.ui.queue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.chuka.jamesmusicconverter.ui.theme.StatusSuccess
import com.chuka.jamesmusicconverter.ui.theme.StatusSuccessDark
import com.chuka.jamesmusicconverter.ui.theme.StatusWarning
import com.chuka.jamesmusicconverter.ui.theme.StatusWarningDark
import java.io.File

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
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with status overlay
            ThumbnailWithStatus(
                thumbnailUrl = item.thumbnailUrl,
                localThumbnailPath = item.localThumbnailPath,
                status = item.status,
                downloadMode = item.downloadMode
            )

            // Middle content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Title
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Meta line: platform · duration · mode · carousel
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.platform,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.durationMillis > 0) {
                        Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = formatDuration(item.durationMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (item.downloadMode == DownloadMode.VIDEO) "Video" else "Audio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val carouselText = item.carouselPositionText
                    if (carouselText != null) {
                        Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        CarouselIndicatorBadge(
                            position = carouselText.substringBefore("/").toIntOrNull(),
                            total = carouselText.substringAfter("/").toIntOrNull()
                        )
                    }
                }

                // Status line + progress/file size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = getStatusColor(item.status),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if ((item.status == QueueItemStatus.ACTIVE || item.status == QueueItemStatus.PENDING) && item.progress > 0) {
                        Text(
                            text = "${item.progressPercentage}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (item.status == QueueItemStatus.COMPLETED && item.filePath != null) {
                        Text(
                            text = formatFileSize(item.fileSize),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Progress bar for active items
                if (item.status == QueueItemStatus.ACTIVE || item.status == QueueItemStatus.PENDING) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Error message
                if (item.status == QueueItemStatus.FAILED && item.errorMessage != null) {
                    Text(
                        text = item.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action icons
            ActionIcons(
                item = item,
                onCancel = onCancel,
                onRetry = onRetry,
                onRemove = onRemove,
                onPlay = onPlay
            )
        }
    }
}

@Composable
private fun ThumbnailWithStatus(
    thumbnailUrl: String?,
    localThumbnailPath: String?,
    status: QueueItemStatus,
    downloadMode: DownloadMode
) {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
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
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Grey out thumbnail when actively downloading
        if (status == QueueItemStatus.ACTIVE || status == QueueItemStatus.PENDING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        // Small status badges (non-active states only)
        when (status) {
            QueueItemStatus.ACTIVE, QueueItemStatus.PENDING -> {
                // No badge — thumbnail is greyed out instead
            }
            QueueItemStatus.COMPLETED -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isSystemInDarkTheme()) StatusSuccessDark else StatusSuccess),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, "Completed", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            QueueItemStatus.FAILED -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Failed", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            QueueItemStatus.CANCELLED -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isSystemInDarkTheme()) StatusWarningDark else StatusWarning),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Cancelled", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionIcons(
    item: QueueItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onPlay: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy((-4).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (item.status) {
            QueueItemStatus.PENDING, QueueItemStatus.ACTIVE -> {
                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            QueueItemStatus.COMPLETED -> {
                if (item.filePath != null) {
                    IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            QueueItemStatus.FAILED, QueueItemStatus.CANCELLED -> {
                IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun getStatusColor(status: QueueItemStatus): Color {
    val isDark = isSystemInDarkTheme()
    return when (status) {
        QueueItemStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        QueueItemStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        QueueItemStatus.COMPLETED -> if (isDark) StatusSuccessDark else StatusSuccess
        QueueItemStatus.FAILED -> MaterialTheme.colorScheme.error
        QueueItemStatus.CANCELLED -> if (isDark) StatusWarningDark else StatusWarning
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

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
