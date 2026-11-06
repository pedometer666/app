package com.pedometer.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Color(0xFF06351F),
    primaryContainer = Color(0xFF0E3B2A),
    onPrimaryContainer = Color(0xFF9FF0C5),
    secondary = DarkAmber,
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF3A3320),
    onSecondaryContainer = Color(0xFFFDE68A),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnVariant,
    surfaceContainerHighest = Color(0xFF1B2620),
    surfaceContainerHigh = Color(0xFF16201B),
    surfaceContainer = Color(0xFF131C17),
    surfaceContainerLow = Color(0xFF101713),
    surfaceContainerLowest = Color(0xFF0B100E),
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightAmber,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE8C2),
    onSecondaryContainer = Color(0xFF492B00),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFDDE8E1),
    onSurfaceVariant = LightOnVariant,
    surfaceContainerHighest = Color(0xFFE0EAE4),
    surfaceContainerHigh = Color(0xFFE9F1EC),
    surfaceContainer = Color(0xFFEFF5F1),
    surfaceContainerLow = Color(0xFFF6FBF8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

@Composable
fun PedometerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

