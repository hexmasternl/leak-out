# Proposal

## Why

After 96 levels the game has no new pieces to learn. A pump is the opposite of a reservoir: once the liquid has left it, the flow runs faster for the rest of the attempt. A pump scores more than a straight, so placing one is a choice the player makes: extra points against extra pressure. This change is one of the seven mechanics split out of [#102](https://github.com/hexmasternl/leak-out/issues/102), and it ships as its own chapter of 24 levels (one level library page).

Tracked in [#139](https://github.com/hexmasternl/leak-out/issues/139).

## What Changes

- A new pipe section, the **pump** (`PUMP`). Like the reservoir it has one channel, from left to right, and liquid can pass it in either direction.
- A pump is dealt from the stack with its own weight, `stack.weights.PUMP`, which is 0 by default. Only levels that give it a weight deal it. The drought rule never forces a pump.
- A new optional level field, `flow.pumpSpeedFactor`: default 1.5, greater than 1 and at most 3. Each time the liquid leaves a pump, the flow speed is multiplied by this factor for the rest of the attempt. Two pumps multiply twice. A pump never pushes the speed above 10 cells per second, the highest speed a level may have.
- A pump scores **40 points** when the liquid leaves it.
- A pump can be **mandatory**, next to the cross and the reservoir. A mandatory pump is used once the liquid has passed its channel.
- The HUD's flow speed includes the pump boost, and the speed number pulses briefly when a pump makes the flow faster.
- The pump is drawn from its own SVG asset with a glass overlay: a horizontal pipe with an impeller housing and a small "fast" badge, the counterpart of the reservoir's "slow" badge. The liquid fills its channel like a straight. Screen readers name it "Pump".
- A one-time **pump tip** explains the pump the first time one is fully visible in the stack, or the first time a level starts with a mandatory pump.
- The level designer gets a pump speed factor field, a weight field for `PUMP` and a Mandatory pump tool. Its live validation and solvability check understand pumps. The Pipe Lab shows the pump.
- A new chapter of **24 bundled levels** introduces the pump, on the next free level library page, unlocked by the existing page star rule. Every one of them passes the solver check.
- `docs/level-format.md` and `docs/design-system.md` describe the pump.
- Levels 1 to 96, their scores and saved progress do not change: no existing level has a pump, and a level without a pump plays exactly as before.

Related mechanics from #102: the fixed pipe ([#135](https://github.com/hexmasternl/leak-out/issues/135)) lays the "placed by the level" groundwork that the valve ([#136](https://github.com/hexmasternl/leak-out/issues/136)) and the portal ([#140](https://github.com/hexmasternl/leak-out/issues/140)) may reuse. This change does not depend on them: a pump placed by the level uses the existing mandatory sections. The order of the chapters is still an open question in #102; the issue suggests the pump chapter comes after the fixed pipe and valve chapters.

## Capabilities

### New Capabilities
- `game-play-pumps`: the pump section: how it connects, the speed boost after it and its cap, its look and liquid, how the HUD shows the faster flow, its accessibility description, the pump tip text, and the pump chapter of bundled levels.

### Modified Capabilities
- `game-level-model`: `PUMP` is a section id with a stack weight (default 0), `flow.pumpSpeedFactor` is a new optional flow field, and `PUMP` can be mandatory.
- `level-serialization`: the round trip covers a level with pumps and a pump speed factor.
- `level-solvability`: a mandatory pump is entered from the left or the right, and the solver never requires a pump from the stack.
- `game-play-flow`: the flow speed is multiplied by the pump speed factor once per pump the liquid has left, up to the speed cap.
- `game-play-points`: a pump scores 40 points.
- `game-play-stack`: the pump is never dealt by the drought limit.
- `game-play-mandatory-sections`: a mandatory pump behaves like a placed pump and is used once its channel is passed.
- `game-play-fast-forward`: the route check passes a pump only between left and right.
- `game-play-onboarding`: the pump tip is dismissed with "Got it" and comes last in the tip order.
- `game-play-hud`: the flow speed shown includes the pump boost.
- `level-designer`: pump speed factor and `PUMP` weight fields, and a Mandatory pump tool.
- `pipe-section-assets`: a `pump.svg` asset with one left–right channel, an impeller housing and a "fast" badge.
- `pipe-lab`: shows the pump tile.

## Impact

- **core**: `PipeSection.PUMP` (open sides left and right), `LevelDefaults` (`PUMP_SPEED_FACTOR`, `PUMP` weight 0), `LevelDto`/`LevelParser`/`LevelValidator`/`LevelWriter`, `LevelSolver` (mandatory pump), `Points`, `GameState.flowSpeed` (pumps left so far), `FlowSimulation`, `MandatoryProgress`, `RouteTracer`/`FlowStep`, `Dealer` (no drought for `PUMP`), `LevelDraft`, and every exhaustive `when` over `PipeSection` (for example `AttemptSummary`).
- **app**: `pump.svg` and `pump-glass.svg` in `assets/pipes/`, the pipe tile and liquid spans, cell descriptions, the HUD speed pulse, the tip scheduler and tip overlay, the level designer screen and view model, the Pipe Lab, string resources.
- **Levels**: 24 new `level_NNN.json` files in `app/src/app/src/main/assets/levels/`. No existing level file changes.
- **Docs**: `docs/level-format.md` (field, weight, mandatory pump, example, validation rules) and `docs/design-system.md` (pump colours, asset, tip text, HUD pulse).
- **Storage**: a new "pump tip seen" flag next to the other tip flags. No change to level history or the Room database.
- **Server**: none. The level ids of the new chapter are new level ids for the leaderboards; nothing changes for existing ones.
