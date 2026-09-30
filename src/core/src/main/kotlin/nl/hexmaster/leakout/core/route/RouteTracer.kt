package nl.hexmaster.leakout.core.route

import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.exitFor

/** Whether the placed sections connect the start to the finish. */
sealed interface Route {
    /** The route enters the finish through its open side. [cells] are the sections on it, in flow order. */
    data class Complete(val cells: List<Position>) : Route

    /** The route stops at [endsAt] for [reason]. The reason is for tests and debugging only. */
    data class Open(val endsAt: Position, val reason: Reason) : Route

    enum class Reason { EMPTY, BLOCKED, NO_OPENING, WRONG_SIDE, OFF_GRID, LOOP }
}

/**
 * Follows the route the liquid would take from the start. Every entry side of a section leads to
 * exactly one exit side ([exitFor]), so this is a single walk, not a search. Whether the liquid has
 * already entered a section does not change the result.
 */
object RouteTracer {
    fun trace(board: Board, level: Level): Route {
        val cells = mutableListOf<Position>()
        // A channel is a position plus an axis, so a cross can be passed once each way. Channels are
        // reversible and the start cannot be entered, so the walk never comes back to one; the set
        // only guarantees that the walk ends.
        val visited = mutableSetOf<Pair<Position, Boolean>>()
        var current = level.start.position
        var exit = level.start.openSide
        while (true) {
            val next = current.neighbour(exit)
            val entry = exit.opposite()
            if (next !in board.grid) return Route.Open(next, Route.Reason.OFF_GRID)
            when (val cell = board[next]) {
                is Cell.Finish ->
                    return if (entry == cell.openSide) Route.Complete(cells) else Route.Open(next, Route.Reason.WRONG_SIDE)
                Cell.Empty -> return Route.Open(next, Route.Reason.EMPTY)
                Cell.Blocked, is Cell.Start -> return Route.Open(next, Route.Reason.BLOCKED)
                is Cell.Pipe -> {
                    exit = cell.section.exitFor(entry) ?: return Route.Open(next, Route.Reason.NO_OPENING)
                    if (!visited.add(next to entry.isHorizontal)) return Route.Open(next, Route.Reason.LOOP)
                    cells += next
                    current = next
                }
            }
        }
    }

    private val Side.isHorizontal: Boolean get() = this == Side.LEFT || this == Side.RIGHT
}
