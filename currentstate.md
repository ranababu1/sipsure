# Current State

**Goal:** SipSure, an Android hydration accountability app (see `plan.md`). **Maturity:** MVP feature set implemented with test-mode credits; only partly verified on a device (see below). Not release-ready.

**Platform/stack:** Native Kotlin, single `:app` module, Jetpack Compose UI with a custom dark ocean theme (animated water glass, glass cards, floating nav; Material 3 only for dialogs, switches and snackbars), JUnit 4. JDK 21, compileSdk/targetSdk 36, minSdk 26, Gradle wrapper 8.14, AGP 8.13.1. Runtime dependencies: Compose (BOM 2025.06.01), activity-compose, `androidx.core` (FileProvider).

## Architecture
- `domain/` (pure Kotlin, unit tested): `Schedule` (validation, quiet hours, next due), `PolicyEngine` (plan section 8 state machine), `CreditLedger` (idempotent ledger, advances), `PhotoCheck` (plausibility/replay rules), `HydrationController` (orchestrates the above, persists via `StateStore`).
- `data/`: `StateCodec` (JSON), `PrefsStateStore`.
- `platform/`: alarms, receivers, notifications, photo decoding.
- `ui/` (Compose screens, theme, components, `AppHost` bridge) + `MainActivity` (camera, lock-task, dialer, permissions). `SipSureApp` wires one shared controller for UI and receivers.

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

## Phone lock (optional, advanced)
- Off by default. Works only when SipSure is device owner: `adb shell dpm set-device-owner app.sipsure/.platform.SipSureAdminReceiver` (needs a phone with no accounts signed in). Remove via Settings > "Remove phone-lock setup".
- When enabled and a check-in is missed past grace, lock-task mode locks the phone to SipSure (plus dialer, camera, permission dialog). Released by photo check-in, credit, or the free emergency override.
- Verified on the emulator for lock, camera, photo and credit unlock, dialer access; see changelog for what is not verified.
- Debug builds include `DebugTimeReceiver` (clock jumps for testing): `adb shell am broadcast -n app.sipsure/app.sipsure.debug.DebugTimeReceiver --ei minutes 30`.

## Known limitations / risks
- Phone lock has only been tested on one emulator; real phones, OEM skins and reboot-while-locked are untested. Without device owner the app falls back to the in-app screen.
- Alarms are inexact; reminders can be late on some devices. Opening the app re-evaluates time.
- Payments are test-mode only; no real billing, no server validation, no privacy policy or terms yet.
- Photo checks are heuristics (size, light, freshness, near-duplicate hash), not proof of drinking.
- UI strings are inline Kotlin (not yet in resources). Photo flow, phone lock, TalkBack, large fonts and small screens were not re-tested after the Compose redesign.
- Dark theme only. Schedule options are presets (no 15-minute granularity). Icons are hand-drawn Canvas glyphs; launcher icon is a simple adaptive drop.
- No instrumented/UI tests.

## Next
1. Manually test restricted screen, camera flow, notifications and boot receiver (device with adjustable clock or a debug time hook).
2. Move UI strings to resources, add dark theme and accessibility pass.
3. Privacy policy, terms, release/signing documentation.
4. Evaluate device-owner enforcement feasibility before any whole-device feature.
