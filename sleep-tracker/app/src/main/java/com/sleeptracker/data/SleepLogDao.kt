package com.sleeptracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class EnergyWriteResult {
    /** No rating existed for that day; a new one was saved. */
    CREATED,

    /** A rating already existed for that day and was overwritten. */
    REPLACED,

    /** A newer rating already exists for that day, so this (older, delayed) one was ignored. */
    SKIPPED_OLDER,
}

@Dao
abstract class SleepLogDao {

    @Query("SELECT * FROM sleep_logs ORDER BY timestamp DESC, id DESC")
    abstract fun observeAll(): Flow<List<SleepLog>>

    @Query("SELECT * FROM sleep_logs ORDER BY timestamp ASC, id ASC")
    abstract suspend fun getAllChronological(): List<SleepLog>

    @Query("SELECT * FROM sleep_logs WHERE type = :type AND timestamp = :timestamp LIMIT 1")
    abstract suspend fun findExact(type: LogType, timestamp: Long): SleepLog?

    @Query(
        "SELECT * FROM sleep_logs WHERE type = 'ENERGY' " +
            "AND timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp DESC"
    )
    abstract suspend fun energyBetween(startMillis: Long, endMillis: Long): List<SleepLog>

    @Insert
    abstract suspend fun insert(log: SleepLog): Long

    @Update
    abstract suspend fun update(log: SleepLog)

    @Query("DELETE FROM sleep_logs WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    /** Inserts a bedtime/wake-up entry unless an identical one already exists (duplicate delivery). */
    @Transaction
    open suspend fun insertIfAbsent(type: LogType, timestamp: Long): SleepLog {
        findExact(type, timestamp)?.let { return it }
        val log = SleepLog(type = type, timestamp = timestamp)
        return log.copy(id = insert(log))
    }

    /**
     * Makes [rating] the single energy entry for the calendar day [dayStart, dayEnd).
     *
     * @param onlyIfNewest when true, a rating that is older than one already stored for the
     * day is ignored. Used for ratings the watch queued while the phone was out of reach.
     */
    @Transaction
    open suspend fun putEnergyForDay(
        rating: Int,
        timestamp: Long,
        dayStart: Long,
        dayEnd: Long,
        onlyIfNewest: Boolean,
    ): EnergyWriteResult {
        val existing = energyBetween(dayStart, dayEnd)
        if (onlyIfNewest && existing.any { it.timestamp > timestamp }) {
            return EnergyWriteResult.SKIPPED_OLDER
        }
        existing.forEach { deleteById(it.id) }
        insert(SleepLog(type = LogType.ENERGY, timestamp = timestamp, value = rating))
        return if (existing.isEmpty()) EnergyWriteResult.CREATED else EnergyWriteResult.REPLACED
    }

    /**
     * Saves an edited energy entry. Any other rating on the entry's (possibly new) day is removed
     * so there is still only one rating per day. Returns true if another rating was removed.
     */
    @Transaction
    open suspend fun updateEnergy(log: SleepLog, dayStart: Long, dayEnd: Long): Boolean {
        val others = energyBetween(dayStart, dayEnd).filter { it.id != log.id }
        others.forEach { deleteById(it.id) }
        update(log)
        return others.isNotEmpty()
    }
}
