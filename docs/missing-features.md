# Leak Out – Missing Features Before Launch

A gap analysis of the game design (`docs/project-plan.md`, `docs/level-format.md`) and the open OpenSpec changes, listing what still has to be decided or built before the first version can be released.

Written 2026-09-29. It is based on the project plan, the level format, every open change's proposal, the archived `welcome-screen` spec and the current code, plus searches of the designs and tasks. Not every spec and tasks file was read in full, so check each point against the change it names before acting on it.

---

## 1. Must fix before launch

### 1.1 The Settings button leads nowhere

The `welcome-screen` spec has a **Settings** button that raises a settings navigation event, but no change builds a settings screen. `app-welcome-exit-button` adds a "never show again" setting that "a future Settings screen can expose", and the plan (§1.11, §2.10) lists sound and music toggles.

**To do:** write an `app-settings` change (sound, music, re-enable the exit confirmation), or hide the Settings button for v1.

### 1.2 No change says who records a win

`app-game-play-level-complete` leaves three things to `app-game-level-history` and `app-game-level-library`:

- saving the result in the level history,
- unlocking the next level,
- the "new best" badge.

Those two changes were proposed before the win existed. Their tasks may not connect to the win at all.

**To do:** one change owns the whole chain: win → save to history → unlock the next level → show "new best" in the level complete dialog. Check the history and library tasks first; they may already cover part of it.

### 1.3 Rules conflict on a 0-star win

| Change | Rule |
|--------|------|
| `app-game-level-model` / `docs/level-format.md` | A level can be won with 0 stars. |
| `app-game-play-level-complete` | Shows 0 to 3 stars from the final total. |
| `app-game-level-history` | Rejects a record with 0 stars (only 1–3 allowed). |
| `app-welcome-play-button` | A level counts as completed when a record exists with at least 1 star. |
| `app-game-level-library` | Finishing level `n` unlocks level `n+1`. |

As specced, a 0-star win may save nothing and unlock nothing, and **Continue** could keep sending the player back to the same level.

**To do:** pick one rule, for example "a win always saves and unlocks; stars are 0–3", and update the history, library and play-button changes to match.

### 1.4 The two most important changes aren't finished

- `app-game-play-game-engine` has only a `proposal.md`: no design, specs or tasks.
- `app-game-play-level-complete` has a proposal and design, but no specs or tasks.
- Both folders are untracked in git.

The engine is where the liquid actually moves. Nothing is playable from start to finish until it is done.

**To do:** finish the design, specs and tasks for both, and commit them.

### 1.5 Levels may be impossible to win, and nothing checks

The plan (§2.9, Milestone 4) asks for a headless solver test that proves every shipped level can be won. No change includes it. The `app-game-level-model` design accepts that the unseeded stack can make some attempts unwinnable; replacing sections softens bad draws.

**To do:**

- Add an automated check that a route from start to finish exists around the blocked cells, for all 24 bundled levels.
- Playtest all 24 levels and tune the star thresholds (`docs/level-format.md` says to tune them by play-testing).

### 1.6 Nothing happens after the last level

There are 24 levels. After winning level 24, **Next level** is simply disabled. There is no moment that tells the player they finished the game.

**To do:** consider an "all levels complete" screen or message.

The page-unlock star threshold `{x}` from `app-game-level-library` is never used in v1, because all 24 levels fit on one page. That is fine, but worth knowing when choosing its value.

### 1.7 Exit and Next level skip the level library

- In the HUD, level-lost and level-complete changes, **Exit** goes back to the welcome screen. Once the level library exists, players will probably expect Exit to return to the library.
- **Next level** in `app-game-play-level-complete` loads level `id + 1` and ignores the unlock rules. The level-complete design says the library change will replace this lookup with its own unlock rule; make sure the library tasks actually do.

**To do:** decide where Exit goes, and make Next level follow the library's unlock rule.

---

## 2. Should have for a good first release

### 2.1 First-time guidance

Nothing explains the game to a new player: dragging from the bottom of the stack, replacing a section, the cross, the reservoir and fast forward.

**To do:** a short hint on level 1, and on the level where each new section (cross, reservoir) or blocked cells first appear.

### 2.2 Sound and haptics

Plan Milestone 5 lists sound effects (place, replace, flow, reservoir gurgle, leak, win), optional music and a haptic tick on placement. No change covers them. A game with no sound at all feels unfinished. This goes together with the sound and music toggles in 1.1.

### 2.3 Plan and specs disagree on starting the flow early

Plan §1.8 and §1.9 still describe a "start flow now" button with a time bonus. `app-game-play-points` and `app-game-play-fast-forward` both explicitly leave it out; fast forward covers the "route is complete, I don't want to wait" case.

**To do:** drop it from the plan, or add a change for it.

### 2.4 The plan is out of date

`docs/project-plan.md` still describes tap-to-place, the old stack order, the old scoring table, DataStore for progress (history uses Room) and the 7 × 9 default grid. Several changes promise to update their sections.

**To do:** once the open changes have merged, check the plan against the specs and fix what is left.

### 2.5 Accessibility

Plan Milestone 5 asks for a colour-blind-safe liquid colour, content descriptions on stack items and large tap targets. Only a few changes touch this (the splash's reduced motion, the HUD's content descriptions).

**To do:** do an accessibility pass: TalkBack, colour contrast on both themes, reduced motion in the game animations (points popups, leak spurt, star reveal, finish flash).

### 2.6 Languages

`app-game-play-hud` tests number formats in English and Dutch, but nothing says which languages v1 ships.

**To do:** decide English only or English and Dutch, and if Dutch, translate every string resource.

### 2.7 Leaving the app mid-level

Going to the background is covered: the game pauses and shows the pause dialog (or the lost/complete dialog). Nothing covers what happens when Android kills the app while it is in the background. Is the attempt lost (back to the welcome screen) or restored?

**To do:** one requirement in the engine or pause spec.

---

## 3. Release work

These are plan Milestones 0 and 6; no change covers them yet.

### 3.1 CI

There is no `.github/` workflow. Milestone 0 calls for a GitHub Actions workflow that runs the tests on every push.

### 3.2 Release build

- Release signing configuration.
- R8/ProGuard: enable minification and add keep rules for kotlinx.serialization and Room.
- A `versionCode` / `versionName` scheme (`versionCode` is currently fixed at `1`).
- Check the backup setting for the Room database and the settings (`android:allowBackup="true"` today); add backup rules if needed.

### 3.3 Google Play

- Store listing: app icon, feature graphic, screenshots, short and full description.
- Data safety form: declare the user ID (Play Games player, via the score server) and the game scores (app activity) that are sent over HTTPS when the player is signed in to Play Games, for app functionality. See [deployment.md](deployment.md#score-server).
- Content rating questionnaire and target audience.
- A privacy policy URL, covering the scores sent to the score server.

### 3.4 Testing

- Manual test matrix: small phone, large phone, tablet, Android 8 (min SDK 26) and the latest Android.
- Internal test track on Google Play before production.

---

## 4. Suggested order

1. Settle the 0-star rule (1.3) and who records a win (1.2). Both change existing specs.
2. Finish and commit `app-game-play-game-engine` and `app-game-play-level-complete` (1.4).
3. Fix Exit and Next level navigation (1.7) in the library change.
4. Add small changes for settings (1.1), the winnability check (1.5), sound (2.2) and the release setup (section 3).
5. Then the should-haves: first-time hints, accessibility, languages and the plan cleanup.
