# Proposal

## Why

Leak Out is English only, so players whose device runs another language still get English text. Play Console can now translate app strings automatically with Gemini, at no cost: every bundle uploaded to a draft release gets its `strings.xml` translated and built into the bundle Play serves. We want to use that service instead of keeping translations in the repository. To get there, the app's text has to be fully translatable and the layouts and release flow have to cope with translated text. Tracked in [#72](https://github.com/hexmasternl/leak-out/issues/72).

## What Changes

- Every player-facing and screen-reader text in release builds comes from `res/values/strings.xml`. The playing field's English cell descriptions ("Empty", "Placed X, filled", "Mandatory X, used", ...) move to string resources and use the existing spoken section names (`game_section_*`).
- Strings that must not be translated are marked `translatable="false"`. Every string with placeholders uses positional arguments and has a translator comment, and counts use `<plurals>`.
- Lint `HardcodedText` becomes an error for the `app` module.
- Text shown in capitals is capitalised with the current locale instead of the locale-independent default.
- Buttons, dialogs, the HUD and the result dialogs fit text up to 40% longer than English at 640 × 360, checked with the `en-XA` pseudo-locale.
- An `android:localeConfig` lists English plus the enabled languages, so Android 13+ offers the app in its per-app language settings.
- The first languages are Latin-script only (Dutch, German, French, Spanish, Italian, Portuguese (Brazil)), because the bundled fonts only cover Latin.
- The release flow makes sure Play's translation is applied to the uploaded bundle. `docs/deployment.md` documents turning on automatic app-strings translation, the chosen languages, which strings are excluded, and translating the Play Games achievement texts.
- Out of scope: the debug-only level designer and its `LevelValidator` messages, and level names in `assets/levels/*.json`, which players never see. Both stay English.

## Capabilities

### New Capabilities

- `app-localization`: how the app's text is made translatable, which languages the app announces, how text is formatted for the current locale, and how layouts cope with translated text.

### Modified Capabilities

(none: existing specs quote the English text, which stays the base language)

## Impact

- `app/src/main/res/values/strings.xml`: new strings for the cell descriptions, `translatable="false"` markers, translator comments.
- New `app/src/main/res/xml/locales_config.xml` and `android:localeConfig` in `AndroidManifest.xml`. A `res/resources.properties` sets `unqualifiedResLocale=en-US`.
- `app/build.gradle.kts`: lint `HardcodedText` as error, `generateLocaleConfig` left off; `pseudoLocalesEnabled` in debug.
- `PlayingField.kt` (cell descriptions) and every `.uppercase()` on displayed text.
- Layout fixes where pseudo-locale text clips.
- `.github/workflows/deploy-play-internal.yml` (release status, if needed) and `docs/deployment.md`.
- No new dependencies. Translations live in Play Console, not in the repository.
