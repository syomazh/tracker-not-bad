package com.sleeptracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogType { BEDTIME, WAKEUP, ENERGY }

/**
 * One row in the `sleep_logs` table.
 *
 * @property timestamp epoch millis of the event.
 * @property value energy rating 1-5 for [LogType.ENERGY]; null for bedtime and wake-up.
 */
@Entity(tableName = "sleep_logs")
data class SleepLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: LogType,
    val timestamp: Long,
    val value: Int? = null,
)
