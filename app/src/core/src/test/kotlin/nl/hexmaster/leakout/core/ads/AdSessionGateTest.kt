package nl.hexmaster.leakout.core.ads

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdSessionGateTest {
    @Test
    fun `a start from the menu with an ad ready shows the ad`() {
        assertTrue(AdSessionGate.shouldShow(fromMenu = true, adReady = true))
    }

    @Test
    fun `a start from the menu without a ready ad shows nothing`() {
        assertFalse(AdSessionGate.shouldShow(fromMenu = true, adReady = false))
    }

    @Test
    fun `moving from level to level never shows an ad, even with one ready`() {
        assertFalse(AdSessionGate.shouldShow(fromMenu = false, adReady = true))
    }

    @Test
    fun `nothing shows when it is neither a menu start nor ready`() {
        assertFalse(AdSessionGate.shouldShow(fromMenu = false, adReady = false))
    }
}
