## 1. Grid limits in core

- [x] 1.1 In `core/level/LevelValidator.kt`, replace `MAX_GRID_SIZE` with `MAX_GRID_COLUMNS = 25` and `MAX_GRID_ROWS = 15` and check each dimension against its own range
- [x] 1.2 In `core/level/LevelDraft.kt`, clamp columns to 4..25 and rows to 4..15 and expose both limits
- [x] 1.3 Update `LevelValidatorTest` (25 by 15 accepted, 26 columns and 16 rows rejected with the right field) and `LevelDraftTest` (clamping per dimension, shrinking a 24 by 24 draft)

## 2. Level designer

- [x] 2.1 Point `canGrowColumns` and `canGrowRows` in `LevelDesignerViewModel` at the new limits
- [x] 2.2 Shrink a design that is larger than 25 by 15 when it is restored, using the existing `LevelDraft` resize
- [x] 2.3 Update `LevelDesignerViewModelTest`: steppers stop at 25 columns and 15 rows, an oversized design is shrunk on restore

## 3. Bundled levels

- [x] 3.1 Redesign levels 55 to 64 to fit 25 by 15, keeping id, name and features; keep flow and stack; recalculate stars from free cells
- [x] 3.2 Redesign levels 65 to 74 the same way
- [x] 3.3 Redesign levels 75 to 85 the same way
- [x] 3.4 Redesign levels 86 to 96 the same way
- [x] 3.5 Add a `BundledLevelsTest` check that every bundled level is at most 25 by 15, and run `LevelSolvabilityTest` and `BundledLevelsTest` until every level parses, validates and is solvable
- [x] 3.6 Keep the existing portal rules in `BundledLevelsTest` passing (portals from level 49, on fewer than half of levels 50 to 96)

## 4. Docs

- [x] 4.1 Update the grid range in `docs/level-format.md` (field reference and designer steps)

## 5. Verification

- [ ] 5.1 Run `:core:test` and `:app:testDebugUnitTest`
- [ ] 5.2 Play a sample of the redesigned levels on a device or emulator and check the cells are comfortably large in landscape
