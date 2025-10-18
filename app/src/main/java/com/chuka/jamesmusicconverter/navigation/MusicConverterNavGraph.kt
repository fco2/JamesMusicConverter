package com.chuka.jamesmusicconverter.navigation

import androidx.compose.runtime.Composable
import com.chuka.jamesmusicconverter.ui.completed.ConversionCompletedScreen
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

    NavDisplay(navBackStack = navBackStack) { destination, backStack ->
        when (destination) {
            is UrlInputRoute -> {
                android.util.Log.d("CHUKA_NavGraph", "=== Showing UrlInputScreen ===")

                UrlInputScreen(
                    onNavigateToProgress = { videoUrl, username, password, browser ->
                        android.util.Log.d("CHUKA_NavGraph", "=== onNavigateToProgress callback ===")
                        android.util.Log.d("CHUKA_NavGraph", "Navigating to progress with URL: $videoUrl")

                        backStack.navigate(ConversionProgressRoute(
                            videoUrl = videoUrl,
                            username = username,
                            password = password,
                            cookiesFromBrowser = browser
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
                    onNavigateToCompleted = { videoTitle, thumbnailUrl, fileName, fileSize, filePath, durationMillis ->
                        android.util.Log.d("CHUKA_NavGraph", "=== onNavigateToCompleted callback ===")
                        android.util.Log.d("CHUKA_NavGraph", "Navigating to completed: $fileName")

                        // Replace current screen with completed screen
                        backStack.replace(
                            ConversionCompletedRoute(
                                videoTitle = videoTitle,
                                thumbnailUrl = thumbnailUrl,
                                fileName = fileName,
                                fileSize = fileSize,
                                filePath = filePath,
                                durationMillis = durationMillis
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
                android.util.Log.d("CHUKA_NavGraph", "File: ${destination.fileName}")

                ConversionCompletedScreen(
                    videoTitle = destination.videoTitle,
                    thumbnailUrl = destination.thumbnailUrl,
                    fileName = destination.fileName,
                    fileSize = destination.fileSize,
                    filePath = destination.filePath,
                    durationMillis = destination.durationMillis,
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
