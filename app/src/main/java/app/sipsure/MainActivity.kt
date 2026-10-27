package app.sipsure

import android.Manifest
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import app.sipsure.domain.CheckInOutcome
import app.sipsure.domain.Phase
import app.sipsure.domain.PhotoCheck
import app.sipsure.domain.PhotoFeatures
import app.sipsure.platform.PhotoAnalyzer
import app.sipsure.ui.AppHost
import app.sipsure.ui.PhotoOutcome
import app.sipsure.ui.SipSureRoot
import java.io.File

/** Hosts the Compose UI and owns the Android-only pieces: camera, lock-task mode, dialer, permissions. */
class MainActivity : ComponentActivity() {
    private val app get() = application as SipSureApp
    private val controller get() = app.controller
    private lateinit var host: AppHost

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) analysePhoto()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The UI is one dark ocean gradient, so system bar icons are always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        host = AppHost(
            controller = controller,
            deviceLockAvailable = app.deviceLock::isAvailable,
            launchCamera = ::launchCamera,
            openDialer = { startActivity(Intent(Intent.ACTION_DIAL)) },
            removeLockSetup = app.deviceLock::removeCapability,
            requestNotifications = ::requestNotifications,
            afterAction = ::afterAction,
        )
        setContent { SipSureRoot(host, onTick = ::onTick) }
    }

    override fun onResume() {
        super.onResume()
        onTick()
    }

    /** Runs every second while visible: applies elapsed time, notifies on phase changes, keeps the lock in sync. */
    private fun onTick() {
        val before = controller.state
        controller.tick()
        val after = controller.state
        if (after !== before) {
            if (after.policy.phase != before.policy.phase) app.notifier.show(after.policy.phase)
            afterAction()
            host.refresh()
        }
        applyDeviceLock()
    }

    private fun afterAction() {
        if (controller.state.policy.phase == Phase.ACTIVE) app.notifier.cancel()
        app.onStateChanged()
        applyDeviceLock()
    }

    /** Engages lock-task mode exactly while the controller says the phone lock is due, and releases it otherwise. */
    private fun applyDeviceLock() {
        val inLockTask = getSystemService(ActivityManager::class.java).lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        try {
            if (controller.deviceLockActive && !inLockTask) {
                app.deviceLock.prepare()
                startLockTask()
            } else if (!controller.deviceLockActive && inLockTask) {
                stopLockTask()
            }
        } catch (e: SecurityException) {
            // Ownership was revoked between the check and the call; stay usable rather than half-locked.
            Toast.makeText(this, "Phone lock couldn't start on this device.", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    // ---------- Camera ----------

    private fun captureFile() = File(File(cacheDir, "captures").apply { mkdirs() }, "checkin.jpg")

    private fun launchCamera() {
        val file = captureFile().also { it.delete() }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            host.photoOutcome = PhotoOutcome.NoCamera
        }
    }

    private fun analysePhoto() {
        val file = captureFile()
        Thread {
            val features = PhotoAnalyzer.analyze(file)
            file.delete() // Photos are not retained after analysis.
            runOnUiThread { handlePhoto(features) }
        }.start()
    }

    private fun handlePhoto(features: PhotoFeatures?) {
        if (features == null) {
            host.photoOutcome = PhotoOutcome.Unreadable
            return
        }
        val verdict = PhotoCheck.evaluate(features, controller.state.photoHashes, System.currentTimeMillis())
        host.photoOutcome = when (verdict.outcome) {
            CheckInOutcome.ACCEPTED -> {
                host.act { submitCheckIn(verdict, features.hash) }
                PhotoOutcome.Accepted(verdict.message)
            }
            CheckInOutcome.RETRY_NEEDED -> PhotoOutcome.Retry(verdict.message)
            CheckInOutcome.REVIEW_REQUIRED -> PhotoOutcome.Review(verdict.message, features.hash)
        }
    }
}
