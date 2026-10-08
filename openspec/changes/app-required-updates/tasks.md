# Tasks

## 1. Update decision logic in core

- [x] 1.1 Add `core/.../core/update/UpdateStatus.kt` with `UpdateStatus` (`None`, `Optional`, `Required`) and `classifyUpdate(available, priority)` (priority ≥ 4 → required); add `UpdateStatusTest` covering not available, priorities 0, 3, 4 and 5, and an out-of-range priority, and verify `./gradlew :core:test --tests "*UpdateStatusTest"` passes
- [x] 1.2 Add `UpdateGate` (design.md, "Pure decision logic in core") with `mayStartLevel`, blocking-screen and once-per-process optional-prompt rules, including a required update found during a level; add `UpdateGateTest` covering every scenario of `specs/app-updates` that does not need Play, and verify `./gradlew :core:test --tests "*UpdateGateTest"` passes

## 2. Play In-App Updates wrapper

- [x] 2.1 Pin the newest stable `com.google.android.play:app-update` and `app-update-ktx` in `gradle/libs.versions.toml` and add them to `app/build.gradle.kts`; verify `./gradlew :app:assembleDebug` succeeds and the merged manifest gains no new permission besides any the library itself declares (record which) — app-update 2.1.0 adds no permission: the merged debug manifest keeps exactly INTERNET, ACCESS_NETWORK_STATE, VIBRATE and the dynamic-receiver permission
- [x] 2.2 Add the `AppUpdates` interface, `PlayAppUpdates` (wrapping `AppUpdateManager`, every failure → no update) and `FakeAppUpdates`; add JVM tests for `PlayAppUpdates` using Play's `FakeAppUpdateManager` covering no update, priority 5, priority 0, a thrown exception and the flexible download reaching `DOWNLOADED`, and verify `./gradlew :app:testDebugUnitTest --tests "*AppUpdates*"` passes — the fake only opens a flow for an info with real PendingIntents, which JVM stubs leave null, so the download test drives the registered listener with install states
- [x] 2.3 Add `UpdateViewModel` that checks on `onStart`, feeds `UpdateGate`, resumes an immediate update that is in progress on `onResume`, and exposes the state to Compose; add a JVM test with `FakeAppUpdates` for start, return to foreground and check failure, and verify it passes

## 3. Update screens and wiring

- [x] 3.1 Add the blocking update screen (update and close buttons, Back closes the app, design-system styling) and the optional update and "restart to install" prompts, with strings in every supported language; verify `StringResourcesTest` and `HardcodedTextTest` pass — other languages come from Play Console's automatic translation of res/values/strings.xml (docs/deployment.md#translations), so the strings are added there only
- [x] 3.2 Wire the screens into `MainActivity`: launch the immediate and flexible flows through `registerForActivityResult(StartIntentSenderForResult)`, show the blocking screen instead of any non-game screen while required, and gate next level and restart in the game screen through `mayStartLevel`; verify `./gradlew :app:testDebugUnitTest` passes
- [ ] 3.3 Add instrumented tests with `FakeAppUpdates`: required update at start blocks and Back closes the app; cancelled update shows the blocking screen again; optional prompt shown once and dismissible; required update during a level lets the level finish and turns next level and restart into the blocking screen; downloaded optional update is not offered during a level. Verify only these tests on the emulator with `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=nl.hexmaster.leakout.ui.update`, or record that the device run was skipped at the user's request

## 4. Release pipeline

- [ ] 4.1 Add `scripts/resolve-update-priority.sh` (`--labels`, `--manual`, `--check`; output `UPDATE_PRIORITY`, `UPDATE_KIND`, `UPDATE_SOURCE`) and `scripts/resolve-update-priority.test.sh` covering each label, no label, both labels, unrelated labels only, manual true and false; verify `bash scripts/resolve-update-priority.test.sh` passes
- [ ] 4.2 Add `.github/workflows/release-update-label.yml` (pull requests into `main`; opened, reopened, synchronize, labeled, unlabeled; job `Release update label`; `contents: read`; runs the script with `--check`); verify with `actionlint` if available, and by opening a test pull request into `main` without a label (check fails) and adding `update-optional` (check passes), or record that the live check is deferred to the first release pull request
- [ ] 4.3 Change `deploy-play-internal.yml`: `workflow_dispatch` input `required-update` (required boolean), `pull-requests: read`, run the script's test, resolve the priority right after checkout (pull request whose `merge_commit_sha` is `$GITHUB_SHA` and base is `main`), fail before building if unresolved, write it to the job summary and pass `inAppUpdatePriority` to the upload; verify with `actionlint` if available and by reviewing that no step before the priority step builds or uploads anything
- [ ] 4.4 Update `docs/deployment.md`: the two labels and what players see, setting `Release update label` as a required status check on `main` (branch protection or ruleset), the `required-update` input, that the priority cannot be changed after upload, and Play's update caching under troubleshooting; verify every label, input and job name in the guide matches the workflows

## 5. Integration

- [ ] 5.1 Run `./gradlew :core:test :app:testDebugUnitTest` and `./gradlew :app:assembleRelease` from `app/src/` and verify both succeed
- [ ] 5.2 After two consecutive releases on the internal testing track (the second labelled `update-required`), verify on a tester's device that the older version shows the blocking update screen and updates through Play; or record that this check is deferred and why
