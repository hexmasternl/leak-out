# Design

Tracking issue: [#35 Random tile stack population needs to be less random](https://github.com/hexmasternl/leak-out/issues/35).

## Context

`PipeStack.create` and `PipeStack.afterPlacement` draw from a `WeightedPool` built from `StackSettings.weights`. `GameEngine.place` builds a new `WeightedPool` on every placement, so nothing about earlier draws is remembered. Defaults are `H`/`V` 3, corners 2, `X`/`RES` 0. Every bundled level overrides only `X` (0, 1 or 2) and `RES` (0). The only impure input of the engine is its `Random`, which tests seed.

## Goals / Non-Goals

**Goals:** standard sections come out nearly evenly spread; no allowed section is ever eliminated; special sections stay under the level's control; same seed, same sequence; old level files keep parsing.

**Non-Goals:** a deterministic "bag" shuffle; per-level tuning of drop, gain, min or max; changing how many sections the stack shows (that is #37); guaranteeing a solvable sequence.

## Decisions

### 1. The algorithm

State: a likeliness `L[s]` per section. Sections with an initial weight of 0 are *disabled*: they have no likeliness and are never drawn.

Draw: pick section `s` with probability `L[s] / Σ L` over the enabled sections (integer arithmetic, `random.nextInt(total)` as today).

Update, after every draw of `s`:

1. If `s` is adaptive: `L[s] = max(MIN, L[s] − DROP)`.
2. For every enabled adaptive section `t ≠ s`: `L[t] = min(MAX, L[t] + GAIN)`.
3. Fixed sections never change.

Constants (in `LevelDefaults`, global, not per level): `DROP` 3, `GAIN` 1, `MIN` 1, `MAX` 20, standard start 10.

*Gain applies to every other adaptive section, whether the drawn section is adaptive or fixed.* Drawing a fixed cross is "a different section" for the corners, which matches the issue's wording, and it means a cross draw does not freeze the balancing.

*Why MIN 1 and MAX 20.* MIN 1 is the smallest value that never eliminates a section (the issue's hard requirement). MAX is twice the start: it lets a long-absent section become clearly more likely without dominating. A simulation of 4,000 runs of 40 draws over the six standard sections gave:

| | Std. dev. of counts per section | Same section twice in a row |
|---|---|---|
| Fixed weights (today) | 2.24 | 16.2 % |
| MAX 10 | 1.70 | 11.4 % |
| MAX 20 | **1.58** | 13.5 % |
| MAX 30 | 1.35 | 13.9 % |

MAX 20 cuts the spread by about 30 % and repeats by about 17 % with the issue's own numbers (start 10, −3, +1). Higher maxima spread a bit better but make the climb back so slow that a long-missing section returns very late in a level. The constants live in one place, so tuning after play-testing is a one-line change.

*Dynamics to know about.* With six standard sections a section is drawn about 1 in 6 times, so on average its likeliness rises (+1 five times, −3 once). The values drift from 10 up towards 20 over roughly 30 draws, and the standard total grows from 60 to about 105. The balancing then works as "the recently drawn section is less likely", which is the intent.

### 2. Special sections: fixed by default

`stack.adaptive` is an optional object with only `X` and `RES` (boolean, default `false`). An adaptive special section with a low initial weight is rarely drawn, so it *gains* on almost every draw and climbs to MAX 20, as common as a corner. That is almost never what a level designer wants, so fixed is the default and the docs warn about it. Straight and corner sections are always adaptive and cannot be listed: `"adaptive": { "H": true }` is an unknown-key parse error, like any other unknown key.

### 3. Defaults and re-tuning the bundled levels

`stack.weights` keeps its meaning of "relative chance" but is now the *initial* likeliness. The standard default becomes 10 (the issue's start value). Weights above `MAX` (20) are rejected by the validator, because an adaptive section could never get back to them, and a fixed weight above 20 would out-weigh every standard section.

Because the standard total grows from 14 to 60–105, a fixed cross weight of 1 or 2 would become 3–6 times rarer. The bundled levels are re-tuned by ×6: `X` 1 → 6 (about 6 % of draws mid-level, was 6.7 %) and `X` 2 → 12 (about 11 %, was 12.5 %). This is a content change only; the format stays backwards compatible: an old level file still parses and validates, it just plays with the new balancing.

### 4. State in the stack, not in the engine

A new immutable `Likeliness` value (`core/.../stack/Likeliness.kt`) holds the current values and the adaptive set, with `draw(random): Pair<PipeSection, Likeliness>`. `PipeStack` gets a `likeliness` property; `PipeStack.create(settings, random)` starts from the level's weights and draws the initial `visibleSize` sections through the algorithm (so the starting stack is already balanced); `afterPlacement(random)` no longer takes a pool. `GameEngine.place` drops its `WeightedPool(...)`. `GameState` already copies `stack`, so restart (which calls `start`) resets the likeliness and a pause/resume keeps it. `WeightedPool` is deleted with its test; its "weight 0 never drawn" and distribution tests move to `LikelinessTest`.

`Level.isAllowed` keeps meaning "initial weight > 0".

### 5. Tests

- `LikelinessTest`: drop and gain per draw; clamp at MIN and MAX; fixed section unchanged and still triggers gain for the others; disabled section never drawn (10,000 draws); first draw from a fresh state follows the initial weights within 1 percentage point (100,000 seeded fresh states); no adaptive section ever below 1; same seed gives the same sequence.
- Spread test: 10,000 seeded runs of 40 draws with default weights: mean per-section standard deviation of counts at most 1.8 (fixed equal weights give about 2.2), and at least 99.9 % of runs draw every standard section at least once (simulated: 99.97 %; with fixed equal weights about 99.6 %).
- `PipeStackTest`/`GameEngineTest`: the likeliness carries over between placements and resets on restart; two engines with the same seed deal the same stack after the same placements.
- Parser/validator tests for `stack.adaptive` and the new limits; every bundled level still parses, validates and is solvable.

## Risks / Trade-offs

- **Balance of existing levels changes.** Corners are now as common as straights (10 vs. 3/2 before) and the cross share shifts slightly. Mitigation: re-tune factor above, and the headless level tests still pass; star thresholds may need play-testing.
- **Still random.** A run of the same section stays possible, only less likely. A hard guarantee (bag shuffle) was rejected because the issue asks for likeliness, and a bag is predictable to experienced players.
