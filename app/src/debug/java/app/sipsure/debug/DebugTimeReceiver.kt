package app.sipsure.debug

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import app.sipsure.platform.SipSureAdminReceiver

/**
 * Debug builds only.
 *   adb shell am broadcast -n app.sipsure/app.sipsure.debug.DebugTimeReceiver --ei minutes 31   (move the clock, device owner only)
 *   adb shell am broadcast -n app.sipsure/app.sipsure.debug.DebugTimeReceiver --ez reset true   (wipe saved state, restart process)
 */
class DebugTimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra("reset", false)) {
            context.getSharedPreferences("sipsure_state", Context.MODE_PRIVATE).edit().clear().commit()
            Process.killProcess(Process.myPid())
            return
        }
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(context, SipSureAdminReceiver::class.java)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        dpm.setAutoTimeEnabled(admin, false)
        dpm.setTime(admin, System.currentTimeMillis() + intent.getIntExtra("minutes", 0) * 60_000L)
    }
}
