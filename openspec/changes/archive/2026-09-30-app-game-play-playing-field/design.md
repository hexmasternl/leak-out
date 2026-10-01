## Context

This change builds on two open changes and must merge after both:
- `app-game-play-stack` adds `GameScreen`, `StackPanel`, `PipeSectionGlyph`, `SectionDragState` (dragged section, finger position in game-screen coordinates, drop point at the copy's center) and the `SectionDropTarget` contract (`tryPlace(section, position): Boolean`). It uses a stand-in `PlaceholderBoard` that always fits the whole grid and accepts only empty cells. Its design already left `canPlace(section, position)` open for a hover preview.
- `app-game-play-hud` puts a 48 dp HUD on top and adds `GameSession`, which owns the stack, `placed: Map<Position, PipeSection>`, score, `elapsedMs`, `paused` and `restart()`. Pausing cancels a drag through `SectionDragState.cancel()`.

`core` has the level model: `Level` with `grid: GridSize`, `start`/`finish: Endpoint`, `blocked`, and `rules: PlacementRules(allowReplace, replacePenaltyMs)`. There is no `Board`, `Cell` or flow yet. The bundled levels grow from 8 × 6 to 16 × 12 (commit `bc1300e`). On the smallest landscape phone (360 dp high) the field is about 312 dp high, so a 16 × 12 fit gives 26 dp cells: too small to read or hit with a finger. The plan (section 2.8) fits the whole grid with `min(width / columns, height / rows)`, which no longer works.

## Goals / Non-Goals

**Goals:**
- A real `PlayingField` that replaces `PlaceholderBoard`, fills the space next to the stack and below the HUD, and fits, pans and zooms the level within fixed cell-size limits, without rotation.
- One placement rule in `core` that both the hover highlight and the drop use, so they can never disagree.
- Replacing unfilled sections, the permanent lock on sections the liquid has entered, and the level's replace lock.
- Viewport math and placement rules that are unit tested on the JVM; gestures and highlight tested on the emulator.

**Non-Goals:**
- The flow simulation. Nothing sets "liquid has entered" during play yet; tests set it directly on the board.
- `PipeDrawer` and liquid drawing. Placed sections still use `PipeSectionGlyph`.
- Auto-panning while a section is dragged near the field edge, double-tap to zoom, fling or momentum panning, and keeping the zoom between attempts.
- A red "not allowed" highlight. Tiles that do not accept a section simply stay unhighlighted.

## Decisions

**1. `Board` and `Cell` in `core`, immutable.**
New package `nl.hexmaster.leakout.core.board`:
```kotlin
sealed interface Cell {
    data object Empty : Cell
    data object Blocked : Cell
    data class Start(val openSide: Side) : Cell
    data class Finish(val openSide: Side) : Cell
    data class Pipe(val section: PipeSection, val liquidEntered: Boolean = false) : Cell
}
class Board(val grid: GridSize, cells: Map<Position, Cell>) {
    operator fun get(p: Position): Cell        // Empty for positions without an entry
    fun with(p: Position, cell: Cell): Board   // immutable copy
    fun markLiquidEntered(p: Position): Board  // for the flow change and tests
    companion object { fun from(level: Level): Board }
}
```
Only non-empty cells are stored. `liquidEntered` is one flag, not per-channel fill state: the flow change can refine `Pipe` (for the cross's two channels) without touching the placement rule, because the rule only asks "has any liquid entered". This matches the plan's `Board` (section 2.6) and moves forward the part the field needs. *Other option:* keep `placed: Map<Position, PipeSection>` in `GameSession`. It was not chosen because the fill state, start, finish and blocked cells would then live in four places.

**2. One placement rule: `Placement.check`.**
```kotlin
sealed interface PlacementCheck { data object Place; data object Replace; data class Rejected(val reason: Reason) }
object Placement {
    fun check(board: Board, rules: PlacementRules, at: Position, nowMs: Long, lockUntilMs: Long): PlacementCheck
}
```
Order: outside grid → `Rejected(OUTSIDE)`; `nowMs < lockUntilMs` → `Rejected(LOCKED)`; `Empty` → `Place`; `Blocked`/`Start`/`Finish` → `Rejected(FIXED)`; `Pipe` with `liquidEntered` → `Rejected(FILLED)`; `Pipe` and `!allowReplace` → `Rejected(REPLACE_NOT_ALLOWED)`; else `Replace`. The section being dragged does not change the result, so the rule does not take it. Pause is checked in `GameSession`, not here. `GameSession.place(section, at)` calls the check, writes the cell, advances the stack, and on `Replace` sets `replaceLockUntilMs = elapsedMs + rules.replacePenaltyMs`. Because the lock uses `elapsedMs`, which does not move while paused, the lock does not run out during a pause. `restart()` builds a new `Board.from(level)` and clears the lock. When `GameEngine` arrives, `Place` calls the same `Placement.check`.

**3. `FieldViewport` is a pure value in `app`, tested on the JVM.**
It works in pixels, with the density passed in, like `StackLayout`:
```kotlin
data class FieldViewport(val fieldSize: Size, val grid: GridSize, val cellPx: Float, val offset: Offset) {
    val minCellPx: Float; val maxCellPx: Float
    fun zoomBy(factor: Float, focus: Offset): FieldViewport   // clamps cell size, keeps focus fixed, clamps offset
    fun panBy(delta: Offset): FieldViewport                   // clamps offset
    fun cellAt(fieldPoint: Offset): Position?                 // null outside the visible field or the grid
    fun cellRect(p: Position): Rect
    companion object { fun initial(fieldSize: Size, grid: GridSize, start: Position, density: Density): FieldViewport }
}
```
- Limits: `MIN_CELL = 40.dp`, `MAX_CELL = 120.dp`. `fit = min(width / columns, height / rows)`. `minCellPx = fit.coerceIn(MIN_CELL, MAX_CELL)`, `maxCellPx = MAX_CELL`. So zooming out stops as soon as the whole level is visible (or at 40 dp), and a small level on a big screen cannot zoom at all. 40 dp keeps a cell close to the 48 dp touch target while letting about 7 rows fit on a 312 dp field. The limits are constants in `FieldLayout` so they can be tuned in one place.
- Offset is the top-left of the grid in field coordinates. Per axis: if the content is smaller than the field, the offset is fixed at the centring value; otherwise it is clamped to `[field - content, 0]`.
- `initial` uses `minCellPx`. If the grid fits, it is centred; if not, the offset centres the start cell and is then clamped.
- `zoomBy` keeps the grid point under `focus` in place: `newOffset = focus - (focus - offset) * (newCell / oldCell)`, then clamps.

*Other option:* `Modifier.graphicsLayer` scaling plus `transformable`. It was not chosen because hit testing, clamping and the minimum cell size are then spread over Compose state, and the math is not testable on the JVM.

**4. Gestures: a custom `pointerInput` that ignores rotation.**
`PlayingField` uses `awaitEachGesture` with `calculateZoom()` and `calculatePan()` from `androidx.compose.foundation.gestures` and never calls `calculateRotation()`, so a twist does nothing. One finger pans after the touch slop; two fingers zoom around `calculateCentroid()` and pan with the centroid. The gesture is skipped completely while `dragState.isDragging` or while paused. `detectTransformGestures` was not chosen because it reports rotation and starts panning on the first move without the slop check we want between taps and pans. The viewport is held in plain Compose state inside `GameScreen` (not saved across process death), keyed on the session's restart counter, so pause keeps it and restart resets it.

**5. Drawing: one `Canvas`, clipped.**
`PlayingField` is a `Box(Modifier.clipToBounds())` with a `Canvas` that draws only the cells whose rect intersects the field (at most about 20 × 8 at 40 dp): empty cells as the grid, blocked cells as walls, start and finish with a stub on the open side, and placed sections with the `PipeSectionGlyph` drawing code extracted into a `DrawScope.drawPipeSection(section, rect)` function so the stack and the field share it. The highlight is a rounded rectangle in the theme accent color with a thicker border, drawn after the cells. Test tags and semantics are added per visible cell through a light overlay of `Box`es, so instrumented tests can find "cell 3,4" and read whether it is highlighted.

**6. Highlight and drop share the drop target.**
`SectionDropTarget` gets:
```kotlin
fun canPlace(section: PipeSection, position: Offset): Boolean
```
`position` is still in game-screen coordinates. `PlayingField` converts it to field coordinates with its `LayoutCoordinates` (`onGloballyPositioned`), then `viewport.cellAt(...)`, then `Placement.check`. `tryPlace` does the same conversion and calls `session.place`. The hovered cell is derived state: `derivedStateOf { dragState.dropPoint?.let { target.hoverCell(it) } }`, which returns the cell only when `canPlace` is true. It becomes `null` when the drag ends, is cancelled or the game pauses, because `dropPoint` is then `null`. Since `canPlace` and `tryPlace` run the same function on the same position, the highlight and the drop result always agree.

**7. Game screen wiring.**
`GameScreen` swaps `PlaceholderBoard` for `PlayingField(session.board, viewport, dragState, …)` in the `Row` next to `StackPanel`, with `Modifier.weight(1f).fillMaxHeight()`. `PlaceholderBoard` is deleted. `GameSession.placed` becomes `GameSession.board`; HUD code does not read it, so the HUD is not affected.

## Risks / Trade-offs

- [This change edits files from two unmerged changes (`GameScreen`, `GameSession`, `SectionDropTarget`)] → Merge `app-game-play-stack` and `app-game-play-hud` first. The first task checks this.
- [A pan and the start of a section drag can race when the finger is near the stack edge] → They live on different composables: the section drag starts only on the stack's next slot, the pan only inside the field. While a section drag runs, the field ignores all pointer input.
- [A 40 dp minimum on a 16 × 12 level means the player must pan to reach far tiles before dragging] → Accepted for now: the player pans first, then drags. Auto-pan at the edge during a drag is an open question.
- [Coordinate mix-ups between the game screen, the field and the grid] → All drag positions stay in game-screen coordinates; the field converts once, in one function, which has unit tests for an offset and zoomed viewport. Instrumented tests drop on a known cell after a pan and after a zoom.
- [Drawing many cells each frame when zoomed out] → Only visible cells are drawn, and the highlight is derived state, so a hover change redraws but does not recompose the grid.
- [The single `liquidEntered` flag is too coarse for the cross section] → The placement rule only needs "any liquid". The flow change can add per-channel state inside `Cell.Pipe` and keep `liquidEntered` as a derived property.

## Migration Plan

Nothing is stored or migrated. To roll back, revert the change: the game screen goes back to `PlaceholderBoard` and the map of placed sections.

## Open Questions

- Should the field auto-pan when a dragged section is held near its edge? This would make large levels easier. It can be added in `PlayingField` later without changing the placement rule.
- Should a tile that refuses the section show a red "not allowed" tint instead of nothing? The user asked for no highlight; a red tint is left to the polish milestone.
- Should the replace lock be shown (for example the replaced tile flashes), as the plan's milestone 2 suggests? Left to the polish milestone.
