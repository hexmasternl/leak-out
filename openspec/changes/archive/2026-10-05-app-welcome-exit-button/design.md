## Context

The welcome screen (`app-welcome-screen`) is stateless: `WelcomeScreen(onPlay, onSettings, onExit)`, where `MainActivity` implements Exit with `finishAffinity()`. `app-welcome-play-button` already introduces local dialog state (`showPlayMenu`) in the welcome screen with a Compose `Dialog` in the design-system card style. `docs/project-plan.md` selects Jetpack DataStore (Preferences) for settings.

## Goals / Non-Goals

**Goals:**
- Confirm before exiting, with a persisted opt-out.
- Reuse the dialog styling and local-state pattern of the Play dialog.
- Decision logic (show dialog or exit) is unit-testable.

**Non-Goals:**
- A Settings UI to re-enable the dialog (the stored flag makes it possible later).
- Changing the system Back button behavior on the welcome screen.
- Confirming exits from other screens.

## Decisions

**Dialog is local welcome-screen state.** `WelcomeScreen` holds `showExitDialog` and renders `ExitConfirmationDialog(onConfirm, onCancel, onDismiss)`: a Compose `Dialog` with the design-system card (white surface, 3px ink border, Fredoka title), a `Checkbox` row "Never show this again", and cartoon buttons "Yes" (error-red) and "Cancel". Checkbox state is local to the dialog and reset each time it opens. Alternative: navigation destination, rejected as overkill (same reasoning as the Play dialog).

**Preference in DataStore.** A `SettingsRepository` (or the existing settings accessor if present when implemented) exposes `skipExitConfirmation: Flow<Boolean>` and `setSkipExitConfirmation(Boolean)`, backed by a Preferences key `skip_exit_confirmation`, default `false`. Alternative: SharedPreferences, rejected to stay consistent with the project plan.

**Decision at tap time.** On Exit tap, the host reads a snapshot (`first()`) of the flag: if `true`, call `onExit()` directly; otherwise show the dialog. This avoids holding state while the dialog is closed and never shows a stale value. DataStore reads are fast enough that no loading state is needed. If the read fails, fall back to showing the dialog (the safe default).

**When the checkbox is persisted.** The flag is saved when the player taps **Yes** with the box checked, and also on **Cancel** with the box checked, matching the request ("if the user checks the checkbox, a setting is kept"). Dismissing by tapping outside or Back is treated as Cancel without persisting, since the player made no explicit choice. Persisting completes before `onExit()` is called on Yes so the write is not lost when the process ends.

**Exit mechanics unchanged.** Yes calls the existing `onExit()` (`finishAffinity()`).

## Risks / Trade-offs

- [Player checks the box, cancels, and later cannot get the dialog back] → Acceptable for now; the stored flag lets a future Settings screen offer a reset.
- [DataStore write racing with `finishAffinity()`] → Await the write (suspend in the coroutine scope) before invoking `onExit()`.
- [Persisting on Cancel may surprise players who expect only Yes to save] → Follows the requested behavior; the checkbox label is explicit.

## Open Questions

- Should a Settings entry to re-enable the confirmation be part of the Settings screen change? Assumed yes, out of scope here.
