# Tasks

## 0. Prerequisites

- [x] 0.1 Make sure `app-game-achievements` and `app-game-first-time-user-experience` are archived (this change modifies the `game-achievements`, `game-engine` and `achievement-persistence` specs the first introduces, and builds on the "Show tips again" button of the second), or archive them before this change; verify with `openspec list --specs` that `game-achievements` exists

## 1. Engine facts (core)

- [x] 1.1 Add `PathStep(at, section)` and `GameState.flowPath`, appended in flow order wherever an exit is awarded and reset by `GameEngine.start`; verify with engine tests for the "Path in flow order", "Cross passed twice" and "Restart clears the path" scenarios
- [x] 1.2 Add `GameCommand.ReplacementTap(at)`, `Replacement.taps` and `GameState.mostReplacementTaps`; a tap off the replacement tile or without a replacement returns the same instance; verify with engine tests for every scenario of "Taps on a running replacement are counted"
- [x] 1.3 Run `./gradlew :core:test` and make sure the existing engine tests still pass

## 2. Attempt summary and player stats (core)

- [x] 2.1 Add `longestCrossChain`, `longestStraightRun`, `longestCornerRun`, `reservoirsLeft`, `mostReplacementTaps` and `minimumSections` (from `LevelSolver`, distinct cells, null when unsolvable) to `AttemptSummary` and fill them in `from`; verify with `AttemptSummaryTest` cases for each, including a mandatory cross and a won board after `RemoveUnused`
- [x] 2.2 Add `PlayerStats.tipsResets` and `afterTipsReset()`; verify with `PlayerStatsTest`

## 3. Catalogue and rules (core)

- [x] 3.1 Append the 11 entries (first scope; see 3.5) to `Achievement` exactly as in the `game-achievements` spec table (key, group, type, hidden, XP); add them to the `published` list in `AchievementCatalogueTest` and update its total to 710; verify the catalogue test passes
- [x] 3.2 Add the threshold constants and the rules for the 11 achievements (first scope; see 3.5) in `AchievementRules` (exhaustive `when`); verify with `AchievementRulesTest` cases for every scenario of the new `game-achievements` requirements
- [x] 3.3 Add scripted proofs to `AchievementReachabilityTest` on bundled levels for The Long Way Round II and III, Big Spender II, Crossroad Master, Efficiency Master, Highway, Snake, Eraser, Dramatic Failure and On Your Nerves, plus a stats-only proof for Tips Addict; if a tier proves unreachable, drop it from the catalogue, spec and docs before section 5 and record why in design.md
- [x] 3.4 Make sure `AchievementContentTest` still passes (no new milestone exceeds the bundled levels) and run `./gradlew :core:test`
- [x] 3.5 Add Star Collector III / IV and Snail (the library now has 96 levels with reservoirs): catalogue entries and `published` rows (total 745, 39 entries), `SNAIL_RESERVOIRS` and the Snail rule, `AchievementRulesTest` cases for the Snail and Star Collector scenarios, a scripted Snail proof on a bundled level that draws reservoirs; run `./gradlew :core:test`

## 4. App wiring

- [x] 4.1 Send `ReplacementTap` from `GameSession.place` while a replacement runs, still returning the silent rejection; verify with `GameSession` unit tests that no placement or failure sound is triggered and the count reaches the summary
- [x] 4.2 Add the `tips_resets` column to `PlayerStatsEntity` with a Room migration (version + 1, default 0) and export the schema; verify with a `MigrationTestHelper` test for the "Migration with achievements" scenario
- [x] 4.3 Add `AchievementTracker.onTipsReset()` / `tipsReset()` and call it from `SettingsViewModel` after `resetTips()`; verify with `AchievementTrackerTest` (first reset unlocks Tips Addict, second unlocks nothing) and a `SettingsViewModel` test
- [x] 4.4 Add `achievement_name_<key>` / `achievement_desc_<key>` strings for the 14 keys, empty `achievement_<key>` placeholders in `games-ids.xml` (listed as awaiting the export in `AchievementResourcesTest`), the skip of an empty id in `PlayGamesAchievementSync`, the same texts in the console list of `AchievementResourcesTest`, and the entries in the exhaustive `when` of `AchievementResources`; verify with `./gradlew :app:testDebugUnitTest`

## 5. Icons and Play Console

- [x] 5.1 Add the 14 icons to `assets/achievements/generate-icons.mjs` (tier template for the II/III/IV tiers, hidden placeholder for the two hidden ones), render the 512 × 512 PNGs and copy them to `app/src/main/res/drawable-nodpi/achievement_<key>.png`; verify they stay inside the radius-200 safe disc in `preview.html`
- [x] 5.2 Owner: create the 14 achievements in the Play Console with the same names, descriptions, XP, hidden flags and icons, following `docs/achievements-setup.md#adding-achievements-later`
- [x] 5.3 Replace `app/src/main/res/values/games-ids.xml` with the new export (existing ids unchanged) and empty the awaiting-export lists in `AchievementResourcesTest` and `AchievementResourcesInstrumentedTest`; verify `AchievementResourcesInstrumentedTest` passes on the emulator
- [x] 5.4 Owner: smoke test with a tester account (unlock Tips Addict and one skill achievement), then publish the Play Games configuration

## 6. Docs

- [x] 6.1 Update `docs/achievements.md`: XP used 745 / left 255, the rebalanced guideline (packs 5 + 5, Star Collector V / VI at 500 / 1000 stars), and why Big Spender and The Long Way Round got tiers instead of new values; verify the guideline adds up to at most 255
- [x] 6.2 Update `docs/achievements-technical.md` (flow path, replacement taps, minimum sections, tips resets, empty ids before the export), resolve the merge-conflict markers in `docs/achievements-setup.md` (keep the upstream side), and `docs/project-plan.md` section 2.11 with the new achievement list

## 7. Integration

- [x] 7.1 Run `./gradlew :core:test` and `./gradlew :app:testDebugUnitTest` and make sure both pass; run only the affected instrumented tests (migration, achievement resources, settings)
- [x] 7.2 Check that the achievements screen lists 39 entries in catalogue order and the settings count shows the new total
- [x] 7.3 Comment on GitHub issue #64 with what shipped and what is deferred (Star Collector 500 / 1000), and close it when the PR merges into `development`
