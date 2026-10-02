# Proposal

Tracking issue: [#35 Random tile stack population needs to be less random](https://github.com/hexmasternl/leak-out/issues/35).

## Why

The stack draws every section independently from the level's fixed weights. In play-testing, some sections simply did not appear for a very long time: in 1 % of 40-draw runs a standard section is missing for 36 draws or more. Pure chance also regularly deals the same corner three times in a row. That feels unfair rather than challenging: the player loses to the dice, not to the clock. The straight and corner sections should come out *nearly evenly spread*, a long drought should be impossible, and a level designer keeps control over how often the special cross and reservoir appear.

The first idea, a per-section likeliness that drops by 3 when drawn and gains 1 otherwise, clamped to 1..20, was simulated and rejected: the values drift up to the cap within about 30 draws, after which a section missing for 3 draws is as likely as one missing for 25. It barely shortens droughts (worst 1 %: 32 draws instead of 36) and makes a fixed cross rarer as the level goes on. See design §1.

## What Changes

- **Deficit steering**: each allowed section has a *target share*, its weight divided by the sum of all weights. The stack counts how often each section has been dealt in the attempt. A section's *deficit* is how many times it should have been dealt by now (target share × draws so far) minus how many times it was. It is drawn with weight `share × e^(k × deficit)`, with steering strength `k` = 1. Sections that are behind become more likely the longer they stay behind; sections that are ahead become less likely. Deficits always sum to zero, so nothing drifts during a level.
- **Drought limit**: a straight or corner section that has not been dealt for `⌈2.5 / share⌉` draws (15 on a level without cross or reservoir) is dealt next. With deficit steering this fires about once every two levels and puts a hard upper bound on how long any standard section can be missing. The cross and reservoir are exempt: a rare special section is never forced.
- **Cross and reservoir follow the same rule**: their weight is their target share, and steering keeps them at it for the whole level. No fixed/adaptive flag is needed.
- **Equal standard defaults**: the straights and corners default to weight 7 each (was 3 and 2), so they are dealt evenly. The bundled levels' cross weights are re-tuned ×3 (`X` 1 → 3, `X` 2 → 6), which keeps exactly today's cross share (6.7 % and 12.5 %).
- **State per attempt**: the dealt counts and droughts are part of the immutable stack state, so they carry over between placements, reset on restart and give the same sequence for the same seed. This also removes the fresh `WeightedPool` the engine builds on every placement today.
- **Level format** stays backwards compatible: `stack.weights` keeps its keys and its 0-or-more range, and now explicitly means *target share*. No new field.
- `docs/level-format.md` documents steering, the drought limit and the new defaults.

Simulated over 10,000 seeded runs of 40 draws (six standard sections):

| | Pure random (today) | Rejected likeliness idea | **This change** |
|---|---|---|---|
| Spread (std. dev. of counts per section) | 2.24 | 1.58 | **0.65** |
| Longest drought, worst 1 % of runs | 36 | 32 | **15 (guaranteed)** |
| Same section twice in a row | 16.7 % | 13.8 % | **10.4 %** |
| Runs missing a standard section entirely | 0.4 % | 0.08 % | **0 %** |

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `game-play-stack`: sections are drawn by deficit steering towards the weights' shares, with a drought limit for the standard sections; the steering state lives in the stack, resets per attempt and is deterministic for a seed.
- `game-level-model`: default stack weights become 7 for the straight and corner sections; weights are described as target shares.

## Impact

- `core/.../stack/`: `WeightedPool` is replaced by an immutable `Dealer` (dealt count and drought per section, draws so far, and the steering and drought rules); `PipeStack` carries it and returns the next stack with the updated dealer; `PipeStack.create` fills the initial sections through it.
- `core/.../engine/GameEngine.kt`: `place` uses the stack's own dealer instead of a new `WeightedPool` per placement.
- `core/.../level/LevelDefaults.kt`: weights 7, `STEERING_STRENGTH` 1.0, `DROUGHT_FACTOR` 2.5. Parser and validator are unchanged.
- `app/src/main/assets/levels/level_*.json`: cross weights re-tuned; `LevelSolvabilityTest` and the parser keep accepting every bundled level.
- `docs/level-format.md`: defaults table, stack weights section, new "How the stack deals" section.
- No `app` UI change: `StackPanel` keeps rendering `PipeStack.sections`.
- Coordination: `app-game-level-designer` edits the stack weights; it needs no new controls for this change. `app-game-play-mandatory-sections` renames `PipeSection.mandatory` to `alwaysOnStack`; this change uses whatever name is current. `app-game-play-stack-gravity` (#37) changes how many sections the stack holds and animates it; it does not touch how sections are drawn, so the two are independent.
