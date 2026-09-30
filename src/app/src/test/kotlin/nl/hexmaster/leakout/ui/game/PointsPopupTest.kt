package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PointsPopupTest {
    private fun frame(ageMs: Long): PopupFrame = popupFrame(ageMs).also { assertNotNull("frame at $ageMs", it) }!!

    @Test
    fun `timing adds up to 1000 ms`() {
        assertEquals(1_000L, PopupTiming.TOTAL_MS)
    }

    @Test
    fun `starts transparent at 80 percent size`() {
        val start = frame(0)
        assertEquals(0f, start.alpha, 1e-4f)
        assertEquals(0.8f, start.scale, 1e-4f)
        assertEquals(0f, start.rise, 1e-4f)
    }

    @Test
    fun `fully visible at full size after the fade-in`() {
        val shown = frame(150)
        assertEquals(1f, shown.alpha, 1e-4f)
        assertEquals(1f, shown.scale, 1e-4f)
    }

    @Test
    fun `fades in and grows during the first 150 ms`() {
        val mid = frame(75)
        assertTrue(mid.alpha > 0f && mid.alpha < 1f)
        assertTrue(mid.scale > 0.8f && mid.scale < 1f)
    }

    @Test
    fun `stays fully visible through the hold`() {
        for (age in 150L..650L step 25) assertEquals("age $age", 1f, frame(age).alpha, 1e-4f)
    }

    @Test
    fun `fades out towards 0 after the hold`() {
        val alphas = (650L..999L step 25).map { frame(it).alpha }
        alphas.zipWithNext().forEach { (a, b) -> assertTrue("alpha falls: $a then $b", b <= a) }
        assertTrue(frame(999).alpha < 0.05f)
        assertTrue(frame(800).alpha < 1f)
    }

    @Test
    fun `rises from 0 to a quarter tile and never goes down`() {
        val rises = (0L..999L).map { frame(it).rise }
        rises.zipWithNext().forEach { (a, b) -> assertTrue(b >= a) }
        assertEquals(0f, rises.first(), 1e-4f)
        assertTrue(rises.last() <= 0.25f && rises.last() > 0.24f)
    }

    @Test
    fun `gone at 1000 ms and later`() {
        assertNull(popupFrame(1_000))
        assertNull(popupFrame(1_001))
        assertNull(popupFrame(60_000))
    }

    @Test
    fun `alpha and scale stay within their ranges`() {
        for (age in -50L..999L) {
            val f = frame(age)
            assertTrue("alpha $age", f.alpha in 0f..1f)
            assertTrue("scale $age", f.scale in 0.8f..1f)
            assertTrue("rise $age", f.rise in 0f..0.25f)
        }
    }

    @Test
    fun `points text has a plus sign`() {
        assertEquals("+10", PointsFormat.text(10))
        assertEquals("+15", PointsFormat.text(15))
        assertEquals("+25", PointsFormat.text(25))
        assertEquals("+50", PointsFormat.text(50))
    }
}
