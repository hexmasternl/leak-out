package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.engine.GameCommand.FastForward
import nl.hexmaster.leakout.core.engine.GameCommand.Pause
import nl.hexmaster.leakout.core.engine.GameCommand.Place
import nl.hexmaster.leakout.core.engine.GameCommand.Restart
import nl.hexmaster.leakout.core.engine.GameCommand.Resume
import nl.hexmaster.leakout.core.engine.GameCommand.Tick
import nl.hexmaster.leakout.core.engine.GameEngine
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.StarBenchmarks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session is a thin holder: the rules are tested on the engine in `core`. These tests check
 * what the session adds: [GameSession.send], the dialogs and button derived from the state, and
 * the leak effect skip, reached through real commands and ticks.
 */
class GameSessionTest {
    private val level = Level(
        id = 3,
        name = "Test",
        grid = GridSize(4, 4),
        start = Endpoint(Position(0, 0), Side.RIGHT),
        finish = Endpoint(Position(3, 3), Side.LEFT),
        blocked = emptyList(),
        flow = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 1.0),
        stack = StackSettings(visibleSize = 5),
        rules = PlacementRules(),
        stars = StarBenchmarks(100, 200, 350),
    )

    private fun session() = GameSession(level, Random(42))

    /** Places the route start (0, 0) → (1, 0) → (2, 0) → down to (2, 3) → finish (3, 3), leaving out [skip]. */
    private fun GameSession.buildRoute(skip: Position? = null) {
        val route = listOf(
            Position(1, 0) to PipeSection.H,
            Position(2, 0) to PipeSection.LB,
            Position(2, 1) to PipeSection.V,
            Position(2, 2) to PipeSection.V,
            Position(2, 3) to PipeSection.RT,
        )
        for ((at, section) in route) if (at != skip) assertTrue(placeSection(section, at))
    }

    @Test
    fun `a new session holds the engine's fresh state`() {
        val session = session()
        assertEquals(GameEngine(Random(42)).start(level), session.state)
        assertFalse(session.showPauseDialog)
        assertFalse(session.showLostDialog)
        assertFalse(session.showFastForward)
    }

    @Test
    fun `send returns true and replaces the state when the engine accepts`() {
        val session = session()
        val before = session.state
        assertTrue(session.send(Tick(100)))
        assertEquals(100L, session.state.clockMs)
        assertEquals(0L, before.clockMs)
    }

    @Test
    fun `send returns false and keeps the same state when the engine refuses`() {
        val session = session()
        session.send(Pause)
        val paused = session.state
        assertFalse(session.send(Tick(100)))
        assertFalse(session.send(Place(Position(1, 1))))
        assertFalse(session.send(Pause))
        assertFalse(session.send(Tick(-16)))
        assertSame(paused, session.state)
    }

    @Test
    fun `canPlace asks the engine and agrees with a place command`() {
        val session = session()
        assertTrue(session.canPlace(Position(1, 1)))
        assertFalse(session.canPlace(Position(0, 0)))
        assertFalse(session.send(Place(Position(0, 0))))
        session.send(Pause)
        assertFalse(session.canPlace(Position(1, 1)))
    }

    @Test
    fun `the pause dialog follows pause and resume`() {
        val session = session()
        session.send(Pause)
        assertTrue(session.showPauseDialog)
        session.send(Resume)
        assertFalse(session.showPauseDialog)
        session.send(Pause)
        session.send(Restart)
        assertFalse(session.showPauseDialog)
        assertEquals(2, session.state.attempt)
    }

    @Test
    fun `a leak reached with ticks loses the level and shows the dialog after the effect`() {
        val session = session()
        session.placeSection(PipeSection.LB, Position(1, 0))
        session.send(Tick(10_000)) // countdown over
        session.send(Tick(2_000)) // through the start and the LB, which leaks at its bottom into (1, 1)
        val lost = session.state.outcome as Outcome.Lost
        assertEquals(Position(1, 0) to Side.BOTTOM, lost.at to lost.side)
        assertEquals(15, session.state.score)
        assertFalse(session.showLostDialog)
        session.send(Tick(LeakTiming.EFFECT_MS - 1))
        assertFalse(session.showLostDialog)
        session.send(Tick(1))
        assertTrue(session.showLostDialog)
    }

    @Test
    fun `the pause dialog is never shown after a loss`() {
        val session = session()
        session.send(Tick(11_000)) // nothing placed: the start leaks
        assertTrue(session.state.outcome is Outcome.Lost)
        assertFalse(session.send(Pause))
        assertFalse(session.showPauseDialog)
    }

    @Test
    fun `skipping the leak effect shows the dialog at once`() {
        val session = session()
        session.send(Tick(10_500)) // the head is halfway through the start
        session.send(Tick(500)) // leaks at the start
        assertTrue(session.state.outcome is Outcome.Lost)
        session.send(Tick(200))
        session.skipLeakEffect()
        assertTrue(session.showLostDialog)
        assertEquals(LeakTiming.EFFECT_MS, session.lossAgeMs)
    }

    @Test
    fun `skipping the leak effect while playing changes nothing`() {
        val session = session()
        val before = session.state
        session.skipLeakEffect()
        assertSame(before, session.state)
    }

    @Test
    fun `awards come from the flow and drop after their popup time`() {
        val session = session()
        session.placeSection(PipeSection.H, Position(1, 0))
        session.placeSection(PipeSection.LB, Position(2, 0))
        session.send(Tick(12_000)) // countdown, the start, then the H is left
        assertEquals(10, session.state.score)
        val award = session.state.awards.single()
        assertEquals(Position(1, 0), award.position)
        assertEquals(10, award.points)
        session.send(Tick(PopupTiming.TOTAL_MS - 1))
        assertEquals(listOf(Position(1, 0)), session.state.awards.map { it.position })
        session.send(Tick(1))
        assertEquals(listOf(Position(2, 0)), session.state.awards.map { it.position })
    }

    @Test
    fun `the fast forward button shows on a complete route and toggles the speed`() {
        val session = session()
        session.buildRoute(skip = Position(2, 2))
        assertFalse(session.showFastForward)
        assertFalse(session.toggleFastForward())
        session.placeSection(PipeSection.V, Position(2, 2))
        assertTrue(session.showFastForward)
        assertTrue(session.toggleFastForward())
        assertTrue(session.state.fastForward)
        assertEquals(10, session.state.flowTimeScale)
        assertTrue(session.showFastForward)
        assertTrue(session.toggleFastForward())
        assertFalse(session.state.fastForward)
        assertTrue(session.showFastForward)
    }

    @Test
    fun `a complete route is won and hides the fast forward button`() {
        val session = session()
        session.buildRoute()
        session.send(FastForward)
        repeat(200) { session.send(Tick(16)) }
        assertTrue(session.state.outcome is Outcome.Won)
        assertFalse(session.showFastForward)
        assertFalse(session.showPauseDialog)
        assertFalse(session.showLostDialog)
        assertEquals(10 + 15 + 10 + 10 + 15, session.state.score)
        assertTrue(session.state.flow is FlowState.Ended)
    }

    @Test
    fun `restart after a loss gives a fresh attempt`() {
        val session = session()
        session.placeSection(PipeSection.H, Position(1, 0))
        session.send(Tick(20_000))
        assertTrue(session.state.outcome is Outcome.Lost)
        assertTrue(session.send(Restart))
        assertEquals(Outcome.Playing, session.state.outcome)
        assertEquals(0, session.state.score)
        assertEquals(Cell.Empty, session.state.board[Position(1, 0)])
        assertEquals(FlowState.Waiting(10_000), session.state.flow)
        assertFalse(session.showLostDialog)
        assertEquals(2, session.state.attempt)
    }
}
