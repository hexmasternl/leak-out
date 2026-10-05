# Tasks

## 0. Prerequisites

- [ ] 0.1 Make sure `app-game-achievements` and `app-game-first-time-user-experience` are archived (this change modifies the `game-achievements`, `game-engine` and `achievement-persistence` specs the first introduces, and builds on the "Show tips again" button of the second), or archive them before this change; verify with `openspec list --specs` that `game-achievements` exists

## 1. Engine facts (core)

- [x] 1.1 Add `PathStep(at, section)` and `GameState.flowPath`, appended in flow order wherever an exit is awarded and reset by `GameEngine.start`; verify with engine tests for the "Path in flow order", "Cross passed twice" and "Restart clears the path" scenarios
- [x] 1.2 Add `GameCommand.ReplacementTap(at)`, `Replacement.taps` and `GameState.mostReplacementTaps`; a tap off the replacement tile or without a replacement returns the same instance; verify with engine tests for every scenario of "Taps on a running replacement are counted"
- [x] 1.3 Run `./gradlew :core:test` and make sure the existing engine tests still pass

## 2. Attempt summary and player stats (core)

- [ ] 2.1 Add `longestCrossChain`, `longestStraightRun`, `longestCornerRun`, `reservoirsLeft`, `mostReplacementTaps` and `minimumSections` (from `LevelSolver`, distinct cells, null when unsolvable) to `AttemptSummary` and fill them in `from`; verify with `AttemptSummaryTest` cases for each, including a mandatory cross and a won board after `RemoveUnused`
- [ ] 2.2 Add `PlayerStats.tipsResets` and `afterTipsReset()`; verify with `PlayerStatsTest`

## 3. Catalogue and rules (core)

- [ ] 3.1 Append the 11 entries to `Achievement` exactly as in the `game-achievements` spec table (key, group, type, hidden, XP); add them to the `published` list in `AchievementCatalogueTest` and update its total to 710; verify the catalogue test passes
- [ ] 3.2 Add the threshold constants and the rules for the 11 achievements in `AchievementRules` (exhaustive `when`); verify with `AchievementRulesTest` cases for every scenario of the new `game-achievements` requirements
- [ ] 3.3 Add scripted proofs to `AchievementReachabilityTest` on bundled levels for The Long Way Round II and III, Big Spender II, Crossroad Master, Efficiency Master, Highway, Snake, Eraser, Dramatic Failure and On Your Nerves, plus a stats-only proof for Tips Addict; if a tier proves unreachable, drop it from the catalogue, spec and docs before section 5 and record why in design.md
- [ ] 3.4 Make sure `AchievementContentTest` still passes (no new milestone exceeds the bundled levels) and run `./gradlew :core:test`

## 4. App wiring

- [ ] 4.1 Send `ReplacementTap` from `GameSession.place` while a replacement runs, still returning the silent rejection; verify with `GameSession` unit tests that no placement or failure sound is triggered and the count reaches the summary
- [ ] 4.2 Add the `tips_resets` column to `PlayerStatsEntity` with a Room migration (version + 1, default 0) and export the schema; verify with a `MigrationTestHelper` test for the "Migration with achievements" scenario
- [ ] 4.3 Add `AchievementTracker.onTipsReset()` / `tipsReset()` and call it from `SettingsViewModel` after `resetTips()`; verify with `AchievementTrackerTest` (first reset unlocks Tips Addict, second unlocks nothing) and a `SettingsViewModel` test
- [ ] 4.4 Add `achievement_name_<key>` / `achievement_desc_<key>` strings for the 11 keys, the same texts in the console list of `AchievementResourcesTest`, and the entries in the exhaustive `when` of `AchievementResources`; verify with `./gradlew :app:testDebugUnitTest`

## 5. Icons and Play Console

- [ ] 5.1 Add the 11 icons to `assets/achievements/generate-icons.mjs` (tier template for the II/III tiers, hidden placeholder for the two hidden ones), render the 512 × 512 PNGs and copy them to `app/src/main/res/drawable-nodpi/achievement_<key>.png`; verify they stay inside the radius-200 safe disc in `preview.html`
- [ ] 5.2 Owner: create the 11 achievements in the Play Console with the same names, descriptions, XP, hidden flags and icons, following `docs/achievements-setup.md#adding-achievements-later`
- [ ] 5.3 Replace `app/src/main/res/values/games-ids.xml` with the new export (existing ids unchanged); verify `AchievementResourcesInstrumentedTest` passes on the emulator
- [ ] 5.4 Owner: smoke test with a tester account (unlock Tips Addict and one skill achievement), then publish the Play Games configuration

## 6. Docs

- [ ] 6.1 Update `docs/achievements.md`: XP used 710 / left 290, the rebalanced guideline (packs 5 + 5, Star Collector III-VI at 100 / 250 / 500 / 1000 stars, Snail with the first reservoir level), and why Big Spender and The Long Way Round got tiers instead of new values; verify the guideline adds up to at most 290
- [ ] 6.2 Update `docs/achievements-technical.md` (flow path, replacement taps, minimum sections, tips resets) and `docs/project-plan.md` section 2.11 with the new achievement list

## 7. Integration

- [ ] 7.1 Run `./gradlew :core:test` and `./gradlew :app:testDebugUnitTest` and make sure both pass; run only the affected instrumented tests (migration, achievement resources, settings)
- [ ] 7.2 Check that the achievements screen lists 36 entries in catalogue order and the settings count shows the new total
- [ ] 7.3 Comment on GitHub issue #64 with what shipped and what is deferred (Snail, Star Collector 100-1000), and close it when the PR merges into `development`
