# Spec Delta

## ADDED Requirements

### Requirement: Splitter tip
The first time the player starts a level with a splitter, the game SHALL freeze before the countdown runs and show the splitter tip, highlighting the splitter and both finishes. The tip SHALL explain that the splitter sends the liquid out through both other open sides, that each branch needs its own route to a finish, and that the level is lost as soon as either branch leaks. Its text SHALL come from the app's string resources, so it is shown in every supported language.

#### Scenario: First level with a splitter
- **WHEN** the splitter tip has not been seen and a level with a splitter starts
- **THEN** the game is frozen, the splitter and both finishes are highlighted and the splitter tip is shown

#### Scenario: Splitter tip shown once
- **WHEN** the splitter tip has been seen and a level with a splitter starts
- **THEN** no splitter tip is shown

#### Scenario: Tip texts are translatable
- **WHEN** the release build's lint check runs
- **THEN** the splitter tip's title, text and button are string resources with English defaults, and none of them is hard-coded

## MODIFIED Requirements

### Requirement: Single tips are dismissed with "Got it"
The cross, reservoir, mandatory section and splitter tips SHALL each offer one "Got it" button. Tapping it SHALL close the tip, store it as seen and continue the game from where it froze.

#### Scenario: Continue after a tip
- **WHEN** the cross tip is shown while the liquid is halfway through a tile and the player taps "Got it"
- **THEN** the tip closes, the cross tip is stored as seen and the liquid continues from halfway through that tile

#### Scenario: Continue after the splitter tip
- **WHEN** the splitter tip is shown and the player taps "Got it"
- **THEN** the tip closes, the splitter tip is stored as seen and the countdown starts

### Requirement: One tip at a time in a fixed order
When more than one tip is due at the same moment, the game SHALL show them one after the other in this order: intro tour, mandatory section tip, splitter tip, cross tip, reservoir tip. The game SHALL stay frozen until the last of them is closed.

#### Scenario: Everything due on the first level
- **WHEN** no tip has been seen and the first level played has a mandatory section and an `X` and a `RES` in fully visible slots of its starting stack
- **THEN** the intro tour, the mandatory section tip, the cross tip and the reservoir tip are shown in that order, and the countdown only starts after the reservoir tip is closed

#### Scenario: Splitter tip with other tips
- **WHEN** the splitter, mandatory section and cross tips have not been seen and a level with a splitter, a mandatory section and an `X` in a fully visible slot starts
- **THEN** the mandatory section tip, the splitter tip and the cross tip are shown in that order
