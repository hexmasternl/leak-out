## 1. Dependencies and database setup

- [ ] 1.1 Add Room (runtime, ktx, compiler via KSP) and Room testing to the version catalog and app module
- [ ] 1.2 Enable Room schema export to a `schemas/` directory and commit it
- [ ] 1.3 Create `LevelHistoryEntity` (`level_history`: `level_id` PK, `highscore`, `stars`)
- [ ] 1.4 Create `LevelHistoryDao` with get, observe, observeAll, insert and update queries
- [ ] 1.5 Create `LeakOutDatabase` (version 1) and provide a single instance to the app

## 2. Domain model and repository

- [ ] 2.1 Add the `LevelHistory` domain model and `RecordResult` sealed type (`FirstCompletion`, `NewHighscore`, `NoImprovement`)
- [ ] 2.2 Define the `LevelHistoryRepository` interface: `recordCompletion`, `get`, `observe`, `observeAll`
- [ ] 2.3 Implement the Room-backed repository with input validation (score >= 0, stars 1-3)
- [ ] 2.4 Implement `recordCompletion` inside a single database transaction with the keep-the-best rule (strictly higher score replaces score and stars together)
- [ ] 2.5 Add a fake in-memory repository for use in UI and engine tests

## 3. Tests

- [ ] 3.1 Unit test the recording rule: first completion, better score, lower score, equal score
- [ ] 3.2 Unit test validation: negative score, stars 0 and 4 leave history unchanged
- [ ] 3.3 Instrumented DAO test on an in-memory Room database: one row per level, read one, read all, observers notified
- [ ] 3.4 Test that a leak result never reaches the repository (recording is only called on a win)
- [ ] 3.5 Test persistence across database re-open using a file-based test database

## 4. Integration

- [ ] 4.1 Call `recordCompletion` from the win path with the game score and stars derived from the level's star thresholds
- [ ] 4.2 Expose the `RecordResult` to the level-complete overlay so it can show "new best"
- [ ] 4.3 Make history available to level select for completed status, highscore and stars
- [ ] 4.4 Update `docs/project-plan.md` section 2.10 to say per-level progress uses Room and DataStore holds settings only
