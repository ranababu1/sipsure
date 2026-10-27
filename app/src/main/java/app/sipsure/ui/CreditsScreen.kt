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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sipsure.domain.AdvanceStatus
import app.sipsure.domain.AdvanceTerms
import app.sipsure.domain.HydrationController
import app.sipsure.domain.LedgerEntry
import app.sipsure.ui.theme.Hero
import kotlin.math.max

@Composable
fun CreditsScreen(host: AppHost, now: Long) {
    val ledger = host.state.ledger
    var confirmAdvance by remember { mutableStateOf(false) }
    val remaining = ledger.advancesRemaining(now)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Credits", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

        HeroCard(Hero.Calm) {
            Text("Your balance", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
            Text(ledger.balance.toString(), style = MaterialTheme.typography.displayLarge, color = Color.White)
            Text(
                if (ledger.balance == 1) "credit" else "credits",
                style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "One credit skips one restriction for an hour.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center,
            )
        }

        SoftCard(container = MaterialTheme.colorScheme.secondaryContainer) {
            Row(verticalAlignment = Alignment.Top) {
                Icon24(Icons.Filled.Info, MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Test mode: credits are free and no real money is charged. Real purchases come after billing and store rules are reviewed, " +
                        "and refund and support details will appear here first.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HydrationController.CREDIT_PACKS.forEach { pack ->
                SoftCard(Modifier.weight(1f)) {
                    Text("+${pack.credits}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("credits (test)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Add", { host.act { buyCredits(pack) } }, icon = Icons.Filled.Add)
                }
            }
        }

        SectionTitle("Emergency advances")
        SoftCard {
            Text(
                "$remaining of ${AdvanceTerms.MAX_OUTSTANDING} advances available",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Out of credits? An advance gives you ${AdvanceTerms.CREDITS_PER_ADVANCE} credit now, to be repaid within 24 hours. " +
                    "If it isn't, it's marked overdue and you can't take more advances until it's repaid. Nothing is deleted and your phone keeps working.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Take an emergency advance", { confirmAdvance = true }, icon = Icons.Filled.Star)
        }

        ledger.advances(now).filter { it.status != AdvanceStatus.REPAID }.forEach { advance ->
            val overdue = advance.status == AdvanceStatus.OVERDUE
            val issuedAt = advance.dueAtMs - AdvanceTerms.REPAYMENT_WINDOW_MS
            val elapsed = ((now - issuedAt).toFloat() / AdvanceTerms.REPAYMENT_WINDOW_MS).coerceIn(0f, 1f)
            SoftCard(container = if (overdue) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface) {
                Text(
                    if (overdue) "Advance overdue" else "Advance outstanding",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (overdue) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${advance.credits} credit · due ${formatDateTime(advance.dueAtMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (overdue) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { elapsed },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (overdue) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(Modifier.height(14.dp))
                SecondaryButton("Repay with 1 credit", { host.act { repayAdvance(advance.txId) } }, icon = Icons.Filled.Check)
            }
        }

        SectionTitle("History")
        SoftCard {
            if (ledger.entries.isEmpty()) {
                Text("No transactions yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val entries = ledger.entries.takeLast(10).reversed()
            entries.forEachIndexed { index, entry ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entryLabel(entry), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(formatDateTime(entry.atMs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        (if (entry.delta > 0) "+" else "") + entry.delta,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (entry.delta > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index != entries.lastIndex) Spacer(Modifier.height(14.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmAdvance) {
        val due = now + AdvanceTerms.REPAYMENT_WINDOW_MS
        AlertDialog(
            onDismissRequest = { confirmAdvance = false },
            title = { Text("Emergency advance") },
            text = {
                Text(
                    "You'll receive ${AdvanceTerms.CREDITS_PER_ADVANCE} credit now. Repay it by ${formatDateTime(due)} (24 hours).\n\n" +
                        "Advances remaining after this one: ${max(remaining - 1, 0)} of ${AdvanceTerms.MAX_OUTSTANDING}.\n\n" +
                        "If you don't repay in time it's marked overdue and you can't take more advances. Nothing is deleted and your phone is never locked because of it.",
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

@Composable
private fun Icon24(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    androidx.compose.material3.Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
}
