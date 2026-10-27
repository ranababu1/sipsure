package app.sipsure

import android.app.Application
import app.sipsure.data.PrefsStateStore
import app.sipsure.domain.HydrationController
import app.sipsure.platform.Notifier
import app.sipsure.platform.ReminderScheduler

/** Process-wide wiring. Alarms, boot receivers and the UI all share this single controller. */
class SipSureApp : Application() {
    lateinit var controller: HydrationController
        private set
    lateinit var notifier: Notifier
        private set
    private lateinit var scheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        notifier = Notifier(this).also { it.ensureChannel() }
        scheduler = ReminderScheduler(this)
        controller = HydrationController(PrefsStateStore(this), System::currentTimeMillis)
        controller.start()
        scheduler.reschedule(controller.state, System.currentTimeMillis())
    }

    /** Call after any controller action so the next alarm matches the new state. */
    fun onStateChanged() {
        scheduler.reschedule(controller.state, System.currentTimeMillis())
    }
}
