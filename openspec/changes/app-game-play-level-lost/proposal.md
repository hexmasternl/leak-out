## Why

The liquid can already be steered through placed sections, but nothing happens when it runs out of the route: the game has no way to end in a loss. A level without a way to lose is not a puzzle. The plan (sections 1.1 and 1.9) says a leak ends the level and the player can try again, so the game needs a clear leak rule and a clear "you lost" moment with a way forward.

## What Changes

- The level is **lost** when the liquid leaves a section and has nowhere valid to go. The pipe is leaking. This happens when the liquid leaves a section into:
  - an empty tile, a blocked tile or the start tile;
  - the edge of the grid;
  - a placed section that has no opening on the side it enters;
  - the finish from a side other than its open side;
  - a channel of a cross that the liquid already used.
- The leak is also checked when the liquid leaves the **start** tile: if the tile next to the start's open side cannot take the liquid, the level is lost as soon as the flow starts.
- The points for the section the liquid just left are awarded before the loss (as `app-game-play-points` already requires).
- At the moment of the leak the game **stops**: the flow, the countdown and the score freeze, and the player can no longer drag, place or replace sections. A drag that is going on is cancelled. The cog wheel and the fast forward button do nothing.
- The leak is shown on the field: liquid spurts from the open side where it leaked, for a short moment (1 second).
- Then a **level lost** modal dialog appears above the dimmed game screen. It says clearly that the level is lost ("Leaked!" with a short line such as "The pipe is leaking. Level lost."), shows the score of the attempt, and has two buttons:
  - **Restart**: closes the dialog and starts the same level again from the beginning (empty board, new stack, score 0, countdown from the start, fast forward off).
  - **Exit**: closes the dialog and returns to the main menu (welcome screen).
- The dialog can only be closed with its buttons. Tapping outside does nothing. The system Back button works the same as **Exit**.
- When the app goes to the background after the loss, the level lost dialog (not the pause dialog) is shown again when the player comes back.

## Capabilities

### New Capabilities
- `game-play-leak`: The leak rule: when leaving a section (or the start) ends in a leak, and at which tile and side the leak happens.
- `game-play-level-lost`: What a loss does to the game (everything stops), the leak effect, the level lost dialog with its text, score, Restart and Exit, and how Back, tapping outside and going to the background behave.

### Modified Capabilities

None. The HUD, pause, placement, fast-forward and points specs are still in open changes and not in `openspec/specs/`. Their requirements stay the same: a lost level is one more state in which placing is refused and the game clock stops, and restart already resets everything.

## Impact

- New pure Kotlin code in `core` (`nl.hexmaster.leakout.core.flow`): a `FlowStep` that decides, for the liquid leaving a tile through a side, whether it enters the next tile, reaches the finish or leaks, with JVM unit tests. It uses `PipeSection.exitFor(entry)` from `app-game-play-fast-forward`.
- `GameSession` gets an outcome (`Playing` or `Lost(at, side)`), a `liquidLeaked(at, side)` entry point and a lost-at time for the leak effect. `place`, `tick` and `pause` respect the lost state, and `restart` clears it.
- New Compose code in `app` (`ui/game/`): `LevelLostDialog`, a leak spurt drawn on `PlayingField`, string resources, and unit and instrumented tests.
- There is no flow simulation yet. The flow change calls `FlowStep` each time the liquid leaves a tile and calls `liquidLeaked` when it leaks. Until then the tests call it directly.
- This change builds on `app-game-play-stack`, `app-game-play-hud`, `app-game-play-playing-field`, `app-game-play-fast-forward` and `app-game-play-points` and must merge after them.
- `docs/project-plan.md` sections 1.9 and 1.11 are updated with the level lost dialog (Restart and Exit to the main menu).
