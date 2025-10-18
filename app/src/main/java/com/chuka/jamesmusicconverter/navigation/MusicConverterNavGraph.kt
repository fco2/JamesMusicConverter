package com.chuka.jamesmusicconverter.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.chuka.jamesmusicconverter.ui.completed.ConversionCompletedScreen
import com.chuka.jamesmusicconverter.ui.components.MusicSnackbar
import com.chuka.jamesmusicconverter.ui.components.SnackbarController
import com.chuka.jamesmusicconverter.ui.components.SnackbarViewModel
import com.chuka.jamesmusicconverter.ui.error.ConversionErrorScreen
import com.chuka.jamesmusicconverter.ui.progress.ConversionProgressScreen
import com.chuka.jamesmusicconverter.ui.urlinput.UrlInputScreen

/**
 * Main navigation graph for the Music Converter app
 * Uses custom NavDisplay navigation system
 */
@Composable
fun MusicConverterNavGraph() {
    val navBackStack = rememberNavBackStack(UrlInputRoute)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Get snackbar controller from ViewModel
    val snackbarViewModel: SnackbarViewModel = hiltViewModel()
    val snackbarController = snackbarViewModel.snackbarController

    // Initialize snackbar controller
    LaunchedEffect(Unit) {
        snackbarController.initialize(snackbarHostState, scope)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                MusicSnackbar(snackbarData = data)
            }
        }
    ) { paddingValues ->
        NavDisplay(navBackStack = navBackStack) { destination, backStack ->
        when (destination) {
            is UrlInputRoute -> {
                android.util.Log.d("CHUKA_NavGraph", "=== Showing UrlInputScreen ===")

                UrlInputScreen(
                    onNavigateToProgress = { videoUrl, username, password, browser, downloadMode ->
                        android.util.Log.d("CHUKA_NavGraph", "=== onNavigateToProgress callback ===")
                        android.util.Log.d("CHUKA_NavGraph", "Navigating to progress with URL: $videoUrl")
                        android.util.Log.d("CHUKA_NavGraph", "Download mode: $downloadMode")

                        backStack.navigate(ConversionProgressRoute(
                            videoUrl = videoUrl,
                            username = username,
                            password = password,
                            cookiesFromBrowser = browser,
                            downloadMode = downloadMode
                        ))
                    }
                )
            }

            is ConversionProgressRoute -> {
                android.util.Log.d("CHUKA_NavGraph", "=== Showing ConversionProgressScreen ===")
                android.util.Log.d("CHUKA_NavGraph", "URL: ${destination.videoUrl}")

                ConversionProgressScreen(
                    videoUrl = destination.videoUrl,
                    username = destination.username,
                    password = destination.password,
                    cookiesFromBrowser = destination.cookiesFromBrowser,
                    downloadMode = destination.downloadMode,
                    onNavigateToCompleted = { videoTitle, thumbnailUrl, fileName, fileSize, filePath, durationMillis, isVideo, videoUrl, videoCount ->
                        android.util.Log.d("CHUKA_NavGraph", "=== onNavigateToCompleted callback ===")
                        android.util.Log.d("CHUKA_NavGraph", "Navigating to completed: $fileName (isVideo: $isVideo, videoCount: $videoCount)")

                        // Replace current screen with completed screen
                        backStack.replace(
                            ConversionCompletedRoute(
                                videoTitle = videoTitle,
                                thumbnailUrl = thumbnailUrl,
                                fileName = fileName,
                                fileSize = fileSize,
                                filePath = filePath,
                                durationMillis = durationMillis,
                                isVideo = isVideo,
                                videoUrl = videoUrl,
                                videoCount = videoCount
                            )
                        )
                    },
                    onNavigateToError = { errorMessage ->
                        android.util.Log.d("CHUKA_NavGraph", "=== onNavigateToError callback ===")
                        android.util.Log.d("CHUKA_NavGraph", "Error: $errorMessage")

                        backStack.replace(ConversionErrorRoute(errorMessage))
                    },
                    onNavigateBack = {
                        android.util.Log.d("CHUKA_NavGraph", "=== onNavigateBack callback (cancel) ===")

                        // Clear stack and go back to input screen (for cancellation)
                        backStack.clearAndNavigate(UrlInputRoute)
                    }
                )
            }

            is ConversionCompletedRoute -> {
                android.util.Log.d("CHUKA_NavGraph", "=== Showing ConversionCompletedScreen ===")
                android.util.Log.d("CHUKA_NavGraph", "File: ${destination.fileName} (isVideo: ${destination.isVideo}, videoCount: ${destination.videoCount})")

                ConversionCompletedScreen(
                    videoTitle = destination.videoTitle,
                    thumbnailUrl = destination.thumbnailUrl,
                    fileName = destination.fileName,
                    fileSize = destination.fileSize,
                    filePath = destination.filePath,
                    durationMillis = destination.durationMillis,
                    isVideo = destination.isVideo,
                    videoUrl = destination.videoUrl,
                    videoCount = destination.videoCount,
                    onNavigateBack = {
                        // Clear stack and go back to input screen
                        backStack.clearAndNavigate(UrlInputRoute)
                    }
                )
            }

            is ConversionErrorRoute -> {
                ConversionErrorScreen(
                    errorMessage = destination.errorMessage,
                    onNavigateBack = {
                        // Clear entire stack and return to input screen
                        backStack.clearAndNavigate(UrlInputRoute)
                    },
                    onRetry = {
                        // Go back to input screen to try a different URL
                        backStack.clearAndNavigate(UrlInputRoute)
                    }
                )
            }
        }
        }
    }
}
