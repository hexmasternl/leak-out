# Proposal

## Why

The level designer cannot place mandatory sections, so a designed level can never use the `mandatory` field that the game, the level format and `LevelValidator` already support (issue #65). The star thresholds are typed in by hand, so they have no relation to the size of the level. They should follow from the number of free cells.

## What Changes

- Two new designer tools, **Mandatory cross** and **Mandatory reservoir**. Tapping a free cell places a mandatory section of that kind. Tapping a mandatory section of the same kind removes it, and tapping one of the other kind changes its kind. Only `X` and `RES` can be placed. The tools refuse blocked cells, the start, the finish and a seventh section (`LevelValidator.MAX_MANDATORY_SECTIONS` is 6). A drag with these tools only acts on the cell it starts on.
- The Start, Finish and Block tools keep refusing a cell that holds a mandatory section (current `LevelDraft` behaviour, now specified).
- **BREAKING (designer only)** The star thresholds are no longer editable. The designer calculates them from the free cells (every cell except blocked cells, the start and the finish; mandatory cells count as free):
  - 3 stars = free cells × 10
  - 2 stars = 75% of the 3-star value, rounded to the nearest 10 (halves round up)
  - 1 star = 60% of the 3-star value, rounded to the nearest 10 (halves round up)
  They are recalculated after every edit to the grid, including a copy of a bundled level. The panel shows them read-only. With fewer than 4 free cells the values are not strictly increasing, and the existing validation reports the design as invalid.
- The blank level's star thresholds become the calculated 200, 250 and 330 (33 free cells) instead of 50, 80 and 120.
- Mandatory sections are already drawn on the designer grid, written by `LevelWriter`, kept in the saved draft and played in test play. This change specifies that behaviour and covers it with tests.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `level-designer`: new requirements for the mandatory section tools and the calculated star thresholds; the blank level, the start/finish placement, the Block tool and the editable fields change accordingly. The capability is introduced by the not yet archived `app-game-level-designer` change, which has to be archived first.

## Impact

- `core/.../level/LevelDraft.kt`: mandatory placement operation, star calculation applied after every grid edit, blank level stars.
- `app/.../ui/designer/LevelDesignerViewModel.kt`: two new `DesignerTool` entries, drag handling for them, star `NumberField`s removed, copied and restored drafts recalculated.
- `app/.../ui/designer/LevelDesignerScreen.kt`: tool buttons, read-only star values; new strings.
- Tests: `LevelDraftTest`, `LevelDesignerViewModelTest`, `LevelDesignerScreenTest`, `TestPlayScreenTest`.
- No change to the level format, the parser, the validator or the game.
