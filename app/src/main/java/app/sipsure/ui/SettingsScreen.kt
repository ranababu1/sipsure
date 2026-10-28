package app.sipsure.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.sipsure.ui.theme.Ocean

@Composable
fun SettingsScreen(host: AppHost) {
    val state = host.state
    var draft by remember(state.schedule) { mutableStateOf(state.schedule) }
    val changed = draft != state.schedule
    val valid = draft.validationError() == null

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.padding(top = 8.dp)) {
            Text("PREFERENCES", style = MaterialTheme.typography.labelMedium, color = Ocean.TextLow)
            Text("Settings", style = MaterialTheme.typography.headlineLarge, color = Ocean.TextHigh)
        }

        SectionLabel("Schedule")
        GlassCard(padding = 20.dp) {
            ScheduleEditor(draft) { draft = it }
            Spacer(Modifier.height(20.dp))
            PillButton(
                if (changed) "Save schedule" else "Saved",
                { if (changed && valid) host.act { updateSchedule(draft) } },
                kind = if (changed && valid) ButtonKind.Primary else ButtonKind.Glass,
            ) { CheckGlyph(it) }
        }

        SectionLabel("Phone lock")
        PhoneLockCard(host)

        SectionLabel("Privacy")
        GlassCard {
            Row(verticalAlignment = Alignment.Top) {
                GlyphBadge(Ocean.Mint) { CheckGlyph(it, 20.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Stays on this phone", style = MaterialTheme.typography.titleMedium, color = Ocean.TextHigh)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Photos are checked on-device and deleted straight away; only a small fingerprint of recent photos is kept to spot repeats. No analytics, no tracking.",
                        style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneLockCard(host: AppHost) {
    val state = host.state
    val available = host.deviceLockAvailable()
    var confirmOn by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    GlassCard(padding = 20.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlyphBadge(Ocean.Coral, 44.dp) { LockGlyph(it, 22.dp) }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text("Lock my phone", style = MaterialTheme.typography.titleMedium, color = Ocean.TextHigh)
                Text(
                    when {
                        !available -> "Needs device setup"
                        state.deviceLockEnabled -> "On after a missed check-in"
                        else -> "Off"
                    },
                    style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid,
                )
            }
            if (available) {
                Switch(
                    checked = state.deviceLockEnabled,
                    onCheckedChange = { on -> if (on) confirmOn = true else host.act { setDeviceLock(false) } },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Ocean.Aqua, checkedThumbColor = Color(0xFF041A26),
                        uncheckedTrackColor = Ocean.Glass, uncheckedBorderColor = Ocean.GlassBorder, uncheckedThumbColor = Ocean.TextLow,
                    ),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (!available) {
            Text(
                "By default SipSure only shows a screen inside the app. A full phone lock needs SipSure to be the device owner, which Android only allows on a phone with no accounts signed in. With USB debugging on, run:",
                style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "adb shell dpm set-device-owner app.sipsure/.platform.SipSureAdminReceiver",
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(Color.Black.copy(alpha = 0.35f)).padding(12.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Ocean.Foam,
            )
        } else {
            Text(
                "After a missed check-in the phone locks until a credit is used (borrowing one counts). Home, recents and notifications stop working. Emergency calls and incoming calls always work. Nothing is deleted. While a borrowed credit is unpaid, SipSure can't be uninstalled.",
                style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid,
            )
            Spacer(Modifier.height(14.dp))
            PillButton("Remove phone-lock setup", { confirmRemove = true }, kind = ButtonKind.Danger) { LockGlyph(it) }
        }
    }

    if (confirmOn) {
        AlertDialog(
            onDismissRequest = { confirmOn = false },
            title = { Text("Turn on phone lock?") },
            text = {
                Text(
                    "If you miss a check-in, your whole phone is locked to SipSure after the grace period.\n\n" +
                        "Only a credit unlocks it; a photo no longer does. You can borrow a credit and repay it within 24 hours, " +
                        "and while a borrowed credit is unpaid SipSure can't be uninstalled.\n\n" +
                        "Emergency calls and incoming calls always work. You can turn this off here whenever the phone isn't locked.",
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
            text = { Text("SipSure gives up device-owner status, so the phone lock can't be used and the app can be uninstalled normally. This is only possible while no borrowed credit is unpaid.") },
            confirmButton = {
                TextButton({
                    confirmRemove = false
                    host.act { setDeviceLock(false) ?: removeLockSetupBlocker() ?: run { host.removeLockSetup(); null } }
                }) { Text("Remove") }
            },
            dismissButton = { TextButton({ confirmRemove = false }) { Text("Cancel") } },
        )
    }
}
