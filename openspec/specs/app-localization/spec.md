# app-localization Specification

## Purpose
Makes every text the player sees or hears translatable, so Play Console's automatic string translation can show the app in the player's language, and keeps the app correct and readable in those languages.

## Requirements

### Requirement: Translatable text
Every text a player sees, or that a screen reader reads, in a release build SHALL come from the app's default string resources. English SHALL be the default language. Text that is only reachable in debug builds (the level designer) and level names from the level files MAY stay hard-coded English. A hard-coded player-facing text in the app's release code SHALL fail the build's lint check.

#### Scenario: Cell descriptions are resources
- **WHEN** a screen reader reaches a placed, filled cross section on the playing field
- **THEN** it reads "Placed Cross section, filled", built from string resources for the state and the section's spoken name

#### Scenario: Mandatory cell description
- **WHEN** a screen reader reaches a mandatory reservoir that the liquid has not used yet
- **THEN** it reads "Mandatory Reservoir, not used yet", built from string resources

#### Scenario: Hard-coded text fails lint
- **WHEN** a release-code composable sets a `Text` to a string literal
- **THEN** the app module's lint check reports an error

### Requirement: Strings ready for translators
Strings that must not be translated, such as the app's name and the splash wordmark, SHALL be marked as not translatable. Every string with more than one placeholder SHALL use positional placeholders, and every string with a placeholder SHALL have a comment that says what each placeholder holds. Text that depends on a count SHALL use a plural resource rather than joining a number to a word.

#### Scenario: App name is not translated
- **WHEN** the string resources are inspected
- **THEN** the app name "Leak Out" and the wordmark "LEAK\nOUT" are marked not translatable

#### Scenario: Placeholder strings are documented
- **WHEN** a unit test reads the default string resources
- **THEN** every string with a placeholder has a translator comment and uses positional placeholders when it has more than one

### Requirement: Locale-aware formatting
Numbers SHALL be formatted with the current locale's digits and grouping. Text shown in capitals SHALL be capitalised using the current locale's rules.

#### Scenario: Turkish capitals
- **WHEN** the device language is Turkish and a title containing "i" is shown in capitals
- **THEN** the "i" is capitalised as "İ"

#### Scenario: Grouped score
- **WHEN** the device language is German and the score is 12500
- **THEN** the score is shown as "12.500"

### Requirement: Layouts fit translated text
Buttons, dialogs, the HUD, the settings screen, the level library and the result dialogs SHALL keep all their text readable at 640 × 360 when the text is up to 40% longer than English. Long text SHALL wrap or shrink to fit and SHALL NOT be cut off or overlap other content.

#### Scenario: Pseudo-locale at the smallest size
- **WHEN** the app runs under the `en-XA` pseudo-locale on a 640 × 360 screen
- **THEN** the welcome screen, settings, level library, pause dialog, level complete dialog and level lost dialog show every text without clipping

### Requirement: Supported languages
The app SHALL declare the languages it supports: English plus every language enabled for automatic translation in Play Console. Only languages whose script the bundled fonts can draw SHALL be enabled; the first set is Dutch, German, French, Spanish, Italian and Portuguese (Brazil). On Android 13 and later the declared languages SHALL be offered in the system's per-app language settings. A device language that is not supported SHALL show English.

#### Scenario: Per-app language list
- **WHEN** a player opens the app's language setting in Android 13 or later
- **THEN** English, Dutch, German, French, Spanish, Italian and Portuguese (Brazil) are offered

#### Scenario: Unsupported language
- **WHEN** the device language is Japanese
- **THEN** the app is shown in English

### Requirement: Translations applied on release
Every bundle the release workflow uploads to Play SHALL be uploaded in a way that Play Console's automatic string translation is applied to it before it reaches testers. The deployment documentation SHALL describe how to turn on automatic app-strings translation, which languages are enabled, which strings are excluded from translation, how to review translations, and that the Play Games achievement names and descriptions are translated separately in Play Console.

#### Scenario: Translated internal release
- **WHEN** a push to `main` uploads a new bundle and the release reaches the internal track
- **THEN** a tester whose device language is Dutch sees the app in Dutch
