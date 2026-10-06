package nl.hexmaster.leakout.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection.H
import nl.hexmaster.leakout.core.level.PipeSection.LB
import nl.hexmaster.leakout.core.level.PipeSection.LT
import nl.hexmaster.leakout.core.level.PipeSection.RT
import nl.hexmaster.leakout.core.level.PipeSection.V
import nl.hexmaster.leakout.core.level.PipeSection.X
import nl.hexmaster.leakout.core.level.Side

/** The level of [engineLevel]: start at (0, 1) open to the right, finish at (2, 3) open at the top. */
class GapDistanceTest {
    private val tolerance = 1e-9

    private fun fresh(level: Level = engineLevel()): GameState = engine().start(level)

    /** The head in the start tile, [progress] of the way to its open side. */
    private fun GameState.atStart(progress: Double, used: Set<ChannelUse> = emptySet()): GameState =
        copy(flow = FlowSimulation.startFlow(this).copy(progress = progress, used = used))

    private fun assertGap(expected: Double, state: GameState) {
        val actual = GapDistance.of(state)
        requireNotNull(actual) { "expected a gap at $expected, got none" }
        assertEquals(expected, actual, tolerance)
    }

    @Test
    fun `no gap while the flow counts down, however the field looks`() {
        assertNull(GapDistance.of(fresh()))
        assertNull(GapDistance.of(fresh().withPipes(p(1, 1) to H)))
    }

    @Test
    fun `no gap once the flow has ended`() {
        assertNull(GapDistance.of(fresh().copy(flow = FlowState.Ended(emptySet()))))
    }

    @Test
    fun `no gap after the attempt is won or lost`() {
        val flowing = fresh().atStart(0.5)
        assertNull(GapDistance.of(flowing.copy(outcome = Outcome.Won(1_000))))
        assertNull(GapDistance.of(flowing.copy(outcome = Outcome.Lost(p(0, 1), Side.RIGHT, 1_000))))
    }

    @Test
    fun `a gap right after the start tile is the part of the start still to run`() {
        assertGap(0.75, fresh().atStart(0.25))
    }

    @Test
    fun `a gap right after the head's section is the part of that section still to run`() {
        val state = fresh().withPipes(p(1, 1) to H).headAt(p(1, 1), Side.LEFT, 0.5)
        assertGap(0.5, state)
    }

    @Test
    fun `every connected section ahead counts as a whole one`() {
        val state = fresh().withPipes(p(1, 1) to H, p(2, 1) to H, p(3, 1) to H).atStart(0.0)
        assertGap(4.0, state)
    }

    @Test
    fun `just entered a tile with four sections ahead is five sections away`() {
        // Ahead of the head in (1, 1): (2, 1), (3, 1), (4, 1) turning down, (4, 2), then (4, 3) is empty.
        val state = fresh()
            .withPipes(p(1, 1) to H, p(2, 1) to H, p(3, 1) to H, p(4, 1) to LB, p(4, 2) to V)
            .headAt(p(1, 1), Side.LEFT, 0.0)
        assertGap(5.0, state)
    }

    @Test
    fun `the walk ahead may cross the head's cross on its free channel`() {
        // The head runs left to right through the cross at (1, 1). Ahead: (2, 1) down, (2, 2) left,
        // (1, 2) up into the cross's vertical channel, (1, 0) up and off the grid.
        val state = fresh()
            .withPipes(p(1, 1) to X, p(2, 1) to LB, p(2, 2) to LT, p(1, 2) to RT, p(1, 0) to V)
            .headAt(p(1, 1), Side.LEFT, 0.5)
        assertGap(5.5, state)
    }

    @Test
    fun `a route into the finish has no gap`() {
        assertNull(GapDistance.of(fresh().withPipes(ROUTE).atStart(0.0)))
        assertNull(GapDistance.of(fresh().withPipes(ROUTE).headAt(p(2, 2), Side.TOP, 0.9)))
    }

    @Test
    fun `a finish with a mandatory section unused is the gap`() {
        val level = engineLevel(mandatory = listOf(MandatorySection(p(4, 4), V)))
        assertGap(4.0, fresh(level).withPipes(ROUTE).atStart(0.0))
    }

    @Test
    fun `a mandatory section the liquid already ran through counts as used`() {
        val level = engineLevel(mandatory = listOf(MandatorySection(p(2, 1), LB)))
        val used = setOf(ChannelUse(p(0, 1), Channel.SINGLE), ChannelUse(p(1, 1), Channel.SINGLE))
        val inMandatory = fresh(level).withPipes(p(1, 1) to H, p(2, 2) to V).headAt(p(2, 1), Side.LEFT, 0.5)
        assertNull(GapDistance.of(inMandatory.copy(flow = (inMandatory.flow as FlowState.Flowing).copy(used = used))))
        val pastMandatory = fresh(level).withPipes(p(1, 1) to H, p(2, 2) to V).headAt(p(2, 2), Side.TOP, 0.5)
        val usedWithMandatory = used + ChannelUse(p(2, 1), Channel.SINGLE)
        assertNull(GapDistance.of(pastMandatory.copy(flow = (pastMandatory.flow as FlowState.Flowing).copy(used = usedWithMandatory))))
    }

    @Test
    fun `a finish entered from the wrong side is the gap`() {
        // Down from (1, 1) to (1, 3), then right into the finish's left side; the finish is open at the top.
        val state = fresh().withPipes(p(1, 1) to LB, p(1, 2) to V, p(1, 3) to RT).atStart(0.5)
        assertGap(3.5, state)
    }

    @Test
    fun `the grid edge is the gap`() {
        // (1, 1) turns up into (1, 0), which runs off the top of the grid.
        assertGap(3.0, fresh().withPipes(p(1, 1) to LT, p(1, 0) to V).atStart(0.0))
    }

    @Test
    fun `a blocked tile is the gap`() {
        assertGap(2.0, fresh(engineLevel(blocked = listOf(p(2, 1)))).withPipes(p(1, 1) to H).atStart(0.0))
    }

    @Test
    fun `a section without a matching opening is the gap`() {
        assertGap(2.0, fresh().withPipes(p(1, 1) to H, p(2, 1) to V).atStart(0.0))
    }

    @Test
    fun `a cross channel the liquid already used is the gap, the other channel is not`() {
        val board = fresh().withPipes(p(1, 1) to H, p(2, 1) to X)
        assertGap(2.0, board.atStart(0.0, used = setOf(ChannelUse(p(2, 1), Channel.HORIZONTAL))))
        assertGap(3.0, board.atStart(0.0, used = setOf(ChannelUse(p(2, 1), Channel.VERTICAL))))
    }

    @Test
    fun `a tile with a running replacement counts as empty`() {
        val placed = fresh().withPipes(p(1, 1) to H, p(2, 1) to H).atStart(0.0)
        assertGap(3.0, placed)
        val replacing = placed.copy(
            board = placed.board.with(p(2, 1), Cell.Empty),
            replacement = Replacement(p(2, 1), removed = H, incoming = H, startedAtMs = 0, landsAtMs = 1_000),
        )
        assertGap(2.0, replacing)
    }

    @Test
    fun `a placement on the gap moves it further away`() {
        val before = fresh().withPipes(p(1, 1) to H, p(2, 1) to H).atStart(0.5)
        assertGap(2.5, before)
        val after = engine().send(before, GameCommand.Place(p(3, 1)))
        assertGap(3.5, after)
    }
}
