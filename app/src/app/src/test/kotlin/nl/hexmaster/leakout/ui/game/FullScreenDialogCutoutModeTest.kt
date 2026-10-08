package nl.hexmaster.leakout.ui.game

import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FullScreenDialogCutoutModeTest {
    @Test
    fun `Android 11 and newer use the always mode`() {
        for (sdk in listOf(30, 35, 37)) {
            assertEquals("API $sdk", WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS, fullScreenDialogCutoutMode(sdk))
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `Android 9 and 10 use short edges, as always does not exist there`() {
        for (sdk in listOf(28, 29)) {
            assertEquals("API $sdk", WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES, fullScreenDialogCutoutMode(sdk))
        }
    }

    @Test
    fun `older versions have no cutout mode`() {
        for (sdk in listOf(26, 27)) {
            assertNull("API $sdk", fullScreenDialogCutoutMode(sdk))
        }
    }
}
