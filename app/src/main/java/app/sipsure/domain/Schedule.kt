package app.sipsure.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * User-editable hydration schedule. Bounds stop accidental schedules with excessive reminders
 * (plan.md 5.1): intervals under 30 minutes are rejected.
 */
data class Schedule(
    val intervalMinutes: Int = 120,
    val checkInWindowMinutes: Int = 15,
    val graceMinutes: Int = 10,
    /** Minutes after local midnight; quiet hours may wrap past midnight. Equal values mean no quiet hours. */
    val quietStartMinuteOfDay: Int = 22 * 60,
    val quietEndMinuteOfDay: Int = 7 * 60,
) {
    fun validationError(): String? = when {
        intervalMinutes !in MIN_INTERVAL..MAX_INTERVAL ->
            "Reminder interval must be between $MIN_INTERVAL and $MAX_INTERVAL minutes."
        checkInWindowMinutes !in 5..60 -> "Check-in window must be between 5 and 60 minutes."
        graceMinutes !in 0..60 -> "Grace period must be between 0 and 60 minutes."
        checkInWindowMinutes >= intervalMinutes -> "Check-in window must be shorter than the interval."
        quietStartMinuteOfDay !in 0 until MINUTES_PER_DAY || quietEndMinuteOfDay !in 0 until MINUTES_PER_DAY ->
            "Quiet hours must be valid times of day."
        else -> null
    }

    fun toPolicyConfig(deviceEnforcementRequested: Boolean = false) = PolicyConfig(
        checkInWindowMs = checkInWindowMinutes * MS_PER_MINUTE,
        graceMs = graceMinutes * MS_PER_MINUTE,
        deviceEnforcementRequested = deviceEnforcementRequested,
        deviceEnforcementSupported = false,
    )

    private val hasQuietHours get() = quietStartMinuteOfDay != quietEndMinuteOfDay

    fun isQuiet(at: ZonedDateTime): Boolean {
        if (!hasQuietHours) return false
        val minute = at.toLocalTime().toSecondOfDay() / 60
        return if (quietStartMinuteOfDay < quietEndMinuteOfDay) {
            minute in quietStartMinuteOfDay until quietEndMinuteOfDay
        } else {
            minute >= quietStartMinuteOfDay || minute < quietEndMinuteOfDay
        }
    }

    /** Next due time one interval after [afterMs], moved to the end of quiet hours when it lands inside them. */
    fun nextDueAfter(afterMs: Long, zone: ZoneId): Long {
        val candidate = Instant.ofEpochMilli(afterMs + intervalMinutes * MS_PER_MINUTE).atZone(zone)
        if (!isQuiet(candidate)) return candidate.toInstant().toEpochMilli()
        val end = LocalTime.of(quietEndMinuteOfDay / 60, quietEndMinuteOfDay % 60)
        var wake = candidate.with(end)
        if (!wake.isAfter(candidate)) wake = wake.plusDays(1)
        return wake.toInstant().toEpochMilli()
    }

    companion object {
        const val MIN_INTERVAL = 30
        const val MAX_INTERVAL = 360
        const val MINUTES_PER_DAY = 24 * 60
        const val MS_PER_MINUTE = 60_000L
    }
}
