# Proposal

Tracks GitHub issue [#64 Missing achievements](https://github.com/hexmasternl/leak-out/issues/64).

## Why

The 25 launch achievements (`app-game-achievements`) mostly reward clean, careful play. Issue #64 asks for more variety: higher tiers of achievements players already chase (The Long Way Round, Big Spender, Star Collector), achievements for route shapes (crosses in a row, long straights, long chains of corners), for efficient routes, and a few playful ones for failing in style or for impatience. They give players reasons to replay levels they have already finished.

## What Changes

- **14 new achievements** this change publishes, all reachable with the bundled levels, for 150 XP (catalogue total 595 → 745):

  | Key | Name | Group | Condition | XP |
  |---|---|---|---|---|
  | `tips_addict` | Tips Addict | lifetime | the seen tips are reset on the settings screen | 5 |
  | `long_way_round_2` | The Long Way Round II | skill | win after the liquid left 50 or more different placed tiles | 10 |
  | `long_way_round_3` | The Long Way Round III | skill | the same with 100 or more tiles | 15 |
  | `big_spender_2` | Big Spender II | skill | win with a final total of 1000 or more | 15 |
  | `crossroad_master` | Crossroad Master | skill | win with the liquid running through 3 cross sections in a row, each one connected directly to the next | 15 |
  | `efficiency_master` | Efficiency Master | skill | win with no more sections on the board than the shortest possible route needs | 15 |
  | `highway` | Highway | skill | win with a route that runs straight through 7 or more straight sections in a row | 10 |
  | `snake` | Snake | skill | win with a route that has 12 or more corner sections in a row, no straight, cross or reservoir in between | 15 |
  | `eraser` | Eraser | skill | win after replacing 20 or more sections in that attempt | 5 |
  | `dramatic_failure` | Dramatic Failure | hidden | lose after the liquid left more than 50 sections from the start | 5 |
  | `on_your_nerves` | On Your Nerves | hidden | tap the tile of a running replacement 5 or more times before its new section lands | 5 |
| `star_collector_3` | Star Collector III | progress | earn 100 stars (incremental, 100 steps) | 10 |
| `star_collector_4` | Star Collector IV | progress | earn 250 stars (incremental, 250 steps) | 15 |
| `snail` | Snail | skill | win with the liquid running through 3 or more reservoirs | 10 |

- **Existing achievements keep their values.** Play Games can't change a published achievement, so the issue's "Big Spender for 1000 instead of 500" becomes a second tier: Big Spender (500) stays, Big Spender II (1000) is added. The Long Way Round (30 tiles) likewise gets tiers II and III.
- **What the bundled levels can reach decides the rest.** The growth rules only allow achievements that the bundled levels can reach. When this change was proposed the library had 25 levels without reservoirs; it now has 96, with reservoirs from level 25 on and at most 288 stars. So:
  - *Snail* (3 reservoirs in one level) and *Star Collector* 100 / 250 are published now.
  - *Star Collector* 500 / 1000 stay in the growth guideline as Star Collector V / VI, to be published once enough levels ship (167 / 334 levels). They replace the planned Star Collector III / IV (150 / 400).
- **XP guideline rebalanced** in `docs/achievements.md`: with 255 XP left, pack achievements drop to 5 + 5 XP per pack, so packs 2-13, the levels-finished and Perfectionist tiers and Star Collector V / VI still fit in the remaining budget.
- **Engine attempt facts**: the game state also records, in flow order, every section the liquid left (so the route shape can be judged) and the most taps on the tile of one running replacement. Taps on the field during a replacement are still refused and still silent; the engine only counts them.
- **The shortest route** of a level comes from the existing level solver and is part of the attempt summary, so Efficiency Master needs no extra level data.
- **Tips resets** are counted in the lifetime player stats, so tapping "Show tips again" unlocks Tips Addict through the normal achievement pipeline (store, unlock feedback, Play Games sync).
- **14 new icons** in the Deep Flow style, generated with `assets/achievements/generate-icons.mjs`, and the matching name and description strings.
- **Play Console** (manual, by the owner): create the 14 achievements, export `games-ids.xml` again, smoke test, publish. Until the export lands the 14 have an empty Play Games id, and the sync skips an achievement without an id, so nothing invalid reaches Play Games.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

These capabilities are introduced by `app-game-achievements`, which must be archived before this change is.

- `game-achievements`: the catalogue grows from 25 to 39 achievements (745 XP), with the rules for the 14 new ones and their reachability proofs.
- `game-engine`: the attempt facts also hold the liquid's path in flow order and the most taps on a running replacement's tile.
- `achievement-persistence`: the player stats also count tips resets, and the database upgrade keeps existing achievement data.

## Impact

- `core/`: `Achievement` gets 14 entries at the end of the enum; `AchievementRules` the new conditions and thresholds; `AttemptSummary` the route-shape facts (longest cross chain, straight run and corner run, sections left), most replacement taps and the shortest route length; `PlayerStats` a tips-reset count. `GameState` and `GameEngine` record the flow path and replacement taps, with a new command for a tap during a replacement. Catalogue, rules, summary, engine and reachability tests grow.
- `app/`: `GameSession` sends the replacement tap command; the Play Games sync skips an achievement whose id is empty; `AchievementTracker` gets an entry point for a tips reset, called from `SettingsViewModel`; `PlayerStatsEntity` gets a column with a Room migration; `AchievementResources`, strings and 14 PNG icons in `drawable-nodpi`; `res/values/games-ids.xml` after the Play Console export.
- Docs: `docs/achievements.md` (new XP guideline, deferred Star Collector V / VI), `docs/achievements-technical.md` and `docs/project-plan.md` section 2.11.
- External: 14 new achievements in Play Games project `201820010117`. Publishing them makes them permanent.
