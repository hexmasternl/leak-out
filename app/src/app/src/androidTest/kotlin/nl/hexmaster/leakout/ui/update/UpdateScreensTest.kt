package nl.hexmaster.leakout.ui.update

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.core.library.NextLevelResolver
import nl.hexmaster.leakout.ui.game.FieldTest
import nl.hexmaster.leakout.ui.game.GameScreen
import nl.hexmaster.leakout.ui.game.GameScreenTags
import nl.hexmaster.leakout.ui.game.GameSession
import nl.hexmaster.leakout.ui.game.LevelCompleteTags
import nl.hexmaster.leakout.ui.game.LevelLostTags
import nl.hexmaster.leakout.ui.game.WinTally
import nl.hexmaster.leakout.ui.game.WinTiming
import nl.hexmaster.leakout.ui.game.flowToEnd
import nl.hexmaster.leakout.ui.game.placeSection
import nl.hexmaster.leakout.ui.game.sendTick
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import nl.hexmaster.leakout.ui.title.WelcomeScreen
import nl.hexmaster.leakout.ui.title.WelcomeScreenTags
import nl.hexmaster.leakout.update.DownloadState
import nl.hexmaster.leakout.update.FakeAppUpdates
import nl.hexmaster.leakout.update.UpdateCheck
import nl.hexmaster.leakout.update.UpdateViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The update screens (openspec/specs/app-updates) around the welcome and game screens, in a small
 * host that switches screens the way `MainActivity` does, with [FakeAppUpdates] in place of Play.
 */
@RunWith(AndroidJUnit4::class)
class UpdateScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val updates = FakeAppUpdates()
    private lateinit var viewModel: UpdateViewModel
    private var closes = 0

    private val required = UpdateCheck(available = true, priority = 5)
    private val optional = UpdateCheck(available = true, priority = 0)

    private val level = FieldTest.level(id = 99, stars = StarBenchmarks(20, 40, 200))
    private val nextLevel = FieldTest.level(id = 100)
    private lateinit var session: GameSession

    /** The level being played, or null on the welcome screen. */
    private val current = mutableStateOf<Level?>(null)

    @Test
    fun requiredUpdateAtStartBlocksAndBackClosesTheApp() {
        updates.next = required
        show()

        compose.onNodeWithTag(UpdateTags.BLOCKING_SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithText("Update needed").assertIsDisplayed()
        compose.onNodeWithTag(UpdateTags.BLOCKING_SCREEN, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(UpdateTags.BLOCKING_UPDATE).assertIsDisplayed().assertHasClickAction().assertTextEquals("UPDATE")
        compose.onNodeWithTag(UpdateTags.BLOCKING_CLOSE).assertIsDisplayed().assertHasClickAction().assertTextEquals("CLOSE GAME")

        Espresso.pressBack()
        compose.waitForIdle()
        assertEquals("Back closes the app instead of dismissing the screen", 1, closes)
        compose.onNodeWithTag(UpdateTags.BLOCKING_SCREEN).assertIsDisplayed()

        compose.onNodeWithTag(UpdateTags.BLOCKING_CLOSE).performClick()
        assertEquals(2, closes)
    }

    @Test
    fun cancelledUpdateShowsTheBlockingScreenAgain() {
        updates.next = required
        show()

        compose.onNodeWithTag(UpdateTags.BLOCKING_UPDATE).performClick()
        compose.waitForIdle()
        assertEquals("Update opens Play's immediate flow", 1, updates.requiredStarts)

        // Play hands back a cancelled result; the update is still there.
        compose.runOnIdle { viewModel.onFlowResult() }
        compose.waitForIdle()
        assertEquals(2, updates.checks)
        compose.onNodeWithTag(UpdateTags.BLOCKING_SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertDoesNotExist()
    }

    @Test
    fun optionalPromptIsShownOnceAndCanBeDismissed() {
        updates.next = optional
        show()

        compose.onNodeWithTag(UpdateTags.OPTIONAL_DIALOG).assertIsDisplayed()
        compose.onNodeWithText("Update available").assertIsDisplayed()
        compose.onNodeWithTag(UpdateTags.OPTIONAL_LATER).assertTextEquals("NOT NOW").performClick()
        compose.onNodeWithTag(UpdateTags.OPTIONAL_DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(WelcomeScreenTags.PLAY).assertIsEnabled()

        // Back from the background: Play is asked again, but the prompt is not offered twice.
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        assertEquals(2, updates.checks)
        compose.onNodeWithTag(UpdateTags.OPTIONAL_DIALOG).assertDoesNotExist()
        assertEquals(0, updates.optionalStarts)
    }

    @Test
    fun requiredUpdateDuringALevelLetsTheLevelFinishAndNextLevelShowsTheBlockingScreen() {
        startLevelThenFindRequiredUpdate()

        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) assertTrue(session.placeSection(section, at))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
        compose.waitForIdle()
        val tally = compose.runOnIdle { WinTally.totalDurationMs(session.state.finalScore!!, session.state.finalStars) }
        tickToWinAge(WinTiming.FINISH_EFFECT_MS + tally)
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(UpdateTags.BLOCKING_SCREEN).assertDoesNotExist()

        compose.onNodeWithTag(LevelCompleteTags.NEXT).assertIsEnabled().performClick()
        compose.onNodeWithTag(UpdateTags.BLOCKING_SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertDoesNotExist()
    }

    @Test
    fun requiredUpdateDuringALevelTurnsRestartAfterALossIntoTheBlockingScreen() {
        startLevelThenFindRequiredUpdate()

        compose.runOnIdle {
            assertTrue(session.placeSection(PipeSection.H, Position(1, 0)))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Lost)
        }
        compose.waitForIdle()
        tick(1_000)
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(UpdateTags.BLOCKING_SCREEN).assertDoesNotExist()

        compose.onNodeWithTag(LevelLostTags.RESTART).performClick()
        compose.onNodeWithTag(UpdateTags.BLOCKING_SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertDoesNotExist()
    }

    @Test
    fun downloadedOptionalUpdateIsNotOfferedDuringALevel() {
        updates.next = optional
        show()
        compose.onNodeWithTag(UpdateTags.OPTIONAL_UPDATE).assertTextEquals("UPDATE").performClick()
        compose.waitForIdle()
        assertEquals("Update opens Play's flexible flow", 1, updates.optionalStarts)
        compose.onNodeWithTag(UpdateTags.OPTIONAL_DIALOG).assertDoesNotExist()

        compose.runOnIdle { current.value = level }
        compose.waitForIdle()
        compose.runOnIdle { updates.download(DownloadState.Downloaded) }
        compose.waitForIdle()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(UpdateTags.INSTALL_DIALOG).assertDoesNotExist()

        compose.runOnIdle { current.value = null }
        compose.onNodeWithTag(UpdateTags.INSTALL_DIALOG).assertIsDisplayed()
        compose.onNodeWithText("Update ready").assertIsDisplayed()
        compose.onNodeWithTag(UpdateTags.INSTALL_RESTART).assertTextEquals("RESTART").performClick()
        compose.waitForIdle()
        assertEquals(1, updates.completes)
        compose.onNodeWithTag(UpdateTags.INSTALL_DIALOG).assertDoesNotExist()
    }

    private fun startLevelThenFindRequiredUpdate() {
        current.value = level
        show()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()

        updates.next = required
        compose.runOnIdle { viewModel.check() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(false, viewModel.gate.value.mayStartLevel()) }
        compose.onNodeWithTag(UpdateTags.BLOCKING_SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()
    }

    private fun tick(ms: Long) {
        compose.runOnIdle { session.sendTick(ms) }
        compose.waitForIdle()
    }

    private fun tickToWinAge(ageMs: Long) {
        val remaining = compose.runOnIdle { ageMs - session.winAgeMs }
        if (remaining > 0) tick(remaining)
    }

    private fun show() {
        viewModel = UpdateViewModel(updates)
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                // The test activity shows the system bars; the game hides them, so measure a bare 360 dp screen.
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    val shown = current.value
                    UpdateGateHost(
                        viewModel = viewModel,
                        inLevel = shown != null,
                        onWelcome = shown == null,
                        onCloseApp = { closes++ },
                    ) { mayStartLevel ->
                        when (shown) {
                            null -> WelcomeScreen(
                                resolveNextLevel = { NextLevelResolver.resolve(totalLevels = 1, stars = emptyMap()) },
                                onStartLevel = {},
                                onOpenLevelLibrary = {},
                                onSettings = {},
                                onExit = {},
                            )
                            else -> key(shown) {
                                GameScreen(
                                    level = shown,
                                    onExit = { current.value = null },
                                    onNextLevel = { current.value = nextLevel },
                                    random = Random(7),
                                    session = remember { if (shown == level) session else GameSession(shown, Random(7)) },
                                    mayStartLevel = mayStartLevel,
                                )
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }
}
