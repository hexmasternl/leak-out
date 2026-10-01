## Why

The game has many numbered levels, but the player has no screen to choose one, see progress or understand what is playable. A level library gives a clear overview and, through unlock rules, enforces a deliberate flow: players first master easier levels and earn stars before harder ones open up.

## What Changes

- Add a level library screen (Jetpack Compose, landscape) reached from the welcome screen's **Play** button.
- The screen shows a grid of level tiles, 6 columns by 4 rows, so 24 levels per page. Page 1 holds levels 1-24, page 2 levels 25-48, and so on.
- Each tile shows the level number and its state: locked, unlocked (not yet finished) or completed with 1-3 stars.
- The player can leaf through pages (previous/next controls and swipe), but only to pages that contain at least one unlocked level. Locked pages cannot be entered.
- Level unlocking within a page is sequential: level 1 is always unlocked, and finishing level `n` unlocks level `n+1`.
- Page unlocking is star based: the first level of page `p+1` unlocks only when the total earned stars over all 24 levels of page `p` reaches a minimum `{x}`. Until then, page `p+1` is locked.
- `{x}` is a single configurable value, not hard-coded in UI logic. Its final value is undecided and will be set later; a provisional default is used until then.
- Tapping an unlocked tile raises a navigation event to start that level (game screen does not exist yet; stubbed). Tapping a locked tile does nothing except give brief "locked" feedback.

## Capabilities

### New Capabilities
- `level-library`: The paged 6x4 level grid screen: tile states, page navigation restrictions, and the per-level and per-page unlock rules including the configurable star threshold `{x}`.

### Modified Capabilities

None. Completion and stars are consumed from `game-level-history` (in progress) and level ids from `game-level-model` (in progress); their specs are not changed.

## Impact

- New code in the Android app module: `ui/levels/` (screen, view model, tile and pager composables) and a pure-Kotlin unlock/paging use case with unit tests.
- Reads history records (finished levels and stars) from the `game-level-history` repository and the total level count from the `game-level-model` level loader.
- New configuration constant for the page unlock star threshold `{x}`.
- Welcome screen: the stubbed **Play** navigation event now leads here.
- Design system components (tiles, buttons, Fredoka font) are reused; a locked-tile and star-row style are added.
