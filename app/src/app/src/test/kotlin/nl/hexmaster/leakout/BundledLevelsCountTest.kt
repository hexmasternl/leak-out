package nl.hexmaster.leakout

import org.junit.Assert.assertEquals
import org.junit.Test

class BundledLevelsCountTest {
    @Test
    fun `counts consecutive levels from 1`() {
        assertEquals(3, BundledLevels.contiguousCount(listOf("level_002.json", "level_001.json", "level_003.json")))
    }

    @Test
    fun `a gap ends the count and other files are ignored`() {
        assertEquals(1, BundledLevels.contiguousCount(listOf("level_001.json", "level_003.json", "readme.txt")))
    }

    @Test
    fun `no level 1 means no levels`() {
        assertEquals(0, BundledLevels.contiguousCount(listOf("level_002.json")))
        assertEquals(0, BundledLevels.contiguousCount(emptyList()))
    }
}
