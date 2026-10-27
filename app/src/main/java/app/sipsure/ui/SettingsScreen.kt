package app.sipsure.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sipsure.domain.Schedule

@Composable
fun SettingsScreen(host: AppHost) {
    val state = host.state
    var draft by remember(state.schedule) { mutableStateOf(state.schedule) }
    val changed = draft != state.schedule

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

        SoftCard {
            Text("Schedule", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            ScheduleEditor(draft) { draft = it }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                if (changed) "Save schedule" else "Schedule saved",
                { host.act { updateSchedule(draft) } },
                icon = Icons.Filled.Check,
                modifier = Modifier,
            )
        }

        PhoneLockCard(host)

        SoftCard {
            InfoRow(
                Icons.Filled.Info, "Privacy",
                "Everything stays on this phone. Photos are checked on-device and deleted straight away; only a small fingerprint of recent photos is kept to spot repeats. No analytics or tracking.",
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PhoneLockCard(host: AppHost) {
    val state = host.state
    val available = host.deviceLockAvailable()
    var confirmOn by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    SoftCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Lock, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text("Phone lock", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    when {
                        !available -> "Not set up on this phone"
                        state.deviceLockEnabled -> "On"
                        else -> "Off"
                    },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (available) {
                Switch(
                    checked = state.deviceLockEnabled,
                    onCheckedChange = { on -> if (on) confirmOn = true else host.act { setDeviceLock(false) } },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (!available) {
            Text(
                "By default SipSure only shows a restriction screen inside the app and never blocks calls, other apps or uninstalling. " +
                    "A full phone lock needs SipSure to be the device owner, which Android only allows on a phone with no accounts signed in " +
                    "(for example a dedicated or freshly reset phone). With USB debugging on, run:",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    "adb shell dpm set-device-owner app.sipsure/.platform.SipSureAdminReceiver",
                    Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            Text(
                "After a missed check-in and the grace period, the phone locks to SipSure. Home, recents and notifications stop working. " +
                    "Emergency calls and incoming calls still work, and the camera opens for check-in photos. " +
                    "A photo check-in, a credit or the free emergency override unlocks it. Nothing is deleted.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            SecondaryButton("Remove phone-lock setup", { confirmRemove = true }, tint = MaterialTheme.colorScheme.tertiary)
        }
    }

    if (confirmOn) {
        AlertDialog(
            onDismissRequest = { confirmOn = false },
            title = { Text("Turn on phone lock?") },
            text = {
                Text(
                    "If you miss a check-in, your whole phone will be locked to SipSure after the grace period.\n\n" +
                        "You can always make emergency calls, answer calls and use the free emergency override. " +
                        "A photo check-in or a credit also unlocks it, and you can turn this off here whenever the phone isn't locked.",
                )
            },
            confirmButton = { TextButton({ confirmOn = false; host.act { setDeviceLock(true) } }) { Text("Turn on") } },
            dismissButton = { TextButton({ confirmOn = false }) { Text("Cancel") } },
        )
    }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove phone-lock setup?") },
            text = { Text("SipSure gives up device-owner status, so the phone lock can't be used and the app can be uninstalled normally.") },
            confirmButton = {
                TextButton({
                    confirmRemove = false
                    host.act { setDeviceLock(false) ?: run { host.removeLockSetup(); null } }
                }) { Text("Remove") }
            },
            dismissButton = { TextButton({ confirmRemove = false }) { Text("Cancel") } },
        )
    }
}
