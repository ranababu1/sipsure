# Current State

**Goal:** SipSure, an Android hydration accountability app (see `plan.md`). **Maturity:** MVP feature set implemented with test-mode credits; only partly verified on a device (see below). Not release-ready.

**Platform/stack:** Native Kotlin, single `:app` module, plain `Activity` with programmatic views (light theme), JUnit 4. JDK 21, compileSdk/targetSdk 36, minSdk 26, Gradle wrapper 8.14, AGP 8.13.1. Only runtime dependency: `androidx.core` (FileProvider).

## Architecture
- `domain/` (pure Kotlin, unit tested): `Schedule` (validation, quiet hours, next due), `PolicyEngine` (plan section 8 state machine), `CreditLedger` (idempotent ledger, advances), `PhotoCheck` (plausibility/replay rules), `HydrationController` (orchestrates the above, persists via `StateStore`).
- `data/`: `StateCodec` (JSON), `PrefsStateStore`.
- `platform/`: alarms, receivers, notifications, photo decoding.
- `ui/` + `MainActivity`: screens rebuilt from controller state. `SipSureApp` wires one shared controller for UI and receivers.

## Implemented
- Onboarding with safety/medical/permission/data explanation and consent; schedule create/edit with bounds; pause/resume.
- Escalation: window, grace, in-app restriction; free emergency override always available; credit bypass (1 hour).
- Test-mode credit packs; up to 3 outstanding advances with 24h deadlines and clear terms; overdue only blocks further advances.
- Photo check-in via system camera, on-device analysis, photo deleted, hash kept; retry/review/accepted outcomes.
- Persistence with corruption recovery; reboot/time-change handling; notifications.

## Verified
- 53 unit tests pass (`./gradlew.bat :app:testDebugUnitTest`).
- On emulator: onboarding, schedule start, pause/resume, credits screen, advance flow, persistence across force-stop.
- Not verified on device: restricted screen, camera/photo flow, alarm and notification delivery, boot/time-zone receivers.

## Build / test
- `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (needs `ANDROID_HOME` or git-ignored `local.properties`).
- APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions / services
`POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`. No camera permission (system camera app). No network, backend, analytics.

## Known limitations / risks
- Whole-device enforcement is not implemented or validated; only an in-app screen exists. Device-enforcement flag is always "unsupported".
- Alarms are inexact; reminders can be late on some devices. Opening the app re-evaluates time.
- Payments are test-mode only; no real billing, no server validation, no privacy policy or terms yet.
- Photo checks are heuristics (size, light, freshness, near-duplicate hash), not proof of drinking.
- UI strings are inline Kotlin (not yet in resources); light theme only; onboarding step is not saved across app restarts.
- No instrumented/UI tests.

## Next
1. Manually test restricted screen, camera flow, notifications and boot receiver (device with adjustable clock or a debug time hook).
2. Move UI strings to resources, add dark theme and accessibility pass.
3. Privacy policy, terms, release/signing documentation.
4. Evaluate device-owner enforcement feasibility before any whole-device feature.
