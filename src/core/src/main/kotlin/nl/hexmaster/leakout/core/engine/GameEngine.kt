package nl.hexmaster.leakout.core.engine

import kotlin.random.Random
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.board.Placement
import nl.hexmaster.leakout.core.board.PlacementCheck
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.exitFor
import nl.hexmaster.leakout.core.route.Route
import nl.hexmaster.leakout.core.route.RouteTracer
import nl.hexmaster.leakout.core.score.ScoreSheet
import nl.hexmaster.leakout.core.stack.PipeStack

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
        flowPath = emptyList(),
        awards = emptyList(),
        replaceLockUntilMs = 0L,
        replacement = null,
        clockMs = 0L,
        paused = false,
        fastForward = false,
        routeComplete = false,
        outcome = Outcome.Playing,
        finalScore = null,
        sectionsRemoved = 0,
        placements = 0,
        replacements = 0,
        everPaused = false,
        routeCompletedBeforeFlow = false,
        photoFinish = false,
        mostReplacementTaps = 0,
    )

    fun handle(state: GameState, command: GameCommand): GameState = when (command) {
        is GameCommand.Start -> start(command.level, state)
        GameCommand.Restart -> start(state.level, state)
        GameCommand.Pause -> if (state.outcome is Outcome.Playing && !state.paused) state.copy(paused = true, everPaused = true) else state
        GameCommand.Resume -> if (state.paused) state.copy(paused = false) else state
        GameCommand.FastForward ->
            if (state.routeComplete && !state.isStopped && !state.fastForward && state.replacement == null) {
                state.copy(fastForward = true)
            } else {
                state
            }
        GameCommand.NormalSpeed -> if (state.fastForward && !state.isStopped) state.copy(fastForward = false) else state
        is GameCommand.Place -> place(state, command.at)
        is GameCommand.ReplacementTap -> replacementTap(state, command.at)
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

    /**
     * Puts the stack's next section on [at]. A replacement with a penalty empties the tile and starts
     * a [Replacement] that lands the section after the penalty; until then no placement is accepted.
     */
    private fun place(state: GameState, at: Position): GameState {
        val check = placement(state, at)
        if (!check.accepted) return state
        val incoming = state.stack.next
        val replacing = check == PlacementCheck.Replace
        val counted = state.copy(
            stack = state.stack.afterPlacement(random),
            placements = state.placements + 1,
            replacements = state.replacements + if (replacing) 1 else 0,
        )
        val penaltyMs = state.level.rules.replacePenaltyMs
        if (replacing && penaltyMs > 0) {
            val removed = (state.board[at] as Cell.Pipe).section
            val landsAtMs = state.clockMs + penaltyMs
            return withBoard(counted, state.board.with(at, Cell.Empty), at).copy(
                replaceLockUntilMs = landsAtMs,
                replacement = Replacement(at, removed, incoming, state.clockMs, landsAtMs),
            )
        }
        return withBoard(counted, state.board.with(at, Cell.Pipe(incoming)), at)
    }

    /** Counts a tap on the tile of the running replacement. Refused without one, on another tile and while stopped. */
    private fun replacementTap(state: GameState, at: Position): GameState {
        val replacement = state.replacement ?: return state
        if (state.isStopped || replacement.at != at) return state
        val taps = replacement.taps + 1
        return state.copy(replacement = replacement.copy(taps = taps), mostReplacementTaps = maxOf(state.mostReplacementTaps, taps))
    }

    /**
     * [state] with [board], changed on the tile [at], and the route checked again. When the change
     * completes the route, the attempt facts about that moment are recorded: before the flow, or a
     * photo finish on the tile the liquid head is about to enter.
     */
    private fun withBoard(state: GameState, board: Board, at: Position): GameState {
        val complete = RouteTracer.trace(board, state.level) is Route.Complete
        val completedNow = complete && !state.routeComplete
        return state.copy(
            board = board,
            routeComplete = complete,
            routeCompletedBeforeFlow = state.routeCompletedBeforeFlow || (completedNow && state.flow is FlowState.Waiting),
            photoFinish = state.photoFinish || (completedNow && headExitsInto(state, at)),
        )
    }

    /** Whether the liquid head is flowing and its current channel leaves its tile towards [at]. */
    private fun headExitsInto(state: GameState, at: Position): Boolean {
        val flow = state.flow as? FlowState.Flowing ?: return false
        // The start holds no section; the liquid leaves it through its open side.
        val exit = (state.board[flow.position] as? Cell.Pipe)?.section?.exitFor(flow.entry) ?: state.level.start.openSide
        return flow.position.neighbour(exit) == at
    }

    /**
     * Moves the clock by [dtMs]. A replacement that lands within the tick splits it at the landing
     * time, so the liquid meets the tile as it is at that moment whatever the tick length.
     */
    private fun tick(state: GameState, dtMs: Long): GameState {
        if (dtMs <= 0 || state.paused) return state
        val replacement = state.replacement ?: return advance(state, dtMs)
        val toLandingMs = replacement.landsAtMs - state.clockMs
        if (toLandingMs > dtMs) return advance(state, dtMs)
        val landed = land(advance(state, toLandingMs))
        return advance(landed, dtMs - toLandingMs)
    }

    /**
     * Moves the clock by [dtMs], drops old awards and, while playing, runs the flow on the flow time
     * (`dtMs` times [GameState.flowTimeScale]). After a win or a loss only the clock moves, for the
     * end effects, and a replacement in progress is dropped.
     */
    private fun advance(state: GameState, dtMs: Long): GameState {
        if (dtMs <= 0) return state
        val clockMs = state.clockMs + dtMs
        val moved = state.copy(clockMs = clockMs, awards = state.awards.filter { it.ageAt(clockMs) < Award.VISIBLE_MS })
        if (state.outcome !is Outcome.Playing) return moved
        val next = FlowSimulation.advance(moved, dtMs * state.flowTimeScale)
        return if (next.outcome !is Outcome.Playing && next.replacement != null) next.copy(replacement = null) else next
    }

    /** Puts the replacement's section on its tile and ends the replacement. Nothing lands after the attempt ended. */
    private fun land(state: GameState): GameState {
        val replacement = state.replacement ?: return state
        if (state.outcome !is Outcome.Playing || state.board[replacement.at] != Cell.Empty) return state.copy(replacement = null)
        val board = state.board.with(replacement.at, Cell.Pipe(replacement.incoming))
        return withBoard(state.copy(replacement = null), board, replacement.at)
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
