package app.sipsure.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sipsure.domain.Phase
import app.sipsure.ui.theme.Ocean
import app.sipsure.ui.theme.SipSureTheme
import kotlinx.coroutines.delay

private val tabs = listOf(
    NavItem("Today") { DropGlyph(it, 22.dp) },
    NavItem("Credits") { CoinGlyph(it, 22.dp) },
    NavItem("Settings") { GearGlyph(it, 22.dp) },
)

@Composable
fun SipSureRoot(host: AppHost, onTick: () -> Unit) {
    SipSureTheme {
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                now = System.currentTimeMillis()
                onTick()
                delay(1000)
            }
        }

        val snackbar = remember { SnackbarHostState() }
        val message = host.message
        LaunchedEffect(message) {
            if (message != null) {
                snackbar.showSnackbar(message)
                host.message = null
            }
        }

        val phase = host.state.policy.phase
        var tab by rememberSaveable { mutableStateOf(0) }
        host.onGoToCredits = { tab = 1 }
        // A due phone lock takes over the screen; only Today and Credits (to reach an advance) stay reachable.
        if (host.deviceLockActive && tab == 2) tab = 0

        val showNav = phase != Phase.SETUP_REQUIRED && phase != Phase.RECOVERY_REQUIRED && !host.deviceLockActive

        OceanBackground {
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                when (phase) {
                    Phase.SETUP_REQUIRED -> OnboardingScreen(host, Modifier.navigationBarsPadding())
                    Phase.RECOVERY_REQUIRED -> Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) { Recovery(host) }
                    else -> Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp),
                    ) {
                        when (tab) {
                            0 -> TodayScreen(host, now)
                            1 -> CreditsScreen(host, now, onBack = { tab = 0 })
                            else -> SettingsScreen(host)
                        }
                        // Room so the last item can scroll above the floating nav.
                        Spacer(Modifier.height(if (showNav) 120.dp else 32.dp))
                    }
                }
            }

            if (showNav) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    FloatingNav(tabs, tab, { tab = it })
                }
            }
            Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = if (showNav) 84.dp else 8.dp), contentAlignment = Alignment.BottomCenter) {
                SnackbarHost(snackbar)
            }
        }

        host.photoOutcome?.let { PhotoOutcomeDialog(it, host) }
    }
}

@Composable
private fun Recovery(host: AppHost) {
    Spacer(Modifier.height(32.dp))
    Text("Let's get you set up again", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, color = Ocean.TextHigh)
    Spacer(Modifier.height(16.dp))
    GlassCard {
        Text(
            "SipSure couldn't read its saved data, so it paused everything. Nothing is restricted and your phone works normally. " +
                "Resetting clears the saved schedule, credits and history on this phone and starts setup again.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = Ocean.TextMid,
        )
    }
    Spacer(Modifier.height(16.dp))
    PillButton("Reset and start setup", { host.act { recoverFromCorruption() } })
}

@Composable
private fun PhotoOutcomeDialog(outcome: PhotoOutcome, host: AppHost) {
    val dismiss = { host.photoOutcome = null }
    when (outcome) {
        is PhotoOutcome.Accepted -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Check-in recorded") },
            text = { Text(outcome.message) },
            confirmButton = { TextButton(dismiss) { Text("Nice") } },
        )
        is PhotoOutcome.Retry -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Please retake") },
            text = { Text(outcome.message) },
            confirmButton = { TextButton({ dismiss(); host.launchCamera() }) { Text("Retake photo") } },
            dismissButton = { TextButton(dismiss) { Text("Later") } },
        )
        is PhotoOutcome.Review -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Needs your confirmation") },
            text = { Text(outcome.message) },
            confirmButton = { TextButton({ dismiss(); host.act { confirmReviewedCheckIn(outcome.hash) } }) { Text("It's a new check-in") } },
            dismissButton = { TextButton({ dismiss(); host.launchCamera() }) { Text("Retake photo") } },
        )
        PhotoOutcome.Unreadable -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Couldn't read the photo") },
            text = { Text("Please try taking it again.") },
            confirmButton = { TextButton(dismiss) { Text("OK") } },
        )
        PhotoOutcome.NoCamera -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("No camera app found") },
            text = { Text("You can still use a credit, the emergency override or pause.") },
            confirmButton = { TextButton(dismiss) { Text("OK") } },
        )
    }
}
