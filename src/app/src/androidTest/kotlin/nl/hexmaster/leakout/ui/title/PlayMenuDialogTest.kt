package nl.hexmaster.leakout.ui.title

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayMenuDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private var continues = 0
    private var selects = 0
    private var dismisses = 0

    /** Shows the dialog; a dismiss closes it, as the welcome screen does. */
    private fun showDialog() {
        compose.setContent {
            LeakOutTheme {
                var open by remember { mutableStateOf(true) }
                if (open) {
                    PlayMenuDialog(
                        onContinue = { continues++ },
                        onSelectLevel = { selects++ },
                        onDismiss = {
                            dismisses++
                            open = false
                        },
                    )
                }
            }
        }
    }

    @Test
    fun showsTitleAndBothButtons() {
        showDialog()
        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithText("PLAY").assertIsDisplayed()
        compose.onNodeWithTag(PlayMenuTags.CONTINUE).assertIsDisplayed().assertHasClickAction().assertTextEquals("CONTINUE")
        compose.onNodeWithTag(PlayMenuTags.SELECT_LEVEL).assertIsDisplayed().assertHasClickAction()
            .assertTextEquals("SELECT LEVEL")
    }

    @Test
    fun continueInvokesOnlyOnContinueOnce() {
        showDialog()
        compose.onNodeWithTag(PlayMenuTags.CONTINUE).performClick()
        compose.runOnIdle { assertCounts(cont = 1, select = 0, dismiss = 0) }
    }

    @Test
    fun selectLevelInvokesOnlyOnSelectLevelOnce() {
        showDialog()
        compose.onNodeWithTag(PlayMenuTags.SELECT_LEVEL).performClick()
        compose.runOnIdle { assertCounts(cont = 0, select = 1, dismiss = 0) }
    }

    @Test
    fun backDismissesTheDialog() {
        showDialog()
        // Target the dialog window: the default root picker can pick the activity behind it.
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(ViewActions.pressBack())
        compose.waitForIdle()

        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertCounts(cont = 0, select = 0, dismiss = 1) }
    }

    @Test
    fun tapOutsideDismissesTheDialog() {
        showDialog()
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(
            GeneralClickAction(Tap.SINGLE, { floatArrayOf(8f, 8f) }, Press.FINGER, 0, 0),
        )
        compose.waitForIdle()

        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertCounts(cont = 0, select = 0, dismiss = 1) }
    }

    private fun assertCounts(cont: Int, select: Int, dismiss: Int) {
        assertEquals("onContinue", cont, continues)
        assertEquals("onSelectLevel", select, selects)
        assertEquals("onDismiss", dismiss, dismisses)
    }
}
