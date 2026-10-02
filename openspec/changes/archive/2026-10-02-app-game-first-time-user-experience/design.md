# Design

## Context

Tracking issue: [#36 First time user experience](https://github.com/hexmasternl/leak-out/issues/36). See proposal.md for the motivation and specs/ for the behaviour.

- The engine already freezes everything on `GameCommand.Pause` (`GameState.paused`: countdown, flow and placing stop) and continues on `Resume`. `GameSession.showPauseDialog` is `state.paused && outcome is Playing`, and `GameScreen` sends `Pause` on Back and on `ON_STOP`.
- The stack contents are in `GameState` (`PipeStack`), the level carries `flow.reservoirSpeedFactor`, and after `app-game-play-mandatory-sections` lands the level/board carry the mandatory sections.
- Settings are a DataStore behind `SettingsRepository` with an `InMemorySettingsRepository` for tests; the level history (Room) is separate.
- `FieldViewport` owns zoom and pan; `StackPanel` lays out the stack column; neither exposes on-screen bounds today.

## Goals / Non-Goals

**Goals:**
- Tips that point at the real UI element (start tile, finish tile, stack, next slot, a stack section, mandatory cells) instead of a static picture.
- The decision "which tip is due now" as a pure, JVM-tested function.
- No change to game rules or `core`.

**Non-Goals:**
- No interactive "do it now" tutorial that waits for the player to actually place or pinch; the tour explains and the player continues.
- No dedicated tutorial level and no change to the bundled levels.
- No localisation beyond the existing English strings resource.
- No tips for other mechanics (fast forward, replacing a section, flow speed ramp); the scheduler is built so they can be added later.

## Decisions

### 1. Design system first, as a gate
As with #38, task group 1 adds a "Tip overlay" component to `docs/design-system.md` §5 (scrim with a cut-out spotlight around the target, `radius.md` spotlight with a `liquid.glow` ring, a level 3 callout card placed beside the target with a pointer, step counter, buttons) and its motion to §7.2/§7.4, plus a Compose preview mock-up attached to the issue. Implementation starts after approval on the issue.

Alternative: reuse the standard dialog (§5.6). Rejected: a centred dialog cannot point at the start tile or the stack, which is the point of the tips.

### 2. Freeze with the engine's pause, flag the reason in the session
A tip sends `GameCommand.Pause` and remembers `tipPaused = true` in `GameSession`; closing the last due tip sends `Resume`. `showPauseDialog` becomes `state.paused && playing && (activeTip == null || pausedByUser)`. Back and `ON_STOP` while a tip is shown set `pausedByUser` (backgrounding) or act on the tip (Back), so the pause dialog appears over the tip after returning, and its Resume only clears `pausedByUser` while the tip keeps the engine paused.

Alternative: a separate `Frozen` flag in `core`. Rejected: `Pause` already gives exactly the required freeze (including cancelling a drag), and the difference is purely presentational.

### 3. A pure `TipScheduler`
Android-free (`app/.../ui/game/onboarding`): `dueTips(level, stack, outcome, seen: Set<TipId>, atLevelStart: Boolean, fullyVisibleSlots: Set<Int>): List<TipId>` returns the due tips in the fixed order intro → mandatory → cross → reservoir. The cross and reservoir tips are only due when an `X` / `RES` sits in a slot whose index is in `fullyVisibleSlots` (see decision 7), and `tipSlot(stack, kind, fullyVisibleSlots)` returns the slot to highlight (the lowest fully visible one). `GameScreen` evaluates it at level start once the stack has been laid out, after each accepted placement (the only moment the stack changes) and whenever `fullyVisibleSlots` changes (e.g. a window resize). `TipId` is an enum with stable string keys (`intro`, `mandatory`, `cross`, `reservoir`) used for storage. The reservoir text uses `TipText.percent(factor)` (0.3 → "30 %", half up).

Alternative: trigger from composables observing the stack. Rejected: harder to test and easy to fire twice on recomposition.

### 4. Storage as a string set in the settings DataStore
`SettingsRepository` gets `seenTips: Flow<Set<TipId>>`, `markTipSeen(TipId)` and `resetTips()`, stored as one `stringSetPreferencesKey("seen_tips")`; unknown keys are ignored on read so future tips can be added or removed. A tip is written as seen when it is closed, not when it is shown, so killing the app mid-tour shows it again. Kept out of `Settings` (the data class) so the settings screen sliders don't recompose on tip changes.

Alternative: Room alongside the level history. Rejected: it is a preference, and resetting progress must not touch it.

### 5. Target bounds through a small registry
A `TipTargets` holder (a `CompositionLocal`) collects `Rect`s in root coordinates via `Modifier.onGloballyPositioned`: the stack column, each stack slot (by index), and field cells via `FieldViewport`'s cell-to-screen transform. The overlay reads the target rects for the current tip, draws the scrim with `Path.combine(Difference)` cut-outs and positions the callout on the side with the most room. For an off-screen field cell, the overlay asks `FieldViewport` to pan the cell into view (a new `revealCell(position)` with the existing pan limits) and the tour end restores the level-start view.

### 6. Overlay input
The overlay consumes all pointer input outside its buttons, which also blocks dragging, pinching and panning. The drag hint is an `InfiniteTransition` ghost section travelling from the next slot to the nearest empty cell (`TipScheduler.hintCell`, pure: nearest empty, non-blocked cell to the start's exit); reduced motion draws a static arrow.

### 7. Cross and reservoir tips wait until the section is fully visible
On a small screen `StackLayout` shrinks the slots only down to `MinPreviewSlot`, so with a deep visible stack the top slots can fall outside the screen. Highlighting a section the player cannot see is useless, so a stack section only counts for the cross and reservoir tips when its whole slot rect (from `TipTargets`) lies inside the visible bounds of the stack panel, clipped to the window. `TipTargets` derives `fullyVisibleSlots: Set<Int>` from the slot rects and the panel's clipped bounds (`boundsInWindow`) and passes it to the scheduler, which stays pure. Until a section is fully visible the game simply keeps running; each placement moves the stack down one slot. The next slot is always fully visible, so a tip cannot be missed: at the latest it shows when the section becomes the next one to place, before the player can use it.

Alternative: scroll or shrink the stack to bring the section into view when the tip fires. Rejected: it changes the stack layout the player is used to just for a tip, and the HUD layout is owned by `app-game-play-hud-enhancements`.

## Risks / Trade-offs

- [The mandatory section tip depends on an unmerged change] → Its task group is last and gated on `app-game-play-mandatory-sections` being on `development`; the rest can ship without it, with `dueTips` simply never returning `mandatory` until levels can have mandatory sections.
- [On a small screen a cross or reservoir tip comes later than the section was drawn] → Intended: the tip waits until the section can actually be seen, and at the latest shows when the section is the next one to place.
- [Target rects stale after rotation or zoom] → Rects are re-read every layout pass and the overlay recomputes on change; the screen is landscape-locked.
- [Tips feel intrusive to experienced players on a new install] → "Skip tour" on every step; Back skips; each tip only once.
- [Callout covering the target on small screens] → Placement logic picks the side with room and is covered by a JVM test on a 640 × 360 dp layout.

## Migration Plan

Existing installs have no `seen_tips` key, so every player sees the intro tour once on their next level, which the issue accepts. Rollback is removing the overlay; the stored key is harmless.
