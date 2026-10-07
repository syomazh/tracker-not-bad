package com.sleeptracker.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

/** Single place for the app's logging rules. Used by both the phone UI and the watch listener. */
class SleepRepository(private val dao: SleepLogDao) {

    /** All entries, newest first. */
    val logs: Flow<List<SleepLog>> = dao.observeAll()

    suspend fun logSleepEvent(type: LogType, timestamp: Long): SleepLog {
        require(type != LogType.ENERGY) { "Use rateEnergy for energy ratings" }
        return dao.insertIfAbsent(type, timestamp)
    }

    /** Saves the rating for the calendar day of [timestamp], overwriting that day's rating. */
    suspend fun rateEnergy(
        rating: Int,
        timestamp: Long,
        onlyIfNewest: Boolean = false,
    ): EnergyWriteResult {
        require(rating in 1..5) { "Energy rating must be 1-5, was $rating" }
        val (start, end) = dayBounds(timestamp)
        return dao.putEnergyForDay(rating, timestamp, start, end, onlyIfNewest)
    }

    /** Saves an edited entry. Returns true if the edit replaced another energy rating that day. */
    suspend fun update(log: SleepLog): Boolean =
        if (log.type == LogType.ENERGY) {
            val (start, end) = dayBounds(log.timestamp)
            dao.updateEnergy(log, start, end)
        } else {
            dao.update(log)
            false
        }

    suspend fun delete(log: SleepLog) = dao.deleteById(log.id)

    suspend fun allChronological(): List<SleepLog> = dao.getAllChronological()

    companion object {
        fun get(context: Context): SleepRepository =
            SleepRepository(AppDatabase.get(context).sleepLogDao())

        /** Start (inclusive) and end (exclusive) epoch millis of the local calendar day. */
        fun dayBounds(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
            val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return start to end
        }
    }
}
