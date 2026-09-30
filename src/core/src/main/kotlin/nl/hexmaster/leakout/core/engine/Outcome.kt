package nl.hexmaster.leakout.core.engine

import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side

/** How the current attempt stands. Once won or lost, it stays that way until the next attempt. */
sealed interface Outcome {
    data object Playing : Outcome

    /** The liquid entered the finish at game time [atMs]. */
    data class Won(val atMs: Long) : Outcome

    /** The pipe leaked at the tile [at] through [side], at game time [atMs]. */
    data class Lost(val at: Position, val side: Side, val atMs: Long) : Outcome
}
