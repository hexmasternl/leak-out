package nl.hexmaster.leakout.ui.game.onboarding

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the tip overlay mock-ups of issue #36 at 640 × 360 dp and saves each as a PNG in the
 * app's external files folder (`files/tip-mockups/`), for the design review on the issue.
 */
@RunWith(AndroidJUnit4::class)
class TipMockupScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mockupsRenderAtTheSmallestScreen() {
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
        var step by mutableStateOf(TipMockupStep.Start)
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(640.dp, 360.dp)) { TipMockupScene(step) }
            }
        }
        for (each in TipMockupStep.entries) {
            step = each
            compose.waitForIdle()
            compose.onNodeWithTag(TipMockupTags.CALLOUT).assertIsDisplayed()
            save("tip-${each.name.lowercase()}")
        }
    }

    private fun save(name: String) {
        val bitmap = compose.onNodeWithTag(TipMockupTags.SCENE).captureToImage().asAndroidBitmap()
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
        val dir = File(compose.activity.getExternalFilesDir(null), "tip-mockups").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
