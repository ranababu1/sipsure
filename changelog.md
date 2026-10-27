# Changelog

Newest first.

## 2026-10-27 — Optional whole-phone lock (device owner, lock-task mode)

- Requested by the user as an explicit exception to the "app-level only" default in plan.md section 9. Implemented only through the supported Android lock-task API, only when SipSure is the device owner, only after an explicit in-app opt-in, and with the plan.md/instructions.md safety rules kept.
- `platform/DeviceLock` + `SipSureAdminReceiver`: availability = `isDeviceOwnerApp`. While locked, allowed packages are SipSure, the dialer (emergency and incoming calls), the camera app (check-in photo) and the permission dialog; home, recents and notifications are disabled.
- Lock engages only when `HydrationController.deviceLockActive` (opted in + supported + RESTRICTED_IN_APP) and releases on every exit: photo check-in, credit, free emergency override. The setting cannot be changed while locked. Losing device ownership falls back to DEVICE_ENFORCEMENT_UNAVAILABLE (in-app screen).
- Settings: explanation, consent dialog, on/off, "Remove phone-lock setup" (clears device owner so the app can be uninstalled). Onboarding and missed-check-in text now describe the optional lock honestly.
- Debug builds only (`src/debug`): `DebugTimeReceiver` moves the clock on a device-owner test device. It is not in release builds.
- Decision: no anti-uninstall measures beyond what device-owner status inherently implies; removal is one tap in Settings (when not locked) or `adb shell dpm remove-active-admin`. Deleted-data and calling restrictions: none.
- Verified: 57 unit tests pass, build succeeds. On the emulator (set up with `adb shell dpm set-device-owner app.sipsure/.platform.SipSureAdminReceiver`): escalation to lock via clock jumps, `mLockTaskModeState=LOCKED`, Home key blocked, camera opens, photo check-in unlocks, credit bypass unlocks, dialer opens while locked.
- NOT verified: free emergency override while locked on-device (unit-tested only), behaviour after reboot while locked, real-phone behaviour (OEM variations), incoming call UI while locked, background launch of the lock from the alarm receiver without the app open.

## 2026-10-27 — Android app: persistence, reminders, UI, photo check-in

- Added `data/StateCodec` + `PrefsStateStore`: versioned JSON in app-private preferences, committed synchronously; unreadable data becomes `RECOVERY_REQUIRED` (reset path) instead of a crash.
- Added `platform/`: `ReminderScheduler` (single inexact `setAndAllowWhileIdle` alarm for the next policy deadline, no exact-alarm permission), `AlarmReceiver`, `SystemEventsReceiver` (boot, time-zone, time change; restarts the interval instead of punishing undelivered reminders), `Notifier`, `PhotoAnalyzer`.
- Added single-activity UI (`MainActivity`, `ui/Ui.kt`): onboarding with safety, medical, data and enforcement explanations and a consent checkbox; home with consequences and always-visible free emergency override; in-app restriction screen with check-in, credit bypass, advance and pause; credits tab with test-mode packs, advance terms dialog, repayment and ledger; settings.
- Check-in uses the system camera app through a FileProvider (no CAMERA permission). The photo is analysed on-device and deleted; only a 64-bit hash is kept. Outcomes: accepted, retry, review (user may confirm).
- Dependency added: `androidx.core:core:1.16.0` (FileProvider only). Test-only: `org.json:json` for codec tests.
- Decision: light theme only with fixed dark ink; the first dark-theme attempt produced low-contrast grey text on the emulator.
- Verified: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug` BUILD SUCCESSFUL, 53 unit tests pass.
- Verified on emulator (Android 17 image): onboarding, schedule validation and start, pause/resume, credits screen, advance dialog and acceptance, state survives force-stop, advance banner.
- NOT verified on device: escalation to the restricted screen, camera and photo outcomes, alarms/notifications firing, reboot/time-zone receivers (the emulator clock cannot be changed without root).

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
