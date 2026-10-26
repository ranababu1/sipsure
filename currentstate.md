# Current State

**Goal:** SipSure, an Android hydration accountability app (see `plan.md`). **Maturity:** early scaffold; no user-facing features.

**Platform/stack:** Native Kotlin, single `:app` module, plain `Activity` (no UI framework yet), JUnit 4. JDK 21, Android SDK 36, minSdk 26. Gradle wrapper 8.14.

## Implemented and verified
- Project builds (`assembleDebug`) and unit tests pass.
- `app.sipsure.domain.CreditLedger`: idempotent purchase/redeem/advance/repay, 3-advance limit, 24h deadlines, overdue handling that never deletes data. Covered by 10 unit tests.

## Placeholder only
- `MainActivity` shows the tagline; nothing else.

## Not yet implemented
Scheduler, policy engine / state machine, check-in capture and verification, persistence, onboarding and safety explanations, emergency bypass UI, notifications, reboot/time-change handling, payments (test-mode only for MVP).

## Build / test
- `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (needs `ANDROID_HOME` or a `local.properties` with `sdk.dir`; `local.properties` is git-ignored).

## Permissions / services
None declared yet. No backend, analytics, or network use.

## Risks / limits
- Whole-device enforcement is unvalidated and deferred; the fallback is app-level restriction.
- Ledger is in-memory only until persistence is added.

## Next
1. Policy engine with the plan.md section 8 states and tested transitions.
2. Scheduler (AlarmManager-based, reboot/time-zone aware).
3. Local persistence for schedule and ledger.
4. Onboarding with safety/permission explanations.
