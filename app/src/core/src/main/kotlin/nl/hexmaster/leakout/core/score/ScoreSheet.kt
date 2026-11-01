package nl.hexmaster.leakout.core.score

import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position

/**
 * The score of one attempt: the [total] and how often the liquid has exited each tile. Immutable:
 * [award] returns a new sheet, so a restart is simply `ScoreSheet()`. Leak penalties can take the
 * [total] below 0.
 */
data class ScoreSheet(val total: Int = 0, val passes: Map<Position, Int> = emptyMap()) {
    /**
     * Counts one more exit of the liquid from [section] on [at] and adds its points, or, when the
     * tile is [leaky], takes the leak penalty instead.
     */
    fun award(at: Position, section: PipeSection, leaky: Boolean = false): Award {
        val pass = (passes[at] ?: 0) + 1
        val points = if (leaky) -Points.LEAKY_TILE_PENALTY else Points.forExit(section, pass)
        return Award(ScoreSheet(total + points, passes + (at to pass)), points)
    }
}

/** The result of [ScoreSheet.award]: the new [sheet] and the [points] this exit added, negative for a leak penalty. */
data class Award(val sheet: ScoreSheet, val points: Int)
