Tracking issue: [#43 Level designer](https://github.com/hexmasternl/leak-out/issues/43).

## 1. Feature flag

- [ ] 1.1 In `app/build.gradle.kts`, enable `buildFeatures.buildConfig` and add `buildConfigField("boolean", "LEVEL_DESIGNER", ...)`: `true` for `debug`, `false` for `release`
- [ ] 1.2 Add `app/.../FeatureFlags.kt` exposing `levelDesigner` from `BuildConfig.LEVEL_DESIGNER`
- [ ] 1.3 JVM test that the debug unit test variant sees `FeatureFlags.levelDesigner == true`; verify `assembleRelease` generates `LEVEL_DESIGNER = false` (test or build check reading the generated `BuildConfig`)

## 2. Level writer (core)

- [x] 2.1 Implement `core/.../level/LevelWriter.kt`: `Level` to pretty-printed level-format JSON, two-space indent, trailing newline, key order as in `docs/level-format.md`, optional fields omitted when equal to `LevelDefaults`, empty `stack`/`rules`/`weights` omitted
- [x] 2.2 Unit tests: minimal level has no `stack`/`rules` and only required `flow` keys; single changed weight written alone; empty `blocked` written as `[]`; blocked order kept
- [x] 2.3 Round-trip tests: every bundled `level_NNN.json` parse → write → parse is equal; a level with every optional field non-default round-trips; an invalid level (for example start on the edge) still writes and parses back

## 3. Level draft editing model (core)

- [x] 3.1 Implement `core/.../level/LevelDraft.kt` with `LevelDraft.new(id)` producing the blank level from the spec (7x5, start 0,2 `RIGHT`, finish 6,2 `LEFT`, flow 20000 ms / 0.4, stars 50/80/120, defaults elsewhere)
- [x] 3.2 `resize(columns, rows)`: clamp to 4..50, drop blocked cells outside the grid, move an outside start/finish to the nearest inside cell keeping its open side
- [x] 3.3 `placeStart`/`placeFinish` (keep open side, unblock target cell, ignore the other endpoint's cell, ignore cells outside the grid) and `setOpenSide`
- [x] 3.4 `toggleBlocked`, `paintBlocked(cells, blocked)` and `clearBlocked`, never blocking an endpoint and never adding a duplicate
- [x] 3.5 Field setters for id, name, flow, stack visible size and weights, rules and stars
- [x] 3.6 Implement `DesignStatus` (`Invalid(errors)`, `Unsolvable(reason)`, `Solvable(path)`) derived from `LevelValidator` then `LevelSolver`, plus mapping of error fields (`start.*`, `finish.*`, `blocked[i]`) to highlighted cells
- [x] 3.7 Unit tests for every `LevelDraft` operation and every spec scenario (grow, shrink drops blocked, shrink moves finish, range limits, start onto blocked, endpoints cannot share a cell, toggle, drag paint skipping an endpoint, clear) and for `DesignStatus` (blank is solvable, several errors listed with highlighted start, walled-off finish unsolvable)

## 4. Designer view model and export (app)

- [ ] 4.1 Define a `LevelExporter` interface and the Android implementation (clipboard with label `level_NNN.json`, `ACTION_SEND` chooser with JSON text and subject `level_NNN.json`)
- [ ] 4.2 Implement `LevelDesignerViewModel` with `SavedStateHandle`: draft (saved as `LevelWriter` JSON), selected tool, dirty flag, raw text of numeric fields with per-field parse errors, derived `DesignStatus`, actions for every edit, new blank, copy from a bundled level, export (clears dirty) and test play (only when solvable)
- [ ] 4.3 View model JVM tests: initial blank design with id `bundled count + 1`, copy from bundled level, edits mark dirty, invalid number text keeps the draft, export calls the exporter with writer output and clears dirty, export and test play refused when invalid or unsolvable, state restored from a `SavedStateHandle`

## 5. Designer UI (app)

- [ ] 5.1 Build `DesignerGrid`: canvas that fits the whole grid, draws cells, blocked cells and endpoints with open sides using the game's drawing code, highlights error cells and draws the solver path faintly; maps taps and drags to cells for the selected tool
- [ ] 5.2 Build the control panel sections: grid size steppers (disabled at 4 and 50), tool selector (Start, Finish, Block), open side controls for start and finish, Clear blocked, id and name, flow, stack weights and visible size, rules, stars, status with error list, and Test play / Export / Start from bundled level actions
- [ ] 5.3 Build `LevelDesignerScreen` (landscape, game theme, safe-area insets) with back handling and the discard-changes confirmation dialog
- [ ] 5.4 Add strings and test tags; Compose previews for a blank, an invalid and an unsolvable design
- [ ] 5.5 Instrumented Compose tests: tap with Start tool moves the start, drag with Block tool blocks a row, steppers disabled at limits, invalid design disables Test play and Export and lists errors, back with changes shows the dialog

## 6. Welcome screen entry and navigation (app)

- [ ] 6.1 Add an optional `onOpenLevelDesigner: (() -> Unit)?` to `WelcomeScreen` and a 48dp corner icon button (tag `WelcomeScreenTags.LEVEL_DESIGNER`, content description "Level designer") composed only when it is non-null
- [ ] 6.2 In `MainActivity`, add `Screen.Designer` and `Screen.TestPlay(level)`, an activity-scoped `LevelDesignerViewModel`, pass the callback only when `FeatureFlags.levelDesigner` is on, make opening the designer a no-op otherwise, and save the current screen so a recreated activity returns to the designer
- [ ] 6.3 Test play renders `GameScreen` with `history = null` and `onNextLevel = null`, and its exit returns to `Screen.Designer` with the draft intact
- [ ] 6.4 Instrumented tests: welcome button shown and emits once with a callback, absent without one, Play/Settings/Exit unchanged; welcome → designer → test play → exit returns to the same design; a test play win leaves the history empty

## 7. Docs and verification

- [ ] 7.1 Add a "Creating levels with the designer" section to `docs/level-format.md` (debug only, export, where to save the file, run `:core:test`)
- [ ] 7.2 Run `./gradlew :core:test` and `./gradlew :app:testDebugUnitTest` and make sure they pass
- [ ] 7.3 Run the affected instrumented tests (welcome screen, designer, navigation) on the emulator and make sure they pass
- [ ] 7.4 Run `./gradlew :app:assembleRelease` and confirm the generated `BuildConfig.LEVEL_DESIGNER` is false and the welcome screen has no designer button in release
