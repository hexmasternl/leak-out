package nl.hexmaster.leakout.core.engine

import kotlin.random.Random
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.board.Placement
import nl.hexmaster.leakout.core.board.PlacementCheck
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.route.Route
import nl.hexmaster.leakout.core.route.RouteTracer
import nl.hexmaster.leakout.core.score.ScoreSheet
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.core.stack.WeightedPool

/**
 * The single control system of the game: [handle] turns a state and a command into the next state.
 * A refused command returns the same instance, so "accepted" is `new !== old`. The only impure
 * input is [random], which draws the stack; tests pass a seeded one.
 */
class GameEngine(private val random: Random = Random.Default) {
    /**
     * A fresh attempt at [level]: the level's board, a new stack, the countdown at the start delay,
     * score 0, clock 0, playing. This is the only way a state is built, so a restart cannot forget a field.
     */
    fun start(level: Level, previous: GameState? = null): GameState = GameState(
        attempt = (previous?.attempt ?: 0) + 1,
        level = level,
        board = Board.from(level),
        stack = PipeStack.create(level.stack, random),
        flow = FlowState.Waiting(level.flow.initialDelayMs),
        sheet = ScoreSheet(),
        awards = emptyList(),
        replaceLockUntilMs = 0L,
        clockMs = 0L,
        paused = false,
        fastForward = false,
        routeComplete = false,
        outcome = Outcome.Playing,
        finalScore = null,
        sectionsRemoved = 0,
    )

    fun handle(state: GameState, command: GameCommand): GameState = when (command) {
        is GameCommand.Start -> start(command.level, state)
        GameCommand.Restart -> start(state.level, state)
        GameCommand.Pause -> if (state.outcome is Outcome.Playing && !state.paused) state.copy(paused = true) else state
        GameCommand.Resume -> if (state.paused) state.copy(paused = false) else state
        GameCommand.FastForward ->
            if (state.routeComplete && !state.isStopped && !state.fastForward) state.copy(fastForward = true) else state
        GameCommand.NormalSpeed -> if (state.fastForward && !state.isStopped) state.copy(fastForward = false) else state
        is GameCommand.Place -> place(state, command.at)
        is GameCommand.Tick -> tick(state, command.dtMs)
        is GameCommand.RemoveUnused -> removeUnused(state, command.upTo)
    }

    /** Whether a [GameCommand.Place] on [at] would be accepted now. */
    fun canPlace(state: GameState, at: Position): Boolean = placement(state, at).accepted

    // Shared by canPlace and place, so the hover highlight and the drop cannot disagree. Pause, the
    // outcome and fast forward are attempt state, not board rules, so they are checked here.
    private fun placement(state: GameState, at: Position): PlacementCheck =
        if (state.isStopped || state.fastForward) {
            PlacementCheck.Rejected(PlacementCheck.Reason.LOCKED)
        } else {
            Placement.check(state.board, state.level.rules, at, state.clockMs, state.replaceLockUntilMs)
        }

    private fun place(state: GameState, at: Position): GameState {
        val check = placement(state, at)
        if (!check.accepted) return state
        val board = state.board.with(at, Cell.Pipe(state.stack.next))
        return state.copy(
            board = board,
            stack = state.stack.afterPlacement(WeightedPool(state.level.stack.weights), random),
            replaceLockUntilMs = if (check == PlacementCheck.Replace) {
                state.clockMs + state.level.rules.replacePenaltyMs
            } else {
                state.replaceLockUntilMs
            },
            routeComplete = RouteTracer.trace(board, state.level) is Route.Complete,
        )
    }

    /**
     * Moves the clock by [dtMs], drops old awards and, while playing, runs the flow on the flow time
     * (`dtMs` times [GameState.flowTimeScale]). After a win or a loss only the clock moves, for the end effects.
     */
    private fun tick(state: GameState, dtMs: Long): GameState {
        if (dtMs <= 0 || state.paused) return state
        val clockMs = state.clockMs + dtMs
        val moved = state.copy(clockMs = clockMs, awards = state.awards.filter { it.ageAt(clockMs) < Award.VISIBLE_MS })
        return if (state.outcome is Outcome.Playing) FlowSimulation.advance(moved, dtMs * state.flowTimeScale) else moved
    }

    /**
     * Empties the next unused sections of a won attempt, in the final score's order, until [upTo]
     * are gone. Refused while not won and when [upTo] is not ahead of what is already removed.
     */
    private fun removeUnused(state: GameState, upTo: Int): GameState {
        val score = state.finalScore ?: return state
        if (state.outcome !is Outcome.Won) return state
        val target = upTo.coerceAtMost(score.unusedCount)
        if (target <= state.sectionsRemoved) return state
        val board = score.unusedSections.subList(state.sectionsRemoved, target)
            .fold(state.board) { board, at -> board.with(at, Cell.Empty) }
        return state.copy(board = board, sectionsRemoved = target)
    }
}
