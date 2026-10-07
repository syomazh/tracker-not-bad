package com.sleeptracker.ui

import android.content.Context
import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import android.icu.text.DateFormat as IcuDateFormat

/** Locale-aware date and time text that follows the phone's 12/24-hour setting. */
object Formatters {

    fun time(context: Context, millis: Long): String {
        val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
        return format(skeleton, millis)
    }

    /** For example "Tue, Oct 7". The year is added for dates outside the current year. */
    fun date(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val sameYear = Instant.ofEpochMilli(millis).atZone(zone).year == LocalDate.now(zone).year
        return format(if (sameYear) "EEEMMMd" else "EEEMMMdyyyy", millis)
    }

    fun dateAndTime(context: Context, millis: Long): String = "${date(millis)}, ${time(context, millis)}"

    fun isToday(millis: Long): Boolean {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate() == LocalDate.now(zone)
    }

    private fun format(skeleton: String, millis: Long): String =
        IcuDateFormat.getInstanceForSkeleton(skeleton, Locale.getDefault()).format(Date(millis))
}
