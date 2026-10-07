package com.sleeptracker.watch.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.sleeptracker.watch.R
import com.sleeptracker.watch.sync.PhoneSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import android.text.format.DateFormat as AndroidDateFormat

private const val ROUTE_HOME = "home"
private const val ROUTE_ENERGY = "energy"
private const val CONFIRMATION_MILLIS = 2_000L

private enum class LogKind { BEDTIME, WAKEUP, ENERGY }

private sealed interface SendState {
    data object Sending : SendState
    data class Done(
        val kind: LogKind,
        val timestamp: Long,
        val rating: Int?,
        val delivery: PhoneSync.Delivery,
    ) : SendState
    data object Failed : SendState
}

/** Root of the watch UI: the 3-button home screen, the energy picker, and the confirmation overlay. */
@Composable
fun WatchApp(phoneSync: PhoneSync) {
    val navController = rememberSwipeDismissableNavController()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var sendState by remember { mutableStateOf<SendState?>(null) }

    fun log(kind: LogKind, rating: Int? = null) {
        if (sendState == SendState.Sending) return
        val now = System.currentTimeMillis()
        sendState = SendState.Sending
        scope.launch {
            sendState = try {
                val delivery = when (kind) {
                    LogKind.BEDTIME -> phoneSync.logBedtime(now)
                    LogKind.WAKEUP -> phoneSync.logWakeup(now)
                    LogKind.ENERGY -> phoneSync.logEnergy(requireNotNull(rating), now)
                }
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                SendState.Done(kind, now, rating, delivery)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                SendState.Failed
            }
        }
    }

    AppScaffold {
        SwipeDismissableNavHost(navController = navController, startDestination = ROUTE_HOME) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    onBed = { log(LogKind.BEDTIME) },
                    onAwake = { log(LogKind.WAKEUP) },
                    onEnergy = { navController.navigate(ROUTE_ENERGY) },
                )
            }
            composable(ROUTE_ENERGY) {
                EnergyScreen(
                    onRate = { rating ->
                        navController.popBackStack()
                        log(LogKind.ENERGY, rating)
                    },
                )
            }
        }

        sendState?.let { state ->
            SendStatusOverlay(
                state = state,
                onDismiss = { if (state != SendState.Sending) sendState = null },
            )
        }
    }

    val current = sendState
    LaunchedEffect(current) {
        if (current is SendState.Done || current is SendState.Failed) {
            delay(CONFIRMATION_MILLIS)
            sendState = null
        }
    }
}

@Composable
private fun HomeScreen(onBed: () -> Unit, onAwake: () -> Unit, onEnergy: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val colors = MaterialTheme.colorScheme

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                WatchActionButton(
                    icon = R.drawable.ic_bedtime,
                    label = stringResource(R.string.bed),
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    onClick = onBed,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                WatchActionButton(
                    icon = R.drawable.ic_sunny,
                    label = stringResource(R.string.awake),
                    containerColor = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer,
                    onClick = onAwake,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                WatchActionButton(
                    icon = R.drawable.ic_bolt,
                    label = stringResource(R.string.energy),
                    containerColor = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                    onClick = onEnergy,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
        }
    }
}

@Composable
private fun WatchActionButton(
    @DrawableRes icon: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier,
    transformation: SurfaceTransformation,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        icon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            iconColor = contentColor,
        ),
        transformation = transformation,
        label = {
            Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
    )
}

/** Five big numbers: 1 2 3 on top, 4 5 below, sized to fit small round screens. */
@Composable
private fun EnergyScreen(onRate: (Int) -> Unit) {
    ScreenScaffold { _ ->
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val buttonSize = (maxWidth * 0.26f).coerceIn(44.dp, 64.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.energy_question),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (rating in 1..3) RatingButton(rating, buttonSize, onRate)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (rating in 4..5) RatingButton(rating, buttonSize, onRate)
                }
            }
        }
    }
}

@Composable
private fun RatingButton(rating: Int, size: Dp, onRate: (Int) -> Unit) {
    TextButton(
        onClick = { onRate(rating) },
        modifier = Modifier.size(size),
        shapes = TextButtonDefaults.animatedShapes(),
        colors = TextButtonDefaults.filledTonalTextButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(
            text = rating.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Full-screen confirmation: spinner while sending, then a checkmark with the logged time. */
@Composable
private fun SendStatusOverlay(state: SendState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            SendState.Sending -> CircularProgressIndicator(modifier = Modifier.size(52.dp))

            is SendState.Done -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (state.kind) {
                        LogKind.BEDTIME -> stringResource(R.string.logged_bedtime)
                        LogKind.WAKEUP -> stringResource(R.string.logged_wakeup)
                        LogKind.ENERGY -> stringResource(R.string.logged_energy, state.rating ?: 0)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = AndroidDateFormat.getTimeFormat(context).format(Date(state.timestamp)),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.primary,
                )
                Text(
                    text = stringResource(
                        if (state.delivery == PhoneSync.Delivery.SENT) R.string.saved_on_phone else R.string.will_sync
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            SendState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.save_failed),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.error,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.try_again),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
