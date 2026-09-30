package nl.hexmaster.leakout.history

/**
 * What the player achieved on one finished level: the best score and the stars earned with that
 * score. A level has a history record exactly when it has been finished.
 */
data class LevelHistory(val levelId: Int, val highscore: Int, val stars: Int)

/** What recording a win did to the history, so the level complete dialog can show a new best. */
sealed interface RecordResult {
    /** The level had no record; one was created. */
    data object FirstCompletion : RecordResult

    /** The score beat [previous], the highscore it replaced together with its stars. */
    data class NewHighscore(val previous: Int) : RecordResult

    /** The score did not beat the stored highscore; the record is unchanged. */
    data object NoImprovement : RecordResult
}

/**
 * The recording rule, shared by every repository so they cannot disagree. Keep the best: a first
 * completion creates the record, a strictly higher score replaces score and stars together, and
 * anything else leaves the record as it is.
 */
object LevelHistoryRules {
    const val MIN_STARS = 1
    const val MAX_STARS = 3

    /** What recording [score] and [stars] on [levelId] over [current] does. */
    data class Recording(val result: RecordResult, val record: LevelHistory?)

    /** Rejects a negative [score] and [stars] outside 1-3: a finished level earns at least one star. */
    fun validate(score: Int, stars: Int) {
        require(score >= 0) { "A score cannot be negative, was $score" }
        require(stars in MIN_STARS..MAX_STARS) { "Stars must be $MIN_STARS-$MAX_STARS, were $stars" }
    }

    /**
     * The result of recording [score] and [stars] on [levelId], where [current] is the stored
     * record. [Recording.record] is the row to write, null when nothing changes.
     */
    fun record(current: LevelHistory?, levelId: Int, score: Int, stars: Int): Recording {
        validate(score, stars)
        val next = LevelHistory(levelId, score, stars)
        return when {
            current == null -> Recording(RecordResult.FirstCompletion, next)
            score > current.highscore -> Recording(RecordResult.NewHighscore(current.highscore), next)
            else -> Recording(RecordResult.NoImprovement, null)
        }
    }
}
