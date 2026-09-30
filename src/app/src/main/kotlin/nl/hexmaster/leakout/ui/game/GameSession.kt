package nl.hexmaster.leakout.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.board.Placement
import nl.hexmaster.leakout.core.board.PlacementCheck
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.core.stack.WeightedPool

/**
 * The single owner of a running level: stack, board, replace lock, score, flow speed, game clock
 * and the paused state. The future `GameEngine` takes over its insides; the HUD, the field and the
 * pause dialog only depend on the values and actions here.
 */
class GameSession(val level: Level, private val random: Random = Random.Default) {
    private val pool = WeightedPool(level.stack.weights)

    var stack: PipeStack by mutableStateOf(PipeStack.create(level.stack, random))
        private set

    /** The start, finish, blocked cells and every placed section of this attempt. */
    var board: Board by mutableStateOf(Board.from(level))
        private set

    /** Game time until which no drop is accepted, set by a replacement. */
    var replaceLockUntilMs: Long by mutableLongStateOf(0L)
        private set

    /** Counts attempts, starting at 1. Screen state that must reset on restart is keyed on it. */
    var attempt: Int by mutableIntStateOf(1)
        private set

    /** Points for this attempt. Scoring rules do not exist yet; the engine will set this. */
    var score: Int by mutableIntStateOf(0)

    /** Current flow speed in cells per second. The engine's speed ramp will set this. */
    var flowSpeed: Double by mutableDoubleStateOf(level.flow.cellsPerSecond)

    /** Game time in milliseconds. Does not move while [paused]. */
    var elapsedMs: Long by mutableLongStateOf(0L)
        private set

    var paused: Boolean by mutableStateOf(false)
        private set

    var pauseMenuOpen: Boolean by mutableStateOf(false)
        private set

    /** Stars earned so far, derived from [score] so it can never disagree with a win. */
    val stars: Int get() = level.stars.starsFor(score)

    /** Pauses the game and opens the pause menu. Pausing twice is the same as once. */
    fun pause() {
        paused = true
        pauseMenuOpen = true
    }

    fun resume() {
        paused = false
        pauseMenuOpen = false
    }

    /** Starts the level again: new stack, empty board, no lock, score 0, clock and speed reset, running. */
    fun restart() {
        stack = PipeStack.create(level.stack, random)
        board = Board.from(level)
        replaceLockUntilMs = 0L
        attempt += 1
        score = 0
        flowSpeed = level.flow.cellsPerSecond
        elapsedMs = 0L
        resume()
    }

    fun tick(dtMs: Long) {
        if (!paused && dtMs > 0) elapsedMs += dtMs
    }

    /** Whether the next section can be dropped on [at] now: the same check [place] runs. */
    fun canPlace(at: Position): Boolean = check(at).accepted

    /**
     * Puts [section] on [at] and draws the next stack section. Refused while paused or when the
     * placement rule rejects the tile. A replacement starts the level's replace lock.
     */
    fun place(section: PipeSection, at: Position): Boolean {
        val result = check(at)
        if (!result.accepted) return false
        board = board.with(at, Cell.Pipe(section))
        stack = stack.afterPlacement(pool, random)
        if (result == PlacementCheck.Replace) replaceLockUntilMs = elapsedMs + level.rules.replacePenaltyMs
        return true
    }

    /** Marks the section on [at] as entered by liquid. The flow will do this; tests use it now. */
    fun markLiquidEntered(at: Position) {
        board = board.markLiquidEntered(at)
    }

    private fun check(at: Position): PlacementCheck =
        if (paused) {
            PlacementCheck.Rejected(PlacementCheck.Reason.LOCKED)
        } else {
            Placement.check(board, level.rules, at, elapsedMs, replaceLockUntilMs)
        }
}
