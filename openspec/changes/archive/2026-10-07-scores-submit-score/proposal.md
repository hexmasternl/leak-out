# Proposal

## Why

Tracked in [#2](https://github.com/hexmasternl/leak-out-server/issues/2). Depends on #1 (Google Play Games authentication, change `players-google-play-auth`).

The server exists to keep the best score of every player on every level, but it has no way to receive a score yet. The game needs one authenticated endpoint that stores a player's score for a level and only ever keeps the highest one, so the leaderboards (#3, #4) have data to rank.

## What Changes

- New authenticated endpoint `POST /api/scores` that accepts a level number and a score for the signed-in player.
- The server keeps exactly one best score per player per level: a first score is stored, a higher score replaces it, an equal or lower score is ignored.
- The response always returns the player's stored best score for that level and whether this submission became the new best.
- Invalid input (level below 1, negative score) is rejected with `400 Bad Request` and validation details.
- First real content in the `Scores` module: the `SubmitScore` feature slice, the `LevelScore` domain entity, the `Scores.Data.PostgreSQL` persistence adapter and the `AddScoresModule()` registration.
- The Aspire AppHost gets a `scoresdb` database on the PostgreSQL server that #1 introduces, referenced into the API.

## Capabilities

### New Capabilities
- `score-submission`: Submitting a level score for the authenticated player and keeping only the best score per player per level.

### Modified Capabilities

## Impact

- **API**: new route `POST /api/scores` in `src/HexMaster.LeakOut.Api/Endpoints/ScoreEndpoints.cs`, requiring authorization (bearer token from #1).
- **Code**: `src/Scores/HexMaster.LeakOut.Scores` (feature slice, domain model, repository port, registration), `HexMaster.LeakOut.Scores.Abstractions` (request and result DTOs), `HexMaster.LeakOut.Scores.Data.PostgreSQL` (DbContext, repository, migration), `HexMaster.LeakOut.Scores.Tests`.
- **Data**: new PostgreSQL database `scoresdb` with table `level_scores`.
- **Orchestration**: `AppHost.cs` adds `scoresdb` and references it into `hexmaster-leakout-api`.
- **Dependencies**: Npgsql EF Core provider through the Aspire client integration (already introduced for the Players module by #1).
