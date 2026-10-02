## Why

Tracking issue: [#38 HUD enhancements](https://github.com/hexmasternl/leak-out/issues/38).

The HUD shows the right information but is hard to read at a glance. The flow speed "0.8/s" is a decimal the player has to parse. Nothing shows how long until the liquid starts, even though that countdown is the most urgent thing at the start of a level. The stars and the score are two separate items, so the player cannot see how far they are from the next star. The bar itself is a flat strip that does not stand out from the rest of the screen.

## What Changes

- **Level**: unchanged.
- **Flow speed**: the icon stays. The value becomes a whole number: the speed in cells per second times 10, rounded. A speed of 0.2 cells per second shows as "2", 0.8 as "8" and 1.25 as "13". This is display only; the engine and the level format keep cells per second.
- **Flow countdown bar** (new): a progress bar in the gap between the flow speed and the score. It starts full when the attempt starts and drains as the countdown before the flow runs down, reaching empty at the moment the liquid starts. It follows the game's flow time, so it freezes on pause and drains faster in fast forward. It warns in brass in the last 5 seconds and in red in the last 2. Once the liquid flows it fades out. A level with no start delay shows no countdown bar.
- **Score progress bar** (replaces the separate stars and score):
  - The bar's maximum is the level's three-star threshold (`stars.three`).
  - Two marker lines sit at the one-star (`stars.one`) and two-star (`stars.two`) thresholds.
  - The actual score is shown as a number centred on the bar.
  - The fill grows smoothly to the new score when points are gained, rather than jumping.
  - When the fill reaches the one-star, two-star and three-star thresholds, the bar sparkles briefly at that point.
  - Once the bar is full, the number keeps showing the actual score, which can go past `stars.three`.
- **HUD panel**: the whole bar is styled as a distinct panel that stands out clearly from the stack and the board.
- **Design first**: the new look and feel is first written into `docs/design-system.md` (section 5.3 HUD bar and the motion section) and approved by the product owner before any HUD code changes.
- **BREAKING** (UI): the three separate HUD stars and their pop-in animation are removed. Stars earned during play are read from the score bar's markers instead. The level complete dialog keeps its stars.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `game-play-hud`: the item order and alignment change; the flow speed is shown as a whole number; a flow countdown bar is added; the stars and the score are merged into a score progress bar with threshold markers, a smooth fill and threshold sparkles; the HUD panel styling is specified; the accessibility descriptions change to match. The "HUD shows the stars earned so far" and "Earned star pops in" requirements are removed.

## Impact

- `docs/design-system.md`: section 5.3 HUD bar is rewritten (it still describes an older design with a countdown ring and a 64 dp bar), and section 7.2 gets the countdown and score bar animations. This must be approved before implementation starts.
- `app`: `GameHud`, `HudFormat` and `HudLayout` change; `HudStars` is removed from the HUD (the shared `StarIcon` and `WinStars` stay for the level complete dialog). New composables for the countdown bar and the score progress bar. `GameScreen` passes the countdown (remaining and total start delay) and the level's star thresholds to the HUD. String resources for the new descriptions.
- `core`: no rule changes. The HUD reads the existing countdown (`FlowState.Waiting.remainingMs`) and the level's `flow.initialDelayMs` and `stars`.
- Tests: `HudFormat` unit tests, a pure unit-tested model for bar fractions, countdown warning levels and threshold crossings, and instrumented HUD tests updated for the new layout.
- No new dependencies and no new assets. The sparkle is drawn procedurally.
