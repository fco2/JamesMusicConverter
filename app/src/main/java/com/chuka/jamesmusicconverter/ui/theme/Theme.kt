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

// Modern professional light theme with improved colors
private val AppColorScheme = lightColorScheme(
    primary = NavyBlue,
    onPrimary = White,
    primaryContainer = LightBlue.copy(alpha = 0.15f),
    onPrimaryContainer = NavyBlue,

    secondary = LightBlue,
    onSecondary = White,
    secondaryContainer = LightBlue.copy(alpha = 0.2f),
    onSecondaryContainer = NavyBlue,

    tertiary = Purple40,
    onTertiary = White,

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

@Composable
fun JamesMusicConverterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to force custom theme
    content: @Composable () -> Unit
) {
    // Always use the app color scheme (white background)
    val colorScheme = AppColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavyBlue.toArgb()
            window.navigationBarColor = NavyBlue.toArgb()

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