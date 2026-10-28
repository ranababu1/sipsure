package app.sipsure.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // While the phone is locked the nav bar is hidden, so give a clear way back.
        if (host.deviceLockActive) QuietButton("Back to Today", onBack) { BackGlyph(it, 16.dp) }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WALLET", style = MaterialTheme.typography.labelMedium, color = Ocean.TextLow)
                Text("Credits", style = MaterialTheme.typography.headlineLarge, color = Ocean.TextHigh)
            }
            Box(Modifier.clip(RoundedCornerShape(50)).springClick { showInfo = true }) {
                StatusPill("Test mode", Ocean.Amber, pulse = false)
            }
        }

        BalanceCard(ledger.balance)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HydrationController.CREDIT_PACKS.forEach { pack ->
                val shape = RoundedCornerShape(22.dp)
                Row(
                    Modifier.weight(1f).clip(shape).background(Ocean.Surface)
                        .border(1.dp, Ocean.GlassBorder, shape)
                        .springClick { host.act { buyCredits(pack) } }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("+${pack.credits}", style = MaterialTheme.typography.displaySmall, color = Ocean.TextHigh)
                        Text("Free in test mode", style = MaterialTheme.typography.bodySmall, color = Ocean.TextLow)
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Ocean.Aqua), contentAlignment = Alignment.Center) {
                        PlusGlyph(Color(0xFF041A26))
                    }
                }
            }
        }

        SectionLabel("Emergency advance")
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Out of credits?", style = MaterialTheme.typography.titleLarge, color = Ocean.TextHigh)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Borrow ${AdvanceTerms.CREDITS_PER_ADVANCE}, repay within 24 hours.",
                        style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(AdvanceTerms.MAX_OUTSTANDING) { i ->
                            Box(Modifier.width(16.dp).height(6.dp).clip(CircleShape).background(if (i < remaining) Ocean.Aqua else Color.White.copy(alpha = 0.12f)))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("$remaining left", style = MaterialTheme.typography.bodySmall, color = Ocean.TextLow)
                }
            }
            Spacer(Modifier.height(16.dp))
            PillButton("Take an advance", { confirmAdvance = true }, kind = ButtonKind.Glass) { CoinGlyph(it) }
            Spacer(Modifier.height(12.dp))
            Text(
                "An unpaid advance blocks new ones and stops SipSure being uninstalled until it is repaid. Nothing is deleted.",
                style = MaterialTheme.typography.bodySmall, color = Ocean.TextLow,
            )
        }

        ledger.advances(now).filter { it.status != AdvanceStatus.REPAID }.forEach { advance ->
            val overdue = advance.status == AdvanceStatus.OVERDUE
            val issuedAt = advance.dueAtMs - AdvanceTerms.REPAYMENT_WINDOW_MS
            val elapsed = ((now - issuedAt).toFloat() / AdvanceTerms.REPAYMENT_WINDOW_MS).coerceIn(0f, 1f)
            val tint = if (overdue) Ocean.Amber else Ocean.Aqua
            GlassCard {
                ListRow(
                    if (overdue) "Advance overdue" else "Advance to repay",
                    "Due ${formatDateTime(advance.dueAtMs)}",
                    Modifier.padding(vertical = 0.dp),
                    leading = { GlyphBadge(tint) { CoinGlyph(it, 20.dp) } },
                ) {
                    Text("${advance.credits}", style = MaterialTheme.typography.displaySmall, color = tint)
                }
                Spacer(Modifier.height(14.dp))
                Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                    val y = size.height / 2
                    drawLine(Color.White.copy(alpha = 0.10f), Offset(y, y), Offset(size.width - y, y), size.height, StrokeCap.Round)
                    drawLine(tint, Offset(y, y), Offset(y + (size.width - 2 * y) * elapsed, y), size.height, StrokeCap.Round)
                }
                Spacer(Modifier.height(14.dp))
                PillButton("Repay with 1 credit", { host.act { repayAdvance(advance.txId) } }, kind = ButtonKind.Glass) { CheckGlyph(it) }
            }
        }

        SectionLabel("History")
        GlassCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                if (ledger.entries.isEmpty()) {
                    Text("No transactions yet.", Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodyMedium, color = Ocean.TextMid)
                }
                ledger.entries.takeLast(8).reversed().forEachIndexed { index, entry ->
                    if (index > 0) Hairline()
                    ListRow(entryLabel(entry), formatDateTime(entry.atMs)) {
                        Text(
                            (if (entry.delta > 0) "+" else "") + entry.delta,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (entry.delta > 0) Ocean.Mint else Ocean.TextMid,
                        )
                    }
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

/** The balance, set on a deep water gradient with a soft wave across the bottom. */
@Composable
private fun BalanceCard(balance: Int) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier.fillMaxWidth().height(196.dp).clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF1C6FE0), Color(0xFF0F3F86), Color(0xFF0A2147))))
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), size.width * 0.42f, Offset(size.width * 0.95f, -size.height * 0.05f))
            drawCircle(Ocean.Aqua.copy(alpha = 0.12f), size.width * 0.3f, Offset(size.width * 0.9f, size.height * 1.05f))
        }
        Column(Modifier.padding(24.dp)) {
            Text("Balance", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.75f))
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$balance", style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, lineHeight = 72.sp), color = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (balance == 1) "credit" else "credits", Modifier.padding(bottom = 12.dp),
                    style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f),
                )
            }
            Text("One credit lifts one restriction for an hour.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun PlusGlyph(color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val w = size.width
        drawLine(color, Offset(w / 2, w * 0.1f), Offset(w / 2, w * 0.9f), w * 0.16f, StrokeCap.Round)
        drawLine(color, Offset(w * 0.1f, w / 2), Offset(w * 0.9f, w / 2), w * 0.16f, StrokeCap.Round)
    }
}

private fun entryLabel(e: LedgerEntry) = when (e) {
    is LedgerEntry.Purchase -> "Test credits added"
    is LedgerEntry.Redemption -> "Credit used for a bypass"
    is LedgerEntry.Advance -> "Emergency advance"
    is LedgerEntry.Repayment -> "Advance repaid"
}
