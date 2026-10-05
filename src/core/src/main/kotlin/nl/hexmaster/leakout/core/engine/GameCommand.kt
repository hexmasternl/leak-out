package nl.hexmaster.leakout.core.engine

import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.Position

/** The only ways to change a [GameState]. The [GameEngine] refuses a command that is not allowed now. */
sealed interface GameCommand {
    /** Starts [level] as a new attempt. */
    data class Start(val level: Level) : GameCommand

    /** Puts the stack's next section on the tile [at]. */
    data class Place(val at: Position) : GameCommand

    /**
     * A tap on the tile [at] while a replacement runs. It places nothing: a tap on the replaced tile
     * is counted in [Replacement.taps], any other is refused.
     */
    data class ReplacementTap(val at: Position) : GameCommand

    data object Pause : GameCommand

    data object Resume : GameCommand

    /** Starts the same level again as a new attempt. */
    data object Restart : GameCommand

    /** Runs the flow [GameState.FAST_FORWARD_FACTOR] times as fast; only on a complete route. */
    data object FastForward : GameCommand

    /** Back to the level's normal speed after [FastForward], which unlocks placing again. */
    data object NormalSpeed : GameCommand

    /**
     * Takes the won attempt's unused sections off the board, in order, until [upTo] of them are gone
     * (at most all of them). Sent in step with the level complete tally; refused unless won.
     */
    data class RemoveUnused(val upTo: Int) : GameCommand

    /** Advances the game clock by [dtMs] milliseconds of real time. */
    data class Tick(val dtMs: Long) : GameCommand
}
