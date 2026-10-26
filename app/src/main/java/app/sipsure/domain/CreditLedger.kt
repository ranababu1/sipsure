package app.sipsure.domain

/** Terms for emergency advances (plan.md 5.4): at most three outstanding, 24h repayment each. */
object AdvanceTerms {
    const val MAX_OUTSTANDING = 3
    const val REPAYMENT_WINDOW_MS = 24L * 60 * 60 * 1000
    const val CREDITS_PER_ADVANCE = 1
}

sealed interface LedgerEntry {
    /** Unique id; replaying the same id is a no-op (idempotency). */
    val txId: String
    val atMs: Long
    val delta: Int

    data class Purchase(override val txId: String, override val atMs: Long, val credits: Int) : LedgerEntry {
        override val delta get() = credits
    }

    data class Redemption(override val txId: String, override val atMs: Long, val credits: Int) : LedgerEntry {
        override val delta get() = -credits
    }

    data class Advance(override val txId: String, override val atMs: Long, val dueAtMs: Long, val credits: Int) : LedgerEntry {
        override val delta get() = credits
    }

    data class Repayment(override val txId: String, override val atMs: Long, val advanceTxId: String, val credits: Int) : LedgerEntry {
        override val delta get() = -credits
    }
}

enum class AdvanceStatus { OUTSTANDING, OVERDUE, REPAID }

data class AdvanceView(val txId: String, val credits: Int, val dueAtMs: Long, val status: AdvanceStatus)

sealed interface LedgerResult {
    /** [duplicate] is true when the txId was already applied and nothing changed. */
    data class Applied(val ledger: CreditLedger, val duplicate: Boolean = false) : LedgerResult

    data class Rejected(val reason: Reason) : LedgerResult

    enum class Reason {
        INVALID_AMOUNT,
        INSUFFICIENT_BALANCE,
        BALANCE_NOT_EMPTY,
        ADVANCE_LIMIT_REACHED,
        ADVANCE_OVERDUE,
        UNKNOWN_ADVANCE,
        ADVANCE_ALREADY_REPAID,
    }
}

/**
 * Immutable, append-only credit ledger. Every mutation returns a new ledger so callers can persist
 * the entry list atomically. Overdue advances only block further advances; nothing else is penalised.
 */
class CreditLedger private constructor(val entries: List<LedgerEntry>) {

    val balance: Int get() = entries.sumOf { it.delta }

    fun advances(nowMs: Long): List<AdvanceView> {
        val repaid = entries.filterIsInstance<LedgerEntry.Repayment>().map { it.advanceTxId }.toSet()
        return entries.filterIsInstance<LedgerEntry.Advance>().map {
            val status = when {
                it.txId in repaid -> AdvanceStatus.REPAID
                nowMs >= it.dueAtMs -> AdvanceStatus.OVERDUE
                else -> AdvanceStatus.OUTSTANDING
            }
            AdvanceView(it.txId, it.credits, it.dueAtMs, status)
        }
    }

    fun outstandingAdvances(nowMs: Long) = advances(nowMs).filter { it.status != AdvanceStatus.REPAID }

    fun hasOverdueAdvance(nowMs: Long) = advances(nowMs).any { it.status == AdvanceStatus.OVERDUE }

    fun advancesRemaining(nowMs: Long) = (AdvanceTerms.MAX_OUTSTANDING - outstandingAdvances(nowMs).size).coerceAtLeast(0)

    fun purchase(txId: String, nowMs: Long, credits: Int): LedgerResult =
        append(txId) {
            if (credits <= 0) return LedgerResult.Rejected(LedgerResult.Reason.INVALID_AMOUNT)
            LedgerEntry.Purchase(txId, nowMs, credits)
        }

    fun redeem(txId: String, nowMs: Long, credits: Int = 1): LedgerResult =
        append(txId) {
            if (credits <= 0) return LedgerResult.Rejected(LedgerResult.Reason.INVALID_AMOUNT)
            if (balance < credits) return LedgerResult.Rejected(LedgerResult.Reason.INSUFFICIENT_BALANCE)
            LedgerEntry.Redemption(txId, nowMs, credits)
        }

    fun takeAdvance(txId: String, nowMs: Long): LedgerResult =
        append(txId) {
            if (balance > 0) return LedgerResult.Rejected(LedgerResult.Reason.BALANCE_NOT_EMPTY)
            if (hasOverdueAdvance(nowMs)) return LedgerResult.Rejected(LedgerResult.Reason.ADVANCE_OVERDUE)
            if (advancesRemaining(nowMs) == 0) return LedgerResult.Rejected(LedgerResult.Reason.ADVANCE_LIMIT_REACHED)
            LedgerEntry.Advance(txId, nowMs, nowMs + AdvanceTerms.REPAYMENT_WINDOW_MS, AdvanceTerms.CREDITS_PER_ADVANCE)
        }

    fun repayAdvance(txId: String, nowMs: Long, advanceTxId: String): LedgerResult =
        append(txId) {
            val advance = advances(nowMs).firstOrNull { it.txId == advanceTxId }
                ?: return LedgerResult.Rejected(LedgerResult.Reason.UNKNOWN_ADVANCE)
            if (advance.status == AdvanceStatus.REPAID) {
                return LedgerResult.Rejected(LedgerResult.Reason.ADVANCE_ALREADY_REPAID)
            }
            if (balance < advance.credits) return LedgerResult.Rejected(LedgerResult.Reason.INSUFFICIENT_BALANCE)
            LedgerEntry.Repayment(txId, nowMs, advanceTxId, advance.credits)
        }

    private inline fun append(txId: String, build: () -> LedgerEntry): LedgerResult {
        if (entries.any { it.txId == txId }) return LedgerResult.Applied(this, duplicate = true)
        return LedgerResult.Applied(CreditLedger(entries + build()))
    }

    companion object {
        val EMPTY = CreditLedger(emptyList())

        /** Rebuilds a ledger from persisted entries; rejects duplicate ids and a negative running balance. */
        fun restore(entries: List<LedgerEntry>): CreditLedger? {
            if (entries.map { it.txId }.toSet().size != entries.size) return null
            var running = 0
            for (e in entries) {
                running += e.delta
                if (running < 0) return null
            }
            return CreditLedger(entries)
        }
    }
}
