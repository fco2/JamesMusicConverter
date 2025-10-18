package com.chuka.jamesmusicconverter.ui.progress

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onNavigateToCompleted: (String, String?, String, Long, String, Long) -> Unit,
    onNavigateToError: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversionProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Track the generation of this screen's conversion to prevent stale state navigation
    var expectedGeneration by remember(videoUrl) { mutableIntStateOf(0) }
    var hasNavigated by remember(videoUrl) { mutableStateOf(false) }

    // Reset and start conversion when screen is first displayed
    LaunchedEffect(videoUrl) {
        android.util.Log.d("CHUKA_Screen", "=== LaunchedEffect(videoUrl) triggered ===")
        android.util.Log.d("CHUKA_Screen", "URL: $videoUrl")
        android.util.Log.d("CHUKA_Screen", "Current state BEFORE reset: ${viewModel.uiState.value}")

        // Reset navigation flag for this new URL
        hasNavigated = false

        viewModel.reset() // Clear any previous state

        android.util.Log.d("CHUKA_Screen", "State AFTER reset: ${viewModel.uiState.value}")

        // Wait for state to stabilize by checking it's actually Idle
        // This ensures the Navigation LaunchedEffect won't trigger with stale Success state
        while (viewModel.uiState.value !is ConversionUiState.Idle) {
            kotlinx.coroutines.delay(10)
        }

        android.util.Log.d("CHUKA_Screen", "State confirmed Idle, starting conversion...")

        viewModel.startConversion(
            videoUrl = videoUrl,
            username = username,
            password = password,
            cookiesFromBrowser = cookiesFromBrowser
        )

        // Track the generation of this conversion
        expectedGeneration = viewModel.getCurrentGeneration()

        android.util.Log.d("CHUKA_Screen", "Started conversion with generation $expectedGeneration")
    }

    // Handle state changes - only navigate on Success, Error, or Cancelled
    // Only navigate for states matching this conversion's generation
    LaunchedEffect(uiState) {
        android.util.Log.d("CHUKA_Screen", "=== Navigation LaunchedEffect triggered ===")
        android.util.Log.d("CHUKA_Screen", "State: $uiState")
        android.util.Log.d("CHUKA_Screen", "Expected generation: $expectedGeneration")
        android.util.Log.d("CHUKA_Screen", "Has navigated: $hasNavigated")

        // Prevent multiple navigations for the same conversion
        if (hasNavigated) {
            android.util.Log.d("CHUKA_Screen", "IGNORING - Already navigated for this conversion")
            return@LaunchedEffect
        }

        when (val state = uiState) {
            is ConversionUiState.Success -> {
                // Only navigate if this Success is for our conversion
                if (state.generation != expectedGeneration) {
                    android.util.Log.d("CHUKA_Screen", "IGNORING Success - wrong generation (${state.generation} != $expectedGeneration)")
                    return@LaunchedEffect
                }
                android.util.Log.d("CHUKA_Screen", "SUCCESS - Navigating to completed: ${state.result.fileName}")
                hasNavigated = true
                onNavigateToCompleted(
                    state.result.videoTitle,
                    state.result.thumbnailUrl,
                    state.result.fileName,
                    state.result.fileSize,
                    state.result.filePath,
                    state.result.durationMillis
                )
            }
            is ConversionUiState.Error -> {
                // Only navigate if this Error is for our conversion
                if (state.generation != expectedGeneration) {
                    android.util.Log.d("CHUKA_Screen", "IGNORING Error - wrong generation (${state.generation} != $expectedGeneration)")
                    return@LaunchedEffect
                }
                android.util.Log.d("CHUKA_Screen", "ERROR - Navigating to error: ${state.message}")
                hasNavigated = true
                onNavigateToError(state.message)
            }
            is ConversionUiState.Cancelled -> {
                // Only navigate if this Cancelled is for our conversion
                if (state.generation != expectedGeneration) {
                    android.util.Log.d("CHUKA_Screen", "IGNORING Cancelled - wrong generation (${state.generation} != $expectedGeneration)")
                    return@LaunchedEffect
                }
                android.util.Log.d("CHUKA_Screen", "CANCELLED - Navigating back to home")
                hasNavigated = true
                // Show toast and navigate back to home
                android.widget.Toast.makeText(
                    context,
                    "Conversion cancelled",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                onNavigateBack()
            }
            else -> {
                android.util.Log.d("CHUKA_Screen", "State is ${state::class.simpleName} - no navigation")
            }
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

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel Button
            if (uiState is ConversionUiState.Converting && viewModel.isCancellable()) {
                OutlinedButton(
                    onClick = { viewModel.cancelConversion() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel Conversion")
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
