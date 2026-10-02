# Proposal

Tracking issue: [#36 First time user experience](https://github.com/hexmasternl/leak-out/issues/36).

## Why

A new player is dropped on a grid with a ticking countdown and no explanation of what the start, the finish or the stack are, how to place a section, or that the field can be zoomed and panned. The special sections (cross, reservoir and, soon, mandatory sections) change scoring and the flow in ways nothing in the game explains. A short, one-time set of guided tips lets players learn by playing instead of by losing.

## What Changes

- **Intro tour**: on the first level the player ever starts (whichever level that is, not necessarily level 1), the game is frozen before the countdown runs and a short guided tour is shown, one step at a time, each step highlighting what it explains:
  1. the **start** tile: the liquid comes out here when the countdown ends,
  2. the **finish** tile: lead the liquid here to win,
  3. the **stack**: the sections you get, the next one is at the bottom,
  4. **placing**: drag the bottom section onto an empty cell (with an animated hint),
  5. **zoom and pan**: pinch to zoom, drag to move around a larger level.
  The player steps through with "Next" and can skip the rest of the tour at any time. After the last step the countdown starts.
- **Cross tip**: the first time a cross (`X`) in the stack is fully visible on screen, the game freezes and the cross in the stack is highlighted, explaining that it has two separate channels (the liquid goes straight through), that the first pass scores 10 points and the second pass through the other channel 50 points, and that a channel can only be used once. On a small screen the top of the stack can be cut off; a cross in a slot that is not fully visible does not trigger the tip yet, it is shown once placements have moved the cross down into full view.
- **Reservoir tip**: the first time a reservoir (`RES`) in the stack is fully visible on screen (same rule as the cross), the game freezes and the reservoir is highlighted, explaining that it only connects left and right, that it slows the liquid down while it fills (buying time) and that it scores 25 points.
- **Mandatory section tip**: the first time a level with mandatory sections starts, the game freezes and the mandatory sections on the board are highlighted, explaining that they are already placed, cannot be removed or replaced, and that the liquid must pass through every channel of each of them before it reaches the finish, or the level is lost.
- While a tip is shown the game is frozen exactly as when paused (countdown, flow and drags), without opening the pause dialog. Closing the tip continues the game.
- Each tip is shown once per install. Which tips were seen is stored and survives restarts.
- The Settings Progress panel gets a **"Show tips again"** button that makes every tip show again.
- The tip overlay, its highlight and the drag hint are first proposed in `docs/design-system.md` and approved on the issue before they are built.

## Capabilities

### New Capabilities
- `game-play-onboarding`: the one-time guided tips during play: when each tip is shown, what it highlights and explains, how the player moves through and dismisses tips, how the game is frozen meanwhile, and their accessibility.

### Modified Capabilities
- `settings-persistence`: stores which tips the player has seen, defaulting to none, and can reset them.
- `settings-screen`: the Progress panel gets the "Show tips again" button.

## Impact

- `app/settings`: `Settings`/`SettingsRepository`/`DataStoreSettingsRepository` (seen tips and reset).
- `app/ui/game`: a new tip overlay and an Android-free tip scheduler; `GameSession`/`GameScreen` freeze the game for a tip without showing the pause dialog; `StackPanel`, `PlayingField` and `FieldViewport` expose the on-screen bounds of the start, finish, stack, each stack slot and mandatory cells for the highlight, and which stack slots are fully visible.
- `app/ui/settings`: Progress panel button and strings.
- `docs/design-system.md`: a new tip overlay component and its motion.
- No `core` rule change; `core` already has pause/resume and the stack contents. The mandatory section tip depends on the in-flight `app-game-play-mandatory-sections` change and is built after it lands.
