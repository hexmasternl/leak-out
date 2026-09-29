## Context

Leak Out is an Android app (Kotlin, Jetpack Compose) planned in `docs/project-plan.md`. Level ids and star thresholds come from the `game-level-model` change. The plan stores progress in DataStore; this change instead uses a local database, as requested, because the data is per-level rows that are queried and updated conditionally.

## Goals / Non-Goals

**Goals:**
- Persist, per successfully finished level: level id, personal highscore, stars earned with that highscore.
- Record a result with "keep the best" semantics, atomically.
- Expose reads for level select and the level-complete overlay, as observable streams.
- Keep the recording rule unit-testable without a device.

**Non-Goals:**
- Computing score or stars from a game (the engine and star thresholds do that; this change receives both).
- Unlock logic, level select UI, settings storage (DataStore stays for settings).
- Failed attempts, play counts, timing history, cloud sync, accounts.

## Decisions

**Room over raw SQLite or DataStore.** Room (SQLite) gives compile-time-checked queries, Flow support and migrations. Alternative: DataStore Preferences with per-level keys, as in the plan. Rejected: awkward to list all levels and to update conditionally. Raw SQLiteOpenHelper rejected as boilerplate.

**Schema.** One table `level_history`:

| Column | Type | Notes |
|--------|------|-------|
| `level_id` | INTEGER, primary key | Matches the level model id; no foreign key, levels live in JSON |
| `highscore` | INTEGER NOT NULL | Best score, >= 0 |
| `stars` | INTEGER NOT NULL | 1-3, the stars earned with `highscore` |

A row exists only for finished levels, so "finished" means "row exists". The primary key guarantees one row per level.

**Recording rule.** `LevelHistoryRepository.recordCompletion(levelId, score, stars)` runs in one Room transaction: read the row; if none, insert and report `FirstCompletion`; if `score > highscore`, update score and stars together and report `NewHighscore`; otherwise leave the row and report `NoImprovement`. Ties do not update. Stars are stored with the score rather than maxed separately, so the two never disagree.

**Result type.** `RecordResult` is a sealed type (`FirstCompletion`, `NewHighscore(previous)`, `NoImprovement`) so the level-complete overlay can show "new best" without a second query.

**Reads.** `observe(levelId): Flow<LevelHistory?>`, `observeAll(): Flow<List<LevelHistory>>` and a suspend `get(levelId)`. Compose collects the Flows, so level select refreshes after a win.

**Input validation.** The repository rejects a negative score and stars outside 1-3 with `IllegalArgumentException`; a finished level always earns at least one star. Callers derive stars from the level's thresholds before recording.

**Layering.** `LevelHistory` (domain model) and `LevelHistoryRepository` (interface) in the app's domain layer; `LevelHistoryEntity`, `LevelHistoryDao`, `LeakOutDatabase` in the data layer. Tests use a fake repository for the UI and an in-memory Room database for the DAO.

## Risks / Trade-offs

- [Level ids change or levels are renumbered, orphaning rows] → Ids are treated as permanent in the level model; orphaned rows are simply ignored by the UI.
- [Future schema changes lose progress] → Export the Room schema and require a written migration for every version bump; no destructive fallback.
- [Star thresholds are retuned so stored stars no longer match the score] → Accepted: stored stars record what the player earned at the time.
- [Extra dependency (Room, KSP)] → Standard AndroidX library; build time cost is small.
