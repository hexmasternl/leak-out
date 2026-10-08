package nl.hexmaster.leakout.core.library

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LevelLibraryConfigTest {
    @Test
    fun `defaults are 50 stars and a window of 3`() {
        assertEquals(50, LevelLibraryConfig.Default.pageUnlockStars)
        assertEquals(3, LevelLibraryConfig.Default.unlockWindow)
        assertEquals(LevelLibraryConfig.DEFAULT_PAGE_UNLOCK_STARS, LevelLibraryConfig().pageUnlockStars)
        assertEquals(LevelLibraryConfig.DEFAULT_UNLOCK_WINDOW, LevelLibraryConfig().unlockWindow)
        assertEquals(72, LevelLibraryConfig.MAX_PAGE_STARS)
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 73, Int.MIN_VALUE, Int.MAX_VALUE])
    fun `a threshold outside 0 to 72 is rejected`(threshold: Int) {
        assertFailsWith<IllegalArgumentException> { LevelLibraryConfig(pageUnlockStars = threshold) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 50, 72])
    fun `a threshold in 0 to 72 is accepted`(threshold: Int) {
        assertEquals(threshold, LevelLibraryConfig(pageUnlockStars = threshold).pageUnlockStars)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `a window below 1 is rejected`(window: Int) {
        assertFailsWith<IllegalArgumentException> { LevelLibraryConfig(unlockWindow = window) }
    }

    @Test
    fun `a window of 1 is accepted`() {
        assertEquals(1, LevelLibraryConfig(unlockWindow = 1).unlockWindow)
    }
}
