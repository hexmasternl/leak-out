# background-music Specification

## Purpose
TBD - created by archiving change add-background-music. Update Purpose after archive.
## Requirements
### Requirement: Bundled playlist
The app SHALL bundle the ten background tracks as `music/bg_music_0001.mp3` through `music/bg_music_0010.mp3`, and the credits file `music/audio_credits.json`, which lists each track's `title`, `artist` and `file`. The order of `audio_credits.json` SHALL be the playback order, and a track's id SHALL be its file name without the extension. The bundled credits file and tracks SHALL be identical to the design source in `assets/audio/` at the repository root.

#### Scenario: Bundled copy matches the design source
- **WHEN** the bundled `audio_credits.json` and tracks are compared with those in the repository's `assets/audio/`
- **THEN** the credits files have the same content and each bundled track has the same size as its source

#### Scenario: Every credits entry has its audio file
- **WHEN** each entry of `audio_credits.json` is resolved against the bundled assets
- **THEN** an audio file exists for every entry

#### Scenario: Playback order follows the file numbers
- **WHEN** the playlist is loaded
- **THEN** it holds ten tracks with ids `bg_music_0001` to `bg_music_0010` in that order

#### Scenario: Track ids are unique
- **WHEN** a credits file that lists the same file twice is loaded
- **THEN** it is rejected and no music plays

### Requirement: Continuous playback in playlist order
While music is enabled and the app is in the foreground, the app SHALL play one track at a time. When a track finishes, it SHALL start the next track in playlist order, and after the last track it SHALL start the first track. Playback SHALL continue unbroken when the player moves between screens.

#### Scenario: Next track after completion
- **WHEN** track 3 of the playlist finishes
- **THEN** track 4 starts

#### Scenario: Wrap around after the last track
- **WHEN** the last track of the playlist finishes
- **THEN** the first track starts

#### Scenario: Screen change does not interrupt the music
- **WHEN** a track is playing and the player goes from the welcome screen to a level
- **THEN** the same track keeps playing from its current position

### Requirement: Starting track rotates across launches
The app SHALL store the id of the last track that started, each time a new track begins. On a cold start it SHALL begin with the track after the stored one in playlist order, wrapping after the last track. If no id is stored, or the stored id is not in the playlist, it SHALL begin with the first track. Resuming a paused track SHALL NOT count as starting a track.

#### Scenario: First launch
- **WHEN** the app starts with music enabled and no last-started track is stored
- **THEN** the first track of the playlist starts and its id is stored

#### Scenario: Next launch continues the rotation
- **WHEN** the stored last-started track is track 4 and the app is started again
- **THEN** track 5 starts

#### Scenario: Rotation wraps across launches
- **WHEN** the stored last-started track is the last track of the playlist and the app is started again
- **THEN** the first track starts

#### Scenario: Later tracks in a session are recorded
- **WHEN** track 2 started at launch, finished, track 3 started, and the app is then closed and started again
- **THEN** track 4 starts

#### Scenario: Unknown stored id
- **WHEN** the stored last-started id is not in the playlist and the app is started
- **THEN** the first track starts

#### Scenario: Resume does not advance the rotation
- **WHEN** track 6 is paused because the app went to the background, and the app returns to the foreground
- **THEN** track 6 resumes and the stored last-started id is still track 6

#### Scenario: Silent session leaves the rotation alone
- **WHEN** music is disabled for an entire session in which the stored last-started track is track 4, and the app is then started with music enabled
- **THEN** track 5 starts

### Requirement: Pause in the background
The app SHALL pause the music when it leaves the foreground and SHALL resume the same track at the same position when it returns. Recreating the activity, for example after a configuration change, SHALL neither restart the current track nor start a new one.

#### Scenario: Going to the background
- **WHEN** a track is playing and the app goes to the background
- **THEN** the music pauses

#### Scenario: Returning to the foreground
- **WHEN** the app returns to the foreground after its music was paused at 1:20 into a track
- **THEN** the same track resumes from 1:20

#### Scenario: Activity recreated
- **WHEN** the activity is recreated while a track is playing
- **THEN** the same track continues and the stored last-started id is unchanged

### Requirement: Music follows the music settings
The app SHALL play music only while `musicEnabled` is on, and SHALL apply `musicVolume` to playback. Changes to either SHALL take effect immediately, without restarting the app. Turning music off SHALL pause the current track, and turning it back on SHALL resume that track at the position where it paused. If no track has started yet in the session, turning music on SHALL start the track chosen by the rotation.

#### Scenario: Music disabled at launch
- **WHEN** the app starts with music disabled
- **THEN** no track plays and the stored last-started id is unchanged

#### Scenario: Turning music off
- **WHEN** a track is playing and music is turned off
- **THEN** the music stops being heard

#### Scenario: Turning music back on
- **WHEN** music was turned off during a track and is turned on again
- **THEN** the same track resumes from where it paused

#### Scenario: Enabling music in a silent session
- **WHEN** the app started with music disabled, the stored last-started track is track 2, and music is turned on
- **THEN** track 3 starts

#### Scenario: Volume change
- **WHEN** a track is playing and the music volume is changed to 0.3
- **THEN** playback continues at volume 0.3

### Requirement: Audio focus
The app SHALL request audio focus before it starts or resumes music. It SHALL pause when focus is lost, resume when focus returns after a transient loss, and play at a reduced volume while another app is allowed to duck it.

#### Scenario: Focus lost permanently
- **WHEN** another app takes audio focus permanently
- **THEN** the music pauses and does not resume on its own

#### Scenario: Transient focus loss
- **WHEN** audio focus is lost transiently and then regained
- **THEN** the music pauses and then resumes the same track

#### Scenario: Ducking
- **WHEN** audio focus is lost with permission to duck
- **THEN** the music plays at 20 % of the set music volume until focus is regained

### Requirement: Playback failures never stop the game
If a track cannot be opened or played, the app SHALL skip to the next track without recording the failed one as started. If every track fails in a session, or the manifest cannot be read, the app SHALL stay silent and keep working.

#### Scenario: Broken track is skipped
- **WHEN** track 5 fails to open
- **THEN** track 6 starts and the stored last-started id becomes track 6

#### Scenario: Unreadable manifest
- **WHEN** the playlist manifest is missing or invalid
- **THEN** no music plays and the app runs normally

