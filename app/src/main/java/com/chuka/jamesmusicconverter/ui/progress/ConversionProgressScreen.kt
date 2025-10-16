package com.chuka.jamesmusicconverter.ui.progress

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionProgressScreen(
    videoUrl: String,
    username: String? = null,
    password: String? = null,
    cookiesFromBrowser: String? = null,
    onNavigateToCompleted: (String, String?, String, Long, String) -> Unit,
    onNavigateToError: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversionProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Reset and start conversion when screen is first displayed
    LaunchedEffect(videoUrl) {
        viewModel.reset() // Clear any previous state
        viewModel.startConversion(
            videoUrl = videoUrl,
            username = username,
            password = password,
            cookiesFromBrowser = cookiesFromBrowser
        )
    }

    // Handle state changes - only navigate on Success or Error after conversion starts
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ConversionUiState.Success -> {
                onNavigateToCompleted(
                    state.result.videoTitle,
                    state.result.thumbnailUrl,
                    state.result.fileName,
                    state.result.fileSize,
                    state.result.filePath
                )
            }
            is ConversionUiState.Error -> {
                onNavigateToError(state.message)
            }
            else -> { /* No-op */ }
        }
    }

    val progressText = when (val state = uiState) {
        is ConversionUiState.Converting -> state.progress.statusMessage
        else -> "Initializing..."
    }

    val progressPercentage = when (val state = uiState) {
        is ConversionUiState.Converting -> state.progress.percentage
        else -> 0f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "James Music Converter",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Progress Circle
            AnimatedProgressCircle(
                progress = progressPercentage,
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Progress Percentage
            Text(
                text = "${(progressPercentage * 100).toInt()}%",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Text
            Text(
                text = progressText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // URL Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Converting from:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = videoUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedProgressCircle(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "progress"
    )

    // Rotation animation for the circle
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val radius = size.minDimension / 2 - strokeWidth / 2

            // Background circle
            drawCircle(
                color = surfaceVariant,
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = primaryColor,
                startAngle = rotation - 90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Pulsing dots in center
        PulsingDots()
    }
}

@Composable
fun PulsingDots(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx() * scale1
            )
        }
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx() * scale2
            )
        }
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx() * scale1
            )
        }
    }
}
