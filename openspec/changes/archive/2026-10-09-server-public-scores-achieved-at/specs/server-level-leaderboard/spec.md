# Spec Delta

## ADDED Requirements

### Requirement: App leaderboard entries carry no timestamps
The leaderboards the game reads (the top scores of a level and the window around the calling player) SHALL NOT contain the moment a score was received or any other date or time. That moment is only served by the public top scores.

#### Scenario: Top scores for the app
- **WHEN** a signed-in player requests the top scores of level 3
- **THEN** no entry in the response contains a date or time

#### Scenario: Window for the app
- **WHEN** a signed-in player requests the leaderboard window around them on level 3
- **THEN** no entry in the response contains a date or time
