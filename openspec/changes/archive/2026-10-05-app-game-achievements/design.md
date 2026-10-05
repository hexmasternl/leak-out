## Context

The engine (`core/engine`) is pure and immutable. One `GameState` describes an attempt, and `GameSession` in `app` drives it and records a win through `LevelHistoryRepository.recordWin`, which returns a `RecordResult` (`FirstCompletion`, `NewHighscore`, `NoImprovement`). The level history keeps one row per finished level (best score and its stars, 0-3) and deliberately records nothing about losses. Settings live in DataStore; history lives in Room (`LeakOutDatabase`). The settings screen (`app-settings-page`) has Audio, Feel, Progress and About panels, and its About panel currently promises "no account, data stays on the device".

The app has no Google Play dependencies. The app is in the Play Console (closed testing), and the owner has set up the PGS project (app id `201820010117`) with the 25 launch achievements. The export is in this change as `games-ids.xml`. Play Games Services (PGS) v2 needs a Play Console app, a PGS project linked to OAuth credentials for each signing key, and the achievements defined in the console. Their generated ids go in `games-ids.xml`.

There are 24 bundled levels on grids from 8×6 to 16×12, which fill library page 1. More levels will follow in packs of 24, up to 300 or more. Crosses can be drawn from level 12 on, so cross achievements are possible from level 12.

## Goals / Non-Goals

**Goals:**
- 25 achievements, decided by pure, unit-tested rules in `core`.
- Achievements work fully without Play Games: local storage is the source of truth, and the in-app screen reads it.
- Silent PGS sign-in, a manual sign-in on the settings screen, and an idempotent sync that can always "push everything".
- Existing players get the progress achievements they already earned.
- 25 icons in the Deep Flow style that work in the Play Console (512×512) and in the app.

**Non-Goals:**
- Leaderboards, saved games, events or friends from PGS.
- Revoking achievements. Nothing ever un-unlocks: not a reset, not a level change.
- Achievement notifications outside a game (push, widgets).
- Recording losses in the level history. Losses go to the separate player stats.
- Publishing the app to production. Internal testing is enough to use PGS.

## Decisions

### 1. The catalogue

`Achievement` is an enum in `core/achievements`. Each entry has a stable `key` (snake_case, also used as the `games-ids.xml` resource name `achievement_<key>`), a group, a type, `steps` for incremental achievements, `hidden` and `xp`. Names and descriptions are Android string resources, so they can be translated. The catalogue has a unit test that checks the XP adds up to at most 1,000, every XP value is a multiple of 5, keys are unique, and steps are between 2 and 10,000 (PGS limits).

The launch catalogue (24 levels, one library page). XP shown per row; the launch total is **595**:

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
| `double_cross` | Double Cross | skill | standard | | 20 | liquid left a cross twice (both channels) in one attempt |
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
| `personal_best` | Personal Best | lifetime | standard | | 15 | a win beats an existing highscore (`RecordResult.NewHighscore`) |
| `wet_feet` | Wet Feet | lifetime | standard | | 5 | the first loss ever |
| `instant_flood` | Instant Flood | hidden | standard | ✔ | 20 | a loss before the liquid has left any placed section |
| `wall_to_wall` | Wall to Wall | hidden | standard | ✔ | 30 | a win where every free cell held a section when the level was won |
| `photo_finish` | Photo Finish | hidden | standard | ✔ | 30 | a win where the route-completing section went on the tile the liquid head was about to enter |

Group totals: progress 170, skill 230, lifetime 115, hidden 80.

**No achievement depends on "all levels" or on the size of the library.** The game will grow in packs of 24 levels (one library page each, `LevelLibraryLayout.LEVELS_PER_PAGE`) towards 300 or more. PGS step counts are fixed once published and published achievements can't be deleted. So progress uses two shapes that survive new content:
- **Milestones**: absolute counts (levels finished, stars, levels with 3 stars). A new tier is a new achievement with a higher count. Existing tiers never change.
- **Pack achievements**: `pack_<n>_complete` and `pack_<n>_perfect`, tied to library page *n*. A page always holds exactly 24 levels once it is shipped, so the 24 steps are structural, not "the first 24 levels".

### 1a. Growing the catalogue

Rules for every later change that adds achievements, usually together with a level pack:
1. **Add only.** Never remove an achievement, and never change a published key, steps, hidden flag or XP. The catalogue test pins the published entries.
2. **Only publish what can be reached.** A milestone's count must be reachable with the shipped levels: levels finished ≤ level count, stars ≤ 3 × level count, 3-star levels ≤ level count. Pack achievements only for pages that are shipped and full. A test checks this against the bundled library, so a tier can't be published before its levels exist.
3. **Mind the XP budget.** The total stays ≤ 1,000. About 405 XP is held back after launch. A guideline for spending it:

| What | When | XP |
|---|---|---|
| `pack_<n>_complete` + `pack_<n>_perfect` | with each new pack, packs 2-13 | 10 + 10 per pack, 240 for 12 packs |
| Levels finished 50 / 100 (*Master Plumber*) / 250 | from 50 / 100 / 250 levels shipped | 20 / 30 / 40 |
| Star Collector III / IV: 150 / 400 stars | from 50 / 134 levels shipped | 20 / 30 |
| Perfectionist II: 50 levels with 3 stars | from 50 levels shipped | 25 |
| **Total** | | **405** |

4. **Each addition is its own OpenSpec change.** It covers the catalogue entry, the strings, the icon (made with `icons/generate-icons.mjs`) and the Play Console entry. Players who already meet the new condition get it on the next start through the same backfill as existing players (decision 4).

*Alternative:* publish the whole ladder up to 300 levels at launch (rejected: the top tiers would be unreachable for a long time, the thresholds would be fixed before the levels exist, and no XP would be left for new packs).

### 1b. Precise definitions

- *Free cell*: a grid cell that is not blocked, not the start and not the finish.
- *Distinct placed tiles left* (Long Way Round): the number of tiles in `ScoreSheet.passes`. A cross counts once.
- *Sections placed* (lifetime): every accepted `Place` command, replacements included. Attempts that are restarted or exited count too.
- *Paused* (Steady Hands): any `Pause` command, including the automatic pause when the app goes to the background. The engine cannot tell them apart, and a player who leaves the app mid-run was not steady. *Alternative:* a separate `AutoPause` command (rejected: it changes the engine contract for one achievement).
- *Photo finish*: the placement that makes `routeComplete` true happens while the flow is `Flowing`, and the head's current channel exits into the placed tile.

### 2. Attempt facts in the engine

`GameState` gains:

```
placements: Int                  // accepted Place commands this attempt, replacements included
replacements: Int                // accepted Place commands that replaced a section
everPaused: Boolean              // set by an accepted Pause
routeCompletedBeforeFlow: Boolean// set when routeComplete becomes true while flow is Waiting
photoFinish: Boolean             // set as defined above
```

All are reset by `Start` and `Restart`, like the rest of the state. They are plain data the engine sets as it handles commands. It knows nothing about achievements. *Alternative:* derive them in `app` by watching state changes (rejected: logic outside `core`, and it can't be tested with a pure engine test).

### 3. Rules: one pure function

```
AttemptSummary(levelId, outcome won/lost, finalScore?, finalStars, passes, crossDoubles,
               placements, replacements, everPaused, fastForwardAtWin, routeCompletedBeforeFlow,
               photoFinish, freeCells, piecesOnBoardAtWin, sectionsLeftAtEnd, recordResult?)
PlayerStats(sectionsPlaced, losses, lossStreakByLevel: Map<Int, Int>)

AchievementRules.evaluate(summary?, statsAfter, history, alreadyUnlocked)
    -> Evaluation(newlyUnlocked: List<Achievement>, steps: Map<Achievement, Int>)
```

`AttemptSummary.from(state, recordResult)` is built from the state at the moment the outcome is first won or lost, before `RemoveUnused` takes sections off the board. With `summary = null`, the function evaluates only progress and lifetime achievements. This handles start-up and the backfill for existing players. `steps` is clamped to each achievement's total and never goes down (`max(stored, computed)`), so a reset that lowers the star count can't lower progress.

`PlayerStats.after(summary)` is a pure update. Placements are added. A loss adds to `losses` and to that level's streak. A win clears that level's streak, but `Never Give Up` is evaluated with the streak from before the win.

### 4. Local store

The existing `LeakOutDatabase` gets new tables in a version bump with a Room `Migration`. The migration only creates tables, so existing data is untouched.

- `achievement_state(key PK, steps INT, unlocked_at_ms INT NULL)`
- `player_stats(id PK = 0, sections_placed INT, losses INT)`
- `level_loss_streak(level_id PK, streak INT)`

`AchievementRepository` (interface, Room and in-memory implementations) exposes `observeAll(): Flow<List<AchievementProgress>>` and `suspend fun apply(evaluation, stats)`, which runs in one transaction. The key, not the enum ordinal, is stored, so the catalogue can be reordered.

`AchievementTracker` in `app` ties it together. `GameSession` calls it:
- **On win or loss**, once per attempt, after `recordWin` (so the `RecordResult` and the updated history are known). It returns the newly unlocked achievements for the dialog.
- **On restart or exit of an unfinished attempt**, to add its placements to the stats. These can unlock Apprentice and the other placement tiers, which are shown nowhere in-game. The Play Games popup covers it when signed in.
- **At app start**, with `summary = null`. This awards progress achievements to existing players, and again after any history change. It is cheap and idempotent.

### 5. Reset progress keeps achievements

`clearAll()` on the history stays history-only. Achievement state, player stats and loss streaks are kept. The reset dialog body says so. *Alternative:* also wipe local achievements (rejected: Play Games keeps them, so the app and Play Games would disagree, and the next sync couldn't restore them locally).

### 6. Play Games: sync, not source

```
 AchievementRepository ──observeAll──▶ AchievementSyncer ──▶ AchievementSync (interface)
                                         on sign-in              ├─ PlayGamesAchievementSync
                                         on any change           │    unlock(id) / setSteps(id, n)
                                                                 └─ NoOpAchievementSync
```

- `PlayGamesSdk.initialize(context)` runs in a new `LeakOutApplication.onCreate`. `GamesSignInClient.isAuthenticated()` runs at start: this is the silent sign-in.
- `PlayGamesAuth` exposes `state: StateFlow<SignInState>` (`Unavailable`, `SignedOut`, `SigningIn`, `SignedIn(displayName)`) and `signIn(activity)`, which calls `GamesSignInClient.signIn()`.
- The syncer pushes the full state whenever the state becomes `SignedIn` and whenever the local store changes while signed in. Incremental achievements use `setSteps`, unlocked standard ones use `unlock`, and an unlocked incremental one gets `setSteps(total)`. These are idempotent, so there is no "already sent" bookkeeping, and an offline or failed push is simply retried on the next trigger. `increment` is never used, because a retried increment would count twice.
- **Not configured**: `games-ids.xml` ships with the real app id. When the id is not a positive number (the placeholder `0` used in tests), the SDK is not initialized, the state is `Unavailable` and the no-op sync is used. A build whose signing key has no PGS credential, such as a debug build on a machine without a registered debug SHA-1, still starts the SDK, fails the silent sign-in and shows `SignedOut`. Debug and CI builds keep working.
- The ids come from `games-ids.xml` (`R.string.achievement_<key>`). The Console names its resources after the achievement *names*, so five were renamed to their keys in the export: `getting_the_hang_of_it` → `levels_finished_1`, `star_collector_i` → `star_collector_1`, `star_collector_ii` → `star_collector_2`, `perfectionist_i` → `perfectionist_1` and `the_long_way_round` → `long_way_round`. A test checks that every catalogue key has a string resource.

*Alternative:* Play Games as the only store (rejected: nothing works for players who decline sign-in, and the emulator tests would need a real account).

### 7. Settings screen: Play Games panel

A new "Play Games" panel goes at the top of the right column, above Progress:

```
│ PLAY GAMES                      │
│ ● Signed in as Eduard           │   or "Not signed in"  [ Sign in ]
│ [ Achievements      12 / 25 ]   │   glass button, opens the achievements screen
```

- With state `SignedOut`, it shows a glass "Sign in" button. While `SigningIn`, the button is disabled. With `Unavailable` (not configured), the panel shows "Play Games is not available" and no Sign in button.
- The **Achievements** button is always there, because the screen works from local data. It shows the unlocked count.
- The About privacy line becomes: "No account needed. If you sign in to Google Play Games, your achievements are shared with Google Play."
- The reset dialog body becomes: "All finished levels, highscores and stars are deleted. Achievements are kept. This cannot be undone."

### 8. Achievements screen

`Screen.Achievements` in `MainActivity`. Back returns to Settings. The layout is a landscape grid of level 2 cards (icon 64 dp, name `display.sm`, description `body.sm`):

- **Unlocked**: the colour icon, the unlock date in `caption`, and a `success.tint` wash.
- **Locked**: the icon at 40 % with a desaturating filter, plus a progress bar (`liquid.500` on `line.subtle`, `radius.pill`) with "37 / 100" for incremental achievements.
- **Hidden and locked**: a generic "?" medallion, the name "Hidden achievement" and the description "Keep playing to find out."

The top bar has the back button, the title "Achievements", "12 / 25" and, only while `SignedIn`, a glass "Play Games" button that launches `AchievementsClient.getAchievementsIntent()`.

### 9. Unlock row in the result dialogs

When the attempt unlocked achievements, the level complete and level lost dialogs get one row under the score rows: a trophy icon, "Achievement unlocked" in `display.xs` `brass.300`, and the names joined with " · " (at most two names, then "+N more"). It pops in with `motion.pop` once the tally animation is done. The row never delays the buttons. When signed in, Play Games also shows its own popup. That is accepted: the dialog row is part of the result, and the popup is platform behaviour.

### 10. Icons

25 icons, 512×512, kept in this change under `icons/` as SVG source plus rendered PNG. After approval they are copied to the Play Console and to `app/src/main/res/drawable-nodpi/achievement_<key>.png`. Vector drawables are not used, because the glows need blur filters that `VectorDrawable` can't draw.

The style follows Deep Flow:
- A full-bleed `surface.deep` background with a radial vignette. Play Games may crop to a circle, so everything important sits inside a centred disc of radius 200.
- The medallion is a `surface.raised → surface.base` disc with a 1-pixel-style `line.highlight` top edge and a ring whose colour gives the group: progress and the single lifetime achievements `liquid.500`, skill `brass.500`, hidden `reservoir.500`. Tiered sets use `steel.300` (tier I), `liquid.300` (tier II) and `brass.300` (tier III): Apprentice/Journeyman/Guildmaster, Star Collector, Perfectionist and the levels-finished milestones. Pack achievements show the pack number in roman numerals under a trophy (complete) or three stars (perfect). Later tiers and packs reuse these templates through the generator.
- The proposed set is in `icons/`: SVG sources made by `icons/generate-icons.mjs` (design-system tokens as constants), the 512×512 PNGs in `icons/png/`, and a contact sheet in `icons/preview.html`. `achievement_hidden` is the placeholder for locked hidden achievements in the app. Play Games makes its own placeholder for those.
- The glyph is drawn with the pipe recipe (steel body, bore, aqua liquid with `liquid.glow`) or with the rounded-stroke icon style (round caps and joins), scaled to the 512 grid. Liquid stays the brightest thing in each icon.
- Tiered achievements carry roman numerals or pips in `display.xs` style, using Wendy One shapes drawn as paths, so they render without the font.
- Play Games makes the greyed-out locked version itself. The app uses the desaturating filter described above.

## Risks / Trade-offs

- [The Play Console setup is outside the repo and blocks real PGS testing] → Every part except the sync is tested with the no-op and fake syncs. The tasks end with a manual setup checklist and a manual smoke test on a device with a tester account.
- [The PGS ids are only known after the console setup] → Resolved: the console setup is done and its export is `games-ids.xml` in this change. The `Unavailable` fallback stays for a missing or placeholder id.
- [The XP budget runs out before the content does] → The 1a guideline spends exactly the held-back 405 XP over 12 packs and 6 tiers. Beyond pack 13 (more than 312 levels), pack achievements only fit if XP is freed in the guideline before it is spent, for example 5 + 5 XP per pack from pack 8 on. Whether Play Games accepts 0-XP achievements is still to be checked during the console setup. The budget is rechecked in every catalogue change.
- [Achievements nobody can earn] → A `core` test plays a scripted solution with `LevelSolver` on a suitable bundled level for each skill and hidden achievement. It proves Crossroads King (level 12 or later), Long Way Round and Wall to Wall can be reached.
- [Steady Hands fails because of an automatic pause the player didn't intend] → Accepted, and stated in the achievement's description ("Win a level without pausing or leaving the game").
- [A double popup on unlock (dialog row + Play Games)] → Accepted. See decision 9.
- [The Room migration breaks existing installs] → The migration only creates tables. A Room `MigrationTestHelper` test migrates a database that has history rows.
- [Changing the catalogue after publishing] → PGS achievements can't be deleted once published. The catalogue only grows (decision 1a), and the catalogue test pins every published entry.
- [Icon cropping in Play Games] → All content stays inside the radius-200 safe disc.

## Migration Plan

1. Done: the owner created the PGS project and the 25 achievements from `icons/` and the catalogue table, added testers and registered the signing SHA-1s, following `docs/achievements-setup.md`.
2. Done: `games-ids.xml` is exported into this change, with five resources renamed to their keys.
3. Ship with that file in `app/src/main/res/values/`. Achievements work locally whether or not the player signs in, existing players get progress achievements at first start, and on sign-in the syncer pushes all local achievements.
4. After the smoke test (task 11.3), the owner publishes the Play Games configuration. That makes the 25 achievements permanent.

Rollback: removing the PGS dependency leaves the local achievements working.

## Open Questions

- The display name for each achievement in other languages. English only for now. The strings are resources.
- Whether the Daylight theme needs its own icon variant. Proposed: no, because icons are full-bleed `surface.deep`, like the board plate.
