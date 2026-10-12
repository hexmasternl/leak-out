# Proposal

## Why

The website's leaderboard shows who holds the best scores of a level, but not when those scores were set, so a visitor cannot tell a fresh record from one that has stood for a year. The score server already records the moment it received each best score. It only has to share that moment with the website, and the website has to show it in a form people read at a glance ("2 days ago").

Tracked in [#144](https://github.com/hexmasternl/leak-out/issues/144).

## What Changes

- The public top scores endpoint (`GET /api/public/levels/{level}/scores/top`) adds to each entry the moment the server received that best score (`achievedAt`, an ISO 8601 date and time with offset, in UTC).
- The endpoints the app uses (`GET /api/levels/{level}/scores/top`, the window around the player, score submission) do not get this moment.
- The website's leaderboard gets a column that shows each score's moment as relative time in the page's language ("1 hour ago", "2 days ago", "1 year ago"), with the full date and time available on hover.
- No database migration: `level_scores.achieved_at` already exists, is `NOT NULL` and is filled from the server's clock when a new best score is received.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `server-public-top-scores`: each public entry carries the moment its score was received.
- `website-leaderboard`: each entry shows that moment as relative time.
- `server-level-leaderboard`: states that the app's leaderboard entries carry no timestamps.

## Impact

- Server: `PublicLeaderboardEntryDto`, `RankedLevelScore` and `LevelScoreRepository.GetRankedAsync`, `GetPublicLevelTopScoresQueryHandler`, their tests, `server/README.md` (public endpoints).
- Website: `website/src/assets/js/leaderboard.js`, `layouts/leaderboard.html`, the `i18n/*.yaml` files, `assets/css/main.css`, `website/tests/leaderboard.test.js`, `website/README.md`.
- The public response grows by one field. The current website ignores it, so the server can ship first.
