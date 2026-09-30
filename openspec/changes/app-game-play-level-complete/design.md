## Context

The open game play changes build the game screen piece by piece and this change comes last in that row:

- `app-game-play-stack` adds `GameScreen`, `StackPanel`, `SectionDragState` and drag and drop.
- `app-game-play-hud` adds `GameHud` with `HudStars`, `PauseDialog` and `GameSession`, the single owner of the running game (`paused`, `elapsedMs`, `tick`, `pause`, `resume`, `restart`), the frame loop that calls `tick(dt)` while not paused, the `screen` state in `MainActivity` and the `onExit` callback. `GameSession.stars` is `level.stars.starsFor(score)`.
- `app-game-play-playing-field` adds `Board` and `Cell` in `core.board` (`Cell.Pipe(section, liquidEntered)`), `PlayingField` with `FieldViewport` and the cell overlay for test semantics.
- `app-game-play-fast-forward` adds `PipeSection.exitFor`, `RouteTracer` and the fast forward button.
- `app-game-play-points` adds `ScoreSheet`, `GameSession.liquidExited(at)` and the points popups, all driven by the game clock (`elapsedMs`), with pure `popupFrame(ageMs)` timing.
- `app-game-play-level-lost` adds `FlowStep.after(...)` in `core.flow`, which returns `Enter`, `Finish` or `Leak`, and the `Outcome` (`Playing`, `Lost(at, side, atMs)`) on `GameSession` with `isStopped`, `liquidLeaked`, `skipLeakEffect`, the leak effect on the field and `LevelLostDialog`.

There is still no flow simulation. This change supplies what happens on `FlowStep.Finish`. `core` has `Level.stars: StarBenchmarks` with `starsFor(points): Int` (0 to 3) and the bundled levels carry `one`, `two` and `three` thresholds.

The design system (`docs/design-system.md` 5.6, 7.3 and 8) describes the win: the finish ring flashes, the board dims, the dialog pops in, the stars pop in with a stagger in `brass.300`, missed stars stay muted, and the primary action on the win dialog is a success-green "Next level". The existing dialogs (`PauseDialog`, `LevelLostDialog`) set the implemented style: a Compose `Dialog` with the design-system card and `CartoonButton`s.

## Goals / Non-Goals

**Goals:**
- One pure rule in `core` for the final score of a won attempt: unused sections, penalty, total and stars, unit tested on the JVM.
- A won state in `GameSession` that stops the game exactly like the lost state, with one entry point for the flow change.
- A level complete dialog whose score and star animations are a pure function of the game clock, so they are unit tested, freeze in the background and can be skipped.
- The unused sections disappearing from the field in step with the penalty, driven by the same clock.
- Next level, Restart and Exit that are disabled until the animation is done, and correct Back, tap-outside and background behaviour.

**Non-Goals:**
- The flow simulation and the liquid filling the finish tile. Tests call the entry point directly.
- Recording the result in the level history, showing "new best", and unlocking the next level. Those are wired by `app-game-level-history` and `app-game-level-library`.
- Sound, haptics and the brass particle burst on a star.
- Skipping the animation with a tap. See the open questions.
- A level select button on the dialog. Exit goes to the main menu, like the lost dialog.

## Decisions

**1. `FinalScore` is a pure value in `core.score`.**
```kotlin
object Points { /* existing */ const val UNUSED_SECTION_PENALTY = 3 }

data class FinalScore(val earned: Int, val unusedSections: List<Position>, val total: Int) {
    val unusedCount: Int get() = unusedSections.size
    val penalty: Int get() = unusedCount * Points.UNUSED_SECTION_PENALTY
    companion object {
        fun of(earned: Int, board: Board): FinalScore
    }
}
```
`of` walks the board once and collects every `Cell.Pipe` with `liquidEntered == false`, in row-major order (top-left to bottom-right), and sets `total = (earned - penalty).coerceAtLeast(0)`. Stars are not stored on it: `level.stars.starsFor(total)` gives them, the same function the HUD uses, so the HUD stars and the dialog stars can only differ by the penalty. `liquidEntered` is the right flag because a section the liquid entered was on the route, even when the flow later left it through a cross's other channel. *Other option:* count the unused sections from `ScoreSheet.passes`. It was not chosen because the sheet only knows sections the liquid exited, not where the liquid is, and because the section list is needed anyway to remove them from the field.

**2. The win is one more `Outcome` on `GameSession`.**
`Outcome.Won(atMs: Long)` joins `Playing` and `Lost`. `liquidReachedFinish()`:
- ignored while `isStopped` (already won, lost or paused), so a level ends at most once;
- sets `outcome = Won(atMs = elapsedMs)` and `finalScore = FinalScore.of(sheet.total, board)`, computed once because the board cannot change after the win;
- `isStopped` becomes `paused || outcome !is Playing` and `pause()` does nothing when `outcome !is Playing`, so every place that stops on a loss also stops on a win: `place`, `canPlace`, `liquidExited`, `StackPanel.enabled`, the cog wheel, the fast forward button and the drag cancel in `GameScreen`.

The flow contract in the KDoc, extending the one from the lost change: when the liquid leaves a tile, the flow calls `liquidExited(at)` first (points), then `FlowStep.after`, and on `Finish` calls `liquidReachedFinish()`, on `Leak` calls `liquidLeaked(at, side)`. *Other option:* a `GameSession.flowStepped(result)` that takes the `FlowStep.Result`. It was not chosen because the flow change should stay free to decide how it steps, and the two entry points mirror the two terminal results one to one.

**3. The whole celebration runs on the game clock through one pure timeline.**
After the win, `tick(dtMs)` keeps adding to `elapsedMs` (as after a loss) and `winAgeMs = elapsedMs - Won.atMs`. In `app`, `WinTally` turns the age into what the screen shows:
```kotlin
object WinTiming {
    const val FINISH_EFFECT_MS = 1_000L   // finish flash, dialog appears after
    const val EARNED_HOLD_MS = 500L       // earned row shown, before the penalty starts
    const val PENALTY_STEP_MS = 120L      // per unused section, phase capped at PENALTY_MAX_MS
    const val PENALTY_MAX_MS = 2_000L
    const val TOTAL_COUNT_MS = 800L       // 0 to total
    const val STAR_STAGGER_MS = 300L      // between two stars lighting up
    const val STAR_POP_MS = 320L          // motion.pop
}
data class WinFrame(
    val sectionsRemoved: Int, val penaltyShown: Int, val totalShown: Int,
    val starsLit: Int, val starScales: List<Float>, val buttonsEnabled: Boolean,
)
fun winFrame(dialogAgeMs: Long, score: FinalScore, stars: Int): WinFrame
```
`dialogAgeMs = winAgeMs - FINISH_EFFECT_MS`. The phases follow each other with no gaps:
- **Earned** (0 to `EARNED_HOLD_MS`): earned points shown, penalty 0, total 0, no sections removed.
- **Penalty**: step = `min(PENALTY_STEP_MS, PENALTY_MAX_MS / unusedCount)`; at step *k* the frame has `sectionsRemoved = k` and `penaltyShown = k * 3`. With no unused sections the phase takes 0 ms.
- **Total** (`TOTAL_COUNT_MS`): `totalShown` runs from 0 to `total` with an ease-out curve so it starts fast and lands softly. The final value is exact at the end of the phase.
- **Stars**: star *i* (0-based, only earned stars) lights up at `i * STAR_STAGGER_MS` and its scale follows a pop curve from 1.0 up to 1.35 and back to 1.0 over `STAR_POP_MS`. Missed stars stay grey at scale 1.
- **Done**: `buttonsEnabled` is true from the end of the last star's pop (or right after the total phase when no star is earned).

`totalShown` is monotonic and every phase's values are exact at its end, which the unit tests check. Using the game clock keeps this change consistent with the popups and the leak effect: the animation is testable by calling `tick`, it freezes when the frame loop stops, and `skipWinAnimation()` (decision 6) can jump to the end. *Other option:* one `Animatable` per row and per star in `LaunchedEffect`s. It was not chosen for the same reasons as in the points change: it cannot be tested on the JVM, it keeps running in the background, and sequencing five animations across the dialog and the field would need a coroutine choreography that a single timeline replaces. The star stagger is 300 ms, wider than the design system's 80 ms, because the request is a reveal one star at a time, not a group pop.

**4. `tick` removes the unused sections from the board in step with the penalty.**
`GameSession` keeps `sectionsRemoved: Int`. After moving the clock, `tick` computes the frame's `sectionsRemoved` and, while its own count is behind, replaces the next position of `finalScore.unusedSections` on the board with `Cell.Empty` and bumps the count. The board stays the single truth of what the field shows: `PlayingField` draws the new board and the section is gone. The route stays on the field, so the player sees the pipe that won behind the dialog. Because it happens in `tick`, the field and the penalty row change in the same frame. Restart resets the count with everything else. *Other option:* let `PlayingField` hide sections whose index is below the frame's count. It was not chosen because the field would then need the unused list and the timeline, and the board would lie about its contents.

**5. `LevelCompleteDialog` follows the lost dialog, with the design system's win layout.**
`LevelCompleteDialog(levelId, score: FinalScore, stars: Int, frame: WinFrame, hasNextLevel, onNextLevel, onRestart, onExit)`, a `Dialog` with `dismissOnClickOutside = false`, `dismissOnBackPress = frame.buttonsEnabled` and `onDismissRequest = onExit`. The card holds:
- the eyebrow "LEVEL COMPLETE" in the success colour and the title "Level complete!" in Fredoka, with the line "You kept the water flowing.";
- `WinStars(lit = frame.starsLit, scales = frame.starScales)` (decision 7);
- three score rows, right-aligned numbers formatted with `HudFormat.score`: "Pipe points" `earned`, "Unused pipes" `-penaltyShown` (the row shows "-0" until the penalty starts), a divider, and "Total" `totalShown` in a larger numeric style;
- the buttons, always present: **Next level** as a success-green `CartoonButton` (`enabled = frame.buttonsEnabled && hasNextLevel`), **Restart** as the primary `CartoonButton` and **Exit** in the error-red style (both `enabled = frame.buttonsEnabled`). Disabled buttons use the existing `DisabledFill` and `DisabledShadow` look.

The card gets `Modifier.semantics { paneTitle = "Level complete" }` so a screen reader announces the win, and the score rows are read as "Pipe points 120", "Unused pipes minus 12", "Total 108". The dialog reads `session.elapsedMs` through the frame, so it recomposes each frame while the animation runs and settles once `buttonsEnabled` is reached. *Other option:* a `Dialog` only for the buttons and the score drawn on the field. It was not chosen because the lost dialog already set the pattern and the score needs text layout, not canvas drawing.

**6. Back, tapping outside and the background.**
- Tapping outside does nothing (`dismissOnClickOutside = false`).
- Back during the finish effect and during the animation does nothing: the `GameScreen` `BackHandler` that swallows Back while lost is widened to `outcome !is Playing && !buttonsEnabled`, and the dialog's `dismissOnBackPress` is false until the buttons are enabled. Once enabled, Back is Exit, like the lost dialog.
- `ON_STOP` while won calls `skipWinAnimation()`, which moves `Won.atMs` back so that the age is at least the timeline's total length, then runs the section removal to the end. When the player returns, the dialog shows the final numbers, all earned stars lit and the buttons enabled, and the pause dialog does not open because `pause()` is refused.
- `showWinDialog = winAgeMs >= FINISH_EFFECT_MS`, like `showLostDialog`.

**7. `WinStars` reuses the HUD star drawing with an external pop.**
`HudStars` draws a star from `ic_star.xml`, tinted `TextMuted` or `Brass300` with the brass glow behind an earned star, and pops it with `animateFloatAsState`. Its drawing is extracted into `StarIcon(earned, scale, size, modifier)`; `HudStars` keeps its own animated scale, and `WinStars(lit, scales)` lays out three 40 dp `StarIcon`s 12 dp apart with the scale from the frame. Each star gets a test tag `win_star_1` to `win_star_3` and the existing `StarEarned` semantics property, and the row has the content description "%1$d of 3 stars". *Other option:* a separate star drawing for the dialog. It was not chosen because the HUD design already promised reuse and two drawings would drift.

**8. The finish flash on `PlayingField`.**
`PlayingField` gets a `() -> Long?` for `winAgeMs` (null while not won), read only inside the draw lambda like the leak effect. A pure `finishFlashFrame(ageMs): FinishFlashFrame(ringAlpha, ringScale)` gives a success-green ring on the finish tile that flashes solid and swells to 1.3 of the tile over `FINISH_EFFECT_MS`, then settles to a steady solid ring so the finish stays marked behind the dimmed dialog. The drawing is in tile coordinates and follows pan and zoom. *Other option:* no effect, dialog right away. It was not chosen because the lost side has its 1 second, and the plan and the design system both describe the finish flashing before the dialog.

**9. Next level through a callback, resolved in `MainActivity`.**
`GameScreen(level, onExit, onNextLevel: (() -> Unit)?, random)`. `MainActivity` looks up the bundled level with id `level.id + 1` with the loader the stack change added for level 1, and passes `null` when there is none. Next level sets `screen = Game(nextLevel)`; because `GameSession` and the viewport are remembered on the level, the new level starts fresh. `hasNextLevel = onNextLevel != null`. The level library change replaces this lookup with its own catalogue and unlock rule and keeps the callback. *Other option:* have `GameScreen` load the next level itself. It was not chosen because the screen does not read assets and should not decide which level is next.

**10. Success-green `CartoonButton` style.**
`CartoonButton` has the primary and the error-red styles. A success style is added with `SuccessGreen` on top and a new `SuccessGreenDark` token for the bottom shadow, the same way `ErrorRedDark` was added for the danger button. No hex value lives in the composable. `CartoonButton` has no disabled state yet, so it gets `enabled: Boolean = true`: disabled, it draws `DisabledFill` and `DisabledShadow`, does not press and is not clickable.

**11. Adapted to the game engine (supersedes the `GameSession` parts of decisions 2, 4 and 6).**
`app-game-play-game-engine` merged after this design was written. It moved every piece of game data into an immutable `GameState` in `core.engine`, changed only by `GameEngine.handle(state, command)`, and it runs the flow itself: on `FlowStep.Finish` it already sets `Outcome.Won(atMs)` after awarding the last section's points, and `isStopped`, `Pause`, `Place` and fast forward already treat a win like a loss. `GameSession` is a thin Compose holder with no data of its own. So:
- There is no `liquidReachedFinish()`. The engine reports the win, and the flow contract of decision 2 is the engine's own.
- `GameState` gets `finalScore: FinalScore?`, set by `FlowSimulation` on `Finish` from `sheet.total` and the board, and `sectionsRemoved: Int`, both reset by `GameEngine.start` (so by Restart and Next level). `finalStars` is derived: `level.stars.starsFor(finalScore.total)`.
- The unused sections are removed with a new `GameCommand.RemoveUnused(upTo)`. The engine refuses it unless the level is won and `upTo` is above `sectionsRemoved`, and removes the next unused sections in row-major order up to `min(upTo, unusedCount)`. The timeline stays in `app`: after every accepted command while won, `GameSession.send` also sends `RemoveUnused(winFrame.sectionsRemoved)`, so each tick removes the sections in step with the penalty row, as decision 4 intends. The board in the state stays the single truth of what the field shows.
- `skipWinAnimation()` ticks through the rest of the finish effect and the tally (after a win a tick only moves the clock), which removes every remaining section, the same way `skipLeakEffect()` works.

*Other options:* move `WinTiming` into `core` so a `Tick` removes the sections itself. It was not chosen because animation timing belongs to the screen. Or hide the sections on the field only, which decision 4 already rejected.

## Risks / Trade-offs

- [This change edits `GameSession`, `GameScreen`, `PlayingField`, `HudStars`, `CartoonButton` and `MainActivity` from unmerged changes] → Merge the six earlier changes first; the first task checks this. The edits widen existing checks (`isStopped`, `pause`, Back) rather than adding new ones.
- [The stars can be 0 on a won level, while `game-level-history` rejects a record with 0 stars] → This change shows what `starsFor(total)` says, which is what the HUD showed during play. The history change resolves the mismatch when it wires recording, either by recording at least 1 star or by changing its rule. Noted in the open questions.
- [The flow change might call `liquidReachedFinish` before `liquidExited` for the last section] → The contract is in the KDoc and a unit test checks that a corner then finish gives 15 more points and a win.
- [A long tally on a board full of unused sections] → The penalty phase is capped at 2 seconds; with 40 unused sections a step is 50 ms and the field still visibly empties.
- [The dialog recomposes every frame during the animation] → About 4 seconds of small text and three icons, then it settles. The field behind it already redraws each frame for the popups.
- [Game time keeps growing after the win] → Nothing reads it except the timeline. Restart and Next level set it to 0.
- [The removed sections vanish without a transition] → Accepted for now; the penalty row and the count make the link. A shrink-and-fade per section is polish.
- [The player loses points they saw in the HUD] → The rows show exactly where the points went, and the penalty per section is small.

## Migration Plan

Nothing is stored or migrated. To roll back, revert the change: reaching the finish is then not detected, which is the current behaviour.

## Open Questions

- Should a tap on the dialog card skip the animation to the end? It is a small addition on `skipWinAnimation()`, left out until the animation has been seen on a device.
- Should the level history record a win with 0 stars as 1 star, or should a won level always earn at least 1 star from the thresholds? To be decided when `app-game-level-history` wires the recording.
- Should the dialog offer the level library when it exists, as the lost dialog's open question also asks? Exit goes to the main menu for now.
- Should a removed section shrink and fade (design system's replace-tile motion) instead of vanishing? Left to polish.
