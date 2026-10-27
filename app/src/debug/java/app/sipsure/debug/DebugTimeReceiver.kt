package app.sipsure.debug

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import app.sipsure.platform.SipSureAdminReceiver

/** adb shell am broadcast -n app.sipsure/app.sipsure.debug.DebugTimeReceiver --ei minutes 31 */
class DebugTimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(context, SipSureAdminReceiver::class.java)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        dpm.setAutoTimeEnabled(admin, false)
        dpm.setTime(admin, System.currentTimeMillis() + intent.getIntExtra("minutes", 0) * 60_000L)
    }
}
