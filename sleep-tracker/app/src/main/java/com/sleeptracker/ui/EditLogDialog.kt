package com.sleeptracker.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sleeptracker.R
import com.sleeptracker.data.LogType
import com.sleeptracker.data.SleepLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

private enum class EditStep { MENU, TIME, DATE, CONFIRM_DELETE }

/**
 * Shown when an entry in the list is tapped. Lets the user change the time (and date, for
 * entries logged after midnight by mistake), change an energy rating, or delete the entry.
 * Every change is saved immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLogDialog(
    log: SleepLog,
    onUpdate: (SleepLog) -> Unit,
    onDelete: (SleepLog) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(log.timestamp).atZone(zone)
    val visuals = log.type.visuals()
    var step by rememberSaveable { mutableStateOf(EditStep.MENU) }

    when (step) {
        EditStep.MENU -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(painter = painterResource(visuals.icon), contentDescription = null, tint = visuals.accent)
            },
            title = { Text(stringResource(R.string.edit_title, visuals.label)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditRow(
                        label = stringResource(R.string.edit_time),
                        value = Formatters.time(context, log.timestamp),
                        onClick = { step = EditStep.TIME },
                    )
                    EditRow(
                        label = stringResource(R.string.edit_date),
                        value = Formatters.date(log.timestamp),
                        onClick = { step = EditStep.DATE },
                    )
                    if (log.type == LogType.ENERGY) {
                        Text(
                            text = stringResource(R.string.edit_rating),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        )
                        EnergyScale(
                            selected = log.value,
                            onSelect = { rating -> if (rating != log.value) onUpdate(log.copy(value = rating)) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { step = EditStep.CONFIRM_DELETE },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.delete))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )

        EditStep.TIME -> TimeSelectDialog(
            initial = dateTime,
            onConfirm = { hour, minute ->
                val updated = dateTime.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                onUpdate(log.copy(timestamp = updated.toInstant().toEpochMilli()))
                step = EditStep.MENU
            },
            onDismiss = { step = EditStep.MENU },
        )

        EditStep.DATE -> DateSelectDialog(
            initial = dateTime.toLocalDate(),
            onConfirm = { date ->
                val updated = ZonedDateTime.of(date, dateTime.toLocalTime(), zone)
                onUpdate(log.copy(timestamp = updated.toInstant().toEpochMilli()))
                step = EditStep.MENU
            },
            onDismiss = { step = EditStep.MENU },
        )

        EditStep.CONFIRM_DELETE -> AlertDialog(
            onDismissRequest = { step = EditStep.MENU },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_confirm_body,
                        visuals.label,
                        Formatters.dateAndTime(context, log.timestamp),
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(log) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { step = EditStep.MENU }) { Text(stringResource(R.string.cancel)) }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

@Composable
private fun EditRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectDialog(
    initial: ZonedDateTime,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.select_time),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                )
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // The Material date picker works in UTC midnight millis, independent of the local time zone.
    val today = LocalDate.now()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis == null) {
                        onDismiss()
                    } else {
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
