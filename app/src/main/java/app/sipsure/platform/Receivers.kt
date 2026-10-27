package app.sipsure.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.sipsure.SipSureApp
import app.sipsure.domain.Phase

/** Fires when the policy needs to re-evaluate time (due, window end, grace end, bypass end, advance deadline). */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SipSureApp
        val before = app.controller.state.policy.phase
        app.controller.tick()
        val after = app.controller.state.policy.phase
        if (after != before) app.notifier.show(after)
        app.onStateChanged()
    }
}

/** Reboot and clock/time-zone changes: recompute, without punishing reminders the phone could not deliver. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SipSureApp
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED ->
                app.controller.onSystemRecovered()
        }
        val phase = app.controller.state.policy.phase
        if (phase == Phase.CHECK_IN_DUE || phase == Phase.GRACE_PERIOD) app.notifier.show(phase)
        app.onStateChanged()
    }
}
