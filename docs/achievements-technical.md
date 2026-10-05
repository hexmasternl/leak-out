# Achievements: technical reference

How achievements work in Leak Out: the catalogue, the exact rules, where the data lives, how it reaches Google Play Games, and the UI that shows it. This is the reference copy of the design from the OpenSpec change `app-game-achievements` (archived under `openspec/changes/archive/`), updated to match the code as it was built.

Related pages:

- [achievements.md](achievements.md): how the catalogue may grow, and the checklist for adding a level pack.
- [achievements-setup.md](achievements-setup.md): the Play Console setup (project, credentials, achievements, testers, ids, publishing).
- [project-plan.md, section 2.11](project-plan.md#211-achievements): the short summary in the project plan.
- [`assets/achievements/`](../assets/achievements/): the icon sources, PNGs and scripts.

- [Overview](#overview)
- [The catalogue](#the-catalogue)
- [Precise definitions](#precise-definitions)
- [Attempt facts in the engine](#attempt-facts-in-the-engine)
- [Rules](#rules)
- [Local store](#local-store)
- [Tracking attempts](#tracking-attempts)
- [Play Games](#play-games)
- [UI](#ui)
- [Icons](#icons)
- [Tests](#tests)
- [Known trade-offs](#known-trade-offs)

## Overview

```
 core (pure Kotlin)                          app (Android)
 ──────────────────                          ─────────────
 GameEngine ─▶ GameState (attempt facts)
                  │
                  ▼
 AttemptSummary.from(state, record)  ◀────── GameSession (once per attempt)
 PlayerStats.after / afterAbandoned                │
 AchievementRules.evaluate ◀──────────────── AchievementTracker ──▶ AchievementRepository (Room)
                                                                          │ observeAll()
                                              ResultDialogs ◀─ unlockedThisAttempt
                                              AchievementsScreen ◀────────┤
                                              Settings (count) ◀──────────┤
                                                                          ▼
                                              AchievementSyncer ──▶ AchievementSync ──▶ Google Play Games
                                                 (only while signed in)    unlock / setSteps
```

- **The device is the source of truth.** Achievements are decided by pure rules in `core`, stored in the app's Room database, and shown from there, whether or not the player is signed in to Play Games.
- **Play Games is a mirror.** While signed in, the syncer pushes the whole local state with idempotent calls. Nothing is ever read back from Play Games.
- **Nothing is ever revoked.** No reset, level change or new catalogue version takes an achievement away or lowers its progress.
- **Not included:** leaderboards, saved games, events or friends, notifications outside the game, and recording losses in the level history (losses go to the separate player stats).

## The catalogue

`core/achievements/Achievement.kt` holds the catalogue as an enum. Each entry has:

| Field | Meaning |
|---|---|
| `key` | Stable snake_case id. It is stored in the database and names the Play Games id resource `achievement_<key>`. The enum entry is the upper-case key (`ZERO_WASTE` ↔ `zero_waste`). |
| `group` | `AchievementGroup`: `PROGRESS`, `SKILL`, `LIFETIME` or `HIDDEN`. |
| `type` | `AchievementType.Standard`, or `AchievementType.Incremental(steps, measure)`. |
| `xp` | Play Games points. A multiple of 5. |
| `hidden` | Hidden in Play Games and on the achievements screen until unlocked. |
| `pack` | The library page for `pack_<n>_*` entries, otherwise `null`. |

There are also `steps` (null for standard achievements), `isIncremental`, and in the companion `MAX_TOTAL_XP = 1000`, `STEPS_RANGE = 2..10_000`, `totalXp` and `fromKey(key)`.

`Measure` says what an incremental achievement counts: `LEVELS_FINISHED`, `STARS`, `THREE_STAR_LEVELS`, `PACK_LEVELS_FINISHED`, `PACK_THREE_STAR_LEVELS` or `SECTIONS_PLACED`.

Names and descriptions are not in `core`. They are Android string resources (`achievement_name_<key>`, `achievement_desc_<key>`), so they can be translated, and they must match the Play Console text exactly.

### Launch catalogue

25 achievements, 595 of the 1,000 XP Play Games allows. Group totals: progress 170, skill 230, lifetime 115, hidden 80.

| Key | Name | Group | Type / steps | Hidden | XP | Unlocks when |
|---|---|---|---|---|---|---|
| `first_leak_fixed` | First Leak Fixed | progress | standard | | 10 | level 1 is in the history |
| `levels_finished_1` | Getting the Hang of It | progress | incremental 10 | | 15 | 10 levels finished |
| `star_collector_1` | Star Collector I | progress | incremental 25 | | 15 | 25 stars in total |
| `star_collector_2` | Star Collector II | progress | incremental 60 | | 30 | 60 stars in total |
| `perfectionist_1` | Perfectionist I | progress | incremental 10 | | 30 | 10 levels with 3 stars |
| `pack_1_complete` | Pack 1 Complete | progress | incremental 24 | | 30 | every level of library page 1 finished |
| `pack_1_perfect` | Pack 1 Perfect | progress | incremental 24 | | 40 | every level of library page 1 with 3 stars |
| `zero_waste` | Zero Waste | skill | standard | | 30 | a win with no unused sections |
| `double_cross` | Double Cross | skill | standard | | 20 | the liquid left a cross twice (both channels) in one attempt |
| `crossroads_king` | Crossroads King | skill | standard | | 40 | 3 crosses each left twice in one attempt |
| `ahead_of_the_flow` | Ahead of the Flow | skill | standard | | 30 | a win where the route was complete while the flow was still waiting |
| `full_throttle` | Full Throttle | skill | standard | | 15 | a win while fast forward is on |
| `measure_twice` | Measure Twice | skill | standard | | 20 | a win with no replacements |
| `steady_hands` | Steady Hands | skill | standard | | 15 | a win without ever being paused |
| `long_way_round` | The Long Way Round | skill | standard | | 30 | a win where the liquid left 30 or more distinct placed tiles |
| `big_spender` | Big Spender | skill | standard | | 30 | a win with a final total of 500 or more |
| `apprentice` | Apprentice | lifetime | incremental 100 | | 10 | 100 sections placed |
| `journeyman` | Journeyman | lifetime | incremental 1,000 | | 25 | 1,000 sections placed |
| `guildmaster` | Guildmaster | lifetime | incremental 5,000 | | 40 | 5,000 sections placed |
| `never_give_up` | Never Give Up | lifetime | standard | | 20 | a win on a level right after 3 or more losses in a row on that level |
| `personal_best` | Personal Best | lifetime | standard | | 15 | a win beats an existing highscore |
| `wet_feet` | Wet Feet | lifetime | standard | | 5 | the first loss ever |
| `instant_flood` | Instant Flood | hidden | standard | ✔ | 20 | a loss before the liquid has left any placed section |
| `wall_to_wall` | Wall to Wall | hidden | standard | ✔ | 30 | a win where every free cell held a section when the level was won |
| `photo_finish` | Photo Finish | hidden | standard | ✔ | 30 | a win where the route-completing section went on the tile the liquid head was about to enter |

### Later additions

`app-game-more-achievements` (issue #64) added 14 achievements after the launch entries, for 150 XP. The catalogue now holds 39 achievements, 745 of the 1,000 XP. Group totals: progress 195, skill 340, lifetime 120, hidden 90.

| Key | Name | Group | Type / steps | Hidden | XP | Unlocks when |
|---|---|---|---|---|---|---|
| `tips_addict` | Tips Addict | lifetime | standard | | 5 | the seen tips were reset with "Show tips again" |
| `long_way_round_2` | The Long Way Round II | skill | standard | | 10 | a win where the liquid left 50 or more distinct placed tiles |
| `long_way_round_3` | The Long Way Round III | skill | standard | | 15 | the same with 100 or more |
| `big_spender_2` | Big Spender II | skill | standard | | 15 | a win with a final total of 1,000 or more |
| `crossroad_master` | Crossroad Master | skill | standard | | 15 | a win where the liquid left 3 crosses one directly after the other |
| `efficiency_master` | Efficiency Master | skill | standard | | 15 | a win with no more sections on the board than the solver's shortest route has cells |
| `highway` | Highway | skill | standard | | 10 | a win where the liquid left 7 straights of the same direction in a row |
| `snake` | Snake | skill | standard | | 15 | a win where the liquid left 12 corners in a row |
| `eraser` | Eraser | skill | standard | | 5 | a win with 20 or more replacements |
| `dramatic_failure` | Dramatic Failure | hidden | standard | ✔ | 5 | a loss after the liquid left more than 50 sections (a cross twice counts twice) |
| `on_your_nerves` | On Your Nerves | hidden | standard | ✔ | 5 | 5 or more taps on the tile of one running replacement, won or lost |
| `star_collector_3` | Star Collector III | progress | incremental 100 | | 10 | 100 stars in total |
| `star_collector_4` | Star Collector IV | progress | incremental 250 | | 15 | 250 stars in total |
| `snail` | Snail | skill | standard | | 10 | a win where the liquid left 3 or more reservoirs |

### Why progress never depends on the library size

The game grows in packs of 24 levels (one library page, `LevelLibraryLayout.LEVELS_PER_PAGE`) towards 300 or more. Play Games fixes step counts once published and never deletes achievements. So progress only has two shapes, and both survive new content:

- **Milestones** count absolute numbers (levels finished, stars, levels with 3 stars). A new tier is a new achievement with a higher count.
- **Pack achievements** (`pack_<n>_complete`, `pack_<n>_perfect`) belong to library page *n*. A shipped page always holds 24 levels, so their 24 steps are structural. Pack progress is counted with `LevelLibraryLayout.pageOf`.

The growth rules and the XP budget for later packs are in [achievements.md](achievements.md).

## Precise definitions

| Term | Definition |
|---|---|
| Free cell | A grid cell that is not blocked, not the start and not the finish (`AttemptSummary.freeCells(level)`). |
| Distinct placed tiles left (Long Way Round) | The number of tiles in `ScoreSheet.passes`. A cross counts once. Mandatory sections count. |
| Cross left twice (Double Cross, Crossroads King) | An X tile with 2 or more passes in the score sheet. |
| Sections placed (lifetime) | Every accepted `Place` command, replacements included. Attempts that are restarted or exited count too. |
| Paused (Steady Hands) | Any accepted `Pause`, including the automatic pause when the app goes to the background. The engine can't tell them apart, and the description says so ("…without pausing or leaving the game"). |
| Photo finish | The placement that makes `routeComplete` true happens while the flow is `Flowing`, on the tile the head's current channel exits into. While the head is still in the start tile, the start's open side counts as the exit. |
| Instant Flood | A loss with no placed section left by the liquid (`sectionsLeft == 0`). |
| Wall to Wall | A win with `piecesOnBoard >= freeCells`. Mandatory sections count as pieces. |
| Personal Best | A win whose history record result is a new highscore. |
| Runs (Crossroad Master, Highway, Snake) | Taken from `GameState.flowPath` in flow order: consecutive crosses, consecutive straights of the same section (`H` after `H` or `V` after `V`; any other section breaks the run) and consecutive corners. Mandatory sections count. |
| Minimum sections (Efficiency Master) | `LevelSolver.solve(level)`'s path, distinct cells (a mandatory cross is on the path twice). Null for a level the solver can't solve, which never unlocks it. Without mandatory sections the solver's search is exact; with them it is greedy, so a better route still counts (`<=`). |
| Replacement taps (On Your Nerves) | `GameState.mostReplacementTaps`: the most `ReplacementTap` commands on the replaced tile during one replacement. |

## Attempt facts in the engine

`core/engine/GameState` carries seven attempt facts. The engine sets them as it handles commands; it knows nothing about achievements.

```kotlin
placements: Int                    // accepted Place commands this attempt, replacements included
replacements: Int                  // accepted Place commands that replaced a section
everPaused: Boolean                // set by an accepted Pause; Resume keeps it
routeCompletedBeforeFlow: Boolean  // routeComplete became true while the flow was Waiting
photoFinish: Boolean               // see the definition above
flowPath: List<PathStep>           // every exit of the liquid from a section, in flow order: PathStep(at, section)
mostReplacementTaps: Int           // the most ReplacementTap commands one replacement got
```

- `flowPath` grows where the flow awards a section's points (`FlowSimulation.leaveTile`), so a cross left twice is in it twice. `RemoveUnused` only takes sections the liquid never entered and leaves it alone.
- `GameCommand.ReplacementTap(at)` adds one to `Replacement.taps` when `at` is the replaced tile and a replacement runs (not paused, not ended); anything else is refused. It places nothing and doesn't move the landing. `GameSession.place` sends it instead of `Place` while `isReplacing` and still returns `PlaceResult.Rejected`, which is silent during a replacement.

- They have no defaults. `GameEngine.start` is the only place a state is built, so `Start` and `Restart` reset them with the rest of the attempt.
- Refused commands leave them unchanged.
- The route-completion facts are set in one private helper (`withBoard`) only when `routeComplete` goes from false to true. It is used by both `place` and `land`, so a replacement that completes the route when it lands counts too.

## Rules

Everything that decides achievements is pure and lives in `core/achievements/`.

### AttemptSummary

`AttemptSummary.from(state, record: RecordOutcome?)` captures one ended attempt from the state at the moment the outcome first became won or lost. It throws while the attempt is still playing.

Fields: `levelId`, `outcome` (`WON` / `LOST`), `finalTotal`, `finalStars`, `unusedSections`, `distinctTilesLeft`, `sectionsLeft`, `crossesLeftTwice`, `placements`, `replacements`, `everPaused`, `fastForwardAtWin`, `routeCompletedBeforeFlow`, `photoFinish`, `freeCells`, `piecesOnBoard`, `record`, `longestCrossChain`, `longestStraightRun`, `longestCornerRun`, `reservoirsLeft`, `mostReplacementTaps`, `minimumSections`.

- The route-shape numbers are computed from `flowPath` here, so the rules never see the list. `minimumSections` runs the level solver once per ended attempt (milliseconds on the bundled levels).

- `piecesOnBoard` adds back `sectionsRemoved` on a win, so the summary is right even when it is built after `RemoveUnused`.
- `RecordOutcome` (`FIRST_COMPLETION`, `NEW_HIGHSCORE`, `NO_IMPROVEMENT`) is the core copy of the app's `RecordResult`. The app maps it with `RecordResult.toRecordOutcome()` (`app/achievements/AttemptSummaries.kt`). It is ignored on a loss.

### PlayerStats

`PlayerStats(sectionsPlaced, losses, lossStreakByLevel, tipsResets)` holds the lifetime counters the level history doesn't keep.

- `after(summary)` adds the attempt's placements. A loss adds 1 to `losses` and to that level's streak; a win clears that level's streak.
- `afterAbandoned(placements)` adds the placements of a restarted or exited attempt. Negative numbers are refused.
- `afterTipsReset()` adds 1 to `tipsResets` (Tips Addict). Like every stat it survives a progress reset.

### AchievementRules.evaluate

```kotlin
AchievementRules.evaluate(
    summary: AttemptSummary?,                       // null at start-up and for abandoned attempts
    statsBefore: PlayerStats,
    statsAfter: PlayerStats,
    history: Map<Int, Int>,                         // level id → stored stars (0–3)
    stored: Map<Achievement, StoredAchievement>,    // what is already in the store
): Evaluation(newlyUnlocked: List<Achievement>, steps: Map<Achievement, Int>)
```

- With `summary == null`, only progress and lifetime achievements can unlock. That is what the start-up backfill uses.
- Progress achievements follow the level history alone. A level counts as finished when it has a record with any number of stars. First Leak Fixed needs a record for level 1.
- Never Give Up uses the loss streak from `statsBefore`, because the win has already cleared it in `statsAfter`.
- Wet Feet is decided from `statsAfter.losses >= 1`, so it also works without a summary.
- Never Give Up and Personal Best need a summary.
- `steps` has an entry for every incremental achievement: `min(total, max(stored, computed))`, or the full total once the achievement is stored as unlocked. Progress never goes down, even when a reset lowers the star count.
- `newlyUnlocked` is in catalogue order and never repeats a stored unlock.
- Thresholds are constants: `NEVER_GIVE_UP_LOSSES = 3`, `CROSSROADS_KING_CROSSES = 3`, `LONG_WAY_ROUND_TILES = 30`, `BIG_SPENDER_TOTAL = 500`.
- `AchievementRules.progress(measure, pack, history, stats)` computes an incremental achievement's current count. The achievements screen uses the stored steps, which come from it.

## Local store

The achievements live in the existing Room database `LeakOutDatabase` (`app/history/data/`), in version 3. `MIGRATION_1_2` only creates the three new tables, so existing history is untouched; `MIGRATION_2_3` adds `player_stats.tips_resets` (`NOT NULL DEFAULT 0`) and keeps every row. Every schema version is exported to `src/app/schemas/` and committed. There is no destructive fallback.

| Table | Columns |
|---|---|
| `achievement_state` | `key` TEXT PK, `steps` INT, `unlocked_at_ms` INT NULL |
| `player_stats` | `id` INT PK (always 0), `sections_placed` INT, `losses` INT, `tips_resets` INT (version 3) |
| `level_loss_streak` | `level_id` INT PK, `streak` INT |

Rows are stored by key, not by enum ordinal, so the catalogue can be reordered. A catalogue entry without a row is locked with 0 steps.

`app/achievements/AchievementRepository.kt`:

```kotlin
data class AchievementProgress(val achievement: Achievement, val steps: Int = 0, val unlockedAtMs: Long? = null) // + unlocked, toStored()

interface AchievementRepository {
    fun observeAll(): Flow<List<AchievementProgress>>   // every catalogue entry, catalogue order
    suspend fun stats(): PlayerStats
    suspend fun stored(): Map<Achievement, StoredAchievement>
    suspend fun apply(evaluation: Evaluation, stats: PlayerStats)   // one transaction; keeps an existing unlock time
    suspend fun <T> transaction(block: suspend () -> T): T
}
```

Implementations: `RoomAchievementRepository` (`achievements/data/`, with `AchievementDao` and the entities in `AchievementEntities.kt`), and `InMemoryAchievementRepository` for tests and previews.

**Reset progress keeps achievements.** `LevelHistoryRepository.clearAll()` only clears the history. Achievement state, player stats and loss streaks stay, and the reset dialog says so. Wiping them would make the app disagree with Play Games, and the next sync couldn't restore them.

## Tracking attempts

`app/achievements/AchievementTracker(achievements, history, scope)` connects the game to the rules and the store. Each call reads the stats, the stored achievements and the history, evaluates, and writes, all in one transaction. A mutex lets only one call run at a time.

| Call | When | Effect |
|---|---|---|
| `onAttemptEnded(summary)` | once per won or lost attempt, after `recordWin` | `stats.after(summary)`, returns the newly unlocked achievements for the result dialog |
| `onAttemptAbandoned(placements)` / `attemptAbandoned(placements)` | an unfinished attempt is restarted or exited | `stats.afterAbandoned(placements)`; can unlock the placement tiers |
| `reconcile()` / `launchReconcile()` | app start, and after a reset | evaluates with no summary; the backfill for existing players |

`reconcile()` does not run after a win. The win's own `onAttemptEnded` already sees the new history, and an earlier reconcile would unlock First Leak Fixed first, so the dialog wouldn't list it.

`ui/game/GameSession` adds:

- `unlockedThisAttempt: List<Achievement>`: snapshot state the result dialogs read. Empty until the tracker answers, cleared by a restart.
- `suspend fun recordAchievements(tracker)`: once per attempt, from the state captured when the outcome first became won or lost. It finishes even when the caller is cancelled, and recompositions don't count twice.
- `fun abandonAttempt(tracker)` and `fun restart(tracker): Boolean`: an unfinished attempt's placements are counted once.

A tips reset doesn't come from a game: `SettingsViewModel.onShowTipsAgain` resets the tips and then calls its `onTipsReset`, which `MainActivity` wires to `AchievementTracker.tipsReset()` (`onTipsReset()` in the application scope). It goes through the same evaluate-and-store path as an attempt, so a reconcile at start-up would also unlock Tips Addict.

`GameScreen` takes an optional `achievements: AchievementTracker?`. `MainActivity` passes the real tracker; the level designer's test play passes nothing, so test runs are never tracked. Exit from the result dialogs adds nothing, because that attempt was already counted when it ended.

### Wiring

There is no DI framework. `LeakOutApplication` builds an `AppContainer(context, scope, playGamesAppId)`, reached through the `Context.appContainer` extension. It lazily provides `history`, `achievements`, `tracker` and `playGames`, with a process-wide coroutine scope. `MainActivity` builds the view models from it with `viewModelFactory { initializer { … } }`.

## Play Games

Dependency: `com.google.android.gms:play-services-games-v2`, pinned in `src/gradle/libs.versions.toml`.

### Configuration

- `src/app/src/main/res/values/games-ids.xml` is the Play Console export: `app_id` (`201820010117`), `package_name` and one `achievement_<key>` string per achievement. The Console names resources after the achievement *names*, so five were renamed to their keys: `getting_the_hang_of_it` → `levels_finished_1`, `star_collector_i` → `star_collector_1`, `star_collector_ii` → `star_collector_2`, `perfectionist_i` → `perfectionist_1`, `the_long_way_round` → `long_way_round`.
- **An achievement not yet created in the Play Console** has its `achievement_<key>` string with an **empty** value, so the app compiles. `WithPlayGamesId` drops every call for it; once the export brings the real id, the next push sends its state. `AchievementResourcesTest` lists these keys in `awaitingExport`, and the export empties that list.
- The manifest carries `com.google.android.gms.games.APP_ID` → `@string/app_id`.
- **Not configured:** when the app id isn't a positive number (`PlayGamesServices.isConfigured`), the SDK isn't started, the sign-in state is `Unavailable` and a no-op sync is used. Tests use the id `0` for this.
- A build whose signing key has no Play Games credential (for example a debug build on a machine whose debug SHA-1 isn't registered) still starts the SDK, fails the silent sign-in and shows `SignedOut`. The game keeps working.

### Classes (`app/achievements/playgames/`)

| Class | Role |
|---|---|
| `PlayGamesServices(appId, achievements, scope, …)` | Owns `configured`, `auth` and `syncer`. `start()` runs in `Application.onCreate` and calls `PlayGamesSdk.initialize`. `attach(activity)` / `detach(activity)` run from `MainActivity.onCreate` / `onDestroy`: attach binds the sync and runs the silent sign-in check. Every v2 client needs an Activity, which is why the sync is bound per activity. |
| `PlayGamesAuth` | `state: StateFlow<SignInState>` and `checkSilently(activity)` / `signIn(activity)`. `SignInState` is `Unavailable`, `SignedOut`, `SigningIn` or `SignedIn(displayName)`. The state is `SigningIn` during the silent check too. A failed or cancelled sign-in returns to `SignedOut`. When not configured it never touches the SDK. |
| `AchievementSync` | `unlock(id)` and `setSteps(id, n)`. Implemented by `PlayGamesAchievementSync` (`unlockImmediate` / `setStepsImmediate`, so failures are visible) and `NoOpAchievementSync`. `WithPlayGamesId` wraps the Play Games one and skips achievements with an empty id. |
| `AchievementSyncer(repo, signIn, scope, log)` | `start()` combines the sign-in state, `observeAll()` and the bound sync, and pushes everything while signed in. `push(sync, all)` returns the number of failed calls. |
| `PlayGamesLog` | Injectable logger for failed calls. |
| `GmsPlayGames.kt` | The thin wrappers around the GMS clients, including `showPlayGamesAchievements(activity)`. |

### The sync

- It pushes the **full** local state when the state becomes `SignedIn` and whenever the local store changes while signed in.
- Unlocked standard achievements get `unlock`. Incremental achievements get `setSteps` with the local steps, and an unlocked incremental one gets `setSteps(total)`. An incremental with 0 steps is skipped.
- `increment` is never used: a retried increment would count twice. Because `unlock` and `setSteps` are idempotent, there is no "already sent" bookkeeping. A failed or offline push leaves the local data alone, and the next trigger simply sends everything again.
- Achievements earned before signing in reach Play Games on the first sign-in.

## UI

### Result dialogs

When the attempt unlocked achievements, the level complete and level lost dialogs show an `AchievementUnlockedRow` (`ui/game/AchievementUnlockedRow.kt`):

- a trophy icon (`res/drawable/ic_trophy.xml`), "ACHIEVEMENT UNLOCKED" in `display.xs` `brass.300`, and the names joined with " · " (at most two, then "+N more"). The text logic is the pure `AchievementRowText`.
- It pops in with `motion.pop`, or without animation when reduced motion is on.
- In the level complete dialog it sits under the score rows and only appears once the tally is done. It never delays the buttons. In the level lost dialog there's no tally, so it appears straight away under the score.
- A screen reader reads it as "Achievement unlocked: " followed by all names.
- `ResultDialogLayout` measures again (`measureKey`) when the tracker answers after the dialog opened, so the Regular/Compact layout choice stays right.
- When signed in, Play Games also shows its own popup. That double feedback is accepted.

### Settings: Play Games panel

`ui/settings/PlayGamesPanel.kt`, at the top of the right column above Progress:

```
│ PLAY GAMES                      │
│ ● Signed in as <name>           │   or "Not signed in"  [ Sign in ]
│ [ Achievements      12 / 39 ]   │
```

- `SignedOut`: "Not signed in" and a glass Sign in button. `SigningIn`: the button is disabled. `SignedIn`: "Signed in as <name>" ("Signed in" without a name), no button. `Unavailable`: "Play Games is not available", no button.
- The Achievements button is always there, because the screen works from local data. It shows the unlocked count through the shared `FlowButton`'s optional `trailing` text.
- `SettingsViewModel` exposes a `PlayGamesUiState` and calls back on a reset, so the tracker can reconcile.
- The reset dialog body says "Achievements are kept.", and the About privacy line says achievements are shared with Google Play only when the player signs in.

### Achievements screen

`Screen.Achievements` in `MainActivity`, opened from Settings; the back button and system Back return to Settings. `ui/achievements/AchievementsViewModel` builds `AchievementCard`s from `observeAll()` and the sign-in state. `AchievementsScreen` shows:

- a top bar: back, "ACHIEVEMENTS", "N / 25", and, only while signed in, a glass Play Games button that opens the Play Games achievements UI (`showPlayGamesAchievements`);
- a lazy grid of level-2 cards in catalogue order:
  - **Unlocked**: colour icon, name, description, unlock date (medium style, device locale), `success.tint` wash.
  - **Locked**: icon at 40 % and desaturated, name and description. Incremental ones also get a `liquid.500` progress bar with "x / y".
  - **Hidden and locked**: the `achievement_hidden` medallion, "Hidden achievement", "Keep playing to find out."
- Each card is read by a screen reader as one item: name, locked or unlocked, "x of y", description and date.

### Resource mapping

`app/achievements/AchievementResources.kt` maps every `Achievement` to `AchievementRes(name, description, icon, playGamesId)` with an exhaustive `when`, so a new catalogue entry without resources doesn't compile. `HIDDEN_ICON` is the hidden placeholder.

## Icons

Source and output are in [`assets/achievements/`](../assets/achievements/):

| File | What |
|---|---|
| `generate-icons.mjs` | Writes every `achievement_<key>.svg`: `node generate-icons.mjs .` from that folder. The design-system tokens are constants at the top. |
| `achievement_<key>.svg` | The 40 SVG sources: 39 achievements plus `achievement_hidden`. |
| `render-icons.js` | Renders the SVGs to 512 × 512 PNGs in `png/` with the globally installed Playwright Chromium: `node render-icons.js`, or `node render-icons.js <key>…` for some. |
| `png/` | The rendered PNGs. These went to the Play Console and are copied to `src/app/src/main/res/drawable-nodpi/achievement_<key>.png`. |
| `preview.html` | A contact sheet of all PNGs. |

The PNGs in `png/` and in `drawable-nodpi/` are the ones that were uploaded to the Play Console. A re-render from the SVGs looks the same but isn't byte-identical (Chromium writes RGB instead of RGBA), so only re-render icons you're changing.

Style (Deep Flow):

- A full-bleed `surface.deep` background with a radial vignette. Play Games may crop to a circle, so everything important sits inside a centred disc of radius 200.
- The medallion is a `surface.raised → surface.base` disc with a `line.highlight` top edge and a ring whose colour gives the group: progress and the single lifetime achievements `liquid.500`, skill `brass.500`, hidden `reservoir.500`. Tiered sets use `steel.300` (tier I), `liquid.300` (tier II), `brass.300` (tier III) and `liquid.glow` (tier IV). A higher tier of a skill achievement shrinks the first tier's glyph to make room for the numeral.
- Pack achievements show the pack number in roman numerals under a trophy (complete) or three stars (perfect). Later tiers and packs reuse these templates in the generator.
- Glyphs use the pipe recipe (steel body, bore, aqua liquid with `liquid.glow`) or the rounded-stroke icon style. Liquid is the brightest thing in each icon.
- Roman numerals and pips are drawn as paths in Wendy One shapes, so they render without the font.
- Vector drawables aren't used, because the glows need blur filters `VectorDrawable` can't draw.
- Play Games makes its own greyed-out locked version and hidden placeholder. The app desaturates locked icons itself.

## Tests

| Where | Test | What it proves |
|---|---|---|
| core | `AttemptFactsTest` | Every attempt-fact scenario: placements, refused placements, pause, completion during the countdown, photo finish and its negatives, the flow path (order, a cross twice, kept by `RemoveUnused`), replacement taps, restart clears. |
| core | `AchievementCatalogueTest` | XP 745 (≤ 1,000), group totals, multiples of 5, steps within 2–10,000, unique snake_case keys, hidden flags, pack ids, and the **pinned `published` list** that may only grow. |
| core | `AchievementRulesTest` | Every rule scenario per group, idempotence, no repeated unlocks. |
| core | `AttemptSummaryTest`, `PlayerStatsTest` | Summaries from real engine attempts; the stats updates. |
| core | `AchievementReachabilityTest` | Every skill and hidden achievement unlocked by an attempt played through the engine on a bundled level (scripted with `ScriptedPlay`, whose `routeThrough` finds a route through a given row or staircase of cells); Tips Addict from the stats alone. |
| core | `AchievementContentTest` | Milestones fit the bundled levels; pack achievements exist only for full pages; pack progress uses `LevelLibraryLayout.pageOf`. |
| core | `ThreeStarReachabilityTest` | 3 stars can be reached on every page 1 level, so Perfectionist I and Pack 1 Perfect can be earned. The smallest margin is level 3 (630 against 600). |
| app JVM | `AchievementTrackerTest` | Once per attempt, abandoned attempts counted once, backfill from history, reset keeps achievements. |
| app JVM | `GameSessionAchievementsTest`, `GameSessionPlaceTest` | No double counting on recomposition; restart and exit add placements; taps during a replacement are counted, silent and reach the tracker. |
| app JVM | `WithPlayGamesIdTest` | No call for an achievement with an empty Play Games id; the others still go out. |
| app JVM | `PlayGamesAuthTest`, `AchievementSyncerTest`, `PlayGamesServicesTest` | Sign-in states; push on sign-in and on change, idempotent repeats, failures leave local data alone; the unconfigured fallback makes no calls. |
| app JVM | `AchievementResourcesTest`, `AchievementRowTextTest`, `AchievementsViewModelTest`, `SettingsViewModelTest` | Strings pinned to the Console texts, 512 px icons; row text; card models; panel state. |
| instrumented | `LeakOutDatabaseMigrationTest`, `AchievementDatabaseTest` | Migration 1→2 keeps history rows; 2→3 keeps history, unlocks and stats and starts the tips resets at 0; the Room repository. |
| instrumented | `AchievementResourcesInstrumentedTest` | Every key has a non-empty Play Games id and no two share one. |
| instrumented | `AchievementRowDialogsTest`, `LevelCompleteTest`, `LevelLostTest` | The unlock row in both dialogs: one, four and no achievements; screen reader text. |
| instrumented | `SettingsPlayGamesTest`, `SettingsScreenTest`, `AchievementsScreenTest`, `MainActivityTest` | The panel in all four states, the count and navigation, the screen's cards, the Play Games button, Back. |

The real Play Games UI can't be launched from a test. Signing in and seeing an unlock in Play Games is a manual smoke test with a tester account ([achievements-setup.md, step 8](achievements-setup.md#8-smoke-test)).

## Known trade-offs

- **Steady Hands also fails on an automatic pause** (the app went to the background). Accepted, and stated in the description.
- **Double feedback on unlock** while signed in: the dialog row plus the Play Games popup. Accepted.
- **Abandoned attempts unlock silently.** Placement tiers reached by a restarted or exited attempt show up on the achievements screen and, when signed in, in the Play Games popup, but not in a dialog.
- **The XP budget is finite.** The guideline in [achievements.md](achievements.md) spends 240 of the remaining 255 XP over packs 2–13 and six milestone tiers. Beyond pack 13, XP has to be freed in the guideline before it is spent. Whether Play Games accepts 0-XP achievements is unverified.
- **English only.** Names and descriptions are string resources, ready for translation, but every language also needs the same text in the Play Console.
- **No Daylight icon variant.** The icons are full-bleed `surface.deep`, like the board plate.
- **Rollback:** removing the Play Games dependency leaves the local achievements working.
