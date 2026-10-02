## Why

The game has numbered levels, scores and star benchmarks, but nothing remembers what the player has achieved. Level select, unlocking and the level-complete screen all need to know which levels were finished, the player's personal best score per level and the stars that score earned. Progress has to survive app restarts, so it needs a local database.

## What Changes

- Add a local on-device database that stores one history record per level the player has successfully finished.
- A record holds the level id, the personal highscore for that level and the number of stars (0-3) earned with that highscore.
- When a level is finished successfully, the result is recorded: a first completion creates the record, a later completion only updates it if the new score beats the stored highscore (stars are updated together with the score).
- A failed level (leak) never creates or changes a record.
- Provide read access: the record for one level, all records, and whether a level has been finished, for use by level select and the level-complete screen.
- Provide a way to tell whether a finished game set a new personal best, so the UI can show it.
- No accounts and no network; data stays on the device.

## Capabilities

### New Capabilities
- `game-level-history`: The local persistent record of finished levels, with the personal highscore and stars per level, including the rules for recording a result and reading history back.

### Modified Capabilities

None. Star thresholds come from the `game-level-model` change (in progress); this change only consumes them as an input and does not alter that spec.

## Impact

- New code in the Android app module: history entity, DAO/repository and a small recording service, with unit and instrumented database tests.
- New dependency: Room (SQLite) with KSP. This replaces the plan's "DataStore for per-level best score and stars" in `docs/project-plan.md` section 2.10; DataStore stays for settings.
- Later work depends on it: level select (completed status and stars), unlock logic and the level-complete overlay.
- Depends on `game-level-model` for level ids and star thresholds.
