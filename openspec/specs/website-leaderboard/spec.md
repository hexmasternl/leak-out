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
The leaderboard SHALL show at most 25 entries for the chosen level, highest score first, each with its position, the player's display name and the score.

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
