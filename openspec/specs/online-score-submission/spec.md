# online-score-submission Specification

## Purpose
Sends the score of every won level to the Leak Out score server for players signed in to Google Play Games, keeping scores won offline in a queue until they can be sent, so the online leaderboards hold the players' results, and shows the player whether the score was sent.

## Requirements

### Requirement: A won level's score is queued and sent while the level complete dialog is shown
When the level complete dialog of a won attempt is shown and posting is possible, the system SHALL put the attempt's final total for the level id in the pending score queue once, and SHALL then send the queue. Posting is possible when the player is signed in to Play Games, the score server is configured, and the level is not a test play from the level designer.

#### Scenario: Signed-in player wins a level online
- **WHEN** the player is signed in to Play Games and online, wins level 4 with a final total of 108 and the level complete dialog is shown
- **THEN** the score server receives a score of 108 for level 4 and level 4 is no longer in the queue

#### Scenario: Sent when the dialog shows, not before
- **WHEN** the level has been won for less than 1 second and the level complete dialog is not shown yet
- **THEN** nothing has been queued or sent

#### Scenario: Total of zero
- **WHEN** a signed-in player wins level 2 with a final total of 0
- **THEN** a score of 0 for level 2 is queued and sent

### Requirement: No post without a Play Games sign-in
The system SHALL NOT queue scores and SHALL NOT contact the score server when the Play Games sign-in state is anything other than signed in, including signed out, signing in and unavailable.

#### Scenario: Signed out
- **WHEN** the player is signed out of Play Games and wins a level
- **THEN** nothing is queued, no request is sent to the score server and no status dot is shown

#### Scenario: Play Games unavailable
- **WHEN** Play Games is not configured in the build and the player wins a level
- **THEN** nothing is queued, no request is sent to the score server and no status dot is shown

#### Scenario: Queue waits for a sign-in
- **WHEN** scores are queued and the player is signed out
- **THEN** the queue is not sent and keeps its scores

### Requirement: Score server configuration
The build SHALL carry a score server address and a server client id. By default the address SHALL be `https://rg-leakout-prod-neu-api.jollywater-e41c70f8.northeurope.azurecontainerapps.io` and the client id `201820010117-uteqrtu77v48c9ovd8d9k9qan0sktcj4.apps.googleusercontent.com`; a build MAY override either. When the address is blank or not `https://`, or the client id is blank, the system SHALL NOT queue or post scores and SHALL NOT show the status dot.

#### Scenario: Default configuration
- **WHEN** the app is built without overriding the score server settings
- **THEN** scores are sent to `https://rg-leakout-prod-neu-api.jollywater-e41c70f8.northeurope.azurecontainerapps.io` and the server auth code is requested for client id `201820010117-uteqrtu77v48c9ovd8d9k9qan0sktcj4.apps.googleusercontent.com`

#### Scenario: Score server turned off
- **WHEN** the build overrides the score server address with a blank value, the player is signed in to Play Games and wins a level
- **THEN** nothing is queued or sent and the level complete dialog behaves as without score posting

### Requirement: Test plays are not posted
A test play of a design in the level designer SHALL NOT queue or post a score.

#### Scenario: Designer test play won
- **WHEN** a signed-in player wins a test play in the level designer
- **THEN** nothing is queued or sent and no status dot is shown

### Requirement: The queue keeps only the highest score per level
The pending score queue SHALL hold at most one score per level. Queuing a score for a level that is not in the queue SHALL add it; a higher score SHALL replace the queued one; an equal or lower score SHALL leave the queue unchanged.

#### Scenario: Higher score replaces
- **WHEN** level 3 is queued with 90 and the player wins level 3 offline with 120
- **THEN** the queue holds 120 for level 3 only

#### Scenario: Lower score ignored
- **WHEN** level 3 is queued with 120 and the player wins level 3 offline with 90
- **THEN** the queue still holds 120 for level 3

#### Scenario: Different levels
- **WHEN** the player wins level 3 with 120 and level 5 with 80 while offline
- **THEN** the queue holds 120 for level 3 and 80 for level 5

### Requirement: The queue survives restarts
The pending score queue SHALL be stored on the device and SHALL keep its scores when the app is closed, killed or the device restarts, until they are sent.

#### Scenario: App restarted while offline
- **WHEN** level 3 is queued with 120 and the app is closed and opened again
- **THEN** the queue still holds 120 for level 3

### Requirement: The queue is sent when sending can succeed
While posting is possible, the system SHALL send every queued score when a level is won, when the app starts, when the player becomes signed in to Play Games and when the device gets a network connection. Only one send of the queue SHALL run at a time.

#### Scenario: Back online
- **WHEN** levels 3 and 5 are queued, the player is signed in and the device gets a network connection
- **THEN** the score server receives the queued scores for levels 3 and 5 and the queue is empty

#### Scenario: Signing in sends the queue
- **WHEN** level 3 is queued, the player was signed out and signs in to Play Games while online
- **THEN** the score server receives the queued score for level 3

#### Scenario: App start sends the queue
- **WHEN** level 3 is queued and the app starts online with the player signed in
- **THEN** the score server receives the queued score for level 3

#### Scenario: Win sends earlier scores too
- **WHEN** level 3 is queued from an offline win and the player wins level 4 online
- **THEN** the score server receives the scores for levels 3 and 4

### Requirement: Sent and rejected scores leave the queue
A queued score SHALL be removed when the server accepts it, also when the server keeps an earlier, higher best. A score the server rejects as invalid SHALL be removed and not sent again. A score that could not be sent (no network, timeout, server error, failed sign-in) SHALL stay queued. A higher score queued while a lower one for the same level is being sent SHALL stay queued.

#### Scenario: Offline send
- **WHEN** a send fails because the device is offline
- **THEN** every queued score stays in the queue

#### Scenario: Server rejects the score
- **WHEN** the server answers a queued score with a validation error
- **THEN** that score is removed from the queue and the other queued scores are still sent

#### Scenario: Higher score arrives during a send
- **WHEN** 90 for level 3 is being sent and the player queues 120 for level 3 before the server answers
- **THEN** after the server accepts 90, the queue still holds 120 for level 3

### Requirement: A failed send changes nothing else
A send that fails SHALL NOT affect the game, the dialog, the local level history or the achievements. Each request SHALL give up after at most 35 seconds. Once connected, a request SHALL wait at least 30 seconds for the server's answer, so a server that is starting up from idle can still answer the first request. Connecting SHALL give up after at most 10 seconds.

#### Scenario: Offline win
- **WHEN** a signed-in player wins a level while the device is offline
- **THEN** the dialog works as usual, the local best score is recorded and the score is queued

#### Scenario: Buttons do not wait for the send
- **WHEN** the score and star animations are done while the send is still in progress
- **THEN** Next Level, Restart and Exit are enabled

#### Scenario: Server starting up from idle
- **WHEN** the server accepts the connection and answers a request after 15 seconds
- **THEN** the request succeeds and is not counted as a failed send

#### Scenario: Server does not answer
- **WHEN** the server accepts the connection but has not answered after 35 seconds
- **THEN** the request gives up and counts as a failed send

### Requirement: The app authenticates with a Play Games server auth code
To send, the system SHALL sign in to the score server with a one-time Play Games server auth code requested for the configured client id, and SHALL send the access token it receives with each score. It SHALL reuse that token until shortly before it expires.

#### Scenario: First send of the session
- **WHEN** the queue is sent and the app has no access token yet
- **THEN** the app requests a server auth code, signs in to the score server with it and sends the scores with the returned access token

#### Scenario: Token still valid
- **WHEN** the queue is sent again 10 minutes later with an access token valid for one hour
- **THEN** the app sends the scores with the same token without signing in again

#### Scenario: Token rejected
- **WHEN** the score server answers a score with 401
- **THEN** the app signs in to the score server again once and sends that score once more with the new token

### Requirement: Leaving the dialog does not cancel the send
When the player leaves the level complete dialog (Next Level, Restart, Exit or Back) while the send is in progress, the send SHALL still complete, and the status dot SHALL disappear with the dialog.

#### Scenario: Next level during the send
- **WHEN** the send is in progress and the player taps Next Level
- **THEN** the next level starts without a status dot and the score server still receives the score

### Requirement: Status dot shows the send in progress
While the won level's score is being sent, the system SHALL show a small yellow dot in the top-left corner of the screen, above the dialog's scrim, blinking on and off.

#### Scenario: Send in progress
- **WHEN** the level complete dialog is shown and the send has not finished
- **THEN** a yellow dot blinks in the top-left corner

### Requirement: Status dot blinks green once on success
When the won level's queued score reached the server, the dot SHALL turn green, blink once and then disappear.

#### Scenario: Score accepted
- **WHEN** the score server accepts the won level's score
- **THEN** the dot blinks green once and is then gone

#### Scenario: Not a new best
- **WHEN** the score server accepts the score but keeps an earlier, higher best
- **THEN** the dot blinks green once and is then gone

### Requirement: Status dot blinks red three times on failure
When the won level's score could not be sent and stays queued, or was rejected, the dot SHALL turn red, blink three times and then disappear.

#### Scenario: Send failed
- **WHEN** the device is offline and the send fails
- **THEN** the dot blinks red three times and is then gone, and the score is still queued

### Requirement: Status dot is unobtrusive
The status dot SHALL NOT take touch input, SHALL NOT cover the dialog's content or buttons, and SHALL NOT be announced by a screen reader. Sends of the queue outside the level complete dialog SHALL show no dot.

#### Scenario: Screen reader during the send
- **WHEN** a screen reader is on and the send is in progress
- **THEN** the screen reader announces the level complete dialog and does not read the status dot

#### Scenario: Background send
- **WHEN** the queue is sent because the device came back online while the player is on the welcome screen
- **THEN** no status dot is shown
