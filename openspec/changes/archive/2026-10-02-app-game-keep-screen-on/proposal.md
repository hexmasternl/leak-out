# Proposal

Tracking issue: [#52 When in game (the game screen is open) always on](https://github.com/hexmasternl/leak-out/issues/52).

## Why

The game follows the device's normal screen timeout. A player who is thinking about the next placement, watching the countdown or reading the pause dialog does not touch the screen, so the display dims and switches off in the middle of a level (often after 30 s or 1 min). Because going to the background pauses the game, the player then has to unlock, find their place and resume: the timeout interrupts exactly the moment the game asks them to think.

## What Changes

- While the **game screen is open**, the app asks Android to keep the display on. That covers every state of a level: the countdown, the flowing liquid, the pause dialog, the leak effect and finish flash, and the level lost and level complete dialogs. Next level keeps the game screen open, so the screen stays on.
- As soon as the game screen closes (Exit or Back to the main menu), the normal screen timeout applies again. The main menu, level library and settings never keep the screen on.
- When the app goes to the background (Home, app switcher, power button), nothing is held: Android only honours the request for a visible window, and the game already pauses itself.
- No setting, no permission (`WAKE_LOCK` is not needed), no `core` change.

## Capabilities

### New Capabilities
- `game-play-screen-awake`: the display stays on while the game screen is shown and follows the system timeout everywhere else.

### Modified Capabilities
<!-- none -->

## Impact

- `app/src/main/kotlin/nl/hexmaster/leakout/ui/game/GameScreen.kt`: requests keep-screen-on for as long as it is composed (a small `KeepScreenOn` composable in `ui/components`).
- `docs/project-plan.md`: one line on the screen staying on during play.
- Tests: instrumented tests that the activity window carries the keep-screen-on flag while the game screen is shown (playing, paused, result dialog) and loses it once the game screen is left.
- No dependency, permission, manifest, level-format or engine change.
