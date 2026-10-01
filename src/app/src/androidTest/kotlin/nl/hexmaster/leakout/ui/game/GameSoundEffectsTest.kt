package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.audio.RecordingSoundEffectPlayer
import nl.hexmaster.leakout.audio.SoundCatalog
import nl.hexmaster.leakout.audio.SoundEffects
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
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
 * Which sound the game screen plays, through a recording player. The frame loop does not run in
 * these tests, so the clock is moved with tick commands, as in the level lost and complete tests.
 */
@RunWith(AndroidJUnit4::class)
class GameSoundEffectsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val blocked = Position(1, 2)
    private val level = FieldTest.level(blocked = listOf(blocked))
    private lateinit var session: GameSession
    private val player = RecordingSoundEffectPlayer()
    private val sounds = SoundEffects(
        SoundCatalog.from(
            listOf(
                "section_placement_001.mp3",
                "section_destroy_001.mp3",
                "section_placement_failed.mp3",
                "level_complete.mp3",
                "level_failed.mp3",
            ),
        ),
        player,
    )

    private fun played(): List<String> = compose.runOnIdle { player.played.toList() }

    @Test
    fun dropOnAnEmptyTilePlaysThePlacedSound() {
        show()
        compose.dropOnCell(Position(1, 0))
        assertEquals(listOf("section_placement_001.mp3"), played())
    }

    @Test
    fun tapOnAnEmptyTilePlaysThePlacedSound() {
        show()
        compose.cellNode(Position(1, 1)).performTouchInput { click() }
        compose.waitForIdle()
        assertEquals(listOf("section_placement_001.mp3"), played())
    }

    @Test
    fun replacingASectionPlaysTheDestroyedSoundAndThePlacedSoundWhenItLands() {
        show()
        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.V, Position(1, 0))) }
        compose.dropOnCell(Position(1, 0))
        assertEquals(listOf("section_destroy_001.mp3"), played())

        compose.runOnIdle { session.sendTick(level.rules.replacePenaltyMs - 1) }
        compose.waitForIdle()
        assertEquals(listOf("section_destroy_001.mp3"), played())

        compose.runOnIdle { session.sendTick(1) }
        compose.waitForIdle()
        assertEquals(listOf("section_destroy_001.mp3", "section_placement_001.mp3"), played())
    }

    @Test
    fun tapsDuringAReplacementAreSilent() {
        show()
        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.V, Position(1, 0))) }
        compose.dropOnCell(Position(1, 0))
        compose.cellNode(Position(1, 1)).performTouchInput { click() }
        compose.cellNode(blocked).performTouchInput { click() }
        compose.waitForIdle()
        assertEquals(listOf("section_destroy_001.mp3"), played())
    }

    @Test
    fun aReplacementEndedByARestartPlaysNoPlacedSound() {
        show()
        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.V, Position(1, 0))) }
        compose.dropOnCell(Position(1, 0))
        compose.runOnIdle {
            session.send(GameCommand.Restart)
            session.sendTick(level.rules.replacePenaltyMs)
        }
        compose.waitForIdle()
        assertEquals(listOf("section_destroy_001.mp3"), played())
    }

    @Test
    fun dropOnABlockedTileAndTapOnTheStartPlayTheFailedSound() {
        show()
        compose.dropOnCell(blocked)
        compose.cellNode(level.start.position).performTouchInput { click() }
        compose.waitForIdle()
        assertEquals(listOf("section_placement_failed.mp3", "section_placement_failed.mp3"), played())
    }

    @Test
    fun lettingGoOverTheStackIsSilent() {
        show()
        val stack = compose.nextNode().fetchSemanticsNode().boundsInRoot.center
        compose.dropAt(stack + Offset(0f, -1f))
        assertEquals(emptyList<String>(), played())
    }

    @Test
    fun aDragCancelledByPauseIsSilent() {
        show()
        compose.beginDragTo(compose.cellRect(Position(1, 0)).center)
        compose.runOnIdle { session.send(GameCommand.Pause) }
        compose.releaseDrag()
        assertEquals(emptyList<String>(), played())
    }

    @Test
    fun winningPlaysTheCompleteSoundOnceWhenTheDialogAppears() {
        show()
        win()
        assertEquals(emptyList<String>(), played())
        tickToWinAge(WinTiming.FINISH_EFFECT_MS)
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
        assertEquals(listOf("level_complete.mp3"), played())
        // The tally keeps the dialog open and recomposing; the sound does not repeat.
        tick(2_000)
        assertEquals(listOf("level_complete.mp3"), played())
    }

    @Test
    fun restartAndWinAgainPlaysTheCompleteSoundOnceMore() {
        show()
        win()
        tickToWinAge(WinTiming.FINISH_EFFECT_MS)
        compose.runOnIdle { session.send(GameCommand.Restart) }
        compose.waitForIdle()
        win()
        tickToWinAge(WinTiming.FINISH_EFFECT_MS)
        assertEquals(listOf("level_complete.mp3", "level_complete.mp3"), played().filter { it == "level_complete.mp3" })
    }

    @Test
    fun aLeakPlaysTheFailedSoundOnlyWhenTheDialogAppears() {
        show()
        compose.runOnIdle {
            assertTrue(session.placeSection(PipeSection.H, Position(1, 0)))
            session.flowToEnd()
            player.plays.clear()
        }
        compose.waitForIdle()
        assertEquals(emptyList<String>(), played())
        tick(LeakTiming.EFFECT_MS)
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()
        assertEquals(listOf("level_failed.mp3"), played())
    }

    @Test
    fun soundOffPlaysNothing() {
        sounds.apply(Settings(soundEnabled = false))
        show()
        compose.dropOnCell(Position(1, 0))
        compose.dropOnCell(blocked)
        assertEquals(emptyList<String>(), played())
    }

    @Test
    fun aWinInTheBackgroundSoundsOnceAfterComingBack() {
        show()
        win()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.waitForIdle()
        assertEquals(emptyList<String>(), played())
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
        assertEquals(listOf("level_complete.mp3"), played())
    }

    @Test
    fun aLeakInTheBackgroundSoundsOnceAfterComingBack() {
        show()
        compose.runOnIdle {
            assertTrue(session.placeSection(PipeSection.H, Position(1, 0)))
            session.flowToEnd()
            player.plays.clear()
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.waitForIdle()
        assertEquals(emptyList<String>(), played())
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()
        assertEquals(listOf("level_failed.mp3"), played())
    }

    /** Places the route through the session, so no placement sound plays, and runs the flow into the finish. */
    private fun win() {
        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) assertTrue(session.placeSection(section, at))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
        compose.waitForIdle()
    }

    private fun tick(ms: Long) {
        compose.runOnIdle { session.sendTick(ms) }
        compose.waitForIdle()
    }

    private fun tickToWinAge(ageMs: Long) {
        val remaining = compose.runOnIdle { ageMs - session.winAgeMs }
        if (remaining > 0) tick(remaining)
    }

    private fun show(shown: Level = level) {
        session = GameSession(shown, Random(7))
        compose.setContent {
            LeakOutTheme {
                // The test activity shows the system bars; the game hides them, so measure a bare 360 dp screen.
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = shown, onExit = {}, session = session, soundEffects = sounds)
                }
            }
        }
    }
}
