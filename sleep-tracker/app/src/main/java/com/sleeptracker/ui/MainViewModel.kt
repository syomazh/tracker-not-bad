package com.sleeptracker.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleeptracker.R
import com.sleeptracker.data.EnergyWriteResult
import com.sleeptracker.data.LogType
import com.sleeptracker.data.SleepLog
import com.sleeptracker.data.SleepRepository
import com.sleeptracker.export.CsvExporter
import com.sleeptracker.sync.WatchSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** One-off things the screen should do: show a snackbar or open the share sheet. */
sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class Share(val uri: Uri) : UiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SleepRepository.get(application)
    private val app: Application get() = getApplication()

    /** All entries, newest first. Null until the database has loaded. */
    val logs: StateFlow<List<SleepLog>?> = repository.logs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    fun logBedtime() = logSleep(LogType.BEDTIME, R.string.logged_bedtime)

    fun logWakeup() = logSleep(LogType.WAKEUP, R.string.logged_wakeup)

    private fun logSleep(type: LogType, @StringRes message: Int) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.logSleepEvent(type, now)
            say(app.getString(message, Formatters.time(app, now)))
        }
    }

    fun rateEnergy(rating: Int) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val message = when (repository.rateEnergy(rating, now)) {
                EnergyWriteResult.REPLACED -> R.string.energy_replaced
                else -> R.string.energy_saved
            }
            say(app.getString(message, rating))
        }
    }

    fun updateLog(log: SleepLog) {
        viewModelScope.launch {
            val replacedOther = repository.update(log)
            say(app.getString(if (replacedOther) R.string.entry_updated_replaced else R.string.entry_updated))
        }
    }

    fun deleteLog(log: SleepLog) {
        viewModelScope.launch {
            repository.delete(log)
            say(app.getString(R.string.entry_deleted))
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val all = repository.allChronological()
            if (all.isEmpty()) {
                say(app.getString(R.string.nothing_to_export))
                return@launch
            }
            try {
                val uri = withContext(Dispatchers.IO) { CsvExporter.writeCsv(app, all) }
                _events.send(UiEvent.Share(uri))
            } catch (e: IOException) {
                Log.e("MainViewModel", "CSV export failed", e)
                say(app.getString(R.string.export_failed, e.localizedMessage ?: e.javaClass.simpleName))
            }
        }
    }

    /** Applies any watch events that were queued while the phone was unreachable. */
    fun syncFromWatch() {
        viewModelScope.launch { WatchSync.syncQueued(app) }
    }

    private suspend fun say(text: String) = _events.send(UiEvent.Message(text))
}
