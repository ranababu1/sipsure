package app.sipsure.data

import app.sipsure.domain.AppState
import app.sipsure.domain.CreditLedger
import app.sipsure.domain.HistoryEvent
import app.sipsure.domain.HistoryKind
import app.sipsure.domain.LedgerResult
import app.sipsure.domain.Phase
import app.sipsure.domain.PolicyState
import app.sipsure.domain.Schedule
import app.sipsure.domain.StateCorruptedException
import org.junit.Assert.assertEquals
import org.junit.Test

class StateCodecTest {
    private fun ledger(): CreditLedger {
        var l = (CreditLedger.EMPTY.purchase("p1", 1, 3) as LedgerResult.Applied).ledger
        l = (l.redeem("r1", 2) as LedgerResult.Applied).ledger
        l = (l.redeem("r2", 3, 2) as LedgerResult.Applied).ledger
        l = (l.takeAdvance("a1", 4) as LedgerResult.Applied).ledger
        l = (l.purchase("p2", 5, 1) as LedgerResult.Applied).ledger
        return (l.repayAdvance("rep1", 6, "a1") as LedgerResult.Applied).ledger
    }

    private val sample = AppState(
        schedule = Schedule(90, 20, 5, 23 * 60, 6 * 60),
        policy = PolicyState(Phase.GRACE_PERIOD, 1000, 2000, 3000, null),
        ledger = ledger(),
        photoHashes = listOf(Long.MIN_VALUE, -1L, 0x7FFFFFFFFFFFFFFFL),
        history = listOf(HistoryEvent(5, HistoryKind.CHECK_IN)),
    )

    @Test
    fun roundTripPreservesEverythingIncludingFullRangeHashes() {
        val back = StateCodec.decode(StateCodec.encode(sample))
        assertEquals(sample.schedule, back.schedule)
        assertEquals(sample.policy, back.policy)
        assertEquals(sample.ledger.entries, back.ledger.entries)
        assertEquals(sample.photoHashes, back.photoHashes)
        assertEquals(sample.history, back.history)
    }

    @Test(expected = StateCorruptedException::class)
    fun garbageIsReportedAsCorruption() {
        StateCodec.decode("not json")
    }

    @Test(expected = StateCorruptedException::class)
    fun unknownPhaseIsReportedAsCorruption() {
        StateCodec.decode(StateCodec.encode(sample).replace("GRACE_PERIOD", "NOPE"))
    }

    @Test(expected = StateCorruptedException::class)
    fun inconsistentLedgerIsReportedAsCorruption() {
        StateCodec.decode(StateCodec.encode(sample).replace("\"t\":\"purchase\"", "\"t\":\"redemption\""))
    }
}
