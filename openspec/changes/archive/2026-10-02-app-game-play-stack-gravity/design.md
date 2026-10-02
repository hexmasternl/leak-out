# Design

## Context

- `PipeStack(sections)` in `core` holds exactly `StackSettings.visibleSize` sections (1–10, default 5), the next one last. `afterPlacement` drops the last section and draws a new one at index 0. Sections are plain enum values, so two `H`s in the stack cannot be told apart.
- `StackPanel` draws a centred `Column` of `StackSlot`s, sized by `StackLayout.metrics(maxHeight, stack.size)` (preview slot 20–40 dp, next slot 1.25 ×, gap 2–6 dp). An `animateFloatAsState(0f)` "move down" offset exists but never animates. `GameScreen` sizes the rail width from `StackLayout.metrics(360.dp - HUD, visibleSize)`.
- `rememberReducedMotion()` exists (`ui/splash`) and is used by the HUD.
- The design system §5.4 still describes an old portrait rail where the top piece is next; the game has the next piece at the bottom of a left-hand column.
- In flight: `app-game-first-time-user-experience` needs "fully visible" stack slots for its cross/reservoir tips; `app-game-play-hud-enhancements` keeps the 48 dp HUD so the largest visible stack fits on 360 dp; `app-game-play-balanced-stack` (#35) changes the draw algorithm.

## Goals / Non-Goals

**Goals:**
- A taller stack (ten more sections) whose pieces fall into place with a believable gravity motion.
- Keep level balance stable: a level's `visibleSize` still means what the player can always see.
- Clip what does not fit, at the top, without scrolling.

**Non-Goals:**
- Changing how sections are drawn (that is #35).
- Letting the player scroll the rail or interact with any section other than the bottom one.
- Physics simulation: the motion is a deterministic, testable curve, not a physics engine.

## Decisions

### 1. "Add 10 sections" = the stack holds `visibleSize + 10`
`PipeStack.EXTRA_SECTIONS = 10`; `create` draws `visibleSize + 10` sections. `visibleSize` stays in the level format with its range 1–10 and default 5, and becomes "the guaranteed visible part of the stack".

*Alternatives:* raise the default `visibleSize` from 5 to 15 (rejected: changes the preview every existing level was designed and solved against, and 15 slots cannot fit 360 dp at a readable size), or raise the maximum to 20 (rejected: does not make any current level's stack larger, which is what the issue asks). The chosen option makes every stack larger while every slot that is guaranteed today stays guaranteed.

The trade-off is that a tall screen shows more look-ahead than a small one. That is what the issue asks for ("if the viewport does not have enough space … they are clipped") and the extra sections are a bonus on top of the designed preview, not a requirement to solve a level.

### 2. Stable identity for animation via a draw counter
`PipeStack` gains `placements: Int = 0`, incremented by `afterPlacement`. `idAt(index) = placements + (size - 1 - index)` is the section's draw number: it stays the same as the section moves down. The UI keys each slot's animation state on it. The existing `sections` list and its `copy(sections = …)` use in tests are unchanged, so the engine, solver and headless tester are unaffected.

*Alternative:* track identities only in the UI by diffing lists. Rejected: ambiguous with repeated sections (`H, H, H`).

### 3. Bottom-anchored, clipped layout
`StackLayout.metrics(availableHeight, visibleSize)` is computed exactly as today but from `visibleSize` (so the next slot and the guaranteed slots keep their size). A new pure `StackLayout.slotTops(metrics, stackSize)` places the next slot at the bottom of the rail and each preview slot one `previewSlotSize + gap` pitch above the previous one. `StackPanel` draws the slots in a `Box` with `clipToBounds()` at those offsets, so slots above the rail's top edge are cut off (a partly visible slot shows its lower part). `StackLayout.fullyVisibleSlots(metrics, stackSize, railHeight)` returns the indices whose whole slot lies inside the rail; at least the `visibleSize` bottom slots are always in it. The onboarding change can use this helper instead of measuring.

Slots above the top by more than one pitch are not composed at all (they cannot become visible without a placement), which keeps the composition at most ~`railHeight / pitch + 1` slots.

### 4. Gravity motion
A pure `StackGravity` object gives the vertical offset of a falling section over time, for a fall of `d` dp:
- **Fall**: `y(t) = d · (t / T)²` (constant acceleration) with `T = clamp(sqrt(d / 8 dp) · 90 ms, 120 ms, 260 ms)`: a one-slot drop takes about 200 ms.
- **Bounce**: on landing, one rebound of `min(0.12 · d, 4 dp)` up and back down over 120 ms (`sin(π · u)`), then rest. Total settle about 320 ms, close to `motion.pop`.
- **Stagger**: the section closest to the bottom starts first; each one higher starts 24 ms later, capped at 8 sections (a collapsing pile without a long tail), and shortened where needed so that every one-slot fall still rests within the 400 ms the spec allows (with a 46 dp pitch only the bottom three are staggered; the rest start together).
- **Next section growth**: the section that falls into the next slot interpolates its size from `previewSlotSize` to `nextSlotSize` and its frame from preview to next style over the fall.
- **New section**: enters from one pitch above the top slot (above the rail's clip edge, so it falls into view if the top is visible) and falls with the same curve.

Implementation: a pure, immutable `StackFalls` model keyed by draw number holds each section's fall (start offset, start frame, delay), and the panel advances one frame clock (`withFrameMillis`) while any section is falling; slots read their offset in a `graphicsLayer`. This replaces the planned one `Animatable` per section: the retarget then happens synchronously in the same composition as the placement (no frame where a section shows at its new slot first) and the whole motion is JVM-testable. On a placement every surviving section's offset is increased by one pitch from its *current* value, and its fall restarts with the gravity curve from there. This makes a placement during a fall retarget smoothly: the section keeps falling from where it is, now towards the lower slot, and never jumps back.

*Alternative:* `spring()` with low damping. Rejected: a spring decelerates on approach, which reads as sliding, not falling; the issue explicitly asks for gravity.

### 5. Interaction during the fall
The new bottom section is draggable as soon as the board accepts the drop, even while it is still falling: the game is timed and the player must not wait for an animation. Starting a drag snaps its offset to 0 (the slot shows empty, the copy follows the finger as today). Disabled states (fast forward, replacement sequence, stopped game) work as today. Pausing does not freeze the animation (it is UI only and the opaque pause backdrop hides it).

### 6. Reduced motion
With `rememberReducedMotion()` true: no acceleration, bounce or stagger. Every section animates its offset to 0 with a 160 ms ease-out tween, and the new top section fades in (alpha 0 → 1, 160 ms) in its slot instead of falling in. This follows design system §7.4 (no springs, 160 ms ease-out).

### 7. Accessibility
Only slots in `fullyVisibleSlots` get semantics (content description as today, the next slot says it is next). Partly visible slots use `clearAndSetSemantics {}` so the screen reader does not announce a piece the player cannot see.

## Risks / Trade-offs

- **Screen-dependent look-ahead** → accepted (decision 1); `visibleSize` remains the designed guarantee and the solver/headless tester only ever use the bottom section.
- **Animation cost with 20 slots** → only slots within the rail plus one pitch are composed; offsets are `graphicsLayer` translations so a fall does not trigger relayout.
- **Tests that count the stack** → `PipeStackTest`, `GamePlayHudTest` and `GameEngineScreenTest` assume `size == visibleSize`; they are updated in the tasks.
- **Interplay with onboarding tips** → that change measures fully visible slots; with this change the answer is available from `StackLayout.fullyVisibleSlots` and stays consistent with what is drawn.

## Migration Plan

No data migration. Level files are unchanged; every level gets ten more stack sections on its next attempt. Rollback is reverting the change.
