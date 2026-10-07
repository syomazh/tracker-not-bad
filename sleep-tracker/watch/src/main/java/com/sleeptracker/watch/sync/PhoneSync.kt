package com.sleeptracker.watch.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Sends log events to the phone app over the Wear OS Data Layer.
 *
 * 1. If the phone is reachable, the event goes out right away with MessageClient.
 * 2. If not (out of Bluetooth range, phone off), it is stored as a DataClient item instead.
 *    The Data Layer syncs it automatically once the devices reconnect, so nothing is lost.
 *
 * Protocol (keep in sync with WatchSync.kt in the phone module):
 *   "/bedtime" -> "{timestamp}", "/wakeup" -> "{timestamp}", "/energy" -> "{rating}|{timestamp}"
 */
class PhoneSync(context: Context) {

    enum class Delivery {
        /** The phone received it now. */
        SENT,

        /** Saved on the watch; it will reach the phone when they reconnect. */
        QUEUED,
    }

    private val appContext = context.applicationContext
    private val capabilityClient = Wearable.getCapabilityClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val dataClient = Wearable.getDataClient(appContext)

    suspend fun logBedtime(timestamp: Long): Delivery = send(PATH_BEDTIME, timestamp.toString())

    suspend fun logWakeup(timestamp: Long): Delivery = send(PATH_WAKEUP, timestamp.toString())

    suspend fun logEnergy(rating: Int, timestamp: Long): Delivery {
        require(rating in 1..5) { "Energy rating must be 1-5, was $rating" }
        return send(PATH_ENERGY, "$rating|$timestamp")
    }

    /** Throws only if the event could not even be saved locally. */
    private suspend fun send(path: String, payload: String): Delivery {
        val phone = findPhone()
        if (phone != null) {
            try {
                val sent = withTimeoutOrNull(SEND_TIMEOUT_MS) {
                    messageClient.sendMessage(phone.id, path, payload.toByteArray(Charsets.UTF_8)).await()
                }
                if (sent != null) return Delivery.SENT
                Log.w(TAG, "Message to phone timed out, queueing instead")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Message to phone failed, queueing instead", e)
            }
        }
        // The phone ignores duplicates, so queueing after a slow send is safe.
        queue(path, payload)
        return Delivery.QUEUED
    }

    /** The phone that has Sleep Tracker installed and is reachable right now, if any. */
    private suspend fun findPhone(): Node? =
        try {
            withTimeoutOrNull(FIND_TIMEOUT_MS) {
                capabilityClient
                    .getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE)
                    .await()
                    .nodes
                    .sortedByDescending { it.isNearby }
                    .firstOrNull()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not look up the phone", e)
            null
        }

    private suspend fun queue(path: String, payload: String) {
        val request = PutDataMapRequest.create("$PENDING_PREFIX${UUID.randomUUID()}").apply {
            dataMap.putString(KEY_PATH, path)
            dataMap.putString(KEY_PAYLOAD, payload)
        }.asPutDataRequest().setUrgent()
        val stored = withTimeoutOrNull(QUEUE_TIMEOUT_MS) { dataClient.putDataItem(request).await() }
        checkNotNull(stored) { "Timed out saving the event on the watch" }
    }

    private companion object {
        const val TAG = "PhoneSync"
        const val CAPABILITY_PHONE_APP = "sleeptracker_phone"
        const val PATH_BEDTIME = "/bedtime"
        const val PATH_WAKEUP = "/wakeup"
        const val PATH_ENERGY = "/energy"
        const val PENDING_PREFIX = "/pending/"
        const val KEY_PATH = "path"
        const val KEY_PAYLOAD = "payload"
        const val FIND_TIMEOUT_MS = 3_000L
        const val SEND_TIMEOUT_MS = 5_000L
        const val QUEUE_TIMEOUT_MS = 10_000L
    }
}
