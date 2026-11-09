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
    primary = SeaGlass,
    onPrimary = Color(0xFF12392C),
    primaryContainer = Color(0xFF24483A),
    onPrimaryContainer = Color(0xFFD0EDDA),
    secondary = Ocean,
    onSecondary = Color(0xFF12383F),
    secondaryContainer = Color(0xFF23434A),
    onSecondaryContainer = Color(0xFFD1EDF0),
    tertiary = DarkSand,
    onTertiary = Color(0xFF443116),
    tertiaryContainer = Color(0xFF55452D),
    onTertiaryContainer = Color(0xFFF3E1C1),
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
    secondary = LightSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
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
