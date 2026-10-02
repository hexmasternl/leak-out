package nl.hexmaster.leakout.core.engine

import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.flow.FlowStep
import nl.hexmaster.leakout.core.flow.MandatoryProgress
import nl.hexmaster.leakout.core.flow.channelFor
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.exitFor
import nl.hexmaster.leakout.core.score.FinalScore

/**
 * Runs the countdown and moves the liquid head. One call walks from tile border to tile border, so
 * a long tick leaves every tile in order and stops at the exact tile where the level ends.
 */
object FlowSimulation {
    /** Progress closer than this to a border counts as reaching it, so rounding never misses one. */
    private const val BORDER_EPSILON = 1e-9

    /** The liquid head as it enters the start: from the side opposite its open side, on a single channel. */
    fun startFlow(state: GameState): FlowState.Flowing = FlowState.Flowing(
        position = state.level.start.position,
        entry = state.level.start.openSide.opposite(),
        channel = Channel.SINGLE,
        progress = 0.0,
        used = emptySet(),
    )

    /** The speed of the head in [flow]'s tile: [GameState.flowSpeed] times the reservoir factor inside a reservoir. */
    fun speedIn(state: GameState, flow: FlowState.Flowing): Double {
        val inReservoir = (state.board[flow.position] as? Cell.Pipe)?.section == PipeSection.RES
        return state.flowSpeed * if (inReservoir) state.level.flow.reservoirSpeedFactor else 1.0
    }

    /** Advances the countdown and the head by [flowDtMs] of flow time. Time left after a win or a loss is dropped. */
    fun advance(state: GameState, flowDtMs: Long): GameState {
        var current = state
        var remainingMs = flowDtMs.toDouble()
        val waiting = current.flow
        if (waiting is FlowState.Waiting) {
            if (flowDtMs < waiting.remainingMs) return current.copy(flow = FlowState.Waiting(waiting.remainingMs - flowDtMs))
            remainingMs = (flowDtMs - waiting.remainingMs).toDouble()
            current = current.copy(flow = startFlow(current))
        }
        while (true) {
            val flow = current.flow as? FlowState.Flowing ?: return current
            val speed = speedIn(current, flow)
            val toBorder = 1.0 - flow.progress
            val step = remainingMs / 1000.0 * speed
            if (step < toBorder - BORDER_EPSILON) return current.copy(flow = flow.copy(progress = flow.progress + step))
            remainingMs = (remainingMs - toBorder / speed * 1000.0).coerceAtLeast(0.0)
            current = leaveTile(current, flow)
        }
    }

    /**
     * The head leaves its tile: the channel is filled, the section's points are awarded (the start
     * gives none), then [FlowStep] decides between the next tile, the finish and a leak. The finish
     * only wins when every mandatory section is used; otherwise it refuses the liquid and the
     * attempt is lost at the finish's open side.
     */
    private fun leaveTile(state: GameState, flow: FlowState.Flowing): GameState {
        val used = flow.used + ChannelUse(flow.position, flow.channel)
        val pipe = state.board[flow.position] as? Cell.Pipe
        val exit: Side = pipe?.section?.exitFor(flow.entry) ?: state.level.start.openSide
        var next = state
        if (pipe != null) {
            val award = state.sheet.award(flow.position, pipe.section)
            next = next.copy(sheet = award.sheet, awards = next.awards + Award(flow.position, award.points, state.clockMs))
        }
        return when (val result = FlowStep.after(next.board, flow.position, exit, used)) {
            is FlowStep.Result.Enter -> {
                val section = (next.board[result.next] as Cell.Pipe).section
                next.copy(
                    board = next.board.markLiquidEntered(result.next),
                    flow = FlowState.Flowing(result.next, result.entry, section.channelFor(result.entry), 0.0, used),
                )
            }
            FlowStep.Result.Finish -> if (MandatoryProgress.unused(state.level.mandatory, used).isEmpty()) {
                next.copy(
                    flow = FlowState.Ended(used),
                    outcome = Outcome.Won(state.clockMs),
                    finalScore = FinalScore.of(next.sheet.total, next.board),
                )
            } else {
                val finish = state.level.finish
                next.copy(
                    flow = FlowState.Ended(used),
                    outcome = Outcome.Lost(finish.position, finish.openSide, state.clockMs, LossReason.MANDATORY_MISSED),
                )
            }
            is FlowStep.Result.Leak ->
                next.copy(flow = FlowState.Ended(used), outcome = Outcome.Lost(result.at, result.side, state.clockMs))
        }
    }
}
