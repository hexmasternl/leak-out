package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameCommand.Pause
import nl.hexmaster.leakout.core.engine.GameCommand.Place
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The win as the session shows it: the dialog timing, the section removal in step with the tally and the skip. */
class GameSessionWinTest {
    // 60 points on the route; four unused sections cost 12, so the total is 48: one star.
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
        stars = StarBenchmarks(40, 100, 200),
    )

    private val route = listOf(
        Position(1, 0) to PipeSection.H,
        Position(2, 0) to PipeSection.LB,
        Position(2, 1) to PipeSection.V,
        Position(2, 2) to PipeSection.V,
        Position(2, 3) to PipeSection.RT,
    )

    /** Placed off the route in this order; row-major they are (0, 1), (3, 1), (1, 2), (0, 3). */
    private val unused = listOf(
        Position(1, 2) to PipeSection.V,
        Position(0, 3) to PipeSection.LB,
        Position(3, 1) to PipeSection.X,
        Position(0, 1) to PipeSection.H,
    )
    private val unusedInOrder = listOf(Position(0, 1), Position(3, 1), Position(1, 2), Position(0, 3))

    /** A session that has just been won, ticked in frames of 16 ms. */
    private fun won(withUnused: Boolean = true): GameSession {
        val session = GameSession(level, Random(42))
        val pipes = if (withUnused) route + unused else route
        for ((at, section) in pipes) assertTrue(session.placeSection(section, at))
        var ticks = 0
        while (session.state.outcome is Outcome.Playing && ticks++ < 10_000) session.send(Tick(16))
        assertTrue(session.state.outcome is Outcome.Won)
        return session
    }

    /** Ticks the session until its dialog is [dialogAgeMs] old. */
    private fun GameSession.tickToDialogAge(dialogAgeMs: Long) {
        val remaining = WinTiming.FINISH_EFFECT_MS + dialogAgeMs - winAgeMs
        if (remaining > 0) send(Tick(remaining))
    }

    @Test
    fun `a route played to the finish is won with the final score`() {
        val session = won()
        val score = session.state.finalScore!!
        assertEquals(60, score.earned)
        assertEquals(unusedInOrder, score.unusedSections)
        assertEquals(48, score.total)
        assertEquals(1, session.state.finalStars)
        assertEquals(0, session.state.sectionsRemoved)
        assertTrue(session.winAgeMs < 16)
    }

    @Test
    fun `after a win placing and pausing are refused`() {
        val session = won()
        assertFalse(session.canPlace(Position(3, 0)))
        assertFalse(session.send(Place(Position(3, 0))))
        assertFalse(session.send(Pause))
        assertFalse(session.showPauseDialog)
        assertFalse(session.showLostDialog)
        assertFalse(session.showFastForward)
        val score = session.state.score
        session.send(Tick(10_000))
        assertEquals(score, session.state.score)
        assertTrue(session.state.outcome is Outcome.Won)
    }

    @Test
    fun `the dialog shows after 1 second of finish flash`() {
        val session = won()
        session.send(Tick(WinTiming.FINISH_EFFECT_MS - 1 - session.winAgeMs))
        assertEquals(999L, session.winAgeMs)
        assertFalse(session.showWinDialog)
        session.send(Tick(1))
        assertTrue(session.showWinDialog)
        val frame = session.winFrame!!
        assertEquals(0, frame.sectionsRemoved)
        assertEquals(0, frame.totalShown)
        assertFalse(frame.buttonsEnabled)
    }

    @Test
    fun `no win dialog or frame while playing`() {
        val session = GameSession(level, Random(42))
        assertFalse(session.showWinDialog)
        assertNull(session.winFrame)
        assertEquals(0L, session.winAgeMs)
    }

    @Test
    fun `ticking through the penalty removes the unused sections one per step in order`() {
        val session = won()
        session.tickToDialogAge(WinTiming.EARNED_HOLD_MS)
        assertEquals(0, session.state.sectionsRemoved)
        for ((step, at) in unusedInOrder.withIndex()) {
            assertTrue(session.state.board[at] is Cell.Pipe)
            session.send(Tick(WinTiming.PENALTY_STEP_MS))
            assertEquals(step + 1, session.state.sectionsRemoved)
            assertEquals(session.winFrame!!.sectionsRemoved, session.state.sectionsRemoved)
            assertEquals(Cell.Empty, session.state.board[at])
            for (later in unusedInOrder.drop(step + 1)) assertTrue(session.state.board[later] is Cell.Pipe)
        }
        for ((at, section) in route) assertEquals(Cell.Pipe(section, liquidEntered = true), session.state.board[at])
    }

    @Test
    fun `the tally ends with enabled buttons`() {
        val session = won()
        val end = WinTally.totalDurationMs(session.state.finalScore!!, 1)
        session.tickToDialogAge(end - 1)
        assertFalse(session.winFrame!!.buttonsEnabled)
        session.send(Tick(1))
        val frame = session.winFrame!!
        assertTrue(frame.buttonsEnabled)
        assertEquals(WinFrame(4, 12, 48, 1, listOf(1f, 1f, 1f), true), frame)
    }

    @Test
    fun `skipping the win animation removes all unused sections at once`() {
        val session = won()
        session.send(Tick(200))
        session.skipWinAnimation()
        assertTrue(session.showWinDialog)
        val frame = session.winFrame!!
        assertTrue(frame.buttonsEnabled)
        assertEquals(48, frame.totalShown)
        assertEquals(12, frame.penaltyShown)
        assertEquals(1, frame.starsLit)
        assertEquals(4, session.state.sectionsRemoved)
        for (at in unusedInOrder) assertEquals(Cell.Empty, session.state.board[at])
        val after = session.state
        session.skipWinAnimation()
        assertEquals(after, session.state)
    }

    @Test
    fun `skipping without unused sections still ends the tally`() {
        val session = won(withUnused = false)
        session.skipWinAnimation()
        assertTrue(session.winFrame!!.buttonsEnabled)
        assertEquals(60, session.winFrame!!.totalShown)
    }

    @Test
    fun `skipping the win animation while playing changes nothing`() {
        val session = GameSession(level, Random(42))
        val before = session.state
        session.skipWinAnimation()
        assertEquals(before, session.state)
    }

    @Test
    fun `restart after a win gives a fresh attempt`() {
        val session = won()
        session.skipWinAnimation()
        assertTrue(session.send(Restart))
        assertEquals(Outcome.Playing, session.state.outcome)
        assertEquals(0, session.state.score)
        assertNull(session.state.finalScore)
        assertEquals(0, session.state.sectionsRemoved)
        assertTrue(session.state.board.pipes.isEmpty())
        assertFalse(session.showWinDialog)
        assertNull(session.winFrame)
        assertTrue(session.placeSection(PipeSection.H, Position(1, 0)))
    }
}
