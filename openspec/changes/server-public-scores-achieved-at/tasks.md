# Tasks

## 1. Server

- [x] 1.1 Add `AchievedAt` (`DateTimeOffset`) to `RankedLevelScore` and select `achieved_at` in `LevelScoreRepository.GetRankedAsync`; extend `LevelScoreRepositoryTests` to assert the returned moment, including that a later lower score leaves it unchanged. Verify with `dotnet test` on the Scores tests.
- [x] 1.2 Add `AchievedAt` to `PublicLeaderboardEntryDto` and fill it in `GetPublicLevelTopScoresQueryHandler`; extend `GetPublicLevelTopScoresQueryHandlerTests` to assert the moment is passed through, and verify they pass.
- [x] 1.3 In `PublicLevelScoreEndpointsTests`, assert each JSON entry has `achievedAt` in ISO 8601 with offset; in `LevelScoreEndpointsTests`, assert the app's top scores and window responses contain no `achievedAt`. Verify with `dotnet test server/src/leak-out-server.slnx`.
- [x] 1.4 Document the `achievedAt` field under the public endpoints in `server/README.md` and check the example response matches the endpoint test.

## 2. Website

- [x] 2.1 In `website/src/assets/js/leaderboard.js`, keep `achievedAt` in `parseTopScores` (dropping invalid dates), add an exported `relativeTime(achievedAt, now, locale)` and a full-date formatter, add the relative and full date to `toRows`, and render a `<time>` cell in `createDomView`. Cover each unit, "now", future moments, a missing moment and Dutch output in `website/tests/leaderboard.test.js`; verify with `cd website && npm test`.
- [x] 2.2 Add the column header to `layouts/leaderboard.html` and a `when` text to every `i18n/*.yaml` (en, nl, de, fr, es); style the column in `assets/css/main.css` so the page has no horizontal scroll at 360 px. Verify with `hugo --source website/src --panicOnWarning`.
- [x] 2.3 Describe the new column in the leaderboard section of `website/README.md` and check the text matches the behaviour.
