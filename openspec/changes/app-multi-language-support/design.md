# Design

## Context

- The app already reads almost all of its text from `app/src/main/res/values/strings.xml`: 278 strings and 3 plurals, with 15 comments and one `translatable="false"` (`splash_wordmark`). There is no `values-*` folder.
- One release-build text is still hard-coded: `Cell.describe` in `ui/game/PlayingField.kt`, which builds the cells' content descriptions in English from `section.name` (the code `X`, `RES`, ...). The spoken section names already exist as `game_section_*` strings.
- Numbers are already formatted with `NumberFormat.getIntegerInstance(locale)` (`HudFormat`) and dates with a localized `DateTimeFormatter`. Display capitals use Kotlin's `String.uppercase()`, which is locale-independent (about 12 calls).
- The level designer is only built when `BuildConfig.LEVEL_DESIGNER` is true (debug). Level names in `assets/levels/*.json` are never shown to players.
- Fonts: `fredoka.ttf` and `wendy_one_regular.ttf`, both Latin only.
- Play Console's automatic app-strings translation (Gemini) translates the `strings.xml` of every bundle uploaded to a **draft** release and merges the result into that bundle. It overwrites existing translations for the languages you select, and lets you untick strings and edit translations, which carry over to later releases.
- `deploy-play-internal.yml` uploads with `PLAY_RELEASE_STATUS` (default `completed`) to `PLAY_TRACK` (default `internal`).

## Goals / Non-Goals

**Goals:**
- `strings.xml` is the single source; translations never live in the repository.
- Catch regressions automatically: lint for hard-coded text, a unit test for translator-readiness, the pseudo-locale for layout.

**Non-Goals:**
- An in-app language picker. The system's per-app language setting (Android 13+) is enough.
- Non-Latin scripts (Cyrillic, Greek, CJK, Arabic, Hebrew). These need fallback fonts and RTL checks, so they are a follow-up.
- Translating the store listing. That is done in Play Console and needs no code.

## Decisions

### 1. Play Console translation, no committed `values-*` files
Play merges its translations into the uploaded bundle, so the repository only holds English. *Alternatives:* committing Gemini output downloaded from Play (two sources that drift, and Play overwrites them anyway); a third-party service such as Crowdin (an extra tool and account for no gain). Consequence: debug builds and instrumented tests only ever have English. Locale-dependent behaviour is tested with formatting and the pseudo-locale, not with real translations.

### 2. Upload as a draft release, then promote
Play documents translation for bundles in a draft release. The first task checks in Play Console whether a `completed` upload through the Publishing API gets translated as well. If it does, nothing changes. If it doesn't, the workflow default becomes `PLAY_RELEASE_STATUS=draft`, and promoting to internal testing is a manual step in Play Console. Translations are often worth a review anyway. The decision and the steps go in `docs/deployment.md`. *Alternative:* a second workflow job that promotes the draft through the API after a delay. Rejected because how long Play takes to translate isn't documented.

### 3. A handwritten `locales_config.xml`
`android.generateLocaleConfig` derives the list from `values-*` folders, and we have none, so it would only list English. A handwritten `res/xml/locales_config.xml`, together with `res/resources.properties` (`unqualifiedResLocale=en-US`), lists the enabled languages. A JVM unit test reads both the XML and a `SupportedLanguages` constant, so the list is in one place. The docs say to update it whenever languages are enabled in Play Console.

### 4. Cell descriptions from resources
`Cell.describe` becomes a composable-free function that takes a `(Int, args) -> String` resolver (or `Resources`), so a JVM test can check the mapping. The section's name comes from the existing `game_section_*` strings. New strings: `cell_empty`, `cell_blocked`, `cell_start`, `cell_finish`, `cell_placed`, `cell_placed_filled`, `cell_mandatory_used`, `cell_mandatory_unused`, each with `%1$s` for the section name. The English output stays the same apart from the section name ("Placed Cross section" instead of "Placed X"), so the instrumented tests that look up cells by description need updating. That is one change in their lookup helpers.

### 5. Locale-aware capitals in one helper
A `String.displayUppercase(locale)` helper (with a composable `currentLocale()` taken from `LocalConfiguration`) replaces every `.uppercase()` on displayed text. A unit test covers Turkish `i` → `İ` and German `ß`.

### 6. Guard rails
- `lint { error += "HardcodedText" }` in `app/build.gradle.kts`. Previews and debug-only designer code use `@SuppressLint` or a lint baseline entry, so the rule stays strict for release code. Kotlin string literals in Compose `Text(...)` are not caught by `HardcodedText` (an XML-layout check), so a small JVM test over `app/src/main/kotlin` flags `Text("...")` and `contentDescription = "..."` literals outside `@Preview` functions and the `designer` package.
- A JVM test that parses `strings.xml` and checks the translator-readiness rules from the spec (comments on placeholder strings, positional placeholders when there's more than one, the not-translatable list).
- `pseudoLocalesEnabled = true` on the debug build type. Instrumented layout tests run the main screens under `en-XA` at 640 × 360 and assert that no text node is clipped (`TextLayoutResult.hasVisualOverflow`) on the key labels.

## Risks / Trade-offs

- [Gemini mistranslates game terms or loses tone] → Review each language in Play's built-in emulator before promoting. Untick brand terms. Edits carry over to later releases.
- [Translations not applied to `completed` API uploads] → Decision 2 (draft, then promote).
- [The enabled language list drifts from `locales_config.xml`] → One constant plus a test, and a checklist item in `docs/deployment.md`.
- [Longer text breaks compact layouts] → Pseudo-locale tests. Wrap or shrink text with `maxLines` and auto-size where needed, rather than cutting it off.
- [Instrumented tests can't see real translations] → Accepted. The Play preview emulator covers the real text.
- [A missing glyph in an enabled language] → Only Latin-script languages are enabled. A test renders every enabled language's special characters (é, ß, ç, ã, ñ, ĳ) with both fonts and asserts glyph coverage.

## Migration Plan

1. Merge the code changes. English output stays the same apart from the cell descriptions.
2. In Play Console: turn on automatic app-strings translation for the six languages, untick `app_name`, `welcome_title` and the other brand strings, then upload a release.
3. Review the translations in Play's emulator, then promote.
Rollback: turn off automatic translation in Play Console (or deselect languages) and upload again. The code changes are harmless with English only.

## Open Questions

- Does Play translate bundles uploaded with status `completed` through the Publishing API? This is answered by task 1.1. Either way the approach stays the same, only the workflow default changes (decision 2).
