package nl.hexmaster.leakout.core.score

import nl.hexmaster.leakout.core.level.PipeSection

/** Points for one exit of the liquid from a placed section. */
object Points {
    const val STRAIGHT = 10
    const val CORNER = 15
    const val RESERVOIR = 25
    const val CROSS_FIRST = 10
    const val CROSS_SECOND = 50

    /** Taken off the final score of a won attempt for every placed section the liquid never entered. */
    const val UNUSED_SECTION_PENALTY = 3

    /** Taken off the score, in place of the section's points, each time the liquid exits a section on a leaky tile. */
    const val LEAKY_TILE_PENALTY = 20

    /**
     * The points for the liquid leaving [section] for the [pass]-th time on its tile, starting at 1.
     * Only the cross looks at [pass]: its second exit, through the other channel, is worth more.
     */
    fun forExit(section: PipeSection, pass: Int): Int = when (section) {
        PipeSection.H, PipeSection.V -> STRAIGHT
        PipeSection.LT, PipeSection.LB, PipeSection.RT, PipeSection.RB -> CORNER
        PipeSection.RES -> RESERVOIR
        PipeSection.X -> if (pass >= 2) CROSS_SECOND else CROSS_FIRST
    }
}
