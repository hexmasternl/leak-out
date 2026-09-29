# welcome-screen Specification

## Purpose
TBD - created by archiving change app-welcome-screen. Update Purpose after archive.
## Requirements
### Requirement: Welcome screen is the launch screen
The app SHALL display the welcome screen as the first screen after launch.

#### Scenario: App launch
- **WHEN** the user launches the app
- **THEN** the welcome screen is displayed

### Requirement: Illustrated background
The welcome screen SHALL fill the entire display with a decorative background visual in the Leak Out design-system style (pipes and liquid).

#### Scenario: Background covers the screen
- **WHEN** the welcome screen is displayed on any landscape screen size
- **THEN** the background visual covers the full display without empty bars or stretching artifacts

### Requirement: Centered app title
The welcome screen SHALL show the app title "Leak Out" in large display typography, horizontally centered on the screen.

#### Scenario: Title presentation
- **WHEN** the welcome screen is displayed
- **THEN** the title is rendered large, centered horizontally, in the design system's display font and uppercase style
- **AND** the title is fully visible and not clipped

### Requirement: Menu buttons below the title
The welcome screen SHALL show three buttons labelled "Play", "Settings", and "Exit", positioned below the title, in that order.

#### Scenario: Menu layout
- **WHEN** the welcome screen is displayed
- **THEN** the buttons Play, Settings, and Exit are visible below the title
- **AND** each button has a touch target of at least 48dp

#### Scenario: Button pressed feedback
- **WHEN** the user presses and holds a menu button
- **THEN** the button visibly compresses downward following the design-system pressed state

### Requirement: Play action
Tapping "Play" SHALL signal the host to start the game flow.

#### Scenario: Tap Play
- **WHEN** the user taps "Play"
- **THEN** the play navigation event is emitted exactly once

### Requirement: Settings action
Tapping "Settings" SHALL signal the host to open settings.

#### Scenario: Tap Settings
- **WHEN** the user taps "Settings"
- **THEN** the settings navigation event is emitted exactly once

### Requirement: Exit action
Tapping "Exit" SHALL close the app.

#### Scenario: Tap Exit
- **WHEN** the user taps "Exit"
- **THEN** the app's activities are finished and the app leaves the foreground

