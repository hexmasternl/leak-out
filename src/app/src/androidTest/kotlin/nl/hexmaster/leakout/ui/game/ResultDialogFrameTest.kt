package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultDialogFrameTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun compactInfoColumnScrollsWhileTheButtonsStayDisplayed() {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.requiredSize(640.dp, 360.dp)) {
                    ResultDialogFrame(measureRegular = { Spacer(Modifier.height(1_000.dp)) }) { layout ->
                        ResultDialogPanel(layout) {
                            Box(Modifier.testTag("layout_${layout.name}")) {
                                CompactColumns(
                                    info = {
                                        Spacer(Modifier.height(600.dp))
                                        Text("Bottom", Modifier.testTag(BOTTOM))
                                    },
                                    buttons = {
                                        FlowButton("Next Level", {}, size = FlowButtonSize.Medium, variant = FlowButtonVariant.Success, modifier = Modifier.testTag(NEXT))
                                        FlowButton("Exit", {}, variant = FlowButtonVariant.Tertiary, modifier = Modifier.testTag(EXIT))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("layout_Compact").assertExists()
        compose.onNodeWithTag(NEXT).assertIsDisplayed()
        compose.onNodeWithTag(EXIT).assertIsDisplayed()
        compose.onNodeWithTag(BOTTOM).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(NEXT).assertIsDisplayed()
        compose.onNodeWithTag(EXIT).assertIsDisplayed()
    }

    @Test
    fun panelPopsFromScale092To1() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LeakOutTheme {
                ResultDialogWindow(
                    onDismissRequest = {},
                    dismissOnBackPress = true,
                    panelModifier = Modifier.testTag(PANEL),
                    layoutTagPrefix = "test",
                    measureRegular = { Spacer(Modifier.height(100.dp)) },
                    reducedMotion = false,
                ) { Spacer(Modifier.height(100.dp)) }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag(PANEL).assert(SemanticsMatcher.expectValue(PanelScale, 0.92f))
        compose.mainClock.advanceTimeBy(2_000)
        compose.onNodeWithTag(PANEL).assert(SemanticsMatcher.expectValue(PanelScale, 1f))
    }

    private companion object {
        const val BOTTOM = "info_bottom"
        const val NEXT = "next"
        const val EXIT = "exit"
        const val PANEL = "panel"
    }
}
