# Proposal

## Why

Once a release fixes a serious problem, there is no way to make players move to it: the app never checks for updates, and anyone with auto-updates off keeps playing the broken version. The maintainer wants to be able to require an update for major fixes while minor releases stay optional, without running a server. Tracked in [#97](https://github.com/hexmasternl/leak-out/issues/97).

## What Changes

- The app uses **Google Play In-App Updates** (`com.google.android.play:app-update`) to ask Play, at app start and when it comes back to the foreground, whether a newer version is available and what its *in-app update priority* is (0–5). No server of our own and no new permission.
- An available update with priority 4 or higher is **required**: the player gets a blocking update screen and cannot start a game until they update. A player who is in a level when it is detected finishes that level first.
- An available update with a lower priority is **optional**: at app start the player gets a dismissible prompt and can keep playing; a downloaded optional update is installed only on the player's say-so and never during a level.
- A failed or unavailable check (offline, not installed from Play, debug build) never blocks the player.
- **Release pipeline:** every pull request into `main` must carry exactly one of the labels `update-optional` or `update-required`. A new pull request check workflow enforces this and is made a required status check on `main`.
- The deploy workflow reads that label from the pull request behind the pushed commit and uploads the release with `inAppUpdatePriority` 5 (`update-required`) or 0 (`update-optional`). It fails before building when no pull request or label is found. A manual run takes a required `required-update` input instead.
- `docs/deployment.md` explains the labels, the required status check setup and that Play does not allow changing a release's priority after upload.

## Capabilities

### New Capabilities
- `app-updates`: how the app checks Google Play for a newer version and handles required and optional updates for the player.

### Modified Capabilities
- `app-deployment`: deployments set the in-app update priority from a mandatory pull request label (or a manual-run input), a pull request check enforces the label, and the setup guide documents it.

## Impact

- `app/src/gradle/libs.versions.toml`, `app/src/app/build.gradle.kts`: Play In-App Updates library.
- `app/src/core`: pure decision logic for "required, optional or none" and for deferring a required update until the current level ends.
- `app/src/app`: an update checker wrapping `AppUpdateManager`, the blocking update screen and the optional update prompt, wired into `MainActivity` and the game screen; new strings in every supported language.
- `.github/workflows/release-update-label.yml` (new), `.github/workflows/deploy-play-internal.yml`, `scripts/resolve-update-priority.sh` (new) with `scripts/resolve-update-priority.test.sh`.
- `docs/deployment.md`; a one-time repository setting (required status check on `main`) that the maintainer applies by hand.
- No level format, settings or save data changes.
