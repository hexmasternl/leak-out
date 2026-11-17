package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class HudInsetsTest {
    // A 2400 x 1080 landscape screen with a 144 px tall HUD starting at the top.
    private val width = 2400
    private val hudTop = 0
    private val hudBottom = 144

    private fun insets(vararg cutouts: CutoutRect) = hudHorizontalInsets(cutouts.toList(), hudTop, hudBottom, width)

    @Test
    fun noCutoutLeavesTheHudFullWidth() {
        assertEquals(HudInsets.None, insets())
    }

    @Test
    fun cameraHoleInTheMiddleOfTheRightEdgeLeavesTheHudFullWidth() {
        assertEquals(HudInsets.None, insets(CutoutRect(2310, 480, 2400, 600)))
    }

    @Test
    fun cameraHoleInTheMiddleOfTheLeftEdgeLeavesTheHudFullWidth() {
        assertEquals(HudInsets.None, insets(CutoutRect(0, 480, 90, 600)))
    }

    @Test
    fun cornerCutoutOnTheLeftPushesOnlyTheLeftSideIn() {
        assertEquals(HudInsets(left = 90, right = 0), insets(CutoutRect(0, 0, 90, 120)))
    }

    @Test
    fun cornerCutoutOnTheRightPushesOnlyTheRightSideIn() {
        assertEquals(HudInsets(left = 0, right = 90), insets(CutoutRect(2310, 0, 2400, 120)))
    }

    @Test
    fun cutoutThatOnlyTouchesTheBottomOfTheBandCounts() {
        assertEquals(HudInsets(left = 90, right = 0), insets(CutoutRect(0, 143, 90, 300)))
    }

    @Test
    fun cutoutThatStartsRightBelowTheBandDoesNotCount() {
        assertEquals(HudInsets.None, insets(CutoutRect(0, 144, 90, 300)))
    }

    @Test
    fun cutoutThatEndsRightAboveTheBandDoesNotCount() {
        assertEquals(HudInsets.None, hudHorizontalInsets(listOf(CutoutRect(0, 0, 90, 80)), 80, 224, width))
    }

    @Test
    fun topCentreNotchAboveTheBandDoesNotCount() {
        // The safe top inset moves the HUD below a notch on the top edge.
        assertEquals(HudInsets.None, hudHorizontalInsets(listOf(CutoutRect(1000, 0, 1400, 80)), 80, 224, width))
    }

    @Test
    fun cutoutsOnBothSidesInTheBandPushBothSidesIn() {
        assertEquals(
            HudInsets(left = 100, right = 60),
            insets(CutoutRect(0, 0, 100, 100), CutoutRect(2340, 0, 2400, 100), CutoutRect(0, 0, 40, 50)),
        )
    }

    @Test
    fun emptyRectIsIgnored() {
        assertEquals(HudInsets.None, insets(CutoutRect(0, 0, 0, 100)))
    }
}
