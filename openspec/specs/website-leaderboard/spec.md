# website-leaderboard Specification

## Purpose
Lets website visitors pick a level and see the 25 best scores of that level, so players can compare themselves with others without opening the game.

## Requirements
### Requirement: Choose a level
The leaderboard page SHALL let the visitor choose one of the game's library levels. Level 1 SHALL be selected when the page opens.

#### Scenario: Page opens
- **WHEN** a visitor opens the leaderboard page
- **THEN** level 1 is selected and its top scores are loaded

#### Scenario: Choose another level
- **WHEN** the visitor chooses level 12
- **THEN** the top scores of level 12 are loaded and shown

### Requirement: Top 25 scores
The leaderboard SHALL show at most 25 entries for the chosen level, highest score first, each with its position, the player's display name, the score and when the score was set.

#### Scenario: Level with many scores
- **WHEN** the chosen level has 40 scores
- **THEN** the entries with positions 1 to 25 are shown, highest score first

### Requirement: Leaderboard states
While scores load, the leaderboard SHALL show a loading indicator. A level without scores SHALL show an empty message. When the scores cannot be loaded, it SHALL show an error message and a Try again action. All messages SHALL be in the page's language.

#### Scenario: Empty level
- **WHEN** the chosen level has no scores
- **THEN** the leaderboard shows a message that nobody has a score on this level yet

#### Scenario: Server unreachable
- **WHEN** the score server does not respond
- **THEN** the leaderboard shows an error message and a Try again action, and Try again loads the scores again

### Requirement: When a score was set is shown as relative time
The leaderboard SHALL show the moment each score was set as relative time in the page's language, in the largest whole unit that fits (minutes, hours, days, weeks, months, years), and as "now" or its translation under one minute. A moment in the future SHALL be shown as "now". The full date and time SHALL be available for each entry.

#### Scenario: Score from two days ago
- **WHEN** an entry's score was set 2 days and 3 hours ago and the page is in English
- **THEN** the entry shows "2 days ago"

#### Scenario: Score from over a year ago in Dutch
- **WHEN** an entry's score was set 14 months ago and the page is in Dutch
- **THEN** the entry shows "1 jaar geleden"

#### Scenario: Score from seconds ago
- **WHEN** an entry's score was set 20 seconds ago
- **THEN** the entry shows "now" in the page's language

#### Scenario: Full date on request
- **WHEN** the visitor hovers over or focuses an entry's relative time
- **THEN** the full date and time of that score is shown in the page's language

#### Scenario: Missing moment
- **WHEN** an entry in the response has no valid moment
- **THEN** the entry is still shown, with an empty when column
