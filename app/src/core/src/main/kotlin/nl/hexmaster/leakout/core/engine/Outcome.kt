package nl.hexmaster.leakout.core.engine

import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side

/** How the current attempt stands. Once won or lost, it stays that way until the next attempt. */
sealed interface Outcome {
    data object Playing : Outcome

    /** The liquid entered the finish at game time [atMs]. */
    data class Won(val atMs: Long) : Outcome

    /**
     * The attempt was lost at game time [atMs]: liquid spurts out of the tile [at] through [side].
     * For a [LossReason.MANDATORY_MISSED] loss that is the finish, which refused the liquid.
     */
    data class Lost(val at: Position, val side: Side, val atMs: Long, val reason: LossReason = LossReason.LEAK) : Outcome
}

/** Why an attempt was lost. */
enum class LossReason {
    /** The liquid left a section into a tile that could not take it. */
    LEAK,

    /** The liquid reached the finish while a mandatory section was still unused. */
    MANDATORY_MISSED,
}
