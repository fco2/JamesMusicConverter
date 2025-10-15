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
                UrlInputScreen(
                    onNavigateToProgress = { videoUrl ->
                        backStack.navigate(ConversionProgressRoute(videoUrl))
                    }
                )
            }

            is ConversionProgressRoute -> {
                ConversionProgressScreen(
                    videoUrl = destination.videoUrl,
                    onNavigateToCompleted = { videoTitle, thumbnailUrl, fileName, fileSize, filePath ->
                        // Replace current screen with completed screen
                        backStack.replace(
                            ConversionCompletedRoute(
                                videoTitle = videoTitle,
                                thumbnailUrl = thumbnailUrl,
                                fileName = fileName,
                                fileSize = fileSize,
                                filePath = filePath
                            )
                        )
                    },
                    onNavigateToError = { errorMessage ->
                        backStack.replace(ConversionErrorRoute(errorMessage))
                    }
                )
            }

            is ConversionCompletedRoute -> {
                ConversionCompletedScreen(
                    videoTitle = destination.videoTitle,
                    thumbnailUrl = destination.thumbnailUrl,
                    fileName = destination.fileName,
                    fileSize = destination.fileSize,
                    filePath = destination.filePath,
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
                        backStack.clearAndNavigate(UrlInputRoute)
                    },
                    onRetry = {
                        backStack.navigateUp()
                    }
                )
            }
        }
    }
}
