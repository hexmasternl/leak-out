package nl.hexmaster.leakout.core.achievements

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
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
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

    // --- Scripted attempts for the later additions ---

    /** Level 24 (16 × 12) on a route that sweeps most of the board: the walls hugged, then grown by detours. */
    private val sweep: List<Pair<Position, PipeSection>> by lazy {
        val level = bundled(24)
        routeOf(level, scripted.longPath(level, Random(24)))
    }

    private fun sweepWin(): GameState = play(build(start(24), sweep))

    /** The sweep of level 24 without its last section, so the liquid leaks just before the finish. */
    private fun sweepLeak(): GameState = play(build(start(24), sweep.dropLast(1)))

    /**
     * The first bundled level from [ids] with a route through the cells [shapeAt] gives for some
     * anchor cell, and that route in flow order.
     */
    private fun firstRouteThrough(ids: Iterable<Int>, shapeAt: (Position) -> List<Position>): Pair<Level, List<Position>> {
        for (id in ids) {
            val level = bundled(id)
            for (anchor in freeCells(level)) {
                val path = scripted.routeThrough(level, shapeAt(anchor)) ?: continue
                return level to path
            }
        }
        fail("no bundled level has room for the shape")
    }

    /** A row of [length] cells, left to right from [anchor]. */
    private fun row(anchor: Position, length: Int): List<Position> = (0 until length).map { Position(anchor.x + it, anchor.y) }

    /** A level with a row of 9 free cells on the route: the 7 inside it are passed as horizontal straights. */
    private fun highway(): GameState {
        val (level, path) = firstRouteThrough(ScriptedPlay.bundledIds()) { row(it, 9) }
        return play(build(start(level.id), routeOf(level, path)))
    }

    /** A level that draws crosses, with a row of 5 cells on the route whose middle 3 are crosses passed straight through. */
    private fun crossroadMaster(): GameState {
        val crossLevels = ScriptedPlay.bundledIds().filter { bundled(it).isAllowed(PipeSection.X) }
        var line = emptyList<Position>()
        val (level, path) = firstRouteThrough(crossLevels) { anchor -> row(anchor, 5).also { line = it } }
        val crosses = line.subList(1, 4).toSet()
        val pipes = routeOf(level, path).map { (at, section) -> at to if (at in crosses) PipeSection.X else section }
        return play(build(start(level.id), pipes))
    }

    /** A level that draws reservoirs, with a row of 5 cells on the route whose middle 3 are reservoirs. */
    private fun snail(): GameState {
        val reservoirLevels = ScriptedPlay.bundledIds().filter { bundled(it).isAllowed(PipeSection.RES) }
        var line = emptyList<Position>()
        val (level, path) = firstRouteThrough(reservoirLevels) { anchor -> row(anchor, 5).also { line = it } }
        val reservoirs = line.subList(1, 4).toSet()
        val pipes = routeOf(level, path).map { (at, section) -> at to if (at in reservoirs) PipeSection.RES else section }
        return play(build(start(level.id), pipes))
    }

    /** A level with room for a staircase of 16 cells down and to the right: every cell inside it is a corner. */
    private fun snake(): GameState {
        val (level, path) = firstRouteThrough(ScriptedPlay.bundledIds()) { anchor ->
            (0 until 16).map { i -> Position(anchor.x + (i + 1) / 2, anchor.y + i / 2) }
        }
        return play(build(start(level.id), routeOf(level, path)))
    }

    /** Level 1 on the shortest route, with a spare H off the route. */
    private fun withSpare(): Pair<GameState, Position> {
        val level = bundled(1)
        val route = routeOf(level, solverPath(level))
        val spare = freeCells(level).first { at -> route.none { it.first == at } }
        return build(start(1), route + (spare to PipeSection.H)) to spare
    }

    /** Level 1 with a spare section off the route replaced 20 times, each after the last one landed. */
    private fun eraser(): GameState {
        var (state, spare) = withSpare()
        repeat(20) { i ->
            state = build(state, listOf(spare to if (i % 2 == 0) PipeSection.V else PipeSection.H))
            state = engine.handle(state, GameCommand.Tick(state.level.rules.replacePenaltyMs))
        }
        assertEquals(20, state.replacements)
        return play(state)
    }

    /** Level 1 with the spare section replaced once and its tile tapped 5 times before the new one lands. */
    private fun onYourNerves(): GameState {
        var (state, spare) = withSpare()
        state = build(state, listOf(spare to PipeSection.V))
        repeat(5) { state = engine.handle(state, GameCommand.ReplacementTap(spare)) }
        assertEquals(5, state.mostReplacementTaps)
        return play(state)
    }

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
        Achievement.LONG_WAY_ROUND_2 to ::sweepWin,
        Achievement.LONG_WAY_ROUND_3 to ::sweepWin,
        Achievement.BIG_SPENDER_2 to ::sweepWin,
        Achievement.CROSSROAD_MASTER to ::crossroadMaster,
        Achievement.EFFICIENCY_MASTER to ::cleanWin,
        Achievement.HIGHWAY to ::highway,
        Achievement.SNAKE to ::snake,
        Achievement.ERASER to ::eraser,
        Achievement.DRAMATIC_FAILURE to ::sweepLeak,
        Achievement.ON_YOUR_NERVES to ::onYourNerves,
        Achievement.SNAIL to ::snail,
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
    fun `the large-grid tiers are reached on a bundled 16 by 12 level`() {
        val end = sweepWin()
        assertEquals(16, end.level.grid.columns)
        assertEquals(12, end.level.grid.rows)
        val summary = AttemptSummary.from(end, RecordOutcome.FIRST_COMPLETION)
        assertTrue(summary.distinctTilesLeft >= AchievementRules.LONG_WAY_ROUND_3_TILES, "only ${summary.distinctTilesLeft} tiles")
        val unlocked = evaluateAttempt(summary).newlyUnlocked
        assertTrue(unlocked.containsAll(listOf(Achievement.LONG_WAY_ROUND_3, Achievement.BIG_SPENDER_2)), "unlocked $unlocked")
    }

    @Test
    fun `Crossroad Master is reached on a level that can draw crosses`() {
        val end = crossroadMaster()
        assertIs<Outcome.Won>(end.outcome)
        assertTrue(end.level.isAllowed(PipeSection.X))
        assertTrue(AttemptSummary.from(end, null).longestCrossChain >= AchievementRules.CROSSROAD_MASTER_CHAIN)
    }

    @Test
    fun `Snail is reached on a level that can draw reservoirs`() {
        val end = snail()
        assertIs<Outcome.Won>(end.outcome)
        assertTrue(end.level.isAllowed(PipeSection.RES))
        assertTrue(AttemptSummary.from(end, null).reservoirsLeft >= AchievementRules.SNAIL_RESERVOIRS)
    }

    @Test
    fun `the Dramatic Failure leaks after more than 50 sections`() {
        val end = sweepLeak()
        assertIs<Outcome.Lost>(end.outcome)
        assertTrue(AttemptSummary.from(end, null).sectionsLeft > AchievementRules.DRAMATIC_FAILURE_SECTIONS)
    }

    @Test
    fun `Tips Addict is reached from one tips reset`() {
        val stats = PlayerStats.NONE.afterTipsReset()
        val unlocked = AchievementRules.evaluate(null, PlayerStats.NONE, stats, emptyMap(), emptyMap()).newlyUnlocked
        assertTrue(Achievement.TIPS_ADDICT in unlocked)
    }

    @Test
    fun `Never Give Up is reached after three leaks on level 1`() {
        val leaks = (1..3).map { AttemptSummary.from(instantFlood(), null) }
        val stats = leaks.fold(PlayerStats.NONE) { s, leak -> s.after(leak) }
        val win = AttemptSummary.from(cleanWin(), RecordOutcome.FIRST_COMPLETION)
        assertTrue(Achievement.NEVER_GIVE_UP in evaluateAttempt(win, statsBefore = stats).newlyUnlocked)
    }

    // --- Playing (shared with ThreeStarReachabilityTest) ---

    private val scripted = ScriptedPlay(engine)

    private fun start(id: Int): GameState = scripted.start(id)

    private fun build(from: GameState, pipes: List<Pair<Position, PipeSection>>): GameState = scripted.build(from, pipes)

    private fun play(state: GameState): GameState = scripted.play(state)

    private fun solverPath(level: Level): List<Position> = scripted.solverPath(level)

    private fun routeOf(level: Level, path: List<Position>): List<Pair<Position, PipeSection>> = scripted.routeOf(level, path)

    private fun grow(level: Level, path: List<Position>): List<Position> = scripted.grow(level, path)

    private fun freeCells(level: Level): List<Position> = scripted.freeCells(level)

    companion object {
        @JvmStatic
        fun skillAndHidden(): List<Achievement> =
            Achievement.entries.filter { it.group == AchievementGroup.SKILL || it.group == AchievementGroup.HIDDEN }

        fun bundled(id: Int): Level = ScriptedPlay.bundled(id)
    }
}
