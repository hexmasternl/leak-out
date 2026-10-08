package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
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
import nl.hexmaster.leakout.scores.online.ScorePostStatus
import nl.hexmaster.leakout.scores.online.ScoreSubmissions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** A won attempt's total goes to the score server once per attempt (spec `online-score-submission`). */
class GameSessionScorePostTest {
    // The route earns 60 points and nothing is left unused, so the total is 60.
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
        stars = StarBenchmarks(40, 60, 200),
    )

    private val route = listOf(
        Position(1, 0) to PipeSection.H,
        Position(2, 0) to PipeSection.LB,
        Position(2, 1) to PipeSection.V,
        Position(2, 2) to PipeSection.V,
        Position(2, 3) to PipeSection.RT,
    )

    private val submitted = mutableListOf<Pair<Int, Int>>()
    private val status = MutableStateFlow(ScorePostStatus.InProgress)
    private val scores = ScoreSubmissions { level, score ->
        submitted += level to score
        status
    }

    private fun GameSession.playToEnd() {
        var ticks = 0
        while (state.outcome is Outcome.Playing && ticks++ < 10_000) send(Tick(16))
    }

    private fun GameSession.win(): GameSession = apply {
        for ((at, section) in route) assertTrue(placeSection(section, at))
        playToEnd()
        assertTrue(state.outcome is Outcome.Won)
    }

    @Test
    fun `a won attempt sends its level and final total`() {
        val session = GameSession(level, Random(1)).win()
        session.submitScore(scores)
        assertEquals(listOf(3 to 60), submitted)
        assertSame(status, session.scorePost)
    }

    @Test
    fun `an attempt is sent once`() {
        val session = GameSession(level, Random(1)).win()
        session.submitScore(scores)
        session.submitScore(scores)
        assertEquals(1, submitted.size)
    }

    @Test
    fun `a lost or running attempt sends nothing`() {
        val running = GameSession(level, Random(1))
        running.submitScore(scores)
        val lost = GameSession(level, Random(1)).apply { playToEnd() }
        assertTrue(lost.state.outcome is Outcome.Lost)
        lost.submitScore(scores)
        assertEquals(emptyList<Pair<Int, Int>>(), submitted)
        assertNull(lost.scorePost)
    }

    @Test
    fun `a restarted and won again attempt is sent again`() {
        val session = GameSession(level, Random(1)).win()
        session.submitScore(scores)
        session.send(Restart)
        assertNull("the new attempt has no sending yet", session.scorePost)
        session.win()
        session.submitScore(scores)
        assertEquals(listOf(3 to 60, 3 to 60), submitted)
    }

    @Test
    fun `nothing to send leaves no status`() {
        val session = GameSession(level, Random(1)).win()
        session.submitScore { _, _ -> null }
        assertNull(session.scorePost)
    }
}
