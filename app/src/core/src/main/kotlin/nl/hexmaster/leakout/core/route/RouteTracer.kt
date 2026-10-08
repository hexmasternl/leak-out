package nl.hexmaster.leakout.core.route

import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.flow.FlowStep
import nl.hexmaster.leakout.core.flow.MandatoryProgress
import nl.hexmaster.leakout.core.flow.portalPairUses
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.exitFor

/** Whether the placed sections connect the start to the finish. */
sealed interface Route {
    /**
     * The route enters the finish through its open side. [cells] are the sections on it, in flow
     * order; portals are not sections and are left out.
     */
    data class Complete(val cells: List<Position>) : Route

    /** The route stops at [endsAt] for [reason]. The reason is for tests and debugging only. */
    data class Open(val endsAt: Position, val reason: Reason) : Route

    enum class Reason { EMPTY, BLOCKED, NO_OPENING, WRONG_SIDE, OFF_GRID, LOOP, MANDATORY_MISSED }
}

/**
 * Follows the route the liquid would take from the start. Every entry side of a section leads to
 * exactly one exit side ([exitFor]), and a portal pair leads to its other portal, so this is a
 * single walk, not a search. Whether the liquid has
 * already entered a section does not change the result. A route that reaches the finish without
 * running through every channel of every mandatory section is open at the finish, by the same
 * [MandatoryProgress] rule that decides the win.
 */
object RouteTracer {
    fun trace(board: Board, level: Level): Route {
        val walk = walk(board, level, level.start.position, level.start.openSide, emptySet())
        return walk.open ?: Route.Complete(walk.cells)
    }

    /**
     * The sections a walk entered, in order, and where it stopped. [open] is null when the walk
     * entered the finish and the liquid would win there. [portalPairs] counts the portal pairs the
     * walk passed; their cells are not in [cells].
     */
    internal class Walk(val cells: List<Position>, val open: Route.Open?, val portalPairs: Int = 0)

    /**
     * Walks on from the tile [from], which the liquid leaves through [exit], with the channels in
     * [used] already run through. [trace] and the gap distance both take this walk, and it decides
     * every tile with [FlowStep.next], so it shares one set of leak rules with the flow.
     */
    internal fun walk(board: Board, level: Level, from: Position, exit: Side, used: Set<ChannelUse>): Walk {
        val cells = mutableListOf<Position>()
        var portalPairs = 0
        // A cross has a channel per axis, so it can be passed once each way. Channels are reversible
        // and the start cannot be entered, so the walk never comes back to one; the set guarantees
        // that the walk ends and tells which mandatory channels the route runs through.
        val visited = used.toMutableSet()
        var current = from
        var side = exit
        fun open(at: Position, reason: Route.Reason) = Walk(cells, Route.Open(at, reason), portalPairs)
        while (true) {
            when (val next = FlowStep.next(board, current, side, visited)) {
                is FlowStep.Next.Finish -> return when {
                    MandatoryProgress.unused(level.mandatory, visited).isNotEmpty() ->
                        open(next.at, Route.Reason.MANDATORY_MISSED)
                    else -> Walk(cells, null, portalPairs)
                }
                is FlowStep.Next.Refused -> return open(next.at, reasonFor(next.gap))
                is FlowStep.Next.Section -> {
                    visited += ChannelUse(next.at, next.channel)
                    cells += next.at
                    current = next.at
                    side = next.exit
                }
                is FlowStep.Next.Portal -> {
                    visited += portalPairUses(next.at, next.exitPortal)
                    portalPairs++
                    current = next.exitPortal
                    side = next.exit
                }
            }
        }
    }

    private fun reasonFor(gap: FlowStep.Gap): Route.Reason = when (gap) {
        FlowStep.Gap.OFF_GRID -> Route.Reason.OFF_GRID
        FlowStep.Gap.EMPTY -> Route.Reason.EMPTY
        FlowStep.Gap.BLOCKED -> Route.Reason.BLOCKED
        FlowStep.Gap.NO_OPENING -> Route.Reason.NO_OPENING
        FlowStep.Gap.WRONG_SIDE -> Route.Reason.WRONG_SIDE
        FlowStep.Gap.USED -> Route.Reason.LOOP
    }
}
