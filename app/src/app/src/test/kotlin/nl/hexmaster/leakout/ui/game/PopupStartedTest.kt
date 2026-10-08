package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.engine.Award
import nl.hexmaster.leakout.core.level.Position
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the newest award says a points popup started, which plays the drop. */
class PopupStartedTest {
    private val straight = Award(Position(1, 0), 10, atMs = 11_250)
    private val cross = Award(Position(2, 0), 20, atMs = 12_500)
    private val crossAgain = Award(Position(2, 0), 20, atMs = 14_500)

    @Test
    fun `the first award of an attempt starts a popup`() {
        assertTrue(popupStarted(NewestAward(1, null), NewestAward(1, straight)))
    }

    @Test
    fun `a newer award starts a popup`() {
        assertTrue(popupStarted(NewestAward(1, straight), NewestAward(1, cross)))
    }

    @Test
    fun `a cross scoring again on the same tile starts another popup`() {
        assertTrue(popupStarted(NewestAward(1, cross), NewestAward(1, crossAgain)))
    }

    @Test
    fun `the same newest award starts nothing`() {
        assertFalse(popupStarted(NewestAward(1, cross), NewestAward(1, cross.copy())))
    }

    @Test
    fun `awards ageing out start nothing`() {
        assertFalse(popupStarted(NewestAward(1, cross), NewestAward(1, null)))
    }

    @Test
    fun `a restart starts nothing`() {
        assertFalse(popupStarted(NewestAward(1, cross), NewestAward(2, null)))
        assertFalse(popupStarted(NewestAward(1, null), NewestAward(2, straight)))
    }
}
