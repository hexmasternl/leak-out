## Why

The grids of the later levels have grown far too large: from level 55 on every level is taller than 15 rows, and levels 71 to 96 are 24 by 24. On a phone in landscape the cells become tiny, so placing sections is fiddly and the playing field is hard to read. The level model still allows grids up to 50 by 50, so nothing stops this from happening again.

Tracked in [#159](https://github.com/hexmasternl/leak-out/issues/159).

## What Changes

- **BREAKING** The largest allowed grid drops from 50 by 50 to 25 columns by 15 rows. The smallest stays 4 by 4. Levels with more columns or rows fail validation.
- The level designer's column stepper stops at 25 and its row stepper at 15. A design restored at a larger size (kept from before an update) is shrunk with the existing shrink rules.
- The 42 bundled levels that are too large (levels 55 to 96; all of them have more than 15 rows, none has more than 25 columns) are redesigned to fit 25 by 15. Each keeps its id, name and features (mandatory sections, portals), its flow and stack stay as they were, and its star thresholds are recalculated from its free cells.
- Every redesigned level is checked to be solvable by the existing `LevelSolvabilityTest` and `BundledLevelsTest`.
- `docs/level-format.md` documents the new limits.
- Online top scores already submitted for the redesigned levels are kept as they are; scores set on the old, larger layouts may be out of line with the new ones. This is accepted.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `game-level-model`: the grid size range becomes 4 to 25 columns and 4 to 15 rows.
- `level-designer`: the grid size controls use the new range, and an oversized design is shrunk when restored.

## Impact

- `core/level/LevelValidator.kt`: `MAX_GRID_SIZE` splits into `MAX_GRID_COLUMNS = 25` and `MAX_GRID_ROWS = 15`.
- `core/level/LevelDraft.kt`: clamps columns and rows to their own ranges.
- `app/ui/designer/LevelDesignerViewModel.kt`: `canGrowColumns` and `canGrowRows` use the new limits.
- `app/src/main/assets/levels/level_055.json` to `level_096.json`: redesigned.
- `docs/level-format.md`: grid range.
- Tests: `LevelValidatorTest`, `LevelDraftTest`, `LevelDesignerViewModelTest`, plus the bundled level suites.
- Server: no change. Scores are stored per level id and are not tied to a layout.
