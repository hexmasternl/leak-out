# Spec Delta

## MODIFIED Requirements

### Requirement: About section
The About panel SHALL show the app name "Leak Out", the installed version as "Version <versionName> (<versionCode>)", the maker credit and a privacy line saying that no account is needed and that, only when the player signs in to Google Play Games, achievements are shared with Google Play and the scores of won levels are sent to the Leak Out score server. Below these it SHALL show a "Music" section that lists every background track as its title and artist, in playlist order, read from the bundled `audio_credits.json`. If the credits cannot be read, the Music section SHALL be left out.

#### Scenario: Version shown
- **WHEN** the installed app has version name "0.1.0" and version code 1
- **THEN** the About panel shows "Version 0.1.0 (1)"

#### Scenario: About content
- **WHEN** the About panel is displayed
- **THEN** it shows "Leak Out", the maker credit and the privacy line

#### Scenario: Privacy line mentions scores
- **WHEN** the About panel is displayed
- **THEN** the privacy line says that scores are sent to the Leak Out score server only when signed in to Google Play Games

#### Scenario: Music credits listed
- **WHEN** the About panel is displayed with the bundled credits
- **THEN** a "Music" section shows ten entries in playlist order, the first being "Ebb and Flow" by Fabian Measures and the last "Endless Garden" by Unheard Music Concepts

#### Scenario: Credits unavailable
- **WHEN** the credits file cannot be read
- **THEN** the About panel shows no Music section and the rest of the panel is unchanged
