# Changelog

Newest first.

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
