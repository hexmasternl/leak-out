# Proposal

## Why

The intro tour teaches a new player to drag a section from the stack onto the field, but never tells them that a tap on a cell places the next section too, which is much faster. Players who only learn to drag play every level slower than they need to, and the one place where the game explains itself skips its best shortcut.

Tracked in [#155](https://github.com/hexmasternl/leak-out/issues/155).

## What Changes

- The intro tour gets a sixth step, **Tap to Place**, shown right after the placing step and before the zoom and pan step. It highlights the same empty cell the placing step's drag hint ends on and explains, as a pro tip, that tapping an empty cell places the next section there without dragging.
- The step shows an animated tap hint on that cell (a finger-sized ring that pulses), or a static ring with reduced motion, instead of the drag ghost.
- The step counter, the screen reader's "Step n of 6" and the numbering of every existing step follow the new count; the zoom and pan step stays the last one with "Let's go".
- The design system's tip overlay section (5.9) lists the new step and its text.
- No change to who sees the tour: players who have already seen it do not see the new step.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `game-play-onboarding`: the intro tour has six steps; the tap to place step is added after the placing step, with its highlight, hint, reduced-motion and screen-reader behaviour.

## Impact

- `app/src/app`: the intro tour step count, the tour step contents and the game screen's spotlight and hint for the new step (`ui/game/onboarding`, `ui/game/GameScreen.kt`), a new tap hint drawn by the tip overlay, the tip mockups and previews, and new strings in `res/values/strings.xml`.
- `app/src/app` tests: unit tests for the step contents and order, instrumented tests that walk the tour.
- `docs/design-system.md` section 5.9.
- No change to `core`, the level files, the server or saved progress; the "intro tour seen" flag keeps its meaning.
