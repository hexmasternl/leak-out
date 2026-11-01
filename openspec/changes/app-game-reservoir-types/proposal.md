## Why

Every reservoir slows the liquid down to the same 30 % of its speed, and every reservoir looks the same: a violet tank. The level file can already set `flow.reservoirSpeedFactor` per level, but no bundled level uses it and the player cannot see it. On the fast late levels a 30 % reservoir buys too little time, while on the slow early levels it hardly matters. The maintainer wants four recognisable reservoir types, each with its own slow-down and accent colour, and the bundled levels tuned so that the faster the liquid runs, the stronger the reservoirs slow it down.

Tracked in [#163](https://github.com/hexmasternl/leak-out/issues/163).

## What Changes

- **BREAKING** `flow.reservoirSpeedFactor` only accepts the four reservoir types: 0.1, 0.2, 0.3 and 0.4. The default stays 0.3. Any other value fails validation.
- Each reservoir type has an accent colour: 10 % red, 20 % blue, 30 % purple (the current violet), 40 % orange. The reservoir tile on the playing field and in the stack is drawn with the accent of the level's type, and its "slow" badge shows the percentage, so colour is never the only cue.
- The design system gets the accent tokens for the four types.
- The level designer replaces the free reservoir speed factor field with a choice of the four types. A design restored with another value snaps to the nearest type.
- The bundled levels get a reservoir type based on their base flow speed: the faster the liquid, the lower the factor.
- The reservoir tip keeps showing the level's percentage (it already reads the factor).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `game-level-model`: `flow.reservoirSpeedFactor` is limited to 0.1, 0.2, 0.3 and 0.4.
- `level-designer`: the reservoir speed factor is chosen from the four types; other values snap to the nearest type on restore.
- `pipe-section-assets`: the reservoir is drawn with the accent of the level's reservoir type and its badge shows the percentage.

## Impact

- `core/level/LevelValidator.kt`, `LevelDefaults.kt`: allowed factors; a `ReservoirType` (factor and accent id) in `core/level`.
- `core/level/LevelDraft.kt`: snapping to the nearest type.
- `app/ui/designer/*`: type selector instead of a number field.
- `app/ui/pipes/PipeArt.kt` and the reservoir SVG assets (`res.svg`, `res-glass.svg`): accent tint per type, percentage on the badge.
- `app/src/main/assets/levels/*.json`: `flow.reservoirSpeedFactor` added where it differs from 0.3.
- `docs/design-system.md`, `docs/level-format.md`.
- Tests: `LevelValidatorTest`, `LevelDraftTest`, `LevelDesignerViewModelTest`, `BundledLevelsTest`, pipe asset tests.
- Server: no change.
