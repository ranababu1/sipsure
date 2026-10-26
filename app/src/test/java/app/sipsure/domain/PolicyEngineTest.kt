package app.sipsure.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyEngineTest {
    private val min = 60_000L
    private val config = PolicyConfig(checkInWindowMs = 10 * min, graceMs = 5 * min)
    private val due = 100 * min

    private fun active() = PolicyEngine.reduce(PolicyState(), PolicyEvent.SetupCompleted(due), config)
    private fun at(s: PolicyState, nowMs: Long, c: PolicyConfig = config) = PolicyEngine.advance(s, nowMs, c)

    @Test
    fun setupMovesToActive() {
        assertEquals(Phase.SETUP_REQUIRED, PolicyState().phase)
        assertEquals(Phase.ACTIVE, active().phase)
        // Duplicate setup event is ignored.
        val again = PolicyEngine.reduce(active(), PolicyEvent.SetupCompleted(1), config)
        assertEquals(due, again.nextDueAtMs)
    }

    @Test
    fun escalationFollowsWindowThenGraceThenRestriction() {
        assertEquals(Phase.ACTIVE, at(active(), due - 1).phase)
        assertEquals(Phase.CHECK_IN_DUE, at(active(), due).phase)
        assertEquals(Phase.CHECK_IN_DUE, at(active(), due + 10 * min - 1).phase)
        assertEquals(Phase.GRACE_PERIOD, at(active(), due + 10 * min).phase)
        assertEquals(Phase.RESTRICTED_IN_APP, at(active(), due + 15 * min).phase)
        assertTrue(at(active(), due + 15 * min).appRestricted)
    }

    @Test
    fun clockGoingBackwardsDoesNotEscalateFurther() {
        val restricted = at(active(), due + 15 * min)
        assertEquals(Phase.RESTRICTED_IN_APP, at(restricted, 0).phase)
        val dueState = at(active(), due)
        assertEquals(Phase.CHECK_IN_DUE, at(dueState, due - 50 * min).phase)
    }

    @Test
    fun unsupportedDeviceEnforcementFallsBackToAppLevel() {
        val c = config.copy(deviceEnforcementRequested = true, deviceEnforcementSupported = false)
        val s = at(active(), due + 15 * min, c)
        assertEquals(Phase.DEVICE_ENFORCEMENT_UNAVAILABLE, s.phase)
        assertTrue(s.appRestricted)
    }

    @Test
    fun checkInAcceptedFromAnyEscalationPhaseStartsNextInterval() {
        for (now in listOf(due, due + 10 * min, due + 15 * min)) {
            val s = PolicyEngine.reduce(at(active(), now), PolicyEvent.CheckInAccepted(due + 60 * min), config)
            assertEquals(Phase.CHECK_IN_ACCEPTED, s.phase)
            val settled = at(s, now)
            assertEquals(Phase.ACTIVE, settled.phase)
            assertEquals(due + 60 * min, settled.nextDueAtMs)
            assertFalse(settled.appRestricted)
        }
    }

    @Test
    fun checkInIgnoredWhenNothingIsDue() {
        val s = PolicyEngine.reduce(active(), PolicyEvent.CheckInAccepted(5), config)
        assertEquals(active(), s)
    }

    @Test
    fun bypassLiftsRestrictionAndExpiresBackToActive() {
        val restricted = at(active(), due + 15 * min)
        val bypass = PolicyEngine.reduce(restricted, PolicyEvent.BypassStarted(due + 75 * min, due + 90 * min), config)
        assertEquals(Phase.EMERGENCY_BYPASS, bypass.phase)
        assertFalse(bypass.appRestricted)
        assertEquals(Phase.EMERGENCY_BYPASS, at(bypass, due + 74 * min).phase)
        val after = at(bypass, due + 75 * min)
        assertEquals(Phase.ACTIVE, after.phase)
        assertEquals(due + 90 * min, after.nextDueAtMs)
    }

    @Test
    fun pauseAndResumeWorkFromRestriction() {
        val restricted = at(active(), due + 15 * min)
        val paused = PolicyEngine.reduce(restricted, PolicyEvent.PauseRequested, config)
        assertEquals(Phase.PAUSED, paused.phase)
        // Time passing never escalates a paused schedule.
        assertEquals(Phase.PAUSED, at(paused, due + 1000 * min).phase)
        val resumed = PolicyEngine.reduce(paused, PolicyEvent.Resumed(due + 2000 * min), config)
        assertEquals(Phase.ACTIVE, resumed.phase)
        assertEquals(due + 2000 * min, resumed.nextDueAtMs)
    }

    @Test
    fun pauseAndBypassUnavailableBeforeSetup() {
        val s = PolicyState()
        assertEquals(s, PolicyEngine.reduce(s, PolicyEvent.PauseRequested, config))
        assertEquals(s, PolicyEngine.reduce(s, PolicyEvent.BypassStarted(10, 20), config))
    }

    @Test
    fun advanceStatusOverlayOnlyAppliesToQuietPhases() {
        val outstanding = PolicyEngine.reduce(active(), PolicyEvent.AdvanceStatusChanged(true, false), config)
        assertEquals(Phase.ADVANCE_OUTSTANDING, outstanding.phase)
        val overdue = PolicyEngine.reduce(outstanding, PolicyEvent.AdvanceStatusChanged(true, true), config)
        assertEquals(Phase.ADVANCE_OVERDUE, overdue.phase)
        assertEquals(Phase.ACTIVE, PolicyEngine.reduce(overdue, PolicyEvent.AdvanceStatusChanged(false, false), config).phase)

        val restricted = at(active(), due + 15 * min)
        assertEquals(restricted, PolicyEngine.reduce(restricted, PolicyEvent.AdvanceStatusChanged(true, true), config))
    }

    @Test
    fun overdueAdvanceStillEscalatesNormallyAndNeverBlocksEscape() {
        val overdue = PolicyEngine.reduce(active(), PolicyEvent.AdvanceStatusChanged(true, true), config)
        val restricted = at(overdue, due + 15 * min)
        assertEquals(Phase.RESTRICTED_IN_APP, restricted.phase)
        assertEquals(Phase.PAUSED, PolicyEngine.reduce(restricted, PolicyEvent.PauseRequested, config).phase)
        assertEquals(
            Phase.EMERGENCY_BYPASS,
            PolicyEngine.reduce(restricted, PolicyEvent.BypassStarted(due + 20 * min, due + 60 * min), config).phase,
        )
    }

    @Test
    fun recoveryRoundTrip() {
        val recovery = PolicyEngine.reduce(at(active(), due + 15 * min), PolicyEvent.RecoveryRequired, config)
        assertEquals(Phase.RECOVERY_REQUIRED, recovery.phase)
        assertFalse(recovery.appRestricted)
        assertEquals(Phase.RECOVERY_REQUIRED, at(recovery, due + 9999 * min).phase)
        assertEquals(Phase.ACTIVE, PolicyEngine.reduce(recovery, PolicyEvent.RecoveryCompleted(due), config).phase)
        assertEquals(Phase.SETUP_REQUIRED, PolicyEngine.reduce(recovery, PolicyEvent.RecoveryCompleted(null), config).phase)
    }

    @Test
    fun longGapSettlesInOneAdvanceCall() {
        assertEquals(Phase.RESTRICTED_IN_APP, at(active(), due + 100_000 * min).phase)
    }

    @Test(expected = IllegalArgumentException::class)
    fun configRejectsNonPositiveWindow() {
        PolicyConfig(checkInWindowMs = 0, graceMs = 0)
    }
}
