package nl.hexmaster.leakout.ui.game

import android.content.pm.ActivityInfo
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
 * Renders the start valve and the checkered finish for every open side, each next to a placed
 * straight, and saves them as a PNG in the app's external files folder
 * (`files/endpoint-screenshots/`), for a reviewer to compare with the design-system gallery.
 */
@RunWith(AndroidJUnit4::class)
class EndpointScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun endpointsRenderNextToAPlacedStraight() {
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
        compose.setContent {
            LeakOutTheme {
                Column(Modifier.background(LeakOutColors.SurfaceDeep).padding(8.dp).testTag(TAG)) {
                    EndpointPreviewStates(start = true)
                    EndpointPreviewStates(start = false, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag(TAG).captureToImage().asAndroidBitmap()
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
        val pixels = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
        // The brass wheel and the success ring are drawn, not just the background.
        assertTrue("brass wheel drawn", pixels.any { it.isCloseTo(LeakOutColors.Brass500.toArgb()) || it.isCloseTo(LeakOutColors.Brass300.toArgb()) })
        assertTrue("success ring drawn", pixels.any { it.isCloseTo(LeakOutColors.Success500.toArgb()) })
        assertTrue("light checker squares drawn", pixels.any { it.isCloseTo(LeakOutColors.TextPrimary.toArgb()) })
        val dir = File(compose.activity.getExternalFilesDir(null), "endpoint-screenshots").apply { mkdirs() }
        File(dir, "endpoints.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun Int.isCloseTo(argb: Int, tolerance: Int = 12): Boolean =
        listOf(16, 8, 0).all { shift -> kotlin.math.abs((this shr shift and 0xFF) - (argb shr shift and 0xFF)) <= tolerance }

    private companion object {
        const val TAG = "endpoint_screenshots"
    }
}
