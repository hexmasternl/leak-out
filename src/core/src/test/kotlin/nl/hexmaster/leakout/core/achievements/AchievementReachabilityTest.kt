package nl.hexmaster.leakout.core.achievements

import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import nl.hexmaster.leakout.core.engine.FlowState
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Proves every skill and hidden achievement can be earned on a bundled level: each one gets a
 * scripted attempt that is played through the engine with commands only and then evaluated. The
 * stack is forced to deal the section a tile needs, but only sections the level can deal, so a
 * cross is only used on a level that draws crosses.
 */
class AchievementReachabilityTest {
    private val engine = GameEngine(Random(5))

    // --- Scripted attempts ---

    /** Level 1, the shortest route, built during the countdown and left alone. */
    private fun cleanWin(): GameState = play(build(start(1), routeOf(bundled(1), solverPath(bundled(1)))))

    /** Level 1, the cleanest win, sped up once the route is complete. */
    private fun fastWin(): GameState {
        val built = build(start(1), routeOf(bundled(1), solverPath(bundled(1))))
        val fast = engine.handle(built, GameCommand.FastForward)
        assertTrue(fast.fastForward, "fast forward was refused")
        return play(fast)
    }

    /**
     * Level 12 ("Crossroads"), the first level that draws crosses. Three loops in a staircase from
     * the start up to row 4: each runs right through a cross, round a 2 by 2 block and up through the
     * same cross. Then along row 4 and up column 10 to the finish.
     */
    private fun crossroads(): GameState {
        val level = bundled(12)
        fun loop(cross: Position): List<Position> = listOf(
            cross,
            Position(cross.x + 1, cross.y),
            Position(cross.x + 1, cross.y + 1),
            Position(cross.x, cross.y + 1),
            cross,
        )
        val path = loop(Position(1, 7)) +
            listOf(Position(1, 6), Position(2, 6)) + loop(Position(3, 6)) +
            listOf(Position(3, 5), Position(4, 5)) + loop(Position(5, 5)) +
            (5..10).map { Position(it, 4) } + (3 downTo 0).map { Position(10, it) }
        return play(build(start(12), routeOf(level, path)))
    }

    /** Level 24, the largest grid, on the solver's route grown into a long detour over most of the board. */
    private fun longWin(): GameState {
        val level = bundled(24)
        return play(build(start(24), routeOf(level, grow(level, solverPath(level)))))
    }

    /** Level 24 on the long route, with every remaining free cell filled before the flow starts. */
    private fun fullBoardWin(): GameState {
        val level = bundled(24)
        val route = routeOf(level, grow(level, solverPath(level)))
        val onRoute = route.map { it.first }.toSet()
        val spares = freeCells(level).filter { it !in onRoute }.map { it to PipeSection.H }
        val built = build(start(24), route + spares)
        assertIs<FlowState.Waiting>(built.flow)
        return play(built)
    }

    /** Level 1: the last section of the route goes on its tile while the liquid runs through the tile before it. */
    private fun photoFinish(): GameState {
        val route = routeOf(bundled(1), solverPath(bundled(1)))
        var state = build(start(1), route.dropLast(1))
        val beforeLast = route[route.size - 2].first
        while ((state.flow as? FlowState.Flowing)?.position != beforeLast) {
            assertIs<Outcome.Playing>(state.outcome)
            state = engine.handle(state, GameCommand.Tick(50))
        }
        state = build(state, route.takeLast(1))
        assertTrue(state.photoFinish)
        return play(state)
    }

    /** Level 1 with nothing placed: the liquid leaves the start valve into an empty tile. */
    private fun instantFlood(): GameState = play(start(1))

    private val scripts: Map<Achievement, () -> GameState> = mapOf(
        Achievement.ZERO_WASTE to ::cleanWin,
        Achievement.AHEAD_OF_THE_FLOW to ::cleanWin,
        Achievement.MEASURE_TWICE to ::cleanWin,
        Achievement.STEADY_HANDS to ::cleanWin,
        Achievement.FULL_THROTTLE to ::fastWin,
        Achievement.DOUBLE_CROSS to ::crossroads,
        Achievement.CROSSROADS_KING to ::crossroads,
        Achievement.LONG_WAY_ROUND to ::longWin,
        Achievement.BIG_SPENDER to ::longWin,
        Achievement.WALL_TO_WALL to ::fullBoardWin,
        Achievement.PHOTO_FINISH to ::photoFinish,
        Achievement.INSTANT_FLOOD to ::instantFlood,
    )

    @Test
    fun `every skill and hidden achievement has a scripted attempt`() {
        val needed = Achievement.entries.filter { it.group == AchievementGroup.SKILL || it.group == AchievementGroup.HIDDEN }
        assertEquals(needed.toSet(), scripts.keys)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("skillAndHidden")
    fun `a scripted attempt on a bundled level unlocks it`(achievement: Achievement) {
        val end = scripts.getValue(achievement)()
        val summary = AttemptSummary.from(end, RecordOutcome.FIRST_COMPLETION.takeIf { end.outcome is Outcome.Won })
        val unlocked = evaluateAttempt(summary).newlyUnlocked
        assertTrue(achievement in unlocked, "${achievement.key} not unlocked by $summary; unlocked $unlocked")
    }

    @Test
    fun `Crossroads King is reached on a level that can draw crosses`() {
        val end = crossroads()
        assertIs<Outcome.Won>(end.outcome)
        assertTrue(end.level.isAllowed(PipeSection.X))
        assertEquals(3, AttemptSummary.from(end, null).crossesLeftTwice)
    }

    @Test
    fun `Never Give Up is reached after three leaks on level 1`() {
        val leaks = (1..3).map { AttemptSummary.from(instantFlood(), null) }
        val stats = leaks.fold(PlayerStats.NONE) { s, leak -> s.after(leak) }
        val win = AttemptSummary.from(cleanWin(), RecordOutcome.FIRST_COMPLETION)
        assertTrue(Achievement.NEVER_GIVE_UP in evaluateAttempt(win, statsBefore = stats).newlyUnlocked)
    }

    // --- Playing ---

    private fun start(id: Int): GameState = engine.start(bundled(id))

    /**
     * Places [pipes] in order with [GameCommand.Place], forcing the stack to deal each section.
     * Placing takes no game time, so a route built from the start is ready before the flow.
     */
    private fun build(from: GameState, pipes: List<Pair<Position, PipeSection>>): GameState =
        pipes.fold(from) { state, (at, section) ->
            assertTrue(state.level.isAllowed(section), "level ${state.level.id} cannot deal $section")
            val forced = state.copy(stack = state.stack.copy(sections = List(state.stack.size) { section }))
            engine.handle(forced, GameCommand.Place(at)).also { assertTrue(it !== forced, "placing $section on $at was refused") }
        }

    private fun play(state: GameState): GameState {
        var s = state
        repeat(1_000_000) {
            if (s.outcome !is Outcome.Playing) return s
            s = engine.handle(s, GameCommand.Tick(100))
        }
        fail("level ${state.level.id} never ended")
    }

    // --- Routes ---

    private fun solverPath(level: Level): List<Position> = assertIs<SolverResult.Solvable>(LevelSolver.solve(level)).path

    /**
     * The section for every tile of [path], which runs in flow order from the start's facing cell to
     * the finish's facing cell. A tile on the path twice is a cross passed once per axis; it is placed once.
     */
    private fun routeOf(level: Level, path: List<Position>): List<Pair<Position, PipeSection>> {
        val cells = listOf(level.start.position) + path + level.finish.position
        val sides = path.indices.groupBy({ path[it] }, { setOf(sideTowards(cells[it + 1], cells[it]), sideTowards(cells[it + 1], cells[it + 2])) })
        return path.distinct().map { at ->
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

    private fun sideTowards(from: Position, to: Position): Side =
        Side.entries.firstOrNull { from.neighbour(it) == to } ?: fail("$from and $to are not neighbours")

    /**
     * [path] grown by detours until none fits: a step from `a` to `b` becomes `a, a', b', b`, where
     * `a'` and `b'` are the free, unused cells next to them on the same side. The path stays a
     * single line without crossings, so it is still a valid route.
     */
    private fun grow(level: Level, path: List<Position>): List<Position> {
        val free = freeCells(level).toSet()
        val route = path.toMutableList()
        var grown = true
        while (grown) {
            grown = false
            val used = route.toSet()
            for (i in 0 until route.size - 1) {
                val a = route[i]
                val b = route[i + 1]
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

    private fun freeCells(level: Level): List<Position> =
        (0 until level.grid.rows).flatMap { y -> (0 until level.grid.columns).map { x -> Position(x, y) } }
            .filter { !level.isBlocked(it) && it != level.start.position && it != level.finish.position }

    companion object {
        @JvmStatic
        fun skillAndHidden(): List<Achievement> =
            Achievement.entries.filter { it.group == AchievementGroup.SKILL || it.group == AchievementGroup.HIDDEN }

        private val levelsDir: File by lazy {
            File(System.getProperty("leakout.levelsDir") ?: fail("System property leakout.levelsDir is not set"))
        }

        private val cache = mutableMapOf<Int, Level>()

        fun bundled(id: Int): Level = cache.getOrPut(id) {
            val file = File(levelsDir, "level_%03d.json".format(id))
            assertIs<ParseResult.Success>(LevelParser.parse(file.readText()), "${file.name} does not parse").level
        }
    }
}
