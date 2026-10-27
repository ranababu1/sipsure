package app.sipsure.platform

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager

/** Empty admin receiver; it only exists so the app can be made device owner for lock-task mode. */
class SipSureAdminReceiver : DeviceAdminReceiver()

/**
 * Whole-device lock through Android lock-task mode. This is a supported API that only works when the
 * app is the device owner (set up deliberately over adb or at provisioning), so ordinary installs never
 * get it. While locked, only SipSure and the phone dialer (emergency and incoming calls) may run.
 */
class DeviceLock(private val context: Context) {
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, SipSureAdminReceiver::class.java)
    private var uninstallBlocked: Boolean? = null

    fun isAvailable(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    /** Allowlists the dialer so emergency calls always work. Call before startLockTask(). */
    fun prepare() {
        if (!isAvailable()) return
        val packages = setOf(context.packageName) + dialerPackages()
        dpm.setLockTaskPackages(admin, packages.toTypedArray())
        // Status-bar info only: no notifications shade, home or recents while locked.
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO)
    }

    /**
     * Stops SipSure being uninstalled while a borrowed credit is unpaid, so the loan can't be dodged.
     * Only possible as device owner; on an ordinary install nothing can (or should) block uninstalling.
     */
    fun setUninstallBlocked(blocked: Boolean) {
        if (!isAvailable() || uninstallBlocked == blocked) return
        dpm.setUninstallBlocked(admin, context.packageName, blocked)
        uninstallBlocked = blocked
    }

    /** Gives up device ownership so SipSure can be uninstalled. Callers must check the controller's blocker first. */
    fun removeCapability() {
        if (isAvailable()) {
            dpm.setUninstallBlocked(admin, context.packageName, false)
            dpm.setLockTaskPackages(admin, emptyArray())
            @Suppress("DEPRECATION")
            dpm.clearDeviceOwnerApp(context.packageName)
        }
    }

    private fun dialerPackages(): Set<String> {
        val result = mutableSetOf<String>()
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let(result::add)
        context.packageManager.queryIntentActivities(Intent(Intent.ACTION_DIAL), PackageManager.MATCH_DEFAULT_ONLY)
            .mapTo(result) { it.activityInfo.packageName }
        return result
    }
}
