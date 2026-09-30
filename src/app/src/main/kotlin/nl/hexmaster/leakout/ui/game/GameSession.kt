package nl.hexmaster.leakout.ui.game

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.GameEngine
import nl.hexmaster.leakout.core.engine.GameState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.Position

/**
 * Holds the [GameState] of the running level for Compose. It keeps no game data of its own: every
 * change is a command to the [GameEngine], and the views read [state] and the few screen values
 * derived from it here.
 */
class GameSession(level: Level, private val engine: GameEngine = GameEngine()) {
    constructor(level: Level, random: Random) : this(level, GameEngine(random))

    /** The current attempt. Tests may set a scripted state; the game only changes it through [send]. */
    var state: GameState by mutableStateOf(engine.start(level))
        @VisibleForTesting internal set

    /** Sends [command] to the engine. Returns whether it was accepted, that is, whether the state changed. */
    fun send(command: GameCommand): Boolean {
        val next = engine.handle(state, command)
        if (next === state) return false
        state = next
        return true
    }

    /** Whether the next section can be dropped on [at] now: the same check a place command runs. */
    fun canPlace(at: Position): Boolean = engine.canPlace(state, at)

    /** The pause dialog is open exactly while the game is paused during play. */
    val showPauseDialog: Boolean get() = state.paused && state.outcome is Outcome.Playing

    /** Game time since the leak, which drives the leak effect; 0 unless lost. */
    val lossAgeMs: Long get() = (state.outcome as? Outcome.Lost)?.let { state.clockMs - it.atMs } ?: 0L

    /** Whether the level lost dialog is shown: once the leak effect has run for [LeakTiming.EFFECT_MS]. */
    val showLostDialog: Boolean get() = state.outcome is Outcome.Lost && lossAgeMs >= LeakTiming.EFFECT_MS

    /** The fast forward button is shown during play once the route is complete, and while fast forward is on. */
    val showFastForward: Boolean get() = state.outcome is Outcome.Playing && (state.routeComplete || state.fastForward)

    /** What the fast forward button does: fast forward when off, normal speed when on. */
    fun toggleFastForward(): Boolean = send(if (state.fastForward) GameCommand.NormalSpeed else GameCommand.FastForward)

    /**
     * Ends the leak effect at once, so the level lost dialog shows as soon as the player comes back
     * to the app. After a loss a tick only moves the clock, so this ticks through the rest of the effect.
     */
    fun skipLeakEffect() {
        if (state.outcome !is Outcome.Lost) return
        val remaining = LeakTiming.EFFECT_MS - lossAgeMs
        if (remaining > 0) send(GameCommand.Tick(remaining))
    }
}

object LeakTiming {
    /** How long the leak is shown on the field before the level lost dialog appears. */
    const val EFFECT_MS = 1_000L
}
