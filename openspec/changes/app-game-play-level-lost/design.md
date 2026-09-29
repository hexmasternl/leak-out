## Context

The open game play changes build the game screen piece by piece:

- `app-game-play-stack` adds `GameScreen`, `StackPanel`, `SectionDragState` and the `SectionDropTarget` contract.
- `app-game-play-hud` adds `GameHud`, `PauseDialog` and `GameSession`, the single owner of the running game (`paused`, `pauseMenuOpen`, `elapsedMs`, `tick`, `pause`, `resume`, `restart`). It also adds the `screen` state in `MainActivity` and the `onExit` callback that returns to the welcome screen.
- `app-game-play-playing-field` adds `Board` and `Cell` in `core.board` and `PlayingField` with `FieldViewport`.
- `app-game-play-fast-forward` adds `PipeSection.exitFor(entry: Side): Side?` and a `RouteTracer` that follows the route over a `Board`, including the "cross channel already used" rule. It also adds `fastForward` and the refusal of placements while it is on.
- `app-game-play-points` adds `ScoreSheet`, the popups and `GameSession.liquidExited(at)`, with the flow contract "called once per exit, before the leak or win check".

There is still no flow simulation. Plan section 2.5 describes it: when the liquid's progress in a cell reaches 1, it looks at the neighbour and either continues, wins or leaks. This change supplies that neighbour check and everything that happens after a leak. The flow change will call both.

The welcome and pause dialogs set the dialog style: a Compose `Dialog` with the design-system card (white surface, 3 px ink border, Fredoka title) and `CartoonButton`s, Exit in the error-red style.

## Goals / Non-Goals

**Goals:**
- One pure rule in `core` that says what happens when the liquid leaves a tile: enter the next tile, reach the finish, or leak, and where the leak is.
- A lost state in `GameSession` that stops everything the same way pause does, but cannot be resumed.
- A short leak effect on the field, then a level lost dialog with a clear title, the score, Restart and Exit.
- Correct behaviour for Back, taps outside the dialog and going to the background.

**Non-Goals:**
- The flow simulation itself (moving the liquid head, speed ramp, drawing the liquid in the pipes).
- Winning the level and the level complete dialog.
- Storing attempts, lost counts or progress.
- Sound and vibration on a leak.
- A retry limit or any penalty for losing.

## Decisions

**1. `FlowStep` is a pure function in `core.flow`.**
`FlowStep.after(board, from: Position, exit: Side, usedChannels: Set<ChannelUse>): FlowStep.Result` with:
- `Enter(next: Position, entry: Side)` — the next tile holds a pipe whose `exitFor(entry)` is not `null` and whose channel for that entry is not in `usedChannels`;
- `Finish` — the next tile is the finish and `exit.opposite() == finish.openSide`;
- `Leak(at: Position, side: Side)` — everything else, with `at = from` and `side = exit`.

`ChannelUse(position, channel)` names a used channel. For a cross, the channel is horizontal or vertical, so entering from the left and later from the right is the same channel. For other sections there is only one channel. The flow change keeps the set as the liquid moves; the tests pass it in. The start tile is checked by calling `after(board, start.position, start.openSide, emptySet())`.

The rule is written as an exhaustive `when` over `Cell` without `else`, so a new cell kind fails to compile until it is handled. *Other option:* reuse `RouteTracer` from the fast-forward change. It was not chosen because the tracer walks the whole route in one go and returns only complete or not complete, while the flow needs one step at a time with the leak location. `RouteTracer` can be rewritten on top of `FlowStep` later; both use `exitFor`, so they cannot disagree on the channel rules. A unit test runs the same scripted boards through both and checks that they agree.

**2. The outcome lives in `GameSession`.**
`GameSession` gets `outcome: Outcome` (`Playing` or `Lost(at, side, atMs)`) in Compose state, and `liquidLeaked(at, side)`:
- ignored when the outcome is already `Lost` (lost at most once) or while paused;
- sets `Lost` with `atMs = elapsedMs`;
- makes `GameScreen` cancel a running drag with the same `dragState.cancel()` call it makes on pause (it watches `isStopped`).

The flow contract in the KDoc: when a tile is left, the flow calls `liquidExited(at)` first (points), then `FlowStep.after`, and on `Leak` calls `liquidLeaked`. That keeps the "last section still scores" rule of the points change.

A read-only `isStopped` (`paused || outcome is Lost`) replaces the direct `paused` checks in `place`, `canPlace`, `liquidExited` and the `StackPanel` `enabled` flag, so every place that already stops on pause also stops on a loss. *Other option:* model the loss as "paused with a different dialog". It was not chosen because resume must never leave a lost state, and mixing the two makes every pause rule need an exception.

**3. The clock keeps running for the leak effect only.**
After the loss, `tick(dtMs)` still adds to `elapsedMs` but nothing else uses it: the countdown, the flow and the fast forward scale stay where they were at the loss. `lossAgeMs = elapsedMs - Lost.atMs` drives the spurt animation and `showLostDialog = lossAgeMs >= LeakTiming.EFFECT_MS` (1,000 ms). Using game time means the effect and the dialog are tested by calling `tick`, like the popups. *Other option:* a `LaunchedEffect` with `delay(1000)`. It was not chosen because it cannot be driven from unit tests and does not stop when the app goes to the background.

**4. Pause is refused after a loss.**
`pause()` does nothing while the outcome is `Lost`. So the cog wheel, Back and `ON_STOP` cannot open the pause dialog over a lost level. The cog wheel stays visible but is disabled. `ON_STOP` during the leak effect instead calls `skipLeakEffect()`, which moves `Lost.atMs` back by the rest of the effect so the dialog is shown right away when the player returns. The fast forward button hides when `isStopped`.

**5. Back depends on the phase of the loss.**
`BackHandler(enabled = outcome is Lost && !showLostDialog) {}` swallows Back during the effect. The dialog itself handles Back while it is shown. The existing `BackHandler` that opens the pause dialog becomes `enabled = !pauseMenuOpen && outcome is Playing`.

**6. `LevelLostDialog` follows the pause dialog.**
`LevelLostDialog(score, onRestart, onExit)` uses `Dialog(onDismissRequest = onExit, properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true))`, so Back works as Exit and a tap outside does nothing. The card holds a Fredoka title "Leaked!" in the error-red color, a small leaking pipe drawing, the message "The pipe is leaking. Level lost.", the score (formatted with `HudFormat.score`), then **Restart** as the primary `CartoonButton` and **Exit** in the error-red style. The card gets `Modifier.semantics { paneTitle = "Level lost" }` so a screen reader announces the loss. Restart calls `session.restart()`, which now also sets `outcome = Playing`. Exit calls the existing `onExit`.

**7. The spurt is drawn by `PlayingField`.**
`PlayingField` gets the lost tile, side and a `() -> Long` for `lossAgeMs`, read only inside the draw lambda (like the popups). A pure `leakFrame(ageMs)` returns the length, spread and alpha of a fan of droplets drawn from the middle of the leaking side outwards, in the liquid color. After 1,000 ms it keeps a small puddle so the leak is still visible behind the dimmed dialog. The drawing is in tile coordinates, so it follows pan and zoom. When the leak is over the edge of the grid, the droplets are drawn outside the grid and clipped by the field.

## Risks / Trade-offs

- [`FlowStep` and `RouteTracer` could drift apart] → Both use `exitFor`, and a unit test runs the same boards through both.
- [`isStopped` changes files from several unmerged changes] → The tasks start by checking that all five changes are in `development`, and the change touches only the checks, not their rules.
- [The flow change might call `liquidLeaked` before `liquidExited`] → The contract is in the KDoc and a unit test checks the order of points and loss for a leak from a corner.
- [A leak can happen while the player is mid-drag, which feels sudden] → The drag slides back like on pause, and the 1 second effect shows why the game stopped before the dialog appears.
- [Game time keeps growing after the loss] → Nothing reads it except the effect. Restart sets it to 0.

## Migration Plan

Nothing is stored or migrated. To roll back, revert the change. Leaks are then not detected, which is the current behaviour.

## Open Questions

- Should the level lost dialog also offer the level select screen? There is no level select screen yet. When it exists, Exit may go there instead of the main menu.
- Should the leak effect be skipped by a tap? Not for now. One second is short, and a tap during the effect could be meant as a drag that just got cancelled.
