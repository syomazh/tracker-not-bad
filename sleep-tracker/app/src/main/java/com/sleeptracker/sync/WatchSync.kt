package com.sleeptracker.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.sleeptracker.data.LogType
import com.sleeptracker.data.SleepRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Phone side of the watch protocol.
 *
 * Live messages (MessageClient), sent when the phone is reachable:
 *   path "/bedtime"  data "{timestamp}"
 *   path "/wakeup"   data "{timestamp}"
 *   path "/energy"   data "{rating}|{timestamp}"
 *
 * Queued events (DataClient), used by the watch when the phone is NOT reachable:
 *   data item "/pending/{uuid}" with string fields "path" and "payload" holding the same values.
 *   The Data Layer delivers them once the devices reconnect; the phone applies and deletes them.
 *
 * Keep these constants in sync with PhoneSync.kt in the watch module.
 */
object WatchSync {
    const val PATH_BEDTIME = "/bedtime"
    const val PATH_WAKEUP = "/wakeup"
    const val PATH_ENERGY = "/energy"
    const val PENDING_PREFIX = "/pending/"
    const val KEY_PATH = "path"
    const val KEY_PAYLOAD = "payload"

    private const val TAG = "WatchSync"
    private const val PREFS = "watch_sync"
    private const val PREF_PROCESSED_IDS = "processed_pending_ids"
    private const val DATA_LAYER_TIMEOUT_MS = 10_000L

    /** Serializes writes coming from the listener service and from the app at the same time. */
    private val writeLock = Mutex()

    sealed interface WatchEvent {
        data class Sleep(val type: LogType, val timestamp: Long) : WatchEvent
        data class Energy(val rating: Int, val timestamp: Long) : WatchEvent
    }

    data class PendingItem(val uri: Uri, val path: String, val payload: String)

    /** Parses a path plus payload. Returns null for anything malformed. */
    fun parse(path: String, payload: String): WatchEvent? {
        val parts = payload.trim().split("|")
        return when (path) {
            PATH_BEDTIME -> parts.firstOrNull()?.toLongOrNull()?.let { WatchEvent.Sleep(LogType.BEDTIME, it) }
            PATH_WAKEUP -> parts.firstOrNull()?.toLongOrNull()?.let { WatchEvent.Sleep(LogType.WAKEUP, it) }
            PATH_ENERGY -> {
                val rating = parts.getOrNull(0)?.toIntOrNull()
                val timestamp = parts.getOrNull(1)?.toLongOrNull()
                if (rating != null && rating in 1..5 && timestamp != null) {
                    WatchEvent.Energy(rating, timestamp)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private suspend fun apply(repository: SleepRepository, event: WatchEvent, queued: Boolean) {
        when (event) {
            is WatchEvent.Sleep -> repository.logSleepEvent(event.type, event.timestamp)
            // A queued rating may arrive hours late. Never let it overwrite a newer rating.
            is WatchEvent.Energy -> repository.rateEnergy(event.rating, event.timestamp, onlyIfNewest = queued)
        }
    }

    suspend fun handleMessage(context: Context, path: String, data: ByteArray) {
        val event = parse(path, String(data, Charsets.UTF_8))
        if (event == null) {
            Log.w(TAG, "Ignoring malformed watch message on $path")
            return
        }
        writeLock.withLock { apply(SleepRepository.get(context), event, queued = false) }
    }

    /** Reads a queued event out of a data item. Must be called while the item's buffer is open. */
    fun toPendingItem(item: DataItem): PendingItem? {
        val uri = item.uri
        if (uri.path?.startsWith(PENDING_PREFIX) != true) return null
        val map = DataMapItem.fromDataItem(item).dataMap
        val path = map.getString(KEY_PATH) ?: return null
        val payload = map.getString(KEY_PAYLOAD) ?: return null
        return PendingItem(uri, path, payload)
    }

    suspend fun handlePending(context: Context, items: List<PendingItem>) {
        if (items.isEmpty()) return
        val repository = SleepRepository.get(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        writeLock.withLock {
            val processed = prefs.getStringSet(PREF_PROCESSED_IDS, emptySet()).orEmpty().toMutableSet()
            for (item in items) {
                val id = item.uri.lastPathSegment ?: continue
                if (id in processed) continue
                val event = parse(item.path, item.payload)
                if (event != null) {
                    apply(repository, event, queued = true)
                } else {
                    Log.w(TAG, "Ignoring malformed queued event ${item.path}")
                }
                processed += id
                prefs.edit(commit = true) { putStringSet(PREF_PROCESSED_IDS, processed.toSet()) }
            }
        }

        // Remove the items so they stop syncing. If this fails, the processed-id list above
        // still prevents the same event from being counted twice.
        for (item in items) {
            try {
                val dataClient = Wearable.getDataClient(context)
                withTimeoutOrNull(DATA_LAYER_TIMEOUT_MS) { dataClient.deleteDataItems(item.uri).await() }
                    ?: Log.w(TAG, "Timed out deleting queued item ${item.uri}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete queued item ${item.uri}", e)
            }
        }
    }

    /**
     * Picks up queued watch events that are already on the phone, for example ones that synced
     * before the phone app was installed. Safe to call often. Does nothing without a watch.
     */
    suspend fun syncQueued(context: Context) {
        try {
            val dataClient = Wearable.getDataClient(context)
            val uri = Uri.Builder()
                .scheme(PutDataRequest.WEAR_URI_SCHEME)
                .path(PENDING_PREFIX)
                .build()
            val buffer = withTimeoutOrNull(DATA_LAYER_TIMEOUT_MS) {
                dataClient.getDataItems(uri, DataClient.FILTER_PREFIX).await()
            } ?: return
            val items = try {
                buffer.mapNotNull(::toPendingItem)
            } finally {
                buffer.release()
            }
            handlePending(context, items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Normal on a phone with no watch set up. The phone app works fine on its own.
            Log.i(TAG, "Watch sync skipped: ${e.message}")
        }
    }
}
