package app.sipsure.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.sipsure.domain.Phase
import app.sipsure.ui.theme.SipSureTheme
import kotlinx.coroutines.delay

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Filled.Home),
    CREDITS("Credits", Icons.Filled.Star),
    SETTINGS("Settings", Icons.Filled.Settings),
}

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
        var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
        host.onGoToCredits = { tab = Tab.CREDITS }
        // A due phone lock always takes over the screen.
        if (host.deviceLockActive && tab != Tab.TODAY) tab = Tab.TODAY

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (phase != Phase.SETUP_REQUIRED && phase != Phase.RECOVERY_REQUIRED && !host.deviceLockActive) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            val modifier = Modifier.fillMaxSize().padding(padding)
            when (phase) {
                Phase.SETUP_REQUIRED -> OnboardingScreen(host, modifier)
                Phase.RECOVERY_REQUIRED -> Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp)) { Recovery(host) }
                else -> Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                    when (tab) {
                        Tab.TODAY -> TodayScreen(host, now)
                        Tab.CREDITS -> CreditsScreen(host, now)
                        Tab.SETTINGS -> SettingsScreen(host)
                    }
                }
            }
        }

        host.photoOutcome?.let { PhotoOutcomeDialog(it, host) }
    }
}

@Composable
private fun Recovery(host: AppHost) {
    Spacer(Modifier.height(24.dp))
    Text("Let's get you set up again", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(16.dp))
    SoftCard(container = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            "SipSure couldn't read its saved data, so it paused everything. Nothing is restricted and your phone works normally. " +
                "Resetting clears the saved schedule, credits and history on this phone and starts setup again.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Reset and start setup", { host.act { recoverFromCorruption() } })
}

@Composable
private fun PhotoOutcomeDialog(outcome: PhotoOutcome, host: AppHost) {
    val dismiss = { host.photoOutcome = null }
    when (outcome) {
        is PhotoOutcome.Accepted -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Check-in recorded") },
            text = { Text(outcome.message) },
            confirmButton = { TextButton(dismiss) { Text("OK") } },
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
