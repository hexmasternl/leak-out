# Achievements

How the achievement catalogue may grow, and what to do when a level pack adds achievements. How achievements work in the game (catalogue, rules, storage, sync, UI) is in [achievements-technical.md](achievements-technical.md), with a summary in [project-plan.md, section 2.11](project-plan.md#211-achievements). The Play Console side (creating achievements, exporting the ids, publishing) is in [achievements-setup.md](achievements-setup.md); this page links to it rather than repeating it.

## Why the catalogue only grows

Google Play Games makes a published achievement permanent: it can't be deleted, and its steps can't change. Players also keep what they earned. So the catalogue is treated like a public API:

- `core/achievements/Achievement` is the catalogue. Its `key` is stored in the local database and is the `achievement_<key>` resource that holds the Play Games id.
- `AchievementCatalogueTest` pins every published entry (key, steps, hidden flag, XP) in its `published` list. That list may only grow.

## Growth rules

1. **Add only.** Never remove an achievement, never reorder the published list in a way that drops one, and never change a published key, steps, hidden flag or XP. A new tier is a new achievement with a higher count; existing tiers stay as they are.
2. **Only publish what can be reached with the shipped levels.**
   - Levels finished: at most the number of bundled levels.
   - Stars: at most 3 × the number of bundled levels.
   - Levels with 3 stars: at most the number of bundled levels, and every one of them must actually reach 3 stars.
   - Pack achievements (`pack_<n>_complete`, `pack_<n>_perfect`): only for library page *n* once it is shipped and full (`LevelLibraryLayout.LEVELS_PER_PAGE`, 24 levels).
   - No achievement may depend on "all levels" or on the size of the library. Progress rules count absolute numbers or one library page (`LevelLibraryLayout.pageOf`).

   `AchievementContentTest` checks the counts against the bundled library, so a tier can't be published before its levels exist. Skill and hidden achievements need a scripted proof in `AchievementReachabilityTest`.
3. **Mind the XP budget.** Play Games allows at most 1,000 XP per game. The launch catalogue uses 595, which leaves 405. Every XP value is a multiple of 5. The guideline for spending what is left:

   | What | When | XP |
   |---|---|---|
   | `pack_<n>_complete` + `pack_<n>_perfect` | with each new pack, packs 2-13 | 10 + 10 per pack, 240 for 12 packs |
   | Levels finished 50 / 100 (*Master Plumber*) / 250 | from 50 / 100 / 250 levels shipped | 20 / 30 / 40 |
   | Star Collector III / IV: 150 / 400 stars | from 50 / 134 levels shipped | 20 / 30 |
   | Perfectionist II: 50 levels with 3 stars | from 50 levels shipped | 25 |
   | **Total** | | **405** |

   Beyond pack 13 (more than 312 levels) pack achievements only fit if XP is freed in this guideline before it is spent, for example 5 + 5 XP per pack from pack 8 on. Recheck the budget in every change that touches the catalogue; `AchievementCatalogueTest` fails above 1,000.
4. **Each addition is its own OpenSpec change**, usually together with the level pack that makes it reachable.

Players who already meet a new achievement's condition get it on their next app start: the tracker reconciles the stored history at start-up, the same backfill that gave existing players their progress achievements at launch.

## Checklist: adding a level pack

For pack *n* (library page *n*, 24 new levels). Do the levels first; the achievements are only allowed once the page is full.

**Levels**

- [ ] The 24 levels are bundled and fill page *n* of the library.
- [ ] Every one of them can reach 3 stars (otherwise `pack_<n>_perfect` and the 3-star milestones are unreachable).

**Catalogue (`core`)**

- [ ] Add `PACK_<N>_COMPLETE` and `PACK_<N>_PERFECT` to the `Achievement` enum, at the end, with `pack = n`, incremental 24 steps and the XP from the guideline. Add any milestone tier the new level count allows.
- [ ] Add the new entries to the `published` list in `AchievementCatalogueTest`, and update the totals it checks.
- [ ] `AchievementContentTest` passes: the milestones fit the bundled levels and the pack achievements match full pages.
- [ ] For a new skill or hidden achievement: a scripted proof in `AchievementReachabilityTest`.

**App resources**

- [ ] Name and description strings `achievement_name_<key>` and `achievement_desc_<key>` in `app/src/main/res/values/strings.xml`, exactly as they will be in the Play Console, and the same texts in the console list of `AchievementResourcesTest`.
- [ ] The icon: add it to [`assets/achievements/generate-icons.mjs`](../assets/achievements/generate-icons.mjs) using the pack or tier template, run `node generate-icons.mjs .` and `node render-icons.js <key>` in that folder to get the 512 × 512 PNG in `png/`, and copy it to `app/src/main/res/drawable-nodpi/achievement_<key>.png`. Keep everything inside the radius-200 safe disc.
- [ ] Add the entry to the exhaustive `when` in `AchievementResources` (the build fails until you do).

**Play Console** (follow [achievements-setup.md](achievements-setup.md#adding-achievements-later))

- [ ] Create each achievement with the same name, description, XP, steps, hidden flag and icon.
- [ ] Export `games-ids.xml` again, rename any resource whose name doesn't match its key, and replace `app/src/main/res/values/games-ids.xml`. Existing ids stay the same.
- [ ] The instrumented `AchievementResourcesInstrumentedTest` passes: every key has a Play Games id and no two share one.
- [ ] Smoke test with a tester account, then publish the Play Games configuration. Publishing makes the new achievements permanent, so it comes last.

**Verify**

- [ ] `./gradlew :core:test` and `./gradlew :app:testDebugUnitTest` pass.
- [ ] The achievements screen lists the new entries in catalogue order and the settings count shows the new total.
