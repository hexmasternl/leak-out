## Why

The liquid can leak and lose the level, but it cannot win one yet: nothing happens when the liquid reaches the finish. A puzzle needs its payoff. The plan (sections 1.1 and 1.9) says reaching the finish wins the level and shows a level-complete screen with the score and stars, and the level library and level history both wait for a win to exist. This change adds the win, the celebration and the final score.

## What Changes

- The level is **won** when the liquid leaves the last section and enters the finish through its open side. The points for that last section are awarded first (as `app-game-play-points` already requires).
- At the moment of the win the game **stops** the same way it stops on a loss: the flow, the countdown and the score freeze, the player can no longer drag, place or replace sections, a running drag is cancelled, and the cog wheel and fast forward button do nothing. Panning and zooming the field still work.
- The finish tile flashes in the success colour for a short moment (1 second), then a **level complete** modal dialog appears above the dimmed game screen. It celebrates the win ("Level complete!" with a short cheerful line) and shows the score in three rows and three stars, plus the buttons **Next level**, **Restart** and **Exit**.
- The **final score** is calculated from the attempt:
  - **Earned points**: the points the liquid collected on its way, as shown in the HUD.
  - **Unused sections**: every placed section the liquid never entered costs **3 points**.
  - **Total**: earned points minus the penalty, never below 0.
  - **Stars**: the level's star thresholds applied to the total (0 to 3), with the same rule the HUD uses.
- The score is **animated** in this order, so the player sees where the number comes from:
  1. The earned points appear at once when the dialog opens.
  2. The penalty counts down in steps of 3, and with every step one unused section disappears from the playing field behind the dialog, until all unused sections are gone.
  3. The total runs quickly from 0 to the final total.
  4. The stars are revealed: all three start grey, then each earned star lights up in brass one after the other, popping to a larger size and settling back.
- The three buttons are visible from the moment the dialog opens but **disabled** until the score and star animations are done. Then they are enabled:
  - **Next level**: closes the dialog and starts the next level. It stays disabled when there is no next level.
  - **Restart**: closes the dialog and starts the same level again from the beginning.
  - **Exit**: closes the dialog and returns to the main menu (welcome screen).
- The dialog can only be closed with its buttons. Tapping outside does nothing. The system Back button does nothing while the animations run and works as **Exit** once the buttons are enabled.
- When the app goes to the background after the win, the animations are skipped, and the finished dialog (not the pause dialog) is shown when the player comes back.
- The plan's section 1.9 gets the unused-section penalty and the final score rule; section 1.11 gets the level complete dialog with Next level, Restart and Exit.

## Capabilities

### New Capabilities
- `game-play-level-complete`: The win rule (liquid enters the finish through its open side), what a win does to the game (everything stops), the finish flash, the level complete dialog with its text, rows, stars and buttons, when the buttons are enabled, what Next level, Restart and Exit do, and how Back, tapping outside and going to the background behave.
- `game-play-final-score`: The final score of a won attempt: earned points, the 3-point penalty per unused section, the total (never below 0), the stars from the total, and the order and timing of the score and star animations, including the removal of unused sections from the field.

### Modified Capabilities

None. The leak, level-lost, points, HUD, pause, fast-forward, placement and stack specs are still in open changes and not in `openspec/specs/`. Their requirements stay the same: a won level is one more state in which the game is stopped, and `game-play-level-lost` already describes that state for a loss.

## Impact

- New pure Kotlin code in `core` (`nl.hexmaster.leakout.core.score`): `Points.UNUSED_SECTION_PENALTY` and a `FinalScore` that finds the unused sections on a `Board` and computes penalty, total and stars, with JVM unit tests.
- The game engine (`core.engine`, from `app-game-play-game-engine`) already reports `Outcome.Won` and stops the game on it. `GameState` gets the final score of the attempt and a `RemoveUnused` command that removes unused sections; `GameSession` sends it in step with the animation and can skip the animation (design decision 11).
- New Compose code in `app` (`ui/game/`): a pure `WinTally` timeline (which row shows what at a given age), `LevelCompleteDialog`, a `WinStars` row built on the HUD star drawing, a finish flash on `PlayingField`, a success-green `CartoonButton` style, string resources, and unit and instrumented tests.
- `GameScreen` gets an `onNextLevel` callback next to `onExit`; `MainActivity` wires it to the bundled level with the next id.
- There is no flow simulation yet. The flow change calls `FlowStep.after` each time the liquid leaves a tile and calls `liquidReachedFinish` on `Finish`. Until then the tests call it directly.
- Recording the result in the level history, the "new best" badge and unlocking the next level are not part of this change. `app-game-level-history` and `app-game-level-library` wire them to the win when they merge. The stars this change shows can be 0, while the history spec requires at least 1; see the design.
- This change builds on `app-game-play-stack`, `app-game-play-hud`, `app-game-play-playing-field`, `app-game-play-fast-forward`, `app-game-play-points` and `app-game-play-level-lost` and must merge after them.
- `docs/project-plan.md` sections 1.9 and 1.11 are updated.
