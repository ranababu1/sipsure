# Changelog

Newest first.

## 0.2.0 (code 2) — 2026-10-28 — Sleeker UI

- New visual system: near-black navy page with one soft light behind the hero that takes the state's colour (aqua on track, amber due, coral missed, mint covered); solid raised cards with hairline edges instead of translucent blobs; the old large background circles and bottom waves are gone.
- Bundled Manrope (variable font, SIL OFL, licence in `assets/licenses/manrope-OFL.txt`, about 165 KB) for all text.
- Today: date + greeting top bar with a tappable credit balance; the water glass is now an orb inside a progress ring that shows time left in the current period (glowing head, sweep gradient); stats strip (sips today, next check-in, interval); recent activity as a hairline list with tinted badges; headline cross-fades between states.
- Bottom navigation is a slim frosted bar with labels and an active indicator, so it no longer covers content. Tabs fade/slide in.
- Credits: wallet-style balance card, compact +3/+10 packs, segmented advance meter, hairline history. Settings: grouped header, restyled switch, monospace adb command.
- Refined glyphs (thinner, consistent strokes; coin, back chevron, handset), aqua and amber gradient buttons with dark ink, animated chips, pulsing status dot.
- Timestamps read "Today, 3:49 PM" / "Tomorrow" / "Yesterday" instead of numeric dates.
- Verified on emulator: locked screen, bypass state, Today scroll, Credits, Settings. 60 unit tests pass. Not re-checked: onboarding screens, due/grace state visuals, TalkBack and large fonts, and the release (R8) build on a device.

## 2026-10-28 — Two APKs per release (match teacher)

- Rehearsal `v0.1.0-rc1` published only `sipsure-r1.apk`; the `teacher` repo publishes two APKs, a download guide and device labels. I had missed that when copying its release setup.
- `release.yml` now publishes `sipsure-arm64-v8a-rNN.apk` and `sipsure-rNN.apk`, adds a "Which file should I download?" section to the notes and labels each asset. instructions.md section 9 makes this mandatory for every release.
- Caveat: SipSure has no native libraries, so the two APKs are currently the same file under two names.
- Not yet verified: the new workflow steps (first run will be the next tag).

## 2026-10-28 — Release pipeline and signing

- Added release management rules (instructions.md section 9, written by the user's request, modelled on the `teacher` repo): semantic versions in `gradle.properties` (`APP_VERSION_NAME`, `APP_VERSION_CODE`), tag `vX.Y.Z` must match, hyphenated tags are pre-releases, each meaningful increment bumps the version and is tagged after green CI, the signing key lives outside the repo and in four repository secrets.
- Added `.github/workflows/ci.yml` (tests + debug build on every push) and `.github/workflows/release.yml` (tag-triggered: version check, tests, signing from secrets, signed APK + AAB with R8, signature verification, GitHub Release with `sipsure-rNN.apk`, AAB and mapping archived 90 days).
- Release build now shrinks with R8, reads the version from `gradle.properties`, signs from `key.properties` (git-ignored) and is left unsigned when that file is absent.
- Generated the release keystore (PKCS12, RSA 4096, 10000 days, alias `sipsure`) in `~/.sipsure-signing/`, outside the repository. It has not been backed up by anyone yet.
- Verified locally: `assembleRelease` builds and `apksigner verify` passes (v2); the APK is about 0.96 MB, versionName 0.1.0 / code 1, and the debug-only receiver is absent.
- NOT done: the four GitHub secrets (setting them was blocked from this session; the owner must run the commands below), a run of the release APK on a device (the emulator was closed and the only AVD has a device-owner debug build), and any tag or release.

## 2026-10-28 — Credit-only unlock, no free override, uninstall block, more water

- Product change directed by the user (recorded in `plan.md` Amendment 2026-10-28): the free emergency override is removed everywhere (controller, UI, copy, notifications); once the phone is locked only a credit unlocks it (a photo check-in, review confirmation, pause and settings changes are all refused while locked); the camera is no longer allowlisted in lock-task mode, only the dialer.
- While any advance is unpaid (including overdue), `DeviceLock.setUninstallBlocked(true)` blocks uninstalling (device-owner only); it is released when the advance is repaid. Removing the phone-lock setup is also refused while an advance is unpaid or the phone is locked.
- Kept: emergency and incoming calls always work, overdue never deletes data, phone lock is opt-in with consent. Old saved history entries of the removed override still load (`BYPASS_EMERGENCY` kept in the enum, labelled "no longer offered").
- UI: locked screen shows only Emergency call and "Unlock with 1 credit" / "Borrow 1 credit to unlock"; the Override tile is gone; consent dialogs and onboarding state the new rules.
- More water (`ui/Water.kt`): the full-screen background has rising bubbles, rolling wave layers and shimmering light; the glass has three wave layers with sloshing and tilt, falling drips that ripple the surface, dappled light, more bubbles, depth shading, and tap-to-splash with haptics.
- Verified: build and unit tests pass (new tests for credit-only unlock, locked refusals, uninstall block/release, removal refusal). On emulator: locked screen, borrowing a credit unlocks, `packageUninstallBlocked` goes true while the loan is unpaid and false after repaying (read from `dumpsys device_policy`), water animation visible.
- Caveat: `pm uninstall` fails with DELETE_FAILED_DEVICE_POLICY_MANAGER for any device-owner app, loan or not, so that command can't distinguish the two; the policy value is the real check. After repaying, the owner must still use "Remove phone-lock setup" before the app can be uninstalled.
- Risk (flagged): with no free override, a phone locked while the user has no credits and cannot take an advance (3 outstanding, or an overdue one) has no way out except emergency calls or adb. Tracked in `currentstate.md`.

## 2026-10-27 — Locked-state fixes

- Found by running the lock on the emulator: the floating nav stayed visible while locked because state changes made by the clock/alarm receivers did not refresh the root screen. `AppHost.syncFromController()` now refreshes the UI whenever controller state changes behind its back.
- Missed/locked screen is now focused: no tile row, history or "today" card, just check-in, emergency call (when locked), credit or advance, free override, and pause (when not locked).
- Out-of-credits while locked used to be a dead end ("See emergency advance" could not leave Today). The Credits screen is now reachable while locked (with a Back button); Settings stays unreachable while locked.
- Verified on emulator: locked state shows no nav bar, Home key ignored, Credits opens and Back returns, lock stays on. 57 unit tests pass.
- Note: a clock jump larger than the check-in window is treated as an undelivered reminder and forgiven (restarts the interval) by design; test with exact jumps.

## 2026-10-27 — UI redesign v2 (ocean look, animated water glass)

- Reason: the Compose v1 UI was Material defaults in a blue palette (stacked cards, paragraphs of copy, stock icons) and was rejected as poor. A brief web search for current trends returned mostly low-quality pages; the design leans on the consistent points it did surface (a fill-up water visual, minimal glanceable screens, Material 3 Expressive spring motion and bold type, streak/progress visuals), not on any specific app.
- One immersive dark ocean gradient across all screens (light mode dropped; system bars always light-icon).
- Centrepiece `WaterGlass`: layered animated waves, rising bubbles and glass highlight; water level shows time to the next check-in (full after a check-in, draining), with state colours (aqua on track, amber due, coral missed, green bypass, grey paused) and a lock or pause glyph for missed and paused states.
- Large light numerals, one headline and one line of copy per state; the single next action is a pill button (camera glyph). Glass quick-action tiles (Pause, Credits balance, Override), a "Today" droplet row (counts today's check-ins), compact Recent list.
- Floating pill navigation with spring press feedback; hand-drawn glyphs (camera, lock, pause, play, coin, SOS, gear, check, phone, drop) replace stock icons.
- Credits: gradient balance card with a giant number, pack tiles, an advance card with remaining-advance dots and a 24h progress bar; the test-mode explanation moved behind a pill and dialog.
- Settings and onboarding use preset chips (interval, window, grace, quiet hours) instead of sliders or number boxes; invalid window values are disabled. Onboarding is four short steps: promise, how it works, privacy (plain checklist and consent), schedule.
- Added adaptive launcher icon (white drop on ocean blue).
- Debug builds only: `DebugTimeReceiver` can now also wipe saved state (`--ez reset true`) for testing onboarding.
- Trade-offs: fine-grained intervals (15-minute steps) are replaced by presets; copy that used to be on-screen (consequence text, long privacy paragraph) is shorter, so the full explanations live in onboarding, dialogs and Settings.
- Verified: build and 57 unit tests pass. On emulator: all four onboarding steps, Today in on-track(bypass), missed and bypass states, Credits, Settings.
- NOT re-verified after this redesign: camera/photo flow, phone-lock engagement, the due/grace hero, TalkBack, large font scale, small screens, landscape, reduced-motion preferences.

## 2026-10-27 — UI redesign (Jetpack Compose + Material 3)

- Replaced the programmatic-views UI with Compose. Reason: the first UI was functional but unpolished; a designed UI is a clear product requirement. Added dependencies: Compose BOM 2025.06.01 (ui, foundation, material3, icons-core) and activity-compose 1.10.1. Domain, data and platform layers are unchanged.
- New design: brand theme with light and dark colour schemes, bottom navigation (Today, Credits, Settings), state-coloured hero cards with an animated countdown ring (calm, check-in due, grace, missed, bypass, paused), icon rows, timeline, balance hero and pack cards on Credits, advance cards with a 24h progress bar, slider-based schedule editor with quiet-hours switch, three-step onboarding with consent switch, Material dialogs and snackbars.
- Structure: `ui/theme/Theme.kt`, `ui/Components.kt`, `TodayScreen`, `CreditsScreen`, `SettingsScreen`, `OnboardingScreen`, `ScheduleEditor`, `SipSureRoot`, `AppHost` (bridge between composables and Android). `MainActivity` is now a `ComponentActivity` that owns camera (`TakePicture`), lock-task mode, dialer and permissions.
- Behaviour kept: phone lock takes over the screen when due, free emergency override always reachable, photo outcomes shown as dialogs, all texts avoid medical or shaming claims.
- Verified: build and unit tests pass. On emulator: onboarding (all 3 steps, notification prompt), Today in calm, check-in-due, missed and bypass states, Credits, notification posted when due.
- NOT re-verified after the redesign: camera/photo flow, phone lock engagement, Settings screen visuals, TalkBack, large font scale, small screens, landscape.

## 2026-10-27 — Optional whole-phone lock (device owner, lock-task mode)

- Requested by the user as an explicit exception to the "app-level only" default in plan.md section 9. Implemented only through the supported Android lock-task API, only when SipSure is the device owner, only after an explicit in-app opt-in, and with the plan.md/instructions.md safety rules kept.
- `platform/DeviceLock` + `SipSureAdminReceiver`: availability = `isDeviceOwnerApp`. While locked, allowed packages are SipSure, the dialer (emergency and incoming calls), the camera app (check-in photo) and the permission dialog; home, recents and notifications are disabled.
- Lock engages only when `HydrationController.deviceLockActive` (opted in + supported + RESTRICTED_IN_APP) and releases on every exit: photo check-in, credit, free emergency override. The setting cannot be changed while locked. Losing device ownership falls back to DEVICE_ENFORCEMENT_UNAVAILABLE (in-app screen).
- Settings: explanation, consent dialog, on/off, "Remove phone-lock setup" (clears device owner so the app can be uninstalled). Onboarding and missed-check-in text now describe the optional lock honestly.
- Debug builds only (`src/debug`): `DebugTimeReceiver` moves the clock on a device-owner test device. It is not in release builds.
- Decision: no anti-uninstall measures beyond what device-owner status inherently implies; removal is one tap in Settings (when not locked) or `adb shell dpm remove-active-admin`. Deleted-data and calling restrictions: none.
- Verified: 57 unit tests pass, build succeeds. On the emulator (set up with `adb shell dpm set-device-owner app.sipsure/.platform.SipSureAdminReceiver`): escalation to lock via clock jumps, `mLockTaskModeState=LOCKED`, Home key blocked, camera opens, photo check-in unlocks, credit bypass unlocks, free emergency override unlocks, dialer opens while locked.
- NOT verified: behaviour after reboot while locked, real-phone behaviour (OEM variations), incoming call UI while locked, background launch of the lock from the alarm receiver without the app open.

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
