## Context

Levels live as JSON files under `app/src/main/assets/levels/`, parsed by `LevelParser` and shape-checked by `LevelValidator` in `core`. `LevelValidator`'s own doc comment states it deliberately "does not check that a level is solvable or that its star thresholds can be reached." `BundledLevelsTest` (in `core/src/test/.../level/`) already parses and validates every bundled file via a parameterized JUnit 5 test driven by a `leakout.levelsDir` system property pointing at the assets folder. There is no gameplay/simulation engine yet (no pipe-placement or flow-simulation code exists in `app` or `core`); the puzzle mechanics described in `docs/project-plan.md` (start/finish, pipe ports and channels, blocked cells) are still only a specification.

Because there is no seed for the stack and straight (`H`, `V`) and every corner (`LT`, `LB`, `RT`, `RB`) section must have a weight of at least 1 (enforced by `LevelValidator`), any level where a *simple orthogonal path* of unblocked cells connects the start's facing cell to the finish's facing cell is guaranteed constructible by the player: a long enough sequence of straights and corners can always be drawn and — with `rules.allowReplace` (default `true`), or simply by waiting out an unwanted draw — placed in the right order. Cross (`X`) and reservoir (`RES`) sections are optional extras that let paths overlap or slow the flow; they are never required to make a path exist, only to make one denser. So level solvability reduces to a structural graph question: does an unobstructed orthogonal path exist between the two facing cells? This is decidable by grid search and does not require simulating the stack, timing, or flow.

## Goals / Non-Goals

**Goals:**
- Provide a pure, deterministic `LevelSolver` in `core` that answers "can this level's start reach its finish?" using only the level's grid, blocked cells, and endpoints.
- Provide a headless, reusable test utility that runs this solver over a directory of level files and fails with a clear per-level message when any level is unsolvable.
- Wire this utility into the existing bundled-levels test flow so `./gradlew :core:test` catches an unsolvable shipped level automatically, the same way it already catches a parse or validation failure.
- Keep the solver fast and side-effect free so it can run in CI for dozens or hundreds of levels without noticeable cost.

**Non-Goals:**
- Simulating the actual stack draw order, `allowReplace` mechanics, placement timing, or flow speed. The solver checks structural reachability, not "can a player win within the time limit."
- Verifying that `stars.one` / `two` / three thresholds are reachable (explicitly out of scope per `LevelValidator`'s doc comment, and unchanged by this proposal).
- Building the real gameplay/pipe-placement engine described in `docs/project-plan.md`. That is a separate, much larger effort; this solver is a standalone analysis tool over level data only.
- Handling levels that rely on `X` (cross) or `RES` (reservoir) sections as the *only* way to connect start to finish. The solver treats every non-blocked, in-grid cell as traversable by a straight/corner section; it does not need to model cross/reservoir specifically because they only add capability, never remove it.

## Decisions

### Decision: Model solvability as unweighted grid-graph reachability (BFS)
Treat every non-blocked cell inside the grid as a node, with edges to its four orthogonal neighbours (also non-blocked, in-grid). A level is solvable iff a path exists from `start.facingCell` to `finish.facingCell` in this graph, using the graph itself as both start and end node (which are themselves ordinary path cells, not special-cased) so that a length-0 path is valid when the two facing cells are the same.

Rationale: this exactly matches the guarantee analyzed in **Context** — any such path can always be paved with mandatory straight/corner sections regardless of stack randomness. BFS is O(rows × columns), trivially fast for the grid sizes allowed by `LevelValidator` (4–50 per side), and needs no new dependencies.

Alternatives considered:
- **Full stack/flow simulation** (placing sections in an order a real stack could produce, timing flow ticks): far more complex, requires building gameplay pieces that don't exist yet, and would make the solver non-deterministic or need seeded randomness — rejected as disproportionate to "does a route exist."
- **A\* instead of BFS**: no benefit without a meaningful cost function; BFS already gives shortest path length for free if ever needed for diagnostics.

### Decision: Solver returns a rich result, not just a boolean
`LevelSolver.solve(level: Level): SolverResult` where `SolverResult` is a sealed type with `Solvable(path: List<Position>)` and `Unsolvable(reason: String)` (or equivalent). The reason distinguishes at minimum: start's facing cell is blocked/out of grid (already caught by `LevelValidator`, but the solver should not crash on it), finish's facing cell is unreachable, or (defensively) start/finish facing cells identical-but-blocked edge cases.

Rationale: the proposal requires "report... why no route could be found" so a test failure is actionable without attaching a debugger. Returning structured data keeps the message-formatting in the test layer, not the solver.

Alternatives considered:
- **Boolean-only return**: simplest, but fails the "actionable failure message" requirement from the proposal; rejected.
- **Throwing an exception for unsolvable**: would conflate "level is unsolvable" (a legitimate, expected result the caller must handle) with real programming errors — rejected in favor of a normal return value.

### Decision: Place the solver and its test in `core`, alongside `LevelValidator`/`BundledLevelsTest`
`LevelSolver` goes in `core/src/main/kotlin/nl/hexmaster/leakout/core/level/`; the reusable test entry point goes in `core/src/test/kotlin/nl/hexmaster/leakout/core/level/`, following the exact pattern of `BundledLevelsTest` (JUnit 5 `@ParameterizedTest` + `@MethodSource`, reading `leakout.levelsDir` from a system property, `File.listFiles` filtered by the `level_\d{3}\.json` naming convention).

Rationale: `core` is pure Kotlin/JVM with no Android dependency, already hosts all level-data logic and its tests, and already has the `leakout.levelsDir` wiring from `build.gradle.kts` that points tests at `app/src/main/assets/levels`. Reusing this wiring means zero new Gradle configuration for the bundled-level case, and the same directory-driven design lets any other directory of `level_NNN.json` files (e.g., a future generated batch) be pointed at the same test via the same system property mechanism.

Alternatives considered:
- **New Gradle module** (e.g. `tools` or `level-tester`): unnecessary ceremony for a single object and a test class; rejected to keep the module graph unchanged.
- **Put it in `app`**: would pull level-solvability logic into the Android module and complicate running it in plain JVM CI steps — rejected; `core` is the correct home for level-data-only logic.

### Decision: Fail per-level, not just "some level failed"
Use `@ParameterizedTest(name = "{0}")` with `@MethodSource` over level file names (mirroring `BundledLevelsTest`), asserting each level individually is solvable. A single unsolvable level produces one clearly named failing test case; the rest still report as passing.

Rationale: matches the proposal's requirement that "the tests should fail if either one of the levels is not solvable" — a single aggregate assertion (e.g., asserting an empty list of unsolvable-level names) would satisfy the literal requirement but gives a worse failure message (which level, why) than one test per level. Parameterized-per-file is already the established idiom in this codebase (`BundledLevelsTest`).

## Risks / Trade-offs

- **[Risk]** A future level intentionally requires a cross (`X`) section to be the *only* connective route (e.g., two straight paths that must overlap at one cell) → **Mitigation**: out of scope per Non-Goals; the grid-graph model already treats such a cell as traversable by the plain path (cross is a superset capability), so this only causes false negatives if a level's *only* route requires two channels to cross at the same cell in different directions simultaneously, which the BFS visits as one shared node — actual behavior matches the game rule (a cross cell can carry both channels), so no false negative arises in practice; documented here for future readers.
- **[Risk]** Solver says "solvable" but the real stack/timing rules make a specific level practically very hard to complete (e.g., `initialDelayMs` too short for the path length) → **Mitigation**: explicitly non-goal; this proposal only guards against structurally impossible levels, not difficulty tuning. A future change can add a separate "reachable within time budget" check if needed.
- **[Risk]** Adding a new required passing test could break CI if any of the 24 currently-bundled levels turn out unsolvable → **Mitigation**: run the new test suite against the current bundled levels before merging; fix any level found unsolvable as part of this change's task list (or file a fast-follow if a fix needs design input), so CI is never left red.

