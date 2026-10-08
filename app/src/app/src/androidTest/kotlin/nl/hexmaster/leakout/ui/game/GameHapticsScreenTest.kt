package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.haptics.HapticEffect
import nl.hexmaster.leakout.haptics.Haptics
import nl.hexmaster.leakout.haptics.RecordingHapticPlayer
import nl.hexmaster.leakout.settings.Settings
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * When the game screen vibrates (spec `game-haptics`), through a recording player. The frame loop
 * does not run in these tests, so the clock is moved with tick commands.
 */
@RunWith(AndroidJUnit4::class)
class GameHapticsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val blocked = Position(1, 2)
    private val level = FieldTest.level(blocked = listOf(blocked))
    private lateinit var session: GameSession
    private val player = RecordingHapticPlayer()
    private val haptics = Haptics(player)
    private var visible by mutableStateOf(true)

    private fun vibrations(): List<HapticEffect> = compose.runOnIdle {
        player.vibrations.map { v -> HapticEffect.entries.first { it.durationMs == v.durationMs && it.amplitude == v.amplitude } }
    }

    private fun cancels(): Int = compose.runOnIdle { player.cancels }

    // Section replaced.

    @Test
    fun replacingASectionVibratesOnceShortAndSoft() {
        show()
        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.V, Position(1, 0))) }
        compose.dropOnCell(Position(1, 0))
        assertEquals(listOf(HapticEffect.SectionReplaced), vibrations())
        assertEquals(RecordingHapticPlayer.Vibration(30, 60), compose.runOnIdle { player.vibrations.single() })
        // The landing does not vibrate again.
        tick(level.rules.replacePenaltyMs)
        assertEquals(listOf(HapticEffect.SectionReplaced), vibrations())
    }

    @Test
    fun placingOnAnEmptyTileOrARefusedTileDoesNotVibrate() {
        show()
        compose.dropOnCell(Position(1, 0))
        compose.cellNode(Position(1, 1)).performTouchInput { click() }
        compose.dropOnCell(blocked)
        compose.waitForIdle()
        assertEquals(emptyList<HapticEffect>(), vibrations())
    }

    // Level lost.

    @Test
    fun aLeakVibratesForOneSecondAtOnceAndOncePerAttempt() {
        show()
        leak()
        assertEquals(listOf(HapticEffect.LevelLost), vibrations())
        assertEquals(RecordingHapticPlayer.Vibration(1_000, 255), compose.runOnIdle { player.vibrations.single() })
        tick(LeakTiming.EFFECT_MS)
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()
        assertEquals(listOf(HapticEffect.LevelLost), vibrations())
    }

    @Test
    fun restartAndLoseAgainVibratesAgain() {
        show()
        leak()
        tick(LeakTiming.EFFECT_MS)
        compose.runOnIdle { assertTrue(session.restart(null)) }
        compose.waitForIdle()
        leak()
        assertEquals(listOf(HapticEffect.LevelLost, HapticEffect.LevelLost), vibrations())
    }

    @Test
    fun aWinDoesNotVibrate() {
        show()
        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) assertTrue(session.placeSection(section, at))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
        compose.waitForIdle()
        assertEquals(emptyList<HapticEffect>(), vibrations())
    }

    // Setting, background and leaving.

    @Test
    fun vibrationOffVibratesNothing() {
        haptics.apply(Settings(vibrationEnabled = false))
        show()
        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.V, Position(1, 0))) }
        compose.dropOnCell(Position(1, 0))
        tick(level.rules.replacePenaltyMs)
        leak()
        assertEquals(emptyList<HapticEffect>(), vibrations())
    }

    @Test
    fun goingToTheBackgroundStopsTheVibration() {
        show()
        leak()
        val before = cancels()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.waitForIdle()
        assertTrue(cancels() > before)
    }

    @Test
    fun leavingTheGameStopsTheVibration() {
        show()
        leak()
        val before = cancels()
        compose.runOnIdle { visible = false }
        compose.waitForIdle()
        assertTrue(cancels() > before)
    }

    // Gap warning.

    @Test
    fun theGapWarningVibratesWithEveryWarning() {
        showGapRow()
        assertEquals(emptyList<HapticEffect>(), vibrations())
        tick(4_000) // 3 sections away: blinking starts on "WARNING"
        assertEquals(listOf(HapticEffect.GapWarning), vibrations())
        assertEquals(RecordingHapticPlayer.Vibration(50, 140), compose.runOnIdle { player.vibrations.single() })
        tick(500) // the score: no vibration
        assertEquals(1, vibrations().size)
        tick(500)
        assertEquals(2, vibrations().size)
        tick(500)
        tick(500)
        tick(200) // 2.2 seconds of blinking: warnings at 0, 1 and 2 seconds
        assertEquals(List(3) { HapticEffect.GapWarning }, vibrations())
    }

    @Test
    fun buildingTheGapAwayStopsTheWarningVibration() {
        showGapRow()
        tick(4_000)
        assertEquals(1, vibrations().size)
        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.H, Position(7, 0))) }
        compose.waitForIdle()
        // The gap is 4 sections away now; for the next 0.9 s of flow it stays more than 3 away.
        repeat(3) { tick(300) }
        assertEquals(1, vibrations().size)
    }

    @Test
    fun pausedTheWarningDoesNotVibrate() {
        showGapRow()
        tick(4_000)
        assertEquals(1, vibrations().size)
        compose.runOnIdle { session.send(GameCommand.Pause) }
        compose.waitForIdle()
        repeat(4) { tick(500) }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        assertEquals(1, vibrations().size)
    }

    /** Runs the flow into the empty tile next to a straight on (1, 0). */
    private fun leak() {
        compose.runOnIdle {
            if (session.state.board[Position(1, 0)] !is Cell.Pipe) {
                assertTrue(session.placeSection(PipeSection.H, Position(1, 0)))
            }
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Lost)
        }
        compose.waitForIdle()
    }

    /** Moves the game clock by [ms] and draws a frame, as the game's frame loop does after every tick. */
    private fun tick(ms: Long) {
        compose.runOnIdle { session.sendTick(ms) }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    /** The gap row of the gap warning tests, with the flow entering the start tile seven sections from the gap. */
    private fun showGapRow() {
        val row = GapLevels.row()
        session = GameSession(row, Random(7))
        for (x in 1..6) assertTrue(session.placeSection(PipeSection.H, Position(x, 0)))
        compose.mainClock.autoAdvance = false
        setContent(row)
        compose.runOnIdle { session.endCountdown() }
        compose.waitForIdle()
    }

    private fun show(shown: Level = level) {
        session = GameSession(shown, Random(7))
        setContent(shown)
    }

    private fun setContent(shown: Level) {
        compose.setContent {
            LeakOutTheme {
                // The test activity shows the system bars; the game hides them, so measure a bare 360 dp screen.
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    if (visible) GameScreen(level = shown, onExit = {}, session = session, haptics = haptics)
                }
            }
        }
        compose.waitForIdle()
    }
}
