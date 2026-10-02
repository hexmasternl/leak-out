package nl.hexmaster.leakout.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.engine.GameCommand.Pause
import nl.hexmaster.leakout.core.engine.GameCommand.Place
import nl.hexmaster.leakout.core.engine.GameCommand.Restart
import nl.hexmaster.leakout.core.engine.GameCommand.Resume
import nl.hexmaster.leakout.core.engine.GameCommand.Tick
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position

/** The attempt facts the achievements are decided from: counted and set by the engine, cleared by a restart. */
class AttemptFactsTest {
    private val engine = engine()
    private val level = engineLevel(section = null)

    /** Places [section] on [at] through a real [Place] command, with the stack forced to deal it. */
    private fun GameState.place(at: Position, section: PipeSection): GameState {
        val forced = copy(stack = stack.copy(sections = List(stack.size) { section }))
        val placed = engine.handle(forced, Place(at))
        assertTrue(placed !== forced, "placing $section on $at was refused")
        return placed
    }

    private fun GameState.placeAll(pipes: List<Pair<Position, PipeSection>>): GameState =
        pipes.fold(this) { s, (at, section) -> s.place(at, section) }

    /** Ticks 10 ms at a time until the liquid head is in [at]. */
    private fun GameState.untilHeadIn(at: Position): GameState {
        var s = this
        repeat(100_000) {
            if ((s.flow as? FlowState.Flowing)?.position == at) return s
            s = engine.handle(s, Tick(10))
        }
        error("the head never reached $at")
    }

    @Test
    fun `a fresh attempt has no facts`() {
        val state = engine.start(level)
        assertEquals(0, state.placements)
        assertEquals(0, state.replacements)
        assertFalse(state.everPaused)
        assertFalse(state.routeCompletedBeforeFlow)
        assertFalse(state.photoFinish)
    }

    @Test
    fun `three placements and a replacement count four placements and one replacement`() {
        val state = engine.start(level)
            .place(p(1, 1), PipeSection.H)
            .place(p(2, 1), PipeSection.H)
            .place(p(3, 1), PipeSection.H)
            .place(p(3, 1), PipeSection.V)
        assertEquals(4, state.placements)
        assertEquals(1, state.replacements)
        assertIs<Replacement>(state.replacement)
    }

    @Test
    fun `a replacement without a penalty counts as a replacement too`() {
        val state = engine.start(engineLevel(section = null, rules = PlacementRules(replacePenaltyMs = 0)))
            .place(p(1, 1), PipeSection.H)
            .place(p(1, 1), PipeSection.V)
        assertEquals(2, state.placements)
        assertEquals(1, state.replacements)
    }

    @Test
    fun `a placement refused because the section is filled changes no fact`() {
        val inPipe = engine.start(level).place(p(1, 1), PipeSection.H).untilHeadIn(p(1, 1))
        val refused = engine.handle(inPipe, Place(p(1, 1)))
        assertSame(inPipe, refused)
        assertEquals(1, refused.placements)
        assertEquals(0, refused.replacements)
    }

    @Test
    fun `a placement refused during the replace lock is not counted`() {
        val locked = engine.start(level).place(p(1, 1), PipeSection.H).place(p(1, 1), PipeSection.V)
        val refused = engine.handle(locked, Place(p(3, 3)))
        assertSame(locked, refused)
        assertEquals(2, refused.placements)
    }

    @Test
    fun `pause is remembered after resume`() {
        val state = engine.send(engine.start(level), Pause, Resume)
        assertFalse(state.paused)
        assertTrue(state.everPaused)
    }

    @Test
    fun `a refused pause after the attempt ended does not mark it as paused`() {
        val lost = engine.runToEnd(engine.start(level))
        assertIs<Outcome.Lost>(lost.outcome)
        val refused = engine.handle(lost, Pause)
        assertSame(lost, refused)
        assertFalse(refused.everPaused)
    }

    @Test
    fun `completing the route during the countdown marks it as completed before the flow`() {
        val state = engine.start(level).placeAll(ROUTE)
        assertIs<FlowState.Waiting>(state.flow)
        assertTrue(state.routeComplete)
        assertTrue(state.routeCompletedBeforeFlow)
        assertFalse(state.photoFinish)
        assertIs<Outcome.Won>(engine.runToEnd(state).outcome)
    }

    @Test
    fun `placements that do not complete the route do not mark it`() {
        val state = engine.start(level).placeAll(ROUTE.take(2))
        assertFalse(state.routeComplete)
        assertFalse(state.routeCompletedBeforeFlow)
    }

    @Test
    fun `completing the route after the flow started is not before the flow`() {
        val flowing = engine.start(level).placeAll(ROUTE.take(2)).untilHeadIn(p(0, 1))
        val state = flowing.place(ROUTE[2].first, ROUTE[2].second)
        assertTrue(state.routeComplete)
        assertFalse(state.routeCompletedBeforeFlow)
    }

    @Test
    fun `a replacement that lands during the countdown and completes the route counts as before the flow`() {
        // The V on (2, 2) is first an H, which leaves the route open, and is then replaced.
        val replaced = engine.start(level)
            .placeAll(ROUTE.take(2))
            .place(p(2, 2), PipeSection.H)
            .place(p(2, 2), PipeSection.V)
        assertFalse(replaced.routeComplete)
        val landed = engine.handle(replaced, Tick(level.rules.replacePenaltyMs))
        assertTrue(landed.routeComplete)
        assertTrue(landed.routeCompletedBeforeFlow)
    }

    @Test
    fun `completing the route on the tile the head exits into is a photo finish`() {
        // The head runs through the LB on (2, 1) from the left; its exit at the bottom points at (2, 2).
        val inCorner = engine.start(level).placeAll(ROUTE.take(2)).untilHeadIn(p(2, 1))
        val state = inCorner.place(p(2, 2), PipeSection.V)
        assertTrue(state.routeComplete)
        assertTrue(state.photoFinish)
        assertFalse(state.routeCompletedBeforeFlow)
        val end = engine.runToEnd(state)
        assertIs<Outcome.Won>(end.outcome)
        assertTrue(end.photoFinish)
    }

    @Test
    fun `completing the route on the tile after the start valve while the head is in the start is a photo finish`() {
        val inStart = engine.start(level).placeAll(ROUTE.drop(1)).untilHeadIn(level.start.position)
        val state = inStart.place(ROUTE[0].first, ROUTE[0].second)
        assertTrue(state.photoFinish)
    }

    @Test
    fun `completing the route two tiles ahead of the head is no photo finish`() {
        // The head is in the H on (1, 1), which exits into (2, 1); the route is closed on (2, 2).
        val inStraight = engine.start(level).placeAll(ROUTE.take(2)).untilHeadIn(p(1, 1))
        val state = inStraight.place(p(2, 2), PipeSection.V)
        assertTrue(state.routeComplete)
        assertFalse(state.photoFinish)
    }

    @Test
    fun `a placement after the route is already complete is no photo finish`() {
        val complete = engine.start(level).placeAll(ROUTE)
        val state = complete.untilHeadIn(p(2, 1)).place(p(4, 4), PipeSection.H)
        assertFalse(state.photoFinish)
    }

    @Test
    fun `restart clears the attempt facts`() {
        var state = engine.start(level)
            .placeAll(ROUTE)
            .place(p(3, 3), PipeSection.H)
            .place(p(4, 4), PipeSection.H)
        state = engine.handle(state, Tick(level.rules.replacePenaltyMs))
        state = state.place(p(4, 4), PipeSection.V)
        state = engine.send(state, Tick(level.rules.replacePenaltyMs), Pause)
        assertEquals(6, state.placements)
        assertEquals(1, state.replacements)
        assertTrue(state.everPaused)
        assertTrue(state.routeCompletedBeforeFlow)

        val restarted = engine.handle(state, Restart)
        assertEquals(0, restarted.placements)
        assertEquals(0, restarted.replacements)
        assertFalse(restarted.everPaused)
        assertFalse(restarted.routeCompletedBeforeFlow)
        assertFalse(restarted.photoFinish)
    }

    @Test
    fun `restart clears a photo finish`() {
        val won = engine.runToEnd(engine.start(level).placeAll(ROUTE.take(2)).untilHeadIn(p(2, 1)).place(p(2, 2), PipeSection.V))
        assertTrue(won.photoFinish)
        assertFalse(engine.handle(won, Restart).photoFinish)
    }
}
