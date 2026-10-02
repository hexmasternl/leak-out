# Tasks

## 0. Prerequisites

- [x] 0.1 Make sure `app-game-level-designer` is archived (this change modifies the `level-designer` spec it introduces), or archive it first; verify with `openspec list --specs` that `level-designer` exists

## 1. Core: mandatory placement

- [x] 1.1 Add `LevelDraft.toggleMandatory(cell, section)` as in design.md (add, remove same kind, change kind in place, refuse outside the grid, blocked cells, endpoints and a seventh section, throw for sections other than `X` and `RES`); update the class KDoc
- [x] 1.2 Add `LevelDraftTest` cases for every scenario of "Mandatory section tools" and "Where mandatory sections can be placed", plus "Endpoint onto a mandatory section" and "Mandatory sections are skipped"

## 2. Core: calculated stars

- [x] 2.1 Add `Level.freeCells` and `StarBenchmarks.forFreeCells(n)` with integer half-up rounding; test with 60 → 360/450/600, 59 → 350/440/590, 33 → 200/250/330, 10 → 60/80/100, 3 → 20/20/30 and 0 → 0/0/0
- [x] 2.2 Make the `LevelDraft` constructor set the calculated stars for any level, remove `withStars`, and make `LevelDraft.new` produce 200/250/330; update `LevelDraftTest` (blank level, recalculated after resize, block, endpoint move and clear blocked, unchanged after placing a mandatory section, invalid with 3 free cells)
- [x] 2.3 Run `./gradlew :core:test` and make sure everything passes

## 3. App: view model

- [x] 3.1 Add `DesignerTool.MANDATORY_CROSS` and `MANDATORY_RESERVOIR`, wire them in `onCellTap`, and make `onDrag` ignore them; copy and restore get calculated stars through the `LevelDraft` constructor
- [x] 3.2 Remove `NumberField.StarOne/Two/Three` and their branches
- [x] 3.3 Add `LevelDesignerViewModelTest` cases: place/remove/change with each tool, drag places only the first cell, copy of a bundled level recalculates stars and keeps mandatory sections, export JSON contains the mandatory list, restore from saved state keeps mandatory sections and the selected mandatory tool
- [x] 3.4 Run `./gradlew :app:testDebugUnitTest` and make sure everything passes

## 4. App: screen

- [x] 4.1 Add the two tool chips (strings `designer_tool_mandatory_cross` "Mandatory cross" and `designer_tool_mandatory_reservoir` "Mandatory reservoir"), on a second tool row; update `toolLabel`
- [x] 4.2 Replace the star inputs with read-only values tagged `designer_star_one/two/three`; remove the unused star field strings and update the preview
- [x] 4.3 Add `LevelDesignerScreenTest` cases: tapping a cell with the Mandatory cross tool places it, the star values update after blocking a cell and cannot be edited; add a `TestPlayScreenTest` case that a test play shows the mandatory section
- [ ] 4.4 Run only the affected instrumented tests (`LevelDesignerScreenTest`, `TestPlayScreenTest`) on the emulator and make sure they pass

## 5. Wrap up

- [ ] 5.1 Run `openspec validate app-game-level-designer-mandatory-and-stars --strict` and fix any issue
- [ ] 5.2 Reference issue #65 in the pull request
