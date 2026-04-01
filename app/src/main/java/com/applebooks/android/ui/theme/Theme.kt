package com.applebooks.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AppleBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = AppleBlue.copy(alpha = 0.1f),
    onPrimaryContainer = AppleBlue,
    secondary = AppleGray,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceVariant = AppleGray6,
    outline = AppleGray3,
    error = AppleRed,
    onError = androidx.compose.ui.graphics.Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = AppleBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = AppleBlue.copy(alpha = 0.2f),
    onPrimaryContainer = AppleBlue,
    secondary = AppleGray,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceVariant = DarkSurface,
    outline = AppleGray,
    error = AppleRed,
    onError = androidx.compose.ui.graphics.Color.White
)

@Composable
fun AppleBooksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppleTypography,
        content = content
    )
}
