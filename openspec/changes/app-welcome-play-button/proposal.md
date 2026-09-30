## Why

The welcome screen's **Play** button currently only emits a stubbed navigation event. Returning players want to pick up where they left off with one tap, while others want to choose a specific level. A small choice dialog after tapping Play serves both without adding a screen.

## What Changes

- Tapping **Play** on the welcome screen opens a modal dialog with two actions: **Continue** and **Select level**. The dialog can be dismissed (tap outside or back) without navigating.
- **Continue** starts the next level after the highest completed level, where "completed" means a history record exists (0-3 stars, as the level library counts it). With no completed level, Continue starts level 1.
- If the next level does not exist (all levels completed), Continue starts the last level. If the next level is still locked (first level of a page whose star threshold is not met), Continue opens the level library on that page instead of starting a level.
- **Select level** opens the level library (24 levels per page, leafable) on the page that contains the next level to be played.
- The welcome screen's Play event now opens the dialog instead of navigating directly.

## Capabilities

### New Capabilities
- `play-menu-dialog`: The modal dialog shown after tapping Play, the Continue target rule, and the Select level entry page rule.

### Modified Capabilities

None. `welcome-screen` (Play action) and `level-library` (initial page) are still in-progress changes with no spec in `openspec/specs/`; the affected behavior is stated in this change's spec and the tasks update those changes' code.

## Impact

- New code in the Android app module: `ui/title/PlayMenuDialog`, a pure-Kotlin `NextLevelResolver` use case with unit tests, and wiring in the welcome screen host.
- Reads finished levels from `game-level-history` and level count from `game-level-model`; unlock state from `level-library` rules.
- `level-library` gains an optional initial-page argument (defaulting to its existing "useful page" rule, which yields the same page).
- Welcome screen: Play no longer emits a navigation event to the host directly; it shows the dialog.
