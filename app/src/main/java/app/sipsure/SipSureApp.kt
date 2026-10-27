package app.sipsure

import android.app.Application
import android.content.Intent
import app.sipsure.data.PrefsStateStore
import app.sipsure.domain.HydrationController
import app.sipsure.platform.DeviceLock
import app.sipsure.platform.Notifier
import app.sipsure.platform.ReminderScheduler

/** Process-wide wiring. Alarms, boot receivers and the UI all share this single controller. */
class SipSureApp : Application() {
    lateinit var controller: HydrationController
        private set
    lateinit var notifier: Notifier
        private set
    lateinit var deviceLock: DeviceLock
        private set
    private lateinit var scheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        notifier = Notifier(this).also { it.ensureChannel() }
        scheduler = ReminderScheduler(this)
        deviceLock = DeviceLock(this)
        controller = HydrationController(
            PrefsStateStore(this), System::currentTimeMillis,
            deviceEnforcementSupported = deviceLock::isAvailable,
        )
        controller.start()
        scheduler.reschedule(controller.state, System.currentTimeMillis())
    }

    /** Call after any controller action so the next alarm matches the new state. */
    fun onStateChanged() {
        scheduler.reschedule(controller.state, System.currentTimeMillis())
        deviceLock.setUninstallBlocked(controller.hasUnpaidAdvance())
    }

    /** Brings the app forward so the phone lock can engage; only attempted when a lock is actually due. */
    fun bringToFrontIfLockDue() {
        if (!controller.deviceLockActive) return
        try {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: RuntimeException) {
            // Background launch can be refused; the notification and the next app open still apply the lock.
        }
    }
}
