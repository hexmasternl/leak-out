package nl.hexmaster.leakout.ui.game

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the issue #38 HUD mock-up at 800 dp and 640 dp and saves each as a PNG in the app's
 * external files folder (`files/hud-mockup/`), so the review screenshots come from the real code.
 */
@RunWith(AndroidJUnit4::class)
class HudMockupTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mockupRendersAtWideAndNarrowWidths() {
        // A phone in portrait is narrower than 800 dp.
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
        var width by mutableIntStateOf(800)
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.width(width.dp).testTag(TAG)) { HudMockupStates() }
            }
        }
        save("hud-mockup-800dp")
        width = 640
        compose.waitForIdle()
        save("hud-mockup-640dp")
    }

    private fun save(name: String) {
        val bitmap = compose.onNodeWithTag(TAG).captureToImage().asAndroidBitmap()
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
        // The panel is drawn, not just the screen background.
        assertNotEquals(LeakOutColors.SurfaceDeep.toArgb(), bitmap.getPixel(bitmap.width / 2, 2))
        val dir = File(compose.activity.getExternalFilesDir(null), "hud-mockup").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val TAG = "hud_mockup"
    }
}
