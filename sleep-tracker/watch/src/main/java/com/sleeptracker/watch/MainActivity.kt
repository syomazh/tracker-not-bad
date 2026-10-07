package com.sleeptracker.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sleeptracker.watch.sync.PhoneSync
import com.sleeptracker.watch.ui.WatchApp
import com.sleeptracker.watch.ui.theme.SleepTrackerWatchTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phoneSync = PhoneSync(applicationContext)
        setContent {
            SleepTrackerWatchTheme {
                WatchApp(phoneSync)
            }
        }
    }
}
