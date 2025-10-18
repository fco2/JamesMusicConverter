package com.chuka.jamesmusicconverter.navigation

import kotlinx.serialization.Serializable

/**
 * Base navigation key that all destinations should inherit from
 */
@Serializable
sealed interface NavKey

/**
 * Initial screen where user enters video URL
 */
@Serializable
object UrlInputRoute : NavKey

/**
 * Download mode enum for serialization
 */
@Serializable
enum class DownloadMode {
    AUDIO,  // Convert to MP3
    VIDEO   // Download as MP4
}

/**
 * Screen showing conversion progress with animation
 */
@Serializable
data class ConversionProgressRoute(
    val videoUrl: String,
    val username: String? = null,
    val password: String? = null,
    val cookiesFromBrowser: String? = null,
    val downloadMode: DownloadMode = DownloadMode.AUDIO
) : NavKey

/**
 * Screen showing conversion completed with download option
 */
@Serializable
data class ConversionCompletedRoute(
    val videoTitle: String,
    val thumbnailUrl: String?,
    val fileName: String,
    val fileSize: Long,
    val filePath: String,
    val durationMillis: Long = 0,
    val isVideo: Boolean = false,  // true if video file (MP4), false if audio (MP3)
    val videoUrl: String = "",      // URL to fetch full result from repository (for playlists)
    val videoCount: Int = 1         // Number of videos (for UI hint)
) : NavKey

/**
 * Screen showing conversion error
 */
@Serializable
data class ConversionErrorRoute(
    val errorMessage: String
) : NavKey
