package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.scores.online.ScorePostStatus.Failed
import nl.hexmaster.leakout.scores.online.ScorePostStatus.InProgress
import nl.hexmaster.leakout.scores.online.ScorePostStatus.Succeeded
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The status dot's blinks: yellow while sending, one green blink, three red blinks (spec `online-score-submission`). */
class ScorePostDotTest {
    private fun on(frame: DotFrame) = frame.alpha == 1f && !frame.done

    @Test
    fun `in progress blinks yellow for as long as it lasts`() {
        for (ms in listOf(0L, 399L, 800L, 60_000L)) {
            val frame = ScorePostDot.frame(InProgress, ms)
            assertEquals(ScorePostDot.Progress, frame.color)
            assertTrue("$ms", on(frame))
        }
        for (ms in listOf(400L, 799L, 1_200L)) {
            assertEquals("$ms", ScorePostDot.DIMMED, ScorePostDot.frame(InProgress, ms).alpha)
        }
        assertFalse(ScorePostDot.frame(InProgress, 600_000L).done)
    }

    @Test
    fun `success blinks green once`() {
        assertEquals(ScorePostDot.Success, ScorePostDot.frame(Succeeded, 0).color)
        assertTrue(on(ScorePostDot.frame(Succeeded, 0)))
        assertTrue(on(ScorePostDot.frame(Succeeded, 299)))
        assertEquals(0f, ScorePostDot.frame(Succeeded, 300).alpha)
        assertFalse(ScorePostDot.frame(Succeeded, 599).done)
        assertTrue(ScorePostDot.frame(Succeeded, 600).done)
    }

    @Test
    fun `failure blinks red three times`() {
        val blinks = (0L until 1_200L step 50).map { ScorePostDot.frame(Failed, it) }
        assertTrue(blinks.all { it.color == ScorePostDot.Failure && !it.done })
        // Count the switches to on: one per blink.
        val ons = blinks.map { it.alpha == 1f }.zipWithNext().count { (before, now) -> !before && now } + 1
        assertEquals(3, ons)
        assertTrue(on(ScorePostDot.frame(Failed, 800)))
        assertEquals(0f, ScorePostDot.frame(Failed, 1_000).alpha)
        assertTrue(ScorePostDot.frame(Failed, 1_200).done)
    }
}
