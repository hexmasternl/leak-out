package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GapWarningTest {
    private val tolerance = 1e-5f

    @Test
    fun `no redness without a gap or far from it`() {
        assertEquals(0f, GapWarning.redness(null), tolerance)
        assertEquals(0f, GapWarning.redness(7.0), tolerance)
        assertEquals(0f, GapWarning.redness(5.0), tolerance)
    }

    @Test
    fun `redness grows linearly from 5 to 3 sections`() {
        assertEquals(0.5f, GapWarning.redness(4.0), tolerance)
        assertEquals(0.25f, GapWarning.redness(4.5), tolerance)
    }

    @Test
    fun `fully red at 3 sections and closer`() {
        for (d in listOf(3.0, 2.4, 0.5, 0.0)) assertEquals(1f, GapWarning.redness(d), tolerance)
    }

    @Test
    fun `blinks at 3 sections and closer only`() {
        assertFalse(GapWarning.blinking(null))
        assertFalse(GapWarning.blinking(3.01))
        assertTrue(GapWarning.blinking(3.0))
        assertTrue(GapWarning.blinking(0.1))
    }

    @Test
    fun `the blink starts with Warning and switches every 500 ms`() {
        val since = 10_000L
        assertTrue(GapWarning.showWarningText(2.0, since, since))
        assertTrue(GapWarning.showWarningText(2.0, since + 499, since))
        assertFalse(GapWarning.showWarningText(2.0, since + 500, since))
        assertFalse(GapWarning.showWarningText(2.0, since + 999, since))
        assertTrue(GapWarning.showWarningText(2.0, since + 1_000, since))
        assertFalse(GapWarning.showWarningText(2.0, since + 1_700, since))
    }

    @Test
    fun `no warning text without blinking, and the first frame of blinking shows it`() {
        assertFalse(GapWarning.showWarningText(4.0, 1_000, 0))
        assertFalse(GapWarning.showWarningText(null, 1_000, 0))
        assertTrue(GapWarning.showWarningText(3.0, 1_000, null))
    }

    @Test
    fun `heartbeat factor is 0,15 at 5 sections and 1 at 0`() {
        assertEquals(0.15f, GapWarning.heartbeatFactor(5.0) ?: -1f, tolerance)
        assertEquals(1f, GapWarning.heartbeatFactor(0.0) ?: -1f, tolerance)
        assertEquals(0.83f, GapWarning.heartbeatFactor(1.0) ?: -1f, tolerance)
        assertEquals(0.575f, GapWarning.heartbeatFactor(2.5) ?: -1f, tolerance)
    }

    @Test
    fun `no heartbeat above 5 sections or without a gap`() {
        assertNull(GapWarning.heartbeatFactor(5.01))
        assertNull(GapWarning.heartbeatFactor(null))
    }

    @Test
    fun `sections to the leak are rounded up`() {
        assertEquals(3, GapWarning.sectionsToLeak(2.4))
        assertEquals(3, GapWarning.sectionsToLeak(3.0))
        assertEquals(1, GapWarning.sectionsToLeak(0.2))
        assertEquals(1, GapWarning.sectionsToLeak(0.0))
    }
}
