package nl.hexmaster.leakout.ui.splash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashTimingTest {

    @Test
    fun freshSplashWaitsTheFullMinimum() {
        assertEquals(1_200L, SplashTiming.remainingDelayMs(0L))
    }

    @Test
    fun partlyElapsedSplashWaitsOnlyTheRest() {
        assertEquals(700L, SplashTiming.remainingDelayMs(500L))
        assertEquals(1L, SplashTiming.remainingDelayMs(1_199L))
    }

    @Test
    fun splashThatAlreadyLastedTheMinimumDoesNotWait() {
        assertEquals(0L, SplashTiming.remainingDelayMs(1_200L))
        assertEquals(0L, SplashTiming.remainingDelayMs(60_000L))
    }

    @Test
    fun clockGoingBackwardsNeverExtendsTheMinimum() {
        assertEquals(1_200L, SplashTiming.remainingDelayMs(-250L))
    }

    @Test
    fun crossfadeMatchesMotionPop() {
        assertEquals(320, SplashTiming.CROSSFADE_MS)
    }
}

class SplashLayoutTest {

    @Test
    fun smallLandscapeIsLimitedByHeight() {
        // 640x320: min(96, 140.8) = 96.
        assertEquals(96f, SplashLayout.wordmarkSizeDp(640f, 320f), 0.001f)
    }

    @Test
    fun narrowScreenIsLimitedByWidth() {
        // 400x400: min(120, 88) = 88.
        assertEquals(88f, SplashLayout.wordmarkSizeDp(400f, 400f), 0.001f)
    }

    @Test
    fun neverSmallerThanDisplayHero() {
        assertEquals(56f, SplashLayout.wordmarkSizeDp(200f, 120f), 0.001f)
    }

    @Test
    fun twoLinesFitCommonLandscapeSizes() {
        // Two lines at a 0.95 line height must stay within the screen height with room to spare.
        val sizes = listOf(480f to 260f, 640f to 320f, 800f to 360f, 915f to 412f, 1280f to 800f)
        for ((w, h) in sizes) {
            val size = SplashLayout.wordmarkSizeDp(w, h)
            assertTrue("${w}x$h", size * 2 * 0.95f < h * 0.7f)
        }
    }
}
