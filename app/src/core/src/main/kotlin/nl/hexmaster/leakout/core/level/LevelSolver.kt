package nl.hexmaster.leakout.core.level

/** The outcome of running [LevelSolver] on a level. */
sealed interface SolverResult {
    /**
     * A route exists. [path] runs from the start's facing cell to the finish's facing cell,
     * inclusive, and holds a single cell when the two facing cells are the same. It includes the
     * level's mandatory sections; a mandatory cross appears twice, once per channel. A route
     * through a portal pair holds both portals, in flow order, between the two cells facing them:
     * those are the only consecutive cells of the path that are not neighbours. The path may run over
     * leaky tiles; [leakFree] tells whether some route avoids every one of them.
     */
    data class Solvable(val path: List<Position>, val leakFree: Boolean = true) : SolverResult

    /** No route was found. [reason] names the endpoint or the mandatory section that could not be connected. */
    data class Unsolvable(val reason: String) : SolverResult
}

/**
 * Decides whether a level can be won: whether pipe sections can connect the start's facing cell to
 * the finish's facing cell over cells that lie inside the grid, are not blocked, and are not the
 * start or finish cell itself, running through every channel of every mandatory section.
 *
 * Solvability is a purely structural question. Every straight and corner section has a positive
 * weight (enforced by [LevelValidator]), so any self-avoiding orthogonal route of free cells can
 * always be paved; cross and reservoir sections from the stack only add capability and are
 * therefore never required. The solver does not simulate the stack, the placement timing or the
 * flow.
 *
 * Without mandatory sections this is a breadth-first search for the shortest route. With them,
 * every mandatory channel is a waypoint that must be passed straight through, in one of two
 * directions. The solver tries the waypoints in every order and direction, depth first and nearest
 * first, joining them with shortest paths over cells the route has not used yet. Those greedy
 * shortest paths can block a later one, so a crowded level may be rejected although it could be
 * won; a level that is accepted always has the returned route, which never uses a cell twice
 * except a mandatory cross.
 *
 * A portal pair links the cells its two portals face: a route at one of them may continue at the
 * other, through the pair. Portals cannot carry a pipe themselves, and because a route never uses a
 * cell twice it passes each pair at most once. A mandatory channel may be entered or left through a
 * portal that faces it.
 *
 * Leaky tiles are ordinary free cells for the search. After a level is found solvable, the solver
 * runs once more with every leaky tile treated as blocked to tell whether a leak-free route exists.
 */
object LevelSolver {
    /**
     * The most cells the search may visit before the solver gives up, which bounds the time spent on
     * an unsolvable level with mandatory sections to about a second.
     */
    const val MAX_SEARCH_WORK: Long = 10_000_000

    fun solve(level: Level): SolverResult {
        val result = route(level)
        if (result !is SolverResult.Solvable || level.leaky.isEmpty()) return result
        val withoutLeaky = level.copy(blocked = level.blocked + level.leaky, leaky = emptyList())
        return result.copy(leakFree = route(withoutLeaky) is SolverResult.Solvable)
    }

    private fun route(level: Level): SolverResult {
        unreachableEndpoint(level, level.start.facingCell, level.finish.facingCell)?.let { return it }
        unpassableMandatory(level)?.let { return it }
        return Search(level).run()
    }

    /** The reason a facing cell can never carry a pipe, or `null` when both cells are usable. */
    private fun unreachableEndpoint(level: Level, from: Position, to: Position): SolverResult.Unsolvable? {
        listOf("start" to from, "finish" to to).forEach { (name, cell) ->
            if (!level.isFree(cell)) {
                val why = if (cell in level.grid) "is blocked or holds an endpoint or a portal" else "lies outside the ${level.grid} grid"
                return SolverResult.Unsolvable("The $name's facing cell $cell $why, so the $name can never be connected")
            }
        }
        return null
    }

    /**
     * The reason a mandatory channel can never be passed, or `null`. A channel needs both neighbours
     * on its axis to carry the route: cells that can hold a section, or the start, the finish or a
     * portal when it faces the mandatory section.
     */
    private fun unpassableMandatory(level: Level): SolverResult.Unsolvable? {
        for ((index, mandatory) in level.mandatory.withIndex()) {
            for (axis in axesOf(mandatory.section)) {
                val stuck = axis.firstOrNull { !canAdjoin(level, mandatory.position, it) } ?: continue
                return SolverResult.Unsolvable(
                    "The ${describe(mandatory, index)} can never be passed ${axisName(axis)}: its neighbour " +
                        "${mandatory.position.neighbour(stuck)} on the ${stuck.name.lowercase()} cannot carry a pipe",
                )
            }
        }
        return null
    }

    private fun canAdjoin(level: Level, cell: Position, side: Side): Boolean =
        when (val neighbour = cell.neighbour(side)) {
            level.start.position -> level.start.facingCell == cell
            level.finish.position -> level.finish.facingCell == cell
            else -> level.portalAt(neighbour)?.let { it.facingCell == cell } ?: level.isFree(neighbour)
        }

    /** The channels of a mandatory section, each as the pair of sides it runs between. */
    private fun axesOf(section: PipeSection): List<List<Side>> {
        val horizontal = listOf(Side.LEFT, Side.RIGHT)
        return if (section == PipeSection.X) listOf(horizontal, listOf(Side.TOP, Side.BOTTOM)) else listOf(horizontal)
    }

    private fun axisName(axis: List<Side>): String = if (Side.LEFT in axis) "from left to right" else "from top to bottom"

    private fun describe(mandatory: MandatorySection, index: Int): String =
        "mandatory ${mandatory.section} at ${mandatory.position} (mandatory[$index])"

    /** A cell can carry a pipe when it is in the grid, unblocked, and not the start, the finish or a portal. */
    private fun Level.isFree(cell: Position): Boolean =
        cell in grid && !isBlocked(cell) && cell != start.position && cell != finish.position && portalAt(cell) == null

    /** One mandatory channel to pass: a channel of the level's mandatory section [index] on [axis]. */
    private data class Waypoint(val index: Int, val cell: Position, val axis: List<Side>)

    /** A pass through a portal pair: in at [entryPortal], whose facing cell the route is at, out at [exitPortal] into [to]. */
    private data class Jump(val entryPortal: Position, val exitPortal: Position, val to: Position)

    /**
     * What a breadth-first search reached: every cell mapped to the cell before it, and for a cell
     * reached through a portal pair, that pass.
     */
    private class Reach(val cameFrom: Map<Position, Position>, val jumps: Map<Position, Jump>) {
        val size: Int get() = cameFrom.size

        operator fun contains(cell: Position): Boolean = cell in cameFrom

        /** The cells from the search's origin to [to], both included, with the portals of every pass. */
        fun pathTo(to: Position): List<Position> {
            val path = mutableListOf(to)
            var cell = to
            while (cameFrom.getValue(cell) != cell) {
                jumps[cell]?.let { path += listOf(it.exitPortal, it.entryPortal) }
                cell = cameFrom.getValue(cell)
                path.add(cell)
            }
            return path.reversed()
        }

        companion object {
            val NONE = Reach(emptyMap(), emptyMap())
        }
    }

    /**
     * The depth-first search over waypoint orders and directions. At every step the route so far
     * ends with the liquid about to enter a known cell from a known cell: first the start's facing
     * cell from the start, and last the finish from its facing cell.
     */
    private class Search(private val level: Level) {
        private val waypoints = level.mandatory.withIndex().flatMap { (index, m) ->
            axesOf(m.section).map { Waypoint(index, m.position, it) }
        }
        private val mandatoryCells = level.mandatory.map { it.position }.toSet()

        // Both directions of every portal pair, by the cell the entry portal faces.
        private val jumpsFrom: Map<Position, List<Jump>> = level.portals
            .flatMap { listOf(it.a to it.b, it.b to it.a) }
            .groupBy({ (entry, _) -> entry.facingCell }, { (entry, exit) -> Jump(entry.position, exit.position, exit.facingCell) })
        private var work = 0L

        // The deepest point the search got stuck at, for the reason of an unsolvable level. A null
        // waypoint means the finish could not be reached after every waypoint.
        private var deepest = -1
        private var stuckAt: Waypoint? = null

        fun run(): SolverResult {
            val path = extend(level.start.position, level.start.facingCell, emptySet(), emptySet())
            if (path != null) return SolverResult.Solvable(path)
            val from = level.start.facingCell
            val to = level.finish.facingCell
            val stuck = stuckAt
            return when {
                waypoints.isEmpty() -> SolverResult.Unsolvable(
                    "The finish's facing cell $to cannot be reached from the start's facing cell $from: " +
                        "blocked cells or the grid's edge seal it off",
                )
                stuck == null -> SolverResult.Unsolvable(
                    "The finish's facing cell $to cannot be reached after running through every mandatory section",
                )
                else -> SolverResult.Unsolvable(
                    "No route from the start's facing cell $from runs through the " +
                        "${describe(level.mandatory[stuck.index], stuck.index)} ${axisName(stuck.axis)}",
                )
            }
        }

        /**
         * The rest of the route, entering [next] from [previous], or `null` when it cannot be
         * completed. [used] are the ordinary cells and portals already on the route, [passed] the
         * waypoints. When [next] is a portal facing [previous], the route passes its pair and goes on
         * from the cell the other portal faces.
         */
        private fun extend(previous: Position, next: Position, used: Set<Position>, passed: Set<Waypoint>): List<Position>? {
            if (work > MAX_SEARCH_WORK) return null
            level.portalAt(next)?.let { portal ->
                val exit = level.partnerOf(next) ?: return null
                if (portal.facingCell != previous || next in used) return null
                val rest = extend(exit.position, exit.facingCell, used + next + exit.position, passed) ?: return null
                return listOf(next, exit.position) + rest
            }
            val reach = if (isOpen(next, used)) shortestPaths(next, used) else Reach.NONE
            work += reach.size + used.size + 1

            if (passed.size == waypoints.size) {
                val last = segment(previous, next, level.finish.facingCell, level.finish.position, reach, used)
                if (last == null) noteStuck(passed.size, null)
                return last
            }
            val candidates = waypoints.filter { it !in passed }.flatMap { waypoint ->
                waypoint.axis.mapNotNull { direction ->
                    val entry = waypoint.cell.neighbour(direction.opposite())
                    segment(previous, next, entry, waypoint.cell, reach, used)?.let { Triple(waypoint, direction, it) }
                }
            }
            if (candidates.isEmpty()) noteStuck(passed.size, waypoints.first { it !in passed })
            for ((waypoint, direction, approach) in candidates.sortedBy { it.third.size }) {
                val rest = extend(
                    previous = waypoint.cell,
                    next = waypoint.cell.neighbour(direction),
                    used = used + approach,
                    passed = passed + waypoint,
                ) ?: continue
                return approach + waypoint.cell + rest
            }
            return null
        }

        /**
         * The ordinary cells from [next] to [entry] that the route runs through before it enters
         * [target] from [entry]: empty when [next] already is [target], `null` when it cannot be done.
         * When [entry] is a portal facing [target], the route reaches the cell the other portal of
         * the pair faces and passes the pair, so both portals end the segment.
         */
        private fun segment(
            previous: Position,
            next: Position,
            entry: Position,
            target: Position,
            reach: Reach,
            used: Set<Position>,
        ): List<Position>? {
            if (next == target) return if (previous == entry) emptyList() else null
            if (entry in reach) return reach.pathTo(entry)
            val portal = level.portalAt(entry) ?: return null
            val other = level.partnerOf(entry) ?: return null
            if (portal.facingCell != target || entry in used || other.facingCell !in reach) return null
            return reach.pathTo(other.facingCell) + other.position + entry
        }

        private fun noteStuck(depth: Int, waypoint: Waypoint?) {
            if (depth > deepest) {
                deepest = depth
                stuckAt = waypoint
            }
        }

        /** An ordinary cell the route can still run through: free, not mandatory and not on the route yet. */
        private fun isOpen(cell: Position, used: Set<Position>): Boolean =
            level.isFree(cell) && cell !in mandatoryCells && cell !in used

        /**
         * Breadth-first search from [from] over open cells, stepping to a neighbour or through a
         * portal pair whose portals are not on the route yet. Every cell on a shortest path is
         * reached once, so a path passes each pair at most once.
         */
        private fun shortestPaths(from: Position, used: Set<Position>): Reach {
            val cameFrom = mutableMapOf(from to from)
            val jumps = mutableMapOf<Position, Jump>()
            val queue = ArrayDeque(listOf(from))
            while (queue.isNotEmpty()) {
                val cell = queue.removeFirst()
                for (side in Side.entries) {
                    val neighbour = cell.neighbour(side)
                    if (!isOpen(neighbour, used) || neighbour in cameFrom) continue
                    cameFrom[neighbour] = cell
                    queue.addLast(neighbour)
                }
                for (jump in jumpsFrom[cell].orEmpty()) {
                    if (!isOpen(jump.to, used) || jump.to in cameFrom || jump.entryPortal in used) continue
                    cameFrom[jump.to] = cell
                    jumps[jump.to] = jump
                    queue.addLast(jump.to)
                }
            }
            return Reach(cameFrom, jumps)
        }
    }
}
