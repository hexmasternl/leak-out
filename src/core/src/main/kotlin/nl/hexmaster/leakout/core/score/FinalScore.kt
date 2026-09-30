package nl.hexmaster.leakout.core.score

import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.level.Position

/**
 * The score of a won attempt: the [earned] points of the flow, the placed sections the liquid never
 * entered ([unusedSections], in row-major order) and the [total] after their penalty, never below 0.
 * The stars come from the level's thresholds applied to [total], the same rule the HUD uses.
 */
data class FinalScore(val earned: Int, val unusedSections: List<Position>, val total: Int) {
    val unusedCount: Int get() = unusedSections.size

    val penalty: Int get() = unusedCount * Points.UNUSED_SECTION_PENALTY

    companion object {
        /**
         * The final score for [earned] points on [board]. A section counts as used once the liquid
         * entered it, so a cross passed through one channel only is not unused.
         */
        fun of(earned: Int, board: Board): FinalScore {
            val unused = board.pipes
                .filterValues { !it.liquidEntered }
                .keys
                .sortedWith(compareBy(Position::y, Position::x))
            val penalty = unused.size * Points.UNUSED_SECTION_PENALTY
            return FinalScore(earned, unused, (earned - penalty).coerceAtLeast(0))
        }
    }
}
