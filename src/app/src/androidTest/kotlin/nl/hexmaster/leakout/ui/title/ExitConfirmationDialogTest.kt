package nl.hexmaster.leakout.ui.title

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExitConfirmationDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val confirms = mutableListOf<Boolean>()
    private val cancels = mutableListOf<Boolean>()
    private var dismisses = 0
    private var open by mutableStateOf(true)

    /** Shows the dialog; every action closes it, as the welcome screen does. */
    private fun showDialog() {
        compose.setContent {
            LeakOutTheme {
                if (open) {
                    ExitConfirmationDialog(
                        onConfirm = {
                            confirms += it
                            open = false
                        },
                        onCancel = {
                            cancels += it
                            open = false
                        },
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
    fun showsTheQuestionAnUncheckedCheckboxAndBothButtons() {
        showDialog()
        compose.onNodeWithTag(ExitDialogTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithText("Leave Leak Out?").assertIsDisplayed()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).assertIsDisplayed().assertIsOff()
        compose.onNodeWithText("Never show this again").assertIsDisplayed()
        compose.onNodeWithTag(ExitDialogTags.YES).assertIsDisplayed().assertHasClickAction().assertTextEquals("YES")
        compose.onNodeWithTag(ExitDialogTags.CANCEL).assertIsDisplayed().assertHasClickAction().assertTextEquals("CANCEL")
    }

    @Test
    fun tappingTheRowTogglesTheCheckbox() {
        showDialog()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).performClick().assertIsOn()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).performClick().assertIsOff()
    }

    @Test
    fun yesPassesTheUncheckedState() {
        showDialog()
        compose.onNodeWithTag(ExitDialogTags.YES).performClick()
        compose.runOnIdle { assertCounts(confirms = listOf(false), cancels = emptyList(), dismiss = 0) }
    }

    @Test
    fun yesPassesTheCheckedState() {
        showDialog()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).performClick()
        compose.onNodeWithTag(ExitDialogTags.YES).performClick()
        compose.runOnIdle { assertCounts(confirms = listOf(true), cancels = emptyList(), dismiss = 0) }
    }

    @Test
    fun cancelPassesTheUncheckedState() {
        showDialog()
        compose.onNodeWithTag(ExitDialogTags.CANCEL).performClick()
        compose.runOnIdle { assertCounts(confirms = emptyList(), cancels = listOf(false), dismiss = 0) }
    }

    @Test
    fun cancelPassesTheCheckedState() {
        showDialog()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).performClick()
        compose.onNodeWithTag(ExitDialogTags.CANCEL).performClick()
        compose.runOnIdle { assertCounts(confirms = emptyList(), cancels = listOf(true), dismiss = 0) }
    }

    @Test
    fun theCheckboxIsUncheckedWhenTheDialogOpensAgain() {
        showDialog()
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).performClick().assertIsOn()
        compose.onNodeWithTag(ExitDialogTags.CANCEL).performClick()
        compose.runOnIdle { open = true }
        compose.onNodeWithTag(ExitDialogTags.NEVER_SHOW).assertIsOff()
    }

    @Test
    fun backDismissesTheDialog() {
        showDialog()
        // Target the dialog window: the default root picker can pick the activity behind it.
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(ViewActions.pressBack())
        compose.waitForIdle()

        compose.onNodeWithTag(ExitDialogTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertCounts(confirms = emptyList(), cancels = emptyList(), dismiss = 1) }
    }

    @Test
    fun aTapOutsideDismissesTheDialog() {
        showDialog()
        compose.waitForIdle()
        // Halfway down the left edge: outside the centred panel and clear of the status bar.
        val metrics = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics
        tapScreen(x = 4f * metrics.density, y = metrics.heightPixels / 2f)
        compose.waitForIdle()

        compose.onNodeWithTag(ExitDialogTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertCounts(confirms = emptyList(), cancels = emptyList(), dismiss = 1) }
    }

    private fun tapScreen(x: Float, y: Float) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val down = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }
            automation.injectInputEvent(event, true)
            event.recycle()
        }
    }

    private fun assertCounts(confirms: List<Boolean>, cancels: List<Boolean>, dismiss: Int) {
        assertEquals("onConfirm", confirms, this.confirms)
        assertEquals("onCancel", cancels, this.cancels)
        assertEquals("onDismiss", dismiss, dismisses)
    }
}
