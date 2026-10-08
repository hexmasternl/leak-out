package nl.hexmaster.leakout.ui.game

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
 * Renders the portals of pairs 1 to 3 for every open side, dry and passed, and saves them as a PNG
 * in the app's external files folder (`files/portal-screenshots/`), for a reviewer to compare with
 * the design-system gallery.
 */
@RunWith(AndroidJUnit4::class)
class PortalScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun portalsRenderInTheirPairColours() {
        compose.setContent {
            LeakOutTheme {
                Column(Modifier.background(LeakOutColors.SurfaceDeep).padding(8.dp).testTag(TAG)) {
                    PortalPreviewStates()
                    PortalPreviewStates(Modifier.padding(top = 8.dp), passed = true)
                }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag(TAG).captureToImage().asAndroidBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
        for ((name, color) in listOf("portal.1" to LeakOutColors.Portal1, "portal.2" to LeakOutColors.Portal2, "portal.3" to LeakOutColors.Portal3)) {
            assertTrue("$name ring drawn", pixels.any { it.isCloseTo(color.toArgb()) })
        }
        assertTrue("liquid in the passed stubs", pixels.any { it.isCloseTo(LeakOutColors.Liquid500.toArgb(), tolerance = 24) })
        val dir = File(compose.activity.getExternalFilesDir(null), "portal-screenshots").apply { mkdirs() }
        File(dir, "portals.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun Int.isCloseTo(argb: Int, tolerance: Int = 12): Boolean =
        listOf(16, 8, 0).all { shift -> kotlin.math.abs((this shr shift and 0xFF) - (argb shr shift and 0xFF)) <= tolerance }

    private companion object {
        const val TAG = "portal_screenshots"
    }
}
