# Design

## Context

The tip overlay (`TipOverlay.kt`) is a full-screen `Box` with a catch-all `pointerInput` that consumes every event in the Main pass, so the game underneath gets no input. The callout card sits on top of it and holds `FlowButton`s, which use `clickable` with press feedback through their `MutableInteractionSource`. On the S21 the press feedback shows but `onClick` never runs. A press that starts and is then cancelled, rather than released, causes exactly that.

Things in the current code that can cancel or eat a press:

1. **The card moves or is rebuilt during a press.** `CalloutLayer` places the card next to the union of the spotlights and writes `pointer` state *during layout*. In `GameTips` the spotlight rects are recomputed from `TipTargets` while the field pans to reveal a cell (`revealCell`), and a single spotlight glides between targets. Each change places the card again. If the card shifts while the finger is down, the up event lands outside the button and `clickable` cancels the press. A layout to state to layout loop (the card size or position depends on `pointer`, which `placed.pointer` then flips) would keep the card moving all the time on some aspect ratios.
2. **Recomposition every frame.** The glow pulse (`rememberInfiniteTransition`) is read in composition, so `TipOverlay` recomposes every frame (120 Hz on the S21) and recreates the `CalloutLayer` content lambda and the button `onClick` lambdas. That alone does not cancel a press, but together with (1) it makes it worse.
3. **System areas.** The game runs `sensorLandscape`, edge-to-edge and immersive (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`). In landscape the S21's punch hole and the side back-gesture areas sit at the left and right edges. `CalloutPlacement` only keeps a fixed `ScreenGutter` from the window edge and ignores insets, so a card next to the stack column or the field edge can land in a gesture area. There the system takes the touch, or delays and then cancels it.
4. **The overlay catch-all.** It consumes events in the Main pass, after the children. That should be safe, but it has to be verified that a descendant's `graphicsLayer` scale (`CalloutLayer` scales the card with a spring) does not move hit testing away from where the button is drawn.

## Goals / Non-Goals

**Goals:**
- Reproduce the failure in an instrumented test, ideally on an S21-sized emulator profile, before changing code.
- Fix the root cause. A tap that shows press feedback always runs its action.
- Keep the visuals of design-system.md 5.9 unchanged.

**Non-Goals:**
- Changing the tour content, its order or which tips are due.
- Reworking `FlowButton` for the rest of the app, unless the root cause is in it.

## Decisions

- **Reproduce first.** Add an emulator profile matching the S21 (2400×1080, 420 dpi, cutout emulation "Punch hole", gesture navigation) and an instrumented test that taps "Next" and "Skip tour" with real down/up input right after the overlay appears and during the reveal pan, both through `performTouchInput` and through `UiDevice` (which goes through the system, so gesture-area effects show up). The test must fail on the current code. If it cannot be made to fail on an emulator, record what was tried in this file before fixing the most likely causes.
- **Freeze the card placement while it is pressed and once a step has settled.** Work out the card's place from the step's *final* target rects, not from rects mid-animation, and only move the card when the step changes. Derive `pointer` from the placement result in the same pass instead of writing state during layout, so layout can no longer loop. Alternative: make the button tolerate movement. Rejected, because a card that moves under the finger is a bug in itself.
- **Read animations in the draw phase.** Pass the glow and entry alpha as lambdas read in `graphicsLayer` or `drawBehind`, so the overlay does not recompose every frame.
- **Place the card inside the safe area.** Give `CalloutPlacement` the union of the safe drawing, display cutout and system gesture insets as extra margins, falling back to the side with more room. Covered by JVM tests in `CalloutPlacement`.
- **Ignore taps after close.** Once the tip has closed, the overlay leaves composition, and a second tap must not reach the game. `nextTourStep` already clamps. Add a test that rapid "Next" taps advance one step per tap.

## Risks / Trade-offs

- [The emulator cannot reproduce it (no 120 Hz, no Samsung One UI gesture handling)] → test on a real S21 through the internal testing track before closing the issue, and keep the regression tests that check the safe-area placement and that the card does not move.
- [Freezing the placement can leave the card overlapping a spotlight that changes size after a resize] → place it again on a configuration or viewport change, but never while a pointer is down.
- [System gesture insets in landscape can be large on small phones, which leaves less room for the card] → `CalloutPlacement` already shrinks and moves the card; cover a 640×360 dp window in its tests.

## Open Questions

- Which One UI version and navigation mode (gestures or buttons) the reporting device uses. This narrows the reproduction but does not change the fix.
