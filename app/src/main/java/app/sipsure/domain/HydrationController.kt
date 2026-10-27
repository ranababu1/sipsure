package app.sipsure.domain

import java.time.ZoneId
import java.util.UUID

enum class HistoryKind { CHECK_IN, CHECK_IN_CONFIRMED_BY_USER, BYPASS_CREDIT, BYPASS_EMERGENCY, PAUSED, RESUMED, CREDITS_BOUGHT, ADVANCE_TAKEN, ADVANCE_REPAID }

data class HistoryEvent(val atMs: Long, val kind: HistoryKind)

/** Everything that must survive process death. Photos are never stored, only hashes. */
data class AppState(
    val schedule: Schedule = Schedule(),
    val policy: PolicyState = PolicyState(),
    val ledger: CreditLedger = CreditLedger.EMPTY,
    val photoHashes: List<Long> = emptyList(),
    val history: List<HistoryEvent> = emptyList(),
)

interface StateStore {
    /** Returns null when nothing is saved yet; throws [StateCorruptedException] when saved data is unreadable. */
    fun load(): AppState?
    fun save(state: AppState)
}

class StateCorruptedException(cause: Throwable? = null) : Exception("Saved state could not be read", cause)

/** Test-mode credit pack; real billing is deferred (plan.md section 9). */
data class CreditPack(val credits: Int, val label: String)

/**
 * Orchestrates schedule, policy and ledger and persists after every change. Each public action
 * returns null on success or a user-facing error message; state never changes on error.
 */
class HydrationController(
    private val store: StateStore,
    private val clock: () -> Long,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    var state: AppState = AppState()
        private set

    private val schedule get() = state.schedule
    private val config get() = schedule.toPolicyConfig()

    /** Loads saved state. Unreadable data puts the app in RECOVERY_REQUIRED instead of crashing. */
    fun start() {
        state = try {
            store.load() ?: AppState()
        } catch (e: StateCorruptedException) {
            AppState(policy = PolicyState(Phase.RECOVERY_REQUIRED))
        }
        if (state.policy.phase != Phase.RECOVERY_REQUIRED) tick()
    }

    fun completeSetup(newSchedule: Schedule): String? {
        newSchedule.validationError()?.let { return it }
        if (state.policy.phase != Phase.SETUP_REQUIRED) return "Setup is already complete."
        val due = newSchedule.nextDueAfter(clock(), zone())
        update(state.copy(schedule = newSchedule, policy = reduce(PolicyEvent.SetupCompleted(due), newSchedule)))
        return null
    }

    fun updateSchedule(newSchedule: Schedule): String? {
        newSchedule.validationError()?.let { return it }
        var policy = state.policy
        if (policy.phase in QUIET_PHASES) {
            policy = policy.copy(nextDueAtMs = newSchedule.nextDueAfter(clock(), zone()))
        }
        update(state.copy(schedule = newSchedule, policy = policy))
        return null
    }

    /** Applies elapsed time and keeps the advance overlay in sync with the ledger. */
    fun tick() {
        val now = clock()
        var policy = PolicyEngine.advance(state.policy, now, config)
        policy = PolicyEngine.reduce(policy, advanceEvent(now), config)
        if (policy != state.policy) update(state.copy(policy = policy))
    }

    /**
     * After reboot, time change or the app being killed, a due time may have passed without any chance
     * to remind the user. Missing a reminder the phone never delivered is not a missed check-in, so the
     * schedule restarts instead of escalating.
     */
    fun onSystemRecovered() {
        val now = clock()
        val p = state.policy
        val due = p.nextDueAtMs
        if (p.phase in QUIET_PHASES && due != null && now >= due + schedule.checkInWindowMinutes * Schedule.MS_PER_MINUTE) {
            update(state.copy(policy = p.copy(nextDueAtMs = schedule.nextDueAfter(now, zone()))))
        }
        tick()
    }

    fun recoverFromCorruption(): String? {
        if (state.policy.phase != Phase.RECOVERY_REQUIRED) return "Nothing to recover."
        val policy = reduce(PolicyEvent.RecoveryCompleted(null))
        update(AppState(policy = policy))
        return null
    }

    /** [verdict] comes from [PhotoCheck]; [hash] is remembered to catch resubmitted photos. */
    fun submitCheckIn(verdict: CheckInVerdict, hash: Long): String? {
        if (state.policy.phase !in ESCALATED_PHASES) return "No check-in is due right now."
        return when (verdict.outcome) {
            CheckInOutcome.RETRY_NEEDED -> verdict.message
            CheckInOutcome.REVIEW_REQUIRED -> verdict.message
            CheckInOutcome.ACCEPTED -> recordCheckIn(hash, HistoryKind.CHECK_IN)
        }
    }

    /** The user vouches for a photo flagged for review; recorded as such, not as verified. */
    fun confirmReviewedCheckIn(hash: Long): String? {
        if (state.policy.phase !in ESCALATED_PHASES) return "No check-in is due right now."
        return recordCheckIn(hash, HistoryKind.CHECK_IN_CONFIRMED_BY_USER)
    }

    private fun recordCheckIn(hash: Long, kind: HistoryKind): String? {
        val now = clock()
        val next = schedule.nextDueAfter(now, zone())
        val policy = reduce(PolicyEvent.CheckInAccepted(next))
        update(state.copy(policy = policy, photoHashes = (state.photoHashes + hash).takeLast(MAX_HASHES)).withHistory(kind, now))
        tick()
        return null
    }

    fun redeemBypassWithCredit(): String? {
        if (state.policy.phase !in ESCALATED_PHASES) return "There is no restriction to bypass."
        val now = clock()
        return when (val r = state.ledger.redeem(newId(), now)) {
            is LedgerResult.Rejected -> ledgerMessage(r.reason)
            is LedgerResult.Applied -> {
                startBypass(r.ledger, HistoryKind.BYPASS_CREDIT, now)
                null
            }
        }
    }

    /** Always free and always available once set up; safety paths are never gated on credits. */
    fun emergencyOverride(): String? {
        if (state.policy.phase == Phase.SETUP_REQUIRED || state.policy.phase == Phase.RECOVERY_REQUIRED) {
            return "Emergency override isn't needed yet."
        }
        startBypass(state.ledger, HistoryKind.BYPASS_EMERGENCY, clock())
        return null
    }

    private fun startBypass(ledger: CreditLedger, kind: HistoryKind, now: Long) {
        val until = now + BYPASS_DURATION_MS
        val policy = reduce(PolicyEvent.BypassStarted(until, schedule.nextDueAfter(until, zone())))
        if (policy.phase == Phase.EMERGENCY_BYPASS) update(state.copy(policy = policy, ledger = ledger).withHistory(kind, now))
        else update(state.copy(ledger = ledger))
        tick()
    }

    fun pause(): String? {
        val policy = reduce(PolicyEvent.PauseRequested)
        if (policy == state.policy) return "The schedule can't be paused right now."
        update(state.copy(policy = policy).withHistory(HistoryKind.PAUSED, clock()))
        return null
    }

    fun resume(): String? {
        val now = clock()
        val policy = reduce(PolicyEvent.Resumed(schedule.nextDueAfter(now, zone())))
        if (policy == state.policy) return "The schedule isn't paused."
        update(state.copy(policy = policy).withHistory(HistoryKind.RESUMED, now))
        return null
    }

    fun buyCredits(pack: CreditPack): String? {
        val now = clock()
        return when (val r = state.ledger.purchase(newId(), now, pack.credits)) {
            is LedgerResult.Rejected -> ledgerMessage(r.reason)
            is LedgerResult.Applied -> {
                update(state.copy(ledger = r.ledger).withHistory(HistoryKind.CREDITS_BOUGHT, now))
                tick()
                null
            }
        }
    }

    fun takeAdvance(): String? {
        val now = clock()
        return when (val r = state.ledger.takeAdvance(newId(), now)) {
            is LedgerResult.Rejected -> ledgerMessage(r.reason)
            is LedgerResult.Applied -> {
                update(state.copy(ledger = r.ledger).withHistory(HistoryKind.ADVANCE_TAKEN, now))
                tick()
                null
            }
        }
    }

    fun repayAdvance(advanceTxId: String): String? {
        val now = clock()
        return when (val r = state.ledger.repayAdvance(newId(), now, advanceTxId)) {
            is LedgerResult.Rejected -> ledgerMessage(r.reason)
            is LedgerResult.Applied -> {
                update(state.copy(ledger = r.ledger).withHistory(HistoryKind.ADVANCE_REPAID, now))
                tick()
                null
            }
        }
    }

    private fun advanceEvent(now: Long) = PolicyEvent.AdvanceStatusChanged(
        outstanding = state.ledger.outstandingAdvances(now).isNotEmpty(),
        overdue = state.ledger.hasOverdueAdvance(now),
    )

    private fun reduce(event: PolicyEvent, forSchedule: Schedule = schedule) =
        PolicyEngine.reduce(state.policy, event, forSchedule.toPolicyConfig())

    private fun update(new: AppState) {
        store.save(new)
        state = new
    }

    private fun AppState.withHistory(kind: HistoryKind, at: Long) =
        copy(history = (history + HistoryEvent(at, kind)).takeLast(MAX_HISTORY))

    private fun ledgerMessage(reason: LedgerResult.Reason) = when (reason) {
        LedgerResult.Reason.INVALID_AMOUNT -> "That amount isn't valid."
        LedgerResult.Reason.INSUFFICIENT_BALANCE -> "You don't have enough credits."
        LedgerResult.Reason.BALANCE_NOT_EMPTY -> "Advances are only available when your balance is empty."
        LedgerResult.Reason.ADVANCE_LIMIT_REACHED -> "You already have ${AdvanceTerms.MAX_OUTSTANDING} outstanding advances."
        LedgerResult.Reason.ADVANCE_OVERDUE -> "An advance is overdue. Repay it before taking another."
        LedgerResult.Reason.UNKNOWN_ADVANCE -> "That advance wasn't found."
        LedgerResult.Reason.ADVANCE_ALREADY_REPAID -> "That advance is already repaid."
    }

    companion object {
        const val BYPASS_DURATION_MS = 60L * 60 * 1000
        const val MAX_HASHES = 20
        const val MAX_HISTORY = 100
        private val QUIET_PHASES = setOf(Phase.ACTIVE, Phase.ADVANCE_OUTSTANDING, Phase.ADVANCE_OVERDUE)
        private val ESCALATED_PHASES = setOf(
            Phase.CHECK_IN_DUE, Phase.GRACE_PERIOD, Phase.RESTRICTED_IN_APP, Phase.DEVICE_ENFORCEMENT_UNAVAILABLE,
        )
        val CREDIT_PACKS = listOf(CreditPack(3, "3 credits"), CreditPack(10, "10 credits"))
    }
}
