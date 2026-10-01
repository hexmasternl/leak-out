## Context

Three open changes come before this one and must merge first:
- `app-game-play-stack` adds `GameScreen`, the stack and drag and drop.
- `app-game-play-hud` adds the HUD and `GameSession`, which owns `score: Int`, `elapsedMs` (the game clock, which stops while paused), `paused` and `restart()`. The HUD shows `session.score` and already formats it with digit grouping.
- `app-game-play-playing-field` adds `Board` and `Cell` in `core.board` (`Cell.Pipe(section, liquidEntered)`), the `PlayingField` canvas with `FieldViewport` (pan, zoom, `cellRect(position)`), and a light overlay of `Box`es per visible cell for test tags and semantics.

There is no flow simulation yet. The plan (section 2.5) describes it: the liquid head leaves a cell when its `progress` reaches 1.0, marks the channel as filled and emits `SectionFilled`, then checks the next cell for a leak, a win or the next channel. The exit from a section is exactly the moment this change awards points. `core` has `PipeSection` (`H`, `V`, `LT`, `LB`, `RT`, `RB`, `X`, `RES`) and `Position` in `core.level`.

The plan's old scoring (section 1.9: +10 per section, +20 per reservoir, +50 extra for a double cross) is replaced by the values in the proposal.

## Goals / Non-Goals

**Goals:**
- One points table and one score keeper in `core`, unit tested on the JVM, that the flow change can call without knowing about points.
- A single entry point on `GameSession` for "the liquid exited the section on this tile", which updates the score and starts a popup.
- A popup on the tile that fades in, holds, fades out and follows pan, zoom, pause and restart, with its timing as a pure function that is unit tested.

**Non-Goals:**
- The flow simulation itself. Tests call the entry point directly.
- The time bonus for starting the flow early, star thresholds and best scores.
- An animated count-up of the HUD score. The HUD jumps to the new total.
- Sound for an award.
- Popups for the start and finish tiles, or a "level complete" score summary.

## Decisions

**1. Points table as a pure function in `core.score`.**
```kotlin
object Points {
    const val STRAIGHT = 10; const val CORNER = 15; const val RESERVOIR = 25
    const val CROSS_FIRST = 10; const val CROSS_SECOND = 50
    fun forExit(section: PipeSection, pass: Int): Int
}
```
`pass` is 1 for the first exit from that tile and 2 for the second. Only `X` looks at it. A `when` over the enum without an `else` makes the compiler fail when a new section is added without a value. *Other option:* a `points` property on `PipeSection`. It was not chosen because the cross needs two values and the level model should not carry game rules.

**2. `ScoreSheet` keeps the total and the passes per tile.**
```kotlin
data class ScoreSheet(val total: Int = 0, val passes: Map<Position, Int> = emptyMap()) {
    fun award(at: Position, section: PipeSection): Award   // Award(sheet: ScoreSheet, points: Int)
}
```
`award` bumps the pass count for `at`, looks up `Points.forExit(section, pass)` and returns the new sheet with the points. It is immutable like `Board`, so `restart()` is `ScoreSheet()`. The passes are kept here, not read from the board, because the board only has one `liquidEntered` flag per pipe. If the flow change later adds per-channel fill state to `Cell.Pipe`, the sheet still works and does not need it. *Other option:* derive the pass from the board's filled channels. It was not chosen because it ties scoring to a board model that is still changing, and it makes the order "mark filled, then score" matter.

**3. `GameSession.liquidExited(at: Position)` is the one entry point.**
- Refused while paused.
- Reads `board[at]`. Only `Cell.Pipe` scores; `Start`, `Finish` and anything else is ignored, so the flow change can call it for every cell it leaves without special cases.
- Replaces `sheet` with `award.sheet`, and adds `PointsPopup(id, at, points, startedAtMs = elapsedMs)` to `popups`.
- `score` becomes a read-only property `sheet.total`, so the HUD code does not change.
- `tick(dtMs)` removes popups with `elapsedMs - startedAtMs >= PopupTiming.TOTAL_MS` after moving the clock.
- `restart()` sets `sheet = ScoreSheet()` and `popups = emptyList()`.

The contract for the flow change: call `liquidExited(position)` once each time the head leaves a cell, after the cell's channel is marked and before the next cell is checked for a leak or a win. This gives the "last section before a leak still scores" rule for free. When `GameEngine` arrives, the engine's `Tick` handles this itself and adds the awards to `GameState`; `GameSession` then only mirrors them.

**4. Popup timing is a pure function in `app`.**
```kotlin
object PopupTiming { const val FADE_IN_MS = 150L; const val HOLD_MS = 500L; const val FADE_OUT_MS = 350L; const val TOTAL_MS = 1_000L }
data class PopupFrame(val alpha: Float, val scale: Float, val rise: Float)   // rise in tiles, 0 → 0.25
fun popupFrame(ageMs: Long): PopupFrame?                                      // null at or after TOTAL_MS
```
- Fade-in: alpha 0 → 1 and scale 0.8 → 1.0 with `FastOutSlowInEasing`.
- Hold: alpha 1, scale 1.
- Fade-out: alpha 1 → 0 with `FastOutLinearInEasing`.
- Rise: linear from 0 to 0.25 of a tile over the whole 1,000 ms.

The age is `session.elapsedMs - popup.startedAtMs`, so pause freezes the popup without extra code, and the frame loop from the HUD change already redraws while the game runs. *Other option:* `Animatable` per popup in a `LaunchedEffect`. It was not chosen because Compose animations run on the frame clock, not the game clock: they would keep fading during a pause and would need extra code to stop and resume them, and they cannot be tested without a device.

**5. Drawing in the `PlayingField` canvas, after the highlight.**
Popups are drawn last in the same `Canvas`, for each popup whose `cellRect` intersects the field, so they sit above the sections, the liquid and the highlight. The text is "+N" in `Fredoka` bold, with the font size at 40 % of `cellPx`, drawn with `drawText` from a remembered `TextMeasurer`: first as a 3 px ink outline (`Stroke`), then filled in white, in the cartoon style of the game title. The centre is the tile centre moved up by `rise × cellPx`; alpha and scale come from `popupFrame`. `elapsedMs` and `popups` are read inside the draw lambda only, so an award or a frame redraws the canvas without recomposing the grid. The canvas takes no pointer input of its own, so popups never block pan, zoom or the hover highlight.

**6. Test hook through the cell overlay.**
Canvas text cannot be found by UI tests. The cell overlay from the playing-field change gets a custom `SemanticsPropertyKey<List<String>>` `PointsPopupText`, set to the texts of the popups that are visible on that tile. It is not a content description, so screen readers do not read it. Instrumented tests use it to check that "+15" is on tile (3, 4), and that it is gone after 1,000 ms of ticks.

**7. No screen reader announcement per award.**
The HUD score keeps its content description ("Score 55"), which updates with every award. An announcement for each section would talk over the game every second. *Other option:* a polite live region on the HUD score. Left as an open question.

## Risks / Trade-offs

- [This change edits `GameSession`, `GameScreen` and `PlayingField` from three unmerged changes] → Merge `app-game-play-stack`, `app-game-play-hud` and `app-game-play-playing-field` first. The first task checks this.
- [Nothing awards points during play until the flow exists, so the feature cannot be seen in the app yet] → Unit tests and instrumented tests call `liquidExited` directly. The flow change gets a clear contract (decision 3) and a test that plays a short route and checks the score.
- [The flow change could call `liquidExited` twice for one exit, or skip it for the last section before a leak] → The contract is written in the design and in `GameSession`'s KDoc, and the flow change's tests check the score after a leak and after a win.
- [Many popups at once at high speed] → Speeds are at most about 2 cells per second, so at most two or three popups are alive. Each is one text layout; `TextMeasurer` caches layouts for the same text and size.
- [Readability of white text on a light liquid color] → The ink outline keeps the text readable on any background. Checked in the instrumented screenshot on a filled section.
- [The new values change the balance the plan assumed for star thresholds] → Stars are not implemented yet. The level files' `stars` values are tuned when stars are built.

## Migration Plan

Nothing is stored or migrated. To roll back, revert the change: the score stays at 0 and no popups are drawn.

## Open Questions

- Should the HUD score count up to the new total instead of jumping? It is a small polish item and does not change the rules.
- Should a screen reader hear each award through a polite live region on the score? Not for now; see decision 7.
- Should the popup for the second pass of a cross look different (larger, or another color) to reward the move? It uses the same style for now.
