package com.sleeptracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Calm night palette: deep navy surfaces with soft periwinkle, lavender and sky accents.
val Navy950 = Color(0xFF0A0E18)
val Navy900 = Color(0xFF0E1320)
val Navy850 = Color(0xFF131929)
val Navy800 = Color(0xFF171E2F)
val Navy750 = Color(0xFF1D2437)
val Navy700 = Color(0xFF242C41)

val Periwinkle = Color(0xFFA9B4FF)
val Lavender = Color(0xFFC6B8F2)
val Sky = Color(0xFF93C7E6)
val TextPrimary = Color(0xFFE3E5F0)
val TextSecondary = Color(0xFFA7AEC4)

private val NightColors = darkColorScheme(
    primary = Periwinkle,
    onPrimary = Color(0xFF151C4A),
    primaryContainer = Color(0xFF2D3870),
    onPrimaryContainer = Color(0xFFDDE1FF),
    inversePrimary = Color(0xFF4A56A0),
    secondary = Lavender,
    onSecondary = Color(0xFF2B2152),
    secondaryContainer = Color(0xFF41366A),
    onSecondaryContainer = Color(0xFFE8DEFF),
    tertiary = Sky,
    onTertiary = Color(0xFF0D3247),
    tertiaryContainer = Color(0xFF1F4862),
    onTertiaryContainer = Color(0xFFCBE7FA),
    background = Navy900,
    onBackground = TextPrimary,
    surface = Navy900,
    onSurface = TextPrimary,
    surfaceVariant = Navy700,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Periwinkle,
    inverseSurface = TextPrimary,
    inverseOnSurface = Color(0xFF1B2030),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF4A5269),
    outlineVariant = Color(0xFF2E3548),
    scrim = Color.Black,
    surfaceBright = Navy700,
    surfaceDim = Navy900,
    surfaceContainerLowest = Navy950,
    surfaceContainerLow = Navy850,
    surfaceContainer = Navy800,
    surfaceContainerHigh = Navy750,
    surfaceContainerHighest = Navy700,
)

/** The app is dark-only by design, so there is no light scheme. */
@Composable
fun SleepTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NightColors,
        typography = Typography(),
        content = content,
    )
}
