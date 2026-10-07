package com.sleeptracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SleepLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sleepLogDao(): SleepLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** One shared instance per process, so the UI and the watch listener see the same data. */
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sleep_tracker.db",
                ).build().also { instance = it }
            }
    }
}
