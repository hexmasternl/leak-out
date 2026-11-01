## Context

`PlayingField` draws each visible cell and scales the cell that just received a section by the landing pop (`landingPopScale`). The condition is `pop != null && cell is Cell.Pipe && position == landing?.at`. The compiler smart-casts `landing` to non-null there and reports the safe call as unnecessary (warning at `PlayingField.kt:366:76`).

## Goals / Non-Goals

**Goals:**
- Remove the warning without changing how the landing pop looks or when it plays.

**Non-Goals:**
- Fixing other compiler warnings or turning warnings into errors (`allWarningsAsErrors`).
- Refactoring the drawing loop.

## Decisions

- **Drop the `?` rather than restructure.** `landing.at` is the smallest change and keeps the code as readable as it is. An alternative, hoisting `val landingAt = landing?.at` above the loop, also removes the per-cell lookup but is not needed to fix the warning.

## Risks / Trade-offs

- [The smart cast stops holding after a later edit] → the compiler then reports an error on `landing.at`, so the regression cannot slip through silently.

## Open Questions

- Should the build treat Kotlin warnings as errors (or at least fail on new ones) so similar warnings are caught in the PR instead of in a follow-up issue? Left out of scope here.
