## Context

The level library (change `app-game-level-library`) is implemented: a paged 6x4 grid built from `LevelLibraryRules.compute(totalLevels, stars, config)` in `core`, rendered by `LevelLibraryScreen` through `LevelLibraryViewModel`. Today level `n` unlocks only when `n-1` is finished, and the first level of page `p+1` also unlocks when page `p` reaches a provisional 36 stars. Paging reaches every page that has an unlocked level, so finishing level 24 opens page 2 without any stars. A text hint ("24/36 stars") shows progress only when the next page is locked.

This change replaces both rules and gives the page threshold its final value of 50 out of 72 stars. `NextLevelResolver` ("Continue" on the play menu) derives its unlocked check from the same rules, so it follows automatically.

## Goals / Non-Goals

**Goals:**
- Let the player play up to three levels ahead of what they have finished, so a hard level can be skipped.
- Make the next page a reward for stars only: 50 stars on a page opens the next one.
- Show the page's star progress as a progress bar, always visible while a next page exists.
- Keep all rules in `core`, pure and unit tested, with the numbers in `LevelLibraryConfig`.

**Non-Goals:**
- Changing how stars are earned or recorded (`game-level-history`, `game-play-final-score`).
- A cumulative (all-pages) star gate or per-page thresholds.
- Teaser rendering of locked pages; they stay out of the pager as before.
- Changing the "Continue" target rule (the level after the highest finished level).

## Decisions

**1. Unlock window of three.** On an open page `p`, level `n` is unlocked when it is finished, when it is one of the first three levels of the page (`n < first(p) + 3`), or when any of `n-1`, `n-2`, `n-3` is finished. A finished level is always playable. *Why:* "the next three levels" read as the three levels after any finished level; this generalises the old `n-1` rule, needs no ordering state, and only ever grows as levels are finished, so a level never relocks. Fresh install: levels 1-3. Finish 1 → 1-4. Finish 1 and 4, skip 2 and 3 → 2, 3, 5, 6, 7 playable. At most two levels in a row can be skipped, because a gap of three finished-less levels closes the window. *Alternative:* "only the three lowest unfinished levels" (e.g. 2, 3, 5 above); rejected because finishing a level could leave an earlier skipped one counting against the player, and it is harder to explain.

**2. A page opens by stars only.** Page 1 is always open. Page `p > 1` is open when page `p-1` is open and totals at least `{x}` stars (0 for unplayed levels, max 72). On a page that is not open no level is unlocked, even if the last levels of the previous page are finished; the window does not cross into a closed page. When a page opens, its first three levels unlock through decision 1. *Why:* the request says the player may leaf to the next page ONLY at 50 stars; with the window, finishing levels 22-24 would otherwise unlock 25-27. Requiring page `p-1` to be open keeps the chain strict if the threshold is ever raised. `LibraryPage` gets an explicit `isOpen` flag and `isAccessible` becomes `isOpen`.

**3. Config.** `LevelLibraryConfig` keeps `pageUnlockStars` with `DEFAULT_PAGE_UNLOCK_STARS = 50` and gains `unlockWindow` with `DEFAULT_UNLOCK_WINDOW = 3`, both validated (`pageUnlockStars` 0..72, `unlockWindow >= 1`). *Why:* tuning stays in one place; `unlockWindow = 1` reproduces the old sequential rule, which keeps the old tests meaningful as a parameterised case. *Alternative:* hard-coding 3 in the rule; rejected for the same reason `{x}` was made configurable.

**4. Opening page.** `LevelLibrary.initialPage()` opens on the page of the level after the highest finished level when that level is playable, else on the last open page; page 1 when nothing is finished. *Why:* with skipping allowed, "lowest unlocked unfinished level" can be a skipped level on page 1 while the player is on page 3, which would open the library in the wrong place. This matches `NextLevelResolver`, so "Continue" and "Select level" agree.

**5. Progress bar.** The screen shows, below the grid, a `LinearProgressIndicator`-style bar styled with the design system (track + fill, rounded, Fredoka label with a star glyph): fill = `min(earned / x, 1)`, label `"<earned> / <x>"`. When the threshold is reached the bar is full and uses the success colour, and the next button is enabled. It is shown for every page that has a next page; on the last page it is hidden. It replaces the star text hint; the locked-tile tap message briefly takes the place of the bar in the same row, as it does with the hint today. `LevelLibraryUiState` already exposes `currentPageStars` and `nextPageLocked` (and `library.pageUnlockStars`); it adds `starProgress: Float` and `showStarProgress: Boolean`. The bar sits in the existing hint row height so the 6x4 grid still fits without scrolling. *Alternative:* a bar over all 72 stars with a marker at 50; rejected as less direct about what the player is working towards.

**6. Paging.** Unchanged mechanism: the pager holds only accessible (open) pages, the next button is enabled only when the next page is open. Only the definition of accessible changes (decision 2).

## Risks / Trade-offs

- [Existing players can lose access to page 2 they reached by finishing level 24 with fewer than 50 stars] → no release has shipped with the library yet; the change is noted as breaking in the proposal. Completed levels on a closed page stay playable by rule, but the page is not reachable until the gate is met.
- [50 of 72 stars may be too hard for early pages] → a single config value; the progress bar makes the requirement visible.
- [Players can rush ahead and never return to skipped levels] → the star gate requires about 17 finished levels on a page at 3 stars each, so skipping is bounded.
- [The `level-library` spec is not archived yet] → archive `app-game-level-library` before this change; the deltas are written against its requirements.

## Migration Plan

No stored data changes; the library is recomputed from history on every read. Rollback is reverting the commit.

## Open Questions

- None blocking. The unlock window and threshold can be tuned after play testing.
