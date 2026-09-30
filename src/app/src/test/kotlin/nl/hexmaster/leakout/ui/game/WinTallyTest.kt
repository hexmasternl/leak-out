package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.score.FinalScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WinTallyTest {
    private fun score(earned: Int, unused: Int) =
        FinalScore(earned, List(unused) { Position(it % 10, it / 10) }, (earned - 3 * unused).coerceAtLeast(0))

    // 120 earned, 4 unused: penalty from 500 to 980, total from 980 to 1780, stars from 1780.
    private val four = score(120, 4)

    @Test
    fun `the dialog opens on the earned points alone`() {
        val frame = winFrame(0, four, stars = 2)
        assertEquals(WinFrame(0, 0, 0, 0, listOf(1f, 1f, 1f), buttonsEnabled = false), frame)
    }

    @Test
    fun `a negative age is the opening frame`() {
        assertEquals(winFrame(0, four, 2), winFrame(-300, four, 2))
    }

    @Test
    fun `the earned hold keeps the penalty at 0`() {
        val frame = winFrame(WinTiming.EARNED_HOLD_MS - 1, four, 2)
        assertEquals(0, frame.sectionsRemoved)
        assertEquals(0, frame.penaltyShown)
        assertEquals(0, frame.totalShown)
    }

    @Test
    fun `the penalty grows one section per step in step with the removed sections`() {
        var previous = 0
        for (age in 0L..1_000L) {
            val frame = winFrame(age, four, 2)
            assertTrue(frame.sectionsRemoved - previous in 0..1)
            assertEquals(frame.sectionsRemoved * 3, frame.penaltyShown)
            previous = frame.sectionsRemoved
        }
        assertEquals(1, winFrame(620, four, 2).sectionsRemoved)
        assertEquals(2, winFrame(740, four, 2).sectionsRemoved)
        assertEquals(3, winFrame(979, four, 2).sectionsRemoved)
        assertEquals(4, winFrame(980, four, 2).sectionsRemoved)
        assertEquals(12, winFrame(980, four, 2).penaltyShown)
    }

    @Test
    fun `the penalty phase takes 120 ms per section and at most 2 seconds`() {
        assertEquals(4 * 120L, WinTally.totalStartMs(four) - WinTally.penaltyStartMs())
        val forty = score(300, 40)
        assertEquals(2_000L, WinTally.totalStartMs(forty) - WinTally.penaltyStartMs())
        assertEquals(40, winFrame(WinTally.totalStartMs(forty), forty, 0).sectionsRemoved)
        assertEquals(39, winFrame(WinTally.totalStartMs(forty) - 1, forty, 0).sectionsRemoved)
        val thirty = score(300, 30)
        assertTrue(WinTally.totalStartMs(thirty) - WinTally.penaltyStartMs() <= WinTiming.PENALTY_MAX_MS)
    }

    @Test
    fun `without unused sections the total starts right after the earned hold`() {
        val none = score(150, 0)
        assertEquals(WinTiming.EARNED_HOLD_MS, WinTally.totalStartMs(none))
        assertEquals(0, winFrame(WinTiming.EARNED_HOLD_MS, none, 2).totalShown)
        assertTrue(winFrame(WinTiming.EARNED_HOLD_MS + 100, none, 2).totalShown > 0)
        assertEquals(0, winFrame(5_000, none, 2).penaltyShown)
    }

    @Test
    fun `the total counts up monotonically and ends exactly on the total`() {
        var previous = 0
        for (age in 0L..3_000L) {
            val shown = winFrame(age, four, 1).totalShown
            assertTrue("total went down at $age", shown >= previous)
            assertTrue(shown <= 108)
            previous = shown
        }
        assertTrue(winFrame(1_779, four, 1).totalShown < 108)
        assertEquals(108, winFrame(1_780, four, 1).totalShown)
        // Ease-out: more than half the total in the first half of the phase.
        assertTrue(winFrame(980 + 400, four, 1).totalShown > 54)
    }

    @Test
    fun `the earned stars light up 300 ms apart and pop`() {
        val start = WinTally.starsStartMs(four)
        assertEquals(0, winFrame(start - 1, four, 2).starsLit)
        assertEquals(1, winFrame(start, four, 2).starsLit)
        assertEquals(1, winFrame(start + 299, four, 2).starsLit)
        assertEquals(2, winFrame(start + 300, four, 2).starsLit)
        assertEquals(2, winFrame(start + 5_000, four, 2).starsLit)

        val popping = winFrame(start + 160, four, 2).starScales
        assertTrue(popping[0] > 1.3f)
        assertEquals(1f, popping[1])
        assertEquals(1f, winFrame(start + WinTiming.STAR_POP_MS, four, 2).starScales[0])
        assertTrue(winFrame(start + 300 + 100, four, 2).starScales[1] > 1f)
        assertEquals(1f, winFrame(start + 300 + WinTiming.STAR_POP_MS, four, 2).starScales[1])
    }

    @Test
    fun `missed stars stay unlit at scale 1`() {
        val start = WinTally.starsStartMs(four)
        for (age in start..start + 2_000) {
            val frame = winFrame(age, four, 1)
            assertTrue(frame.starsLit <= 1)
            assertEquals(1f, frame.starScales[1])
            assertEquals(1f, frame.starScales[2])
        }
    }

    @Test
    fun `the buttons are enabled when the last star has popped`() {
        val end = WinTally.totalDurationMs(four, 2)
        assertEquals(WinTally.starsStartMs(four) + 300 + 320, end)
        assertFalse(winFrame(end - 1, four, 2).buttonsEnabled)
        assertTrue(winFrame(end, four, 2).buttonsEnabled)
        val done = winFrame(end, four, 2)
        assertEquals(WinFrame(4, 12, 108, 2, listOf(1f, 1f, 1f), true), done)
    }

    @Test
    fun `with no star the buttons are enabled right after the total`() {
        val poor = score(10, 5)
        val end = WinTally.totalDurationMs(poor, 0)
        assertEquals(WinTally.starsStartMs(poor), end)
        assertFalse(winFrame(end - 1, poor, 0).buttonsEnabled)
        assertTrue(winFrame(end, poor, 0).buttonsEnabled)
        assertEquals(0, winFrame(end, poor, 0).totalShown)
        assertEquals(15, winFrame(end, poor, 0).penaltyShown)
        assertEquals(0, winFrame(end, poor, 0).starsLit)
    }

    @Test
    fun `the frame does not change without time passing`() {
        assertEquals(winFrame(1_234, four, 3), winFrame(1_234, four, 3))
    }
}
