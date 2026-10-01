# game-sound-effects Specification

## Purpose
TBD - created by archiving change app-game-sound-effects. Update Purpose after archive.
## Requirements
### Requirement: Bundled sound effects
The app SHALL bundle the sound effects from the repository's `assets/sfx/` folder unchanged under `app/src/main/assets/sfx/`. Each file SHALL belong to a sound group named by its file stem without a trailing `_NNN` number: `section_placement_001.mp3` belongs to `section_placement`, and `section_placement_failed.mp3` belongs to `section_placement_failed`. The groups SHALL map to game sounds as follows:
- `section_placement`: section placed
- `section_destroy`: section destroyed
- `section_placement_failed`: placement failed
- `level_complete`: level complete
- `level_failed`: level failed

A file whose group maps to no game sound SHALL be ignored. A game sound without any file SHALL be silent, and the game SHALL keep running.

#### Scenario: Bundled copies match the source
- **WHEN** the app's `assets/sfx/` folder is compared with the repository's `assets/sfx/` folder
- **THEN** both hold the same file names with the same contents

#### Scenario: Variants are grouped
- **WHEN** the bundled effects are read
- **THEN** the section placed sound has three variants, and the section destroyed, placement failed, level complete and level failed sounds have one each

#### Scenario: Numbered suffix only
- **WHEN** the file `section_placement_failed.mp3` is grouped
- **THEN** it belongs to the placement failed sound and not to the section placed sound

#### Scenario: Missing sound is silent
- **WHEN** no file belongs to the level failed sound and the level lost dialog appears
- **THEN** no sound plays and the dialog works as usual

### Requirement: Random variant
When a game sound has more than one file, the system SHALL pick one of its files at random, each with equal chance, every time the sound plays. A game sound with one file SHALL always play that file.

#### Scenario: Placement variants vary
- **WHEN** the section placed sound plays many times with a seeded random source
- **THEN** each of the three placement files is played at least once, in the order the random source picks them

#### Scenario: Single variant
- **WHEN** the section destroyed sound plays
- **THEN** `section_destroy_001.mp3` plays

### Requirement: Section placed sound
When a section is placed on an empty tile, by a drop or by a tap, the system SHALL play the section placed sound once.

#### Scenario: Drop on an empty tile
- **WHEN** the player drops a section on an empty tile and it is placed
- **THEN** the section placed sound plays once

#### Scenario: Tap on an empty tile
- **WHEN** the player taps an empty tile and the next section is placed there
- **THEN** the section placed sound plays once

### Requirement: Section destroyed sound
When a section is placed on a tile that held a placed section, so the old section is destroyed, the system SHALL play the section destroyed sound once. It SHALL NOT also play the section placed sound for that placement.

#### Scenario: Replacing a section
- **WHEN** the player drops `V` on a tile holding an `H` that the liquid has not entered, in a level that allows replacing
- **THEN** the section destroyed sound plays once and the section placed sound does not play

### Requirement: Placement failed sound
When the player drops or taps a section on a tile of the level that does not accept it by the placement rule, the system SHALL play the placement failed sound once. This covers blocked, start and finish tiles, a placed section the liquid has entered, a placed section in a level that does not allow replacing, and any tile while the replace lock is active. Letting go of a section outside the level's tiles, for example over the stack or the HUD, SHALL NOT play a sound. A drag that is cancelled, for example because the game pauses, SHALL NOT play a sound.

#### Scenario: Drop on a blocked tile
- **WHEN** the player drops a section on a blocked tile
- **THEN** nothing is placed and the placement failed sound plays once

#### Scenario: Drop on a filled section
- **WHEN** the player drops a section on a placed section the liquid has entered
- **THEN** nothing is placed and the placement failed sound plays once

#### Scenario: Drop during the replace lock
- **WHEN** the player drops a section on an empty tile while the replace lock is active
- **THEN** nothing is placed and the placement failed sound plays once

#### Scenario: Tap on the start tile
- **WHEN** the player taps the start tile
- **THEN** nothing is placed and the placement failed sound plays once

#### Scenario: Letting go outside the field
- **WHEN** the player lets go of a dragged section over the stack
- **THEN** the section goes back to the stack and no sound plays

#### Scenario: Drag cancelled by pause
- **WHEN** the game pauses while the player drags a section
- **THEN** the section goes back to the stack and no sound plays

### Requirement: Level complete sound
The system SHALL play the level complete sound once when the level complete dialog appears for an attempt. It SHALL NOT play again for the same attempt while the dialog stays open, for example while its tally runs. When the dialog first appears while the app is in the background, the sound SHALL play when the app comes back to the foreground, not in the background.

#### Scenario: Dialog appears
- **WHEN** a level is won and the level complete dialog appears
- **THEN** the level complete sound plays once

#### Scenario: Tally runs
- **WHEN** the level complete dialog stays open while its tally counts up
- **THEN** the level complete sound does not play again

#### Scenario: Restart and win again
- **WHEN** the player restarts from the level complete dialog and wins the next attempt
- **THEN** the level complete sound plays once more when the dialog appears again

#### Scenario: Won while in the background
- **WHEN** the app goes to the background during the finish flash and the player returns
- **THEN** the level complete sound plays once after the app is back in the foreground

### Requirement: Level failed sound
The system SHALL play the level failed sound once when the level lost dialog appears for an attempt, not when the leak starts. It SHALL NOT play again for the same attempt while the dialog stays open. When the dialog first appears while the app is in the background, the sound SHALL play when the app comes back to the foreground, not in the background.

#### Scenario: Dialog appears after the leak effect
- **WHEN** the liquid leaks and the level lost dialog appears after the leak effect
- **THEN** the level failed sound plays once, at the moment the dialog appears, and not when the leak starts

#### Scenario: Lost while in the background
- **WHEN** the app goes to the background during the leak effect and the player returns
- **THEN** the level failed sound plays once after the app is back in the foreground

### Requirement: Sound effects follow the settings
The system SHALL play sound effects only while the stored `soundEnabled` setting is on, and SHALL play them at the stored `soundVolume`, used linearly from 0.0 (silent) to 1.0 (full). A change to either setting SHALL apply to the next sound that plays, without restarting the game. Sound effects SHALL NOT change the music volume or pause the music.

#### Scenario: Sound effects off
- **WHEN** `soundEnabled` is off and the player places a section
- **THEN** no sound plays

#### Scenario: Volume applied
- **WHEN** `soundVolume` is 0.3 and the player places a section
- **THEN** the section placed sound plays at volume 0.3

#### Scenario: Setting changed mid-game
- **WHEN** the player turns sound effects off in the settings and goes back to a game
- **THEN** placing a section plays no sound

