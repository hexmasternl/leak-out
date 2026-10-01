## 1. Configuration

- [x] 1.1 In `core/.../library/LevelLibraryConfig.kt` set `DEFAULT_PAGE_UNLOCK_STARS = 50`, add `unlockWindow` with `DEFAULT_UNLOCK_WINDOW = 3`, and validate `pageUnlockStars` in `0..72` and `unlockWindow >= 1`
- [x] 1.2 Unit tests for the defaults and for rejecting a threshold of -1 or 73 and a window of 0

## 2. Unlock rules (core)

- [x] 2.1 Add `isOpen` to `LibraryPage` and make `isAccessible` mean open; compute page openness in `LevelLibraryRules.compute`: page 1 always, page `p` when page `p-1` is open and totals at least `pageUnlockStars`
- [x] 2.2 Replace the sequential rule with the unlock window: on an open page a level is unlocked when finished, among the first `unlockWindow` levels of the page, or within `unlockWindow` levels after a finished level; on a closed page only finished levels are playable
- [x] 2.3 Change `LevelLibrary.initialPage()` to the page of the level after the highest finished level when it is playable, else the last open page, else page 1
- [x] 2.4 Update `LevelLibraryRulesTest` with the spec scenarios: fresh install (1-3), window extension, skipping (1 and 4 finished), at most two skipped in a row, window not crossing a closed page, threshold reached/not reached with level 24 finished, improved stars opening a page, pages opening in order, window of 1, partial last page, initial page cases
- [x] 2.5 Update `NextLevelResolverTest` for the new rules (next level locked behind the star gate, next level reachable after skipping)
- [x] 2.6 Run `./gradlew :core:test` and make sure it passes

## 3. View model

- [ ] 3.1 Add `starProgress` (`min(earned / threshold, 1)`, 1 when the threshold is 0) and `showStarProgress` (a next page exists) to `LevelLibraryUiState`
- [ ] 3.2 Update `LevelLibraryViewModelTest`: progress values below, at and above the threshold, hidden on the last page, follows the current page, next disabled until the gate is met
- [ ] 3.3 Run `./gradlew :app:testDebugUnitTest` and make sure it passes

## 4. UI

- [ ] 4.1 Replace the star text hint in `LevelLibraryScreen` with a star progress bar in the hint row (design system track and fill, Fredoka label "earned / threshold" with a star glyph, success style when reached), with a test tag and a content description
- [ ] 4.2 Keep the locked-tile message showing briefly in the same row, then restore the bar
- [ ] 4.3 Update string resources (remove the unused star hint string, add the progress label and content description) and the Compose previews (below threshold, reached, last page)
- [ ] 4.4 Update `LevelLibraryScreenTest`: levels 1-3 tappable on a fresh install, level 4 locked, skipping a level, next button disabled until 50 stars, progress bar label and visibility per page, locked message in the bar row
- [ ] 4.5 Run the affected instrumented tests (`LevelLibraryScreenTest`, plus `WelcomeScreenTest`/`MainActivityTest` if the play menu flow is touched) on the emulator and make sure they pass
- [ ] 4.6 Verify on a landscape emulator that the grid and progress bar fit without scrolling
