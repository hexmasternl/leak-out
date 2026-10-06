# splash-screen Specification

## Purpose
TBD - created by archiving change app-splash-screen. Update Purpose after archive.

## Requirements

### Requirement: System splash uses the deep surface colour
The system splash screen SHALL use `surface.deep` (`#0C1A2A`) as its background and SHALL NOT show a visible icon. This SHALL hold on every supported API level (26 and up).

#### Scenario: Cold start on Android 12 or newer
- **WHEN** the app is cold-started on a device running API 31 or higher
- **THEN** the system splash screen shows a solid `#0C1A2A` background with no visible launcher icon

#### Scenario: Cold start on Android 11 or older
- **WHEN** the app is cold-started on a device running API 26–30
- **THEN** the launch window shows a solid `#0C1A2A` background with no visible icon, and no white or default-coloured window appears

### Requirement: In-app splash shows the two-line wordmark
After the system splash is dismissed, the app SHALL show a splash screen with a `surface.deep` background. It SHALL display the app name as two lines, "LEAK" on the first line and "OUT" on the second. Each line SHALL be horizontally centred, and the two-line block SHALL be vertically centred on the screen.

#### Scenario: Wordmark layout
- **WHEN** the in-app splash screen is displayed on any landscape screen size
- **THEN** "LEAK" appears on the first line and "OUT" directly below it, both centred horizontally, with the block centred vertically

#### Scenario: Seamless handoff from the system splash
- **WHEN** the system splash is dismissed and the in-app splash appears
- **THEN** the background colour stays `#0C1A2A`, with no flash of another colour in between

### Requirement: Wordmark uses the liquid gradient and glow style
The wordmark SHALL be set in Wendy One at no less than the `display.hero` size (56 sp). It SHALL be filled with a vertical gradient running top to bottom through `liquid.glow` (`#9DF3FF`), `liquid.300` (`#5EE7FF`), `liquid.500` (`#19C6F2`) and `liquid.700` (`#0A8DC2`). It SHALL have a soft outer glow in `liquid.500` at about 35–45 % opacity with a blur radius of 24 dp.

#### Scenario: Gradient fill
- **WHEN** the wordmark is rendered
- **THEN** its glyphs are filled with the liquid gradient, lightest (`liquid.glow`) at the top and darkest (`liquid.700`) at the bottom of the text block, rather than a flat colour

#### Scenario: Glow
- **WHEN** the wordmark is rendered
- **THEN** a soft aqua glow surrounds the glyphs, and nothing else on the screen is brighter than the wordmark

#### Scenario: Font scaling does not break layout
- **WHEN** the system font scale is set to 130 %
- **THEN** both lines of the wordmark stay fully visible on a 640×320 dp landscape screen without clipping

### Requirement: Splash always uses the Deep palette
The splash screen SHALL use the Deep palette values whatever the active app theme is.

#### Scenario: Daylight theme active
- **WHEN** the app launches while the Daylight theme is selected
- **THEN** the system and in-app splash screens still show the `#0C1A2A` background and the liquid-gradient wordmark

### Requirement: System bars blend with the splash
While either splash screen is visible, the app SHALL draw edge-to-edge, with the status and navigation bar areas showing `surface.deep` and light (white) system bar icons.

#### Scenario: Bars during splash
- **WHEN** the in-app splash is displayed
- **THEN** no status or navigation bar area shows a colour other than `#0C1A2A`

### Requirement: Splash duration and transition to the welcome screen
The in-app splash SHALL stay visible for at least 1 200 ms from the moment it first appears. It SHALL then cross-fade into the welcome screen over 320 ms. The welcome screen SHALL be the destination, and the splash SHALL NOT come back when the player presses back or rotates the device.

#### Scenario: Normal launch
- **WHEN** the app launches and the in-app splash appears
- **THEN** after at least 1 200 ms the splash cross-fades into the welcome screen

#### Scenario: Back from the welcome screen
- **WHEN** the player presses back on the welcome screen
- **THEN** the app exits and the splash screen is not shown again

#### Scenario: Configuration change
- **WHEN** the device flips between the two landscape orientations after the splash has finished
- **THEN** the welcome screen stays visible and the splash is not replayed

#### Scenario: Hot start
- **WHEN** the app is brought back to the foreground while its activity is still alive
- **THEN** the current screen is shown directly, with no splash

### Requirement: Reduced motion
When the system "remove animations" setting is on, the splash SHALL show a static glow and SHALL switch to the welcome screen without a cross-fade once the minimum duration has passed.

#### Scenario: Remove animations enabled
- **WHEN** the app launches with system animations disabled
- **THEN** the wordmark appears with a static glow, and after at least 1 200 ms the welcome screen replaces it instantly
