package com.chuka.jamesmusicconverter.ui.completed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionCompletedScreen(
    videoTitle: String,
    thumbnailUrl: String?,
    fileName: String,
    fileSize: Long,
    filePath: String,
    durationMillis: Long = 0,
    isVideo: Boolean = false,
    videoUrl: String = "",
    videoCount: Int = 1,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversionCompletedViewModel = hiltViewModel()
) {
    // Load full result from repository (for playlists)
    LaunchedEffect(videoUrl) {
        viewModel.loadResult(videoUrl)
    }

    val conversionResult by viewModel.conversionResult.collectAsState()

    // State to prevent rapid clicks at UI level
    var isPlayButtonEnabled by remember { mutableStateOf(true) }

    // Re-enable button after delay
    LaunchedEffect(isPlayButtonEnabled) {
        if (!isPlayButtonEnabled) {
            kotlinx.coroutines.delay(2000L) // 2 seconds cooldown
            isPlayButtonEnabled = true
        }
    }

    // Determine if we have multiple videos
    val hasMultipleVideos = conversionResult?.isPlaylist() == true
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "James Music Converter",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (hasMultipleVideos && conversionResult != null) {
            // Playlist View - Scrollable list of videos
            PlaylistView(
                conversionResult = conversionResult!!,
                isVideo = isVideo,
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                modifier = modifier.padding(paddingValues)
            )
        } else {
            // Single Video View
            SingleVideoView(
                videoTitle = videoTitle,
                thumbnailUrl = thumbnailUrl,
                fileName = fileName,
                fileSize = fileSize,
                filePath = filePath,
                durationMillis = durationMillis,
                isVideo = isVideo,
                isPlayButtonEnabled = isPlayButtonEnabled,
                onPlayButtonClick = {
                    if (isPlayButtonEnabled) {
                        android.util.Log.d("CHUKA_VIDEO", "Play button clicked - disabling for 2s")
                        isPlayButtonEnabled = false
                        viewModel.playFile(filePath, isVideo)
                    }
                },
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                modifier = modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun PlaylistView(
    conversionResult: com.chuka.jamesmusicconverter.domain.model.ConversionResult,
    isVideo: Boolean,
    viewModel: ConversionCompletedViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Success Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isVideo) "All Videos Downloaded!" else "Playlist Converted!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${conversionResult.getVideoCount()} ${if (isVideo) "videos" else "tracks"} • ${viewModel.formatFileSize(conversionResult.getTotalSize())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        // List of videos
        items(conversionResult.videos) { video ->
            VideoItemCard(
                video = video,
                isVideo = isVideo,
                viewModel = viewModel
            )
        }

        // Bottom Actions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider()

                // Open Folder Button (for all files)
                OutlinedButton(
                    onClick = {
                        // Open first file's location (all are in same folder)
                        if (conversionResult.videos.isNotEmpty()) {
                            viewModel.openFileLocation(conversionResult.videos.first().filePath)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Files Location",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                // Convert/Download Another Button
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVideo) "Download Another" else "Convert Another",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun VideoItemCard(
    video: com.chuka.jamesmusicconverter.domain.model.VideoItem,
    isVideo: Boolean,
    viewModel: ConversionCompletedViewModel,
    modifier: Modifier = Modifier
) {
    var isPlayButtonEnabled by remember { mutableStateOf(true) }

    // Re-enable button after delay
    LaunchedEffect(isPlayButtonEnabled) {
        if (!isPlayButtonEnabled) {
            kotlinx.coroutines.delay(2000L)
            isPlayButtonEnabled = true
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Card(
                modifier = Modifier
                    .size(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (!video.thumbnailUrl.isNullOrEmpty()) {
                    // Show actual video thumbnail
                    AsyncImage(
                        model = java.io.File(video.thumbnailUrl),
                        contentDescription = "Video thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(
                            android.R.drawable.ic_menu_gallery
                        )
                    )
                } else {
                    // Fallback to icon if thumbnail not available
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.VideoLibrary else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                // File Info
                Text(
                    text = viewModel.formatFileSize(video.fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Play Button
                    Button(
                        onClick = {
                            if (isPlayButtonEnabled) {
                                android.util.Log.d("CHUKA_VIDEO", "Play button clicked for: ${video.title}")
                                isPlayButtonEnabled = false
                                viewModel.playFile(video.filePath, isVideo)
                            }
                        },
                        enabled = isPlayButtonEnabled,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", style = MaterialTheme.typography.labelSmall)
                    }

                    // Share Button
                    OutlinedButton(
                        onClick = {
                            viewModel.shareFile(video.filePath, video.fileName)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun SingleVideoView(
    videoTitle: String,
    thumbnailUrl: String?,
    fileName: String,
    fileSize: Long,
    filePath: String,
    durationMillis: Long,
    isVideo: Boolean,
    isPlayButtonEnabled: Boolean,
    onPlayButtonClick: () -> Unit,
    viewModel: ConversionCompletedViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success Icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Title
        Text(
            text = if (isVideo) "Download Successful!" else "Conversion Successful!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        // Video Thumbnail
        if (!thumbnailUrl.isNullOrEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Video thumbnail",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.res.painterResource(
                        android.R.drawable.ic_menu_gallery
                    ),
                    placeholder = androidx.compose.ui.res.painterResource(
                        android.R.drawable.ic_menu_gallery
                    )
                )
            }
        }

        // Video Title Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.VideoLibrary else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "File Information",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                // Video Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Title:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = videoTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }

                // File Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "File Name:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }

                // File Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "File Size:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = viewModel.formatFileSize(fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Duration
                if (durationMillis > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Duration:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = viewModel.formatDuration(durationMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Play Button
        Button(
            onClick = onPlayButtonClick,
            enabled = isPlayButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isVideo) "Play Video" else "Play MP3",
                style = MaterialTheme.typography.titleSmall
            )
        }

        // Open File Location Button
        OutlinedButton(
            onClick = {
                viewModel.openFileLocation(filePath)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Open File Location",
                style = MaterialTheme.typography.titleSmall
            )
        }

        // Share Button
        OutlinedButton(
            onClick = {
                viewModel.shareFile(filePath, fileName)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Share File",
                style = MaterialTheme.typography.titleSmall
            )
        }

        // Convert/Download Another Button
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isVideo) "Download Another" else "Convert Another",
                style = MaterialTheme.typography.titleSmall
            )
        }

        // Bottom padding to ensure content isn't cut off
        Spacer(modifier = Modifier.height(8.dp))
    }
}
