# Spec Delta

## Purpose

Lets the player feel the important moments of a game through the device's vibration: a destroyed section, the gap warning and a lost level, following the Vibration setting.

## ADDED Requirements

### Requirement: Section replaced vibration
When the player drops a section on a placed section and the old section is destroyed, the system SHALL vibrate once, short and soft: about 30 ms at low strength. It SHALL vibrate at the moment the old section is destroyed, together with the section destroyed sound, and not again when the new section lands. A placement on an empty tile and a refused placement SHALL NOT vibrate.

#### Scenario: Replacing a section
- **WHEN** vibration is on and the player drops `V` on a tile that holds an `H` the liquid has not entered
- **THEN** the device vibrates once for about 30 ms at low strength

#### Scenario: Replacement lands
- **WHEN** the replacement penalty has passed and the new section lands on the tile
- **THEN** the device does not vibrate again

#### Scenario: Placing on an empty tile
- **WHEN** the player places a section on an empty tile
- **THEN** the device does not vibrate

#### Scenario: Refused placement
- **WHEN** the player drops a section on a tile that does not accept it
- **THEN** the device does not vibrate

### Requirement: Gap warning vibration
Every time the blinking score bar of the gap warning (`game-play-gap-warning`) starts to show the word "Warning", the system SHALL vibrate once, briefly: about 50 ms at medium strength. It SHALL NOT vibrate when the bar switches back to the score. When the bar stops blinking, the vibrations SHALL stop as well. Because the bar does not blink while the game is paused, the game SHALL NOT vibrate for the warning while paused.

#### Scenario: Blinking starts
- **WHEN** vibration is on and the gap distance drops to 3
- **THEN** the device vibrates once for about 50 ms as the bar shows "WARNING"

#### Scenario: Vibrates with every warning
- **WHEN** the bar has been blinking for 2.2 seconds
- **THEN** the device has vibrated three times, at 0, 1 and 2 seconds, once for each time the bar showed "WARNING"

#### Scenario: Building stops the vibration
- **WHEN** the bar is blinking and the player places sections that put the gap 4 sections away
- **THEN** the device does not vibrate for the warning any more

#### Scenario: Paused while blinking
- **WHEN** the game is paused while the bar blinks
- **THEN** the device does not vibrate for the warning until the game is resumed

### Requirement: Level lost vibration
When the attempt is lost, the system SHALL vibrate once for one second at full strength. It SHALL vibrate at the moment the attempt is lost, whatever the loss reason (a leak or a missed mandatory section), and not when the level lost dialog appears. It SHALL vibrate once per lost attempt. A gap warning vibration that is still running SHALL be replaced by it.

#### Scenario: Liquid leaks
- **WHEN** vibration is on and the liquid leaks into an empty tile
- **THEN** the device vibrates for one second at full strength, starting when the leak starts

#### Scenario: Mandatory section missed
- **WHEN** the liquid reaches the finish while a mandatory section is unused
- **THEN** the device vibrates for one second at full strength

#### Scenario: Once per attempt
- **WHEN** the attempt is lost and the level lost dialog appears afterwards
- **THEN** the device has vibrated only once for that attempt

#### Scenario: Restart and lose again
- **WHEN** the player restarts from the level lost dialog and the next attempt leaks as well
- **THEN** the device vibrates for one second again

#### Scenario: Won
- **WHEN** the liquid reaches the finish and the level is complete
- **THEN** the device does not vibrate

### Requirement: Haptics follow the vibration setting
The system SHALL vibrate only while the stored `vibrationEnabled` setting is on. A change to the setting SHALL apply to the next vibration without restarting the game. Turning the setting off while the device vibrates SHALL stop the vibration at once. Haptics SHALL NOT depend on the sound effect or music settings.

#### Scenario: Vibration off
- **WHEN** `vibrationEnabled` is off and the liquid leaks
- **THEN** the device does not vibrate

#### Scenario: Sound off, vibration on
- **WHEN** `soundEnabled` is off, `vibrationEnabled` is on and the player replaces a section
- **THEN** no sound plays and the device vibrates

#### Scenario: Setting changed mid-game
- **WHEN** the player turns vibration off in the settings and goes back to a game
- **THEN** replacing a section does not vibrate

### Requirement: Haptics without a vibrator
On a device without a vibrator the system SHALL NOT vibrate and the game SHALL work as usual. The app SHALL NOT ask the player for a permission to vibrate.

#### Scenario: No vibrator
- **WHEN** the device has no vibrator and the liquid leaks
- **THEN** nothing vibrates and the level lost dialog appears as usual

#### Scenario: No permission prompt
- **WHEN** the app is installed and a game is played with vibration on
- **THEN** no permission dialog is shown

### Requirement: Haptics stop in the background
When the app leaves the foreground, the system SHALL stop any running vibration and SHALL NOT vibrate until it is back in the foreground. Leaving the game screen SHALL stop any running vibration as well.

#### Scenario: App goes to the background
- **WHEN** the device is vibrating for a lost level and the player switches to another app
- **THEN** the vibration stops at once

#### Scenario: Leaving the game
- **WHEN** the device is vibrating and the player leaves the game screen for the main menu
- **THEN** the vibration stops
