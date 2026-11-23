package com.offlinefirst.notes.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateFormatter {

    private val fullDateTimeFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    fun format(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val now = System.currentTimeMillis()
        val diffMillis = now - timestamp

        if (diffMillis < 0) {
            return fullDateTimeFormat.format(Date(timestamp))
        }

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> fullDateTimeFormat.format(Date(timestamp))
        }
    }

    fun formatFull(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return fullDateTimeFormat.format(Date(timestamp))
    }
}
