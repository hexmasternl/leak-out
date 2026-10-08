package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class LeakSplashSpecTest {

    @Test
    fun `six droplets fly out once in 600 ms`() {
        assertEquals(6, LeakSplashSpec.DROPLETS)
        assertEquals(600, LeakSplashSpec.DURATION_MS)
    }

    @Test
    fun `droplets start at the blob and rest at the edge in six directions`() {
        for (index in 0 until LeakSplashSpec.DROPLETS) {
            assertEquals(0f, LeakSplashSpec.dropletOffset(index, 0f).getDistance(), 1e-6f)
            assertEquals(0.8f, LeakSplashSpec.dropletOffset(index, 1f).getDistance(), 1e-4f)
            // Past the end they stay at rest.
            assertEquals(LeakSplashSpec.dropletOffset(index, 1f), LeakSplashSpec.dropletOffset(index, 2f))
        }
        val directions = (0 until LeakSplashSpec.DROPLETS).map { LeakSplashSpec.dropletOffset(it, 1f) }
        assertEquals(6, directions.map { (it.x * 1000).toInt() to (it.y * 1000).toInt() }.toSet().size)
    }
}
