# Proposal

## Why

Tracked in [#4](https://github.com/hexmasternl/leak-out-server/issues/4).

The top 10 of a level (#3) only shows the best players. Most players never appear there, so the game cannot show them where they stand. This change adds a leaderboard window centred on the calling player: the five players above them, the player, and the four players below them, each with their position.

Depends on #1 (Google Play Games authentication: the caller is identified by the session token and resolved to an internal `UserId`).
Depends on #2 (score submission: the stored best score per player per level, with its `achieved_at` timestamp).
Depends on #3 (level top 10: the `level-leaderboard` capability, its ranking rule, its DTOs and the display-name lookup).

## What Changes

- New authenticated endpoint `GET /api/levels/{level}/scores/around-me` that returns up to 10 leaderboard entries around the calling player on that level.
- The window holds the 5 entries ranked directly above the player, the player, and the 4 entries ranked directly below. When fewer than 5 entries are above, the window shifts down; when fewer than 4 are below, it shifts up. It always holds 10 entries unless the level has fewer than 10 scores.
- Each entry carries its position, the player's display name, the score, and whether it is the calling player. Positions and ordering follow the ranking rule of the `level-leaderboard` capability (introduced by #3).
- A caller without a score on the level gets `200 OK` with an empty list. A level below 1 returns `400 Bad Request`.
- New query slice `GetLevelScoresAroundPlayer` in the Scores module; it reuses `LevelLeaderboardDto` and `LeaderboardEntryDto` from #3.

## Capabilities

### New Capabilities

_None._ The `level-leaderboard` capability is introduced by the change for #3 (`scores-level-top-ten`).

### Modified Capabilities

- `level-leaderboard`: adds the requirement for the leaderboard window around the calling player.

## Impact

- **Scores module** (`HexMaster.LeakOut.Scores`): new feature slice `Features/GetLevelScoresAroundPlayer` (query and handler), and two new read methods on the level score repository (position of a player, and a ranked window by offset).
- **Scores data** (`HexMaster.LeakOut.Scores.Data.PostgreSQL`): implementation of those read methods, using the `(level, score desc, achieved_at)` index introduced by #2. No schema change.
- **API** (`HexMaster.LeakOut.Api`): one more route in the scores leaderboard endpoint file, behind the same authorization as the other score endpoints.
- **Players module**: none; the display-name lookup through `IPlayerProfileService` (Players.Abstractions) from #3 is reused.
- **Tests**: handler tests in `HexMaster.LeakOut.Scores.Tests`, and an endpoint test.
