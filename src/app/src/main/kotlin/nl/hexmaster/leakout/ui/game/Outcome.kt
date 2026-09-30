package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side

/** How the current attempt stands. */
sealed interface Outcome {
    data object Playing : Outcome

    /** The pipe leaked at the tile [at] through [side], at game time [atMs]. */
    data class Lost(val at: Position, val side: Side, val atMs: Long) : Outcome
}

object LeakTiming {
    /** How long the leak is shown on the field before the level lost dialog appears. */
    const val EFFECT_MS = 1_000L
}
