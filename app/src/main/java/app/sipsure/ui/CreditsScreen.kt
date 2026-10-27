package app.sipsure.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sipsure.domain.AdvanceStatus
import app.sipsure.domain.AdvanceTerms
import app.sipsure.domain.HydrationController
import app.sipsure.domain.LedgerEntry
import app.sipsure.ui.theme.Ocean
import kotlin.math.max

@Composable
fun CreditsScreen(host: AppHost, now: Long, onBack: () -> Unit = {}) {
    val ledger = host.state.ledger
    var confirmAdvance by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val remaining = ledger.advancesRemaining(now)

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // While the phone is locked the nav bar is hidden, so give a clear way back.
        if (host.deviceLockActive) PillButton("Back", onBack, kind = ButtonKind.Glass)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Credits", style = MaterialTheme.typography.headlineLarge, color = Ocean.TextHigh, modifier = Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(50)).springClick { showInfo = true }) {
                StatusPill("Test mode", Ocean.Amber)
            }
        }

        // Balance
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1479C4), Color(0xFF0E5A8F), Color(0xFF0A3E63))))
                .border(1.dp, Ocean.GlassBorder, RoundedCornerShape(32.dp)).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CoinGlyph(Color.White.copy(alpha = 0.9f), 34.dp)
            Spacer(Modifier.height(6.dp))
            Text(ledger.balance.toString(), style = MaterialTheme.typography.displayLarge.copy(fontSize = 84.sp), color = Color.White)
            Text(
                if (ledger.balance == 1) "credit" else "credits",
                style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(10.dp))
            Text("One credit skips one restriction for an hour.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HydrationController.CREDIT_PACKS.forEach { pack ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(26.dp)).background(Ocean.Glass)
                        .border(1.dp, Ocean.GlassBorder, RoundedCornerShape(26.dp))
                        .springClick { host.act { buyCredits(pack) } }.padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("+${pack.credits}", style = MaterialTheme.typography.displayMedium, color = Ocean.Aqua)
                    Text("credits", style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid)
                    Spacer(Modifier.height(10.dp))
                    StatusPill("Add free", Ocean.Aqua)
                }
            }
        }

        SectionLabel("Emergency advance")
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Out of credits?", style = MaterialTheme.typography.titleLarge, color = Ocean.TextHigh)
                    Text(
                        "Borrow ${AdvanceTerms.CREDITS_PER_ADVANCE}, repay within 24 hours.",
                        style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(AdvanceTerms.MAX_OUTSTANDING) { i ->
                        Box(Modifier.size(12.dp).clip(CircleShape).background(if (i < remaining) Ocean.Aqua else Color.White.copy(alpha = 0.16f)))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            PillButton("Take an advance", { confirmAdvance = true }, kind = ButtonKind.Glass) { CoinGlyph(it) }
            Spacer(Modifier.height(10.dp))
            Text(
                "An unpaid advance blocks new ones and stops SipSure being uninstalled until it is repaid. Nothing is deleted.",
                style = MaterialTheme.typography.bodyMedium, color = Ocean.TextLow,
            )
        }

        ledger.advances(now).filter { it.status != AdvanceStatus.REPAID }.forEach { advance ->
            val overdue = advance.status == AdvanceStatus.OVERDUE
            val issuedAt = advance.dueAtMs - AdvanceTerms.REPAYMENT_WINDOW_MS
            val elapsed = ((now - issuedAt).toFloat() / AdvanceTerms.REPAYMENT_WINDOW_MS).coerceIn(0f, 1f)
            val tint = if (overdue) Ocean.Amber else Ocean.Aqua
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (overdue) "Advance overdue" else "Advance to repay", style = MaterialTheme.typography.titleMedium, color = Ocean.TextHigh)
                        Text("Due ${formatDateTime(advance.dueAtMs)}", style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid)
                    }
                    Text("${advance.credits}", style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp), color = tint)
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { elapsed }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = tint, trackColor = Color.White.copy(alpha = 0.12f), gapSize = 0.dp, drawStopIndicator = {},
                )
                Spacer(Modifier.height(14.dp))
                PillButton("Repay with 1 credit", { host.act { repayAdvance(advance.txId) } }, kind = ButtonKind.Glass) { CheckGlyph(it) }
            }
        }

        SectionLabel("History")
        GlassCard(padding = 6.dp) {
            if (ledger.entries.isEmpty()) {
                Text("No transactions yet.", Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid)
            }
            val entries = ledger.entries.takeLast(8).reversed()
            entries.forEach { entry ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entryLabel(entry), style = MaterialTheme.typography.titleMedium, color = Ocean.TextHigh)
                        Text(formatDateTime(entry.atMs), style = MaterialTheme.typography.bodyMedium, color = Ocean.TextLow)
                    }
                    Text(
                        (if (entry.delta > 0) "+" else "") + entry.delta,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (entry.delta > 0) Ocean.Aqua else Ocean.TextMid,
                    )
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Test mode") },
            text = {
                Text(
                    "Credits are free right now and no real money is charged. Real purchases will come after billing and store rules are reviewed, " +
                        "and refund and support details will appear here before any real purchase.",
                )
            },
            confirmButton = { TextButton({ showInfo = false }) { Text("Got it") } },
        )
    }
    if (confirmAdvance) {
        val due = now + AdvanceTerms.REPAYMENT_WINDOW_MS
        AlertDialog(
            onDismissRequest = { confirmAdvance = false },
            title = { Text("Emergency advance") },
            text = {
                Text(
                    "You'll receive ${AdvanceTerms.CREDITS_PER_ADVANCE} credit now. Repay it by ${formatDateTime(due)} (24 hours).\n\n" +
                        "Advances left after this one: ${max(remaining - 1, 0)} of ${AdvanceTerms.MAX_OUTSTANDING}.\n\n" +
                        "If you don't repay in time it's marked overdue and you can't take more advances. Until it is repaid, SipSure can't be uninstalled. Nothing is deleted.",
                )
            },
            confirmButton = { TextButton({ confirmAdvance = false; host.act { takeAdvance() } }) { Text("Accept advance") } },
            dismissButton = { TextButton({ confirmAdvance = false }) { Text("Cancel") } },
        )
    }
}

private fun entryLabel(e: LedgerEntry) = when (e) {
    is LedgerEntry.Purchase -> "Test credits added"
    is LedgerEntry.Redemption -> "Credit used for a bypass"
    is LedgerEntry.Advance -> "Emergency advance"
    is LedgerEntry.Repayment -> "Advance repaid"
}
