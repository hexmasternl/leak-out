package nl.hexmaster.leakout.ui.game

import androidx.annotation.VisibleForTesting
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
import nl.hexmaster.leakout.core.route.Route
import nl.hexmaster.leakout.core.route.RouteTracer
import nl.hexmaster.leakout.core.score.ScoreSheet
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.core.stack.WeightedPool

/**
 * The single owner of a running level: stack, board, replace lock, score, flow speed, game clock,
 * the paused state and fast forward. The future `GameEngine` takes over its insides; the HUD, the
 * field and the pause dialog only depend on the values and actions here.
 *
 * Contract for the flow: the countdown and the liquid head run on `dtMs * flowTimeScale`
 * (`Waiting.remainingMs -= flowDtMs`, `progress += flowDtMs / 1000 * …`), and the flow step loops
 * while a whole cell is crossed, because at 10x one frame can cross more than one. Everything else
 * runs on `dtMs`: [elapsedMs], the replace lock and the points popups.
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

    /** The points of this attempt and how often the liquid has left each tile. */
    var sheet: ScoreSheet by mutableStateOf(ScoreSheet())
        @VisibleForTesting internal set

    /** Points for this attempt, always [ScoreSheet.total] of [sheet]. */
    val score: Int get() = sheet.total

    /** The points popups that are still shown, oldest first. */
    var popups: List<PointsPopup> by mutableStateOf(emptyList())
        private set

    private var nextPopupId = 0L

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

    /** Whether the placed sections connect the start to the finish. Recomputed after every placement. */
    var routeComplete: Boolean by mutableStateOf(false)
        private set

    /** Once on, it stays on until a restart, and placing is locked. */
    var fastForward: Boolean by mutableStateOf(false)
        private set

    /** How much faster than real time the flow runs: [FAST_FORWARD_FACTOR] with fast forward, else 1. */
    val flowTimeScale: Int get() = if (fastForward) FAST_FORWARD_FACTOR else 1

    /** Whether the fast forward button is shown: when it can be turned on, and while it is on. */
    val showFastForward: Boolean get() = routeComplete || fastForward

    /** Turns fast forward on when the route is complete and the game runs. Otherwise does nothing. */
    fun startFastForward() {
        if (routeComplete && !paused && !fastForward) fastForward = true
    }

    /** Pauses the game and opens the pause menu. Pausing twice is the same as once. */
    fun pause() {
        paused = true
        pauseMenuOpen = true
    }

    fun resume() {
        paused = false
        pauseMenuOpen = false
    }

    /**
     * Starts the level again: new stack, empty board, no lock, no route and no fast forward, score 0
     * and no popups, clock and speed reset, running.
     */
    fun restart() {
        stack = PipeStack.create(level.stack, random)
        board = Board.from(level)
        replaceLockUntilMs = 0L
        routeComplete = false
        fastForward = false
        attempt += 1
        sheet = ScoreSheet()
        popups = emptyList()
        flowSpeed = level.flow.cellsPerSecond
        elapsedMs = 0L
        resume()
    }

    /** Moves the game clock by [dtMs] and drops the popups whose time is over. Does nothing while paused. */
    fun tick(dtMs: Long) {
        if (paused || dtMs <= 0) return
        elapsedMs += dtMs
        if (popups.any { it.ageAt(elapsedMs) >= PopupTiming.TOTAL_MS }) {
            popups = popups.filter { it.ageAt(elapsedMs) < PopupTiming.TOTAL_MS }
        }
    }

    /**
     * The liquid has left the tile [at]: awards the points for the section there and shows them on
     * the tile. Refused while paused. Every cell that is not a placed section (start, finish, empty,
     * blocked) is ignored, so the flow can call this for every cell it leaves.
     *
     * Contract for the flow: call it exactly once each time the liquid head leaves a cell, after the
     * cell's channel is marked as filled and before the next cell is checked for a leak or a win.
     * That way the last section before a leak or before the finish still scores, and the second
     * exit from a cross counts as its second pass.
     */
    fun liquidExited(at: Position) {
        if (paused) return
        val pipe = board[at] as? Cell.Pipe ?: return
        val award = sheet.award(at, pipe.section)
        sheet = award.sheet
        popups = popups + PointsPopup(nextPopupId++, at, award.points, startedAtMs = elapsedMs)
    }

    /** Whether the next section can be dropped on [at] now: the same check [place] runs. */
    fun canPlace(at: Position): Boolean = check(at).accepted

    /**
     * Puts [section] on [at] and draws the next stack section. Refused while paused, while fast
     * forward is on, or when the placement rule rejects the tile. A replacement starts the level's
     * replace lock. Afterwards the route is checked again.
     */
    fun place(section: PipeSection, at: Position): Boolean {
        val result = check(at)
        if (!result.accepted) return false
        board = board.with(at, Cell.Pipe(section))
        stack = stack.afterPlacement(pool, random)
        if (result == PlacementCheck.Replace) replaceLockUntilMs = elapsedMs + level.rules.replacePenaltyMs
        routeComplete = RouteTracer.trace(board, level) is Route.Complete
        return true
    }

    /** Marks the section on [at] as entered by liquid. The flow will do this; tests use it now. */
    fun markLiquidEntered(at: Position) {
        board = board.markLiquidEntered(at)
    }

    // Pause and fast forward are session state, not board rules, so they are checked here.
    private fun check(at: Position): PlacementCheck =
        if (paused || fastForward) {
            PlacementCheck.Rejected(PlacementCheck.Reason.LOCKED)
        } else {
            Placement.check(board, level.rules, at, elapsedMs, replaceLockUntilMs)
        }

    companion object {
        /** How many times as fast the flow runs with fast forward on. */
        const val FAST_FORWARD_FACTOR = 10
    }
}
