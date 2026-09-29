## Why

The player builds a route from pipe sections, but the game has no way yet to hand out those sections. The stack is how the game supplies them. It shows the player what comes next so they can plan ahead, and the level's weights control how often each section appears. The game screen, placement and flow all need the stack before a level can be played.

## What Changes

- Add a pipe stack: a queue of upcoming pipe sections, as long as the level's `stack.visibleSize` (default 5).
- Draw sections at random from the level's stack weights. A section with weight 0 never appears. No seed is stored in the level, so each attempt gets a new stack. Tests can pass in their own random source.
- Draw the stack as a vertical column on the left of the game screen, in landscape.
- New sections enter at the **top** of the column and the stack moves **down**. The **bottom** section is the next one to place and is highlighted. It is the only section that can be dragged. The sections above it are a preview.
- The player places a section by pressing the next section and dragging it onto the board. While dragging, a copy of the section follows the finger.
- The stack changes only when the board accepts the placement. Then the bottom section is removed, the others move down by one, and a new random section is added at the top.
- If the drop is rejected (outside the board, or on a cell that cannot take a pipe) or the drag is cancelled, the section goes back to its place and the stack stays the same.
- Drag and drop is the only way to place a section. **BREAKING** (plan only): this replaces the tap-to-place rule in `docs/project-plan.md` sections 1.1, 1.6 and 1.7, where the top section is placed and new sections are added at the bottom.

## Capabilities

### New Capabilities
- `game-play-stack`: The stack of upcoming pipe sections. It covers the stack size and weighted draws, the order (in at the top, next one at the bottom), which section can be dragged, what happens when a placement is accepted or rejected, and how the stack is shown on the left side of the game screen.

### Modified Capabilities

None. `game-level-model` is still an open change and is not in `openspec/specs/`. This change reads its `stack.visibleSize` and weights and does not change its requirements.

## Impact

- New pure Kotlin code in `core` (`nl.hexmaster.leakout.core.stack`): `WeightedPool` and `PipeStack`, with JVM unit tests.
- New Compose code in `app` (`ui/game/`): `StackPanel`, a mini pipe section drawing, and a drag and drop contract with the board, with unit and instrumented tests.
- There is no game board yet. This change defines what the board must provide: a drop target that accepts or rejects a section at a position. It also adds a stand-in board so the stack can be tested now. The real board and the `GameEngine` connect to this contract later.
- `docs/project-plan.md` is updated to describe the new stack order and drag to place.
