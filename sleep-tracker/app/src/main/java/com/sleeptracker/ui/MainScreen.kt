package com.sleeptracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleeptracker.R
import com.sleeptracker.data.LogType
import com.sleeptracker.data.SleepLog
import com.sleeptracker.export.CsvExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val loadedLogs by viewModel.logs.collectAsStateWithLifecycle()
    val logs = loadedLogs.orEmpty()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showEnergyPicker by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> {
                    // Replace whatever is showing so rapid taps never queue up stale messages.
                    snackbarHostState.currentSnackbarData?.dismiss()
                    scope.launch { snackbarHostState.showSnackbar(event.text) }
                }
                is UiEvent.Share -> context.startActivity(CsvExporter.shareIntent(context, event.uri))
            }
        }
    }

    val lastBedtime = logs.firstOrNull { it.type == LogType.BEDTIME }
    val lastWakeup = logs.firstOrNull { it.type == LogType.WAKEUP }
    val todayEnergy = logs.firstOrNull { it.type == LogType.ENERGY && Formatters.isToday(it.timestamp) }?.value

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    TextButton(
                        onClick = viewModel::exportCsv,
                        modifier = Modifier.padding(end = 8.dp).heightIn(min = 48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.export_csv))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "bed") {
                val visuals = LogType.BEDTIME.visuals()
                BigActionButton(
                    icon = visuals.icon,
                    label = stringResource(R.string.going_to_bed),
                    supportingText = lastBedtime?.let {
                        stringResource(R.string.last_logged, Formatters.dateAndTime(context, it.timestamp))
                    },
                    containerColor = visuals.container,
                    contentColor = visuals.onContainer,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        viewModel.logBedtime()
                    },
                )
            }
            item(key = "awake") {
                val visuals = LogType.WAKEUP.visuals()
                BigActionButton(
                    icon = visuals.icon,
                    label = stringResource(R.string.im_awake),
                    supportingText = lastWakeup?.let {
                        stringResource(R.string.last_logged, Formatters.dateAndTime(context, it.timestamp))
                    },
                    containerColor = visuals.container,
                    contentColor = visuals.onContainer,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        viewModel.logWakeup()
                    },
                )
            }
            item(key = "energy") {
                val visuals = LogType.ENERGY.visuals()
                BigActionButton(
                    icon = visuals.icon,
                    label = stringResource(R.string.rate_energy),
                    supportingText = todayEnergy?.let { stringResource(R.string.today_energy, it) }
                        ?: stringResource(R.string.not_rated_today),
                    containerColor = visuals.container,
                    contentColor = visuals.onContainer,
                    onClick = { showEnergyPicker = true },
                )
            }
            item(key = "history_header") {
                Text(
                    text = stringResource(R.string.history),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 2.dp),
                )
            }
            if (loadedLogs != null && logs.isEmpty()) {
                item(key = "empty") { EmptyHistory() }
            }
            items(logs, key = { it.id }) { log ->
                LogRow(log = log, onClick = { editingId = log.id })
            }
        }
    }

    if (showEnergyPicker) {
        EnergyPicker(
            currentRating = todayEnergy,
            onRate = { rating ->
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.rateEnergy(rating)
                showEnergyPicker = false
            },
            onDismiss = { showEnergyPicker = false },
        )
    }

    val editing = editingId?.let { id -> logs.firstOrNull { it.id == id } }
    if (editing != null) {
        EditLogDialog(
            log = editing,
            onUpdate = viewModel::updateLog,
            onDelete = { log ->
                viewModel.deleteLog(log)
                editingId = null
            },
            onDismiss = { editingId = null },
        )
    }
}

@Composable
private fun BigActionButton(
    @DrawableRes icon: Int,
    label: String,
    supportingText: String?,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun LogRow(log: SleepLog, onClick: () -> Unit) {
    val context = LocalContext.current
    val visuals = log.type.visuals()
    val isEnergy = log.type == LogType.ENERGY
    val subtitle = if (isEnergy) Formatters.dateAndTime(context, log.timestamp) else Formatters.date(log.timestamp)
    val value = if (isEnergy) {
        stringResource(R.string.energy_value, log.value ?: 0)
    } else {
        Formatters.time(context, log.timestamp)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(visuals.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(visuals.icon),
                    contentDescription = null,
                    tint = visuals.onContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(visuals.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = visuals.accent,
            )
        }
    }
}

@Composable
private fun EmptyHistory() {
    Text(
        text = stringResource(R.string.empty_history),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
    )
}
