package nl.hexmaster.leakout.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlowButtonTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyVariantIsAClickableButtonAtLeast48dpHigh() {
        val clicks = mutableMapOf<FlowButtonVariant, Int>()
        compose.setContent {
            LeakOutTheme {
                Column {
                    FlowButtonVariant.entries.forEach { variant ->
                        FlowButton(
                            text = variant.name,
                            onClick = { clicks[variant] = (clicks[variant] ?: 0) + 1 },
                            variant = variant,
                            size = FlowButtonSize.Small,
                            modifier = Modifier.testTag(variant.name),
                        )
                    }
                }
            }
        }
        FlowButtonVariant.entries.forEach { variant ->
            compose.onNodeWithTag(variant.name)
                .assertHeightIsAtLeast(48.dp)
                .assertHasClickAction()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .performClick()
            assertEquals(variant.name, 1, clicks[variant])
        }
    }

    @Test
    fun disabledButtonIgnoresClicks() {
        var clicks = 0
        compose.setContent {
            LeakOutTheme {
                FlowButton(text = "Next Level", onClick = { clicks++ }, enabled = false, modifier = Modifier.testTag(TAG))
            }
        }
        compose.onNodeWithTag(TAG).assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    private companion object {
        const val TAG = "flow_button"
    }
}
