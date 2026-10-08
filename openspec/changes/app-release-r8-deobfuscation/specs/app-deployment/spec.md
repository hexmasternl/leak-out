# Spec Delta

## ADDED Requirements

### Requirement: Release bundle is minified and uploaded with its mapping file
The release build SHALL run R8 code shrinking, obfuscation and resource shrinking. The deployment SHALL upload the R8 mapping file produced by the same build together with the bundle, so the Play Console can deobfuscate crash and ANR stack traces, and SHALL keep the mapping file as a workflow artifact.

#### Scenario: Mapping file uploaded with the bundle
- **WHEN** the deployment uploads a release bundle to the Play Console
- **THEN** the mapping file of that build is attached to the same version code
- **THEN** the Play Console shows no warning about a missing deobfuscation file

#### Scenario: Missing mapping file stops the upload
- **WHEN** the release build produces no mapping file
- **THEN** the deployment fails before anything is uploaded

#### Scenario: Minified build still works
- **WHEN** the minified release build is installed and a level is played, won and recorded in the level history
- **THEN** the app behaves as the unminified build does
