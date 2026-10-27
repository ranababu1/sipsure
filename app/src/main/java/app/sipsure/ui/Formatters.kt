package app.sipsure.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val clockFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val dateTimeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

fun formatClock(ms: Long): String = clockFormat.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

fun formatDateTime(ms: Long): String = dateTimeFormat.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

/** Big number and unit for a countdown, e.g. ("28", "min") or ("1:12", "hours"). */
fun countdownParts(remainingMs: Long): Pair<String, String> {
    val totalSeconds = (remainingMs.coerceAtLeast(0) + 999) / 1000
    return when {
        totalSeconds < 60 -> totalSeconds.toString() to "sec"
        totalSeconds < 3600 -> ((totalSeconds + 59) / 60).toString() to "min"
        else -> {
            val minutes = (totalSeconds + 59) / 60
            "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}" to "hours"
        }
    }
}

fun hourLabel(hour: Int): String = "${hour.toString().padStart(2, '0')}:00"

fun intervalLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
