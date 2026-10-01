## Why

Leak Out ships with 24 levels and will grow in packs of 24 (one library page each) towards 300 or more. It has scores and stars, but once a player has finished a level there is little reason to come back to it. Achievements reward playing well (no wasted pipes, crosses passed twice, building the route before the liquid moves) and playing long (lifetime pipe counts, bouncing back after losses). Google Play Games makes them visible outside the app and keeps them across devices. Level history and the settings screen are now in place, so this is a good time to build on them.

## What Changes

- **25 launch achievements** in four groups. Each has a fixed id, name, description, type (standard or incremental), hidden flag and XP value. Together they use 595 of the 1,000 XP Play Games allows, so about 400 XP stays free for achievements added with later level packs:
  - *Progress* (from level history): First Leak Fixed, Getting the Hang of It, Star Collector I/II, Perfectionist I, Pack 1 Complete, Pack 1 Perfect. These are milestones (counts that stay meaningful as levels are added) and per-pack achievements, never "all levels".
  - *Skill in one run* (from the attempt): Zero Waste, Double Cross, Crossroads King, Ahead of the Flow, Full Throttle, Measure Twice, Steady Hands, The Long Way Round, Big Spender.
  - *Lifetime* (from new player stats): Apprentice, Journeyman, Guildmaster, Never Give Up, Personal Best, Wet Feet.
  - *Hidden*: Instant Flood, Wall to Wall, Photo Finish.
- **Achievement rules in `core`**: a pure `AchievementRules.evaluate` that takes an attempt summary, the lifetime stats and the level history, and returns what is newly unlocked and the progress of each incremental achievement.
- **Engine attempt facts**: the game state also records how many sections were placed and replaced, whether the game was ever paused, whether the route was completed before the liquid started, and whether the route was completed while the liquid was in the next tile (photo finish).
- **Local achievement store** (Room): unlocks with their time, progress steps, lifetime player stats and loss streaks per level. This store is the source of truth, so achievements also work without Play Games.
- **Achievements for existing players**: progress achievements are worked out from the level history, so players who already finished levels get them on the first launch after the update.
- **The catalogue grows with the content**: achievements are only ever added, never changed or removed, because Play Games can't delete or re-step a published achievement. A milestone tier is only published once the shipped levels can reach it. Each new pack adds its own Pack N achievements and can bring higher tiers (for example Master Plumber at 100 levels finished), paid for from the XP held back.
- **Achievements are permanent**: "Reset progress" on the settings screen deletes level history only. Unlocked achievements, their progress and the player stats stay, because Play Games cannot take an achievement back either.
- **Google Play Games Services v2**: silent sign-in at app start, a **Sign in** button on the settings screen when silent sign-in fails, and a sync that pushes every local unlock (`unlock`) and progress (`setSteps`) to Play Games. Both calls are idempotent, so a sync can safely repeat. Debug builds without a configured Play Games project fall back to a no-op sync.
- **Achievements screen**: opened from a new **Achievements** button on the settings screen. It lists all 25 achievements from local data: unlocked, locked with progress, or hidden. When signed in, it also has an "Open in Play Games" button that shows the Play Games achievements UI.
- **Unlock feedback**: the level complete and level lost dialogs show the achievements the attempt unlocked.
- **25 proposed icons** (plus a placeholder for hidden achievements), 512×512 PNG with SVG sources, in the Deep Flow design system style. They are for the Play Console and the achievements screen and are kept in this change for review (`icons/`).
- **Play Console setup** (manual, done by the owner): create the app and a Play Games Services project, register the signing SHA-1s, define the 25 achievements and add testers. The change provides a setup checklist and the console data (names, descriptions, XP, icons).

## Capabilities

### New Capabilities
- `game-achievements`: the achievement catalogue (ids, names, descriptions, groups, types, steps, hidden flags, XP), the rule that decides when each one unlocks, and how the catalogue grows as level packs are added.
- `achievement-persistence`: the local store of unlocks, progress, player stats and loss streaks; awarding achievements to existing players; keeping achievements when progress is reset.
- `play-games-integration`: the Play Games SDK setup, silent and manual sign-in, syncing unlocks and progress, and the fallback when Play Games is not configured or the player is not signed in.
- `achievements-screen`: the in-app list of achievements reached from the settings screen, and the link to the Play Games achievements UI.

### Modified Capabilities
- `game-engine`: the game state also holds the attempt facts (sections placed, replacements, ever paused, route completed before the flow, photo finish), and restart resets them.
- `game-play-level-complete`: the level complete dialog lists the achievements the win unlocked.
- `game-play-level-lost`: the level lost dialog lists the achievements the loss unlocked.
- `settings-screen`: a Play Games panel with the sign-in state, a Sign in button and an Achievements button; the reset confirmation says achievements are kept; the About privacy line mentions Play Games. This capability is introduced by `app-settings-page`, which must be archived before this change is.

## Impact

- `core/`: a new `achievements/` package (catalogue, `AttemptSummary`, `PlayerStats`, `AchievementRules`). `engine/GameState` and `GameEngine` gain the attempt facts.
- `app/`: a new `achievements/` package (Room entities, DAO, repository, `AchievementSync` with Play Games and no-op implementations). The Room database version goes up with a migration. There is a new `ui/achievements/` screen, a Play Games panel on the settings screen, an unlock row in the level complete and level lost dialogs, `GameSession` hooks for when an attempt ends, an `Application` class that starts the Play Games SDK, manifest meta-data and `res/values/games-ids.xml`.
- Dependencies: adds `com.google.android.gms:play-services-games-v2`, pinned in `gradle/libs.versions.toml`.
- Assets: 25 achievement icons in `app/src/main/res/drawable-nodpi/` (or vector equivalents), taken from the approved icons.
- External: a Google Play Console app and Play Games Services project for `nl.hexmaster.leakout`. Until it exists, everything except the sync can be built and tested.
- Docs: `docs/project-plan.md` gets an achievements section, and the settings documentation covers the new panel.
