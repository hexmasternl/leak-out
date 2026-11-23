## MODIFIED Requirements

### Requirement: Daily pipe results are submitted to the server
When the player wins the daily pipe, the app SHALL submit the score and stars for the pipe's date to the server. The level-complete dialog SHALL show the score and the player's current streak and rank, and SHALL have a single Next button. When the server rejects the submission because the day has ended, the dialog SHALL say that the daily pipe has ended.

#### Scenario: Win
- **WHEN** the player wins the daily pipe with 2 stars and 380 points
- **THEN** the app submits 380 points and 2 stars for that date and the dialog shows the streak returned by the server

#### Scenario: Day ended during play
- **WHEN** the player wins after 00:00 UTC on the pipe of the previous day
- **THEN** the dialog shows that the daily pipe has ended and no streak change

#### Scenario: Only a Next button
- **WHEN** the level complete dialog of a daily pipe win is shown
- **THEN** it has exactly one button, Next, and no list of top scores

## ADDED Requirements

### Requirement: Daily leaderboard shows the top 5
The daily leaderboard SHALL be a table with the columns Position, Player and Points, listing today's best results from position 1 up to position 5, in order. Scores SHALL be formatted in the player's locale.

#### Scenario: Full top 5
- **WHEN** the server returns 10 results for today
- **THEN** the leaderboard shows the results at positions 1 to 5

#### Scenario: Fewer than 5 results
- **WHEN** the server returns 3 results for today
- **THEN** the leaderboard shows those 3 rows only

#### Scenario: No results yet
- **WHEN** the server returns no results for today
- **THEN** the leaderboard shows that there are no scores yet today

### Requirement: The player's own row is added below the top 5
When the player has a result for today and their position is not in the top 5, the leaderboard SHALL show a sixth row below the top 5 with their position, their display name marked as theirs, and their best score. When their position is in the top 5, their row there SHALL be marked as theirs and highlighted, and no extra row SHALL be added. When the player has no result for today, no own row SHALL be shown.

#### Scenario: Player outside the top 5
- **WHEN** the player is at position 76 with a best score of 125
- **THEN** below the top 5 a row shows "76", their name followed by "(you)" and "125"

#### Scenario: Player inside the top 5
- **WHEN** the player is at position 3
- **THEN** the row at position 3 is marked "(you)" and highlighted, and the table has 5 rows

#### Scenario: Player has not played today
- **WHEN** the player has no result for today
- **THEN** the leaderboard shows only the top rows

### Requirement: Each leaderboard row is read as one sentence
Each row of the daily leaderboard SHALL be announced by screen readers as one item holding the position, the name, whether it is the player's own row, and the points.

#### Scenario: TalkBack on the player's row
- **WHEN** TalkBack focuses the player's own row at position 76 with 125 points
- **THEN** it reads the position, the name, that it is the player, and 125 points as one item

### Requirement: The daily pipe starts paused behind the leaderboard
When the daily pipe opens, the game SHALL be paused with no flow and no countdown running, and SHALL show today's daily leaderboard with a Start button. While the leaderboard is downloading a loading state SHALL be shown, and when the download fails a short message SHALL replace the table. Start SHALL be available in every state, SHALL close the leaderboard and start the game. Back SHALL return to the welcome screen.

#### Scenario: Opening the daily pipe
- **WHEN** the player opens today's daily pipe
- **THEN** the game screen shows the board paused with the daily leaderboard and a Start button over it

#### Scenario: Start
- **WHEN** the player taps Start
- **THEN** the leaderboard closes and the game runs from its beginning

#### Scenario: Leaderboard cannot be loaded
- **WHEN** the top scores cannot be downloaded
- **THEN** a message says the leaderboard is unavailable and Start still starts the game

#### Scenario: Back before starting
- **WHEN** the player presses back on the starting leaderboard
- **THEN** the app returns to the welcome screen

#### Scenario: Bundled level
- **WHEN** the player starts a bundled level
- **THEN** no leaderboard is shown and the game starts as before

### Requirement: Next shows the leaderboard with Restart and Exit
Tapping Next in the daily pipe's level complete dialog SHALL show the daily leaderboard, downloaded again after the result was sent, with the buttons Restart and Exit. Restart SHALL start today's daily pipe again with the same level and stack seed, opening paused behind the leaderboard. Exit SHALL return to the welcome screen.

#### Scenario: Next after a win
- **WHEN** the player taps Next after winning the daily pipe
- **THEN** the leaderboard including the new result is shown with Restart and Exit buttons

#### Scenario: Restart
- **WHEN** the player taps Restart on the leaderboard after a win
- **THEN** today's daily pipe starts again with the same stack, paused behind the leaderboard

#### Scenario: Exit
- **WHEN** the player taps Exit on the leaderboard after a win
- **THEN** the app returns to the welcome screen
