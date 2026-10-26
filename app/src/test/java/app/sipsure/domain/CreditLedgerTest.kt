package app.sipsure.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditLedgerTest {
    private val hour = 60L * 60 * 1000

    private fun LedgerResult.ledger(): CreditLedger = (this as LedgerResult.Applied).ledger
    private fun LedgerResult.reason() = (this as LedgerResult.Rejected).reason

    @Test
    fun purchaseIsIdempotent() {
        val once = CreditLedger.EMPTY.purchase("tx1", 0, 5).ledger()
        val replay = once.purchase("tx1", 10, 5) as LedgerResult.Applied
        assertTrue(replay.duplicate)
        assertEquals(5, replay.ledger.balance)
        assertEquals(1, replay.ledger.entries.size)
    }

    @Test
    fun redemptionNeedsBalanceAndIsIdempotent() {
        assertEquals(LedgerResult.Reason.INSUFFICIENT_BALANCE, CreditLedger.EMPTY.redeem("r1", 0).reason())
        val l = CreditLedger.EMPTY.purchase("p", 0, 2).ledger().redeem("r1", 1).ledger()
        assertEquals(1, l.balance)
        assertEquals(1, (l.redeem("r1", 2) as LedgerResult.Applied).ledger.balance)
    }

    @Test
    fun invalidAmountsRejected() {
        assertEquals(LedgerResult.Reason.INVALID_AMOUNT, CreditLedger.EMPTY.purchase("p", 0, 0).reason())
        assertEquals(LedgerResult.Reason.INVALID_AMOUNT, CreditLedger.EMPTY.redeem("r", 0, -1).reason())
    }

    @Test
    fun advanceHasTwentyFourHourDeadline() {
        val l = CreditLedger.EMPTY.takeAdvance("a1", 1000).ledger()
        assertEquals(1, l.balance)
        val adv = l.advances(1000).single()
        assertEquals(1000 + 24 * hour, adv.dueAtMs)
        assertEquals(AdvanceStatus.OUTSTANDING, adv.status)
    }

    @Test
    fun advanceRequiresEmptyBalance() {
        val l = CreditLedger.EMPTY.purchase("p", 0, 1).ledger()
        assertEquals(LedgerResult.Reason.BALANCE_NOT_EMPTY, l.takeAdvance("a1", 1).reason())
    }

    @Test
    fun neverMoreThanThreeOutstanding() {
        var l = CreditLedger.EMPTY
        for (i in 1..3) {
            l = l.takeAdvance("a$i", i.toLong()).ledger().redeem("r$i", i.toLong()).ledger()
        }
        assertEquals(0, l.advancesRemaining(10))
        assertEquals(LedgerResult.Reason.ADVANCE_LIMIT_REACHED, l.takeAdvance("a4", 10).reason())
    }

    @Test
    fun overdueAdvanceBlocksNewAdvancesButKeepsData() {
        val l = CreditLedger.EMPTY.takeAdvance("a1", 0).ledger().redeem("r1", 1).ledger()
        val later = 25 * hour
        assertTrue(l.hasOverdueAdvance(later))
        assertEquals(LedgerResult.Reason.ADVANCE_OVERDUE, l.takeAdvance("a2", later).reason())
        // Ledger history is untouched by overdue status.
        assertEquals(2, l.entries.size)
        assertEquals(AdvanceStatus.OVERDUE, l.advances(later).single().status)
    }

    @Test
    fun repaymentClearsOverdueAndIsIdempotent() {
        var l = CreditLedger.EMPTY.takeAdvance("a1", 0).ledger()
        val later = 25 * hour
        l = l.purchase("p", later, 1).ledger()
        l = l.repayAdvance("rep1", later, "a1").ledger()
        assertFalse(l.hasOverdueAdvance(later))
        assertEquals(AdvanceStatus.REPAID, l.advances(later).single().status)
        assertEquals(1, l.balance)
        assertTrue((l.repayAdvance("rep1", later, "a1") as LedgerResult.Applied).duplicate)
        assertEquals(LedgerResult.Reason.ADVANCE_ALREADY_REPAID, l.repayAdvance("rep2", later, "a1").reason())
    }

    @Test
    fun repaymentRejectedWhenUnknownOrUnfunded() {
        val l = CreditLedger.EMPTY.takeAdvance("a1", 0).ledger().redeem("r1", 1).ledger()
        assertEquals(LedgerResult.Reason.UNKNOWN_ADVANCE, l.repayAdvance("x", 2, "nope").reason())
        assertEquals(LedgerResult.Reason.INSUFFICIENT_BALANCE, l.repayAdvance("x", 2, "a1").reason())
    }

    @Test
    fun restoreValidatesPersistedEntries() {
        val l = CreditLedger.EMPTY.purchase("p", 0, 3).ledger().redeem("r", 1).ledger()
        assertEquals(2, CreditLedger.restore(l.entries)!!.balance)
        assertNull(CreditLedger.restore(l.entries + l.entries.first()))
        assertNull(CreditLedger.restore(listOf(LedgerEntry.Redemption("r", 0, 1))))
    }
}
