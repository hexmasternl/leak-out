package nl.hexmaster.leakout.ui.game.onboarding

import android.app.UiAutomation
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The tip buttons in a real landscape, edge-to-edge window, as the game shows them: clear of the
 * display cutout and the system gesture areas. Run on an S21-sized device (the Pixel_10 AVD:
 * 1080 × 2424 at 420 dpi with a punch hole; the S21 is 1080 × 2400 at 421 dpi).
 */
@RunWith(AndroidJUnit4::class)
class TipButtonsSafeAreaTest {
    companion object {
        private val automation: UiAutomation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

        // Before the rule starts the activity, so it starts in landscape and is not recreated.
        @BeforeClass
        @JvmStatic
        fun landscape() {
            automation.setRotation(UiAutomation.ROTATION_FREEZE_90)
        }

        @AfterClass
        @JvmStatic
        fun restoreRotation() {
            automation.setRotation(UiAutomation.ROTATION_FREEZE_0)
            automation.setRotation(UiAutomation.ROTATION_UNFREEZE)
        }
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var unsafe = UnsafeEdges.None
    private var size by mutableStateOf(IntSize.Zero)

    /** Shows the start step with its spotlight at [spotlight], which gets the overlay's pixel size. */
    private fun show(onNext: () -> Unit = {}, spotlight: (IntSize, Float) -> Rect) {
        compose.runOnUiThread {
            compose.activity.enableEdgeToEdge()
            compose.activity.window.attributes = compose.activity.window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        compose.setContent {
            val density = LocalDensity.current
            val direction = LocalLayoutDirection.current
            val insets = WindowInsets.displayCutout.union(WindowInsets.systemGestures)
            unsafe = UnsafeEdges(
                insets.getLeft(density, direction).toFloat(), insets.getTop(density).toFloat(),
                insets.getRight(density, direction).toFloat(), insets.getBottom(density).toFloat(),
            )
            LeakOutTheme {
                Box(Modifier.fillMaxSize().onGloballyPositioned { size = it.size }) {
                    if (size != IntSize.Zero) {
                        TipOverlay(
                            content = TipContent(TipId.Intro, 0, "The Start", "The liquid comes out here.", "start valve"),
                            spotlights = listOf(spotlight(size, density.density)),
                            hint = null,
                            onNext = onNext, onSkip = {}, onDone = {},
                            reducedMotion = true,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun assertClear(tag: String) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        val safe = Rect(unsafe.left, unsafe.top, size.width - unsafe.right, size.height - unsafe.bottom)
        val inside = safe.contains(bounds.topLeft) && safe.contains(bounds.bottomRight - Offset(1f, 1f))
        assertTrue("$tag at $bounds lies inside the safe area $safe (unsafe edges $unsafe)", inside)
    }

    @Test
    fun theWindowIsLandscapeWithAnUnsafeEdge() {
        show { size, _ -> Rect(0f, 0f, size.width / 4f, size.height.toFloat()) }
        assertTrue("landscape: $size", size.width > size.height)
        assertTrue("the device reports a cutout or gesture area at the side: $unsafe", unsafe.left > 0f || unsafe.right > 0f)
    }

    @Test
    fun aCardThatWouldOpenAgainstTheLeftEdgeStaysClearOfIt() {
        // Just wide enough on its left for the card inside the 16 dp gutter, so without the unsafe
        // edges the card would sit right against the left edge; too little room on the other sides.
        show { size, d -> Rect((16 + 12 + 296 + 4) * d, 0f, size.width - (16 + 12 + 200) * d, size.height.toFloat()) }
        assertClear(TipOverlayTags.NEXT)
        assertClear(TipOverlayTags.SKIP)
    }

    @Test
    fun aCardThatWouldOpenAgainstTheRightEdgeStaysClearOfIt() {
        show { size, d -> Rect((16 + 12 + 200) * d, 0f, size.width - (16 + 12 + 296 + 4) * d, size.height.toFloat()) }
        assertClear(TipOverlayTags.NEXT)
        assertClear(TipOverlayTags.SKIP)
    }

    @Test
    fun aFingerTapOnNextTakesEffect() {
        var next = 0
        show(onNext = { next++ }) { size, d -> Rect((16 + 12 + 296 + 4) * d, 0f, size.width - (16 + 12 + 200) * d, size.height.toFloat()) }
        compose.onNodeWithTag(TipOverlayTags.NEXT, useUnmergedTree = true).performTouchInput { fingerTap() }
        compose.waitForIdle()
        assertEquals(1, next)
    }
}
