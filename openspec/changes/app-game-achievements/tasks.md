## 0. Prerequisites

- [ ] 0.1 Make sure `app-settings-page` is archived (this change modifies its `settings-screen` spec) or archive it before this change

## 1. Engine attempt facts (core)

- [ ] 1.1 Add `placements`, `replacements`, `everPaused`, `routeCompletedBeforeFlow` and `photoFinish` to `GameState`, all reset by `GameEngine.start` (so also by `Restart`)
- [ ] 1.2 Count accepted placements and replacements in `GameEngine.place`; leave refused commands unchanged
- [ ] 1.3 Set `everPaused` on an accepted `Pause`; `Resume` keeps it
- [ ] 1.4 Set `routeCompletedBeforeFlow` when a placement completes the route while the flow is `Waiting`
- [ ] 1.5 Set `photoFinish` when a placement completes the route while `Flowing` on the tile the head's channel exits into
- [ ] 1.6 Unit tests for every scenario of the `game-engine` delta spec (placements, refused placement, pause, countdown completion, photo finish and its negative, restart clears)

## 2. Achievement catalogue and rules (core)

- [ ] 2.1 Add `core/achievements/Achievement` enum with key, group, type, steps, hidden and XP exactly as the launch catalogue in the `game-achievements` spec, plus the pack id for `pack_<n>_*` entries
- [ ] 2.2 Catalogue tests: unique keys, launch XP 595 (≤ 1000 overall), multiples of 5, steps within 2-10000, and a pinned list of published entries (key, steps, hidden, XP) that may only grow
- [ ] 2.3 Add `AttemptSummary.from(state, recordResult)` built from the state at the moment the outcome first becomes won or lost (cross doubles from `passes` on X tiles, distinct tiles left, free cells, pieces on board, fast forward at win)
- [ ] 2.4 Add `PlayerStats` (sections placed, losses, loss streak per level) with a pure `after(summary)` and `afterAbandoned(placements)`
- [ ] 2.5 Implement `AchievementRules.evaluate(summary?, statsBefore, statsAfter, history, stored)` returning newly unlocked achievements and clamped, never-decreasing steps
- [ ] 2.6 Unit tests for every scenario in the `game-achievements` spec, one group per achievement group
- [ ] 2.7 Reachability test: scripted engine attempts on bundled levels that unlock each skill and hidden achievement (Crossroads King on a level that can draw crosses; Long Way Round and Wall to Wall on a large grid)
- [ ] 2.8 Reachability-against-content test: every milestone fits the bundled library (levels ≤ level count, stars ≤ 3 × level count) and pack achievements exist only for full library pages; progress rules use `LevelLibraryLayout.pageOf`, never the library size
- [ ] 2.9 Run `./gradlew :core:test` and make sure it passes

## 3. Local store (app)

- [ ] 3.1 Add Room entities `AchievementStateEntity` (`achievement_state`), `PlayerStatsEntity` (`player_stats`) and `LevelLossStreakEntity` (`level_loss_streak`) and an `AchievementDao`
- [ ] 3.2 Bump `LeakOutDatabase` version with a `Migration` that only creates the new tables; export the schema
- [ ] 3.3 Migration test with `MigrationTestHelper`: history rows survive, new tables are empty
- [ ] 3.4 Define `AchievementRepository` (`observeAll`, `stats`, transactional `apply`) with Room and in-memory implementations, storing by key
- [ ] 3.5 Implement `AchievementTracker` with `onAttemptEnded(summary)`, `onAttemptAbandoned(placements)` and `reconcile()` (no summary), each running in one transaction
- [ ] 3.6 Call `reconcile()` at app start and after every history change, so existing players receive progress achievements
- [ ] 3.7 Confirm `LevelHistoryRepository.clearAll()` leaves the achievement tables alone
- [ ] 3.8 JVM tests for the tracker with in-memory repositories: once per attempt, abandoned attempts counted once, backfill from history, reset keeps achievements

## 4. Game session wiring (app)

- [ ] 4.1 In `GameSession`, after `recordWin`, call `onAttemptEnded` once per attempt and expose `unlockedThisAttempt: List<Achievement>`; do the same on a loss
- [ ] 4.2 Call `onAttemptAbandoned` on restart and exit of an unfinished attempt (pause dialog Restart, Exit, Back to menu)
- [ ] 4.3 JVM tests: recompositions don't double count; restart from the pause dialog adds placements

## 5. Result dialog rows (app)

- [ ] 5.1 Add an `AchievementUnlockedRow` (trophy icon, "Achievement unlocked" in `display.xs` `brass.300`, up to two names joined with " · " then "+N more") that pops in with `motion.pop`
- [ ] 5.2 Show it in the level complete dialog after the tally, without delaying the buttons, and in the level lost dialog
- [ ] 5.3 Accessibility semantics: the row reads "Achievement unlocked" plus all names
- [ ] 5.4 Compose tests for both dialogs: one, four and zero achievements; screen reader text

## 6. Play Games integration (app)

- [ ] 6.1 Add `com.google.android.gms:play-services-games-v2` (latest stable, pinned exactly) to `gradle/libs.versions.toml` and the app module
- [ ] 6.2 Add `res/values/games-ids.xml` with placeholder app id `0` and an `achievement_<key>` string for every catalogue key; add the `com.google.android.gms.games.APP_ID` manifest meta-data
- [ ] 6.3 Add `LeakOutApplication` that initialises `PlayGamesSdk` only when the app id is a positive number
- [ ] 6.4 Implement `PlayGamesAuth` with `state: StateFlow<SignInState>` (`Unavailable`, `SignedOut`, `SigningIn`, `SignedIn(displayName)`), silent check at start and `signIn(activity)`
- [ ] 6.5 Define `AchievementSync` with `PlayGamesAchievementSync` (`unlock`, `setSteps`, never `increment`) and `NoOpAchievementSync`
- [ ] 6.6 Implement `AchievementSyncer` that pushes the full local state on `SignedIn` and on every local change while signed in; failures are logged and retried on the next trigger
- [ ] 6.7 JVM tests with a recording fake sync: push on sign-in, push on new unlock, repeated push idempotent, failure leaves local data alone, unavailable makes no calls
- [ ] 6.8 Instrumented test: every catalogue key has an `achievement_<key>` string resource

## 7. Settings screen changes (app)

- [ ] 7.1 Add the Play Games panel at the top of the right column: sign-in state text, glass "Sign in" button (disabled while signing in, hidden when signed in or unavailable)
- [ ] 7.2 Add the always-visible "Achievements  N / total" glass button that navigates to `Screen.Achievements`
- [ ] 7.3 Update the reset dialog body ("Achievements are kept.") and the About privacy line
- [ ] 7.4 Extend `SettingsViewModel`/`SettingsUiState` with the sign-in state and unlocked count
- [ ] 7.5 Compose tests for the panel in all four sign-in states, the Achievements button count and navigation, and the updated texts; update existing settings tests

## 8. Achievements screen (app)

- [ ] 8.1 Add `Screen.Achievements` to `MainActivity`; back button and system Back return to Settings
- [ ] 8.2 Build `AchievementsViewModel` from `AchievementRepository.observeAll()` and `PlayGamesAuth.state`
- [ ] 8.3 Build `ui/achievements/AchievementsScreen.kt`: top bar (back, "Achievements", "N / total", "Play Games" button only when signed in), scrolling grid of cards
- [ ] 8.4 Card states: unlocked (colour icon, date, `success.tint`), locked (dimmed desaturated icon, progress bar with "x / y" for incremental), hidden locked ("?" medallion, "Hidden achievement", "Keep playing to find out.")
- [ ] 8.5 "Play Games" launches `AchievementsClient.getAchievementsIntent()`
- [ ] 8.6 Card semantics merged: name, locked/unlocked, "x of y"
- [ ] 8.7 Compose previews and Compose tests for count, incremental progress, hidden locked/unlocked, signed-in/out button, Back

## 9. Icons

- [ ] 9.1 Review the proposed icons in `openspec/changes/app-game-achievements/icons/` (SVG sources, 512×512 PNGs, `preview.html` contact sheet) and adjust any that are rejected
- [ ] 9.2 Copy the approved PNGs to `app/src/main/res/drawable-nodpi/achievement_<key>.png` plus a `achievement_hidden.png` for the hidden placeholder
- [ ] 9.3 Map each `Achievement` to its drawable in `app` and test that every key has one

## 10. Documentation

- [ ] 10.1 Add an achievements section to `docs/project-plan.md` (catalogue table, rules, persistence, Play Games sync)
- [ ] 10.3 Add `docs/achievements.md`: the growth rules (add only, reachable with shipped levels, XP budget table from design 1a) and a checklist for adding a level pack (catalogue entries, strings, icons via `generate-icons.mjs`, Play Console entries, pinned-list update)
- [ ] 10.2 Add `docs/play-games-setup.md`: the Play Console checklist (create the app, PGS project, OAuth credentials for debug/upload/app-signing SHA-1, all catalogue achievements with names, descriptions, XP, steps, hidden flags and icons, testers, export `games-ids.xml`)

## 11. Verification

- [ ] 11.1 Run `./gradlew :core:test` and `./gradlew :app:testDebugUnitTest`; both pass
- [ ] 11.2 Run the affected instrumented tests (migration, settings, achievements screen, result dialogs, resource check) on the emulator; they pass
- [ ] 11.3 Manual smoke test once the Play Console project exists: install a tester build, sign in from Settings, unlock Wet Feet, see it in the Play Games achievements UI
