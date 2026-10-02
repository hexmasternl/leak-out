package nl.hexmaster.leakout.ui.game.onboarding

import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PipeSection.H
import nl.hexmaster.leakout.core.level.PipeSection.RES
import nl.hexmaster.leakout.core.level.PipeSection.V
import nl.hexmaster.leakout.core.level.PipeSection.X
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.core.stack.Dealer
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.settings.TipId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class TipSchedulerTest {
    private val allVisible = (0 until 5).toSet()

    private fun stack(vararg sections: PipeSection) = PipeStack(sections.toList(), Dealer.initial(StackSettings()))

    private fun due(
        stack: PipeStack = stack(H, V, H, V, H),
        seen: Set<TipId> = emptySet(),
        atLevelStart: Boolean = true,
        visible: Set<Int> = allVisible,
        outcome: Outcome = Outcome.Playing,
        mandatory: List<Position> = emptyList(),
    ) = TipScheduler.dueTips(stack, outcome, seen, atLevelStart, visible, mandatory)

    // 3.1

    @Test
    fun `the intro tour is due at the start of the first level, whichever level it is`() {
        assertEquals(listOf(TipId.Intro), due())
    }

    @Test
    fun `the intro tour is not due once seen or after a placement`() {
        assertEquals(emptyList<TipId>(), due(seen = setOf(TipId.Intro)))
        assertEquals(emptyList<TipId>(), due(atLevelStart = false))
    }

    @Test
    fun `a cross in the starting stack comes after the intro tour`() {
        assertEquals(listOf(TipId.Intro, TipId.Cross), due(stack(H, X, V, H, V)))
    }

    @Test
    fun `a cross drawn later is due after that placement`() {
        val before = stack(H, V, H, V, H)
        val after = PipeStack(listOf(X) + before.sections.dropLast(1), before.dealer)
        assertEquals(emptyList<TipId>(), due(before, seen = setOf(TipId.Intro), atLevelStart = false))
        assertEquals(listOf(TipId.Cross), due(after, seen = setOf(TipId.Intro), atLevelStart = false))
    }

    @Test
    fun `a reservoir is due and a seen one is not`() {
        assertEquals(listOf(TipId.Reservoir), due(stack(H, V, RES, V, H), seen = setOf(TipId.Intro), atLevelStart = false))
        assertEquals(emptyList<TipId>(), due(stack(H, V, RES, V, H), seen = setOf(TipId.Intro, TipId.Reservoir), atLevelStart = false))
    }

    @Test
    fun `everything due at once comes in the fixed order`() {
        assertEquals(
            listOf(TipId.Intro, TipId.Mandatory, TipId.Cross, TipId.Reservoir),
            due(stack(RES, H, X, V, H), mandatory = listOf(Position(2, 2))),
        )
    }

    @Test
    fun `the mandatory tip comes after the tour on a level with mandatory sections, at level start only`() {
        val mandatory = listOf(Position(2, 2), Position(3, 1))
        assertEquals(listOf(TipId.Intro, TipId.Mandatory), due(mandatory = mandatory))
        assertEquals(listOf(TipId.Mandatory), due(seen = setOf(TipId.Intro), mandatory = mandatory))
        assertEquals(emptyList<TipId>(), due(seen = setOf(TipId.Intro), atLevelStart = false, mandatory = mandatory))
    }

    @Test
    fun `the mandatory tip is not due once seen or on a level without mandatory sections`() {
        assertEquals(emptyList<TipId>(), due(seen = setOf(TipId.Intro, TipId.Mandatory), mandatory = listOf(Position(2, 2))))
        assertEquals(emptyList<TipId>(), due(seen = setOf(TipId.Intro)))
    }

    @Test
    fun `nothing is due once the attempt has ended`() {
        assertEquals(emptyList<TipId>(), due(stack(RES, X, H, V, H), outcome = Outcome.Won(1_000)))
        assertEquals(emptyList<TipId>(), due(stack(RES, X, H, V, H), outcome = Outcome.Lost(Position(1, 1), Side.LEFT, 1_000)))
    }

    // 3.4

    @Test
    fun `a cross or reservoir in a cut-off slot is not due`() {
        // Slots 0 and 1 (the top of the stack) are cut off.
        val visible = setOf(2, 3, 4)
        assertEquals(emptyList<TipId>(), due(stack(X, RES, H, V, H), seen = setOf(TipId.Intro), atLevelStart = false, visible = visible))
    }

    @Test
    fun `the same section is due once it moves into a visible slot, which is highlighted`() {
        val visible = setOf(2, 3, 4)
        var stack = stack(X, H, V, H, V)
        assertNull(TipScheduler.tipSlot(stack, X, visible))
        repeat(2) { stack = stack.afterPlacement(Random(1)) }
        assertEquals(X, stack.sections[2])
        assertEquals(listOf(TipId.Cross), due(stack, seen = setOf(TipId.Intro), atLevelStart = false, visible = visible))
        assertEquals(2, TipScheduler.tipSlot(stack, X, visible))
    }

    @Test
    fun `a starting stack with the section only in cut-off slots shows just the intro`() {
        assertEquals(listOf(TipId.Intro), due(stack(RES, X, H, V, H), visible = setOf(2, 3, 4)))
    }

    @Test
    fun `the lowest visible slot is chosen when several match`() {
        val stack = stack(X, X, H, X, V)
        assertEquals(3, TipScheduler.tipSlot(stack, X, allVisible))
        assertEquals(1, TipScheduler.tipSlot(stack, X, setOf(0, 1, 2)))
    }

    // 3.2

    @Test
    fun `reservoir speed is shown as a whole percentage, half up`() {
        assertEquals(30, TipText.percent(0.3))
        assertEquals(25, TipText.percent(0.25))
        assertEquals(33, TipText.percent(0.333))
        assertEquals(13, TipText.percent(0.125))
        assertEquals(100, TipText.percent(1.0))
    }

    private fun level(start: Endpoint, blocked: List<Position> = emptyList()) = Level(
        id = 1,
        name = "Test",
        grid = GridSize(5, 4),
        start = start,
        finish = Endpoint(Position(4, 3), Side.LEFT),
        blocked = blocked,
        flow = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 0.8),
        stack = StackSettings(),
        rules = PlacementRules(),
        stars = StarBenchmarks(100, 200, 350),
    )

    @Test
    fun `the hint cell is the one the start exit opens onto`() {
        assertEquals(Position(1, 1), TipScheduler.hintCell(level(Endpoint(Position(0, 1), Side.RIGHT))))
        assertEquals(Position(2, 1), TipScheduler.hintCell(level(Endpoint(Position(2, 0), Side.BOTTOM))))
    }

    @Test
    fun `a blocked or filled exit cell is skipped for the nearest empty one`() {
        val level = level(Endpoint(Position(0, 1), Side.RIGHT), blocked = listOf(Position(1, 1)))
        // (1, 0), (2, 1) and (1, 2) are all one step from the exit cell (1, 1); the upper one wins.
        assertEquals(Position(1, 0), TipScheduler.hintCell(level))
        val board = Board.from(level).with(Position(1, 0), Cell.Pipe(H))
        assertEquals(Position(2, 1), TipScheduler.hintCell(level, board))
    }
}
