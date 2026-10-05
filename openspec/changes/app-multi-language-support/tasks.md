# Tasks

## 1. Release flow decision

- [x] 1.1 In Play Console, find out whether automatic app-strings translation is applied to a bundle uploaded through the Publishing API with status `completed` (check one internal release, or Play's documentation). Record the answer in design.md decision 2. Verify: the answer and its source are written down.
- [x] 1.2 If `completed` uploads are not translated, change the `PLAY_RELEASE_STATUS` default in `.github/workflows/deploy-play-internal.yml` to `draft`. Verify: `actionlint` (or a workflow dry run) passes and the step summary shows `draft`.

## 2. Translator-ready strings

- [x] 2.1 Add a JVM test in `app` that parses `res/values/strings.xml` and checks the spec's translator-readiness rules: every placeholder string has a translator comment, strings with more than one placeholder use positional placeholders, and `app_name`, `welcome_title` and `splash_wordmark` are marked not translatable. Verify: the test runs and fails on the current file.
- [x] 2.2 Update `strings.xml`: add translator comments, make placeholders positional, mark brand strings `translatable="false"`, and turn any number-plus-word text into `<plurals>`. Verify: the test from 2.1 passes with `./gradlew :app:testDebugUnitTest`.

## 3. No hard-coded text

- [x] 3.1 Move `Cell.describe` in `ui/game/PlayingField.kt` to string resources (`cell_*` strings using the `game_section_*` names), resolved through a function a JVM test can call. Add unit tests for every cell kind, mandatory used and unused, and filled. Verify: `:app:testDebugUnitTest` passes.
- [x] 3.2 Update the instrumented tests that find cells by content description (`PlayingFieldTest`, `GameScreenTest`, `GameEngineScreenTest`, ...) to the new descriptions. Verify: the affected instrumented tests pass on the emulator. *(Compiled with `:app:compileDebugAndroidTestKotlin`. Not run on a device, at the user's request.)*
- [ ] 3.3 Make lint `HardcodedText` an error in `app/build.gradle.kts`, and add the JVM source scan for `Text("...")` and `contentDescription = "..."` literals outside `@Preview` functions and the `designer` package. Verify: `./gradlew :app:lintDebug :app:testDebugUnitTest` passes, and the scan fails when a literal is added on purpose.

## 4. Locale-aware formatting

- [ ] 4.1 Add a `displayUppercase(locale)` helper and replace every `.uppercase()` on displayed text with it. Add unit tests for Turkish `i` → `İ` and German grouping of 12500 in `HudFormat`. Verify: `:app:testDebugUnitTest` passes and `grep -rn "\.uppercase()" app/src/main` only finds non-display uses.

## 5. Supported languages

- [ ] 5.1 Add a `SupportedLanguages` constant (en, nl, de, fr, es, it, pt-BR), `res/xml/locales_config.xml`, `res/resources.properties` (`unqualifiedResLocale=en-US`) and `android:localeConfig` in the manifest. Add a JVM test that the XML matches the constant. Verify: the test passes and `aapt2 dump xmltree` of the debug APK manifest shows the `localeConfig`.
- [ ] 5.2 Add a font coverage test: both bundled fonts have glyphs for the special characters of every enabled language. Verify: the instrumented test passes on the emulator.

## 6. Layouts fit translated text

- [ ] 6.1 Turn on `pseudoLocalesEnabled` for the debug build type, and add instrumented tests that run the welcome screen, settings, level library, pause dialog, level complete dialog and level lost dialog under `en-XA` at 640 × 360 and assert that key labels have no visual overflow. Verify: the tests run, and their failures list the screens that clip.
- [ ] 6.2 Fix every clipping layout found in 6.1 by wrapping or auto-sizing the text. Verify: the 6.1 tests pass, and the existing result-dialog tests at 130% font still pass.

## 7. Documentation

- [ ] 7.1 Extend `docs/deployment.md` with a "Translations" section: turning on automatic app-strings translation, the enabled languages, unticking brand strings, reviewing in Play's emulator, the release-status decision from 1.1, keeping `SupportedLanguages` and `locales_config.xml` in step, the Latin-script-only limit, and translating the Play Games achievement texts in Play Console. Verify: every step can be followed in Play Console as written.
- [ ] 7.2 Note in `docs/level-format.md` that level names are not translated, because players never see them. Verify: the doc reads correctly.

## 8. Integration

- [ ] 8.1 Run `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug` and the instrumented tests touched in 3.2, 5.2 and 6.1. Verify: all pass.
- [ ] 8.2 After the first release with translation turned on, install from the internal track on a device set to Dutch, and set the app to German in the per-app language setting. Verify: the app shows in Dutch, then in German.
