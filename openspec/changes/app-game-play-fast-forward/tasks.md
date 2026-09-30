## 1. Setup

- [x] 1.1 Check that `app-game-play-stack`, `app-game-play-hud` and `app-game-play-playing-field` are merged into `development` (`GameSession.place`, `canPlace`, `Board`, `Cell`, `StackPanel(enabled)`, `SectionDragState.cancel()` and `PipeSection.openSides` exist). If not, stop and merge them first.
- [x] 1.2 Add the string resources `fast_forward` ("Fast forward") and `fast_forward_on` ("On"), and the `ic_fast_forward.xml` vector drawable.

## 2. Core: channel rule

- [x] 2.1 Add `PipeSection.exitFor(entry: Side): Side?` in `core.level`: `null` when `entry` is not an open side, straight through for `X`, the other open side for every other section.
- [x] 2.2 Unit test `exitFor` for every section and every side, including `RES` from the top and bottom (`null`) and `X` from all four sides.

## 3. Core: route tracer

- [x] 3.1 Create the package `nl.hexmaster.leakout.core.route` with `Route` (`Complete(cells)`, `Open(endsAt, reason)`) and `RouteTracer.trace(board, level)`, following the walk and the check order in the design, with a visited set per position and axis.
- [x] 3.2 Unit test `RouteTracer` with scripted boards: straight and corner route complete; empty tile; blocked tile; section without a matching opening; finish from the wrong side; off the grid; reservoir from the top; cross passed straight through; a route that uses both channels of one cross and is complete; a route that reaches a used channel (`LOOP`); a complete route with `liquidEntered` on its first sections is still complete; the listed cells of a complete route are in order.
- [x] 3.3 Run `./gradlew :core:test` and check that everything passes.

## 4. App: game session

- [x] 4.1 Add `routeComplete`, `fastForward`, `FAST_FORWARD_FACTOR = 10`, `flowTimeScale` and `showFastForward` to `GameSession`. Recompute `routeComplete` at the end of every accepted `place(...)`.
- [x] 4.2 Add `startFastForward()`: it turns fast forward on only when the route is complete, the game is not paused and fast forward is off.
- [x] 4.3 Make `place(...)` and `canPlace(...)` reject while fast forward is on, and make `restart()` clear `routeComplete` and `fastForward`.
- [x] 4.4 Document the flow contract in `GameSession`'s KDoc: the flow uses `dtMs * flowTimeScale` for the countdown and the head, and everything else uses `dtMs`.
- [x] 4.5 Unit test `GameSession`:
  - completing a route sets `routeComplete`;
  - a replacement that breaks the route clears it and hides the button;
  - `startFastForward` does nothing without a complete route or while paused, and sets `flowTimeScale` to 10 otherwise;
  - placing and `canPlace` are rejected while fast forward is on;
  - pause and resume keep fast forward;
  - restart clears everything;
  - `elapsedMs` still advances by `dtMs`, not by 10 × `dtMs`.
- [x] 4.6 Add `stopFastForward()` (turns it off when not paused) and `toggleFastForward()` (start when off, stop when on). Unit test: toggling on and off switches `flowTimeScale` between 10 and 1, keeps the button shown while the route is complete, does nothing while paused, and placing works again after turning it off.
- [x] 4.7 Add the string resources `fast_forward_normal_speed` ("Normal speed"), change `fast_forward_on` to "Fast forward on", and add the `ic_slow.xml` vector drawable.

## 5. App: fast forward button

- [x] 5.1 Add `FastForwardButton(active, onClick, modifier)`: a 56 dp circular cartoon-style button. Not active: the brass fast forward button with a pulse and the description "Fast forward". Active: the liquid-blue slow button with the slow icon, no pulse, the description "Normal speed" and the state "Fast forward on". Test tag `fast_forward` in both states.
- [x] 5.2 Wrap `GameScreen`'s layout in a `Box` and place `FastForwardButton` with `align(Alignment.BottomEnd)` and 16 dp padding, inside `AnimatedVisibility(session.showFastForward)` with scale and fade in and out.
- [x] 5.3 Wire `onClick` to `session.toggleFastForward()`, followed by `dragState.cancel()` when fast forward is now on, and set `StackPanel`'s `enabled` to `!session.paused && !session.fastForward`.

## 6. Instrumented tests (emulator)

- [x] 6.1 With a scripted level and a seeded stack, placing the section that completes the route shows the button; its bounds end 16 dp from the right and bottom edges of the screen and are at least 56 dp square.
- [x] 6.2 Without a complete route, no button is shown. Replacing a route section so that the route breaks hides the button again.
- [x] 6.3 Tapping the button sets `flowTimeScale` to 10 and shows the slow button ("Normal speed", state "Fast forward on"). Tapping the slow button sets `flowTimeScale` back to 1 and shows the fast forward button again, and placing works again.
- [x] 6.4 After fast forward is on: dragging the next section does nothing and no tile is highlighted; pan and pinch still move the field.
- [x] 6.5 Pause and resume keep fast forward on. Restart hides the button and sets `flowTimeScale` back to 1.
- [x] 6.6 Run `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` on the emulator and record the results. Result: `:core:test` and `:app:testDebugUnitTest` pass; `FastForwardTest` (6.1–6.5, 5 tests) passes on the Pixel_10 AVD (Android 17). The full `connectedDebugAndroidTest` run of the other suites was skipped at the user's request.

## 7. Wrap up

- [x] 7.1 Update `docs/project-plan.md`:
  - section 1.8: once the route is complete, a fast forward button runs the countdown and the flow at 10x and locks placing; it then becomes a slow button that restores the normal speed and unlocks placing;
  - section 1.11: the game screen has a fast forward button at the bottom right;
  - section 2.5: the flow step uses `dt × flowTimeScale` and loops over whole cells.
- [x] 7.2 Run `openspec validate app-game-play-fast-forward` and fix any issues.
