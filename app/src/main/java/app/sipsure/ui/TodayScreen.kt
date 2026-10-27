package app.sipsure.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sipsure.domain.AdvanceStatus
import app.sipsure.domain.AppState
import app.sipsure.domain.HistoryEvent
import app.sipsure.domain.HistoryKind
import app.sipsure.domain.Phase
import app.sipsure.ui.theme.Hero

private const val MS_PER_MINUTE = 60_000L

@Composable
fun TodayScreen(host: AppHost, now: Long) {
    val state = host.state
    val p = state.policy
    val locked = host.deviceLockActive

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Header(p.phase)
        when (p.phase) {
            Phase.CHECK_IN_DUE -> DueHero(state, now, grace = false)
            Phase.GRACE_PERIOD -> DueHero(state, now, grace = true)
            Phase.RESTRICTED_IN_APP, Phase.DEVICE_ENFORCEMENT_UNAVAILABLE -> MissedHero(locked)
            Phase.EMERGENCY_BYPASS -> BypassHero(state, now)
            Phase.PAUSED -> PausedHero()
            else -> CalmHero(state, now)
        }

        when (p.phase) {
            Phase.CHECK_IN_DUE, Phase.GRACE_PERIOD ->
                PrimaryButton("Take check-in photo", host.launchCamera, icon = Icons.Filled.Check)
            Phase.RESTRICTED_IN_APP, Phase.DEVICE_ENFORCEMENT_UNAVAILABLE -> MissedActions(host, state, locked)
            Phase.PAUSED -> PrimaryButton("Resume schedule", { host.act { resume() } }, icon = Icons.Filled.PlayArrow)
            else -> Unit
        }

        AdvanceNotice(state, now)

        val canPause = p.phase != Phase.PAUSED && !p.appRestricted
        if (canPause || !locked) {
            SoftCard {
                Text("Quick actions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                if (canPause) {
                    SecondaryButton("Pause schedule", { host.act { pause() } })
                    Spacer(Modifier.height(10.dp))
                }
                if (!locked) {
                    SecondaryButton(
                        "Emergency override (free)", { host.act { emergencyOverride() } },
                        icon = Icons.Filled.Warning, tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Free, always available, and pauses check-ins for an hour.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.history.isNotEmpty()) Timeline(state.history.takeLast(5).reversed())
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header(phase: Phase) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        WaterDrop(Modifier.size(width = 18.dp, height = 24.dp), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text("SipSure", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.weight(1f))
        val (label, container) = when (phase) {
            Phase.CHECK_IN_DUE, Phase.GRACE_PERIOD -> "Check-in open" to MaterialTheme.colorScheme.tertiaryContainer
            Phase.RESTRICTED_IN_APP, Phase.DEVICE_ENFORCEMENT_UNAVAILABLE -> "Check-in missed" to MaterialTheme.colorScheme.tertiaryContainer
            Phase.EMERGENCY_BYPASS -> "Bypass" to MaterialTheme.colorScheme.secondaryContainer
            Phase.PAUSED -> "Paused" to MaterialTheme.colorScheme.secondaryContainer
            else -> "On track" to MaterialTheme.colorScheme.primaryContainer
        }
        StatusPill(label, container = container, content = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CalmHero(state: AppState, now: Long) {
    val due = state.policy.nextDueAtMs
    val interval = state.schedule.intervalMinutes * MS_PER_MINUTE
    val remaining = ((due ?: now) - now).coerceAtLeast(0)
    val (value, unit) = countdownParts(remaining)
    HeroCard(Hero.Calm) {
        ProgressRing(
            progress = 1f - remaining.toFloat() / interval,
            description = "Next check-in in $value $unit",
        ) {
            RingCenter(value, unit, "until your next sip")
        }
        Spacer(Modifier.height(20.dp))
        HeroTitle("You're all set")
        HeroBody(
            due?.let { "Next check-in at ${formatClock(it)}. If it's missed there's a ${state.schedule.checkInWindowMinutes}-minute window, " +
                "then ${state.schedule.graceMinutes} minutes of grace, then the restriction screen." } ?: "",
        )
    }
}

@Composable
private fun DueHero(state: AppState, now: Long, grace: Boolean) {
    val p = state.policy
    val end = (if (grace) p.graceEndsAtMs else p.windowEndsAtMs) ?: now
    val total = (if (grace) state.schedule.graceMinutes else state.schedule.checkInWindowMinutes) * MS_PER_MINUTE
    val remaining = (end - now).coerceAtLeast(0)
    val (value, unit) = countdownParts(remaining)
    HeroCard(Hero.Due) {
        ProgressRing(
            progress = if (total > 0) remaining.toFloat() / total else 0f,
            description = "$value $unit left to check in",
        ) {
            RingCenter(value, unit, if (grace) "of grace left" else "left to check in")
        }
        Spacer(Modifier.height(20.dp))
        HeroTitle(if (grace) "Grace period" else "Time for a sip")
        HeroBody(
            if (grace) "The window has closed. Check in by ${formatClock(end)} to avoid the restriction screen."
            else "Take a quick photo of you drinking water. The window is open until ${formatClock(end)}.",
        )
    }
}

@Composable
private fun MissedHero(locked: Boolean) {
    HeroCard(Hero.Missed) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        HeroTitle("Check-in missed")
        HeroBody(
            "That's okay, it happens. SipSure stays on this screen until you check in, use a credit or use the emergency override." +
                if (locked) " Your phone is locked to SipSure until then; emergency calls and incoming calls still work."
                else " Your phone is not locked.",
        )
    }
}

@Composable
private fun BypassHero(state: AppState, now: Long) {
    val until = state.policy.bypassUntilMs ?: now
    val remaining = (until - now).coerceAtLeast(0)
    val (value, unit) = countdownParts(remaining)
    HeroCard(Hero.Bypass) {
        ProgressRing(
            progress = remaining.toFloat() / (60 * MS_PER_MINUTE),
            description = "Bypass active for $value $unit",
        ) {
            RingCenter(value, unit, "of bypass left")
        }
        Spacer(Modifier.height(20.dp))
        HeroTitle("Bypass active")
        HeroBody("No check-ins are needed until ${formatClock(until)}. Your next check-in is at ${state.policy.nextDueAtMs?.let(::formatClock) ?: "soon"}.")
    }
}

@Composable
private fun PausedHero() {
    HeroCard(Hero.Paused) {
        WaterDrop(Modifier.size(width = 40.dp, height = 52.dp), Color.White.copy(alpha = 0.85f))
        Spacer(Modifier.height(16.dp))
        HeroTitle("Schedule paused")
        HeroBody("No reminders or restrictions while paused. Resume whenever you're ready.")
    }
}

@Composable
private fun MissedActions(host: AppHost, state: AppState, locked: Boolean) {
    val balance = state.ledger.balance
    PrimaryButton("Take check-in photo", host.launchCamera, icon = Icons.Filled.Check)
    if (locked) {
        SecondaryButton("Emergency call", host.openDialer, icon = Icons.Filled.Call)
        SecondaryButton(
            "Emergency override (free)", { host.act { emergencyOverride() } },
            icon = Icons.Filled.Warning, tint = MaterialTheme.colorScheme.tertiary,
        )
    }
    SecondaryButton(
        if (balance > 0) "Use 1 credit to bypass ($balance left)" else "No credits — see emergency advance",
        { if (balance > 0) host.act { redeemBypassWithCredit() } else host.goToCredits() },
        icon = Icons.Filled.Star,
    )
}

@Composable
private fun AdvanceNotice(state: AppState, now: Long) {
    val open = state.ledger.advances(now).filter { it.status != AdvanceStatus.REPAID }
    if (open.isEmpty()) return
    val overdue = open.any { it.status == AdvanceStatus.OVERDUE }
    SoftCard(container = if (overdue) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            if (overdue) "An advance is overdue" else "${open.size} of 3 advances outstanding",
            style = MaterialTheme.typography.titleMedium,
            color = if (overdue) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (overdue) "Repay it in the Credits tab to take new advances. Nothing else is affected."
            else "Repayment due by ${formatDateTime(open.minOf { it.dueAtMs })}.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (overdue) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun Timeline(events: List<HistoryEvent>) {
    SectionTitle("Recent activity")
    SoftCard {
        events.forEachIndexed { index, event ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(iconFor(event.kind), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, Modifier.size(36.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(labelFor(event.kind), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(formatDateTime(event.atMs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (index != events.lastIndex) Spacer(Modifier.height(14.dp))
        }
    }
}

private fun iconFor(kind: HistoryKind) = when (kind) {
    HistoryKind.CHECK_IN, HistoryKind.CHECK_IN_CONFIRMED_BY_USER, HistoryKind.ADVANCE_REPAID -> Icons.Filled.Check
    HistoryKind.BYPASS_EMERGENCY -> Icons.Filled.Warning
    HistoryKind.PAUSED, HistoryKind.RESUMED -> Icons.Filled.PlayArrow
    else -> Icons.Filled.Star
}

fun labelFor(kind: HistoryKind) = when (kind) {
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

@Composable
private fun RingCenter(value: String, unit: String, caption: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.displayLarge, color = Color.White)
        Text(unit, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
        Spacer(Modifier.height(4.dp))
        Text(caption, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun HeroTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
}

@Composable
private fun HeroBody(text: String) {
    if (text.isEmpty()) return
    Spacer(Modifier.height(8.dp))
    Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.92f), textAlign = TextAlign.Center)
}
