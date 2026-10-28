# SipSure: Development and Agent Instructions

## Project Rules (`instructions.md`)

These instructions govern implementation, coding agents, Git activity,
testing, releases, and project documentation. Follow them for every
change unless the user explicitly authorizes an exception.

## 1. Source of Truth and Required Files

-   `plan.md` is the product's foundational plan. **Do not modify it
    during routine implementation.** Change it only when the user
    explicitly approves a product-plan change.
-   `instructions.md` contains the engineering, safety, Git, and release
    rules. Update it only when the user asks to change the project rules
    or explicitly approves a change.
-   Create and maintain these two additional files:
    -   `changelog.md`: chronological record of meaningful changes,
        tests, decisions, fixes, and releases.
    -   `currentstate.md`: concise description of the app's actual
        current implementation, working features, architecture, known
        issues, build/run commands, and next steps. An agent should be
        able to understand the current state without reading the entire
        source tree.
-   Keep documentation truthful. Do not claim a feature is complete
    unless it is implemented and verified.
-   Update `changelog.md` and `currentstate.md` before committing
    whenever feasible.

## 2. Mandatory Workflow

For each task:

1.  Inspect the repository, current branch, working tree, project files,
    and existing instructions before changing anything.
2.  Read `plan.md`, `instructions.md`, `changelog.md`, and
    `currentstate.md` when present.
3.  Identify the smallest safe implementation that satisfies the task.
4.  Preserve all existing user changes, including uncommitted and
    untracked files.
5.  Make focused changes. Avoid unrelated refactors and speculative
    features.
6.  Build/compile and run relevant tests before committing or pushing.
7.  Review the complete diff, including staged and unstaged changes.
8.  Update documentation to reflect verified changes.
9.  Commit and push only under the Git rules below.
10. Report what changed, commands/tests run, outcomes, commit/push
    status, and remaining risks.

If a build or test fails, investigate and fix it before claiming
success. If it cannot be fixed safely, report the failure and do not
push a broken solution.

## 3. Product and Safety Boundaries

-   Follow `plan.md` and its product principles.
-   Android is the initial target. Validate the chosen technology
    against Android scheduling, permissions, and device-management
    requirements before locking in architecture.
-   Never promise that a normal consumer app can silently or universally
    lock any Android phone. Whole-device enforcement must be proven
    feasible for the declared deployment model and must use supported,
    user-authorized APIs.
-   If device enforcement is unsupported, denied, or unreliable, use an
    app-level restriction and explain the limitation.
-   Never interfere with emergency calling or other essential device
    functions.
-   Never implement remote lockout, stealth persistence, anti-uninstall
    tricks on ordinary unmanaged devices, or any behavior designed to
    trap users on their own phones.
-   Never delete user data, account data, or local files as punishment
    for missing a check-in or failing to repay an emergency advance.
-   Emergency advances are limited to three outstanding advances. Each
    must display its amount/status and 24-hour repayment deadline.
    Overdue status may block further advances, but must not trigger data
    destruction or block essential phone functionality.
-   Make permissions, device restrictions, photo processing, billing,
    and credit terms clear before the user opts in.
-   Do not make medical claims. Do not encourage excessive water intake
    or disregard for medical advice.
-   Photo verification must be described as plausibility checking, not
    definitive proof of consumption.
-   Do not hard-code secrets, private keys, payment credentials, tokens,
    or personal data.

## 4. Technology and Architecture

-   Keep the implementation simple and maintainable. Do not add
    frameworks or dependencies without a clear requirement.
-   Prefer well-supported platform APIs for reminders and background
    work. Avoid continuous polling when OS scheduling APIs are
    appropriate.
-   Separate scheduling, check-in verification, enforcement policy,
    payment/credit ledger, persistence, and UI so they can be tested
    independently.
-   Treat credit balance changes as transactions. Use unique transaction
    identifiers and idempotency safeguards to prevent duplicate charges
    or redemptions.
-   Do not trust client-only payment success. Production purchases must
    be validated according to the selected store/provider's
    requirements.
-   Handle offline use, process termination, device reboot,
    clock/time-zone changes, permission revocation, and interrupted
    payments gracefully.
-   Request the minimum permissions required and explain why each is
    needed.
-   Do not introduce a backend until the MVP requires it. If a backend
    is added, document authentication, authorization, data retention,
    and recovery behavior.

## 5. Coding Standards

-   Use the repository's established language, formatting, architecture,
    naming, and dependency conventions.
-   Write readable, production-quality code with focused functions and
    clear names.
-   Avoid duplicated logic, unnecessary abstractions, dead code, and
    broad unrelated rewrites.
-   Validate external input and handle errors explicitly.
-   Use structured error states rather than swallowing exceptions.
-   Do not log secrets, access tokens, raw photos, payment details, or
    unnecessary personal data.
-   Add or update tests for changed behavior, especially scheduling,
    state transitions, billing/credit accounting, and recovery.
-   Add comments where they explain intent, platform constraints, or
    non-obvious decisions, not where they merely repeat the code.
-   Keep accessibility, screen-reader labels, readable contrast, and
    clear error messages in mind.

## 6. Security and Privacy

-   Follow least privilege and secure-by-default settings.
-   Store credentials and sensitive tokens using platform-appropriate
    secure storage.
-   Use encrypted transport for network requests and validate server
    responses.
-   Keep personal data and photos to the minimum required; define
    retention and deletion behavior.
-   Never commit `.env` files, signing keys, keystores, certificates,
    credentials, production configuration, customer data, or private
    test data.
-   Ensure `.gitignore` covers platform build outputs, IDE files, local
    configuration, secrets, and generated artifacts.
-   Review dependencies and permissions before adding them.
-   Do not add analytics or telemetry that collects personal data
    without an explicit product requirement and disclosure.

## 7. Build and Test Requirements

-   Compilation/build verification is mandatory before every commit and
    push.
-   Run the most relevant automated tests. Add tests when practical for
    new logic.
-   For Android, use the project's configured Gradle wrapper and
    documented JDK/Android SDK versions.
-   Do not claim that a test passed unless it was actually run and its
    result observed.
-   If an environment lacks the required SDK, emulator, credentials, or
    network access, report exactly what could and could not be verified.
-   Never push a solution that does not build. If a change leaves the
    solution non-building, fix it before pushing, even if that means
    exceeding the preferred change-size threshold.

## 8. Git and Release Rules

### Branch and working tree

-   Work on the existing intended branch unless the user authorizes a
    branch change.
-   Check `git status` before editing and preserve uncommitted user
    changes.
-   Never use destructive commands such as `git reset --hard`,
    `git clean -fd`, force push, history rewrite, or branch deletion
    without explicit permission.
-   Never overwrite or discard user work to make a build pass.

### Change size and push frequency

-   Push after every file change that leaves the solution buildable;
    **or**
-   At minimum, push when approximately 50 lines of production code have
    changed since the last push, whichever comes first.
-   Compilation is mandatory. A push must never leave the solution
    non-building. If honoring the 50-line threshold would leave the
    solution non-compiling, the compilation requirement wins.
-   Keep commits focused and use clear, descriptive commit messages.

### Before commit

-   Review `git diff` and `git diff --cached`.
-   Stage only intended files. Check staged content for secrets,
    credentials, personal data, generated output, and unrelated changes.
-   Do not commit database files, customer data, build output, `.vs/`,
    `bin/`, or `obj/`.
-   Ensure `.gitignore` covers the IDE and build artifacts for the
    actual stack.
-   Update `changelog.md` and `currentstate.md` when feasible.
-   Do not add an AI co-author trailer or claim human review that did
    not occur.

### Push

-   Push only to the configured remote and intended branch.
-   Never force push without explicit permission.
-   Verify that the push succeeded. If it fails, report the error and do
    not imply that the remote was updated.
-   Do not create a release or publish a build except through the release process in
    section 9 (a pushed version tag), and only as that section allows.

## 9. Release Management

Releases follow the same flow as the `teacher` repository: a pushed version tag builds, signs and
publishes a GitHub Release from CI. Nothing is released by hand-uploading a locally built APK.

### Versioning

-   Semantic versioning: `MAJOR.MINOR.PATCH` in `APP_VERSION_NAME` (`gradle.properties`).
    `APP_VERSION_CODE` is an integer that increases by at least one on every release and never
    goes down or is reused. These two properties are the only place the version is defined.
-   Tags are `vMAJOR.MINOR.PATCH` and must equal `APP_VERSION_NAME`; the release workflow fails
    if they differ.
-   While the app is `0.x`, a new user-visible feature or behavior change bumps MINOR, a fix or
    internal change bumps PATCH. `1.0.0` needs the user's explicit approval and the review in the
    "Before a public or production release" list below.

### Incremental changes trigger a release

-   Each meaningful increment (a feature, a user-visible fix, or a rule change) that is
    committed and pushed under section 8 also gets its own version: in the same commit, bump
    `APP_VERSION_NAME` and `APP_VERSION_CODE` and add the version to the top of `changelog.md`.
-   After CI is green on that commit, tag it and push the tag (`git tag vX.Y.Z` then
    `git push origin vX.Y.Z`). The tag push publishes the release; do not create the release by
    other means.
-   Documentation-only or CI-only changes do not need a release. Changes that touch the app's
    behavior, rules, or UI always do.
-   Creating and pushing a tag publishes a build outside this machine, so do it only after the
    user has asked for releases to be cut automatically (this section, once the user approves
    it, is that standing request) or for a specific tag. If unsure, ask before tagging.

### Pre-releases

-   A tag with a hyphen (`v0.2.0-rc1`, `v0.2.0-beta1`) publishes as a pre-release and never takes
    the "Latest" badge. Use it to rehearse the pipeline or share a build that is not ready.
-   Pre-release suffixes do not change `APP_VERSION_NAME`: `v0.2.0-rc1` ships `0.2.0` with the
    versionCode the commit declares.

### What the release workflow does (`.github/workflows/release.yml`)

-   Runs on every `v*` tag: checks the tag against the version, runs unit tests, writes the
    signing files from repository secrets, builds the signed release APK and app bundle with
    R8 shrinking, verifies the APK signature, and publishes the GitHub Release with
    two APKs (NN = `APP_VERSION_CODE`): `sipsure-arm64-v8a-rNN.apk` (most modern phones) and
    `sipsure-rNN.apk` (universal). Every release, stable or pre-release, must carry both, a
    "Which file should I download?" section in the notes, and a device label on each asset, as in
    the `teacher` repo. While the app has no native libraries the two files are identical; if
    native code is added, the arm64-v8a file must become a real arm64-only build.
-   Archives the signed `.aab` and the R8 `mapping.txt` for 90 days. Keep `mapping.txt` to read
    release crash traces.
-   `.github/workflows/ci.yml` runs unit tests and a debug build on every push and pull request.
    A tag should only be pushed from a commit whose CI run is green.

### Signing key

-   One release keystore signs every release. Android refuses to update an installed app that
    was signed with a different key, so **losing or replacing this key breaks upgrades for every
    existing install**.
-   The keystore and `key.properties` live outside the repository (currently
    `~/.sipsure-signing/`) and in the repository secrets below. Back them up somewhere private
    (a password manager or encrypted drive). Never commit them, paste them into chat, issues,
    logs or documentation, or print them in CI.
-   Repository secrets (Settings, Secrets and variables, Actions): `ANDROID_KEYSTORE_BASE64`,
    `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. The workflow fails
    fast if one is missing.
-   `key.properties`, `*.jks` and `*.keystore` stay in `.gitignore`. Without `key.properties`
    the release build is produced unsigned rather than falling back to a debug key.
-   Rotating the key needs the user's explicit approval and a plan for existing installs
    (Play App Signing key upgrade, or reinstalling).

### Release artifacts

-   Release builds must not contain debug-only code. Debug-only helpers (such as the test clock
    receiver) live in `app/src/debug` and are therefore absent from release builds; check the
    release APK manifest if anything is moved.
-   Do not include secrets, test payment credentials or personal data in release artifacts.
-   Build release artifacts only in CI from the tagged commit, not from a dirty working tree.
-   Each release's notes state the supported Android versions (minSdk 26), the permissions, that
    credits are in test mode until real billing exists, and the device-owner requirement for
    the phone lock. Known limitations from `currentstate.md` go in the notes when relevant.
-   Document each artifact's purpose in the release notes (the two-APK download guide).

### Before a public or production release

-   Do not distribute a production build until signing, privacy policy and terms, store-policy
    and device-enforcement assumptions, billing and consumer-credit rules (`plan.md` section 11)
    have been reviewed. Until then every tag is `0.x` and the release notes say it is a test
    build.

## 10. Documentation Rules

### `changelog.md`

For each meaningful change, record: - Date - Summary of changes -
Important implementation decisions - Tests/build commands and observed
results - Known issues or follow-up work

Keep entries chronological, newest first or follow the repository's
existing convention consistently.

### `currentstate.md`

Keep this file short, factual, and current. Include: - Product goal and
current maturity - Target platform and chosen stack - Implemented and
verified features - Important architecture and data-flow notes -
Build/run/test commands - Permissions and external services - Known
limitations and risks - Next recommended tasks

Do not duplicate the entire product plan or paste source files into
these documents.

## 11. Completion Report

At the end of a task, report: - Files changed - Behavior implemented -
Build and test commands run, with actual outcomes - Git commit and push
status, if applicable - Known limitations, assumptions, and next steps

Clearly distinguish implemented behavior from a proposal or placeholder.

## 12. Definition of Done

A task is complete only when: - The requested change is implemented
within scope. - The code is readable and security/privacy implications
have been considered. - Relevant tests pass, or unavailable tests are
explicitly reported. - The app builds successfully before commit/push. -
The diff contains no accidental changes or secrets. - `changelog.md` and
`currentstate.md` are updated when feasible. - The solution follows the
safety boundaries in this file and `plan.md`. - The final report
accurately states what was and was not verified.
