## Why

The release build logs a Kotlin compiler warning in the playing field: `PlayingField.kt:366:76 Unnecessary safe call on a non-null receiver of type 'Landing'.` The compiler already knows `landing` is non-null at that point, so the `?.` is dead code and the warning is noise in every CI build.

Tracked in [#166](https://github.com/hexmasternl/leak-out/issues/166).

## What Changes

- Replace the unnecessary safe call `landing?.at` with `landing.at` in the cell drawing loop of the playing field, where the landing pop is applied.
- No behaviour changes: the section that landed last still pops exactly as before.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is a code-quality fix with no change to observable behaviour, so the change skips spec deltas.

## Impact

- `app/src/main/kotlin/nl/hexmaster/leakout/ui/game/PlayingField.kt`: one expression.
- Build: `:app:compileReleaseKotlin` no longer reports this warning.
- Tests: existing app unit tests and the placement/landing UI tests keep passing.
