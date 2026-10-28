package app.sipsure.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sipsure.domain.AdvanceStatus
import app.sipsure.domain.AppState
import app.sipsure.domain.HistoryKind
import app.sipsure.domain.Phase
import app.sipsure.ui.theme.Ocean
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val MS_PER_MINUTE = 60_000L
private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM")

@Composable
fun TodayScreen(host: AppHost, now: Long) {
    val state = host.state
    val p = state.policy
    val locked = host.deviceLockActive
    val missed = p.appRestricted
    val due = p.phase == Phase.CHECK_IN_DUE || p.phase == Phase.GRACE_PERIOD
    val view = glassView(state, now, locked)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar(host, state, now)
        Spacer(Modifier.height(8.dp))

        WaterGlass(view.level, view.water, progress = view.progress, description = view.description) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (view.glyph != null) {
                    view.glyph.invoke()
                } else {
                    Text(view.value, style = MaterialTheme.typography.displayLarge, color = Color.White)
                    Text(view.unit, style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.78f))
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        StatusPill(view.status, view.tint, pulse = due || missed)
        Spacer(Modifier.height(14.dp))
        AnimatedContent(view.headline, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "headline") { headline ->
            Text(
                headline, Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium, color = Ocean.TextHigh, textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            view.subline, Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodyLarge, color = Ocean.TextMid, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        Actions(host, state, locked, missed, due)
        AdvanceNotice(state, now)

        // A missed check-in is a focused screen: just the ways out, nothing else competing for attention.
        if (!missed) {
            Spacer(Modifier.height(24.dp))
            TodayStats(state, now)
            Recent(state)
        }
    }
}

// ---------- Hero content per state ----------

private class GlassView(
    val level: Float,
    val water: Color,
    val progress: Float? = null,
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
            val length = (if (grace) s.graceMinutes else s.checkInWindowMinutes).coerceAtLeast(1) * MS_PER_MINUTE
            val (value, unit) = countdownParts(end - now)
            GlassView(
                level = 0.2f, water = Ocean.WaterDue, progress = ((end - now).toFloat() / length).coerceIn(0f, 1f),
                value = value, unit = "$unit left",
                status = if (grace) "Grace period" else "Check-in open", tint = Ocean.Amber,
                headline = if (grace) "Almost out of time" else "Time for a sip",
                subline = "Snap a photo of you drinking water by ${clock(end)}.",
                description = "$value $unit left to check in",
            )
        }
        Phase.RESTRICTED_IN_APP, Phase.DEVICE_ENFORCEMENT_UNAVAILABLE -> GlassView(
            level = 0.14f, water = Ocean.WaterMissed, glyph = { LockGlyph(Color.White, 56.dp) },
            status = "Check-in missed", tint = Ocean.Coral,
            headline = if (locked) "Your phone is locked" else "No worries, it happens",
            subline = if (locked) "Only a credit unlocks it. Emergency calls still work."
            else "Check in with a photo, or use a credit to carry on.",
            description = "Check-in missed",
        )
        Phase.EMERGENCY_BYPASS -> {
            val until = p.bypassUntilMs ?: now
            val left = ((until - now).toFloat() / (60 * MS_PER_MINUTE)).coerceIn(0f, 1f)
            val (value, unit) = countdownParts(until - now)
            GlassView(
                level = left.coerceIn(0.12f, 0.82f), water = Ocean.WaterBypass, progress = left,
                value = value, unit = "$unit covered", status = "Bypass active", tint = Ocean.Mint,
                headline = "You're covered", subline = "Next check-in at ${clock(p.nextDueAtMs)}.",
                description = "Bypass active for $value $unit",
            )
        }
        Phase.PAUSED -> GlassView(
            level = 0.5f, water = Ocean.WaterPaused, glyph = { PauseGlyph(Color.White, 52.dp) },
            status = "Paused", tint = Ocean.TextMid,
            headline = "Schedule paused", subline = "No reminders or restrictions until you resume.",
            description = "Schedule paused",
        )
        else -> {
            val remaining = ((p.nextDueAtMs ?: now) - now).coerceAtLeast(0)
            val left = (remaining.toFloat() / (s.intervalMinutes * MS_PER_MINUTE)).coerceIn(0f, 1f)
            val (value, unit) = countdownParts(remaining)
            GlassView(
                level = left.coerceIn(0.14f, 0.82f), water = Ocean.WaterCalm, progress = left,
                value = value, unit = "$unit to next sip", status = "On track", tint = Ocean.Aqua,
                headline = "You're all set", subline = "Next check-in at ${clock(p.nextDueAtMs)}.",
                description = "Next check-in in $value $unit",
            )
        }
    }
}

/** The tint for the page's light, so the whole screen shifts colour with the state. */
fun phaseGlow(phase: Phase): Color = when (phase) {
    Phase.CHECK_IN_DUE, Phase.GRACE_PERIOD -> Ocean.Amber
    Phase.RESTRICTED_IN_APP, Phase.DEVICE_ENFORCEMENT_UNAVAILABLE -> Ocean.Coral
    Phase.EMERGENCY_BYPASS -> Ocean.Mint
    Phase.PAUSED -> Ocean.TextLow
    else -> Ocean.Aqua
}

// ---------- Sections ----------

@Composable
private fun TopBar(host: AppHost, state: AppState, now: Long) {
    val time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    val greeting = when (time.hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(dayFormat.format(time).uppercase(), style = MaterialTheme.typography.labelMedium, color = Ocean.TextLow)
            Spacer(Modifier.height(2.dp))
            Text(greeting, style = MaterialTheme.typography.headlineSmall, color = Ocean.TextHigh)
        }
        val balance = state.ledger.balance
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(Ocean.Glass).border(1.dp, Ocean.GlassBorder, RoundedCornerShape(50))
                .springClick { host.goToCredits() }
                .semantics { contentDescription = "$balance credits. Open credits." }
                .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoinGlyph(Ocean.Aqua, 18.dp)
            Spacer(Modifier.width(6.dp))
            Text("$balance", style = MaterialTheme.typography.titleMedium, color = Ocean.TextHigh)
        }
    }
}

@Composable
private fun Actions(host: AppHost, state: AppState, locked: Boolean, missed: Boolean, due: Boolean) {
    val p = state.policy
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            due -> PillButton("Check in with a photo", host.launchCamera, kind = ButtonKind.Amber) { CameraGlyph(it) }
            // Once the phone is locked only a credit unlocks it; a photo no longer does.
            locked -> PillButton("Emergency call", host.openDialer, kind = ButtonKind.Glass) { PhoneGlyph(it) }
            missed -> PillButton("Check in with a photo", host.launchCamera) { CameraGlyph(it) }
            p.phase == Phase.PAUSED -> PillButton("Resume schedule", { host.act { resume() } }) { PlayGlyph(it) }
        }
        if (missed) {
            val balance = state.ledger.balance
            PillButton(
                if (balance > 0) "Unlock with 1 credit" else "Borrow 1 credit to unlock",
                { if (balance > 0) host.act { redeemBypassWithCredit() } else host.goToCredits() },
                kind = if (locked) ButtonKind.Primary else ButtonKind.Glass,
            ) { CoinGlyph(it) }
            if (!locked) PillButton("Pause schedule", { host.act { pause() } }, kind = ButtonKind.Glass) { PauseGlyph(it) }
        } else if (p.phase != Phase.PAUSED) {
            QuietButton("Pause reminders", { host.act { pause() } }, Modifier.fillMaxWidth()) { PauseGlyph(it, 18.dp) }
        }
    }
}

@Composable
private fun AdvanceNotice(state: AppState, now: Long) {
    val open = state.ledger.advances(now).filter { it.status != AdvanceStatus.REPAID }
    if (open.isEmpty()) return
    val overdue = open.any { it.status == AdvanceStatus.OVERDUE }
    val tint = if (overdue) Ocean.Amber else Ocean.Aqua
    Spacer(Modifier.height(16.dp))
    GlassCard(padding = 16.dp) {
        ListRow(
            if (overdue) "An advance is overdue" else "${open.size} advance${if (open.size > 1) "s" else ""} to repay",
            if (overdue) "Repay it in Credits to take new ones." else "Due ${formatDateTime(open.minOf { it.dueAtMs })}",
            Modifier.padding(vertical = 0.dp),
            leading = { GlyphBadge(tint) { CoinGlyph(it, 20.dp) } },
        )
    }
}

@Composable
private fun TodayStats(state: AppState, now: Long) {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val count = state.history.count {
        (it.kind == HistoryKind.CHECK_IN || it.kind == HistoryKind.CHECK_IN_CONFIRMED_BY_USER) &&
            Instant.ofEpochMilli(it.atMs).atZone(zone).toLocalDate() == today
    }
    val next = state.policy.nextDueAtMs?.takeIf { state.policy.phase != Phase.PAUSED }
    StatStrip(
        listOf(
            Stat("Sips today", "$count", if (count > 0) Ocean.Aqua else Ocean.TextHigh),
            Stat("Next", next?.let(::formatClock) ?: "—"),
            Stat("Every", intervalLabel(state.schedule.intervalMinutes)),
        ),
    )
}

@Composable
private fun Recent(state: AppState) {
    val events = state.history.takeLast(4).reversed()
    if (events.isEmpty()) return
    Spacer(Modifier.height(24.dp))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Recent")
        GlassCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                events.forEachIndexed { index, event ->
                    if (index > 0) Hairline(Modifier.padding(start = 50.dp))
                    ListRow(labelFor(event.kind), formatDateTime(event.atMs), leading = { glyphFor(event.kind) })
                }
            }
        }
    }
}

@Composable
private fun glyphFor(kind: HistoryKind) = when (kind) {
    HistoryKind.CHECK_IN, HistoryKind.CHECK_IN_CONFIRMED_BY_USER, HistoryKind.ADVANCE_REPAID -> GlyphBadge(Ocean.Aqua, 36.dp) { CheckGlyph(it, 18.dp) }
    HistoryKind.BYPASS_EMERGENCY -> GlyphBadge(Ocean.Coral, 36.dp) { SosGlyph(it, 18.dp) }
    HistoryKind.PAUSED -> GlyphBadge(Ocean.TextMid, 36.dp) { PauseGlyph(it, 16.dp) }
    HistoryKind.RESUMED -> GlyphBadge(Ocean.TextMid, 36.dp) { PlayGlyph(it, 16.dp) }
    HistoryKind.BYPASS_CREDIT -> GlyphBadge(Ocean.Mint, 36.dp) { CoinGlyph(it, 18.dp) }
    else -> GlyphBadge(Ocean.Aqua, 36.dp) { CoinGlyph(it, 18.dp) }
}

fun labelFor(kind: HistoryKind) = when (kind) {
    HistoryKind.CHECK_IN -> "Check-in recorded"
    HistoryKind.CHECK_IN_CONFIRMED_BY_USER -> "Check-in confirmed by you"
    HistoryKind.BYPASS_CREDIT -> "Bypass using a credit"
    HistoryKind.BYPASS_EMERGENCY -> "Earlier override (no longer offered)"
    HistoryKind.PAUSED -> "Schedule paused"
    HistoryKind.RESUMED -> "Schedule resumed"
    HistoryKind.CREDITS_BOUGHT -> "Test credits added"
    HistoryKind.ADVANCE_TAKEN -> "Emergency advance taken"
    HistoryKind.ADVANCE_REPAID -> "Advance repaid"
}
