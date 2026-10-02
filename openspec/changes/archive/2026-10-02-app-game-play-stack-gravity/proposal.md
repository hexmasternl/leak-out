# Proposal

Tracking issue: [#37 Stack animation](https://github.com/hexmasternl/leak-out/issues/37).

## Why

The stack holds exactly `stack.visibleSize` sections (5 by default, at most 10) and after a placement the column simply redraws one slot lower. It feels static: there is no sense of pieces being stacked, and the player only ever sees a short preview. The request is a taller stack whose pieces behave as if gravity pulls them down: the bottom piece is placed, the rest drop into place and a new piece falls in on top, with pieces that do not fit cut off at the top of the column.

## What Changes

- **Ten more sections**: the stack holds `stack.visibleSize + 10` sections (15 with the default visible size, at most 20). The ten extra sections are drawn from the same weights and in the same order as today; they just sit above the ones that are always shown.
- **`visibleSize` keeps its meaning for level design**: it is the number of sections the player is guaranteed to see. The slot sizes are still computed so that `visibleSize` slots fit the column (same metrics as today, so the largest stack still fits a 360 dp tall screen). The extra sections continue upwards at the preview size and are shown as far as the stack rail has room; whatever does not fit is **clipped off the top** of the rail. On a tall screen the player sees more of them, on a small one fewer, never fewer than `visibleSize`.
- **Gravity animation**: when the board accepts a placement the bottom slot empties and every section falls one slot down: it accelerates like a falling object, lands with one small bounce and settles. The sections start falling from the bottom up with a short stagger, so the column collapses like a real pile. The section that becomes the next one grows to the next-slot size while it falls. One new section falls in from above the top edge of the rail into the top slot.
- **Rapid placements** retarget: a placement while sections are still falling makes each one continue from where it is to its new slot, never jumping back.
- **Reduced motion**: no acceleration or bounce; every section slides to its new slot over 160 ms ease-out and the new top section fades in.
- **Accessibility**: only sections whose slot is fully visible are exposed to the screen reader; clipped sections are hidden from it.
- `docs/design-system.md` §5.4 "Stack rail" is brought in line with the game (next piece at the bottom, gravity, clipping) and §7 gets a `motion.fall` entry and the reduced-motion rule. `docs/level-format.md` explains that `visibleSize` is the guaranteed visible part of a stack that holds ten more.

No game rule changes: only the bottom section can be dragged, the stack still changes only when the board accepts a drop, the replacement penalty still takes the section at the drop.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `game-play-stack`: the stack holds ten sections more than its visible size, sections fall into place with a gravity animation, and sections that do not fit the rail are clipped off the top.
- `game-level-model`: `stack.visibleSize` is defined as the guaranteed visible part of the stack; the stack itself holds ten more sections.

## Impact

- `core/stack/PipeStack.kt`: `create` fills `visibleSize + EXTRA_SECTIONS` (10); a `placements` counter gives every section a stable draw number (`idAt(index)`) so the UI can animate the same piece from slot to slot. `PipeStackTest`, the solver and headless level tester keep working because only the bottom section is ever used.
- `app/ui/game/StackLayout.kt`: metrics keyed on `visibleSize` (not on the stack size), plus a pure layout of slot positions from the bottom up and a `fullyVisibleSlots` helper.
- `app/ui/game/StackPanel.kt`: bottom-anchored, clipped column; per-section `Animatable` offsets keyed by draw number; gravity spec and reduced-motion spec in a new pure `StackGravity` model.
- `app/ui/game/GameScreen.kt`: passes `level.stack.visibleSize` to the panel.
- Tests that assume `stack.size == visibleSize` (`PipeStackTest`, `GamePlayHudTest`, `GameEngineScreenTest`) are updated.
- Coordination: `app-game-first-time-user-experience` already reasons about "fully visible" slots and clipped tops; this change provides that geometry via `StackLayout`. `app-game-play-balanced-stack` (issue #35, proposed in parallel) changes how sections are drawn; it applies to the ten extra draws unchanged and neither change depends on the other.
