## ADDED Requirements

### Requirement: Daily Pipe button only for signed-in Play Games players
The welcome screen SHALL show a Daily Pipe button only while the player is signed in with Google Play Games. When the player is not signed in, the button SHALL NOT be shown.

#### Scenario: Signed in
- **WHEN** the welcome screen is shown and the player is signed in with Play Games
- **THEN** the Daily Pipe button is visible

#### Scenario: Not signed in
- **WHEN** the welcome screen is shown and the player is not signed in with Play Games
- **THEN** no Daily Pipe button is shown

#### Scenario: Signing in later
- **WHEN** the player signs in with Play Games while the welcome screen is shown
- **THEN** the Daily Pipe button appears

### Requirement: Daily Pipe button is disabled without internet
While the device has no internet connection, the Daily Pipe button SHALL be shown disabled with a hint that an internet connection is needed. The button SHALL become enabled again as soon as a connection is available.

#### Scenario: Offline
- **WHEN** a signed-in player opens the welcome screen without an internet connection
- **THEN** the Daily Pipe button is disabled and the hint is shown

#### Scenario: Connection restored
- **WHEN** the internet connection returns while the welcome screen is shown
- **THEN** the Daily Pipe button becomes enabled

### Requirement: The daily pipe is downloaded before it is played
Tapping the enabled Daily Pipe button SHALL download today's daily pipe from the server, show a loading state while doing so, parse the level with `LevelParser` and validate it with `LevelValidator`. When the server has no pipe for today, the download fails, or the level cannot be parsed or validated, the app SHALL show an error message and stay on the welcome screen.

#### Scenario: Successful download
- **WHEN** a signed-in, online player taps the Daily Pipe button and the server returns today's pipe
- **THEN** the game screen opens with the downloaded level

#### Scenario: No pipe yet
- **WHEN** the server responds that no daily pipe is published yet
- **THEN** the app shows that the Daily Pipe is not available yet and stays on the welcome screen

#### Scenario: Unsupported level
- **WHEN** the downloaded level fails to parse or validate
- **THEN** the app shows an error and does not open the game screen

### Requirement: Every player gets the same stack on the daily pipe
The stack of the daily pipe SHALL be dealt from a random source seeded with the stack seed from the server, so every player gets the same sections in the same order. Restarting the daily pipe SHALL deal the same sequence again.

#### Scenario: Same seed, same stack
- **WHEN** two devices start the daily pipe with the same stack seed
- **THEN** both deal the same sections in the same order

#### Scenario: Restart
- **WHEN** the player restarts the daily pipe
- **THEN** the stack deals the same sequence as the first attempt

### Requirement: Daily pipe results are submitted to the server
When the player wins the daily pipe, the app SHALL submit the score and stars for the pipe's date to the server. The level-complete dialog SHALL show the score, the player's current streak and the day's top scores. When the server rejects the submission because the day has ended, the dialog SHALL say that the daily pipe has ended.

#### Scenario: Win
- **WHEN** the player wins the daily pipe with 2 stars and 380 points
- **THEN** the app submits 380 points and 2 stars for that date and the dialog shows the streak returned by the server

#### Scenario: Day ended during play
- **WHEN** the player wins after 00:00 UTC on the pipe of the previous day
- **THEN** the dialog shows that the daily pipe has ended and no streak change

### Requirement: Daily results stay separate from level progress
Daily pipe attempts SHALL NOT be recorded in level history, SHALL NOT unlock bundled levels or pages, and SHALL NOT be evaluated for achievements.

#### Scenario: Win does not unlock levels
- **WHEN** the player wins the daily pipe
- **THEN** the set of unlocked bundled levels and pages is unchanged and no level history entry is added

### Requirement: Daily Pipe strings are translated
All new Daily Pipe strings SHALL be available in every supported language.

#### Scenario: Translations complete
- **WHEN** the app's string resources are checked
- **THEN** every Daily Pipe string has a translation in every supported language
