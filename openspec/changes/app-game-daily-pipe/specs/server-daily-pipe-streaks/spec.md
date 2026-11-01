## ADDED Requirements

### Requirement: A streak counts consecutive UTC days with a daily pipe win
The system SHALL keep, per player, a current streak and a best streak. A win is a submission with at least 1 star for the current daily pipe. On the player's first win on date D, the current streak SHALL increase by 1 when the player's last win was on D minus 1, and SHALL become 1 otherwise. Further wins on the same date SHALL NOT change the streak. The best streak SHALL be the highest current streak the player has reached.

#### Scenario: Consecutive days
- **WHEN** a player with a current streak of 3 and a last win on 2026-11-01 wins the pipe of 2026-11-02
- **THEN** the current streak becomes 4

#### Scenario: Second win on the same day
- **WHEN** a player who already won today's pipe wins it again
- **THEN** the current streak is unchanged

#### Scenario: A submission without stars is not a win
- **WHEN** a player submits a score with 0 stars for today's pipe
- **THEN** the streak is unchanged

### Requirement: A missed day resets the streak
The system SHALL report a current streak of 0 for a player whose last win is before yesterday's UTC date, and SHALL restart the streak at 1 on that player's next win. The best streak SHALL be kept.

#### Scenario: Day missed
- **WHEN** a player with a current streak of 5 and a last win on 2026-10-30 requests today's pipe on 2026-11-02
- **THEN** the reported current streak is 0 and the best streak is still at least 5

#### Scenario: Win after a missed day
- **WHEN** that player wins the pipe of 2026-11-02
- **THEN** the current streak becomes 1

### Requirement: The streak is returned to the app
The system SHALL include the player's current streak, best streak and whether the player has already won today's pipe in the response of `GET /api/daily-pipes/today` and of a daily pipe score submission.

#### Scenario: Streak after a win
- **WHEN** a player wins today's pipe and submits the score
- **THEN** the response contains the updated current streak and states that today's pipe has been won
