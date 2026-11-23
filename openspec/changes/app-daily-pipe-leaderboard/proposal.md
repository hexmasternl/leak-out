## Why

The daily pipe is a race for the day's best score, but players only see a short top 3 in the win dialog, and only if their own score made it into those 3. They cannot see the score to beat before they start, and a player in 76th place never sees their own position next to the leaders. The server already returns the top 10 and the player's own position and best score. The app drops the player's score and shows the list only once.

Tracked in [#187](https://github.com/hexmasternl/leak-out/issues/187).

## What Changes

- A **daily leaderboard** table with the columns Position, Player and Points: today's top 5 and, when the player has a result for today outside the top 5, a sixth row with their position, name marked "(you)" and best score. A player inside the top 5 is marked and highlighted in place.
- **At the start** the daily pipe opens paused, with the leaderboard over the board and a Start button. Start closes it and the flow begins. Back returns to the welcome screen. A failed download shows a short message and never blocks play.
- **After a win** the daily pipe's level complete dialog has a single **Next** button. It keeps the score and the daily result (streak, rank, ended, failed with Try again) but drops the top-3 list.
- **Next** shows the leaderboard again, downloaded after the result was sent, with the buttons **Restart** (today's pipe again, same stack, opening paused on the leaderboard) and **Exit** (welcome screen).
- The app reads the player's best score and stars from the top-scores response.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `daily-pipe`: the level complete requirement changes (single Next button, no top-3 list), and new requirements add the leaderboard table, the paused start with the leaderboard, and the leaderboard after a win.

## Impact

- **App code** (`app/`): `DailyPipeJson.topScores` and `DailyTopScores` gain the player's score and stars; `DailyPipes` gets a call for today's top scores; a new daily leaderboard composable and the row selection logic; `GameScreen` gates the clock behind the start leaderboard for a daily pipe; `LevelCompleteDialog` gets the daily variant with only Next, followed by the leaderboard with Restart and Exit.
- **Strings**: Position, Player, Points, "(you)", Start, Next, the empty and failed messages, in every supported language.
- **Design system**: an entry for the leaderboard table.
- **Server**: none. `GET /api/daily-pipes/{date}/scores/top` already returns everything needed.
- **Bundled levels**: unchanged.
