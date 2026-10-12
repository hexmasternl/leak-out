package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.engine.GameCommand.FastForward
import nl.hexmaster.leakout.core.engine.GameCommand.Restart
import nl.hexmaster.leakout.core.engine.GameCommand.Tick
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
import nl.hexmaster.leakout.settings.TipId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The tip freeze on the session (game-play-onboarding): a pause without the pause dialog. */
class GameSessionTipsTest {
    private val level = Level(
        id = 4,
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

    private val GameSession.remainingMs: Long get() = (state.flow as FlowState.Waiting).remainingMs

    @Test
    fun `a tip freezes the game without the pause dialog`() {
        val session = session()
        session.offerTips(listOf(TipId.Intro), atLevelStart = true)
        assertEquals(TipId.Intro, session.activeTip)
        assertTrue(session.state.paused)
        assertFalse(session.showPauseDialog)
        assertEquals(session.state.attempt, session.tipsCheckedAttempt)
    }

    @Test
    fun `the countdown does not move during a tip and continues after it`() {
        val session = session()
        session.send(Tick(2_000))
        session.offerTips(listOf(TipId.Cross))
        session.send(Tick(20_000))
        assertEquals(8_000L, session.remainingMs)
        assertEquals(TipId.Cross, session.closeTip())
        assertFalse(session.state.paused)
        session.send(Tick(1_000))
        assertEquals(7_000L, session.remainingMs)
    }

    @Test
    fun `due tips show one after the other and the game stays frozen until the last closes`() {
        val session = session()
        session.offerTips(listOf(TipId.Intro, TipId.Cross, TipId.Reservoir), atLevelStart = true)
        session.closeTip()
        assertEquals(TipId.Cross, session.activeTip)
        assertTrue(session.state.paused)
        session.closeTip()
        assertEquals(TipId.Reservoir, session.activeTip)
        assertTrue(session.state.paused)
        session.closeTip()
        assertNull(session.activeTip)
        assertFalse(session.state.paused)
    }

    @Test
    fun `the tour moves through its six steps and stops at the last`() {
        val session = session()
        session.offerTips(listOf(TipId.Intro), atLevelStart = true)
        repeat(10) { session.nextTourStep() }
        assertEquals(INTRO_STEPS - 1, session.tourStep)
        session.closeTip()
        assertEquals(0, session.tourStep)
    }

    @Test
    fun `a closed tip is not offered again in the session`() {
        val session = session()
        session.offerTips(listOf(TipId.Cross))
        session.closeTip()
        session.offerTips(listOf(TipId.Cross))
        assertNull(session.activeTip)
        assertFalse(session.state.paused)
    }

    @Test
    fun `nothing is offered while a tip shows or while paused by the player`() {
        val session = session()
        session.offerTips(listOf(TipId.Cross))
        session.offerTips(listOf(TipId.Reservoir))
        assertEquals(listOf(TipId.Cross), session.tipQueue)

        val paused = session()
        paused.pauseByUser()
        paused.offerTips(listOf(TipId.Reservoir))
        assertNull(paused.activeTip)
        assertTrue(paused.showPauseDialog)
    }

    @Test
    fun `leaving the app during a tip shows the pause dialog and Resume keeps the tip and the freeze`() {
        val session = session()
        session.send(Tick(2_000))
        session.offerTips(listOf(TipId.Reservoir))
        session.pauseByUser()
        assertTrue(session.showPauseDialog)
        session.resumeByUser()
        assertFalse(session.showPauseDialog)
        assertEquals(TipId.Reservoir, session.activeTip)
        assertTrue(session.state.paused)
        session.send(Tick(5_000))
        assertEquals(8_000L, session.remainingMs)
    }

    @Test
    fun `without a tip the player's pause and resume work as before`() {
        val session = session()
        session.pauseByUser()
        assertTrue(session.showPauseDialog)
        session.resumeByUser()
        assertFalse(session.state.paused)
    }

    @Test
    fun `a restart during a tip drops it and the new attempt is looked at again`() {
        val session = session()
        session.offerTips(listOf(TipId.Intro), atLevelStart = true)
        session.pauseByUser()
        session.send(Restart)
        assertNull(session.activeTip)
        assertFalse(session.pausedByUser)
        assertTrue(session.tipsCheckedAttempt != session.state.attempt)
    }

    @Test
    fun `fast forward keeps its state after a tip`() {
        val session = session()
        // Fast forward needs a complete route: start (0, 0) → (1, 0) → (2, 0) → down to (2, 3) → finish (3, 3).
        assertTrue(session.placeSection(PipeSection.H, Position(1, 0)))
        assertTrue(session.placeSection(PipeSection.LB, Position(2, 0)))
        assertTrue(session.placeSection(PipeSection.V, Position(2, 1)))
        assertTrue(session.placeSection(PipeSection.V, Position(2, 2)))
        assertTrue(session.placeSection(PipeSection.RT, Position(2, 3)))
        assertTrue(session.send(FastForward))
        session.offerTips(listOf(TipId.Cross))
        session.closeTip()
        assertTrue(session.state.fastForward)
    }

    @Test
    fun `an empty level-start check still lets the clock run`() {
        val session = session()
        session.offerTips(emptyList(), atLevelStart = true)
        assertEquals(session.state.attempt, session.tipsCheckedAttempt)
        assertFalse(session.state.paused)
    }
}
