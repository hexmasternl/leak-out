package nl.hexmaster.leakout.achievements

import nl.hexmaster.leakout.core.achievements.RecordOutcome
import nl.hexmaster.leakout.history.RecordResult

/** The core copy of what recording a win did, for the achievement rules. */
fun RecordResult.toRecordOutcome(): RecordOutcome = when (this) {
    RecordResult.FirstCompletion -> RecordOutcome.FIRST_COMPLETION
    is RecordResult.NewHighscore -> RecordOutcome.NEW_HIGHSCORE
    RecordResult.NoImprovement -> RecordOutcome.NO_IMPROVEMENT
}
