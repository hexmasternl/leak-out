## Context

The project plan (`docs/project-plan.md`, sections 1.6, 2.6 and 2.8) describes a stack where the player taps a cell to place the **top** section and new sections are added at the **bottom**. This change turns that around. New sections enter at the top and the stack moves down. The bottom section is the next one, and the player **drags** it onto the board. Drag and drop is the only way to place a section.

What exists today:
- `core` has the level model (`nl.hexmaster.leakout.core.level`). `Level.stack` holds `visibleSize` (1-10, default 5) and a weight for each `PipeSection` (`H`, `V`, `LT`, `LB`, `RT`, `RB`, `X`, `RES`). There is no board, flow or `GameEngine` yet.
- `app` has a splash screen and a welcome screen. There is no game screen, board canvas or `PipeDrawer` yet. The app runs in landscape only.

So the stack is being built before the board. It needs a clear contract with the board, and it needs a stand-in board so it can be tested now.

## Goals / Non-Goals

**Goals:**
- A pure Kotlin stack in `core`: weighted random draws, the top-to-bottom order, and moving down after a placement. It must be fully testable on the JVM with a seeded random source.
- A Compose `StackPanel` in `app`: a vertical column on the left, the next section highlighted at the bottom, and dragging of the next section only.
- A drag and drop contract. The board decides whether it accepts a drop, and the stack changes only when it does.
- A stand-in game screen (stack plus a simple grid) so the whole flow can be tested with instrumented tests on the emulator.

**Non-Goals:**
- The real board, `BoardCanvas`, `PipeDrawer`, flow simulation, scoring and the replace lock. The stand-in board is replaced later.
- Tap-to-place. It is dropped completely.
- Section rotation, discarding a section, or holding a section back.
- Final artwork. The stack sections use a simple procedural drawing until `PipeDrawer` exists.

## Decisions

**1. `PipeStack` is an immutable value in `core`, and the caller passes in the random source.**
`PipeStack(sections: List<PipeSection>)` holds the sections from top to bottom. `next` is `sections.last()`. `PipeStack.create(level.stack, random)` fills the stack. `afterPlacement(pool, random)` returns a new stack: the last section is removed and a newly drawn section is added at index 0. This fits the planned immutable `GameState` (plan section 2.6): the future `GameEngine` keeps a `PipeStack` in its state, just as the stand-in screen does now. *Other option:* a mutable queue that owns its `Random`. It was not chosen because it is harder to compare in tests and does not fit an immutable game state.

**2. `WeightedPool` uses cumulative weights.**
`WeightedPool(weights: Map<PipeSection, Int>)` builds a list of cumulative weights from the entries with a weight above 0, in `PipeSection` order. It then draws with `random.nextInt(total)` and a search through that list. Sections with weight 0 are never in the list, so they can never be drawn. The pool rejects a map whose total weight is 0. The level validator already makes sure a valid level cannot have that. `kotlin.random.Random` is used, so `core` needs no new dependency. *Other option:* repeating each section `weight` times in a list. That is simpler, but it wastes memory when weights are large.

**3. The next section is at the bottom of the list, and the list is drawn in the same order.**
Index 0 is the top of the stack and the top of the column. So the UI draws `sections` in order and needs no reverse step anywhere. The spec example (`H V LT LB RT` → place `RT` → `X H V LT LB`) becomes a single unit test.

**4. Drag and drop goes through a state holder that sits above both the stack and the board.**
The dragged copy has to be drawn above the board, so the drag cannot live inside `StackPanel`. `GameScreen` owns a `SectionDragState` (a Compose state holder). It holds the dragged section, the finger position in the game screen's coordinates, and whether the section is animating back to its slot. The parts work together like this:
- `StackPanel` adds a `pointerInput` with `detectDragGestures` to the **next** slot only. The drag starts once the finger passes the touch slop, so no long press is needed. `onDragStart`, `onDrag` and `onDragEnd`/`onDragCancel` update `SectionDragState`. The preview slots have no gesture handler. If a drag is already going on, a new `onDragStart` is ignored, so only one drag can run at a time.
- The board provides a `SectionDropTarget`:
  ```kotlin
  fun interface SectionDropTarget {
      /** Tries to place [section] at [position] in game-screen coordinates. Returns true when placed. */
      fun tryPlace(section: PipeSection, position: Offset): Boolean
  }
  ```
  The board turns the position into a cell using its own bounds, which it gets from `onGloballyPositioned`. It returns `false` when the point is outside the board or the cell cannot take a pipe.
- On `onDragEnd`, `GameScreen` calls `tryPlace` once. If the result is `true`, it replaces its `PipeStack` with `afterPlacement(...)`. If the result is `false`, or the drag was cancelled, `SectionDragState` animates the copy back to the slot and the stack does not change.

When the real `GameEngine` exists, its `Place` command result takes over from the stand-in board's `tryPlace`, and the engine moves the stack down itself. The contract of "accepted means advance" stays the same. *Other option:* Compose's `dragAndDropSource`/`dragAndDropTarget`. It was not chosen because it is built for moving data between apps, has slower long-press defaults, and gives less control over how the copy is drawn and how it animates back.

**5. Stack layout is a pure function that gets a unit test.**
`StackLayout.metrics(availableHeight: Dp, visibleSize: Int)` returns the preview slot size, the next slot size (1.25 × preview) and the gap. It solves `(n - 1) × s + 1.25 × s + (n - 1) × gap + padding ≤ height`, with a maximum slot size so that small stacks do not get too big. This follows the `WelcomeLayout`/`WelcomeLayoutTest` pattern already in the app. A test checks the 360 dp × `visibleSize` 10 case from the spec.

**6. Stack sections use a simple procedural glyph until `PipeDrawer` exists.**
`PipeSectionGlyph(section, size)` draws the cell background and a pipe to each open side on a `Canvas`. `RES` also gets a tank in the middle. The open sides come from a new `PipeSection.openSides: Set<Side>` in `core`: `H` = LEFT+RIGHT, `V` = TOP+BOTTOM, `LT` = LEFT+TOP, `LB` = LEFT+BOTTOM, `RT` = RIGHT+TOP, `RB` = RIGHT+BOTTOM, `X` = all four, `RES` = LEFT+RIGHT (the reservoir is horizontal only, plan section 1.5). Both the stack and the stand-in board use this glyph. The flow engine will need the open sides too, so keeping them in `core` is not extra work.

**7. Placement gets a short animation.**
When a placement is accepted, the column animates its sections down by one slot, and the new top section fades or slides in, in about 150 ms. When a drop is rejected, the copy slides back to the next slot in about 150 ms. Both use `Animatable` and `animateFloatAsState`. Only the drawing is animated: the `PipeStack` value changes at once, so the game logic never waits for an animation. A new drag cannot start until the copy is back in its slot.

**8. Stand-in game screen and how to reach it.**
`GameScreen(level, random)` shows `StackPanel` on the left and a `PlaceholderBoard` in the rest of the width. `PlaceholderBoard` draws the grid, the start and finish cells and the blocked cells, and shows the glyphs of placed sections. Its `tryPlace` accepts only empty cells that are not blocked, start or finish. For now, `MainActivity` wires the welcome screen's **Play** button to `GameScreen` with level 1 from the bundled assets. The `app-welcome-play-button` and `app-game-level-library` changes replace this wiring later.

## Risks / Trade-offs

- [The plan still describes tap-to-place and adding at the bottom] → The tasks update `docs/project-plan.md` (sections 1.1, 1.6, 1.7, 2.6 and 2.8), so later changes build on the new rule.
- [The stand-in board and the Play wiring are temporary and may conflict with the play-menu and level-library changes] → Keep the stand-in in `ui/game/PlaceholderBoard.kt` behind `SectionDropTarget`, and keep the Play wiring to one line in `MainActivity`. Whichever change merges later resolves that line.
- [Drag hit-testing is wrong when coordinates come from different spaces (root, window or local)] → Store every drag position in the game screen's coordinates, which come from its `LayoutCoordinates`, and convert only inside the board. Instrumented tests drop on a known cell and on a spot outside the board.
- [A finger covers the section being dragged] → Draw the copy a little above the finger, and measure the drop point at the copy's center. The offset is one constant, which the board's hit test also uses.
- [The statistical test in `core` could fail at random] → Use a fixed seed with 100,000 draws and a 1 percentage point tolerance. The test is repeatable, not at random.
- [Only one section can be placed, so a player can get stuck with sections they do not need] → This is intended (plan 1.7: no discard). Replacing unfilled pipes, which comes with the engine, is the way out.

## Migration Plan

Nothing is migrated: no data is stored and no screen is replaced. To roll back, revert the change. Play goes back to a stub.

## Open Questions

- Should the next section show a small hint, such as a pulse, when the flow is about to start and the player has placed nothing yet? This is left to the polish milestone.
- When the real board arrives, should a hovered cell show a preview (green or red) during the drag? The contract could get a `canPlace(section, position)` query for this later without breaking `tryPlace`.
