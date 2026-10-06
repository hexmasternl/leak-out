## MODIFIED Requirements

### Requirement: Bundled sound effects
The app SHALL bundle the sound effects from the repository's `assets/sfx/` folder unchanged under `app/src/main/assets/sfx/`. Each file SHALL belong to a sound group named by its file stem without a trailing `_NNN` number: `section_placement_001.mp3` belongs to `section_placement`, and `section_placement_failed.mp3` belongs to `section_placement_failed`. The groups SHALL map to game sounds as follows:
- `section_placement`: section placed
- `section_destroy`: section destroyed
- `section_placement_failed`: placement failed
- `level_complete`: level complete
- `level_failed`: level failed
- `heartbeat`: gap warning heartbeat, played in a loop with its own volume as described in `game-play-gap-warning`

A file whose group maps to no game sound SHALL be ignored. A game sound without any file SHALL be silent, and the game SHALL keep running.

#### Scenario: Bundled copies match the source
- **WHEN** the app's `assets/sfx/` folder is compared with the repository's `assets/sfx/` folder
- **THEN** both hold the same file names with the same contents

#### Scenario: Variants are grouped
- **WHEN** the bundled effects are read
- **THEN** the section placed sound has three variants, and the section destroyed, placement failed, level complete, level failed and heartbeat sounds have one each

#### Scenario: Numbered suffix only
- **WHEN** the file `section_placement_failed.mp3` is grouped
- **THEN** it belongs to the placement failed sound and not to the section placed sound

#### Scenario: Missing sound is silent
- **WHEN** no file belongs to the level failed sound and the level lost dialog appears
- **THEN** no sound plays and the dialog works as usual

#### Scenario: Missing heartbeat is silent
- **WHEN** no file belongs to the heartbeat and the liquid comes within 5 sections of a gap
- **THEN** no heartbeat plays and the score bar still warns
