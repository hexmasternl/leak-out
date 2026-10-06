# Spec Delta

## MODIFIED Requirements

### Requirement: Moving through and skipping the intro tour
Every step except the last SHALL offer "Next" and "Skip tour"; the last step SHALL offer "Let's go". Finishing or skipping the tour SHALL mark it as seen, close it and start the countdown.

#### Scenario: Finishing the tour
- **WHEN** the zoom and pan step is shown and the player taps "Let's go"
- **THEN** the tour closes, the countdown starts running and the intro tour is stored as seen

#### Scenario: Skipping the tour
- **WHEN** the stack step is shown and the player taps "Skip tour"
- **THEN** the tour closes without showing the remaining steps, the countdown starts running and the intro tour is stored as seen

#### Scenario: Tour interrupted by closing the app
- **WHEN** the app is closed while the intro tour is shown and the player later starts a level
- **THEN** the intro tour is shown again from its first step

#### Scenario: Tap during a tip animation
- **WHEN** a tour step has just appeared, and its card entry, spotlight glide or field reveal is still animating, and the player taps "Next"
- **THEN** the tour moves to the next step

#### Scenario: Tap on the first step after a fresh install
- **WHEN** the app is freshly installed, the first level opens with the start step of the tour and the player taps "Skip tour" once
- **THEN** the tour closes, the countdown starts running and the intro tour is stored as seen

#### Scenario: Rapid taps on Next
- **WHEN** the start step is shown and the player taps "Next" twice in quick succession
- **THEN** the tour shows the stack step: each completed tap advances exactly one step

#### Scenario: Rapid taps after skipping
- **WHEN** the player taps "Skip tour" and then quickly taps the same spot again
- **THEN** the tour stays closed and the second tap does not reach the game

## ADDED Requirements

### Requirement: Tip buttons respond on every supported device
A tip's buttons SHALL lie fully inside the screen area the app can safely take touches in, clear of the display cutout and of the system gesture areas, on every supported screen size and density in landscape. A press on a tip button that shows press feedback SHALL run the button's action when the finger lifts on the button, and the button SHALL not move while it is pressed.

#### Scenario: Buttons on a phone with a punch-hole camera
- **WHEN** the intro tour is shown on a 2400×1080 phone at about 420 dpi with a display cutout
- **THEN** "Next" and "Skip tour" lie fully outside the cutout and the system gesture areas, and a tap on either takes effect

#### Scenario: Press feedback means the action runs
- **WHEN** the player presses "Next", sees its press feedback and lifts the finger on the button
- **THEN** the tour moves to the next step

#### Scenario: The card stays put during a press
- **WHEN** the player holds a finger on "Skip tour" while the spotlight still animates
- **THEN** the button stays under the finger until it is lifted
