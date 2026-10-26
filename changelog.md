# Changelog

Newest first.

## 2026-10-27 — Policy engine

- Added `domain/PolicyEngine`: pure state machine over the 12 plan.md section 8 phases (`reduce` for events, `advance` to settle elapsed time).
  - Escalation: ACTIVE -> CHECK_IN_DUE -> GRACE_PERIOD -> RESTRICTED_IN_APP, or DEVICE_ENFORCEMENT_UNAVAILABLE when device enforcement was requested but is unsupported (still app-level only).
  - Pause, bypass and check-in are reachable from every escalation phase; invalid or duplicate events return the state unchanged.
  - Advance overdue/outstanding is an overlay on quiet phases, fed from the ledger by the caller; it never blocks escape paths.
  - Paused, recovery and clock-goes-backwards cases do not escalate.
- Decision: a long gap (e.g. device off) settles straight to the restricted state; quiet-hours/grace handling for that belongs to the scheduler task.
- Verified: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — BUILD SUCCESSFUL; 24/24 tests passed (10 ledger, 14 policy).

## 2026-10-27 — Project scaffold and credit ledger

- Added native Kotlin Android project (Gradle wrapper 8.14, AGP 8.13.1, Kotlin 2.2.21, compileSdk/targetSdk 36, minSdk 26).
- Added placeholder `MainActivity` (no UI logic yet) and `.gitignore`.
- Added `domain/CreditLedger`: immutable append-only ledger with idempotent purchase, redemption, emergency advance, and repayment.
  - Advances: max 3 outstanding, 24h deadline each, only when balance is empty, blocked while any advance is overdue.
  - Overdue only blocks new advances; no data deletion or other penalty (plan.md 5.4).
  - `restore()` rejects persisted entries with duplicate ids or a negative running balance.
- Decision: native Kotlin chosen so device-management APIs stay available; whole-device enforcement remains deferred and unvalidated.
- Decision: no AI co-author trailer on commits, per instructions.md section 8.
- Verified: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — BUILD SUCCESSFUL; 10/10 `CreditLedgerTest` tests passed.
- Follow-up: policy engine, scheduler, persistence, UI (see `currentstate.md`).
