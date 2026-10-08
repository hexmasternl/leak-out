## MODIFIED Requirements

### Requirement: Endpoint glows
While the countdown runs, the valve wheel SHALL have a brass glow that pulses with a period of 1.6 seconds. Once the liquid starts, the valve's glow SHALL stop pulsing and stay at a steady 60 %. Until the level is won or lost, the finish ring SHALL have a success glow that pulses with a period of 2 seconds. When the level is won, the finish flash SHALL replace the pulse and the ring SHALL stay solid afterwards. When the level is lost, the finish ring's glow SHALL stop pulsing and stay at a steady 60 %. The pulses SHALL follow game time, so they freeze while the game is paused. When the system setting to remove animations is on, both glows SHALL be steady at 60 % and SHALL NOT pulse.

#### Scenario: Valve pulses during the countdown
- **WHEN** a level starts and the countdown runs
- **THEN** the valve's glow grows and fades with a 1.6 second period

#### Scenario: Valve settles when the liquid starts
- **WHEN** the countdown ends
- **THEN** the valve's glow stays steady at 60 %

#### Scenario: Finish pulses until the win
- **WHEN** the liquid flows and the level is not yet won
- **THEN** the finish ring's glow grows and fades with a 2 second period

#### Scenario: Finish settles after a loss
- **WHEN** the level is lost
- **THEN** the finish ring's glow stays steady at 60 %

#### Scenario: Pulses freeze on pause
- **WHEN** the player pauses the game
- **THEN** the valve and finish glows stay as they were until the game resumes

#### Scenario: Reduced motion
- **WHEN** the system setting to remove animations is on and a level starts
- **THEN** the valve and the finish ring have steady glows at 60 % and do not pulse
