# Spec Delta

## MODIFIED Requirements

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
