## Why

The level library only lets the player start the single next unfinished level, so one hard level blocks all progress. Players should be able to play ahead or skip a level they are stuck on, while the next page stays a reward for playing a page well: it should open only once enough stars have been earned, and the player should see how close they are.

## What Changes

- **BREAKING** (rule change): a level unlocks when it is among the three levels following a finished level, instead of only the level directly after it. On a fresh install levels 1, 2 and 3 are playable. A player can skip at most two levels in a row.
- **BREAKING** (rule change): the next page is gated by stars only. Page `p+1` opens, and its first three levels unlock, once page `p` totals at least **50** of its 72 stars. Finishing the last levels of a page no longer unlocks levels on the next page by itself.
- The page unlock threshold `{x}` gets its final value: 50 (was a provisional 36). It stays a single configuration value.
- Paging (next button and swipe) reaches a page only when the star gate for it has been met.
- The level library shows a progress bar of the stars earned on the current page towards the threshold, with the count ("34 / 50"), replacing the text-only star hint.
- Completed tiles keep showing their best stars (0-3); unlocked tiles show the number; locked tiles stay dimmed with a lock.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `level-library`: sequential unlocking becomes a three-level unlock window; the next page unlocks by stars only, with a threshold of 50; paging is limited to star-unlocked pages; the star hint becomes a progress bar.

## Impact

- `core/.../library/LevelLibrary.kt` (`LevelLibraryRules`, `LibraryPage`, `LevelLibrary.initialPage`): new unlock rule and page-open rule.
- `core/.../library/LevelLibraryConfig.kt`: `DEFAULT_PAGE_UNLOCK_STARS` = 50, plus the unlock window size as a constant.
- `core/.../library/NextLevel.kt`: "Continue" keeps going to the level after the highest finished one; its unlocked check follows the new rules.
- `app/.../ui/levels/LevelLibraryScreen.kt`, `LevelLibraryViewModel.kt`: progress bar instead of the hint text, next-page gating.
- Tests: `LevelLibraryRulesTest`, `NextLevelResolverTest`, `LevelLibraryViewModelTest`, `LevelLibraryScreenTest`.
- Depends on `app-game-level-library`, which introduces the `level-library` spec; that change has to be archived before this one.
- No data or schema change: the history store (`game-level-history`) is read as before.
