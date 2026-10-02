package nl.hexmaster.leakout.core.route

import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.flow.MandatoryProgress
import nl.hexmaster.leakout.core.flow.channelFor
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.exitFor

/** Whether the placed sections connect the start to the finish. */
sealed interface Route {
    /** The route enters the finish through its open side. [cells] are the sections on it, in flow order. */
    data class Complete(val cells: List<Position>) : Route

    /** The route stops at [endsAt] for [reason]. The reason is for tests and debugging only. */
    data class Open(val endsAt: Position, val reason: Reason) : Route

    enum class Reason { EMPTY, BLOCKED, NO_OPENING, WRONG_SIDE, OFF_GRID, LOOP, MANDATORY_MISSED }
}

/**
 * Follows the route the liquid would take from the start. Every entry side of a section leads to
 * exactly one exit side ([exitFor]), so this is a single walk, not a search. Whether the liquid has
 * already entered a section does not change the result. A route that reaches the finish without
 * running through every channel of every mandatory section is open at the finish, by the same
 * [MandatoryProgress] rule that decides the win.
 */
object RouteTracer {
    fun trace(board: Board, level: Level): Route {
        val cells = mutableListOf<Position>()
        // A cross has a channel per axis, so it can be passed once each way. Channels are reversible
        // and the start cannot be entered, so the walk never comes back to one; the set guarantees
        // that the walk ends and tells which mandatory channels the route runs through.
        val visited = mutableSetOf<ChannelUse>()
        var current = level.start.position
        var exit = level.start.openSide
        while (true) {
            val next = current.neighbour(exit)
            val entry = exit.opposite()
            if (next !in board.grid) return Route.Open(next, Route.Reason.OFF_GRID)
            when (val cell = board[next]) {
                is Cell.Finish -> return when {
                    entry != cell.openSide -> Route.Open(next, Route.Reason.WRONG_SIDE)
                    MandatoryProgress.unused(level.mandatory, visited).isNotEmpty() ->
                        Route.Open(next, Route.Reason.MANDATORY_MISSED)
                    else -> Route.Complete(cells)
                }
                Cell.Empty -> return Route.Open(next, Route.Reason.EMPTY)
                Cell.Blocked, is Cell.Start -> return Route.Open(next, Route.Reason.BLOCKED)
                is Cell.Pipe -> {
                    exit = cell.section.exitFor(entry) ?: return Route.Open(next, Route.Reason.NO_OPENING)
                    if (!visited.add(ChannelUse(next, cell.section.channelFor(entry)))) {
                        return Route.Open(next, Route.Reason.LOOP)
                    }
                    cells += next
                    current = next
                }
            }
        }
    }
}
