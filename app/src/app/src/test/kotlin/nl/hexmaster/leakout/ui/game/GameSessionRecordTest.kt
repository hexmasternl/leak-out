package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.core.engine.GameCommand.Restart
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
import nl.hexmaster.leakout.history.InMemoryLevelHistoryRepository
import nl.hexmaster.leakout.history.LevelHistory
import nl.hexmaster.leakout.history.RecordResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The win path into the level history: only a won attempt is recorded, once, with its final total and stars. */
class GameSessionRecordTest {
    // The route earns 60 points and nothing is left unused, so the total is 60: two stars.
    private fun level(stars: StarBenchmarks = StarBenchmarks(40, 60, 200)) = Level(
        id = 3,
        name = "Test",
        grid = GridSize(4, 4),
        start = Endpoint(Position(0, 0), Side.RIGHT),
        finish = Endpoint(Position(3, 3), Side.LEFT),
        blocked = emptyList(),
        flow = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 1.0),
        stack = StackSettings(visibleSize = 5),
        rules = PlacementRules(),
        stars = stars,
    )

    private val route = listOf(
        Position(1, 0) to PipeSection.H,
        Position(2, 0) to PipeSection.LB,
        Position(2, 1) to PipeSection.V,
        Position(2, 2) to PipeSection.V,
        Position(2, 3) to PipeSection.RT,
    )

    private val history = InMemoryLevelHistoryRepository()

    private fun GameSession.playToEnd() {
        var ticks = 0
        while (state.outcome is Outcome.Playing && ticks++ < 10_000) send(Tick(16))
    }

    private fun GameSession.win(): GameSession = apply {
        for ((at, section) in route) assertTrue(placeSection(section, at))
        playToEnd()
        assertTrue(state.outcome is Outcome.Won)
    }

    /** No sections placed: the liquid leaves the start into an empty cell. */
    private fun GameSession.lose(): GameSession = apply {
        playToEnd()
        assertTrue(state.outcome is Outcome.Lost)
    }

    @Test
    fun `a won attempt is recorded with its final total and stars`() = runTest {
        val session = GameSession(level(), Random(1)).win()
        session.recordWin(history)
        assertEquals(LevelHistory(3, 60, 2), history.get(3))
        assertEquals(RecordResult.FirstCompletion, session.recordResult)
    }

    @Test
    fun `a leak never reaches the history`() = runTest {
        val session = GameSession(level(), Random(1)).lose()
        session.recordWin(history)
        assertEquals(emptyList<LevelHistory>(), history.recorded)
        assertNull(history.get(3))
        assertNull(session.recordResult)
    }

    @Test
    fun `a leak on a finished level leaves its record unchanged`() = runTest {
        history.recordCompletion(3, 100, 3)
        val session = GameSession(level(), Random(1)).lose()
        session.recordWin(history)
        assertEquals(1, history.recorded.size)
        assertEquals(LevelHistory(3, 100, 3), history.get(3))
    }

    @Test
    fun `a game still running records nothing`() = runTest {
        val session = GameSession(level(), Random(1))
        session.recordWin(history)
        assertEquals(emptyList<LevelHistory>(), history.recorded)
    }

    @Test
    fun `an attempt is recorded once`() = runTest {
        val session = GameSession(level(), Random(1)).win()
        session.recordWin(history)
        session.recordWin(history)
        assertEquals(1, history.recorded.size)
    }

    @Test
    fun `a restart after a win clears the result and a second win is recorded again`() = runTest {
        val session = GameSession(level(), Random(1)).win()
        session.recordWin(history)
        session.send(Restart)
        assertNull(session.recordResult)
        session.win()
        session.recordWin(history)
        assertEquals(2, history.recorded.size)
        assertEquals(RecordResult.NoImprovement, session.recordResult)
    }

    @Test
    fun `beating the stored highscore reports a new highscore`() = runTest {
        history.recordCompletion(3, 50, 1)
        val session = GameSession(level(), Random(1)).win()
        session.recordWin(history)
        assertEquals(RecordResult.NewHighscore(previous = 50), session.recordResult)
        assertEquals(LevelHistory(3, 60, 2), history.get(3))
    }

    @Test
    fun `a win below the one star threshold is recorded with 0 stars`() = runTest {
        val session = GameSession(level(StarBenchmarks(100, 150, 200)), Random(1)).win()
        assertEquals(0, session.state.finalStars)
        session.recordWin(history)
        assertEquals(LevelHistory(3, 60, 0), history.get(3))
    }
}
