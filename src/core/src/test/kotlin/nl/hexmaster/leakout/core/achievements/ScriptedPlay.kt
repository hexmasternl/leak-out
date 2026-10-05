package nl.hexmaster.leakout.core.achievements

import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.GameEngine
import nl.hexmaster.leakout.core.engine.GameState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelParser
import nl.hexmaster.leakout.core.level.LevelSolver
import nl.hexmaster.leakout.core.level.ParseResult
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.SolverResult

/**
 * Scripted attempts on bundled levels, played through [engine] with commands only. The stack is
 * forced to deal the section a tile needs, but only sections the level can deal.
 */
internal class ScriptedPlay(private val engine: GameEngine) {
    fun start(id: Int): GameState = engine.start(bundled(id))

    /**
     * Places [pipes] in order with [GameCommand.Place], forcing the stack to deal each section.
     * Placing takes no game time, so a route built from the start is ready before the flow.
     */
    fun build(from: GameState, pipes: List<Pair<Position, PipeSection>>): GameState =
        pipes.fold(from) { state, (at, section) ->
            assertTrue(state.level.isAllowed(section), "level ${state.level.id} cannot deal $section")
            val forced = state.copy(stack = state.stack.copy(sections = List(state.stack.size) { section }))
            engine.handle(forced, GameCommand.Place(at)).also { assertTrue(it !== forced, "placing $section on $at was refused") }
        }

    /** Ticks until the attempt ends. */
    fun play(state: GameState): GameState {
        var s = state
        repeat(1_000_000) {
            if (s.outcome !is Outcome.Playing) return s
            s = engine.handle(s, GameCommand.Tick(100))
        }
        fail("level ${state.level.id} never ended")
    }

    fun solverPath(level: Level): List<Position> = assertIs<SolverResult.Solvable>(LevelSolver.solve(level)).path

    /**
     * The section for every tile of [path], which runs in flow order from the start's facing cell to
     * the finish's facing cell. A tile on the path twice is a cross passed once per axis; it is placed
     * once. Mandatory tiles are already on the board, so they are left out.
     */
    fun routeOf(level: Level, path: List<Position>): List<Pair<Position, PipeSection>> {
        val cells = listOf(level.start.position) + path + level.finish.position
        val sides = path.indices.groupBy({ path[it] }, { setOf(sideTowards(cells[it + 1], cells[it]), sideTowards(cells[it + 1], cells[it + 2])) })
        return path.distinct().filter { level.mandatoryAt(it) == null }.map { at ->
            val uses = sides.getValue(at)
            val section = if (uses.size == 2) {
                assertEquals(Side.entries.toSet(), uses[0] + uses[1], "the cross on $at is not passed once per axis")
                PipeSection.X
            } else {
                PipeSection.entries.first { it != PipeSection.X && it != PipeSection.RES && it.openSides == uses.single() }
            }
            at to section
        }
    }

    fun sideTowards(from: Position, to: Position): Side =
        Side.entries.firstOrNull { from.neighbour(it) == to } ?: fail("$from and $to are not neighbours")

    /**
     * [path] grown by detours until none fits: a step from `a` to `b` becomes `a, a', b', b`, where
     * `a'` and `b'` are the free, unused cells next to them on the same side. The path stays a
     * single line without crossings, so it is still a valid route. Steps into or out of a mandatory
     * tile are kept, so the liquid still passes it straight through its channel.
     */
    fun grow(level: Level, path: List<Position>): List<Position> {
        val free = freeCells(level).filter { level.mandatoryAt(it) == null }.toSet()
        val route = path.toMutableList()
        var grown = true
        while (grown) {
            grown = false
            val used = route.toSet()
            for (i in 0 until route.size - 1) {
                val a = route[i]
                val b = route[i + 1]
                if (level.mandatoryAt(a) != null || level.mandatoryAt(b) != null) continue
                val step = sideTowards(a, b)
                val detour = Side.entries.filter { it != step && it != step.opposite() }
                    .map { a.neighbour(it) to b.neighbour(it) }
                    .firstOrNull { (a2, b2) -> a2 in free && b2 in free && a2 !in used && b2 !in used }
                    ?: continue
                route.addAll(i + 1, detour.toList())
                grown = true
                break
            }
        }
        return route
    }

    /**
     * A long route for a level without mandatory sections: the best of [tries] walks from the start's
     * facing cell to the finish's facing cell, each [grow]n afterwards. A walk steps to the free,
     * unused neighbour with the fewest free, unused neighbours of its own (random on a near tie), but
     * only to one from which the finish's facing cell can still be reached, so it hugs walls and
     * sweeps the grid instead of taking the shortest way. Routes are compared by their points: 10
     * per straight, 15 per corner.
     */
    fun longPath(level: Level, random: Random, tries: Int = 30): List<Position> {
        require(level.mandatory.isEmpty()) { "level ${level.id} has mandatory sections" }
        val free = freeCells(level).toSet()
        val first = level.start.position.neighbour(level.start.openSide)
        val last = level.finish.position.neighbour(level.finish.openSide)
        fun open(at: Position, used: Set<Position>) = Side.entries.map { at.neighbour(it) }.filter { it in free && it !in used }
        fun reachable(from: Position, used: Set<Position>): Boolean {
            val seen = mutableSetOf(from)
            val queue = ArrayDeque(listOf(from))
            while (queue.isNotEmpty()) {
                val at = queue.removeFirst()
                if (at == last) return true
                open(at, used).filter { seen.add(it) }.forEach(queue::addLast)
            }
            return false
        }
        fun walk(): List<Position> {
            val path = mutableListOf(first)
            val used = mutableSetOf(first)
            while (path.last() != last) {
                val next = open(path.last(), used).filter { it != last && reachable(it, used + it) }
                    .minByOrNull { open(it, used).size + random.nextDouble(-0.45, 0.45) } ?: last
                path += next
                used += next
            }
            return path
        }
        fun points(path: List<Position>): Int {
            val cells = listOf(level.start.position) + path + level.finish.position
            return (1 until cells.size - 1).sumOf { i ->
                if (cells[i - 1].x == cells[i + 1].x || cells[i - 1].y == cells[i + 1].y) 10 else 15
            }
        }
        return (1..tries).map { grow(level, walk()) }.maxBy(::points)
    }

    /**
     * A route for a level without mandatory sections that runs through [shape] in order: the
     * shortest way from the start's facing cell to the shape's first cell, the shape, and the
     * shortest way on from its last cell to the finish's facing cell, never using a cell twice.
     * Null when a cell of the shape is not free or either end cannot be reached.
     */
    fun routeThrough(level: Level, shape: List<Position>): List<Position>? {
        if (level.mandatory.isNotEmpty()) return null
        val free = freeCells(level).toSet()
        if (!free.containsAll(shape)) return null
        val first = level.start.position.neighbour(level.start.openSide)
        val last = level.finish.position.neighbour(level.finish.openSide)
        val inShape = shape.toSet()
        if ((first in inShape && first != shape.first()) || (last in inShape && last != shape.last())) return null
        val before = shortestPath(free - inShape + shape.first(), first, shape.first()) ?: return null
        val after = shortestPath(free - inShape - before.toSet() + shape.last(), shape.last(), last) ?: return null
        return before + shape.drop(1) + after.drop(1)
    }

    /** The shortest path from [from] to [to], both included, over the cells of [open]; null when there is none. */
    fun shortestPath(open: Set<Position>, from: Position, to: Position): List<Position>? {
        if (from !in open || to !in open) return null
        val cameFrom = mutableMapOf(from to from)
        val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val at = queue.removeFirst()
            if (at == to) break
            for (side in Side.entries) {
                val next = at.neighbour(side)
                if (next in open && next !in cameFrom) {
                    cameFrom[next] = at
                    queue.addLast(next)
                }
            }
        }
        if (to !in cameFrom) return null
        val path = mutableListOf(to)
        while (path.last() != from) path += cameFrom.getValue(path.last())
        return path.reversed()
    }

    fun freeCells(level: Level): List<Position> =
        (0 until level.grid.rows).flatMap { y -> (0 until level.grid.columns).map { x -> Position(x, y) } }
            .filter { !level.isBlocked(it) && it != level.start.position && it != level.finish.position }

    companion object {
        private val levelsDir: File by lazy {
            File(System.getProperty("leakout.levelsDir") ?: fail("System property leakout.levelsDir is not set"))
        }

        private val cache = mutableMapOf<Int, Level>()

        /** The bundled level [id], parsed once. */
        fun bundled(id: Int): Level = cache.getOrPut(id) {
            val file = File(levelsDir, "level_%03d.json".format(id))
            assertIs<ParseResult.Success>(LevelParser.parse(file.readText()), "${file.name} does not parse").level
        }

        /** The ids of every bundled level, in order. */
        fun bundledIds(): List<Int> =
            levelsDir.listFiles { f -> f.name.matches(Regex("level_\\d{3}\\.json")) }.orEmpty()
                .map { it.name.substring(6, 9).toInt() }
                .sorted()
    }
}
