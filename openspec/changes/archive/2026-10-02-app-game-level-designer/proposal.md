Tracking issue: [#43 Level designer](https://github.com/hexmasternl/leak-out/issues/43).

## Why

Levels are written by hand as `level_NNN.json` files, which is slow and error prone: coordinates are counted on paper, open sides are easy to get wrong, and a mistake only shows up when the parser, validator or solver test fails. An in-game level designer lets a developer lay out a level visually on the real playing field, see at once whether it is valid and solvable, try it out, and export the JSON. It is a development tool, so players must never see it.

## What Changes

- Add a build-type feature flag `levelDesigner`: on in debug builds, off in release builds. It is the single switch the app checks to decide whether the designer exists.
- When the flag is on, the welcome screen shows a small, unobtrusive "Level designer" button (an icon button in a corner, not part of the Play / Settings / Exit row). When the flag is off, the button is not composed at all.
- Tapping it opens a new Level Designer screen (landscape, game theme) that starts from a new blank level, or from a copy of an existing bundled level.
- In the designer the developer can:
  - set the grid size (columns and rows, 4 to 50 each); shrinking the grid drops blocked cells that fall outside it and keeps start and finish inside it;
  - place the start and the finish by tapping a cell, and change each one's open side;
  - toggle blocked cells by tapping (or dragging across) cells;
  - edit the remaining level fields: id, name, flow (initial delay, cells per second, reservoir speed factor, speed ramp), stack (visible size, weight per section), rules (allow replace, replace penalty) and the star thresholds.
- The designer validates the level live with the existing `LevelValidator` and `LevelSolver`, and lists every problem by field. Export and test play are only enabled for a valid, solvable level.
- **Test play**: plays the designed level in the normal game screen, without recording anything in the level history, and returns to the designer afterwards with the design intact.
- **Export**: produces the level's JSON in the documented level format (only non-default optional fields written), copies it to the clipboard and offers the Android share sheet, so it can be saved as `assets/levels/level_NNN.json`. The exported JSON parses back to the same level.
- The designer keeps the level being edited across configuration changes and process death; designs are not stored permanently.

## Capabilities

### New Capabilities
- `feature-flags`: build-type feature flags, starting with `levelDesigner` (on in debug, off in release), and the rule that flagged-off features are not reachable.
- `level-designer`: the Level Designer screen: creating a level, editing grid, start, finish, blocked cells and all other level fields, live validation and solvability, test play and JSON export.
- `level-serialization`: writing a `Level` back to level-format JSON that round-trips through `LevelParser`.

### Modified Capabilities
- `welcome-screen`: adds the level designer entry button, shown only when the `levelDesigner` flag is on.

## Impact

- `app/build.gradle.kts`: enable `buildConfig` and add a per-build-type `LEVEL_DESIGNER` `buildConfigField`; new `app/.../FeatureFlags.kt`.
- `app/.../ui/title/WelcomeScreen.kt`: optional `onOpenLevelDesigner` callback and the corner button.
- `app/.../MainActivity.kt`: new `Screen.Designer` and `Screen.TestPlay` destinations; test play uses `GameScreen` with no history.
- New `app/.../ui/designer/` package (screen, view model, grid editor, field editors) with JVM and instrumented tests.
- New pure-Kotlin code in `core/.../level/`: `LevelWriter` (Level to JSON) and a `LevelDraft` editing model with grid resize and cell-tool operations, with unit tests.
- No new dependencies: kotlinx.serialization is already used by `core`; the share sheet and clipboard are platform APIs.
- `docs/level-format.md`: a short section on creating levels with the designer.
