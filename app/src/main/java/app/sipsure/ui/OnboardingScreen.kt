package app.sipsure.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sipsure.domain.Schedule
import app.sipsure.ui.theme.Hero

/** Three short steps: what SipSure does, what it needs, and the schedule. */
@Composable
fun OnboardingScreen(host: AppHost, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var consented by rememberSaveable { mutableStateOf(false) }
    var schedule by remember { mutableStateOf(Schedule()) }

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { index ->
                Box(
                    Modifier.padding(end = 8.dp).height(6.dp).width(if (index == step) 28.dp else 12.dp).clip(CircleShape)
                        .background(if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
            Spacer(Modifier.weight(1f))
            Text("Step ${step + 1} of 3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))

        AnimatedContent(step, label = "onboarding") { current ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (current) {
                    0 -> Welcome()
                    1 -> Permissions(consented) { consented = it }
                    else -> ScheduleStep(schedule) { schedule = it }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        when (step) {
            0 -> PrimaryButton("Get started", { step = 1 })
            1 -> {
                PrimaryButton("Continue", { step = 2 }, container = if (consented) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                if (!consented) {
                    Text(
                        "Turn on the switch above to continue.",
                        Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton({ step = 0 }, Modifier.fillMaxWidth()) { Text("Back") }
            }
            else -> {
                PrimaryButton("Start my schedule", {
                    host.act { completeSetup(schedule) }
                    if (host.controller.state.policy.phase != app.sipsure.domain.Phase.SETUP_REQUIRED) host.requestNotifications()
                }, icon = Icons.Filled.Check)
                TextButton({ step = 1 }, Modifier.fillMaxWidth()) { Text("Back") }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Welcome() {
    HeroCard(Hero.Calm) {
        WaterDrop(Modifier.size(width = 56.dp, height = 74.dp))
        Spacer(Modifier.height(18.dp))
        Text("SipSure", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text("Build the habit. Keep control.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
    }
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            InfoRow(Icons.Filled.Notifications, "Reminders that fit your day", "Pick how often you want to be reminded, and set quiet hours for sleep.")
            InfoRow(Icons.Filled.Check, "A quick photo check-in", "Snap a photo of yourself drinking water. It's checked on your phone and deleted.")
            InfoRow(
                Icons.Filled.Lock, "Gentle accountability",
                "Miss a check-in and SipSure shows a restriction screen. Credits or the free emergency override get you out of it. Calls and emergency calling always work.",
            )
        }
    }
    SoftCard(container = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            "SipSure is a habit tool, not medical advice. Don't drink more water than is comfortable, and don't follow a schedule that conflicts with advice from your doctor.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun Permissions(consented: Boolean, onConsent: (Boolean) -> Unit) {
    Text("Your data and permissions", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            InfoRow(Icons.Filled.Face, "Camera", "Check-ins open your phone's camera app. SipSure itself doesn't get camera access.")
            InfoRow(
                Icons.Filled.Info, "Photos",
                "Checked on your device for size, light and whether it matches an earlier photo, then deleted. Only a small fingerprint is kept to spot repeats. Nothing is uploaded.",
            )
            InfoRow(Icons.Filled.Notifications, "Notifications", "Used for check-in reminders. You can change this in system settings.")
            InfoRow(Icons.Filled.Star, "Credits", "Test mode only: no real money is charged. Emergency advances are explained before you accept one.")
            InfoRow(
                Icons.Filled.Lock, "Phone lock (optional)",
                "Off by default. It needs special device setup, can be switched off, and always allows emergency calls and the free override.",
            )
        }
    }
    SoftCard(container = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            "Photo checks only look at whether a picture seems fresh. They can't prove you drank water, or how much.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "I understand how SipSure works and what it does with my data.",
                Modifier.weight(1f).padding(end = 12.dp),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(
                checked = consented, onCheckedChange = onConsent,
                modifier = Modifier.semantics { contentDescription = "I understand how SipSure works" },
            )
        }
    }
}

@Composable
private fun ScheduleStep(schedule: Schedule, onChange: (Schedule) -> Unit) {
    Text("Set your rhythm", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
    Text(
        "You can change all of this later in Settings.",
        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SoftCard { ScheduleEditor(schedule, onChange) }
    HeroCard(Hero.Calm) {
        Text(
            "Every ${intervalLabel(schedule.intervalMinutes)}, you'll have ${schedule.checkInWindowMinutes} minutes to check in" +
                if (schedule.graceMinutes > 0) ", plus ${schedule.graceMinutes} minutes of grace." else ".",
            style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center,
        )
    }
}
