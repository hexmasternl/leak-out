## Context

The level designer (`app-game-level-designer`, all tasks done, not archived) edits a `LevelDraft` in `core/.../level/LevelDraft.kt` through `LevelDesignerViewModel` with three tools: Start, Finish and Block. `LevelDraft` already knows about mandatory sections: a resize drops those outside the grid, `placeEndpoint` refuses a mandatory cell and `canBlock` skips one. `DesignerGrid` already draws them on the red-tinted tile, `LevelWriter` writes them, the saved state is `LevelWriter` JSON and test play passes the whole `Level` to `GameScreen`. Only a way to add and remove them is missing. Star thresholds are three `NumberField`s typed in by hand.

## Goals / Non-Goals

**Goals:**
- Place, remove and change mandatory `X` and `RES` sections on the designer grid, within the validator's rules.
- Calculate the star thresholds from the free cells after every edit, as pure `core` code.

**Non-Goals:**
- Changing the level format, `LevelValidator`, `LevelSolver` or how the game plays mandatory sections.
- Checking that the mandatory sections can all be used on one route. The solver does not consider them today; that stays a separate concern.
- A manual override for the calculated stars.

## Decisions

### `LevelDraft.toggleMandatory(cell, section)`
One operation covers both tools: on a cell holding `section` it removes the entry; on a cell holding the other kind it replaces the entry in place (same list index, so validation error indices stay stable); on a free cell it appends one, unless the cell is outside the grid, blocked, an endpoint, or `MAX_MANDATORY_SECTIONS` is reached. A `section` other than `X` or `RES` throws `IllegalArgumentException`: the UI never offers one, so it is a programming error. The 6-limit is enforced here instead of only reported by the validator, because a tool that silently builds an invalid level is worse than one that does nothing.

*Alternative:* let the Start, Finish and Block tools replace a mandatory section. Rejected: `LevelDraft` already refuses those cells, which is predictable and easy to test, and the developer can remove the section with its own tool first.

### Two `DesignerTool` entries
`DesignerTool` gains `MANDATORY_CROSS` and `MANDATORY_RESERVOIR`, each mapping to `toggleMandatory(cell, X | RES)`. They reuse the tool row and its test tags (`designer_tool_mandatory_cross`, `designer_tool_mandatory_reservoir`). In `onDrag`, the mandatory tools do nothing: `onDragStart` already taps the first cell, and toggling every cell passed would flicker sections on and off. Five tool chips may not fit one row on a phone, so the row becomes a wrapping `FlowRow`.

### Stars derived in `LevelDraft`
`StarBenchmarks.forFreeCells(n)` in `core` returns `one = roundTo10(6n)`, `two = roundTo10(7.5n)` and `three = 10n`. It is computed in integers: `three = 10n`, `two = (75n + 50) / 100 * 10`, `one = (60n + 50) / 100 * 10`, which rounds halves up for every n ≥ 0. `Level.freeCells` (grid cells minus distinct blocked cells inside the grid minus the two endpoints) is the input.

`LevelDraft` becomes a plain class whose constructor takes any `Level` and stores it with the calculated `stars`, so no operation, copied bundled level or restored draft can skip the rule. Equality is on that level, as the data class had it. `withStars` is removed. `LevelDraft.new` calculates them too, so the blank level gets 200, 250 and 330.

*Alternative:* keep the stars in the draft and recalculate only in the view model. Rejected: the rule belongs to the level model, and `core` tests cover it without Android.

### Read-only star display
The Stars section of the panel shows the three values as plain text with tags `designer_star_one/two/three`, instead of `NumberInput`s. `NumberField.StarOne/Two/Three` and their `apply`/`valueText` branches are removed. A fewer-than-4-free-cells design gets non-increasing thresholds and is listed as invalid by the existing validation.

## Risks / Trade-offs

- [Copying a bundled level and exporting it changes that level's stars] → Intended: designer output follows one rule. The developer sees the new values in the panel before exporting.
- [The calculated thresholds may not suit the scoring of every level, for example levels with many reservoirs] → Out of scope here; a later change can add an override if needed.
- [This change modifies a spec that only exists in an unarchived change] → Task 0.1 archives `app-game-level-designer` first.
