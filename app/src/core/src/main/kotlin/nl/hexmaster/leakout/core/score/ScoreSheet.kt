package nl.hexmaster.leakout.core.score

import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position

/**
 * The score of one attempt: the [total] and how often the liquid has exited each tile. Immutable:
 * [award] returns a new sheet, so a restart is simply `ScoreSheet()`.
 */
data class ScoreSheet(val total: Int = 0, val passes: Map<Position, Int> = emptyMap()) {
    /** Counts one more exit of the liquid from [section] on [at] and adds its points. */
    fun award(at: Position, section: PipeSection): Award {
        val pass = (passes[at] ?: 0) + 1
        val points = Points.forExit(section, pass)
        return Award(ScoreSheet(total + points, passes + (at to pass)), points)
    }
}

/** The result of [ScoreSheet.award]: the new [sheet] and the [points] this exit added. */
data class Award(val sheet: ScoreSheet, val points: Int)
