package nl.hexmaster.leakout.core.engine

import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.flow.portalPairUses
import nl.hexmaster.leakout.core.level.exitFor
import nl.hexmaster.leakout.core.route.RouteTracer

/**
 * How far, in sections, the liquid head is from the **gap**: the point where it would leak if
 * nothing changes. It is derived from the board and the flow, like the other values of [GameState],
 * so it is measured again whenever it is needed instead of being stored.
 */
object GapDistance {
    /**
     * The part of the head's tile still to run plus one for every section or portal pair the liquid
     * would enter before it leaks, walking on by the same rules as the flow ([RouteTracer]). A head
     * inside a portal pair walks on from the exit portal. Null while the flow waits or has ended,
     * once the attempt is won or lost, and when the route ahead wins at the finish. A tile with a
     * running replacement is empty on the board, so it is a gap.
     */
    fun of(state: GameState): Double? {
        if (state.outcome !is Outcome.Playing) return null
        val flow = state.flow as? FlowState.Flowing ?: return null
        var from = flow.position
        var used = flow.used + ChannelUse(flow.position, flow.channel)
        val exit = when (val cell = state.board[flow.position]) {
            is Cell.Pipe -> cell.section.exitFor(flow.entry) ?: return null
            is Cell.Portal -> {
                from = cell.partner
                used = flow.used + portalPairUses(flow.position, cell.partner)
                (state.board[cell.partner] as? Cell.Portal)?.openSide ?: return null
            }
            else -> state.level.start.openSide
        }
        val walk = RouteTracer.walk(state.board, state.level, from, exit, used)
        if (walk.open == null) return null
        return (1.0 - flow.progress).coerceIn(0.0, 1.0) + walk.cells.size + walk.portalPairs
    }
}
