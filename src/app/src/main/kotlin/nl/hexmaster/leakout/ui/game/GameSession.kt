package nl.hexmaster.leakout.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.core.stack.WeightedPool

/**
 * The single owner of a running level: stack, placed sections, score, flow speed, game clock and
 * the paused state. The future `GameEngine` takes over its insides; the HUD and pause dialog only
 * depend on the values and actions here.
 */
class GameSession(val level: Level, private val random: Random = Random.Default) {
    private val pool = WeightedPool(level.stack.weights)
    private val placedCells = mutableStateMapOf<Position, PipeSection>()

    var stack: PipeStack by mutableStateOf(PipeStack.create(level.stack, random))
        private set

    val placed: Map<Position, PipeSection> get() = placedCells

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

    /** Starts the level again: new stack, empty board, score 0, clock and speed reset, running. */
    fun restart() {
        stack = PipeStack.create(level.stack, random)
        placedCells.clear()
        score = 0
        flowSpeed = level.flow.cellsPerSecond
        elapsedMs = 0L
        resume()
    }

    fun tick(dtMs: Long) {
        if (!paused && dtMs > 0) elapsedMs += dtMs
    }

    /**
     * Puts [section] on [cell] and draws the next stack section. Refused while paused or when the
     * cell already holds a section; the board decides which cells can hold a section at all.
     */
    fun place(cell: Position, section: PipeSection): Boolean {
        if (paused || cell in placedCells) return false
        placedCells[cell] = section
        stack = stack.afterPlacement(pool, random)
        return true
    }
}
