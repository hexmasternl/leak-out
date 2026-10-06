# Spec Delta

## Purpose

Keeps release builds free of the outdated SDK versions and deprecated platform APIs that the Google Play Console reports, so publishing an update is never held up by a policy warning.

## ADDED Requirements

### Requirement: No outdated fragment library
Release builds SHALL NOT include a version of the AndroidX fragment library that the Play SDK Index reports as outdated, including when the library only arrives as a dependency of another SDK such as Play Games Services. The resolved fragment version SHALL be 1.2.1 or newer.

#### Scenario: Play services asks for an old fragment library
- **WHEN** the release build is assembled while Play Games Services declares `androidx.fragment:fragment` 1.1.0
- **THEN** the bundle contains the newer, explicitly pinned fragment library and not 1.1.0

#### Scenario: Play Console SDK check
- **WHEN** a release built from this change is uploaded to the Play Console
- **THEN** the Play Console no longer reports `androidx.fragment:fragment` as an outdated SDK

#### Scenario: Play Games keeps working
- **WHEN** the player signs in to Play Games and unlocks an achievement on a release build
- **THEN** sign-in and the achievement sync work as before

### Requirement: No deprecated edge-to-edge APIs
Release code SHALL NOT request window display behaviour that Android 15 deprecates for edge-to-edge apps, such as the short-edges display cutout mode, on Android versions where a non-deprecated alternative exists. Full-screen windows that must reach into the display cutout SHALL use the "always" cutout mode on Android 11 and newer.

#### Scenario: Pause dialog on Android 11 or newer
- **WHEN** the pause dialog is open on a device running Android 11 or newer
- **THEN** its window uses the "always" display cutout mode and the backdrop covers the cutout area

#### Scenario: Pause dialog on Android 9 or 10
- **WHEN** the pause dialog is open on a device running Android 9 or 10, where the "always" mode does not exist
- **THEN** its window uses the short-edges cutout mode so the backdrop still covers a cutout on the short edge in landscape

#### Scenario: Play Console edge-to-edge check
- **WHEN** a release built from this change is uploaded to the Play Console
- **THEN** the Play Console no longer reports `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` as a deprecated edge-to-edge API used by the app
