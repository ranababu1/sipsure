package app.sipsure.domain

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {
    private val utc = ZoneId.of("UTC")
    private val berlin = ZoneId.of("Europe/Berlin")
    private fun ms(z: ZoneId, y: Int, mo: Int, d: Int, h: Int, mi: Int = 0) =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, z).toInstant().toEpochMilli()

    private val day = Schedule(intervalMinutes = 120, quietStartMinuteOfDay = 22 * 60, quietEndMinuteOfDay = 7 * 60)

    @Test
    fun defaultsAreValid() = assertNull(Schedule().validationError())

    @Test
    fun rejectsExcessiveOrBrokenSettings() {
        assertNotNull(Schedule(intervalMinutes = 10).validationError())
        assertNotNull(Schedule(intervalMinutes = 1000).validationError())
        assertNotNull(Schedule(intervalMinutes = 60, checkInWindowMinutes = 60).validationError())
        assertNotNull(Schedule(graceMinutes = 90).validationError())
        assertNotNull(Schedule(quietStartMinuteOfDay = 2000).validationError())
    }

    @Test
    fun nextDueIsOneIntervalLaterOutsideQuietHours() {
        val now = ms(utc, 2026, 1, 1, 9)
        assertEquals(ms(utc, 2026, 1, 1, 11), day.nextDueAfter(now, utc))
    }

    @Test
    fun dueInsideQuietHoursMovesToWakeTime() {
        assertEquals(ms(utc, 2026, 1, 2, 7), day.nextDueAfter(ms(utc, 2026, 1, 1, 21), utc))
        assertEquals(ms(utc, 2026, 1, 2, 7), day.nextDueAfter(ms(utc, 2026, 1, 2, 3), utc))
    }

    @Test
    fun quietHoursWithoutWrapAndDisabledCase() {
        val s = Schedule(quietStartMinuteOfDay = 13 * 60, quietEndMinuteOfDay = 14 * 60, intervalMinutes = 60)
        assertTrue(s.isQuiet(ZonedDateTime.of(2026, 1, 1, 13, 30, 0, 0, utc)))
        assertFalse(s.isQuiet(ZonedDateTime.of(2026, 1, 1, 14, 0, 0, 0, utc)))
        val none = Schedule(quietStartMinuteOfDay = 0, quietEndMinuteOfDay = 0)
        assertFalse(none.isQuiet(ZonedDateTime.of(2026, 1, 1, 3, 0, 0, 0, utc)))
    }

    @Test
    fun timeZoneChangesQuietHoursInterpretation() {
        val instant = ms(utc, 2026, 1, 1, 21) // 22:00 in Berlin (winter): quiet there, not in UTC
        assertEquals(ms(utc, 2026, 1, 1, 23), Schedule(intervalMinutes = 120).copy(quietStartMinuteOfDay = 0, quietEndMinuteOfDay = 0).nextDueAfter(instant, utc))
        assertEquals(ms(berlin, 2026, 1, 2, 7), day.nextDueAfter(instant, berlin))
    }

    @Test
    fun policyConfigUsesScheduleDurations() {
        val c = Schedule(checkInWindowMinutes = 20, graceMinutes = 5).toPolicyConfig()
        assertEquals(20 * 60_000L, c.checkInWindowMs)
        assertEquals(5 * 60_000L, c.graceMs)
        assertFalse(c.deviceEnforcementSupported)
    }
}
