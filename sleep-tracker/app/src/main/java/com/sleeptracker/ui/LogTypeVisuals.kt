package com.sleeptracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.sleeptracker.R
import com.sleeptracker.data.LogType

/** Icon, label and colors used for each entry type, so buttons and list rows match. */
data class LogTypeVisuals(
    @param:DrawableRes val icon: Int,
    val label: String,
    val container: Color,
    val onContainer: Color,
    val accent: Color,
)

@Composable
fun LogType.visuals(): LogTypeVisuals {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        LogType.BEDTIME -> LogTypeVisuals(
            icon = R.drawable.ic_bedtime,
            label = stringResource(R.string.type_bedtime),
            container = colors.primaryContainer,
            onContainer = colors.onPrimaryContainer,
            accent = colors.primary,
        )
        LogType.WAKEUP -> LogTypeVisuals(
            icon = R.drawable.ic_sunny,
            label = stringResource(R.string.type_wakeup),
            container = colors.tertiaryContainer,
            onContainer = colors.onTertiaryContainer,
            accent = colors.tertiary,
        )
        LogType.ENERGY -> LogTypeVisuals(
            icon = R.drawable.ic_bolt,
            label = stringResource(R.string.type_energy),
            container = colors.secondaryContainer,
            onContainer = colors.onSecondaryContainer,
            accent = colors.secondary,
        )
    }
}
