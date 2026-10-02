## Context

The welcome screen (`app-welcome-screen`) is stateless: `WelcomeScreen(onPlay, onSettings, onExit)`. The level library (`app-game-level-library`) shows a 6x4 paged grid with unlock rules and already opens on a "useful page". History (`app-game-level-history`) stores one Room row per finished level (stars 1-3), so a row existing means completed with at least one star. Level count comes from `game-level-model`.

## Goals / Non-Goals

**Goals:**
- A Play dialog with Continue and Select level.
- One shared rule for "next level to be played", used by both actions so they agree.
- Rule is pure Kotlin and unit-testable.

**Non-Goals:**
- The game screen and the library UI themselves.
- Changing unlock rules or the star threshold.
- Resuming a half-played level.

## Decisions

**Dialog is local welcome-screen state.** `WelcomeScreen` holds `showPlayMenu` and renders `PlayMenuDialog(onContinue, onSelectLevel, onDismiss)`, a Compose `Dialog` styled with the design-system card (white surface, 3px ink border, cartoon buttons, Fredoka header). Play now toggles state instead of calling the host. Host callbacks become `onStartLevel(levelId)` and `onOpenLevelLibrary(page)`. Alternative: a navigation destination for the dialog, rejected as overkill.

**`NextLevelResolver` use case.** Inputs: completed level ids (from `LevelHistoryRepository.observeAll()`), total level count, page size 24, and the library's unlock rule. Output `NextLevel(levelId, page, isUnlocked)`:
- `highest = max(completed ids)`, or 0 if none.
- `levelId = min(highest + 1, totalLevels)`; `page = (levelId - 1) / 24 + 1`.
- `isUnlocked` from the level-library unlock use case (level `n` is unlocked once `n-1` is finished, except a page's first level, which needs the star threshold).

Continue: if `isUnlocked`, emit `onStartLevel(levelId)`; otherwise emit `onOpenLevelLibrary(page of highest)` so the player sees the star-gate hint. Select level: emit `onOpenLevelLibrary(page)`.

"Completed" equals "row exists", 0-star wins included, matching `LevelLibraryRules` and `asHistoryReader` (history accepts 0-star wins since `app-game-level-history`). By those rules the first level of a page also unlocks when the level before it is finished, so the locked fallback is defensive; it is kept and tested through `NextLevel.continueTarget`.

**Library initial page argument.** The library screen takes an optional `initialPage`. When absent it uses its existing rule (page of the lowest unlocked unfinished level). With linear progress both give the same page; the explicit argument keeps the dialog's contract testable and avoids the library re-deriving it. `initialPage` is clamped to accessible pages.

**Data read on tap.** The resolver takes a snapshot (`first()` of the history flow) when a button is tapped, so no state is held while the dialog is open and results are never stale.

## Risks / Trade-offs

- [Highest completed differs from lowest unfinished when the player has gaps] → The spec follows the request literally (next after the highest); the library remains available for skipped levels.
- [Next level locked by the star gate means Continue cannot start a level] → Falls back to the library on the relevant page, where the star hint explains the requirement.
- [History read fails or is slow at tap time] → Keep the dialog open and do not navigate; never default to level 1 on error, only when history is genuinely empty.

## Open Questions

- Whether Continue should read "Start" on a fresh install; kept as "Continue" for now.
