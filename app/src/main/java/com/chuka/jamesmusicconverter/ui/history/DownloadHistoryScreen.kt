package com.chuka.jamesmusicconverter.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chuka.jamesmusicconverter.domain.FileActionHandler
import com.chuka.jamesmusicconverter.domain.model.DownloadHistory
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import com.chuka.jamesmusicconverter.ui.components.CarouselIndicatorBadge
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistoryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fileActionHandler = viewModel.fileActionHandler
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    // Handle back press in selection mode
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.disableSelectionMode()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text(
                            "${uiState.selectedIds.size} selected",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "Download History",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isSelectionMode) {
                            viewModel.disableSelectionMode()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (uiState.isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (uiState.isSelectionMode) "Cancel" else "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        // Select all button
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select all"
                            )
                        }

                        // Delete selected button
                        if (uiState.selectedIds.isNotEmpty()) {
                            IconButton(onClick = { showDeleteSelectedDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete selected",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        // Filter button with dropdown
                        var showFilterMenu by remember { mutableStateOf(false) }

                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter"
                                )
                            }

                            // Filter dropdown menu
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Downloads") },
                                    onClick = {
                                        viewModel.filterByMode(null)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.SelectAll, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Audio Only") },
                                    onClick = {
                                        viewModel.filterByMode(DownloadMode.AUDIO)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.MusicNote, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Video Only") },
                                    onClick = {
                                        viewModel.filterByMode(DownloadMode.VIDEO)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.VideoLibrary, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (uiState.isSelectionMode)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.primary,
                    titleContentColor = if (uiState.isSelectionMode)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = if (uiState.isSelectionMode)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = if (uiState.isSelectionMode)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Minimal stats and filter row
            if (uiState.totalCount > 0 || uiState.downloads.isNotEmpty()) {
                StatsAndFilterRow(
                    totalCount = uiState.totalCount,
                    totalSize = uiState.totalSize,
                    filterMode = uiState.filterMode,
                    onFilterChange = { viewModel.filterByMode(it) }
                )
            }

            // Download list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.downloads.isEmpty()) {
                EmptyState(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.downloads,
                        key = { it.id }
                    ) { download ->
                        DownloadHistoryItem(
                            download = download,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = uiState.selectedIds.contains(download.id),
                            onLongPress = {
                                if (!uiState.isSelectionMode) {
                                    viewModel.enableSelectionMode()
                                    viewModel.toggleSelection(download.id)
                                }
                            },
                            onToggleSelection = {
                                viewModel.toggleSelection(download.id)
                            },
                            onPlay = {
                                fileActionHandler.playFile(
                                    download.filePath,
                                    isVideo = download.downloadMode == DownloadMode.VIDEO
                                )
                            },
                            onShare = {
                                fileActionHandler.shareFile(
                                    download.filePath,
                                    download.fileName
                                )
                            },
                            onDelete = {
                                viewModel.deleteDownload(download.id)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    fadeInSpec = tween(durationMillis = 300),
                                    fadeOutSpec = tween(durationMillis = 300),
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                        )
                    }
                }
            }
        }
    }

    // Delete selected dialog
    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("Delete ${uiState.selectedIds.size} item${if (uiState.selectedIds.size > 1) "s" else ""}?") },
            text = { Text("This will remove the selected entries from history. The downloaded files will remain on your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        showDeleteSelectedDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear all dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("This will remove all download history entries. The downloaded files will remain on your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Minimal stats and filter row
 */
@Composable
private fun StatsAndFilterRow(
    totalCount: Int,
    totalSize: Long,
    filterMode: DownloadMode?,
    onFilterChange: (DownloadMode?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stats text
        Text(
            text = "$totalCount downloads",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "•",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatFileSize(totalSize),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // Filter chips
        FilterChip(
            selected = filterMode == null,
            onClick = { onFilterChange(null) },
            label = { Text("All") },
            modifier = Modifier.height(32.dp)
        )

        FilterChip(
            selected = filterMode == DownloadMode.AUDIO,
            onClick = { onFilterChange(DownloadMode.AUDIO) },
            label = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Audio",
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier.height(32.dp)
        )

        FilterChip(
            selected = filterMode == DownloadMode.VIDEO,
            onClick = { onFilterChange(DownloadMode.VIDEO) },
            label = {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Video",
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier.height(32.dp)
        )
    }
}

@Composable
private fun DownloadHistoryItem(
    download: DownloadHistory,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val fileExists = remember(download.filePath) {
        File(download.filePath).exists()
    }

    Card(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .shadow(
                elevation = if (isSelected) 8.dp else 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,  // Shadow handled by modifier
            pressedElevation = 0.dp
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelection()
                            } else {
                                isExpanded = !isExpanded
                            }
                        },
                        onLongClick = onLongPress
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Checkbox in selection mode
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                }
                // Thumbnail with overlay
                Box(
                    modifier = Modifier.size(88.dp, 66.dp)
                ) {
                    val thumbnailToDisplay = download.getThumbnailToDisplay()
                    if (thumbnailToDisplay != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(thumbnailToDisplay)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Thumbnail",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        // Duration overlay
                        if (download.duration > 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = download.getFormattedDuration(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Mode indicator
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp),
                            color = if (download.downloadMode == DownloadMode.VIDEO)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            else
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (download.downloadMode == DownloadMode.VIDEO)
                                    Icons.Default.VideoLibrary
                                else
                                    Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp),
                                tint = Color.White
                            )
                        }

                        // Carousel badge (top-right)
                        val carouselText = download.getCarouselPositionText()
                        if (carouselText != null) {
                            CarouselIndicatorBadge(
                                position = download.carouselPosition,
                                total = download.carouselTotal,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            )
                        }
                    } else {
                        // Fallback icon with gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (download.downloadMode == DownloadMode.VIDEO)
                                    Icons.Default.VideoLibrary
                                else
                                    Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Platform badge with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = getPlatformColor(download.platform),
                            shape = RoundedCornerShape(6.dp),
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                text = download.platform,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // File status indicator
                        if (!fileExists) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Missing",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // Title
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Metadata row: file size • duration • time ago
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // File size
                        Text(
                            text = download.getFormattedFileSize(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Duration (if available)
                        if (download.duration > 0) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = download.getFormattedDuration(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Time ago
                        Text(
                            text = download.getTimeAgo(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // File name
                    Text(
                        text = download.fileName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Expand indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Show less" else "Show more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Expanded actions
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Additional info section
                    if (download.uploader != null) {
                        InfoRow(
                            icon = Icons.Default.Person,
                            label = "Uploader",
                            value = download.uploader
                        )
                    }

                    InfoRow(
                        icon = Icons.Default.Folder,
                        label = "File Name",
                        value = download.fileName
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (fileExists) {
                            // Play button
                            FilledTonalButton(
                                onClick = onPlay,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play", fontWeight = FontWeight.Medium)
                            }

                            // Share button
                            FilledTonalButton(
                                onClick = onShare,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontWeight = FontWeight.Medium)
                            }
                        }

                        // Delete button
                        FilledTonalButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete from history?") },
            text = { Text("This will only remove the entry from history. The file will remain on your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    // Animated scale for entrance
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Pulsing animation for icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon background
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha + 0.3f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Downloads Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your download history will appear here",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Start downloading videos and audio\nto build your history",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * Get color for platform badge
 */
@Composable
private fun getPlatformColor(platform: String): Color {
    return when (platform) {
        "YouTube" -> Color(0xFFFF0000)
        "TikTok" -> Color(0xFF000000)
        "Instagram" -> Color(0xFFE4405F)
        "Twitter" -> Color(0xFF1DA1F2)
        "Facebook" -> Color(0xFF1877F2)
        "Dailymotion" -> Color(0xFF0062FF)
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Info row composable
 */
@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
