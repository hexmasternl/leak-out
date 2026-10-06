## MODIFIED Requirements

### Requirement: Bundled sound effects
The app SHALL bundle the sound effects from the repository's `assets/sfx/` folder unchanged under `app/src/main/assets/sfx/`. Each file SHALL belong to a sound group named by its file stem without a trailing `_NNN` number: `section_placement_001.mp3` belongs to `section_placement`, and `section_placement_failed.mp3` belongs to `section_placement_failed`. The groups SHALL map to game sounds as follows:
- `section_placement`: section placed
- `section_destroy`: section destroyed
- `section_placement_failed`: placement failed
- `level_complete`: level complete
- `level_failed`: level failed
- `drop`: points awarded

A file whose group maps to no game sound SHALL be ignored. A game sound without any file SHALL be silent, and the game SHALL keep running.

#### Scenario: Bundled copies match the source
- **WHEN** the app's `assets/sfx/` folder is compared with the repository's `assets/sfx/` folder
- **THEN** both hold the same file names with the same contents

#### Scenario: Variants are grouped
- **WHEN** the bundled effects are read
- **THEN** the section placed sound has three variants, and the section destroyed, placement failed, level complete, level failed and points awarded sounds have one each

#### Scenario: Numbered suffix only
- **WHEN** the file `section_placement_failed.mp3` is grouped
- **THEN** it belongs to the placement failed sound and not to the section placed sound

#### Scenario: Missing sound is silent
- **WHEN** no file belongs to the level failed sound and the level lost dialog appears
- **THEN** no sound plays and the dialog works as usual

## ADDED Requirements

### Requirement: Points awarded sound
When a points popup starts, because the liquid left a section and points were awarded for it, the system SHALL play the points awarded sound once. A cross that scores twice SHALL play it once for each award. When several popups start in the same frame, the sound SHALL play once for all of them. The sound SHALL NOT play more often than once per 100 ms of real time: a popup that starts within 100 ms of the last drop SHALL be shown without a sound. Restarting the level SHALL NOT play the sound for popups that were removed. The sound SHALL play in the game and in the level designer's test play.

#### Scenario: Points for a section
- **WHEN** the liquid leaves a straight and "+10" pops up on its tile
- **THEN** the points awarded sound plays once as the popup starts

#### Scenario: Cross scores twice
- **WHEN** the liquid passes a cross for the second time, 2 seconds after the first
- **THEN** the points awarded sound plays again with the second popup

#### Scenario: Several popups at once
- **WHEN** two awards are added in the same frame
- **THEN** the points awarded sound plays once

#### Scenario: Fast forward
- **WHEN** the flow runs in fast forward and awards come every 40 ms
- **THEN** the points awarded sound plays at most once every 100 ms, and every popup is still shown

#### Scenario: Paused
- **WHEN** the game is paused
- **THEN** no popup starts and the points awarded sound does not play

#### Scenario: Sound effects off
- **WHEN** `soundEnabled` is off and points are awarded
- **THEN** the popup is shown and no sound plays
