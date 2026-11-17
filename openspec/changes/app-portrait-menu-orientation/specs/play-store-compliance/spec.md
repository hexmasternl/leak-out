## ADDED Requirements

### Requirement: No required screen orientation feature
Release builds SHALL NOT require, explicitly or implicitly, the `android.hardware.screen.landscape` or `android.hardware.screen.portrait` hardware feature. The merged manifest SHALL declare both features with `android:required="false"`, and no activity in a release build SHALL lock its orientation in the manifest.

#### Scenario: Merged release manifest
- **WHEN** the release build's merged manifest is inspected
- **THEN** it declares `android.hardware.screen.landscape` and `android.hardware.screen.portrait` with `android:required="false"`
- **AND** no activity in it has an `android:screenOrientation` attribute

#### Scenario: Play Console pre-review check
- **WHEN** a release built from this change is sent for production review in the Play Console
- **THEN** the Play Console no longer reports that required hardware feature declarations restrict the app's availability
