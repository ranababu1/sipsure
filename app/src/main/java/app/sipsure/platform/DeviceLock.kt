package app.sipsure.platform

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.telecom.TelecomManager

/** Empty admin receiver; it only exists so the app can be made device owner for lock-task mode. */
class SipSureAdminReceiver : DeviceAdminReceiver()

/**
 * Whole-device lock through Android lock-task mode. This is a supported API that only works when the
 * app is the device owner (set up deliberately over adb or at provisioning), so ordinary installs never
 * get it. While locked, only SipSure, the phone dialer (emergency and incoming calls), the camera app
 * and the permission dialog are allowed to run.
 */
class DeviceLock(private val context: Context) {
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, SipSureAdminReceiver::class.java)

    fun isAvailable(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    /** Allowlists the packages needed to check in and to call for help. Call before startLockTask(). */
    fun prepare() {
        if (!isAvailable()) return
        val packages = setOf(context.packageName) + essentialPackages()
        dpm.setLockTaskPackages(admin, packages.toTypedArray())
        // Status-bar info only: no notifications shade, home or recents while locked.
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO)
    }

    /** Gives up device ownership so SipSure can be uninstalled normally. Never call while locked. */
    fun removeCapability() {
        if (isAvailable()) {
            dpm.setLockTaskPackages(admin, emptyArray())
            @Suppress("DEPRECATION")
            dpm.clearDeviceOwnerApp(context.packageName)
        }
    }

    private fun essentialPackages(): Set<String> {
        val pm = context.packageManager
        val result = mutableSetOf<String>()
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let(result::add)
        val dial = pm.queryIntentActivities(Intent(Intent.ACTION_DIAL), PackageManager.MATCH_DEFAULT_ONLY)
        dial.mapTo(result) { it.activityInfo.packageName }
        val camera = pm.queryIntentActivities(Intent(MediaStore.ACTION_IMAGE_CAPTURE), PackageManager.MATCH_DEFAULT_ONLY)
        camera.mapTo(result) { it.activityInfo.packageName }
        // The camera app may need to show a permission prompt.
        result += setOf("com.google.android.permissioncontroller", "com.android.permissioncontroller")
        return result
    }
}
