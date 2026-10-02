# Design

Tracking issue: [#35 Random tile stack population needs to be less random](https://github.com/hexmasternl/leak-out/issues/35).

## Context

`PipeStack.create` and `PipeStack.afterPlacement` draw from a `WeightedPool` built from `StackSettings.weights`. `GameEngine.place` builds a new `WeightedPool` on every placement, so nothing about earlier draws is remembered. Defaults are `H`/`V` 3, corners 2, `X`/`RES` 0. Every bundled level overrides only `X` (0, 1 or 2) and `RES` (0). The only impure input of the engine is its `Random`, which tests seed.

Play-testing showed the real problem: a section that does not show up for a very long time. The metric that matters is therefore the **longest drought** (draws between two deals of the same standard section), not only the spread of counts.

## Goals / Non-Goals

**Goals:** a hard upper bound on how long a standard section can be missing; standard sections come out nearly evenly spread; fewer back-to-back repeats; special sections keep the share the level designer gives them, for the whole level; the sequence still feels random; same seed, same sequence; old level files keep parsing.

**Non-Goals:** a deterministic "bag" shuffle; per-level tuning of the steering strength or drought factor; changing how many sections the stack shows (that is #37); board-aware dealing (favouring what the open pipe end needs), which would start playing the puzzle for the player; guaranteeing a solvable sequence.

## Decisions

### 1. Why not the likeliness rule from the issue

The issue proposed: every section starts at likeliness 10, −3 when drawn, +1 for every other section, clamped to 1..20. Simulated over 10,000 seeded runs of 40 draws, it improves the spread (2.24 → 1.58) but barely touches droughts (worst 1 %: 36 → 32 draws).

The cause is drift. With six sections, each is drawn about 1 in 6 times, so on average it moves −3 once and +1 five times: +⅓ per draw. All values climb to the cap of 20 within about 30 draws. At the cap a section dealt 3 draws ago is back at 20, the same as one missing for 25 draws, so the rule only remembers the last 2–3 draws, exactly when steering is needed most. The growing total (60 → about 105) also makes a fixed cross rarer during a level (7.8 % → 5.3 % of draws for weight 6), and an adaptive rare section climbs to corner level. A zero-sum variant (−5 / +1) stops the drift and does better (worst 1 %: 25), but deficit steering is the principled version of the same idea and adds a guarantee.

### 2. Deficit steering

State, per attempt: the number of draws so far `n`, and per allowed section `s` the count `dealt[s]` and the drought `age[s]` (draws since `s` was last dealt, 0 at the start). Sections with weight 0 are *disabled*: they have no state and are never drawn.

Draw:

1. `share[s] = weight[s] / Σ weight` over the allowed sections.
2. `deficit[s] = share[s] × n − dealt[s]`.
3. Draw `s` with probability proportional to `share[s] × e^(k × deficit[s])`, where `k` is `LevelDefaults.STEERING_STRENGTH` (1.0). Use `random.nextDouble()` scaled to the total and `StrictMath.exp`, so the same seed gives the same sequence on every JVM.
4. Update: `n += 1`, `dealt[s] += 1`, `age[s] = 0`, every other `age[t] += 1`.

Properties:

- **No drift, no clamps.** `Σ deficit = n − n = 0` after every draw. A section can never be eliminated (its weight is always above 0) and the expected share of each section is exactly its target share at every point of the level.
- **Memory where it matters.** A section that is behind gets more likely the longer it stays behind. A section dealt twice in a row is pushed back.
- **One knob.** `k` = 0 is today's pure random; a large `k` approaches a fixed rotation. `k` = 1 gives most of the benefit while the next section stays hard to guess.
- **The first draw follows the weights exactly** (all deficits are 0), so the initial stack is drawn through the same rule and is already balanced.

### 3. Drought limit

Before step 3, if a standard section (`H`, `V`, `LT`, `LB`, `RT`, `RB`) has `age[s] ≥ limit[s]`, it is dealt instead of drawing at random, with `limit[s] = ⌈DROUGHT_FACTOR / share[s]⌉` and `DROUGHT_FACTOR` 2.5. If several sections are over their limit, the one with the longest drought is dealt; a tie goes to the first in `PipeSection` order. The forced deal does not consume a random value and updates the state like any other draw.

The limit is relative to the share, so it means "2.5 times the expected gap" on every level: 15 draws with six equal standard sections, 17 next to a cross at weight 3, 18 next to a cross at weight 6. A level that gives one standard section a tiny weight does not get it forced far more often than its share. The cross and reservoir are exempt: a designer who makes the reservoir rare means it.

### 4. Defaults and re-tuning the bundled levels

`stack.weights` keeps its keys and its rules (standard sections ≥ 1, specials ≥ 0). It now explicitly means *target share*. The standard default becomes 7 each (was 3 and 2), so the six standard sections are dealt evenly by default. 7 is chosen because the standard total, 42, is exactly 3× today's 14: the bundled levels' cross weights are re-tuned ×3 (`X` 1 → 3, `X` 2 → 6) and keep exactly today's cross share (1/15 = 6.7 % and 2/16 = 12.5 %). No upper bound is added to the weights and there is no new field.

### 5. Simulation

10,000 seeded runs of 40 draws (one level), six standard sections:

| | Std. dev. of counts | Longest drought, p99 / max | Back-to-back repeats | Next section guessable* |
|---|---|---|---|---|
| Pure random (today) | 2.24 | 36 / 40 | 16.7 % | 16.7 % |
| Likeliness rule from the issue | 1.58 | 32 / 40 | 13.8 % | 20.5 % |
| Zero-sum likeliness (−5 / +1) | 0.92 | 25 / 34 | 11.7 % | 26.2 % |
| Deficit steering, `k` 1 | 0.66 | 21 / 26 | 10.5 % | 31.4 % |
| **Deficit steering + drought limit** | **0.65** | **15 / 15** | **10.4 %** | 32.5 % |
| 6-piece bag (rejected) | 0.47 | 10 / 10 | 2.5 % | 39.1 % |

\* How often an observer who knows the algorithm and the history guesses the next section right.

With a cross at weight 3 or 6 next to standard weights of 7, the cross share is 6.8 % and 12.5 % (targets 6.7 % and 12.5 %), stable over 200 draws. The drought limit fires 0.3–0.5 times per 40 draws, so it is a safety net, not the main mechanism.

### 6. State in the stack, not in the engine

A new immutable `Dealer` value (`core/.../stack/Dealer.kt`) holds the shares, `n`, `dealt` and `age`, with `Dealer.initial(settings)` and `deal(random): Pair<PipeSection, Dealer>`. `PipeStack` gets a `dealer` property; `PipeStack.create(settings, random)` draws the initial `visibleSize` sections through it; `afterPlacement(random)` no longer takes a pool. `GameEngine.place` drops its `WeightedPool(...)`. `GameState` already copies `stack`, so restart (which calls `start`) resets the dealer and a pause/resume keeps it. `WeightedPool` is deleted with its test; its "weight 0 never drawn" and distribution assertions move to `DealerTest`.

`Level.isAllowed` keeps meaning "weight > 0".

### 7. Tests

- `DealerTest`: first draw from a fresh dealer follows the weights within 1 percentage point (100,000 seeded fresh dealers); disabled section never drawn (10,000 draws); counts, `n` and droughts update per draw; a section that is behind is more likely than one that is ahead (constructed state); drought limit forces the section with the longest drought, tie by `PipeSection` order, and never forces `X` or `RES`; same seed gives the same sequence.
- Statistical tests, 10,000 seeded runs of 40 draws with default weights: no standard section is ever missing for more than 15 draws; mean per-section standard deviation of counts at most 0.8; back-to-back repeats at most 12 %. With `X` 6: the cross share is within 1 percentage point of 12.5 %.
- `PipeStackTest`/`GameEngineTest`: the dealer carries over between placements and resets on restart; two engines with the same seed deal the same stack after the same placements.
- Level model tests for the new defaults; every bundled level still parses, validates and is solvable.

## Risks / Trade-offs

- **Balance of existing levels changes.** Corners are now as common as straights (7 each vs 3/2 before). Mitigation: cross shares are kept exactly, and the headless level tests still pass; star thresholds may need play-testing.
- **More predictable than pure random.** An expert who counts sections can guess the next one about 1 in 3 times instead of 1 in 6. That is the price of fairness and still far from a bag. `k` lowers it if play-testing says so.
- **Late-level memory.** Deficits count from the start of the attempt, so a long level still remembers an early imbalance. Steering keeps deficits close to zero, so this stays small; a decaying window is a later option, not needed now.
- **Floating point in `core`.** Steering uses doubles; `StrictMath.exp` and a seeded `Random` keep it deterministic.
