## Context

Levels are hand-written `level_NNN.json` files in `app/src/main/assets/levels/`, parsed by `LevelParser`, checked by `LevelValidator` and proven winnable by `LevelSolver` (all in `core/.../level`). There is no way to write a `Level` back to JSON. The app navigates with a small `Screen` sealed interface in `MainActivity` (Welcome, Library, Game). `GameScreen(level, onExit, onNextLevel, history)` already plays any `Level` and records nothing when `history` is null. The only developer tool so far, the Pipe Lab, lives in the `debug` source set with its own launcher activity; the app has no feature flag mechanism and `buildConfig` is not enabled.

## Goals / Non-Goals

**Goals:**
- One build-time feature flag, `levelDesigner`, on in debug and off in release, read from one place.
- A level designer reached from a small button on the welcome screen, that edits every field of the level format, validates live, test plays and exports JSON.
- Editing rules (resize, endpoint moves, blocking) in `core` as pure functions so they are unit tested without Android.
- A `LevelWriter` whose output round-trips through `LevelParser`.

**Non-Goals:**
- Saving designs in the app, a list of drafts, or writing directly into `assets/` (assets are read-only at runtime).
- Placing pre-filled pipes, or any level field that the level format does not have.
- Runtime or remote feature flag toggling, or a settings screen for flags.
- Shipping the designer to players.

## Decisions

### Flag as a `BuildConfig` field, wrapped in `FeatureFlags`
`app/build.gradle.kts` enables `buildFeatures.buildConfig` and declares `buildConfigField("boolean", "LEVEL_DESIGNER", ...)`: `true` in `debug`, `false` in `release`. `app/.../FeatureFlags.kt` exposes `object FeatureFlags { val levelDesigner: Boolean = BuildConfig.LEVEL_DESIGNER }`. Composables never read `FeatureFlags` directly; `MainActivity` passes `onOpenLevelDesigner = if (FeatureFlags.levelDesigner) {...} else null` to `WelcomeScreen`, and the button is only composed for a non-null callback. That keeps the welcome screen testable with the flag either way, and the navigation guard sits next to the `Screen` switch.

*Alternative:* put the designer in the `debug` source set like the Pipe Lab. Rejected: the user asked for a feature flag, the designer has to be launched from the main activity's welcome screen and play through `GameScreen`, and a main-source-set screen can be covered by the normal unit and instrumented tests. The cost is that release builds contain the designer's code, unreachable. Because the flag is a `static final` constant, R8 removes the dead branch and the screen once minification is turned on; until then it is dead code behind a constant.

### Editing model in `core`: `LevelDraft`
`core/.../level/LevelDraft.kt` holds a `Level` plus pure operations that each return a new draft: `resize(columns, rows)`, `placeStart(cell)`, `placeFinish(cell)`, `setOpenSide(endpoint, side)`, `toggleBlocked(cell)`, `paintBlocked(cells, blocked)`, `clearBlocked()`, and field setters. It enforces the editing rules from the spec (clamp endpoints into the grid on shrink, drop out-of-grid blocked cells, never block an endpoint, endpoints never share a cell, placing an endpoint unblocks its cell) but it does **not** duplicate `LevelValidator`: a draft may be invalid (for example a start opening onto the edge), and validity is reported, not prevented. `LevelDraft.new(id)` builds the blank level from the spec. Grid size is clamped to 4..50 here too, so the UI cannot produce an out-of-range grid.

*Alternative:* keep editing logic in the view model. Rejected: project convention keeps rules in `core`, and these rules are the bulk of what needs testing.

### `LevelWriter` via a JSON tree, defaults omitted
`core/.../level/LevelWriter.kt` builds a `JsonObject` by hand (kotlinx.serialization's `buildJsonObject`) rather than encoding `LevelDto`, because `LevelDto` has nullable optional fields and default-omission logic per nested object would otherwise leak into the DTO. Each optional value is compared with `LevelDefaults` and left out when equal; empty `stack`/`rules`/`weights` objects are left out. Output uses a `Json { prettyPrint = true; prettyPrintIndent = "  " }` instance plus a trailing newline. Key order follows `docs/level-format.md`. A round-trip test runs every bundled level through parse → write → parse.

### Status = validator, then solver
A `DesignStatus` is derived (not stored) from the draft: `Invalid(errors)` when `LevelValidator.validate` reports errors, else `Unsolvable(reason)` or `Solvable(path)` from `LevelSolver`. The solver is a BFS over at most 2,500 cells, cheap enough to run on every edit on the main thread; the view model computes it in a `derivedStateOf`/`map` so it only reruns when the draft changes. Cell highlights come from mapping error fields (`start.*`, `finish.*`, `blocked[i]`) to positions in the draft. When solvable, the solver path is drawn faintly on the grid as a hint.

### Screen and grid
`app/.../ui/designer/`: `LevelDesignerScreen` (grid left, scrollable control panel right), `LevelDesignerViewModel`, `DesignerGrid`, `DesignerPanel` sections (Grid, Tools, Endpoints, Flow, Stack, Rules, Stars, Status). `DesignerGrid` is a Compose `Canvas` that reuses the game's cell, blocked-cell and endpoint drawing from `ui/game` and `ui/pipes` so a design looks like the game; it fits the whole grid in the available space (no pan/zoom needed for editing, since up to 50x50 still gives usable cells on a tablet; on a phone a 50x50 cell is small, see risks). Pointer input maps taps and drags to cells; a drag with the Block tool collects distinct cells and calls `paintBlocked` with the target state decided by the first cell. Numeric fields are text fields holding their raw text in UI state; only text that parses updates the draft, otherwise the field shows an error.

### Navigation and test play
`MainActivity`'s `Screen` gains `Designer` and `TestPlay(level)`. The `LevelDesignerViewModel` is activity-scoped (like the library view model) so the draft survives the trip to `TestPlay` and back. `TestPlay` renders `GameScreen(level, onExit = { screen = Screen.Designer }, onNextLevel = null, history = null)`. Opening the designer is a no-op when `FeatureFlags.levelDesigner` is false.

### Surviving process death
The view model takes a `SavedStateHandle` and stores the draft as `LevelWriter` JSON plus the tool and a dirty flag; on restore it parses the JSON back with `LevelParser`. The JSON is small (a 50x50 grid fully blocked is ~40 KB, well under the bundle limit in practice). The writer must accept invalid drafts for this, which it does since it does not validate. The `Screen` value itself is saved with `rememberSaveable` using a saver that maps `Designer`/`TestPlay` back to `Designer`.

### Export
`LevelExporter` (app) takes the JSON: puts it on the clipboard via `ClipboardManager` with label `level_NNN.json`, and starts `Intent.createChooser(Intent(ACTION_SEND) { type = "application/json"; EXTRA_TEXT; EXTRA_SUBJECT = "level_NNN.json" })`. It is behind an interface so the view model test checks the call without Android. File name formatting reuses `BundledLevels.path`.

### Welcome screen button
A 48dp round icon button in the top-right corner of the safe area (pencil/grid glyph drawn with the design system's colours, no new icon dependency), tagged `WelcomeScreenTags.LEVEL_DESIGNER`, content description "Level designer". It is placed in the outer `Box`, so the Play / Settings / Exit layout computed by `WelcomeLayout` is untouched.

## Risks / Trade-offs

- [Designer code ships in release APKs while minification is off] → It is unreachable behind a compile-time `false`; the release-flag test asserts `BuildConfig.LEVEL_DESIGNER` is false. Enabling R8 later removes it.
- [A 50x50 grid on a phone gives cells of ~6dp, too small to tap accurately] → Accept for a developer tool; drag painting helps. Pan/zoom via the game's `FieldViewport` can be added later if needed.
- [Writer and parser drift apart when the level format gains a field] → The round-trip test over bundled levels plus an "every optional field set" test fails when a field is not written.
- [Solver on every edit could jank on big grids] → BFS over ≤2,500 cells is sub-millisecond; recomputed only on draft changes, not on frames.
- [Exported id may collide with an existing level] → The blank level suggests `bundled count + 1`; duplicate ids are still caught by the existing set validation test when the file is added.

## Open Questions

- None blocking. A later change may add saving drafts or writing the file directly via the Storage Access Framework if share/clipboard turns out to be clumsy.
