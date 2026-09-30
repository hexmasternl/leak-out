## Why

The game screen shows the stack and the board, but the player cannot see which level they are playing, how fast the liquid flows or what they have scored. They also have no way to pause, restart or leave a level. The project plan (section 1.11) lists a speed indicator and a pause button on the game screen and a pause overlay with resume, restart and quit. This change adds both.

## What Changes

- Add a HUD bar along the top of the game screen, across the full width. The stack column and the board move down and fill the space below it.
- The HUD shows, from left to right:
  - the **current level** ("Level 3"), at the far left;
  - the **current flow speed**, directly to the right of the level;
  - the **stars earned so far**: three stars, directly to the left of the score. All three start grey. When the score reaches the level's first star threshold, the first star turns glowing brass-orange; the second and the third star do the same at their thresholds;
  - the **current score**, right-aligned, directly to the left of the cog wheel;
  - a **cog wheel** button, at the far right.
- Tapping the cog wheel pauses the game and opens a modal pause dialog with, from top to bottom:
  - **Resume**: closes the dialog and continues the game where it stopped;
  - **Restart**: starts the current level again from the beginning (new stack, empty board, score 0);
  - a separator;
  - **Exit**: leaves the level and goes back to the main menu (welcome screen).
- While the game is paused, nothing on the game screen moves: the flow and the countdown stop, and the player cannot drag a section. A drag that is going on when the game pauses is cancelled and the section goes back to its slot.
- The system Back button on the game screen opens the same pause dialog. Pressing Back while the dialog is open works the same as Resume. Tapping outside the dialog does nothing, so the player cannot close it by accident.
- When the app goes to the background during a level, the game pauses and the pause dialog is shown when the player comes back.

## Capabilities

### New Capabilities
- `game-play-hud`: The HUD bar at the top of the game screen: what it shows (level, flow speed, score, cog wheel), where each item is placed, and how the values stay up to date.
- `game-play-pause`: Pausing the game: the pause dialog opened from the cog wheel, Back or going to the background, and its Resume, Restart and Exit actions, and what "paused" means for the flow, the countdown and dragging.

### Modified Capabilities

None. The game screen and the stack are still in the open `app-game-play-stack` change and have no spec in `openspec/specs/`. The stack keeps its requirements: its column fits the height below the HUD instead of the full screen height, which `StackLayout` already handles through its `availableHeight` input.

## Impact

- New Compose code in `app` (`ui/game/`): `GameHud`, `HudStars`, `PauseDialog`, a pure `HudFormat` helper for the level, speed and score texts, and a `GameSession` state holder that owns the paused flag, the score and restart. Unit tests and instrumented tests on the emulator.
- The stars are derived from the score with the level's `stars` thresholds through `StarBenchmarks.starsFor(score)`, which already exists in `core`. During play they show what the player would earn if the level were won with the current score. Restart sets them back to grey together with the score.
- `GameScreen` (from `app-game-play-stack`) gets the HUD above the stack and board, and a new `onExit` callback. `MainActivity` navigates from the game screen back to the welcome screen on Exit.
- There is no `GameEngine`, flow or scoring yet. The HUD reads the score and flow speed from `GameSession`, which starts at score 0 and the level's `flow.cellsPerSecond`. The engine takes over those values later (plan section 2.6 already has `score`, `paused`, `Pause`, `Resume` and `Restart`).
- New string resources, a cog wheel vector icon, a star vector icon, and the brass and muted-text colour tokens from the design system in `LeakOutColors`.
