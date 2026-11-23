## Context

See proposal.md for the motivation. The server's `GET /api/daily-pipes/{date}/scores/top` returns up to 10 entries (`position`, `displayName`, `score`, `stars`, `isCurrentPlayer`) and `player` (`position`, `score`, `stars`) or `null`. The app's `DailyPipeJson.topScores` reads only `player.position`. `DailyPipeApi.submit` fetches the top scores after a submission, but nothing fetches them on their own. `GameScreen` already holds the clock back while the intro tips wait (`GameClock(session, paused || waitingForTips)`). The daily pipe's win dialog is `LevelCompleteDialog` with a `DailyResultPanel` under the score row.

## Goals / Non-Goals

**Goals:**
- One leaderboard composable, used at the start and after a win.
- The row selection is a pure function, unit tested without Compose.
- Bundled levels behave exactly as they do today.

**Non-Goals:**
- No server change, and no change to the number of entries the server returns.
- The daily pipe's level lost dialog stays as it is.
- No live refresh of the leaderboard while it is open.

## Decisions

- **Row selection as a pure function.** `dailyLeaderboardRows(top, rows = 5)` returns the first 5 entries by position with `isCurrentPlayer` kept. When `player` has a position greater than 5, it appends an own row built from `player`. It lives next to `DailyTopScores` in `scores/online`. Alternative: let the server return only 5 entries. Rejected because the server contract already covers this case.
- **`DailyTopScores.playerPosition` becomes `player: DailyPlayerRank?`** (position, score, stars). Callers that only need the position read `player?.position`.
- **`DailyPipes.top(date)`** returns either loaded or failed, built on the existing top-scores call with its 401 retry. The start leaderboard and the post-win leaderboard both use it. The post-win list is fetched when Next is tapped, after the submission has settled, so it includes the new result. A submission no longer fetches the top scores itself (`DailySubmission.Accepted` drops its `top`), because nothing shows them in the win dialog any more.
- **`DailyResults`** becomes an interface with `submit`, `top` and `playerName`, made by `DailyResults.of(dailyPipes, date) { playerName }`. `MainActivity` passes the Play Games display name.
- **Start gate in `GameScreen`.** A daily game starts with a `waitingForStart` flag that is folded into the clock's paused condition, like `waitingForTips`. The intro tips are offered only after Start. This holds the flow and the countdown without a new engine state. Alternative: start in the engine's paused state and show the pause dialog. Rejected because the pause dialog's buttons do not match this flow and it would count as a pause by the player.
- **The daily variant of the win dialog.** With `onDailyNext`, `LevelCompleteDialog`'s buttons are only Next. Next replaces the win dialog with `DailyLeaderboardDialog`, a result dialog of its own built on `ResultDialogWindow`, so it reuses the scrim, the panel and the Regular / Compact measuring. The same dialog serves the start with a Start button. `DAILY_TOP_ROWS` and the top-3 list in `DailyResultPanel` are removed.
- **Restart** uses the existing restart path, which deals the same seeded stack (requirement "Every player gets the same stack on the daily pipe"). The leaderboard state (`DailyLeaderboardHolder`) is remembered per attempt, so a new attempt opens on the starting leaderboard again.
- **Going to the background before Start** does not pause the session, since nothing runs yet; otherwise the pause dialog would show on top of the starting leaderboard on return.

## Risks / Trade-offs

- [The own row needs the player's display name, and `player` does not carry one] → Use the Play Games display name the app already has from sign-in, and fall back to "You" when there is none.
- [Two leaderboard downloads per attempt add server load] → Each is one small GET that the server already serves after every submission. Acceptable.
- [The start gate meets the interstitial ad planned in #176] → If that ad is added, it plays before the game screen opens, so it comes before the start leaderboard.

## Open Questions

- The label of the start button: "Start" or "Play". It can be settled during implementation without changing the behaviour.
