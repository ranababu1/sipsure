# SipSure: Hydration Accountability App

## Product Plan (`plan.md`)

**Working product name:** SipSure\
**Tagline:** Build the habit. Keep control.\
**Document status:** Foundational product plan\
**Rule:** This file defines the product's intent and boundaries. Do not
edit it during implementation unless the user explicitly authorizes a
product-plan change. Record implementation progress and current
decisions in `changelog.md` and `currentstate.md`, not here.

------------------------------------------------------------------------

## 1. Product Vision

SipSure helps users build a consistent hydration habit through scheduled
check-ins and meaningful accountability. At user-configured intervals,
the app asks the user to confirm that they drank water, using a
photo-based check-in. If a check-in is missed, the app escalates its
accountability experience and, on supported Android configurations, may
use permitted device-management capabilities to apply a restricted mode.

The product must encourage hydration without creating an unsafe
situation, trapping a user away from essential phone functions, or
risking personal data.

## 2. Problem Statement

People often forget to drink water while working, studying, gaming, or
travelling. Gentle reminders alone can be easy to dismiss. SipSure adds
a user-configured commitment, proof-of-action check-ins, a clear
escalation policy, and an emergency bypass mechanism.

## 3. Target Platform and Technical Direction

-   **Initial platform:** Android-first.
-   **Implementation:** Choose a maintainable cross-platform or native
    approach only after validating the device-control requirements.
    Prefer Kotlin/native Android for device-management features if a
    cross-platform framework cannot support them reliably.
-   **iOS:** Treat as a later, reduced-capability version. Do not
    promise whole-device locking on iOS.
-   **Device restriction feasibility:** Whole-device kiosk/lock-task
    behavior generally requires an appropriately configured
    managed/dedicated device or device-owner setup. Ordinary consumer
    apps cannot silently or universally lock down an arbitrary Android
    phone. Validate the exact supported deployment model before
    committing to this feature.
-   **Architecture:** Keep hydration scheduling, verification, credit
    accounting, policy enforcement, and UI separated so that each can be
    tested independently.

## 4. Core User Journey

1.  The user creates a profile and reviews the safety and
    device-permission explanation.
2.  The user configures hydration intervals, quiet hours, reminder
    behavior, and the level of enforcement supported by their device.
3.  At each due time, the app sends a reminder and opens a check-in
    window.
4.  The user captures a fresh photo showing the drinking-water action,
    then submits it for verification.
5.  The app checks basic plausibility and duplicate/replay signals. It
    should avoid claiming that a photo proves actual consumption with
    certainty.
6.  A successful check-in is recorded and the next interval begins.
7.  If the user misses the check-in window, the app applies the
    configured escalation policy.
8.  If a restricted mode is supported and enabled, the user can still
    access emergency calling and other legally or technically essential
    functions.
9.  The user can use purchased credits to bypass a restriction when
    needed. Credit purchases must be transparent, authenticated, and
    recorded.
10. If the user's balance is empty, the app may offer up to three
    emergency credit advances, subject to the repayment rules described
    below.

## 5. Functional Requirements

### 5.1 Hydration Schedule

-   Let users configure reminder intervals and check-in windows.
-   Support editing schedules, pause periods, sleep/quiet hours, travel,
    and time-zone changes.
-   Make the next due time and consequences visible.
-   Prevent accidental schedules that create excessive reminders or
    unsafe enforcement.
-   Provide a clearly accessible pause and emergency path.

### 5.2 Photo Check-In

-   Capture a new photo from the in-app camera where practical.
-   Prefer on-device processing for basic checks when feasible.
-   Detect obvious duplicates and replay attempts without overstating
    accuracy.
-   Show clear outcomes: accepted, retry needed, or review required.
-   Minimize photo retention. Ask for informed consent, state retention
    periods, secure stored data, and allow deletion where legally and
    technically appropriate.
-   Do not infer medical status or claim to measure the quantity of
    water consumed from a photo.

### 5.3 Missed Check-In and Enforcement

-   Start with reminders and in-app restriction rather than immediate
    whole-device lockout.
-   Provide a configurable grace period and clear warnings.
-   Only use stronger Android enforcement on devices and deployments
    where the required APIs, permissions, and management model are
    supported and explicitly authorized by the user.
-   Never hide what enforcement will do.
-   Do not block emergency calls, emergency services, essential
    accessibility features, or access needed to recover the device.
-   Do not prevent the user from uninstalling the app on ordinary
    unmanaged devices.
-   If the device cannot safely or reliably enforce a lock, use an
    app-level accountability screen instead.

### 5.4 Credits and Emergency Bypass

-   Allow users to purchase bypass credits through a compliant,
    platform-approved payment flow.
-   Show credit balance, price, purchase confirmation, usage history,
    and refund/support information.
-   Deduct credits atomically and idempotently so retries cannot charge
    twice.
-   Support up to **three outstanding emergency advances** when the user
    has no credits, subject to clear terms and a 24-hour repayment
    window for each advance.
-   Explain the number of advances remaining and each due time before
    the user accepts an advance.
-   Do not use hidden fees, coercive dark patterns, or ambiguous debt
    language.
-   Do not lock the user out of essential phone functionality because a
    credit advance was not repaid.
-   The original concept proposed deleting all data if an advance is
    unpaid. **Do not implement data deletion or destructive penalties.**
    If an advance remains unpaid after 24 hours, mark it overdue,
    prevent additional advances until resolved, and show a clear
    repayment/status notice. Preserve the user's account, personal data,
    and access to essential functions.

### 5.5 Account and Data

-   Provide secure authentication if cloud sync or purchases require an
    account.
-   Store only necessary data.
-   Protect tokens, payment references, photos, and personal
    information.
-   Provide account export/deletion controls where applicable.
-   Maintain a privacy policy and terms before public release.

### 5.6 Accessibility and Safety

-   Support accessible text, screen readers, and clear visual states.
-   Provide a readily discoverable emergency override.
-   Do not treat a missed check-in as proof of wrongdoing; users may be
    driving, asleep, ill, in an emergency, or unable to use the phone.
-   Avoid shame-based language.
-   Include a setup warning that users should not drink excessive water
    or follow a schedule that conflicts with professional medical
    advice.

## 6. Non-Functional Requirements

-   **Reliability:** Scheduled reminders should survive normal process
    termination and device reboot where the OS permits.
-   **Battery:** Use platform scheduling APIs appropriately; avoid
    continuous background polling.
-   **Security:** Use least privilege, secure storage, transport
    encryption, server-side validation for purchases and balances, and
    dependency review.
-   **Privacy:** Minimize collection and retention; document all use of
    photos and device permissions.
-   **Resilience:** Gracefully handle offline mode, clock/time-zone
    changes, permission revocation, and payment verification failures.
-   **Observability:** Log operational events without logging secrets,
    raw photos, or unnecessary personal data.
-   **Testability:** Use unit tests for scheduling, credit ledger, grace
    periods, and escalation state transitions.

## 7. Suggested High-Level Components

1.  **Hydration Scheduler:** Computes due times and handles schedule
    changes.
2.  **Reminder and Check-In UI:** Presents prompts and captures
    check-ins.
3.  **Photo Verification:** Performs conservative plausibility and
    duplicate checks.
4.  **Policy Engine:** Calculates grace periods and escalation states.
5.  **Enforcement Adapter:** Uses only supported, user-authorized
    Android capabilities; falls back to app-level restrictions when
    unavailable.
6.  **Credit Ledger:** Tracks purchases, redemptions, and emergency
    advances with auditable transactions.
7.  **Payment Adapter:** Integrates with the approved payment
    provider/store billing flow.
8.  **Local Persistence:** Stores schedule and state safely.
9.  **Optional Backend:** Handles account sync, validated transactions,
    and recovery if cloud functionality is required.

## 8. Key States

-   `SETUP_REQUIRED`
-   `ACTIVE`
-   `CHECK_IN_DUE`
-   `GRACE_PERIOD`
-   `CHECK_IN_ACCEPTED`
-   `RESTRICTED_IN_APP`
-   `DEVICE_ENFORCEMENT_UNAVAILABLE`
-   `EMERGENCY_BYPASS`
-   `ADVANCE_OUTSTANDING`
-   `ADVANCE_OVERDUE`
-   `PAUSED`
-   `RECOVERY_REQUIRED`

All transitions must be explicit, tested, and recoverable. Avoid a state
in which the user cannot access essential device functions or recover
from an app failure.

## 9. MVP Scope

### Include

-   Android-first app shell and onboarding.
-   User-configurable hydration reminders and check-in windows.
-   Local schedule persistence and reboot/time-change handling.
-   Fresh photo capture and conservative check-in validation.
-   Missed-check-in grace period and app-level restriction.
-   Credit ledger prototype with test-mode purchases.
-   Up to three emergency advances with visible 24-hour due times.
-   Safe emergency bypass and recovery flow.
-   Privacy/security basics, unit tests, and build/release
    documentation.

### Defer

-   Production payments until billing, legal, and store requirements are
    reviewed.
-   Cloud sync until local-first flows are stable.
-   Any whole-device restriction until the supported Android
    management/deployment model is proven.
-   iOS version and advanced image/AI verification.
-   Social features, leaderboards, and medical recommendations.

## 10. Acceptance Criteria for MVP

-   Users can create, edit, pause, and resume a schedule.
-   Reminders and check-in windows behave correctly across restart and
    time changes within platform limits.
-   A check-in can be submitted, recorded, and reviewed without falsely
    claiming certainty about water consumption.
-   Missed check-ins follow a documented grace period and predictable
    escalation.
-   Credit balance and ledger remain consistent after retries, process
    death, and offline/online transitions.
-   Emergency advances never exceed three outstanding advances.
-   Each advance has a clear 24-hour repayment deadline.
-   Overdue advances never trigger deletion of personal data or block
    essential phone functions.
-   Unsupported device-enforcement scenarios fall back safely to
    app-level restrictions.
-   Permissions, privacy implications, and enforcement behavior are
    explained before activation.
-   Automated tests cover scheduling, ledger idempotency, advance
    limits, overdue handling, and recovery.

## 11. Risks and Decisions to Validate Early

-   Whether the intended Android deployment can legitimately use
    device-owner/lock-task capabilities.
-   App-store policies for accessibility, device administration, payment
    handling, and enforcement behavior.
-   Photo verification's limits, privacy impact, and potential for false
    acceptance/rejection.
-   Payment provider rules for credits, refunds, and advances.
-   Consumer protection and local legal requirements for any credit-like
    mechanism.
-   Battery and background scheduling restrictions across Android
    manufacturers.

**Do not build the product around an assumption of unrestricted control
over every phone. Validate platform feasibility before implementing
enforcement.**

## 12. Product Principles

1.  Safety before enforcement.
2.  Clear consent before permissions, restrictions, or purchases.
3.  Essential phone functions and user data are never bargaining chips.
4.  Prefer reliable, testable behavior over clever but fragile tricks.
5.  Be honest about the limits of photo verification and device control.
6.  Keep the MVP small and prove the core habit loop before adding
    complexity.
