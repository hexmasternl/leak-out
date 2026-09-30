## 1. Setup

- [x] 1.1 Check that `app-game-play-stack`, `app-game-play-hud` and `app-game-play-playing-field` are merged into `development` (`GameScreen`, `GameSession` with `score` and `elapsedMs`, `Board` and `Cell` in `core.board`, `PlayingField` with `FieldViewport` and the cell overlay exist). If not, stop and merge them first.

## 2. Core: points rules

- [x] 2.1 Create the package `nl.hexmaster.leakout.core.score` with `Points` (`STRAIGHT = 10`, `CORNER = 15`, `RESERVOIR = 25`, `CROSS_FIRST = 10`, `CROSS_SECOND = 50`) and `forExit(section, pass)` as an exhaustive `when` without `else`.
- [x] 2.2 Add the immutable `ScoreSheet(total, passes)` and `Award(sheet, points)` with `ScoreSheet.award(at, section)`, which counts the pass for the tile and adds the points.
- [x] 2.3 Unit test `Points`: `H` and `V` give 10; `LT`, `LB`, `RT` and `RB` give 15; `RES` gives 25; `X` gives 10 for pass 1 and 50 for pass 2.
- [x] 2.4 Unit test `ScoreSheet`: a new sheet is 0; a route of two straights, three corners and a reservoir totals 90; two exits from the same cross give 10 then 50; exits from two different crosses give 10 each; `award` does not change the original sheet.
- [x] 2.5 Run `./gradlew :core:test` and check that everything passes.

## 3. App: game session

- [ ] 3.1 Add `PointsPopup(id, position, points, startedAtMs)` and `PopupTiming` (`FADE_IN_MS = 150`, `HOLD_MS = 500`, `FADE_OUT_MS = 350`, `TOTAL_MS = 1000`).
- [ ] 3.2 Give `GameSession` a `sheet: ScoreSheet` and `popups: List<PointsPopup>` in Compose state, and turn `score` into a read-only property that returns `sheet.total`.
- [ ] 3.3 Add `GameSession.liquidExited(at)`: refused while paused; ignores every cell that is not `Cell.Pipe`; otherwise awards through the sheet and adds a popup with `startedAtMs = elapsedMs`. Write the flow contract from the design (once per exit, after marking the channel, before the leak or win check) in its KDoc.
- [ ] 3.4 Make `tick(dtMs)` drop popups whose age has reached `PopupTiming.TOTAL_MS`, and make `restart()` reset the sheet and clear the popups.
- [ ] 3.5 Unit test `GameSession`: exiting a straight on a placed pipe adds 10 and one popup at that tile; exiting the start or finish tile changes nothing; the second exit from a cross adds 50; nothing changes while paused; ticks while paused do not age or drop popups; a popup is dropped after 1,000 ms of ticks; restart sets the score to 0, clears popups, and the next cross exit on the same tile is a first pass again.

## 4. App: popup animation

- [ ] 4.1 Add the pure `PopupFrame(alpha, scale, rise)` and `popupFrame(ageMs)` with the easing from the design, returning `null` at or after `TOTAL_MS`.
- [ ] 4.2 Unit test `popupFrame`: at 0 ms alpha 0 and scale 0.8; at 150 ms alpha 1 and scale 1; alpha stays 1 through 650 ms; alpha falls towards 0 during the fade-out; rise grows from 0 to 0.25 and never goes down; `null` at 1,000 ms and later; alpha and scale are always within their ranges.
- [ ] 4.3 Add `PointsFormat.text(points)` that returns "+10", "+15", "+25" or "+50", and unit test it.

## 5. App: drawing on the playing field

- [ ] 5.1 Pass `session.popups` and a `() -> Long` for `elapsedMs` to `PlayingField`, and read both only inside the draw lambda.
- [ ] 5.2 Draw each visible popup last in the canvas, after the highlight: "+N" in `Fredoka` bold at 40 % of `cellPx`, an ink outline then a white fill, centred on the tile and moved up by `rise × cellPx`, with the frame's alpha and scale. Skip popups whose tile is outside the field.
- [ ] 5.3 Add the `PointsPopupText` semantics property to the cell overlay with the texts of the visible popups on that tile. Do not add it to the content description.
- [ ] 5.4 Check that the canvas and the popups add no pointer input, so pan, zoom and the hover highlight work the same on a tile with a popup.

## 6. Tests and docs

- [ ] 6.1 Instrumented test: place a corner, call `session.liquidExited` on its tile, and check that the HUD shows 15 and the tile shows "+15"; after 1,000 ms of game time the popup is gone and the HUD still shows 15.
- [ ] 6.2 Instrumented test: two exits 300 ms apart on two tiles show both popups at the same time, and the first one is gone 300 ms before the second.
- [ ] 6.3 Instrumented test: pause while a popup is shown, wait, resume, and check that it is still visible and disappears after the rest of its 1,000 ms of play; restart removes all popups and the HUD shows 0.
- [ ] 6.4 Instrumented test: pan the field while a popup is shown and check that it stays on its tile; start a pan on a tile with a popup and check that the field pans.
- [ ] 6.5 Update `docs/project-plan.md` section 1.9 with the new points (10, 15, 25, cross 10 then 50, awarded on exit, none for start and finish, the last section before a leak still scores) and mention the popup in section 2.8.
- [ ] 6.6 Run `./gradlew test` and the instrumented tests on the emulator, and check that everything passes.
