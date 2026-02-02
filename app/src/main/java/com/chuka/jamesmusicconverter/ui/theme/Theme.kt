package com.chuka.jamesmusicconverter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Muted slate blue light theme (easy on the eyes)
private val LightColorScheme = lightColorScheme(
    primary = SlateBlue,
    onPrimary = White,
    primaryContainer = SlateBluePale,
    onPrimaryContainer = SlateBlueDark,

    secondary = SlateBlueDark,
    onSecondary = White,
    secondaryContainer = SlateBlueLight.copy(alpha = 0.2f),
    onSecondaryContainer = SlateBlueDark,

    tertiary = SlateBlueLight,
    onTertiary = TextPrimary,

    background = BackgroundGray,
    onBackground = TextPrimary,

    surface = White,
    onSurface = TextPrimary,

    surfaceVariant = SurfaceGray,
    onSurfaceVariant = TextSecondary,

    error = androidx.compose.ui.graphics.Color(0xFFD32F2F),
    onError = White,

    outline = androidx.compose.ui.graphics.Color(0xFFBDBDBD),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
)

// Dark theme
private val DarkColorScheme = darkColorScheme(
    primary = SlateBlueLight,
    onPrimary = White,
    primaryContainer = SlateBlueDark,
    onPrimaryContainer = SlateBluePale,

    secondary = SlateBlueLight,
    onSecondary = White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,

    tertiary = SlateBlueLight,
    onTertiary = DarkTextPrimary,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,

    error = androidx.compose.ui.graphics.Color(0xFFEF5350),
    onError = White,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

@Composable
fun JamesMusicConverterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to force custom theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            if (darkTheme) {
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
            } else {
                window.statusBarColor = SlateBlue.toArgb()
                window.navigationBarColor = SlateBlue.toArgb()
            }

            // Set status bar icons to white (light = false means white icons)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
