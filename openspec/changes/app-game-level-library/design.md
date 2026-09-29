## Context

The app is an Android game (Kotlin, Jetpack Compose, landscape only). The welcome screen has a **Play** button whose navigation event is still stubbed. Two in-progress changes provide the data this screen needs: `game-level-model` (numbered levels loaded from JSON) and `game-level-history` (Room-backed records of finished levels with stars 1-3). Landscape orientation suits a 6-column by 4-row grid.

The project plan (section 1.10) says level `n+1` unlocks when `n` is won. This change extends that with page-level star gating. The star threshold `{x}` is not decided yet.

## Goals / Non-Goals

**Goals:**
- A paged 6x4 level grid with clear tile states (locked, unlocked, completed with stars).
- Deterministic, unit-testable unlock rules kept separate from Compose.
- `{x}` defined in one place so it can be tuned without touching logic or UI.
- Paging limited to pages that contain at least one unlocked level.

**Non-Goals:**
- The game screen itself; tapping a level only raises a navigation event.
- Choosing the final value of `{x}`.
- Level authoring, star thresholds per level (owned by `game-level-model`), or recording results (owned by `game-level-history`).
- Cross-device sync, level packs or themes.

## Decisions

**1. Pure `LevelLibraryState` computed from history.** A use case takes `totalLevels`, a map `levelId -> stars` (from history) and the config, and returns a list of pages, each with 24 tile states plus page-locked info. No Android types. *Why:* the rules are the risky part and must be testable on the JVM. *Alternative:* compute in the composable or DAO queries; rejected as untestable and scattering rules.

**2. Unlock rule.** Level `id` is unlocked if `id == 1`, or level `id-1` is finished, or `id` is the first level of a page `p > 1` and the star sum of page `p-1` is `>= x`. Completed levels are always unlocked. A page is *accessible* iff any of its levels is unlocked. *Why:* matches the requirement literally; the remaining levels of a page follow the sequential rule. *Alternative:* require both the star gate and level 24 finished; rejected as stricter than the request.

**3. Page stars are summed from stored stars.** Stars per level (0-3) come from the history record; unplayed levels count 0. Maximum per page is 72. The last page may contain fewer than 24 levels (empty cells render blank).

**4. `{x}` lives in `LevelLibraryConfig(pageUnlockStars: Int)`.** Provided by a single constant in the app's config with a provisional default (proposed 36, half the maximum), injected into the use case. *Alternative:* remote or per-page thresholds; rejected as premature, but the config type allows a later per-page function.

**5. Paging UI.** A `HorizontalPager` over accessible pages only, plus previous/next buttons. Inaccessible pages are not part of the pager, so swiping cannot reach them. If a next page exists but is locked, the next button is shown disabled with a star-progress hint ("24/36 stars").

**6. Data flow.** `LevelLibraryViewModel` observes the history repository as a `Flow`, combines it with the total level count, maps through the use case and exposes `StateFlow<LevelLibraryUiState>`. The screen opens on the page containing the first unfinished unlocked level so returning from a game lands in a useful place.

**7. Tile visuals.** Reuse the design system's 3-layer cartoon tile and Fredoka font: locked tiles are dimmed with a lock icon, unlocked show the number, completed show the number and a 3-slot star row.

## Risks / Trade-offs

- [`{x}` may be set higher than a player can reasonably reach with easy levels] → single config value, easy to tune; show star progress toward it so players understand the gate.
- [Depends on two unmerged changes] → code against small interfaces (`HistoryReader`, `LevelCatalog`) with fakes in tests.
- [Ambiguity between star gate and finishing level 24] → decision 2 documents the rule and it is covered by unit tests.
- [Total level count changes when new levels ship] → pages are derived, nothing is stored per page.

## Open Questions

- Final value of `{x}` (provisional 36).
- Should a locked page be visible as a teaser (greyed) rather than hidden? Currently hidden from the pager, with the disabled next button as the only hint.
