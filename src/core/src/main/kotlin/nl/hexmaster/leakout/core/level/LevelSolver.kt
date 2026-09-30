package nl.hexmaster.leakout.core.level

/** The outcome of running [LevelSolver] on a level. */
sealed interface SolverResult {
    /**
     * A route exists. [path] runs from the start's facing cell to the finish's facing cell,
     * inclusive, and holds a single cell when the two facing cells are the same.
     */
    data class Solvable(val path: List<Position>) : SolverResult

    /** No route exists. [reason] names the endpoint that could not be connected. */
    data class Unsolvable(val reason: String) : SolverResult
}

/**
 * Decides whether a level can be won: whether pipe sections can connect the start's facing cell to
 * the finish's facing cell over cells that lie inside the grid, are not blocked, and are not the
 * start or finish cell itself.
 *
 * Solvability is a purely structural question. Every straight and corner section has a positive
 * weight (enforced by [LevelValidator]), so any orthogonal route of free cells can always be paved;
 * cross and reservoir sections only add capability and are therefore never required. The solver
 * models this as unweighted reachability on the grid graph and does not simulate the stack, the
 * placement timing or the flow.
 */
object LevelSolver {
    fun solve(level: Level): SolverResult {
        val from = level.start.facingCell
        val to = level.finish.facingCell
        unreachableEndpoint(level, from, to)?.let { return it }
        if (from == to) return SolverResult.Solvable(listOf(from))

        val cameFrom = mutableMapOf(from to from)
        val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            for (side in Side.entries) {
                val next = cell.neighbour(side)
                if (!level.isFree(next) || next in cameFrom) continue
                cameFrom[next] = cell
                if (next == to) return SolverResult.Solvable(pathTo(to, cameFrom))
                queue.addLast(next)
            }
        }
        return SolverResult.Unsolvable(
            "The finish's facing cell $to cannot be reached from the start's facing cell $from: " +
                "blocked cells or the grid's edge seal it off",
        )
    }

    /** The reason a facing cell can never carry a pipe, or `null` when both cells are usable. */
    private fun unreachableEndpoint(level: Level, from: Position, to: Position): SolverResult.Unsolvable? {
        listOf("start" to from, "finish" to to).forEach { (name, cell) ->
            if (!level.isFree(cell)) {
                val why = if (cell in level.grid) "is blocked or holds an endpoint" else "lies outside the ${level.grid} grid"
                return SolverResult.Unsolvable("The $name's facing cell $cell $why, so the $name can never be connected")
            }
        }
        return null
    }

    /** A cell can carry a pipe when it is in the grid, unblocked, and not the start or finish itself. */
    private fun Level.isFree(cell: Position): Boolean =
        cell in grid && !isBlocked(cell) && cell != start.position && cell != finish.position

    private fun pathTo(to: Position, cameFrom: Map<Position, Position>): List<Position> {
        val path = mutableListOf(to)
        var cell = to
        while (cameFrom.getValue(cell) != cell) {
            cell = cameFrom.getValue(cell)
            path.add(cell)
        }
        return path.reversed()
    }
}
