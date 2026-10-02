# Design

## Context

`app-game-achievements` is merged into `development` but not archived. Its pipeline is: the engine records attempt facts in `GameState`, `AttemptSummary.from(state, record)` turns a finished attempt into plain values, `PlayerStats` holds lifetime counts, and the pure `AchievementRules.evaluate` decides unlocks. `AchievementTracker` runs that in one Room transaction, and `AchievementSyncer` pushes results to Play Games. The catalogue is an enum that may only grow (`docs/achievements.md`), pinned by `AchievementCatalogueTest`, and every skill or hidden entry needs a scripted proof in `AchievementReachabilityTest`.

What the current facts can't answer:
- **Route shape** (crosses, straights or corners in a row). `ScoreSheet.passes` is a map of exit counts per tile, with no order.
- **Taps during a replacement.** `GameScreen` sends taps to `GameSession.place`, which the engine refuses while a replacement runs. A refused command must return the same instance (`new !== old` means accepted), so a refused `Place` can't count anything.
- **Tips reset.** This happens on the settings screen through `SettingsRepository.resetTips()` and never reaches the tracker.

Bundled content (25 levels, grids up to 16 × 12, cross weight 6 from level 18, reservoir weight 0 everywhere) limits what can be published now. See proposal.md for the list and what is deferred.

## Goals / Non-Goals

**Goals:**
- Add the 11 achievements without changing any launch entry, and keep the rules pure and in `core`.
- Record just enough new engine facts that every new rule is a function of `AttemptSummary` and `PlayerStats`.

**Non-Goals:**
- Changing level content so that Snail or the higher Star Collector tiers become reachable. Each of those comes with the level pack that makes it reachable.
- Any new UI. The achievements screen, unlock rows and sync handle new entries without changes.

## Decisions

### 1. Record the flow path as an ordered list in `GameState`
`GameState.flowPath: List<PathStep>` with `PathStep(at: Position, section: PipeSection)`, added wherever the engine awards points for an exit (the same place `ScoreSheet.award` is called). `AttemptSummary` turns it into numbers so the rules never see the list: `longestCrossChain`, `longestStraightRun` (consecutive `H` or consecutive `V`; a straight that follows another of the same direction is in line with it, because the liquid leaves a straight through the side opposite its entry), `longestCornerRun` and `reservoirsLeft` (not used yet; it costs nothing and is ready for Snail). `sectionsLeft` already exists and is used for Dramatic Failure.

*Alternative:* rebuild the order from the board with `RouteTracer` at the end of the attempt. Rejected: on a loss the traced route and the liquid can differ after a later placement, and a cross's second pass needs the flow history anyway.

### 2. A separate `GameCommand.ReplacementTap(at)` for impatient taps
`GameSession.place` sends `ReplacementTap(at)` instead of `Place(at)` while `isReplacing`, and still returns the silent rejection it returns today, so the sound and haptics don't change. The engine adds one to `Replacement.taps` when `at == replacement.at`, otherwise it returns the same instance. `GameState.mostReplacementTaps` keeps the highest count, because the replacement itself is dropped when it lands. Drags released during a replacement already go back to their slot and are not taps.

*Alternative:* count refused `Place` commands. Rejected: it breaks the engine's "refused returns the same instance" contract that `GameSession.send` relies on.

### 3. Shortest route from `LevelSolver`, computed in `AttemptSummary.from`
`minimumSections = (LevelSolver.solve(level) as? Solvable)?.path?.distinct()?.size`. `distinct()` because a mandatory cross appears twice in the path. Efficiency Master: `won && minimumSections != null && piecesOnBoard <= minimumSections`. For levels without mandatory sections the solver's BFS is exact, and no route can use fewer distinct cells, since every route holds a simple start-to-finish path. With mandatory sections the solver is greedy and may be longer than optimal; `<=` means a better route still counts. The solve runs once per finished attempt, which is milliseconds on bundled levels.

*Alternative:* store the minimum in the level file. Rejected: it duplicates what the solver already knows and would drift when a level is edited in the designer.

### 4. Tips resets as a lifetime stat
`PlayerStats.tipsResets` with `afterTipsReset()`, a new `tips_resets` column on `player_stats` (Room version + 1, `ALTER TABLE … ADD COLUMN … NOT NULL DEFAULT 0`), and `AchievementTracker.onTipsReset()` / `tipsReset(): Job` in the application scope. `SettingsViewModel` calls it after `settingsRepository.resetTips()`. Rule: `TIPS_ADDICT -> statsAfter.tipsResets >= 1`. It runs in the same evaluate path as Wet Feet, so a reconcile at start-up also unlocks it. The unlock is synced like any other; the settings screen shows no unlock row (it has none), and the achievements screen and count update from the store.

*Alternative:* unlock it directly in the tracker. Rejected: that skips the pure rules and their tests, and `reconcile` couldn't recover a lost unlock.

### 5. Thresholds as constants beside the existing ones
`LONG_WAY_ROUND_2_TILES = 50`, `LONG_WAY_ROUND_3_TILES = 100`, `BIG_SPENDER_2_TOTAL = 1000`, `CROSSROAD_MASTER_CHAIN = 3`, `HIGHWAY_RUN = 7`, `SNAKE_RUN = 12`, `ERASER_REPLACEMENTS = 20`, `DRAMATIC_FAILURE_SECTIONS = 50` (strictly more), `ON_YOUR_NERVES_TAPS = 5`. Crossroad Master and Eraser also count on a loss, like Double Cross. Eraser is a skill achievement in the catalogue but is decided on any ended attempt. Abandoned attempts have no summary, so they never count.

### 6. Catalogue order and the XP guideline
The new entries go at the end of the enum in the table order of the spec, and are added to the `published` list in `AchievementCatalogueTest` (its total becomes 710). `docs/achievements.md` gets the rebalanced guideline for the 290 XP left: packs 2-13 at 5 + 5 (120), levels finished 50 / 100 / 250 at 15 / 20 / 25 (60), Perfectionist II 15, Star Collector III-VI at 100 / 250 / 500 / 1000 stars for 10 / 15 / 20 / 25 (70), Snail 10. That is 275, leaving 15 spare. Star Collector III-VI replace the earlier III / IV (150 / 400) plan.

## Risks / Trade-offs

- [The Long Way Round III and Big Spender II may be hard or impossible to script on the bundled 16 × 12 levels with a random stack] → the reachability test drives a seeded engine and replaces sections that don't fit; if no bundled level can reach a tier, that tier is dropped from this change before the Play Console step (unpublished entries can still be removed), and noted in the guideline.
- [Highway needs a free row or column of 7 cells in line, plus room to turn] → the larger pack 1 grids (11+ columns) have that; it is proven by the reachability test.
- [Efficiency Master is easy on small levels] → accepted; it is 15 XP and teaches efficient routes. The `<=` against a greedy solver can only make it easier, never unreachable.
- [`GameSession` sending `ReplacementTap` changes the tap path during a replacement] → `GameSession` tests check that the result is still the silent rejection and that no placement sound plays.
- [Play Games locks the 11 entries once published] → the console step comes last, after the catalogue test pins them and the smoke test passes.

## Migration Plan

1. Archive `app-game-achievements` (and `app-game-first-time-user-experience`, which owns "Show tips again") before this change is archived.
2. Ship the Room migration with the code; existing achievement data is kept (see the `achievement-persistence` delta).
3. Once the catalogue is pinned and the reachability tests pass, the owner creates the 11 achievements in the Play Console and exports `games-ids.xml`, which replaces `app/src/main/res/values/games-ids.xml`. `AchievementResourcesInstrumentedTest` must pass with the new ids. After the smoke test the owner publishes the Play Games configuration, which is the last step and can't be undone.
4. Players who already meet a progress or lifetime condition get it at the next start through `reconcile`. Skill achievements need a new attempt.
