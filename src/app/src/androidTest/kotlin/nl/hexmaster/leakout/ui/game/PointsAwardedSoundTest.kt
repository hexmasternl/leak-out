package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.audio.RecordingSoundEffectPlayer
import nl.hexmaster.leakout.audio.SoundCatalog
import nl.hexmaster.leakout.audio.SoundEffects
import nl.hexmaster.leakout.core.engine.Award
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.settings.Settings
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The points drop through a recording player. The frame loop does not run in these tests: the
 * awards are either set on the state directly, with a test clock for the throttle, or made by the
 * flow with tick commands on the game screen.
 */
@RunWith(AndroidJUnit4::class)
class PointsAwardedSoundTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val level = FieldTest.level()
    private val session = GameSession(level, Random(7))
    private val player = RecordingSoundEffectPlayer()
    private val sounds = SoundEffects(SoundCatalog.from(listOf("drop.mp3", "section_placement_001.mp3")), player)
    private var nowMs = 10_000L

    private fun played(): List<String> = compose.runOnIdle { player.played.toList() }

    private fun showSound() {
        compose.setContent { PointsAwardedSound(session, sounds) { nowMs } }
        compose.waitForIdle()
    }

    /** Appends [awards] in one state change, as one frame of the flow would, a second later in real time. */
    private fun award(vararg awards: Award, afterMs: Long = 1_000) {
        compose.runOnIdle {
            nowMs += afterMs
            session.state = session.state.copy(awards = session.state.awards + awards)
        }
        compose.waitForIdle()
    }

    private fun drops(count: Int) = List(count) { "drop.mp3" }

    @Test
    fun oneDropPerAward() {
        showSound()
        assertEquals(emptyList<String>(), played())
        award(Award(Position(1, 0), 10, 11_000))
        assertEquals(drops(1), played())
        award(Award(Position(2, 0), 10, 12_000))
        assertEquals(drops(2), played())
    }

    @Test
    fun twoAwardsInOneFrameShareOneDrop() {
        showSound()
        award(Award(Position(1, 0), 10, 11_000), Award(Position(2, 0), 10, 11_000))
        assertEquals(drops(1), played())
    }

    @Test
    fun aCrossScoringTwiceDropsTwice() {
        showSound()
        award(Award(Position(2, 0), 20, 11_000))
        award(Award(Position(2, 0), 20, 13_000), afterMs = 2_000)
        assertEquals(drops(2), played())
    }

    @Test
    fun awardsAgeingOutAreSilent() {
        showSound()
        award(Award(Position(1, 0), 10, 11_000))
        compose.runOnIdle { session.state = session.state.copy(awards = emptyList()) }
        compose.waitForIdle()
        assertEquals(drops(1), played())
    }

    @Test
    fun dropsWithin100MsAreSkipped() {
        showSound()
        award(Award(Position(1, 0), 10, 11_000))
        award(Award(Position(2, 0), 10, 11_040), afterMs = 40)
        award(Award(Position(2, 1), 10, 11_080), afterMs = 40)
        assertEquals(drops(1), played())
        award(Award(Position(2, 2), 10, 11_120), afterMs = 40)
        assertEquals(drops(2), played())
    }

    @Test
    fun aRestartIsSilentAndTheNextAttemptDropsAgain() {
        showSound()
        award(Award(Position(1, 0), 10, 11_000))
        compose.runOnIdle { assertTrue(session.send(GameCommand.Restart)) }
        compose.waitForIdle()
        assertEquals(drops(1), played())
        award(Award(Position(1, 0), 10, 11_000))
        assertEquals(drops(2), played())
    }

    @Test
    fun soundOffPlaysNoDrop() {
        sounds.apply(Settings(soundEnabled = false))
        showSound()
        award(Award(Position(1, 0), 10, 11_000))
        assertEquals(emptyList<String>(), played())
    }

    @Test
    fun theGameScreenDropsWhenTheFlowAwardsPoints() {
        showGame()
        compose.runOnIdle {
            assertTrue(session.placeSection(PipeSection.H, Position(1, 0)))
            session.endCountdown()
        }
        compose.waitForIdle()
        player.plays.clear()
        tickUntilAwarded()
        assertEquals(drops(1), played())
    }

    @Test
    fun thePausedGameScreenAwardsNothingAndStaysSilent() {
        showGame()
        compose.runOnIdle {
            assertTrue(session.placeSection(PipeSection.H, Position(1, 0)))
            session.endCountdown()
            session.send(GameCommand.Pause)
            player.plays.clear()
            repeat(50) { session.sendTick(100) }
            assertTrue(session.state.awards.isEmpty())
        }
        compose.waitForIdle()
        assertEquals(emptyList<String>(), played())
    }

    /** Ticks the flow until the first award; one tick per frame, so the screen sees each state. */
    private fun tickUntilAwarded() {
        var guard = 0
        while (compose.runOnIdle { session.state.awards.isEmpty() }) {
            check(++guard < 200) { "no points were awarded" }
            compose.runOnIdle { session.sendTick(100) }
            compose.waitForIdle()
        }
    }

    private fun showGame() {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session, soundEffects = sounds)
                }
            }
        }
        compose.waitForIdle()
    }
}
