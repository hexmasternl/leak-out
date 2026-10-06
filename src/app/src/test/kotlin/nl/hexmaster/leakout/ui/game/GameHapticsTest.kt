package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.GameCommand
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
import nl.hexmaster.leakout.haptics.HapticEffect
import nl.hexmaster.leakout.haptics.Haptics
import nl.hexmaster.leakout.haptics.RecordingHapticPlayer
import nl.hexmaster.leakout.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which placements vibrate (spec `game-haptics`) and when an attempt counts as lost for the haptic. */
class GameHapticsTest {
    private val blocked = Position(1, 1)
    private val player = RecordingHapticPlayer()
    private val haptics = Haptics(player)
    private val replaced = RecordingHapticPlayer.Vibration(
        HapticEffect.SectionReplaced.durationMs,
        HapticEffect.SectionReplaced.amplitude,
    )

    private fun session(rules: PlacementRules = PlacementRules(replacePenaltyMs = 2_000)) = GameSession(
        Level(
            id = 3,
            name = "Test",
            grid = GridSize(4, 4),
            start = Endpoint(Position(0, 0), Side.RIGHT),
            finish = Endpoint(Position(3, 3), Side.LEFT),
            blocked = listOf(blocked),
            flow = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 1.0),
            stack = StackSettings(visibleSize = 5),
            rules = rules,
            stars = StarBenchmarks(100, 200, 350),
        ),
        Random(42),
    )

    @Test
    fun `a replacement vibrates once, short and soft`() {
        val session = session()
        session.placeSection(PipeSection.H, Position(2, 0))
        assertTrue(session.placeWithSound(Position(2, 0), null, haptics))
        assertEquals(listOf(replaced), player.vibrations)
        // The landing of the new section does not vibrate again.
        session.send(GameCommand.Tick(2_000))
        assertFalse(session.isReplacing)
        assertEquals(listOf(replaced), player.vibrations)
    }

    @Test
    fun `a placement on an empty tile does not vibrate`() {
        assertTrue(session().placeWithSound(Position(2, 0), null, haptics))
        assertTrue(player.vibrations.isEmpty())
    }

    @Test
    fun `a refused placement does not vibrate`() {
        assertFalse(session().placeWithSound(blocked, null, haptics))
        assertTrue(player.vibrations.isEmpty())
    }

    @Test
    fun `taps during a replacement do not vibrate`() {
        val session = session()
        session.placeSection(PipeSection.H, Position(2, 0))
        session.placeWithSound(Position(2, 0), null, haptics)
        repeat(3) { session.placeWithSound(Position(2, 0), null, haptics) }
        assertEquals(listOf(replaced), player.vibrations)
    }

    @Test
    fun `a replacement without a penalty vibrates once`() {
        val session = session(PlacementRules(replacePenaltyMs = 0))
        session.placeSection(PipeSection.H, Position(2, 0))
        assertTrue(session.placeWithSound(Position(2, 0), null, haptics))
        assertEquals(listOf(replaced), player.vibrations)
    }

    @Test
    fun `a replacement with vibration off does not vibrate`() {
        haptics.apply(Settings(vibrationEnabled = false))
        val session = session()
        session.placeSection(PipeSection.H, Position(2, 0))
        session.placeWithSound(Position(2, 0), null, haptics)
        assertTrue(player.vibrations.isEmpty())
    }

    @Test
    fun `losing the attempt counts`() {
        assertTrue(lostOnTheWay(AttemptLoss(1, false), AttemptLoss(1, true)))
    }

    @Test
    fun `staying lost counts once`() {
        assertFalse(lostOnTheWay(AttemptLoss(1, true), AttemptLoss(1, true)))
    }

    @Test
    fun `playing on or winning does not count`() {
        assertFalse(lostOnTheWay(AttemptLoss(1, false), AttemptLoss(1, false)))
    }

    @Test
    fun `a restart resets, so losing again counts`() {
        assertFalse(lostOnTheWay(AttemptLoss(1, true), AttemptLoss(2, false)))
        assertTrue(lostOnTheWay(AttemptLoss(2, false), AttemptLoss(2, true)))
    }

    @Test
    fun `a new attempt that is lost at once counts`() {
        assertTrue(lostOnTheWay(AttemptLoss(1, true), AttemptLoss(2, true)))
    }

    @Test
    fun `the state says whether its attempt is lost`() {
        val session = session()
        assertEquals(AttemptLoss(session.state.attempt, false), AttemptLoss.of(session.state))
    }
}
