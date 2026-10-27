package app.sipsure.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.sipsure.domain.AppState
import app.sipsure.domain.HydrationController

/** Result of analysing a captured photo; shown to the user as a dialog. */
sealed interface PhotoOutcome {
    data class Accepted(val message: String) : PhotoOutcome
    data class Retry(val message: String) : PhotoOutcome
    data class Review(val message: String, val hash: Long) : PhotoOutcome
    data object Unreadable : PhotoOutcome
    data object NoCamera : PhotoOutcome
}

/**
 * Bridge between composables and the Android-specific parts. Composables stay free of Context:
 * they read [state] and call [act] with a controller action, which then triggers a refresh.
 */
class AppHost(
    val controller: HydrationController,
    val deviceLockAvailable: () -> Boolean,
    val launchCamera: () -> Unit,
    val openDialer: () -> Unit,
    val removeLockSetup: () -> Unit,
    val requestNotifications: () -> Unit,
    private val afterAction: () -> Unit,
) {
    private var revision by mutableIntStateOf(0)

    /** Latest message to show in a snackbar (an error from a controller action). */
    var message by mutableStateOf<String?>(null)

    /** Reading this inside a composable subscribes it to every refresh. */
    val state: AppState get() = revision.let { controller.state }

    val deviceLockActive: Boolean get() = revision.let { controller.deviceLockActive }

    var photoOutcome by mutableStateOf<PhotoOutcome?>(null)

    /** Set by the root so deep screens can jump to the Credits tab. */
    var onGoToCredits: () -> Unit = {}

    fun goToCredits() = onGoToCredits()

    fun refresh() {
        revision++
    }

    private var lastSeen: AppState? = null

    /** Refreshes the UI if state changed behind its back (alarms, boot and clock receivers also mutate it). */
    fun syncFromController() {
        if (controller.state !== lastSeen) {
            lastSeen = controller.state
            refresh()
        }
    }

    fun act(block: HydrationController.() -> String?) {
        message = controller.block()
        afterAction()
        refresh()
    }
}
