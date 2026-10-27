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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sipsure.domain.AdvanceStatus
import app.sipsure.domain.AppState
import app.sipsure.domain.HistoryEvent
import app.sipsure.domain.HistoryKind
import app.sipsure.domain.Phase
import app.sipsure.ui.theme.Ocean
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val MS_PER_MINUTE = 60_000L

@Composable
fun TodayScreen(host: AppHost, now: Long) {
    val state = host.state
    val p = state.policy
    val locked = host.deviceLockActive
    val missed = p.appRestricted
    val due = p.phase == Phase.CHECK_IN_DUE || p.phase == Phase.GRACE_PERIOD

    Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Greeting(state, now)

        val view = glassView(state, now, locked)
        WaterGlass(view.level, view.water, description = view.description) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (view.glyph != null) {
                    view.glyph.invoke()
                } else {
                    Text(view.value, style = MaterialTheme.typography.displayLarge, color = Color.White)
                    Text(view.unit, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }

        StatusPill(view.status, view.tint)
        Text(
            view.headline, style = MaterialTheme.typography.headlineMedium, color = Ocean.TextHigh, textAlign = TextAlign.Center,
        )
        Text(
            view.subline, style = MaterialTheme.typography.bodyLarge, color = Ocean.TextMid, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // The one thing to do right now.
        when {
            due -> PillButton("Check in with a photo", host.launchCamera, kind = ButtonKind.Amber) { CameraGlyph(it) }
            missed -> {
                PillButton("Check in with a photo", host.launchCamera) { CameraGlyph(it) }
                if (locked) PillButton("Emergency call", host.openDialer, kind = ButtonKind.Glass) { PhoneGlyph(it) }
            }
            p.phase == Phase.PAUSED -> PillButton("Resume schedule", { host.act { resume() } }) { PlayGlyph(it) }
        }

        Tiles(host, state, locked, missed)
        AdvanceNotice(state, now)
        // A missed check-in is a focused screen: just the ways out, nothing else competing for attention.
        if (!missed) {
            DropsToday(state, now)
            Recent(state)
        }
    }
}

// ---------- Hero content per state ----------

private class GlassView(
    val level: Float,
    val water: Color,
    val value: String = "",
    val unit: String = "",
    val glyph: (@Composable () -> Unit)? = null,
    val status: String,
    val tint: Color,
    val headline: String,
    val subline: String,
    val description: String,
)

private fun glassView(state: AppState, now: Long, locked: Boolean): GlassView {
    val p = state.policy
    val s = state.schedule
    fun clock(ms: Long?) = ms?.let(::formatClock) ?: "soon"
    return when (p.phase) {
        Phase.CHECK_IN_DUE, Phase.GRACE_PERIOD -> {
            val grace = p.phase == Phase.GRACE_PERIOD
            val end = (if (grace) p.graceEndsAtMs else p.windowEndsAtMs) ?: now
            val (value, unit) = countdownParts(end - now)
            GlassView(
                level = 0.14f, water = Ocean.WaterDue, value = value, unit = "$unit left",
                status = if (grace) "Grace period" else "Check-in open", tint = Ocean.Amber,
                headline = if (grace) "Almost out of time" else "Time for a sip",
                subline = "Snap a photo of you drinking water by ${clock(end)}.",
                description = "$value $unit left to check in",
            )
        }
        Phase.RESTRICTED_IN_APP, Phase.DEVICE_ENFORCEMENT_UNAVAILABLE -> GlassView(
            level = 0.14f, water = Ocean.WaterMissed, glyph = { LockGlyph(Color.White, 64.dp) },
            status = "Check-in missed", tint = Ocean.Coral,
            headline = "No worries, it happens",
            subline = if (locked) "Your phone is locked to SipSure. Check in, use a credit or the free override. Calls still work."
            else "Check in, use a credit, or use the free emergency override.",
            description = "Check-in missed",
        )
        Phase.EMERGENCY_BYPASS -> {
            val until = p.bypassUntilMs ?: now
            val (value, unit) = countdownParts(until - now)
            GlassView(
                level = ((until - now).toFloat() / (60 * MS_PER_MINUTE)).coerceIn(0.1f, 0.86f), water = Ocean.WaterBypass,
                value = value, unit = "$unit of bypass", status = "Bypass active", tint = Ocean.Mint,
                headline = "You're covered", subline = "Next check-in at ${clock(p.nextDueAtMs)}.",
                description = "Bypass active for $value $unit",
            )
        }
        Phase.PAUSED -> GlassView(
            level = 0.5f, water = Ocean.WaterPaused, glyph = { PauseGlyph(Color.White, 64.dp) },
            status = "Paused", tint = Ocean.TextMid,
            headline = "Schedule paused", subline = "No reminders or restrictions until you resume.",
            description = "Schedule paused",
        )
        else -> {
            val remaining = ((p.nextDueAtMs ?: now) - now).coerceAtLeast(0)
            val interval = s.intervalMinutes * MS_PER_MINUTE
            val (value, unit) = countdownParts(remaining)
            GlassView(
                level = (remaining.toFloat() / interval).coerceIn(0.12f, 0.86f), water = Ocean.WaterCalm,
                value = value, unit = "$unit to next sip", status = "On track", tint = Ocean.Aqua,
                headline = "You're all set", subline = "Next check-in at ${clock(p.nextDueAtMs)}.",
                description = "Next check-in in $value $unit",
            )
        }
    }
}

// ---------- Sections ----------

@Composable
private fun Greeting(state: AppState, now: Long) {
    val hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DropGlyph(Ocean.Aqua, 26.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(greeting, style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid)
            Text("SipSure", style = MaterialTheme.typography.titleLarge, color = Ocean.TextHigh)
        }
    }
}

@Composable
private fun Tiles(host: AppHost, state: AppState, locked: Boolean, missed: Boolean) {
    if (missed) {
        // A missed check-in is a focused screen: only the ways out.
        val balance = state.ledger.balance
        PillButton(
            if (balance > 0) "Use 1 credit to bypass" else "Out of credits? See emergency advance",
            { if (balance > 0) host.act { redeemBypassWithCredit() } else host.goToCredits() },
            kind = ButtonKind.Glass,
        ) { CoinGlyph(it) }
        PillButton("Emergency override (free)", { host.act { emergencyOverride() } }, kind = ButtonKind.Danger) { SosGlyph(it) }
        if (!locked) PillButton("Pause schedule", { host.act { pause() } }, kind = ButtonKind.Glass) { PauseGlyph(it) }
        return
    }
    val paused = state.policy.phase == Phase.PAUSED
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!paused) {
            ActionTile("Pause", { host.act { pause() } }, Modifier.weight(1f), accent = Ocean.TextMid, detail = "Take a break") { PauseGlyph(it) }
        }
        ActionTile(
            "Credits", { host.goToCredits() }, Modifier.weight(1f), accent = Ocean.Aqua,
            detail = "${state.ledger.balance} left",
        ) { CoinGlyph(it) }
        ActionTile("Override", { host.act { emergencyOverride() } }, Modifier.weight(1f), accent = Ocean.Coral, detail = "Always free") { SosGlyph(it) }
    }
}

@Composable
private fun AdvanceNotice(state: AppState, now: Long) {
    val open = state.ledger.advances(now).filter { it.status != AdvanceStatus.REPAID }
    if (open.isEmpty()) return
    val overdue = open.any { it.status == AdvanceStatus.OVERDUE }
    val tint = if (overdue) Ocean.Amber else Ocean.Aqua
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(tint))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (overdue) "An advance is overdue" else "${open.size} advance${if (open.size > 1) "s" else ""} to repay",
                    style = MaterialTheme.typography.titleMedium, color = Ocean.TextHigh,
                )
                Text(
                    if (overdue) "Repay it in Credits to take new ones."
                    else "Due ${formatDateTime(open.minOf { it.dueAtMs })}",
                    style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid,
                )
            }
        }
    }
}

/** A row of droplets, one per recorded check-in today. */
@Composable
private fun DropsToday(state: AppState, now: Long) {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val count = state.history.count {
        (it.kind == HistoryKind.CHECK_IN || it.kind == HistoryKind.CHECK_IN_CONFIRMED_BY_USER) &&
            Instant.ofEpochMilli(it.atMs).atZone(zone).toLocalDate() == today
    }
    GlassCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Today", style = MaterialTheme.typography.labelMedium, color = Ocean.TextLow)
                Text(
                    if (count == 0) "No check-ins yet" else "$count check-in${if (count > 1) "s" else ""}",
                    style = MaterialTheme.typography.titleLarge, color = Ocean.TextHigh,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(minOf(maxOf(count, 4), 6)) { index ->
                    DropGlyph(if (index < count) Ocean.Aqua else Color.White.copy(alpha = 0.16f), 22.dp)
                }
            }
        }
    }
}

@Composable
private fun Recent(state: AppState) {
    val events = state.history.takeLast(4).reversed()
    if (events.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Recent")
        GlassCard(padding = 6.dp) {
            events.forEachIndexed { index, event ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Ocean.GlassStrong), contentAlignment = Alignment.Center) {
                        glyphFor(event.kind)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(labelFor(event.kind), style = MaterialTheme.typography.titleMedium, color = Ocean.TextHigh)
                        Text(formatDateTime(event.atMs), style = MaterialTheme.typography.bodyMedium, color = Ocean.TextLow)
                    }
                }
                if (index != events.lastIndex) Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun glyphFor(kind: HistoryKind) = when (kind) {
    HistoryKind.CHECK_IN, HistoryKind.CHECK_IN_CONFIRMED_BY_USER, HistoryKind.ADVANCE_REPAID -> CheckGlyph(Ocean.Aqua, 18.dp)
    HistoryKind.BYPASS_EMERGENCY -> SosGlyph(Ocean.Coral, 18.dp)
    HistoryKind.PAUSED -> PauseGlyph(Ocean.TextMid, 18.dp)
    HistoryKind.RESUMED -> PlayGlyph(Ocean.TextMid, 18.dp)
    else -> CoinGlyph(Ocean.Aqua, 18.dp)
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
