# Design

## Context

See proposal.md for the motivation and specs/ for the required behaviour. Relevant current state:

- The app has no network code and declares no `INTERNET` permission. Play Games Services v2 is the only Google Play dependency.
- Navigation is a `Screen` sealed interface in `MainActivity` (`Welcome`, `Library`, `Settings`, `Achievements`, `Game`, ...). The game screen gets `onExit` and `onNextLevel` from `MainActivity`; restart is handled inside the game screen.
- `deploy-play-internal.yml` runs on push to `main` and on `workflow_dispatch`, computes the version with `scripts/compute-release-version.sh`, and uploads with `r0adkll/upload-google-play` v1.1.5, which accepts an `inAppUpdatePriority` input. Release scripts live in `scripts/` as `<name>.sh` with a `<name>.test.sh` next to them.
- Releases reach `main` through a `development` → `main` pull request.

## Goals / Non-Goals

**Goals:**
- The required/optional/none decision and the "finish the level first" rule are pure Kotlin in `core`, fully unit-tested without Play.
- Play is behind one small app-side interface, so screens are instrumented-tested with a fake and the Play wrapper is tested with Play's own `FakeAppUpdateManager`.
- The label → priority mapping is one shell script used by both workflows and covered by a `.test.sh`.

**Non-Goals:**
- Showing release notes or version numbers in the update UI (Play's own flow shows them).
- Staleness rules (for example "optional becomes required after N days"). Only the priority decides.
- Any fallback update mechanism for installs that did not come from Google Play.

## Decisions

### Google Play In-App Updates, priority threshold 4
Play reports `updateAvailability()` and `updatePriority()` through `AppUpdateManager.appUpdateInfo`. The app maps priority ≥ 4 to required and anything lower to optional; the pipeline only ever sets 5 or 0, which leaves room to use 1–3 and 4 later without an app change.
*Alternative:* a version file in the GitHub repository. Rejected (decided in #97): it needs the `INTERNET` permission and GitHub availability, and the update still happens in Play.

### Pure decision logic in `core`
A new package `nl.hexmaster.leakout.core.update`:
- `UpdateStatus` (`None`, `Optional`, `Required`) and `classifyUpdate(available: Boolean, priority: Int): UpdateStatus`.
- `UpdateGate`: given the current `UpdateStatus` and whether a level is in progress, answers `mayStartLevel()`, `showBlockingScreen(onWelcome/inLevel)` and `showOptionalPrompt()` (once per process). This is where "a required update found mid-level waits for the level to end" lives.

### App-side `AppUpdates` interface
`app/.../update/AppUpdates.kt` exposes `suspend fun check(): UpdateCheck` (available, priority, immediate/flexible allowed, download state), `startRequiredUpdate(launcher)`, `startOptionalUpdate(launcher)`, `completeUpdate()` and a flow of flexible download state. `PlayAppUpdates` wraps `AppUpdateManager` (`app-update` + `app-update-ktx`) and turns every exception into "no update". `FakeAppUpdates` is used by instrumented tests. An `UpdateViewModel` owns the state, runs the check from `onStart` (covers both app start and return to the foreground) and keeps the optional prompt to once per process.

### Play flows: immediate for required, flexible for optional
The required update uses `AppUpdateType.IMMEDIATE` started from our own blocking screen, not as the blocking mechanism itself: the Play immediate flow can be cancelled, so our screen stays until the app is actually replaced. On `onResume`, an `DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS` update is resumed. The optional update uses `AppUpdateType.FLEXIBLE`; when its state becomes `DOWNLOADED`, the "restart to install" prompt is shown only when no level is in progress, and `completeUpdate()` is called only after the player agrees. Flows are launched with `registerForActivityResult(StartIntentSenderForResult)` and `AppUpdateOptions`.

### Gating level starts
`MainActivity` renders the blocking update screen instead of any non-game `Screen` while `UpdateGate` says so. The game screen gets a `mayStartLevel` check; its next-level and restart actions go to the blocking screen when the gate is closed. The level itself is never interrupted.

### Label → priority script
`scripts/resolve-update-priority.sh` takes either `--labels "<newline separated labels>"` or `--manual true|false` and prints `UPDATE_PRIORITY=<0|5>`, `UPDATE_KIND=<optional|required>` and `UPDATE_SOURCE=<label|manual input>`, or fails with a message naming both labels (none or both labels). `--check` mode only validates, for the pull request check.

### Pull request check workflow
`.github/workflows/release-update-label.yml`, on `pull_request` (not `pull_request_target`) to `main` with types `opened, reopened, synchronize, labeled, unlabeled`, `permissions: contents: read`, one job named `Release update label` that runs the script in `--check` mode on `github.event.pull_request.labels`. That job name is what the maintainer selects as the required status check on `main`.

### Finding the pull request in the deploy
A step right after checkout calls `gh api repos/$GITHUB_REPOSITORY/commits/$GITHUB_SHA/pulls`, keeps the pull request whose base is `main` and whose `merge_commit_sha` is `$GITHUB_SHA`, and passes its labels to the script. The deploy job gets `pull-requests: read`. For `workflow_dispatch`, a required boolean input `required-update` goes to `--manual`. The result goes into `$GITHUB_ENV` and the job summary, and the upload step passes `inAppUpdatePriority: ${{ env.UPDATE_PRIORITY }}`. The script's `.test.sh` runs in the workflow before it is used, as with the release notes script.

## Risks / Trade-offs

- [Play does not allow changing a release's priority after upload] → documented; ship a new release with `update-required`.
- [Play caches update information; a new release can take a while to show up as available on a device] → documented in the troubleshooting part of `docs/deployment.md`; this delays, but never breaks, the required update.
- [In-app updates work only for installs from Google Play, so debug builds and sideloaded APKs never see an update] → by design they behave as "no update"; testing is done through the internal testing track with two consecutive releases.
- [A player can force-close during the immediate flow and be offline afterwards] → offline means no check, so they can play until they are online again. Accepted: a failed check never locks the player out (spec).
- [The required status check is a repository setting, not code] → `docs/deployment.md` describes it step by step; the deploy also fails on a missing label, so a misconfigured setting never yields a release with a guessed priority.

## Migration Plan

1. Merge the implementation through `development`.
2. Before the `development` → `main` pull request that carries it, set the required status check `Release update label` on `main` and label that pull request (normally `update-optional`).
3. Rollback: revert the commits; releases already uploaded keep their priority, and older app versions ignore it.
