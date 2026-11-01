## Why

The game ships 96 levels, four full library pages. Players who finish them have nothing left to play. The maintainer wants a fifth page of 24 levels with a different feel from the large, slow late levels: small grids, a generous head start of around 30 seconds, but liquid that runs clearly faster than anywhere before, so the challenge is speed rather than size. The levels must stay doable.

Tracked in [#162](https://github.com/hexmasternl/leak-out/issues/162).

## What Changes

- 24 new bundled levels, ids 97 to 120, filling library page 5. Page 5 opens through the existing page unlock rule (50 stars on page 4).
- Small grids: 6 to 12 columns and 5 to 8 rows.
- Initial delay between 28 and 32 seconds (about 30 seconds).
- Base flow speed from 0.5 cells per second on level 97 rising to 0.9 on level 120, well above the 0.4 of level 96.
- Reservoirs in these levels slow the liquid to 10 % (`flow.reservoirSpeedFactor` 0.1), the strongest slow-down, in line with the reservoir types proposed in `app-game-reservoir-types` (#163). 0.1 is also valid under today's rules.
- The pack mixes the existing features (cross, reservoir, mandatory sections, portals) on top of the speed; no new mechanics.
- Every new level is solvable and doable: its shortest route can be placed at one section per 1.5 seconds before the liquid reaches the end of it.
- Star thresholds follow the designer's rule (calculated from free cells).

## Capabilities

### New Capabilities

- `bundled-levels`: rules for the bundled level set as a whole, starting with the fast-flow pack of levels 97 to 120.

### Modified Capabilities

None. The level library already pages by 24 and unlocks pages by stars; the level model already allows these grids and speeds.

## Impact

- `app/src/main/assets/levels/level_097.json` to `level_120.json`: new.
- `core` tests: `BundledLevelsTest` (count, pack rules, doability), `LevelSolvabilityTest` runs over the new files.
- Achievements: none depend on the total number of levels; no pack achievement is added for page 5 (only page 1 has one).
- Server: no change. Scores are stored per level id and no level id range is enforced.
- Play release notes mention the new levels.
