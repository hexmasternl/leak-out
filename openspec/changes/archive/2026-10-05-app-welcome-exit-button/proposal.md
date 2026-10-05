## Why

The welcome screen's **Exit** button closes the app immediately, so an accidental tap throws the player out of the game. A confirmation dialog prevents that, while a "Never show this again" option keeps it from becoming an annoyance for players who know what they are doing.

## What Changes

- Tapping **Exit** on the welcome screen opens a modal confirmation dialog asking whether to leave the app, with **Yes** and **Cancel** actions and a **Never show this again** checkbox.
- **Yes** exits the app. **Cancel** closes the dialog and leaves the welcome screen unchanged.
- If **Never show this again** is checked when the player confirms or cancels, the preference is persisted. From then on, tapping Exit skips the dialog and exits the app immediately.
- The Exit behavior of the welcome screen changes from "always exits immediately" to "asks first unless the player opted out".

## Capabilities

### New Capabilities
- `exit-confirmation-dialog`: The confirmation dialog shown after tapping Exit, its Yes/Cancel behavior, and the persisted "never show again" preference.

### Modified Capabilities

None. `welcome-screen` (Exit action) is a still in-progress change with no spec in `openspec/specs/`; the affected behavior is stated in this change's spec and the tasks update that change's code.

## Impact

- New code in the Android app module: `ui/title/ExitConfirmationDialog`, a small preferences accessor for the "skip exit confirmation" flag (Jetpack DataStore Preferences, as chosen in `docs/project-plan.md`), and wiring in the welcome screen host.
- `WelcomeScreen` Exit no longer calls the host's exit directly when the dialog is enabled.
- Adds a DataStore setting that a future Settings screen can expose to re-enable the dialog.
