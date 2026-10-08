package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.GameCommand.Pause
import nl.hexmaster.leakout.core.engine.GameCommand.Resume
import nl.hexmaster.leakout.core.engine.GameCommand.Tick
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [heartbeatFactor] of a real session, driven by ticks: a straight row the liquid runs along. */
class HeartbeatFactorTest {
    private val level = Level(
        id = 1,
        name = "Row",
        grid = GridSize(8, 2),
        start = Endpoint(Position(0, 0), Side.RIGHT),
        finish = Endpoint(Position(7, 1), Side.TOP),
        blocked = emptyList(),
        flow = FlowSettings(initialDelayMs = 1_000, cellsPerSecond = 1.0),
        stack = StackSettings(visibleSize = 5),
        rules = PlacementRules(),
        stars = StarBenchmarks(100, 200, 350),
    )

    /** Six straights from (1, 0) to (6, 0); (7, 0) is empty, so the gap is 7 sections from the start's entry. */
    private fun session() = GameSession(level, Random(3)).apply {
        for (x in 1..6) placeSection(PipeSection.H, Position(x, 0))
    }

    @Test
    fun `silent during the countdown`() {
        assertNull(heartbeatFactor(session().state))
    }

    @Test
    fun `silent far from the gap, starting quietly at 5 sections and louder closer`() {
        val session = session()
        session.send(Tick(1_000))
        assertNull(heartbeatFactor(session.state)) // 7 sections away
        session.send(Tick(2_000))
        assertEquals(0.15f, heartbeatFactor(session.state) ?: -1f, 1e-4f) // 5 sections away
        session.send(Tick(4_000))
        assertEquals(0.83f, heartbeatFactor(session.state) ?: -1f, 1e-4f) // 1 section away
    }

    @Test
    fun `silent while paused, back on resume`() {
        val session = session()
        session.send(Tick(5_000))
        session.send(Pause)
        assertNull(heartbeatFactor(session.state))
        session.send(Resume)
        assertTrue((heartbeatFactor(session.state) ?: 0f) > 0f)
    }

    @Test
    fun `silent once the liquid leaks`() {
        val session = session()
        session.send(Tick(9_000))
        assertTrue(session.state.outcome is Outcome.Lost)
        assertNull(heartbeatFactor(session.state))
    }
}
