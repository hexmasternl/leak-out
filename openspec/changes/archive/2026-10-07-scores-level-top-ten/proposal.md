# Proposal

## Why

Tracked in [#3](https://github.com/hexmasternl/leak-out-server/issues/3).

Players can submit their best score per level, but the game has no way to show how that score compares to others. A per-level top 10 gives players a goal to chase. Depends on #1 (players are authenticated and have a display name) and Depends on #2 (best scores per player per level are stored in `level_scores`).

## What Changes

- New authenticated endpoint `GET /api/levels/{level}/scores/top` that returns the 10 best scores of a level, highest first, with each entry's position, the player's display name, the score and whether the entry belongs to the calling player.
- A deterministic ranking rule for a level: score descending, then the earliest achieved score first, then user id. Positions are 1-based row numbers; two entries never share a position. This rule is reused by the "scores around the player" endpoint (#4).
- New query slice `GetLevelTopScores` in the Scores module.
- New cross-module contract `IPlayerProfileService` in `Players.Abstractions`, implemented by the Players module, so the Scores module can look up display names by internal `UserId` without storing them.

## Capabilities

### New Capabilities

- `level-leaderboard`: ranking of the best scores on a level and the leaderboard views built on that ranking, starting with the top 10.

### Modified Capabilities

(none)

## Impact

- `HexMaster.LeakOut.Scores`: new `Features/GetLevelTopScores` slice, repository read method, registration in `AddScoresModule`.
- `HexMaster.LeakOut.Scores.Abstractions`: new `LevelLeaderboardDto` and `LeaderboardEntryDto`.
- `HexMaster.LeakOut.Scores.Data.PostgreSQL`: top-N query on `level_scores` using the `(level, score desc, achieved_at)` index added in #2.
- `HexMaster.LeakOut.Players.Abstractions`: new `Services/IPlayerProfileService`.
- `HexMaster.LeakOut.Players` / `Players.Data.PostgreSQL`: implementation that reads display names from the `players` table.
- `HexMaster.LeakOut.Api`: new route in the scores endpoints file, requiring authorization.
- New project reference: `HexMaster.LeakOut.Scores` → `HexMaster.LeakOut.Players.Abstractions`.
