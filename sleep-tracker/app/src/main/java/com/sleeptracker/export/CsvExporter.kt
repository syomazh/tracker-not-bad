package com.sleeptracker.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.sleeptracker.R
import com.sleeptracker.data.LogType
import com.sleeptracker.data.SleepLog
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Writes all entries to a CSV file with the columns `date,type,time,value`
 * and hands it to the Android share sheet.
 */
object CsvExporter {

    private const val HEADER = "date,type,time,value"
    private const val EXPORT_DIR = "exports"
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    /** Builds the CSV text. Rows are oldest first, which suits spreadsheets and analysis. */
    fun buildCsv(logs: List<SleepLog>, zone: ZoneId = ZoneId.systemDefault()): String =
        buildString {
            append(HEADER).append("\r\n")
            for (log in logs.sortedWith(compareBy(SleepLog::timestamp, SleepLog::id))) {
                val dateTime = Instant.ofEpochMilli(log.timestamp).atZone(zone)
                append(DATE_FORMAT.format(dateTime)).append(',')
                append(typeName(log.type)).append(',')
                append(TIME_FORMAT.format(dateTime)).append(',')
                if (log.type == LogType.ENERGY && log.value != null) append(log.value)
                append("\r\n")
            }
        }

    private fun typeName(type: LogType): String = when (type) {
        LogType.BEDTIME -> "bedtime"
        LogType.WAKEUP -> "wakeup"
        LogType.ENERGY -> "energy"
    }

    /** Writes the CSV into the app cache and returns a shareable content:// URI. Call off the main thread. */
    fun writeCsv(context: Context, logs: List<SleepLog>): Uri {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        // Only keep the latest export around.
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "sleep-tracker-${LocalDate.now()}.csv")
        file.writeText(buildCsv(logs), Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(context: Context, uri: Uri): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_subject))
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, context.getString(R.string.export_chooser_title))
    }
}
