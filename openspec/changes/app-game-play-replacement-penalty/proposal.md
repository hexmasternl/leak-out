## Why

Replacing a placed section is nearly free right now. The new section appears at once, and the only cost is a 500 ms placement lock, which the player hardly notices. That makes replacing a reflex instead of a decision, and it undercuts the core tension of the game: you place sections before the liquid arrives. Replacing should cost real time and feel deliberate, and the player should see and hear that cost.

## What Changes

- Replacing a section becomes a **replacement sequence** that lasts the level's replace penalty:
  1. The old section breaks apart in an animation and the `section_destroy` sound plays.
  2. When the penalty has passed, the new section lands on the tile and the `section_placement` sound plays.
- During the sequence the player cannot interact with the game. No tile accepts a section, sections cannot be dragged from the stack, taps on the field are ignored without the "placement failed" sound, and fast forward cannot be toggled. Pause stays available, and pausing freezes the sequence.
- During the sequence the tile is empty. The liquid keeps flowing on the game clock, and if it reaches the empty tile before the new section lands, it leaks there by the normal rules.
- The sequence runs on the game clock, so it freezes while the game is paused. It ends early when the attempt ends (won, lost or restarted).
- **BREAKING** (gameplay tuning): the default `rules.replacePenaltyMs` goes from 500 to 3000. It stays configurable per level, and a value of 0 replaces at once and plays both sounds.
- The section taken from the stack leaves the stack when it is dropped, as it does today. It is held by the sequence until it lands.

## Capabilities

### New Capabilities
- `game-play-replacement-penalty`: the replacement sequence: its phases (break, wait, land), timing on the game clock, the input lock, the break and landing animations, the sounds, how the sequence behaves on pause, fast forward and the end of an attempt, and what happens when the liquid reaches the tile during it.

### Modified Capabilities
- `game-play-placement`: "Replacing a placed section" changes. The old section leaves the board at once and the new one lands only when the penalty has passed, instead of the swap being instant followed by a lock.
- `game-level-model`: the default `rules.replacePenaltyMs` changes from 500 to 3000, and the field now describes the length of the replacement sequence.
- `game-play-fast-forward`: the route is also checked when a replacement section lands. A replacement that is still running counts as an empty tile on the route.

## Impact

- `core`: `GameState` gets a pending replacement (tile, old section, new section, start time and landing time). `GameEngine.place` starts it, and `GameEngine.tick` lands it, splitting the tick at the landing time so that tick length doesn't change the outcome. The `LevelDefaults.REPLACE_PENALTY_MS` default changes. Engine, placement and parser tests are updated.
- `app`: `GameSession` and `GameSounds` play the landing sound when a pending replacement lands. `PlayingField` draws the break animation and the landing, and shows the tile as busy. The stack drag, field taps and the fast forward button are disabled while a replacement runs.
- `docs`: `docs/level-format.md` and `docs/project-plan.md` get the new default and the new behaviour.
- Depends on the sound effects from `app-game-sound-effects`, which is merged but not yet archived: `GameSound.SectionDestroyed` and `GameSound.SectionPlaced`.
- No new dependencies and no new assets. The break animation is drawn procedurally.
