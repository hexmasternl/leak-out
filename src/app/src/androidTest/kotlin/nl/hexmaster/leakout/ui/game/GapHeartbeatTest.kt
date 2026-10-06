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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.audio.RecordingLoopingSoundPlayer
import nl.hexmaster.leakout.audio.RecordingSoundEffectPlayer
import nl.hexmaster.leakout.audio.SoundCatalog
import nl.hexmaster.leakout.audio.SoundEffects
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.settings.Settings
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The heartbeat on the game screen (spec `game-play-gap-warning`, task 3.4), through a recording
 * looping player. The frame loop does not run in these tests, so the clock is moved with ticks.
 */
@RunWith(AndroidJUnit4::class)
class GapHeartbeatTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val loop = RecordingLoopingSoundPlayer()
    private val sounds = SoundEffects(
        SoundCatalog.from(listOf("heartbeat.mp3", "level_failed.mp3")),
        RecordingSoundEffectPlayer(),
        Random(1),
        loop,
    ).apply { apply(Settings(soundVolume = 0.8f)) }
    private lateinit var session: GameSession
    private var showScreen by mutableStateOf(true)

    private fun playingAt(): Float? = compose.runOnIdle { loop.playingAt }

    @Test
    fun silentDuringTheCountdownAndFarFromTheGap() {
        show()
        assertNull(playingAt())
        startFlow()
        assertNull(playingAt()) // 7 sections away
    }

    @Test
    fun startsQuietlyAtFiveSectionsAndGetsLouder() {
        show()
        startFlow()
        tick(2_000) // 5 sections away
        assertEquals(0.12f, playingAt() ?: -1f, 0.001f)
        tick(4_000) // 1 section away
        assertEquals(0.664f, playingAt() ?: -1f, 0.001f)
    }

    @Test
    fun buildingTheGapAwayStopsIt() {
        show()
        startFlow()
        tick(2_000)
        assertTrue(playingAt() != null)
        compose.runOnIdle {
            assertTrue(session.placeSection(PipeSection.H, Position(7, 0)))
            assertTrue(session.placeSection(PipeSection.H, Position(8, 0)))
        }
        compose.waitForIdle()
        assertNull(playingAt()) // 7 sections away
    }

    @Test
    fun pauseStopsItAndResumeStartsItAgain() {
        show()
        startFlow()
        tick(3_000)
        compose.runOnIdle { session.send(GameCommand.Pause) }
        compose.waitForIdle()
        assertNull(playingAt())
        compose.runOnIdle { session.send(GameCommand.Resume) }
        compose.waitForIdle()
        assertTrue(playingAt() != null)
        assertTrue(loop.calls.count { it is RecordingLoopingSoundPlayer.Call.Start } == 2)
    }

    @Test
    fun aLeakStopsIt() {
        show()
        startFlow()
        tick(3_000)
        compose.runOnIdle { session.flowToEnd() }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(session.state.outcome is Outcome.Lost) }
        assertNull(playingAt())
    }

    @Test
    fun soundOffPlaysNoHeartbeat() {
        sounds.apply(Settings(soundEnabled = false))
        show()
        startFlow()
        tick(4_000)
        assertNull(playingAt())
        assertTrue(loop.calls.isEmpty())
    }

    @Test
    fun leavingTheForegroundStopsIt() {
        show()
        startFlow()
        tick(3_000)
        assertTrue(playingAt() != null)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.waitForIdle()
        assertNull(playingAt())
    }

    @Test
    fun leavingTheScreenStopsIt() {
        show()
        startFlow()
        tick(3_000)
        assertTrue(playingAt() != null)
        compose.runOnIdle { showScreen = false }
        compose.waitForIdle()
        assertNull(playingAt())
    }

    private fun tick(ms: Long) {
        compose.runOnIdle { session.sendTick(ms) }
        compose.waitForIdle()
    }

    private fun startFlow() {
        compose.runOnIdle { session.endCountdown() }
        compose.waitForIdle()
    }

    private fun show() {
        session = GameSession(GapLevels.row(columns = 12), Random(7))
        for (x in 1..6) assertTrue(session.placeSection(PipeSection.H, Position(x, 0)))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    if (showScreen) GameScreen(level = session.state.level, onExit = {}, session = session, soundEffects = sounds)
                }
            }
        }
        compose.waitForIdle()
    }
}
