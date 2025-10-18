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
 * Screen showing conversion progress with animation
 */
@Serializable
data class ConversionProgressRoute(
    val videoUrl: String,
    val username: String? = null,
    val password: String? = null,
    val cookiesFromBrowser: String? = null
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
    val durationMillis: Long = 0
) : NavKey

/**
 * Screen showing conversion error
 */
@Serializable
data class ConversionErrorRoute(
    val errorMessage: String
) : NavKey
