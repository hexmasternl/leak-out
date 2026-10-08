package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.achievements.AchievementTracker
import nl.hexmaster.leakout.achievements.InMemoryAchievementRepository
import nl.hexmaster.leakout.achievements.toRecordOutcome
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.RecordOutcome
import nl.hexmaster.leakout.core.engine.GameCommand.Pause
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
import nl.hexmaster.leakout.history.RecordResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The session hands every attempt to the achievement tracker exactly once: ended, restarted or exited. */
class GameSessionAchievementsTest {
    private val level = Level(
        id = 1,
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

    private val history = InMemoryLevelHistoryRepository()
    private val store = InMemoryAchievementRepository()

    private fun TestScope.tracker() = AchievementTracker(store, history, backgroundScope)

    private fun GameSession.playToEnd() {
        var ticks = 0
        while (state.outcome is Outcome.Playing && ticks++ < 10_000) send(Tick(16))
    }

    private fun GameSession.win(): GameSession = apply {
        for ((at, section) in route) assertTrue(placeSection(section, at))
        playToEnd()
        assertTrue(state.outcome is Outcome.Won)
    }

    private fun GameSession.lose(): GameSession = apply {
        playToEnd()
        assertTrue(state.outcome is Outcome.Lost)
    }

    /** What the game screen's effect does, however often it runs. */
    private suspend fun GameSession.recordEnd(tracker: AchievementTracker) {
        recordWin(history)
        recordAchievements(tracker)
    }

    @Test
    fun `a win is tracked after it is recorded and reports what it unlocked`() = runTest {
        val session = GameSession(level, Random(1)).win()
        session.recordEnd(tracker())
        // Level 1 in the history, no waste, no replacement, no pause, built before the flow.
        assertEquals(
            listOf(
                Achievement.FIRST_LEAK_FIXED,
                Achievement.ZERO_WASTE,
                Achievement.AHEAD_OF_THE_FLOW,
                Achievement.MEASURE_TWICE,
                Achievement.STEADY_HANDS,
                // The route is the level's shortest one.
                Achievement.EFFICIENCY_MASTER,
            ),
            session.unlockedThisAttempt,
        )
        assertEquals(5, store.currentStats.sectionsPlaced)
    }

    @Test
    fun `running the end effect again does not count the attempt twice`() = runTest {
        val session = GameSession(level, Random(1)).win()
        val tracker = tracker()
        repeat(3) { session.recordEnd(tracker) }
        assertEquals(5, store.currentStats.sectionsPlaced)
        assertEquals(1, store.applied.size)
        assertEquals(6, session.unlockedThisAttempt.size)
    }

    @Test
    fun `a loss is tracked once and unlocks Wet Feet`() = runTest {
        val session = GameSession(level, Random(1)).lose()
        val tracker = tracker()
        session.recordEnd(tracker)
        session.recordEnd(tracker)
        assertTrue(Achievement.WET_FEET in session.unlockedThisAttempt)
        assertEquals(1, store.currentStats.losses)
        assertEquals(mapOf(1 to 1), store.currentStats.lossStreakByLevel)
    }

    @Test
    fun `nothing is tracked while the attempt is playing`() = runTest {
        val session = GameSession(level, Random(1))
        session.recordAchievements(tracker())
        assertEquals(0, store.applied.size)
        assertEquals(emptyList<Achievement>(), session.unlockedThisAttempt)
    }

    @Test
    fun `a restart from the pause dialog adds the placements of the abandoned attempt`() = runTest {
        val session = GameSession(level, Random(1))
        for ((at, section) in route.take(4)) assertTrue(session.placeSection(section, at))
        session.send(Pause)
        assertTrue(session.restart(tracker()))
        testScheduler.runCurrent()
        assertEquals(4, store.currentStats.sectionsPlaced)
        assertEquals(0, store.currentStats.losses)
        assertEquals(0, session.state.placements)
    }

    @Test
    fun `an abandoned attempt is counted once, even when exited after a restart was asked`() = runTest {
        val session = GameSession(level, Random(1))
        val tracker = tracker()
        for ((at, section) in route.take(2)) session.placeSection(section, at)
        session.abandonAttempt(tracker)
        session.abandonAttempt(tracker)
        testScheduler.runCurrent()
        assertEquals(2, store.currentStats.sectionsPlaced)
    }

    @Test
    fun `the next attempt after a restart is counted on its own`() = runTest {
        val session = GameSession(level, Random(1))
        val tracker = tracker()
        session.placeSection(PipeSection.H, Position(1, 0))
        session.restart(tracker)
        session.placeSection(PipeSection.H, Position(1, 0))
        session.placeSection(PipeSection.LB, Position(2, 0))
        session.restart(tracker)
        testScheduler.runCurrent()
        assertEquals(3, store.currentStats.sectionsPlaced)
    }

    @Test
    fun `restarting or exiting an ended attempt adds nothing more`() = runTest {
        val session = GameSession(level, Random(1)).win()
        val tracker = tracker()
        session.recordEnd(tracker)
        session.abandonAttempt(tracker)
        session.restart(tracker)
        testScheduler.runCurrent()
        assertEquals(5, store.currentStats.sectionsPlaced)
    }

    @Test
    fun `a restart clears what the last attempt unlocked`() = runTest {
        val session = GameSession(level, Random(1)).lose()
        session.recordEnd(tracker())
        assertFalse(session.unlockedThisAttempt.isEmpty())
        session.send(Restart)
        assertEquals(emptyList<Achievement>(), session.unlockedThisAttempt)
    }

    @Test
    fun `five taps on a running replacement reach the tracker as On Your Nerves`() = runTest {
        val session = GameSession(level, Random(1))
        assertTrue(session.placeSection(PipeSection.V, Position(1, 1)))
        assertEquals(PlaceResult.Replaced, session.place(Position(1, 1)))
        repeat(5) { assertEquals(PlaceResult.Rejected, session.place(Position(1, 1))) }
        session.send(Tick(level.rules.replacePenaltyMs))
        session.win()
        session.recordEnd(tracker())
        assertTrue(Achievement.ON_YOUR_NERVES in session.unlockedThisAttempt)
    }

    @Test
    fun `beating the highscore unlocks Personal Best`() = runTest {
        history.recordCompletion(1, 10, 0)
        val session = GameSession(level, Random(1)).win()
        session.recordEnd(tracker())
        assertTrue(Achievement.PERSONAL_BEST in session.unlockedThisAttempt)
    }

    @Test
    fun `every record result maps to the core outcome`() {
        assertEquals(RecordOutcome.FIRST_COMPLETION, RecordResult.FirstCompletion.toRecordOutcome())
        assertEquals(RecordOutcome.NEW_HIGHSCORE, RecordResult.NewHighscore(previous = 5).toRecordOutcome())
        assertEquals(RecordOutcome.NO_IMPROVEMENT, RecordResult.NoImprovement.toRecordOutcome())
    }
}
