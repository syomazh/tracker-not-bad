package com.sleeptracker.watch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

// Same night palette as the phone app.
private val WatchColors = ColorScheme(
    primary = Color(0xFFA9B4FF),
    primaryDim = Color(0xFF8E9AE6),
    primaryContainer = Color(0xFF2D3870),
    onPrimary = Color(0xFF151C4A),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC6B8F2),
    secondaryDim = Color(0xFFAB9DD6),
    secondaryContainer = Color(0xFF41366A),
    onSecondary = Color(0xFF2B2152),
    onSecondaryContainer = Color(0xFFE8DEFF),
    tertiary = Color(0xFF93C7E6),
    tertiaryDim = Color(0xFF79ACCA),
    tertiaryContainer = Color(0xFF1F4862),
    onTertiary = Color(0xFF0D3247),
    onTertiaryContainer = Color(0xFFCBE7FA),
    surfaceContainerLow = Color(0xFF131929),
    surfaceContainer = Color(0xFF171E2F),
    surfaceContainerHigh = Color(0xFF1D2437),
    onSurface = Color(0xFFE3E5F0),
    onSurfaceVariant = Color(0xFFA7AEC4),
    outline = Color(0xFF4A5269),
    outlineVariant = Color(0xFF2E3548),
    background = Color(0xFF0E1320),
    onBackground = Color(0xFFE3E5F0),
    error = Color(0xFFFFB4AB),
    errorDim = Color(0xFFE59990),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun SleepTrackerWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WatchColors, content = content)
}
