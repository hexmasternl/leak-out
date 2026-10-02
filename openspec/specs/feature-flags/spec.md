# feature-flags Specification

## Purpose
TBD - created by archiving change app-game-level-designer. Update Purpose after archive.

## Requirements

### Requirement: Feature flags are fixed per build type
The app SHALL expose its feature flags from a single `FeatureFlags` source whose values are fixed at build time per build type. The flag `levelDesigner` SHALL be on in debug builds and off in release builds. A flag SHALL NOT be changeable at runtime.

#### Scenario: Debug build
- **WHEN** the debug build runs
- **THEN** `FeatureFlags.levelDesigner` is true

#### Scenario: Release build
- **WHEN** the release build is assembled
- **THEN** its generated build configuration has the level designer flag set to false

### Requirement: A feature that is off is unreachable
When a feature flag is off, the app SHALL NOT show any entry point to that feature and SHALL NOT navigate to it, even if asked to.

#### Scenario: Designer off
- **WHEN** `levelDesigner` is off and the welcome screen is shown
- **THEN** no level designer button exists in the UI tree, and a request to open the designer leaves the current screen unchanged
