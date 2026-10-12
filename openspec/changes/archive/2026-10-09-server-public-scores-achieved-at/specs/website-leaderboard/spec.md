# Spec Delta

## MODIFIED Requirements

### Requirement: Top 25 scores
The leaderboard SHALL show at most 25 entries for the chosen level, highest score first, each with its position, the player's display name, the score and when the score was set.

#### Scenario: Level with many scores
- **WHEN** the chosen level has 40 scores
- **THEN** the entries with positions 1 to 25 are shown, highest score first

## ADDED Requirements

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
