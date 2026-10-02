package nl.hexmaster.leakout.ui.game

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the result dialog design mock-ups of issue #40 at 640 × 360 dp and 1280 × 800 dp (plus the
 * win dialog at 130 % font) and saves each as a PNG in the app's external files folder
 * (`files/result-dialog-mockups/`), for the screenshots on the issue. Each screen is laid out at its
 * true dp size; the density is lowered so a screen larger than the emulator's still fits on it.
 */
@RunWith(AndroidJUnit4::class)
class ResultDialogMockupScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private data class Shot(
        val name: String,
        val dialog: MockupDialog,
        val compact: Boolean,
        val widthDp: Int,
        val heightDp: Int,
        val fontScale: Float = 1f,
        val newBest: Boolean = false,
    )

    @Test
    fun mockupsRenderInBothLayouts() {
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
        val shots = listOf(
            Shot("win-compact-640x360", MockupDialog.Win, compact = true, 640, 360),
            Shot("win-compact-640x360-font130", MockupDialog.Win, compact = true, 640, 360, fontScale = 1.3f, newBest = true),
            Shot("win-regular-1280x800", MockupDialog.Win, compact = false, 1280, 800),
            Shot("leak-compact-640x360", MockupDialog.Leak, compact = true, 640, 360),
            Shot("leak-regular-1280x800", MockupDialog.Leak, compact = false, 1280, 800),
        )
        var shot by mutableStateOf(shots.first())
        compose.setContent {
            val window = compose.activity.window.decorView
            val device = LocalDensity.current
            val current = shot
            // Fit the dp screen into the window: never scale up, only down.
            val scale = minOf(1f, window.width / (current.widthDp * device.density), window.height / (current.heightDp * device.density))
            CompositionLocalProvider(LocalDensity provides Density(device.density * scale, current.fontScale)) {
                LeakOutTheme {
                    Box(Modifier.requiredSize(current.widthDp.dp, current.heightDp.dp).testTag(TAG)) {
                        ResultDialogMockupScreen(current.dialog, current.compact, current.newBest)
                    }
                }
            }
        }
        for (next in shots) {
            shot = next
            compose.waitForIdle()
            save(next.name)
        }
    }

    private fun save(name: String) {
        val bitmap = compose.onNodeWithTag(TAG).captureToImage().asAndroidBitmap()
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
        val dir = File(compose.activity.getExternalFilesDir(null), "result-dialog-mockups").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val TAG = "result_dialog_mockup"
    }
}
