package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.ui.game.HudModel.Threshold
import nl.hexmaster.leakout.ui.game.HudModel.Warning
import org.junit.Assert.assertEquals
import org.junit.Test

class HudModelTest {
    private val stars = StarBenchmarks(100, 200, 350)

    @Test
    fun `speed is cells per second times 10, halves rounded up`() {
        assertEquals(2, HudModel.speedNumber(0.2))
        assertEquals(8, HudModel.speedNumber(0.8))
        assertEquals(8, HudModel.speedNumber(0.82))
        assertEquals(9, HudModel.speedNumber(0.85))
        assertEquals(9, HudModel.speedNumber(0.9))
        assertEquals(13, HudModel.speedNumber(1.25))
    }

    @Test
    fun `countdown fraction is remaining over the start delay`() {
        assertEquals(1f, HudModel.countdownFraction(10_000, 10_000), 0.0001f)
        assertEquals(0.4f, HudModel.countdownFraction(4_000, 10_000), 0.0001f)
        assertEquals(0.6f, HudModel.countdownFraction(6_000, 10_000), 0.0001f)
        assertEquals(0f, HudModel.countdownFraction(0, 10_000), 0.0001f)
    }

    @Test
    fun `countdown fraction is 0 without a start delay`() {
        assertEquals(0f, HudModel.countdownFraction(0, 0), 0f)
    }

    @Test
    fun `countdown warns in brass from 5 s and red from 2 s`() {
        assertEquals(Warning.Calm, HudModel.countdownWarning(8_000))
        assertEquals(Warning.Calm, HudModel.countdownWarning(5_001))
        assertEquals(Warning.Brass, HudModel.countdownWarning(5_000))
        assertEquals(Warning.Brass, HudModel.countdownWarning(2_001))
        assertEquals(Warning.Red, HudModel.countdownWarning(2_000))
        assertEquals(Warning.Red, HudModel.countdownWarning(1_500))
    }

    @Test
    fun `countdown seconds round up`() {
        assertEquals(7, HudModel.countdownSeconds(6_200))
        assertEquals(6, HudModel.countdownSeconds(6_000))
        assertEquals(1, HudModel.countdownSeconds(1))
        assertEquals(0, HudModel.countdownSeconds(0))
    }

    @Test
    fun `score fraction is the score over three stars, full past it`() {
        assertEquals(0f, HudModel.scoreFraction(0, stars), 0.0001f)
        assertEquals(0.5f, HudModel.scoreFraction(175, stars), 0.0001f)
        assertEquals(1f, HudModel.scoreFraction(350, stars), 0.0001f)
        assertEquals(1f, HudModel.scoreFraction(500, stars), 0.0001f)
    }

    @Test
    fun `markers sit at 2 of 7 and 4 of 7`() {
        val markers = HudModel.markerFractions(stars)
        assertEquals(2f / 7, markers[0], 0.0001f)
        assertEquals(4f / 7, markers[1], 0.0001f)
    }

    @Test
    fun `thresholds passed by a fill change`() {
        fun passed(from: Int, to: Int) =
            HudModel.thresholdsPassed(HudModel.scoreFraction(from, stars), HudModel.scoreFraction(to, stars), stars)
        assertEquals(listOf(Threshold.One), passed(90, 100))
        assertEquals(listOf(Threshold.Two, Threshold.Three), passed(190, 360))
        assertEquals(emptyList<Threshold>(), passed(360, 400))
        assertEquals(emptyList<Threshold>(), passed(100, 150))
        assertEquals(listOf(Threshold.One, Threshold.Two, Threshold.Three), passed(0, 350))
    }
}
