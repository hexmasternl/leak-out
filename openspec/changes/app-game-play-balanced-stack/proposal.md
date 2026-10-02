# Proposal

Tracking issue: [#35 Random tile stack population needs to be less random](https://github.com/hexmasternl/leak-out/issues/35).

## Why

The stack draws every section independently from the level's fixed weights. Pure chance regularly deals the same corner three or four times in a row, or holds back the straight a player has been waiting for over ten placements. That feels unfair rather than challenging: the player loses to the dice, not to the clock. The straight and corner sections should come out *nearly evenly spread*, while a level designer keeps control over how often the special cross and reservoir appear.

## What Changes

- **Adaptive likeliness**: every section on the stack has a *likeliness* that changes as sections are drawn. A section's chance is its likeliness divided by the sum of all likeliness values. When a section is drawn, its likeliness **drops by 3**; every other adaptive section that the level allows **gains 1**. Likeliness is clamped between **1** and **20**, so no allowed section is ever eliminated and none can crowd out the rest.
- **Standard sections always adapt**: the straights (`H`, `V`) and corners (`LT`, `LB`, `RT`, `RB`) always follow the algorithm and start at **10** each (the new default weight, up from 3 and 2).
- **Special sections are configurable**: for the cross (`X`) and reservoir (`RES`) the level sets the initial likeliness (the existing `stack.weights.X` / `RES`) and, with a new optional `stack.adaptive` object, whether that section adapts like the standard ones or stays **fixed** at its weight (default: fixed). A fixed section never changes, but drawing it still counts as "a different section" for the adaptive ones.
- **State per attempt**: the current likeliness values are part of the immutable stack state, so they carry over between placements, reset on restart and give the same sequence for the same seed. This also removes the fresh `WeightedPool` the engine builds on every placement today.
- **Level format** stays backwards compatible: `stack.weights` keeps its keys and now means *initial likeliness*; `stack.adaptive` is optional. The bundled levels that enable the cross are re-tuned (`X` 1 → 6, `X` 2 → 12) so the cross keeps roughly its current share next to the larger standard weights.
- `docs/level-format.md` documents the likeliness algorithm, the new defaults and `stack.adaptive`.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `game-play-stack`: sections are drawn by adaptive likeliness instead of fixed weights; the likeliness state lives in the stack, resets per attempt and is deterministic for a seed.
- `game-level-model`: default stack weights become 10 for the straight and corner sections; new optional `stack.adaptive` flags for `X` and `RES`.

## Impact

- `core/.../stack/`: `WeightedPool` is replaced by an immutable `Likeliness` (current values per section plus the draw/update rule); `PipeStack` carries it and returns the next stack with the updated values; `PipeStack.create` fills the initial sections with the same algorithm.
- `core/.../engine/GameEngine.kt`: `place` uses the stack's own likeliness instead of a new `WeightedPool` per placement.
- `core/.../level/`: `LevelDefaults` (weights 10, `ADAPTIVE` flags, `LIKELINESS_MIN` 1, `LIKELINESS_MAX` 20, `LIKELINESS_DROP` 3, `LIKELINESS_GAIN` 1), `StackSettings.adaptive`, `LevelDto` (`AdaptiveDto` with `X` and `RES` only; other keys rejected as unknown), `LevelValidator` (weights above 20 rejected).
- `app/src/main/assets/levels/level_*.json`: cross weights re-tuned; `LevelSolvabilityTest` and the parser keep accepting every bundled level.
- `docs/level-format.md`: defaults table, stack weights section, new "Likeliness" section.
- No `app` UI change: `StackPanel` keeps rendering `PipeStack.sections`.
- Coordination: `app-game-level-designer` edits the stack weights and writes level files; whichever lands second adds the `adaptive` toggles and their default-omission. `app-game-play-mandatory-sections` renames `PipeSection.mandatory` to `alwaysOnStack`; this change uses whatever name is current. `app-game-play-stack-gravity` (#37) changes how many sections the stack holds and animates it; it does not touch how sections are drawn, so the two are independent.
