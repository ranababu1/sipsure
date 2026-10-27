package app.sipsure.domain

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HydrationControllerTest {
    private class MemoryStore(var saved: AppState? = null, var corrupt: Boolean = false) : StateStore {
        override fun load(): AppState? = if (corrupt) throw StateCorruptedException() else saved
        override fun save(state: AppState) { saved = state }
    }

    private val min = 60_000L
    private val noQuiet = Schedule(
        intervalMinutes = 60, checkInWindowMinutes = 10, graceMinutes = 5,
        quietStartMinuteOfDay = 0, quietEndMinuteOfDay = 0,
    )
    private var now = 1_000_000L * min
    private var ids = 0
    private val store = MemoryStore()

    private fun controller(s: MemoryStore = store) =
        HydrationController(s, { now }, { ZoneId.of("UTC") }, { "id${ids++}" }).also { it.start() }

    private fun ready() = controller().also { assertNull(it.completeSetup(noQuiet)) }
    private fun HydrationController.phase() = state.policy.phase
    private val good = CheckInVerdict(CheckInOutcome.ACCEPTED, "ok")

    private fun HydrationController.restrict() {
        now = state.policy.nextDueAtMs!! + 20 * min
        tick()
        assertEquals(Phase.RESTRICTED_IN_APP, phase())
    }

    @Test
    fun setupValidatesAndStartsSchedule() {
        val c = controller()
        assertNotNull(c.completeSetup(Schedule(intervalMinutes = 5)))
        assertEquals(Phase.SETUP_REQUIRED, c.phase())
        assertNull(c.completeSetup(noQuiet))
        assertEquals(Phase.ACTIVE, c.phase())
        assertEquals(now + 60 * min, c.state.policy.nextDueAtMs)
        assertNotNull(c.completeSetup(noQuiet))
    }

    @Test
    fun missedCheckInEscalatesToRestriction() {
        val c = ready()
        now += 60 * min; c.tick(); assertEquals(Phase.CHECK_IN_DUE, c.phase())
        now += 10 * min; c.tick(); assertEquals(Phase.GRACE_PERIOD, c.phase())
        now += 5 * min; c.tick(); assertEquals(Phase.RESTRICTED_IN_APP, c.phase())
    }

    @Test
    fun acceptedCheckInSchedulesNextAndRemembersHash() {
        val c = ready()
        now += 60 * min; c.tick()
        assertNull(c.submitCheckIn(good, 42L))
        assertEquals(Phase.ACTIVE, c.phase())
        assertEquals(now + 60 * min, c.state.policy.nextDueAtMs)
        assertEquals(listOf(42L), c.state.photoHashes)
        assertEquals(HistoryKind.CHECK_IN, c.state.history.last().kind)
    }

    @Test
    fun retryAndReviewDoNotRecordUntilConfirmed() {
        val c = ready()
        now += 60 * min; c.tick()
        assertNotNull(c.submitCheckIn(CheckInVerdict(CheckInOutcome.RETRY_NEEDED, "dark"), 1))
        assertNotNull(c.submitCheckIn(CheckInVerdict(CheckInOutcome.REVIEW_REQUIRED, "dupe"), 1))
        assertEquals(Phase.CHECK_IN_DUE, c.phase())
        assertNull(c.confirmReviewedCheckIn(1))
        assertEquals(HistoryKind.CHECK_IN_CONFIRMED_BY_USER, c.state.history.last().kind)
    }

    @Test
    fun checkInRejectedWhenNothingDue() {
        assertNotNull(ready().submitCheckIn(good, 1))
    }

    @Test
    fun creditBypassSpendsOneCreditAndLiftsRestriction() {
        val c = ready()
        assertNull(c.buyCredits(CreditPack(3, "3")))
        c.restrict()
        assertNull(c.redeemBypassWithCredit())
        assertEquals(Phase.EMERGENCY_BYPASS, c.phase())
        assertEquals(2, c.state.ledger.balance)
    }

    @Test
    fun bypassNeedsACreditAndBorrowingOneWorks() {
        val c = ready()
        c.restrict()
        assertNotNull(c.redeemBypassWithCredit())
        assertEquals(Phase.RESTRICTED_IN_APP, c.phase())
        assertNull(c.takeAdvance())
        assertNull(c.redeemBypassWithCredit())
        assertEquals(Phase.EMERGENCY_BYPASS, c.phase())
        assertEquals(0, c.state.ledger.balance)
        assertTrue(c.hasUnpaidAdvance())
    }

    @Test
    fun advanceThenBypassThenOverdueBlocksOnlyMoreAdvances() {
        val c = ready()
        c.restrict()
        assertNull(c.takeAdvance())
        assertNull(c.redeemBypassWithCredit())
        now += 25 * 60 * min
        c.tick()
        assertTrue(c.state.ledger.hasOverdueAdvance(now))
        assertNotNull(c.takeAdvance())
        // An overdue advance never deletes anything and never blocks pausing the (app-level) restriction.
        assertNull(c.pause())
        assertEquals(Phase.PAUSED, c.phase())
        assertEquals(1, c.state.ledger.entries.count { it is LedgerEntry.Advance })
        assertTrue(c.hasUnpaidAdvance())
    }

    @Test
    fun repayingAdvanceNeedsCredits() {
        val c = ready()
        assertNull(c.takeAdvance())
        val id = c.state.ledger.entries.first().txId
        assertNull(c.redeemBypassWithCredit().let { c.repayAdvance(id) })
        assertEquals(AdvanceStatus.REPAID, c.state.ledger.advances(now).single().status)
    }

    @Test
    fun pauseStopsEscalationAndResumeRestartsInterval() {
        val c = ready()
        assertNull(c.pause())
        now += 1000 * min; c.tick()
        assertEquals(Phase.PAUSED, c.phase())
        assertNull(c.resume())
        assertEquals(Phase.ACTIVE, c.phase())
        assertEquals(now + 60 * min, c.state.policy.nextDueAtMs)
        assertNotNull(c.resume())
    }

    @Test
    fun rebootAfterMissedDueRestartsInsteadOfPunishing() {
        val c = ready()
        now += 600 * min
        c.onSystemRecovered()
        assertEquals(Phase.ACTIVE, c.phase())
        assertEquals(now + 60 * min, c.state.policy.nextDueAtMs)
    }

    @Test
    fun stateSurvivesRestartAndCorruptionRecovers() {
        val c = ready()
        c.buyCredits(CreditPack(3, "3"))
        val again = controller()
        assertEquals(3, again.state.ledger.balance)
        assertEquals(Phase.ACTIVE, again.phase())

        val broken = controller(MemoryStore(corrupt = true))
        assertEquals(Phase.RECOVERY_REQUIRED, broken.phase())
        assertNull(broken.recoverFromCorruption())
        assertEquals(Phase.SETUP_REQUIRED, broken.phase())
    }

    private fun lockController(supported: () -> Boolean) =
        HydrationController(store, { now }, { ZoneId.of("UTC") }, { "id${ids++}" }, supported).also {
            it.start()
            assertNull(it.completeSetup(noQuiet))
        }

    @Test
    fun phoneLockCannotBeEnabledWithoutDeviceSupport() {
        val c = lockController { false }
        assertNotNull(c.setDeviceLock(true))
        assertEquals(false, c.state.deviceLockEnabled)
    }

    @Test
    fun photoCheckInStillPreventsTheLockBeforeItEngages() {
        val c = lockController { true }
        c.setDeviceLock(true)
        now = c.state.policy.nextDueAtMs!!
        c.tick()
        assertEquals(Phase.CHECK_IN_DUE, c.phase())
        assertEquals(false, c.deviceLockActive)
        assertNull(c.submitCheckIn(good, 1))
        assertEquals(Phase.ACTIVE, c.phase())
    }

    @Test
    fun onceLockedOnlyACreditUnlocks() {
        val c = lockController { true }
        c.setDeviceLock(true)
        c.restrict()
        assertTrue(c.deviceLockActive)

        // No photo, no pause, no settings change, and no free override (it no longer exists).
        assertEquals(HydrationController.LOCKED_MESSAGE, c.submitCheckIn(good, 1))
        assertEquals(HydrationController.LOCKED_MESSAGE, c.confirmReviewedCheckIn(1))
        assertEquals(HydrationController.LOCKED_MESSAGE, c.pause())
        assertEquals(HydrationController.LOCKED_MESSAGE, c.setDeviceLock(false))
        assertEquals(Phase.RESTRICTED_IN_APP, c.phase())
        assertTrue(c.deviceLockActive)

        // Without credits the only way out is to borrow one, then spend it.
        assertNotNull(c.redeemBypassWithCredit())
        assertNull(c.takeAdvance())
        assertNull(c.redeemBypassWithCredit())
        assertEquals(false, c.deviceLockActive)
    }

    @Test
    fun boughtCreditAlsoUnlocksWithoutAnyAdvance() {
        val c = lockController { true }
        c.setDeviceLock(true)
        c.buyCredits(CreditPack(1, "1"))
        c.restrict()
        assertTrue(c.deviceLockActive)
        assertNull(c.redeemBypassWithCredit())
        assertEquals(false, c.deviceLockActive)
        assertEquals(false, c.hasUnpaidAdvance())
    }

    @Test
    fun uninstallAndRemovalAreBlockedUntilTheLoanIsRepaid() {
        val c = lockController { true }
        assertEquals(false, c.hasUnpaidAdvance())
        assertNull(c.removeLockSetupBlocker())

        assertNull(c.takeAdvance())
        assertTrue(c.hasUnpaidAdvance())
        assertNotNull(c.removeLockSetupBlocker())

        // Still blocked once the advance is overdue.
        now += 25 * 60 * min
        assertTrue(c.hasUnpaidAdvance())
        assertNotNull(c.removeLockSetupBlocker())

        val id = c.state.ledger.entries.first { it is LedgerEntry.Advance }.txId
        assertNull(c.repayAdvance(id))
        assertEquals(false, c.hasUnpaidAdvance())
        assertNull(c.removeLockSetupBlocker())
    }

    @Test
    fun removalIsRefusedWhileLocked() {
        val c = lockController { true }
        c.setDeviceLock(true)
        c.restrict()
        assertEquals(HydrationController.LOCKED_MESSAGE, c.removeLockSetupBlocker())
    }

    @Test
    fun lostDeviceOwnershipFallsBackToAppLevelRestriction() {
        var supported = true
        val c = lockController { supported }
        c.setDeviceLock(true)
        supported = false
        now = c.state.policy.nextDueAtMs!! + 20 * min
        c.tick()
        assertEquals(Phase.DEVICE_ENFORCEMENT_UNAVAILABLE, c.phase())
        assertEquals(false, c.deviceLockActive)
        assertTrue(c.state.policy.appRestricted)
    }

    @Test
    fun updatingScheduleRecomputesNextDue() {
        val c = ready()
        assertNull(c.updateSchedule(noQuiet.copy(intervalMinutes = 120)))
        assertEquals(now + 120 * min, c.state.policy.nextDueAtMs)
        assertNotNull(c.updateSchedule(noQuiet.copy(intervalMinutes = 1)))
    }
}
