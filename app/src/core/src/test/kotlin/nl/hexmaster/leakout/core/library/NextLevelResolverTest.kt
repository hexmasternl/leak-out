package nl.hexmaster.leakout.core.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NextLevelResolverTest {
    private val config = LevelLibraryConfig.Default

    private fun resolve(total: Int, stars: Map<Int, Int>) = NextLevelResolver.resolve(total, stars, config)

    /** Levels [ids] finished, each with [stars]. */
    private fun finished(ids: IntRange, stars: Int = 1): Map<Int, Int> = ids.associateWith { stars }

    @Test
    fun `fresh install targets level 1 on page 1`() {
        val next = resolve(48, emptyMap())
        assertEquals(NextLevel(levelId = 1, page = 1, isUnlocked = true, highestCompleted = 0), next)
        assertEquals(ContinueTarget.StartLevel(1), next.continueTarget)
    }

    @Test
    fun `linear progress targets the level after the highest completed`() {
        val next = resolve(48, finished(1..7, stars = 2))
        assertEquals(8, next.levelId)
        assertEquals(1, next.page)
        assertEquals(ContinueTarget.StartLevel(8), next.continueTarget)
    }

    @Test
    fun `gaps in the history follow the highest completed level`() {
        val next = resolve(48, mapOf(1 to 3, 2 to 1, 5 to 2))
        assertEquals(6, next.levelId)
        assertEquals(5, next.highestCompleted)
        assertEquals(ContinueTarget.StartLevel(6), next.continueTarget)
    }

    @Test
    fun `a 0-star win counts as completed`() {
        val next = resolve(48, mapOf(1 to 2, 2 to 0))
        assertEquals(3, next.levelId)
        assertTrue(next.isUnlocked)
    }

    @Test
    fun `page boundary moves from page 1 to page 2`() {
        assertEquals(1, resolve(48, finished(1..23)).page)
        val next = resolve(48, finished(1..24))
        assertEquals(25, next.levelId)
        assertEquals(2, next.page)
    }

    @Test
    fun `highest completed 30 targets 31 on page 2`() {
        val next = resolve(72, finished(1..30, stars = 3))
        assertEquals(31, next.levelId)
        assertEquals(2, next.page)
    }

    @Test
    fun `all levels completed targets the last level`() {
        val next = resolve(48, finished(1..48, stars = 3))
        assertEquals(48, next.levelId)
        assertEquals(2, next.page)
        assertEquals(ContinueTarget.StartLevel(48), next.continueTarget)
    }

    @Test
    fun `history beyond the level count is ignored`() {
        val next = resolve(10, finished(1..3) + (99 to 3))
        assertEquals(4, next.levelId)
    }

    @Test
    fun `the next level is locked behind the star gate even with level 24 finished`() {
        // 48 stars on page 1 is below the 50 needed, so page 2 stays closed.
        val next = resolve(48, finished(1..24, stars = 2))
        assertEquals(25, next.levelId)
        assertFalse(next.isUnlocked)
        assertEquals(ContinueTarget.OpenLibrary(1), next.continueTarget)
    }

    @Test
    fun `the next level on page 2 is unlocked once page 1 reaches the star gate`() {
        val next = resolve(48, finished(1..24, stars = 3))
        assertEquals(25, next.levelId)
        assertTrue(next.isUnlocked)
        assertEquals(ContinueTarget.StartLevel(25), next.continueTarget)
    }

    @Test
    fun `the next level is reachable after skipping levels`() {
        val next = resolve(48, mapOf(1 to 3, 4 to 2))
        assertEquals(5, next.levelId)
        assertTrue(next.isUnlocked)
        assertEquals(ContinueTarget.StartLevel(5), next.continueTarget)
    }

    @Test
    fun `a window of one keeps continue sequential`() {
        val sequential = LevelLibraryConfig(unlockWindow = 1)
        assertTrue(NextLevelResolver.resolve(48, mapOf(1 to 3), sequential).isUnlocked)
        assertEquals(2, NextLevelResolver.resolve(48, mapOf(1 to 3), sequential).levelId)
    }

    @Test
    fun `a locked next level sends continue to the library on the page of the highest completed`() {
        val next = NextLevel(levelId = 25, page = 2, isUnlocked = false, highestCompleted = 24)
        assertEquals(ContinueTarget.OpenLibrary(1), next.continueTarget)
        val beyond = NextLevel(levelId = 31, page = 2, isUnlocked = false, highestCompleted = 30)
        assertEquals(ContinueTarget.OpenLibrary(2), beyond.continueTarget)
    }

    @Test
    fun `no levels at all cannot be started and falls back to page 1`() {
        val next = resolve(0, emptyMap())
        assertFalse(next.isUnlocked)
        assertEquals(ContinueTarget.OpenLibrary(1), next.continueTarget)
    }
}
