package nl.hexmaster.leakout.core.achievements

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.GameEngine
import nl.hexmaster.leakout.core.engine.GameState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.engine.PathStep
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.TestLevels

/** Summaries built from attempts played through the engine. */
class AttemptSummaryTest {
    private val engine = GameEngine(Random(3))

    /**
     * 5 by 5. Start (0, 2) open right into a mandatory reservoir at (1, 2) and a mandatory cross at
     * (2, 2); the finish at (2, 4) is open at the top. One cell blocked at (0, 0).
     */
    private val crossLevel = TestLevels.valid(
        id = 12,
        grid = GridSize(5, 5),
        start = Endpoint(Position(0, 2), Side.RIGHT),
        finish = Endpoint(Position(2, 4), Side.TOP),
        blocked = listOf(Position(0, 0)),
        flow = FlowSettings(initialDelayMs = 2_000, cellsPerSecond = 2.0),
        stack = StackSettings(weights = TestLevels.weights()),
        mandatory = listOf(MandatorySection(Position(1, 2), PipeSection.RES), MandatorySection(Position(2, 2), PipeSection.X)),
    )

    /** Round after the cross and back down through it into the finish. */
    private val crossRoute = listOf(
        Position(3, 2) to PipeSection.LT,
        Position(3, 1) to PipeSection.LB,
        Position(2, 1) to PipeSection.RB,
        Position(2, 3) to PipeSection.V,
    )

    private fun GameState.place(at: Position, section: PipeSection): GameState {
        val forced = copy(stack = stack.copy(sections = List(stack.size) { section }))
        return engine.handle(forced, GameCommand.Place(at)).also { assertTrue(it !== forced, "placing on $at was refused") }
    }

    private fun GameState.placeAll(pipes: List<Pair<Position, PipeSection>>) = pipes.fold(this) { s, (at, section) -> s.place(at, section) }

    private fun GameState.runToEnd(): GameState {
        var s = this
        while (s.outcome is Outcome.Playing) s = engine.handle(s, GameCommand.Tick(16))
        return s
    }

    @Test
    fun `a won attempt through a cross passed twice`() {
        val built = engine.start(crossLevel).placeAll(crossRoute).place(Position(4, 4), PipeSection.H)
        val won = built.runToEnd()
        assertIs<Outcome.Won>(won.outcome)

        val summary = AttemptSummary.from(won, RecordOutcome.NEW_HIGHSCORE)
        assertEquals(12, summary.levelId)
        assertEquals(AttemptOutcome.WON, summary.outcome)
        assertTrue(summary.won)
        assertEquals(won.finalScore?.total, summary.finalTotal)
        assertEquals(25 + 10 + 15 + 15 + 15 + 50 + 10 - 3, summary.finalTotal)
        assertEquals(won.finalStars, summary.finalStars)
        assertEquals(1, summary.unusedSections)
        assertEquals(6, summary.distinctTilesLeft)
        assertEquals(7, summary.sectionsLeft)
        assertEquals(1, summary.crossesLeftTwice)
        assertEquals(5, summary.placements)
        assertEquals(0, summary.replacements)
        assertFalse(summary.everPaused)
        assertFalse(summary.fastForwardAtWin)
        assertTrue(summary.routeCompletedBeforeFlow)
        assertFalse(summary.photoFinish)
        assertEquals(25 - 3, summary.freeCells)
        assertEquals(7, summary.piecesOnBoard)
        assertEquals(RecordOutcome.NEW_HIGHSCORE, summary.record)
        // RES, X, LT, LB, RB, X, V: the corners are the longest run.
        assertEquals(1, summary.longestCrossChain)
        assertEquals(1, summary.longestStraightRun)
        assertEquals(3, summary.longestCornerRun)
        assertEquals(1, summary.reservoirsLeft)
        assertEquals(0, summary.mostReplacementTaps)
        // The mandatory cross is on the solver's path twice but is one cell.
        assertEquals(6, summary.minimumSections)
    }

    @Test
    fun `a win on the shortest route has no more pieces than the minimum`() {
        val won = engine.start(crossLevel).placeAll(crossRoute).runToEnd()
        val summary = AttemptSummary.from(won, null)
        assertEquals(summary.minimumSections, summary.piecesOnBoard)
        assertTrue(Achievement.EFFICIENCY_MASTER in evaluateAttempt(summary).newlyUnlocked)
    }

    @Test
    fun `a won board after removing the unused section still counts it`() {
        val won = engine.start(crossLevel).placeAll(crossRoute).place(Position(4, 4), PipeSection.H).runToEnd()
        val removed = engine.handle(won, GameCommand.RemoveUnused(1))
        val summary = AttemptSummary.from(removed, null)
        assertEquals(7, summary.piecesOnBoard)
        assertEquals(6, summary.minimumSections)
        assertFalse(Achievement.EFFICIENCY_MASTER in evaluateAttempt(summary).newlyUnlocked)
    }

    @Test
    fun `a straight line of seven is the longest straight run`() {
        val level = TestLevels.valid(grid = GridSize(9, 3), start = Endpoint(Position(0, 1), Side.RIGHT), finish = Endpoint(Position(8, 1), Side.LEFT), blocked = emptyList())
        val won = engine.start(level).placeAll((1..7).map { Position(it, 1) to PipeSection.H }).runToEnd()
        assertIs<Outcome.Won>(won.outcome)
        val summary = AttemptSummary.from(won, null)
        assertEquals(7, summary.longestStraightRun)
        assertEquals(0, summary.longestCornerRun)
        assertEquals(7, summary.minimumSections)
    }

    @Test
    fun `an unsolvable level has no minimum`() {
        // The finish's facing cell is blocked.
        val level = TestLevels.valid(grid = GridSize(5, 5), start = Endpoint(Position(0, 2), Side.RIGHT), finish = Endpoint(Position(4, 2), Side.LEFT), blocked = listOf(Position(3, 2)))
        assertNull(AttemptSummary.minimumSections(level))
    }

    /** A lost attempt on [crossLevel] whose recorded path is [sections], one tile apart. */
    private fun withPath(vararg sections: PipeSection): AttemptSummary {
        val lost = engine.start(crossLevel).runToEnd()
        val path = sections.mapIndexed { i, section -> PathStep(Position(i, 0), section) }
        return AttemptSummary.from(lost.copy(flowPath = path), null)
    }

    @Test
    fun `runs of crosses`() {
        val h = PipeSection.H
        val x = PipeSection.X
        assertEquals(3, withPath(h, x, x, x, h).longestCrossChain)
        assertEquals(2, withPath(x, x, h, x).longestCrossChain)
        assertEquals(0, withPath(h, h).longestCrossChain)
    }

    @Test
    fun `runs of straights only continue in the same direction`() {
        val h = PipeSection.H
        val v = PipeSection.V
        assertEquals(4, withPath(h, h, h, h, PipeSection.X, h, h, h).longestStraightRun)
        assertEquals(3, withPath(h, h, v, v, v).longestStraightRun)
        assertEquals(2, withPath(h, PipeSection.RES, h, h).longestStraightRun)
    }

    @Test
    fun `runs of corners are broken by any other section`() {
        val corners = listOf(PipeSection.LT, PipeSection.RB, PipeSection.LB, PipeSection.RT)
        assertEquals(4, withPath(*corners.toTypedArray(), PipeSection.H, PipeSection.LT).longestCornerRun)
        assertEquals(2, withPath(PipeSection.LT, PipeSection.RB, PipeSection.RES, PipeSection.LB).longestCornerRun)
        assertEquals(2, withPath(PipeSection.LT, PipeSection.X, PipeSection.LB, PipeSection.RT).longestCornerRun)
        assertEquals(2, withPath(PipeSection.RES, PipeSection.H, PipeSection.RES).reservoirsLeft)
    }

    @Test
    fun `the most replacement taps reach the summary`() {
        val lost = engine.start(crossLevel).runToEnd()
        assertEquals(6, AttemptSummary.from(lost.copy(mostReplacementTaps = 6), null).mostReplacementTaps)
    }

    @Test
    fun `removing the unused sections does not change the summary`() {
        val won = engine.start(crossLevel).placeAll(crossRoute).place(Position(4, 4), PipeSection.H).runToEnd()
        val removed = engine.handle(won, GameCommand.RemoveUnused(1))
        assertEquals(1, removed.sectionsRemoved)
        assertEquals(AttemptSummary.from(won, null), AttemptSummary.from(removed, null))
    }

    @Test
    fun `fast forward into the finish is recorded`() {
        val built = engine.start(crossLevel).placeAll(crossRoute)
        val won = engine.handle(built, GameCommand.FastForward).runToEnd()
        assertTrue(AttemptSummary.from(won, null).fastForwardAtWin)
    }

    @Test
    fun `a loss after the cross was passed twice`() {
        // Without the V on (2, 3) the liquid leaks out of the cross's bottom after its second pass.
        val lost = engine.start(crossLevel).placeAll(crossRoute.dropLast(1)).runToEnd()
        assertIs<Outcome.Lost>(lost.outcome)

        val summary = AttemptSummary.from(lost, RecordOutcome.FIRST_COMPLETION)
        assertEquals(AttemptOutcome.LOST, summary.outcome)
        assertTrue(summary.lost)
        assertEquals(1, summary.crossesLeftTwice)
        assertNull(summary.finalTotal)
        assertEquals(0, summary.finalStars)
        assertEquals(0, summary.unusedSections)
        assertNull(summary.record, "a loss records nothing in the history")
        assertFalse(summary.fastForwardAtWin)
        assertTrue(Achievement.DOUBLE_CROSS in evaluateAttempt(summary).newlyUnlocked)
    }

    @Test
    fun `a leak straight out of the start leaves no section`() {
        val level = TestLevels.valid(grid = GridSize(5, 5), start = Endpoint(Position(0, 2), Side.RIGHT), finish = Endpoint(Position(4, 2), Side.LEFT), blocked = emptyList())
        val lost = engine.start(level).runToEnd()
        val summary = AttemptSummary.from(lost, null)
        assertEquals(0, summary.sectionsLeft)
        assertEquals(0, summary.distinctTilesLeft)
        assertTrue(Achievement.INSTANT_FLOOD in evaluateAttempt(summary).newlyUnlocked)
    }

    @Test
    fun `a playing attempt has no summary`() {
        assertFailsWith<IllegalArgumentException> { AttemptSummary.from(engine.start(crossLevel), null) }
    }

    @Test
    fun `free cells leave out blocked cells and both endpoints`() {
        assertEquals(22, AttemptSummary.freeCells(crossLevel))
        val level = TestLevels.valid() // 7 by 9 with one blocked cell
        assertEquals(63 - 3, AttemptSummary.freeCells(level))
    }

    @Test
    fun `a full board on a win is wall to wall`() {
        // Fill every free cell; the spare cells are filled before the flow can reach them.
        val spares = (0 until 5).flatMap { x -> (0 until 5).map { y -> Position(x, y) } }
            .filter { it != Position(0, 0) && it != crossLevel.start.position && it != crossLevel.finish.position }
            .filter { crossLevel.mandatoryAt(it) == null && it !in crossRoute.map { (at, _) -> at } }
        val built = spares.fold(engine.start(crossLevel).placeAll(crossRoute)) { s, at -> s.place(at, PipeSection.H) }
        assertIs<FlowState.Waiting>(built.flow)
        val summary = AttemptSummary.from(built.runToEnd(), null)
        assertEquals(summary.freeCells, summary.piecesOnBoard)
        assertTrue(Achievement.WALL_TO_WALL in evaluateAttempt(summary).newlyUnlocked)
    }
}
