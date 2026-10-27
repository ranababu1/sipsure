package app.sipsure

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.content.FileProvider
import app.sipsure.domain.AdvanceStatus
import app.sipsure.domain.AdvanceTerms
import app.sipsure.domain.AppState
import app.sipsure.domain.CheckInOutcome
import app.sipsure.domain.CheckInVerdict
import app.sipsure.domain.HistoryKind
import app.sipsure.domain.HydrationController
import app.sipsure.domain.LedgerEntry
import app.sipsure.domain.Phase
import app.sipsure.domain.PhotoCheck
import app.sipsure.domain.PhotoFeatures
import app.sipsure.domain.Schedule
import app.sipsure.platform.PhotoAnalyzer
import app.sipsure.ui.AMBER
import app.sipsure.ui.CALM_BLUE
import app.sipsure.ui.NEUTRAL
import app.sipsure.ui.SOFT_GREEN
import app.sipsure.ui.SOFT_RED
import app.sipsure.ui.body
import app.sipsure.ui.button
import app.sipsure.ui.caption
import app.sipsure.ui.card
import app.sipsure.ui.checkbox
import app.sipsure.ui.column
import app.sipsure.ui.dp
import app.sipsure.ui.heading
import app.sipsure.ui.numberField
import java.io.File
import java.util.Date
import kotlin.math.max

/** Single-activity UI. Screens are rebuilt from controller state; the controller owns all rules. */
class MainActivity : Activity() {
    private enum class Tab { HOME, CREDITS, SETTINGS }

    private val app get() = application as SipSureApp
    private val controller get() = app.controller
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var scroll: ScrollView

    private var tab = Tab.HOME
    private var onboardingStep = 0
    private var consented = false

    private val refresh = object : Runnable {
        override fun run() {
            syncAndRender(notify = true)
            handler.postDelayed(this, REFRESH_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scroll = ScrollView(this).apply {
            fitsSystemWindows = true
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        // Re-evaluate immediately: alarms are inexact, so the app is the source of truth when visible.
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun syncAndRender(notify: Boolean) {
        val before = controller.state.policy.phase
        controller.tick()
        val after = controller.state.policy.phase
        if (notify && after != before) app.notifier.show(after)
        if (after == Phase.ACTIVE) app.notifier.cancel()
        app.onStateChanged()
        // Forms keep user input, so only passive screens refresh on the timer.
        val editing = after == Phase.SETUP_REQUIRED || tab == Tab.SETTINGS
        if (!editing || scroll.childCount == 0) render()
    }

    private fun render() {
        val page = column(this, 20)
        val state = controller.state
        when (state.policy.phase) {
            Phase.SETUP_REQUIRED -> onboarding(page)
            Phase.RECOVERY_REQUIRED -> recovery(page)
            else -> {
                when (tab) {
                    Tab.HOME -> home(page, state)
                    Tab.CREDITS -> credits(page, state)
                    Tab.SETTINGS -> settings(page, state)
                }
                navigation(page)
            }
        }
        scroll.removeAllViews()
        scroll.addView(page)
    }

    private fun finishAction(error: String?) {
        if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        app.onStateChanged()
        if (controller.state.policy.phase == Phase.ACTIVE) app.notifier.cancel()
        render()
    }

    // ---------- Onboarding ----------

    private fun onboarding(page: LinearLayout) {
        page.heading("SipSure", 30f)
        page.body(getString(R.string.tagline))
        when (onboardingStep) {
            0 -> {
                page.heading("Before you start", 20f)
                page.card(CALM_BLUE) {
                    body("SipSure reminds you to drink water and asks for a quick photo check-in. It is a habit tool, not medical advice.")
                    body("Don't drink more water than is comfortable for you, and don't follow a schedule that conflicts with advice from your doctor.")
                }
                page.card(NEUTRAL) {
                    body("What happens if you miss a check-in", 16f).also { it.setTypeface(it.typeface, android.graphics.Typeface.BOLD) }
                    body("1. A reminder opens a short check-in window.\n2. A grace period follows with a warning.\n3. Then an in-app accountability screen appears inside SipSure.")
                    body("SipSure never locks your phone. Calls, emergency calling and all other apps keep working, and you can uninstall SipSure at any time.")
                }
                page.button("Continue", primary = true) { onboardingStep = 1; render() }
            }
            1 -> {
                page.heading("Permissions and your data", 20f)
                page.card(NEUTRAL) {
                    body("Camera: check-ins use your phone's camera app to take a fresh photo.")
                    body("Photos: SipSure checks the photo on your device (size, light, and whether it matches an earlier photo), then deletes it. Only a small fingerprint is kept to spot repeats. Photos are never uploaded.")
                    body("Notifications: used for check-in reminders. You can allow or deny them in system settings.")
                    body("Credits: purchases are in test mode and no real money is charged. Emergency advances are described in the Credits tab before you accept one.")
                    body("Photo checks only judge whether a photo looks fresh. They cannot prove that you drank water or how much.")
                }
                page.button("Back") { onboardingStep = 0; render() }
                page.checkbox("I understand how SipSure works and what it does with my data.") { consented = it }.isChecked = consented
                page.button("Continue", primary = true) {
                    if (consented) { onboardingStep = 2; render() }
                    else Toast.makeText(this, "Please confirm to continue.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                page.heading("Your schedule", 20f)
                val read = scheduleForm(page, Schedule())
                page.button("Back") { onboardingStep = 1; render() }
                page.button("Start", primary = true) {
                    val schedule = read()
                    val error = schedule?.let { controller.completeSetup(it) } ?: "Please enter numbers in every field."
                    if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    else {
                        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                        tab = Tab.HOME
                        finishAction(null)
                    }
                }
            }
        }
    }

    /** Builds schedule inputs; the returned reader yields null if any field is not a number. */
    private fun scheduleForm(page: LinearLayout, initial: Schedule): () -> Schedule? {
        val interval = page.numberField("Reminder every (minutes, ${Schedule.MIN_INTERVAL}-${Schedule.MAX_INTERVAL})", initial.intervalMinutes)
        val window = page.numberField("Check-in window (minutes, 5-60)", initial.checkInWindowMinutes)
        val grace = page.numberField("Grace period after the window (minutes, 0-60)", initial.graceMinutes)
        val quietStart = page.numberField("Quiet hours start (hour, 0-23)", initial.quietStartMinuteOfDay / 60)
        val quietEnd = page.numberField("Quiet hours end (hour, 0-23)", initial.quietEndMinuteOfDay / 60)
        page.caption("No reminders are sent during quiet hours. Use the same hour for both to turn them off.")
        return {
            val values = listOf(interval, window, grace, quietStart, quietEnd).map { it.text.toString().toIntOrNull() }
            if (values.any { it == null }) null
            else Schedule(values[0]!!, values[1]!!, values[2]!!, values[3]!! * 60, values[4]!! * 60)
        }
    }

    private fun recovery(page: LinearLayout) {
        page.heading("Let's get you set up again")
        page.card(AMBER) {
            body("SipSure couldn't read its saved data, so it paused everything. Nothing is restricted, and your phone works normally.")
            body("Resetting clears SipSure's saved schedule, credits and history on this phone and starts setup again.")
        }
        page.button("Reset and start setup", primary = true) { finishAction(controller.recoverFromCorruption()) }
    }

    // ---------- Home ----------

    private fun home(page: LinearLayout, state: AppState) {
        val p = state.policy
        val now = System.currentTimeMillis()
        page.heading("SipSure")
        when (p.phase) {
            Phase.ACTIVE, Phase.ADVANCE_OUTSTANDING, Phase.ADVANCE_OVERDUE -> {
                page.card(SOFT_GREEN) {
                    body("You're all set for now", 20f)
                    body("Next check-in: ${clock(p.nextDueAtMs)} (${untilText(p.nextDueAtMs, now)})")
                }
                page.caption(consequenceText(state.schedule))
                if (p.phase != Phase.ACTIVE) advanceBanner(page, state, now)
            }
            Phase.CHECK_IN_DUE -> {
                page.card(CALM_BLUE) {
                    body("Time for a sip", 20f)
                    body("Take a quick photo of you drinking water. The check-in window is open until ${clock(p.windowEndsAtMs)}.")
                }
                checkInButton(page)
            }
            Phase.GRACE_PERIOD -> {
                page.card(AMBER) {
                    body("Grace period", 20f)
                    body("The check-in window has closed. Check in by ${clock(p.graceEndsAtMs)} to skip the in-app accountability screen.")
                }
                checkInButton(page)
            }
            Phase.RESTRICTED_IN_APP, Phase.DEVICE_ENFORCEMENT_UNAVAILABLE -> restricted(page, state, now)
            Phase.EMERGENCY_BYPASS -> page.card(SOFT_GREEN) {
                body("Bypass active", 20f)
                body("No check-ins are needed until ${clock(p.bypassUntilMs)}. Your next check-in is at ${clock(p.nextDueAtMs)}.")
            }
            Phase.PAUSED -> {
                page.card(NEUTRAL) {
                    body("Schedule paused", 20f)
                    body("No reminders or restrictions while paused.")
                }
                page.button("Resume schedule", primary = true) { finishAction(controller.resume()) }
            }
            Phase.CHECK_IN_ACCEPTED, Phase.SETUP_REQUIRED, Phase.RECOVERY_REQUIRED -> Unit
        }
        if (p.phase != Phase.PAUSED && p.phase != Phase.RESTRICTED_IN_APP && p.phase != Phase.DEVICE_ENFORCEMENT_UNAVAILABLE) {
            page.button("Pause schedule") { finishAction(controller.pause()) }
        }
        page.button("Emergency override (free)") { finishAction(controller.emergencyOverride()) }
        page.caption("Emergency override is always free and pauses check-ins for an hour. It never needs credits.")
        recentHistory(page, state)
    }

    private fun restricted(page: LinearLayout, state: AppState, now: Long) {
        page.card(SOFT_RED) {
            body("Check-in missed", 20f)
            body("That's okay. It happens when you're busy, driving or asleep. SipSure is showing this screen until you check in, use a bypass, or pause.")
            body("Your phone is not locked. Calls, emergency calling and other apps work as normal.")
        }
        checkInButton(page)
        val ledger = state.ledger
        page.button("Use 1 credit to bypass (${ledger.balance} left)") { finishAction(controller.redeemBypassWithCredit()) }
        if (ledger.balance == 0) page.button("No credits? See emergency advance") { tab = Tab.CREDITS; render() }
        page.button("Pause schedule") { finishAction(controller.pause()) }
        advanceBanner(page, state, now)
    }

    private fun advanceBanner(page: LinearLayout, state: AppState, now: Long) {
        val open = state.ledger.advances(now).filter { it.status != AdvanceStatus.REPAID }
        if (open.isEmpty()) return
        val overdue = open.any { it.status == AdvanceStatus.OVERDUE }
        page.card(if (overdue) AMBER else NEUTRAL) {
            body(if (overdue) "An advance is overdue" else "Advances outstanding: ${open.size} of ${AdvanceTerms.MAX_OUTSTANDING}")
            body(if (overdue) "Repay it in the Credits tab to take new advances. Nothing else is affected." else "Repayment is due by ${dateTime(open.minOf { it.dueAtMs })}.")
        }
    }

    private fun checkInButton(page: LinearLayout) = page.button("Take check-in photo", primary = true) { launchCamera() }

    private fun consequenceText(s: Schedule) =
        "If a check-in is missed: a ${s.checkInWindowMinutes}-minute window, then a ${s.graceMinutes}-minute grace period, " +
            "then an in-app accountability screen. Your phone is never locked."

    private fun recentHistory(page: LinearLayout, state: AppState) {
        if (state.history.isEmpty()) return
        page.heading("Recent activity", 18f)
        state.history.takeLast(5).reversed().forEach { page.caption("${dateTime(it.atMs)} · ${historyLabel(it.kind)}") }
    }

    private fun historyLabel(kind: HistoryKind) = when (kind) {
        HistoryKind.CHECK_IN -> "Check-in recorded"
        HistoryKind.CHECK_IN_CONFIRMED_BY_USER -> "Check-in confirmed by you"
        HistoryKind.BYPASS_CREDIT -> "Bypass using a credit"
        HistoryKind.BYPASS_EMERGENCY -> "Emergency override"
        HistoryKind.PAUSED -> "Schedule paused"
        HistoryKind.RESUMED -> "Schedule resumed"
        HistoryKind.CREDITS_BOUGHT -> "Test credits added"
        HistoryKind.ADVANCE_TAKEN -> "Emergency advance taken"
        HistoryKind.ADVANCE_REPAID -> "Advance repaid"
    }

    // ---------- Credits ----------

    private fun credits(page: LinearLayout, state: AppState) {
        val now = System.currentTimeMillis()
        val ledger = state.ledger
        page.heading("Credits")
        page.card(CALM_BLUE) {
            body("Balance: ${ledger.balance} credit${if (ledger.balance == 1) "" else "s"}", 20f)
            body("One credit skips one restriction for an hour.")
        }
        page.card(NEUTRAL) {
            body("Test mode: credits are free and no real money is charged. Real purchases will be added after billing and store rules are reviewed. Refund and support details will be shown here before any real purchase.")
        }
        HydrationController.CREDIT_PACKS.forEach { pack ->
            page.button("Add ${pack.label} (test)") { finishAction(controller.buyCredits(pack)) }
        }

        page.heading("Emergency advances", 20f)
        val advances = ledger.advances(now)
        val remaining = ledger.advancesRemaining(now)
        page.body("You can have up to ${AdvanceTerms.MAX_OUTSTANDING} outstanding advances. Advances remaining: $remaining.")
        page.caption("Each advance gives ${AdvanceTerms.CREDITS_PER_ADVANCE} credit now and must be repaid within 24 hours. If it isn't, it's marked overdue and you can't take more advances until it's repaid. No data is deleted and your phone keeps working.")
        advances.forEach { adv ->
            page.card(if (adv.status == AdvanceStatus.OVERDUE) AMBER else NEUTRAL) {
                body("Advance of ${adv.credits} credit: ${adv.status.name.lowercase()}")
                if (adv.status != AdvanceStatus.REPAID) body("Due ${dateTime(adv.dueAtMs)}")
            }
            if (adv.status != AdvanceStatus.REPAID) page.button("Repay with 1 credit") { finishAction(controller.repayAdvance(adv.txId)) }
        }
        page.button("Take an emergency advance") { confirmAdvance(remaining, now) }

        page.heading("Ledger", 20f)
        if (ledger.entries.isEmpty()) page.caption("No transactions yet.")
        ledger.entries.takeLast(10).reversed().forEach { page.caption("${dateTime(it.atMs)} · ${entryLabel(it)}") }
    }

    private fun confirmAdvance(remaining: Int, now: Long) {
        val due = now + AdvanceTerms.REPAYMENT_WINDOW_MS
        AlertDialog.Builder(this)
            .setTitle("Emergency advance")
            .setMessage(
                "You'll receive ${AdvanceTerms.CREDITS_PER_ADVANCE} credit now. Repay it by ${dateTime(due)} (24 hours).\n\n" +
                    "Advances remaining after this one: ${max(remaining - 1, 0)} of ${AdvanceTerms.MAX_OUTSTANDING}.\n\n" +
                    "If you don't repay in time it's marked overdue and you can't take more advances. Nothing is deleted and your phone is never locked.",
            )
            .setPositiveButton("Accept advance") { _, _ -> finishAction(controller.takeAdvance()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun entryLabel(e: LedgerEntry) = when (e) {
        is LedgerEntry.Purchase -> "+${e.credits} test credits"
        is LedgerEntry.Redemption -> "-${e.credits} credit used for bypass"
        is LedgerEntry.Advance -> "+${e.credits} credit advance (due ${dateTime(e.dueAtMs)})"
        is LedgerEntry.Repayment -> "-${e.credits} credit repaid"
    }

    // ---------- Settings ----------

    private fun settings(page: LinearLayout, state: AppState) {
        page.heading("Settings")
        val read = scheduleForm(page, state.schedule)
        page.button("Save schedule", primary = true) {
            val schedule = read()
            val error = schedule?.let { controller.updateSchedule(it) } ?: "Please enter numbers in every field."
            if (error == null) Toast.makeText(this, "Schedule saved.", Toast.LENGTH_SHORT).show()
            finishAction(error)
        }
        page.heading("About enforcement", 18f)
        page.body("SipSure only restricts itself (an in-app screen). Whole-phone locking isn't available, and SipSure never blocks calls, emergency calling or uninstalling.")
        page.heading("Privacy", 18f)
        page.body("Everything is stored on this phone. Photos are checked on-device and deleted right away; only a small fingerprint of recent photos is kept to detect repeats. No analytics or tracking.")
    }

    private fun navigation(page: LinearLayout) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Tab.entries.forEach { t ->
            val label = t.name.lowercase().replaceFirstChar { it.uppercase() }
            row.addView(
                android.widget.Button(this).apply {
                    text = if (t == tab) "● $label" else label
                    isAllCaps = false
                    contentDescription = "$label tab"
                    setOnClickListener { tab = t; render() }
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
        }
        page.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(24) })
    }

    // ---------- Camera ----------

    private fun captureFile() = File(File(cacheDir, "captures").apply { mkdirs() }, "checkin.jpg")

    private fun launchCamera() {
        val file = captureFile().also { it.delete() }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, uri)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_PHOTO)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No camera app found. You can still bypass or pause.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PHOTO || resultCode != RESULT_OK) return
        val file = captureFile()
        Thread {
            val features = PhotoAnalyzer.analyze(file)
            file.delete() // Photos are not retained after analysis.
            runOnUiThread { handlePhoto(features) }
        }.start()
    }

    private fun handlePhoto(features: PhotoFeatures?) {
        if (features == null) {
            Toast.makeText(this, "That photo couldn't be read. Please try again.", Toast.LENGTH_LONG).show()
            return
        }
        val verdict = PhotoCheck.evaluate(features, controller.state.photoHashes, System.currentTimeMillis())
        when (verdict.outcome) {
            CheckInOutcome.ACCEPTED -> {
                val error = controller.submitCheckIn(verdict, features.hash)
                showOutcome("Check-in recorded", error ?: verdict.message, retake = false)
            }
            CheckInOutcome.RETRY_NEEDED -> showOutcome("Please retake", verdict.message, retake = true)
            CheckInOutcome.REVIEW_REQUIRED -> showReview(verdict, features.hash)
        }
        finishAction(null)
    }

    private fun showOutcome(title: String, message: String, retake: Boolean) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton(if (retake) "Retake photo" else "OK") { _, _ -> if (retake) launchCamera() }
            .apply { if (retake) setNegativeButton("Later", null) }
            .show()
    }

    private fun showReview(verdict: CheckInVerdict, hash: Long) {
        AlertDialog.Builder(this).setTitle("Needs your confirmation").setMessage(verdict.message)
            .setPositiveButton("Confirm it's a new check-in") { _, _ -> finishAction(controller.confirmReviewedCheckIn(hash)) }
            .setNegativeButton("Retake photo") { _, _ -> launchCamera() }
            .show()
    }

    // ---------- Formatting ----------

    private fun clock(ms: Long?) = ms?.let { DateFormat.getTimeFormat(this).format(Date(it)) } ?: "soon"

    private fun dateTime(ms: Long) =
        "${DateFormat.getMediumDateFormat(this).format(Date(ms))} ${DateFormat.getTimeFormat(this).format(Date(ms))}"

    private fun untilText(ms: Long?, now: Long): String {
        val minutes = ((ms ?: return "soon") - now) / 60_000
        return when {
            minutes <= 0 -> "any moment"
            minutes < 60 -> "in $minutes min"
            else -> "in ${minutes / 60} h ${minutes % 60} min"
        }
    }

    private companion object {
        const val REQUEST_PHOTO = 100
        const val REFRESH_MS = 30_000L
    }
}
