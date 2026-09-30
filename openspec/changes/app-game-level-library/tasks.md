## 1. Configuration and domain logic

- [x] 1.1 Add `LevelLibraryConfig(pageUnlockStars)` with a provisional default of 36 and a single place to change it
- [ ] 1.2 Define small interfaces `LevelCatalog` (total level count) and `HistoryReader` (Flow of levelId to stars) with fakes for tests
- [x] 1.3 Implement the pure use case computing pages, tile states (locked/unlocked/completed+stars), page star totals and page accessibility
- [x] 1.4 Implement initial page selection (lowest unlocked unfinished level, else last accessible page)
- [x] 1.5 Unit tests: fresh install, sequential unlock, skipping, threshold reached/not reached, improved stars, partial last page, changed threshold, initial page

## 2. View model

- [ ] 2.1 Create `LevelLibraryViewModel` combining history Flow, level count and config into `StateFlow<LevelLibraryUiState>`
- [ ] 2.2 Handle page change and level tap (navigation event for unlocked, locked feedback event otherwise)
- [ ] 2.3 View model tests for state emission and events

## 3. UI

- [ ] 3.1 Build the level tile composable (locked, unlocked, completed with star row) using the design system 3-layer style and Fredoka
- [ ] 3.2 Build the 6x4 grid page composable, with empty cells on a partial last page
- [ ] 3.3 Build `LevelLibraryScreen` with `HorizontalPager` over accessible pages, previous/next buttons and disabled states
- [ ] 3.4 Show star progress hint ("earned/{x}") when the next page is locked
- [ ] 3.5 Add locked-tile feedback and content descriptions for accessibility
- [ ] 3.6 Compose previews and UI tests for tile states, paging restrictions and tap behavior

## 4. Navigation

- [ ] 4.1 Route the welcome screen's Play event to the level library
- [ ] 4.2 Expose a start-level navigation event (stubbed until the game screen exists)
- [ ] 4.3 Verify on a landscape device or emulator that the 6x4 grid fits without scrolling
