package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.core.library.NextLevel
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val startedLevels = mutableListOf<Int>()
    private val openedPages = mutableListOf<Int>()
    private var resolves = 0
    private var settings = 0
    private var exits = 0

    /** What the play menu resolves to; throwing simulates a failed history read. */
    private var nextLevel: () -> NextLevel = { NextLevel(levelId = 1, page = 1, isUnlocked = true, highestCompleted = 0) }

    private var designerOpens = 0

    /** The stored exit opt-out, behind the same [ExitConfirmation] the activity uses. */
    private val exitSettings = InMemorySettingsRepository()
    private val exitConfirmation = ExitConfirmation(exitSettings)

    /** Whether the host passes a level designer callback, as `MainActivity` does with the flag on. */
    private val designerShown = mutableStateOf(false)

    private fun showScreen(widthDp: Int = 800, heightDp: Int = 360, withDesigner: Boolean = false) {
        designerShown.value = withDesigner
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(widthDp.dp, heightDp.dp)) {
                    WelcomeScreen(
                        resolveNextLevel = {
                            resolves++
                            nextLevel()
                        },
                        onStartLevel = { startedLevels += it },
                        onOpenLevelLibrary = { openedPages += it },
                        onSettings = { settings++ },
                        onExit = { exits++ },
                        onOpenLevelDesigner = if (designerShown.value) ({ designerOpens++ }) else null,
                        shouldConfirmExit = exitConfirmation::shouldAsk,
                        onExitAnswered = exitConfirmation::answered,
                    )
                }
            }
        }
    }

    private fun openPlayMenu() {
        compose.onNodeWithTag(WelcomeScreenTags.PLAY).performClick()
        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertIsDisplayed()
    }

    @Test
    fun titleIsDisplayedInUppercase() {
        showScreen()
        compose.onNodeWithText("LEAK OUT").assertIsDisplayed()
        compose.onNodeWithTag(WelcomeScreenTags.TITLE).assertIsDisplayed()
    }

    @Test
    fun allThreeButtonsAreDisplayedWithTheirLabels() {
        showScreen()
        listOf(
            WelcomeScreenTags.PLAY to "PLAY",
            WelcomeScreenTags.SETTINGS to "SETTINGS",
            WelcomeScreenTags.EXIT to "EXIT",
        ).forEach { (tag, label) ->
            compose.onNodeWithTag(tag)
                .assertIsDisplayed()
                .assertHasClickAction()
                .assertTextEquals(label)
        }
    }

    @Test
    fun playOpensThePlayMenuWithoutNavigating() {
        showScreen()
        openPlayMenu()
        compose.onNodeWithTag(PlayMenuTags.CONTINUE).assertIsDisplayed()
        compose.onNodeWithTag(PlayMenuTags.SELECT_LEVEL).assertIsDisplayed()
        compose.runOnIdle {
            assertNoNavigation()
            assertEquals("history is read only on a choice", 0, resolves)
            assertCounts(settings = 0, exit = 0)
        }
    }

    @Test
    fun continueStartsTheNextLevelAndClosesTheMenu() {
        nextLevel = { NextLevel(levelId = 8, page = 1, isUnlocked = true, highestCompleted = 7) }
        showScreen()
        openPlayMenu()
        compose.onNodeWithTag(PlayMenuTags.CONTINUE).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(8), startedLevels)
            assertEquals(emptyList<Int>(), openedPages)
        }
    }

    @Test
    fun continueOnALockedNextLevelOpensTheLibraryOnThePageOfTheHighestCompleted() {
        nextLevel = { NextLevel(levelId = 25, page = 2, isUnlocked = false, highestCompleted = 24) }
        showScreen()
        openPlayMenu()
        compose.onNodeWithTag(PlayMenuTags.CONTINUE).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(emptyList<Int>(), startedLevels)
            assertEquals(listOf(1), openedPages)
        }
    }

    @Test
    fun selectLevelOpensTheLibraryOnThePageOfTheNextLevel() {
        nextLevel = { NextLevel(levelId = 31, page = 2, isUnlocked = true, highestCompleted = 30) }
        showScreen()
        openPlayMenu()
        compose.onNodeWithTag(PlayMenuTags.SELECT_LEVEL).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(emptyList<Int>(), startedLevels)
            assertEquals(listOf(2), openedPages)
        }
    }

    @Test
    fun aFailedHistoryReadKeepsTheMenuOpenAndNavigatesNowhere() {
        nextLevel = { error("history unavailable") }
        showScreen()
        openPlayMenu()
        compose.onNodeWithTag(PlayMenuTags.CONTINUE).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertIsDisplayed()
        compose.runOnIdle { assertNoNavigation() }
    }

    @Test
    fun backClosesThePlayMenuAndKeepsTheWelcomeScreen() {
        showScreen()
        openPlayMenu()
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(ViewActions.pressBack())
        compose.waitForIdle()

        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
        compose.runOnIdle { assertNoNavigation() }
    }

    @Test
    fun settingsInvokesOnlyOnSettingsOnce() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.SETTINGS).performClick()
        compose.runOnIdle {
            assertNoNavigation()
            assertCounts(settings = 1, exit = 0)
        }
    }

    @Test
    fun exitAsksFirstWithoutExiting() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.onNodeWithTag(ExitDialogTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).assertIsOff()
        compose.runOnIdle {
            assertNoNavigation()
            assertCounts(settings = 0, exit = 0)
        }
    }

    @Test
    fun yesExitsOnceWithoutStoringTheOptOut() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.onNodeWithTag(ExitDialogTags.YES).performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertCounts(settings = 0, exit = 1)
            assertEquals(emptyList<String>(), exitSettings.writes)
        }
    }

    @Test
    fun cancelClosesTheDialogAndTheNextExitAsksAgain() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.onNodeWithTag(ExitDialogTags.CANCEL).performClick()
        compose.onNodeWithTag(ExitDialogTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
        compose.runOnIdle { assertCounts(settings = 0, exit = 0) }

        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.onNodeWithTag(ExitDialogTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).assertIsOff()
        compose.runOnIdle { assertEquals(emptyList<String>(), exitSettings.writes) }
    }

    @Test
    fun checkedYesStoresTheOptOutBeforeExiting() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).performClick()
        compose.onNodeWithTag(ExitDialogTags.YES).performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertCounts(settings = 0, exit = 1)
            assertTrue(exitSettings.skipExitConfirmation.value)
        }
    }

    @Test
    fun checkedCancelStoresTheOptOutAndTheNextExitIsImmediate() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).performClick()
        compose.onNodeWithTag(ExitDialogTags.CANCEL).performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertCounts(settings = 0, exit = 0)
            assertTrue(exitSettings.skipExitConfirmation.value)
        }

        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(ExitDialogTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertCounts(settings = 0, exit = 1) }
    }

    @Test
    fun backClosesTheExitDialogWithoutAnAnswer() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).performClick()
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(ViewActions.pressBack())
        compose.waitForIdle()
        compose.onNodeWithTag(ExitDialogTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle {
            assertCounts(settings = 0, exit = 0)
            assertEquals(emptyList<String>(), exitSettings.writes)
        }
    }

    @Test
    fun anOptedOutPlayerExitsWithoutTheDialog() {
        runBlocking { exitSettings.setSkipExitConfirmation(true) }
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(ExitDialogTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertCounts(settings = 0, exit = 1) }
    }

    @Test
    fun buttonsSitBelowTheTitleInPlaySettingsExitOrder() {
        showScreen()
        val title = compose.onNodeWithTag(WelcomeScreenTags.TITLE).getUnclippedBoundsInRoot()
        val play = compose.onNodeWithTag(WelcomeScreenTags.PLAY).getUnclippedBoundsInRoot()
        val settings = compose.onNodeWithTag(WelcomeScreenTags.SETTINGS).getUnclippedBoundsInRoot()
        val exit = compose.onNodeWithTag(WelcomeScreenTags.EXIT).getUnclippedBoundsInRoot()
        listOf(play, settings, exit).forEach { assertTrue(it.top >= title.bottom) }
        assertTrue(play.right <= settings.left)
        assertTrue(settings.right <= exit.left)
    }

    @Test
    fun titleIsHorizontallyCentred() {
        showScreen()
        val title = compose.onNodeWithTag(WelcomeScreenTags.TITLE).getUnclippedBoundsInRoot()
        val screen = compose.onNodeWithTag(WelcomeScreenTags.SCREEN).getUnclippedBoundsInRoot()
        val titleCentre = (title.left + title.right).value / 2f
        val screenCentre = (screen.left + screen.right).value / 2f
        assertEquals(screenCentre, titleCentre, 1f)
    }

    @Test
    fun smallLandscapeKeepsTitleUnclippedAndTargetsAtLeast48dp() {
        showScreen(widthDp = 640, heightDp = 320)
        val screen = compose.onNodeWithTag(WelcomeScreenTags.SCREEN).getUnclippedBoundsInRoot()
        val title = compose.onNodeWithTag(WelcomeScreenTags.TITLE).getUnclippedBoundsInRoot()
        assertTrue(title.left >= screen.left && title.right <= screen.right)
        assertTrue(title.top >= screen.top)
        listOf(WelcomeScreenTags.PLAY, WelcomeScreenTags.SETTINGS, WelcomeScreenTags.EXIT).forEach { tag ->
            val node = compose.onNodeWithTag(tag)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
            assertTrue(node.getUnclippedBoundsInRoot().bottom <= screen.bottom)
        }
    }

    @Test
    fun levelDesignerButtonIsShownInTheTopRightAndOpensTheDesignerOnce() {
        showScreen(withDesigner = true)
        val screen = compose.onNodeWithTag(WelcomeScreenTags.SCREEN).getUnclippedBoundsInRoot()
        val button = compose.onNodeWithTag(WelcomeScreenTags.LEVEL_DESIGNER)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Level designer")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        val bounds = button.getUnclippedBoundsInRoot()
        assertTrue("right half", bounds.left > (screen.left + screen.right) / 2)
        assertTrue("top half", bounds.bottom < (screen.top + screen.bottom) / 2)

        button.performClick()
        compose.runOnIdle {
            assertEquals(1, designerOpens)
            assertNoNavigation()
            assertCounts(settings = 0, exit = 0)
        }
        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
    }

    @Test
    fun levelDesignerButtonLeavesPlaySettingsAndExitUnchanged() {
        showScreen()
        val without = listOf(WelcomeScreenTags.PLAY, WelcomeScreenTags.SETTINGS, WelcomeScreenTags.EXIT)
            .map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot() }
        compose.runOnIdle { designerShown.value = true }
        compose.onNodeWithTag(WelcomeScreenTags.LEVEL_DESIGNER).assertIsDisplayed()
        val with = listOf(WelcomeScreenTags.PLAY, WelcomeScreenTags.SETTINGS, WelcomeScreenTags.EXIT)
            .map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot() }
        assertEquals(without, with)
    }

    @Test
    fun levelDesignerButtonIsAbsentWithoutACallback() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.LEVEL_DESIGNER).assertDoesNotExist()
        compose.onNodeWithContentDescription("Level designer").assertDoesNotExist()
    }

    private fun assertCounts(settings: Int, exit: Int) {
        assertEquals("onSettings", settings, this.settings)
        assertEquals("onExit", exit, exits)
    }

    private fun assertNoNavigation() {
        assertEquals("onStartLevel", emptyList<Int>(), startedLevels)
        assertEquals("onOpenLevelLibrary", emptyList<Int>(), openedPages)
    }
}
