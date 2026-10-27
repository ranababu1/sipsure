package app.sipsure.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.sipsure.domain.AppState
import app.sipsure.domain.Phase

/**
 * Keeps exactly one pending alarm, set for the next moment the policy needs to re-evaluate.
 * Uses an inexact, doze-friendly alarm so no exact-alarm permission is required; reminders may
 * arrive a few minutes late on some devices, and the app re-checks time whenever it is opened.
 */
class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun reschedule(state: AppState, nowMs: Long) {
        val pending = pendingIntent()
        val at = nextWakeUp(state, nowMs)
        if (at == null) alarms.cancel(pending) else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    private fun pendingIntent() = PendingIntent.getBroadcast(
        context, 0, Intent(context, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        /** Earliest future time at which state could change; null when nothing is time-driven. */
        fun nextWakeUp(state: AppState, nowMs: Long): Long? {
            val p = state.policy
            if (p.phase == Phase.PAUSED || p.phase == Phase.SETUP_REQUIRED || p.phase == Phase.RECOVERY_REQUIRED) return null
            val advanceDeadlines = state.ledger.outstandingAdvances(nowMs).map { it.dueAtMs }
            return (listOfNotNull(p.nextDueAtMs, p.windowEndsAtMs, p.graceEndsAtMs, p.bypassUntilMs) + advanceDeadlines)
                .filter { it > nowMs }
                .minOrNull()
        }
    }
}
