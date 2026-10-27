package app.sipsure.domain

/** Key states from plan.md section 8. */
enum class Phase {
    SETUP_REQUIRED,
    ACTIVE,
    CHECK_IN_DUE,
    GRACE_PERIOD,
    CHECK_IN_ACCEPTED,
    RESTRICTED_IN_APP,
    DEVICE_ENFORCEMENT_UNAVAILABLE,
    EMERGENCY_BYPASS,
    ADVANCE_OUTSTANDING,
    ADVANCE_OVERDUE,
    PAUSED,
    RECOVERY_REQUIRED,
}

data class PolicyConfig(
    val checkInWindowMs: Long,
    val graceMs: Long,
    /** User opted in to whole-device enforcement. */
    val deviceEnforcementRequested: Boolean = false,
    /** Whether the device/deployment actually supports it (device owner etc.). Unproven, so callers pass false today. */
    val deviceEnforcementSupported: Boolean = false,
) {
    init {
        require(checkInWindowMs > 0) { "check-in window must be positive" }
        require(graceMs >= 0) { "grace period cannot be negative" }
    }
}

data class PolicyState(
    val phase: Phase = Phase.SETUP_REQUIRED,
    val nextDueAtMs: Long? = null,
    val windowEndsAtMs: Long? = null,
    val graceEndsAtMs: Long? = null,
    val bypassUntilMs: Long? = null,
) {
    /** True when the in-app accountability screen is shown. Never implies blocking essential phone functions. */
    val appRestricted: Boolean
        get() = phase == Phase.RESTRICTED_IN_APP || phase == Phase.DEVICE_ENFORCEMENT_UNAVAILABLE
}

sealed interface PolicyEvent {
    data class SetupCompleted(val nextDueAtMs: Long) : PolicyEvent
    data class Tick(val nowMs: Long) : PolicyEvent
    /** Photo check-in accepted (or approved on review); starts the next interval. */
    data class CheckInAccepted(val nextDueAtMs: Long) : PolicyEvent
    /** A credit was redeemed (charging happens in the ledger, not here). */
    data class BypassStarted(val untilMs: Long, val nextDueAtMs: Long) : PolicyEvent
    data object PauseRequested : PolicyEvent
    data class Resumed(val nextDueAtMs: Long) : PolicyEvent
    /** Derived from the ledger by the caller; only affects the quiet phases. */
    data class AdvanceStatusChanged(val outstanding: Boolean, val overdue: Boolean) : PolicyEvent
    data object RecoveryRequired : PolicyEvent
    /** Null [nextDueAtMs] means no usable schedule survived, so setup restarts. */
    data class RecoveryCompleted(val nextDueAtMs: Long?) : PolicyEvent
}

/**
 * Pure state machine for check-in escalation. Invalid transitions return the state unchanged, so a
 * stale or duplicate event can never wedge the user. Pause, bypass and check-in are reachable from every
 * escalation phase, so there is always a way out of a restriction.
 */
object PolicyEngine {
    private val quiet = setOf(Phase.ACTIVE, Phase.ADVANCE_OUTSTANDING, Phase.ADVANCE_OVERDUE)
    private val escalated = setOf(
        Phase.CHECK_IN_DUE,
        Phase.GRACE_PERIOD,
        Phase.RESTRICTED_IN_APP,
        Phase.DEVICE_ENFORCEMENT_UNAVAILABLE,
    )
    private val configured = Phase.entries.toSet() - Phase.SETUP_REQUIRED - Phase.RECOVERY_REQUIRED

    fun reduce(state: PolicyState, event: PolicyEvent, config: PolicyConfig): PolicyState = when (event) {
        is PolicyEvent.SetupCompleted ->
            if (state.phase == Phase.SETUP_REQUIRED) active(event.nextDueAtMs) else state

        is PolicyEvent.Tick -> tick(state, event.nowMs, config)

        is PolicyEvent.CheckInAccepted ->
            if (state.phase in escalated) PolicyState(Phase.CHECK_IN_ACCEPTED, nextDueAtMs = event.nextDueAtMs) else state

        is PolicyEvent.BypassStarted ->
            if (state.phase in configured && state.phase != Phase.PAUSED) {
                PolicyState(Phase.EMERGENCY_BYPASS, nextDueAtMs = event.nextDueAtMs, bypassUntilMs = event.untilMs)
            } else {
                state
            }

        PolicyEvent.PauseRequested ->
            if (state.phase in configured && state.phase != Phase.PAUSED) PolicyState(Phase.PAUSED) else state

        is PolicyEvent.Resumed ->
            if (state.phase == Phase.PAUSED) active(event.nextDueAtMs) else state

        is PolicyEvent.AdvanceStatusChanged ->
            if (state.phase in quiet) {
                val phase = when {
                    event.overdue -> Phase.ADVANCE_OVERDUE
                    event.outstanding -> Phase.ADVANCE_OUTSTANDING
                    else -> Phase.ACTIVE
                }
                state.copy(phase = phase)
            } else {
                state
            }

        PolicyEvent.RecoveryRequired -> PolicyState(Phase.RECOVERY_REQUIRED)

        is PolicyEvent.RecoveryCompleted ->
            if (state.phase != Phase.RECOVERY_REQUIRED) state
            else event.nextDueAtMs?.let { active(it) } ?: PolicyState()
    }

    /** Applies time passing until the state is stable, e.g. after the device was off for hours. */
    fun advance(state: PolicyState, nowMs: Long, config: PolicyConfig): PolicyState {
        var current = state
        repeat(Phase.entries.size) {
            val next = reduce(current, PolicyEvent.Tick(nowMs), config)
            if (next == current) return current
            current = next
        }
        return current
    }

    private fun active(nextDueAtMs: Long) = PolicyState(Phase.ACTIVE, nextDueAtMs = nextDueAtMs)

    private fun tick(state: PolicyState, nowMs: Long, config: PolicyConfig): PolicyState {
        val due = state.nextDueAtMs
        return when (state.phase) {
            in quiet ->
                if (due != null && nowMs >= due) {
                    state.copy(phase = Phase.CHECK_IN_DUE, windowEndsAtMs = due + config.checkInWindowMs)
                } else {
                    state
                }

            Phase.CHECK_IN_DUE ->
                state.windowEndsAtMs?.takeIf { nowMs >= it }?.let {
                    state.copy(phase = Phase.GRACE_PERIOD, graceEndsAtMs = it + config.graceMs)
                } ?: state

            Phase.GRACE_PERIOD ->
                if (state.graceEndsAtMs != null && nowMs >= state.graceEndsAtMs) {
                    val deviceUnavailable = config.deviceEnforcementRequested && !config.deviceEnforcementSupported
                    state.copy(phase = if (deviceUnavailable) Phase.DEVICE_ENFORCEMENT_UNAVAILABLE else Phase.RESTRICTED_IN_APP)
                } else {
                    state
                }

            Phase.CHECK_IN_ACCEPTED -> state.copy(phase = Phase.ACTIVE)

            Phase.EMERGENCY_BYPASS ->
                if (state.bypassUntilMs != null && nowMs >= state.bypassUntilMs) {
                    state.copy(phase = Phase.ACTIVE, bypassUntilMs = null)
                } else {
                    state
                }

            else -> state
        }
    }
}
