package com.sleeptracker.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

/**
 * Receives events from the watch app, even when the phone app is closed.
 *
 * Google Play services calls these methods on a background thread, so blocking until the
 * database write finishes is safe and guarantees nothing is lost if the service stops right after.
 */
class WatchListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        runBlocking {
            WatchSync.handleMessage(applicationContext, messageEvent.path, messageEvent.data)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Copy what we need now. The buffer is released as soon as this method returns.
        val items = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .mapNotNull { WatchSync.toPendingItem(it.dataItem) }
        if (items.isNotEmpty()) {
            runBlocking { WatchSync.handlePending(applicationContext, items) }
        }
    }
}
