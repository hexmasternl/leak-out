## Why

The HUD already shows a score, but nothing ever changes it: the score stays at 0 for the whole level. Points are the player's reward for a long, clever route and the reason to take the risk of a cross or a reservoir. Without them, every winning route is worth the same. The flow is the next thing to be built, so the scoring rules and the feedback on the tile should be ready for it to plug into.

## What Changes

- The player earns points for every pipe section the liquid actually runs through. The points are awarded at the moment the liquid **exits** the section, not when it enters.
- Points per section:
  - Straight section (`H`, `V`): **10** points.
  - Corner section (`LT`, `LB`, `RT`, `RB`): **15** points.
  - Reservoir (`RES`): **25** points.
  - Cross (`X`): **10** points for the first pass, **50** points for the second pass, whichever channel is used first.
- The start and finish tiles are not pipe sections and never give points. A placed section the liquid never reaches gives no points.
- When the liquid exits a section and the next step is a leak, the points for that section are still awarded: the liquid did run through it.
- Each award is shown briefly on the tile of the section as "+10", "+15", "+25" or "+50", with a fade-in, a short hold and a fade-out. The popup moves with the tile when the player pans or zooms.
- The score in the HUD goes up by the same amount at the moment the popup appears.
- The popups and the score follow the game clock: while paused, a popup does not fade and no points are awarded. Restart clears all popups and sets the score back to 0.
- The scoring table in `docs/project-plan.md` section 1.9 is replaced by these values. The time bonus for starting the flow early stays in the plan but is not part of this change.

## Capabilities

### New Capabilities
- `game-play-points`: The points rules: which sections give points, how many, when they are awarded, the two passes of the cross, and how the total score of an attempt is kept, shown in the HUD and reset.
- `game-play-points-popup`: The points shown on the tile: text, placement, fade-in, hold and fade-out, following the tile during pan and zoom, and pausing and restarting.

### Modified Capabilities

None. The HUD spec (`game-play-hud`) is still in the open change `app-game-play-hud` and not in `openspec/specs/`. Its requirement "HUD shows the current score" already asks that the HUD shows every score change; this change is what makes the score change.

## Impact

- New pure Kotlin code in `core` (`nl.hexmaster.leakout.core.score`): the points table and a `ScoreSheet` that keeps the total and the number of passes per tile, with JVM unit tests.
- `GameSession` (from `app-game-play-hud`, changed by `app-game-play-playing-field`) gets a `ScoreSheet`, a list of active popups and a `liquidExited(position)` entry point. `score` is read from the sheet.
- New Compose code in `app` (`ui/game/`): a pure `PointsPopupAnimation` (alpha, rise and scale for a given age) and the popup drawing in `PlayingField`, with unit and instrumented tests.
- There is no flow simulation yet, so nothing calls `liquidExited` during play. The flow change will call it each time the liquid leaves a section. Until then the tests call it directly.
- This change builds on `app-game-play-stack`, `app-game-play-hud` and `app-game-play-playing-field` and must merge after all three.
- `docs/project-plan.md` section 1.9 is updated.
