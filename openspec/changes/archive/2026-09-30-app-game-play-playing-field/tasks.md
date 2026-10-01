## 1. Setup

- [x] 1.1 Check that `app-game-play-stack` and `app-game-play-hud` are merged into `development` (`GameScreen`, `GameSession`, `SectionDragState`, `SectionDropTarget`, `PlaceholderBoard`, `PipeSectionGlyph` exist). If not, stop and merge them first.

## 2. Core: board model

- [x] 2.1 Create the package `nl.hexmaster.leakout.core.board` with the sealed `Cell` (`Empty`, `Blocked`, `Start(openSide)`, `Finish(openSide)`, `Pipe(section, liquidEntered = false)`).
- [x] 2.2 Add the immutable `Board(grid, cells)` with `get` (empty for unknown positions), `with(position, cell)`, `markLiquidEntered(position)` and `Board.from(level)` (start, finish and blocked cells from the level).
- [x] 2.3 Unit test `Board`: `from(level)` puts start, finish and blocked cells in place and leaves the rest empty; `with` does not change the original; `markLiquidEntered` sets the flag on a pipe only.

## 3. Core: placement rule

- [x] 3.1 Add `PlacementCheck` (`Place`, `Replace`, `Rejected(reason)`) and `Placement.check(board, rules, at, nowMs, lockUntilMs)` in the order from the design: outside, locked, empty, fixed cells, liquid entered, replace not allowed, replace.
- [x] 3.2 Unit test `Placement.check`: empty → `Place`; blocked, start and finish → rejected; pipe without liquid → `Replace`; pipe with liquid → rejected; `allowReplace = false` → rejected; outside the grid → rejected; `nowMs` before `lockUntilMs` → rejected for every cell, and accepted again at `lockUntilMs`.
- [x] 3.3 Run `./gradlew :core:test` and check that everything passes.

## 4. App: game session

- [x] 4.1 Replace `GameSession.placed` with `board: Board` (from `Board.from(level)`) and add `replaceLockUntilMs`.
- [x] 4.2 Change `GameSession.place(section, at)` to use `Placement.check`: refuse while paused or when rejected; on `Place` or `Replace` write `Cell.Pipe(section)` and advance the stack; on `Replace` set `replaceLockUntilMs = elapsedMs + rules.replacePenaltyMs`. Add `canPlace(at)` that runs the same check.
- [x] 4.3 Make `restart()` rebuild the board and clear the lock, and add a restart counter the view state can key on.
- [x] 4.4 Unit test `GameSession`: a replacement swaps the section and starts the lock; the lock does not run out while paused (`tick` does nothing); the lock ends after `replacePenaltyMs` of ticks; a drop on an empty tile does not start the lock; a section with liquid cannot be replaced; restart clears the board, including sections with liquid.

## 5. App: viewport

- [x] 5.1 Add `FieldLayout` with `MIN_CELL = 40.dp` and `MAX_CELL = 120.dp`.
- [x] 5.2 Add the pure `FieldViewport(fieldSize, grid, cellPx, offset)` with `minCellPx`, `maxCellPx`, `initial(fieldSize, grid, start, density)`, `zoomBy(factor, focus)`, `panBy(delta)`, `cellAt(fieldPoint)` and `cellRect(position)`, as in the design.
- [x] 5.3 Unit test `FieldViewport`: 8 × 6 in 600 × 300 dp gives 50 dp cells, centred; a tiny grid is capped at 120 dp and cannot zoom; 16 × 12 in a 312 dp high field starts at 40 dp with the start cell visible; zoom stops at `minCellPx` and `maxCellPx`; zoom keeps the focus point on the same grid point; pan is clamped at every edge; an axis that fits stays centred and does not pan; zooming out after a pan to a corner re-clamps; `cellAt` maps correctly after a pan and a zoom, and returns `null` outside the grid and outside the field.

## 6. App: playing field

- [x] 6.1 Extract the drawing from `PipeSectionGlyph` into `DrawScope.drawPipeSection(section, rect)` and use it in both the stack glyph and the field.
- [x] 6.2 Add `PlayingField(board, viewport, onViewportChange, highlighted, enabled, modifier)`: a clipped `Box` with a `Canvas` that draws the visible cells (grid, blocked cells, start and finish with their open side, placed sections) and the highlight on top, plus per-cell test tags and semantics (for example `cell_3_4`, with a highlighted state).
- [x] 6.3 Add the gesture handler with `awaitEachGesture`, `calculateZoom`, `calculatePan` and `calculateCentroid`: one finger pans after the touch slop, two fingers zoom around the centroid, rotation is never read, and all input is ignored while `enabled` is false.
- [x] 6.4 Extend `SectionDropTarget` with `canPlace(section, position)`. `PlayingField` converts game-screen positions to field coordinates with its `LayoutCoordinates`, then to a cell with `cellAt`, and calls `session.canPlace` or `session.place`. Positions outside the visible field give no cell.
- [x] 6.5 Derive the highlighted cell from `dragState.dropPoint` with `derivedStateOf`, only when `canPlace` is true, and `null` when there is no drag.

## 7. App: game screen wiring

- [x] 7.1 Replace `PlaceholderBoard` with `PlayingField` in `GameScreen` (`Modifier.weight(1f).fillMaxHeight()` next to `StackPanel`, below the HUD), and delete `PlaceholderBoard`.
- [x] 7.2 Hold the viewport in Compose state in `GameScreen`, created with `FieldViewport.initial` once the field is measured, and keyed on the session's restart counter, so pause keeps it and restart resets it.
- [x] 7.3 Disable field gestures while a section drag is active or the game is paused.

## 8. Instrumented tests (emulator)

- [x] 8.1 Layout: the field starts right of the stack and below the HUD and reaches the right and bottom edges; an 8 × 6 level is shown completely and centred; a 16 × 12 level on a 360 dp tall screen shows 40 dp cells with the start cell visible and draws nothing over the HUD or the stack.
- [x] 8.2 Pan: on a 16 × 12 level, a swipe moves the level and stops at the edges; a short tap does not move it; on a level that fits, a swipe does not move it.
- [x] 8.3 Zoom: a pinch out makes cells larger and stops at 120 dp; a pinch in stops at the starting size; a two-finger twist does not rotate or move the level.
- [x] 8.4 Highlight: dragging over an empty tile highlights it; over a blocked, start or finish tile, over a section with liquid (set on the board), over the stack or the HUD, nothing is highlighted; moving to the next tile moves the highlight; letting go clears it.
- [x] 8.5 Drop and replace: dropping on a highlighted empty tile places the section; dropping on a placed section without liquid replaces it and advances the stack; dropping on a section with liquid is rejected and leaves the stack; right after a replacement nothing is highlighted and a drop is rejected; after the lock time a drop works again.
- [x] 8.6 Drag and view: dragging a section over the field does not pan it; after a pan and a zoom, a drop still lands on the tile under the drop point.
- [x] 8.7 Pause and restart: pausing during a drag clears the highlight; resume keeps the zoom and pan; restart resets the view and clears the board.
- [x] 8.8 Run `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` on the emulator and record the results. Result: core 157/157, app unit 150/150, instrumented 67/68. `GamePlayHudTest.backOpensDialogAndBackInDialogResumes` failed once in the full run (Espresso `pressBack` found the window without focus) after passing in the game-package run; accepted as done on request.

## 9. Wrap up

- [x] 9.1 Update `docs/project-plan.md` sections 1.2, 1.7 and 2.8: the board fits the level when it can and otherwise pans and zooms within 40 to 120 dp cells, it never rotates, a drop highlights the target tile, and a drop on an unfilled section replaces it.
- [x] 9.2 Run `openspec validate app-game-play-playing-field` and fix any issues.
