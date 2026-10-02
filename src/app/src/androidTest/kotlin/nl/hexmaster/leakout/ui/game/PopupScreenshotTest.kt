package nl.hexmaster.leakout.ui.game

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders "+10" over a filled straight and "+50" over an empty tile at 44 dp and 120 dp, compares
 * them with the same tiles without popups, and saves the popups as a PNG in the app's external
 * files folder (`files/popup-screenshots/`), for a reviewer to compare with the design-system
 * gallery.
 */
@RunWith(AndroidJUnit4::class)
class PopupScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun popupHasADeepFillAGlowEdgeAndNoInkOutline() {
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
        var popups by mutableStateOf(false)
        compose.setContent {
            LeakOutTheme {
                Column(Modifier.padding(8.dp).testTag(TAG)) {
                    PopupPreviewStates(FieldLayout.MIN_CELL, popups = popups)
                    PopupPreviewStates(120.dp, Modifier.padding(top = 8.dp), popups = popups)
                }
            }
        }
        compose.waitForIdle()
        val bare = capture()
        popups = true
        compose.waitForIdle()
        val shown = capture()
        val bareCounts = counts(bare)
        val shownCounts = counts(shown)
        // The opaque deep fill covers part of the pipe, the liquid and the tile.
        assertTrue("deep fill drawn: $bareCounts -> $shownCounts", shownCounts.deep > bareCounts.deep + MIN_PIXELS)
        // The liquid-glow edge and halo surround the glyphs.
        assertTrue("glow edge drawn: $bareCounts -> $shownCounts", shownCounts.glow > bareCounts.glow + MIN_PIXELS)
        // No slim ink outline and no white fill.
        assertTrue("no ink outline: $bareCounts -> $shownCounts", shownCounts.ink <= bareCounts.ink + INK_SLACK)
        assertTrue("no white fill: $bareCounts -> $shownCounts", shownCounts.white <= bareCounts.white + INK_SLACK)
        val dir = File(compose.activity.getExternalFilesDir(null), "popup-screenshots").apply { mkdirs() }
        File(dir, "popups.png").outputStream().use { shown.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun capture(): Bitmap = compose.onNodeWithTag(TAG).captureToImage().asAndroidBitmap()

    private data class Counts(val deep: Int, val glow: Int, val ink: Int, val white: Int)

    private fun counts(bitmap: Bitmap): Counts {
        val pixels = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
        return Counts(
            deep = pixels.count { it.isCloseTo(LeakOutColors.SurfaceDeep.toArgb(), tolerance = 4) },
            glow = pixels.count { it.isCloseTo(LeakOutColors.LiquidGlow.toArgb()) },
            ink = pixels.count { it.isCloseTo(LeakOutColors.InkDark.toArgb(), tolerance = 4) },
            white = pixels.count { it.isCloseTo(0xFFFFFFFF.toInt(), tolerance = 4) },
        )
    }

    private fun Int.isCloseTo(argb: Int, tolerance: Int = 12): Boolean =
        listOf(16, 8, 0).all { shift -> kotlin.math.abs((this shr shift and 0xFF) - (argb shr shift and 0xFF)) <= tolerance }

    private companion object {
        const val TAG = "popup_screenshots"
        const val MIN_PIXELS = 50
        const val INK_SLACK = 20
    }
}
