## MODIFIED Requirements

### Requirement: Illustrated background
The welcome screen SHALL fill the entire display with a decorative background visual in the Leak Out design-system style (pipes and liquid).

#### Scenario: Background covers the screen
- **WHEN** the welcome screen is displayed on any screen size, in landscape or in portrait
- **THEN** the background visual covers the full display without empty bars or stretching artifacts

## ADDED Requirements

### Requirement: Portrait menu layout
When the welcome screen is displayed in portrait, it SHALL keep the title at the top and SHALL show the menu buttons stacked vertically in their usual order, as one column centred horizontally and aligned to the bottom of the screen, clear of the navigation bar. The landscape layout SHALL stay as it is.

#### Scenario: Portrait layout
- **WHEN** the welcome screen is displayed in portrait
- **THEN** the menu buttons are shown in a single vertical stack in their usual order
- **AND** the stack is centred horizontally and sits at the bottom of the screen, above the navigation bar
- **AND** the title stays fully visible above the stack and every button has a touch target of at least 48dp

#### Scenario: Large font in portrait
- **WHEN** the welcome screen is displayed in portrait on a 360×640 dp screen with the system font scale at 130 %
- **THEN** the title and every button stay fully visible without overlapping or clipping

#### Scenario: Rotating to landscape
- **WHEN** the device is rotated from portrait to landscape on the welcome screen
- **THEN** the welcome screen shows its landscape layout
