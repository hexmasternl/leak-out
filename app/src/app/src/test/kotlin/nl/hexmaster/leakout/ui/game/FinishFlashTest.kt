package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinishFlashTest {
    @Test
    fun `the ring fades in and swells from the start`() {
        val start = finishFlashFrame(0)
        assertEquals(0f, start.ringAlpha)
        assertEquals(1f, start.ringScale)
        val soon = finishFlashFrame(100)
        assertTrue(soon.ringAlpha > start.ringAlpha)
        assertTrue(soon.ringScale > start.ringScale)
    }

    @Test
    fun `the scale peaks at about 1_3 within the effect`() {
        val peak = (0 until WinTiming.FINISH_EFFECT_MS).maxOf { finishFlashFrame(it).ringScale }
        assertEquals(1.3f, peak, 0.01f)
        assertEquals(1.3f, finishFlashFrame(WinTiming.FINISH_EFFECT_MS / 2).ringScale, 0.01f)
    }

    @Test
    fun `alpha stays within 0 and 1`() {
        for (age in 0 until WinTiming.FINISH_EFFECT_MS) {
            val alpha = finishFlashFrame(age).ringAlpha
            assertTrue(alpha in 0f..1f)
        }
    }

    @Test
    fun `after the effect the ring is steady`() {
        assertEquals(FinishRing, finishFlashFrame(WinTiming.FINISH_EFFECT_MS))
        assertEquals(FinishRing, finishFlashFrame(60_000))
        assertEquals(FinishFlashFrame(1f, 1f), FinishRing)
    }

    @Test
    fun `the end of the flash meets the steady ring`() {
        val last = finishFlashFrame(WinTiming.FINISH_EFFECT_MS - 1)
        assertEquals(1f, last.ringAlpha, 0.01f)
        assertEquals(1f, last.ringScale, 0.01f)
    }
}
