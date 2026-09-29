## 1. Core: pipe sections and weighted pool

- [ ] 1.1 Add `openSides: Set<Side>` to `PipeSection` in `core` (`H` LEFT+RIGHT, `V` TOP+BOTTOM, `LT` LEFT+TOP, `LB` LEFT+BOTTOM, `RT` RIGHT+TOP, `RB` RIGHT+BOTTOM, `X` all four, `RES` LEFT+RIGHT). Add a unit test for every section.
- [ ] 1.2 Create the package `nl.hexmaster.leakout.core.stack` and add `WeightedPool(weights)`. It uses cumulative weights, skips sections with weight 0, and rejects a total weight of 0. `draw(random: Random): PipeSection`.
- [ ] 1.3 Test `WeightedPool`: a section with weight 0 is never drawn in 10,000 draws; the shares are within 1 percentage point of weight/total in 100,000 draws with a fixed seed; the same seed gives the same sequence; a total of 0 is rejected.

## 2. Core: pipe stack

- [ ] 2.1 Add the immutable `PipeStack(sections)`, ordered from top to bottom, with `next` = bottom section and `size`. Add `PipeStack.create(settings: StackSettings, random)`, which fills `visibleSize` sections.
- [ ] 2.2 Add `afterPlacement(pool, random)`. It removes the bottom section and adds a newly drawn section at the top, so the size stays the same.
- [ ] 2.3 Test the stack: a new stack holds `visibleSize` sections (1, 5 and 10); the size stays the same after a placement; the spec example (`H V LT LB RT` → place → `X H V LT LB`, with `LB` next) passes with a stubbed random; two stacks with the same seed stay equal over 50 placements.
- [ ] 2.4 Run `./gradlew :core:test` and check that everything passes.

## 3. App: stack drawing and layout

- [ ] 3.1 Add `StackLayout.metrics(availableHeight, visibleSize)` in `app/.../ui/game/`. It returns the preview slot size, the next slot size (1.25 × preview), the gap and the column width, with a maximum slot size.
- [ ] 3.2 Unit test `StackLayout`: for every `visibleSize` from 1 to 10, the column fits 360 dp without scrolling; the next slot is larger than the preview slots; the maximum slot size is used for small stacks.
- [ ] 3.3 Add the `PipeSectionGlyph(section, modifier)` Canvas composable. It draws the cell and a pipe to each open side, and a tank for `RES`, using the theme colors.
- [ ] 3.4 Add `StackPanel(stack, dragState, modifier)`: a vertical column along the left edge that draws `stack.sections` from top to bottom, with the bottom slot highlighted and larger. Add content descriptions ("Left-top corner", "Next: Left-top corner") from string resources.

## 4. App: drag and drop

- [ ] 4.1 Add the `SectionDropTarget` fun interface (`tryPlace(section, position): Boolean`, where the position is in game-screen coordinates).
- [ ] 4.2 Add the `SectionDragState` state holder: the dragged section, the finger position, whether a drag is active, and the animation back to the slot. It ignores a second drag start while one is active or while the section is animating back.
- [ ] 4.3 Add `detectDragGestures` to the next slot only. On start, begin the drag. On drag, move the position. On end, call `tryPlace` once. On cancel, go back to the slot. The preview slots have no gesture handler.
- [ ] 4.4 Draw the dragged copy in an overlay above the board and stack, a small constant offset above the finger, and show the bottom slot as empty during the drag. Use the copy's center as the drop point.
- [ ] 4.5 When a placement is accepted, replace the stack with `afterPlacement(...)` and animate the column down one slot and the new top in (about 150 ms). When a drop is rejected or cancelled, slide the copy back to the slot (about 150 ms) and leave the stack unchanged.

## 5. App: stand-in game screen

- [ ] 5.1 Add `PlaceholderBoard(level, placed, modifier)`. It draws the grid, the start, finish and blocked cells, and the glyphs of placed sections. It implements `SectionDropTarget`, which accepts only empty cells that are not blocked, start or finish, and maps positions to cells through its bounds from `onGloballyPositioned`.
- [ ] 5.2 Add `GameScreen(level, random = Random.Default)`, which shows `StackPanel` on the left and `PlaceholderBoard` in the rest of the landscape width, keeps the `PipeStack` state, and owns `SectionDragState` and the drag overlay.
- [ ] 5.3 Load level 1 from `assets/levels/level_001.json` with `LevelParser`, and wire the welcome screen's **Play** in `MainActivity` to `GameScreen` (a temporary one-line wiring, to be replaced by the play-menu and level-library changes).

## 6. Instrumented tests (emulator)

- [ ] 6.1 Stack layout: with a seeded random, the column shows `visibleSize` glyphs from top to bottom in the expected order, the next section is at the bottom with the "Next" content description, and all 10 sections are visible on a 360 dp tall screen.
- [ ] 6.2 Dragging the next section onto an empty cell places it on that cell, and the stack moves down with a new section at the top.
- [ ] 6.3 Dropping outside the board, dropping on a blocked cell, and dropping on the start cell each leave the stack unchanged and place nothing.
- [ ] 6.4 Dragging a preview section does not start a drag and changes nothing.
- [ ] 6.5 A cancelled gesture leaves the stack unchanged, and one drop changes the stack at most once.
- [ ] 6.6 Run `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` on the emulator and record the results.

## 7. Wrap up

- [ ] 7.1 Update `docs/project-plan.md` sections 1.1, 1.6, 1.7, 2.6 and 2.8: new sections enter at the top, the next section is at the bottom, and drag and drop is the only way to place a section (no tap-to-place).
- [ ] 7.2 Run `openspec validate app-game-play-stack` and fix any issues.
