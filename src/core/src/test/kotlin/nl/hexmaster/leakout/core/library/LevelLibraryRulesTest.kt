package nl.hexmaster.leakout.core.library

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LevelLibraryRulesTest {
    private val config = LevelLibraryConfig.Default

    private fun library(total: Int, stars: Map<Int, Int>, cfg: LevelLibraryConfig = config) =
        LevelLibraryRules.compute(total, stars, cfg)

    private fun LevelLibrary.state(id: Int): TileState = tile(id)!!.state

    private fun LevelLibrary.playable(): List<Int> =
        pages.flatMap { it.tiles }.filter { it.isPlayable }.map { it.levelId }

    private fun LibraryPage.playable(): List<Int> = tiles.filter { it.isPlayable }.map { it.levelId }

    /** Levels [ids] finished, each with [stars]. */
    private fun finished(ids: IntRange, stars: Int = 1): Map<Int, Int> = ids.associateWith { stars }

    /** Page 1 at 51 stars (levels 1..17 with 3 stars): enough to open page 2 at the default 50. */
    private val page1Open = finished(1..17, stars = 3)

    @Test
    fun `pages hold 24 levels in order`() {
        val lib = library(48, emptyMap())
        assertEquals(2, lib.pages.size)
        assertEquals((1..24).toList(), lib.pages[0].tiles.map { it.levelId })
        assertEquals((25..48).toList(), lib.pages[1].tiles.map { it.levelId })
    }

    @Test
    fun `the last page is partial`() {
        val lib = library(30, emptyMap())
        assertEquals(2, lib.pages.size)
        assertEquals((25..30).toList(), lib.page(2)!!.tiles.map { it.levelId })
    }

    @Test
    fun `a partial last page shorter than the window opens with all its levels`() {
        val lib = library(26, page1Open)
        assertTrue(lib.page(2)!!.isOpen)
        assertEquals(listOf(25, 26), lib.page(2)!!.playable())
    }

    @Test
    fun `no levels means no pages and page 1 to open on`() {
        val lib = library(0, emptyMap())
        assertTrue(lib.pages.isEmpty())
        assertEquals(1, lib.initialPage())
    }

    @Test
    fun `fresh install unlocks levels 1 to 3`() {
        val lib = library(48, emptyMap())
        assertEquals(listOf(1, 2, 3), lib.playable())
        assertEquals(TileState.Locked, lib.state(4))
        assertEquals(listOf(1), lib.accessiblePages.map { it.number })
    }

    @Test
    fun `finishing a level extends the window`() {
        val lib = library(48, mapOf(1 to 2))
        assertEquals(TileState.Completed(2), lib.state(1))
        assertEquals(listOf(1, 2, 3, 4), lib.playable())
        assertEquals(TileState.Locked, lib.state(5))
    }

    @Test
    fun `skipping levels 2 and 3`() {
        val lib = library(48, mapOf(1 to 3, 4 to 1))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), lib.playable())
        assertEquals(TileState.Unlocked, lib.state(2))
        assertEquals(TileState.Unlocked, lib.state(3))
        assertEquals(TileState.Locked, lib.state(8))
    }

    @Test
    fun `at most two levels in a row can be skipped`() {
        val lib = library(48, mapOf(1 to 3))
        assertEquals(TileState.Locked, lib.state(5))
    }

    @Test
    fun `a finished level stays playable and extends the window even with a gap before it`() {
        val lib = library(48, mapOf(10 to 2))
        assertEquals(TileState.Completed(2), lib.state(10))
        assertEquals(listOf(1, 2, 3, 10, 11, 12, 13), lib.playable())
    }

    @Test
    fun `a 0-star win counts as finished for the window`() {
        val lib = library(48, mapOf(1 to 0))
        assertEquals(TileState.Completed(0), lib.state(1))
        assertEquals(TileState.Unlocked, lib.state(4))
    }

    @Test
    fun `the window does not cross into a closed page`() {
        val lib = library(48, finished(22..24, stars = 3)) // 9 stars on page 1
        assertFalse(lib.page(2)!!.isOpen)
        (25..27).forEach { assertEquals(TileState.Locked, lib.state(it), "level $it") }
    }

    @Test
    fun `a window of one restores sequential unlocking`() {
        val lib = library(48, mapOf(1 to 1, 2 to 1), LevelLibraryConfig(unlockWindow = 1))
        assertEquals(TileState.Unlocked, lib.state(3))
        assertEquals(TileState.Locked, lib.state(4))
        assertEquals(listOf(1, 2, 3), lib.playable())
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, 24])
    fun `on a fresh install exactly the first window levels are playable`(window: Int) {
        val lib = library(48, emptyMap(), LevelLibraryConfig(unlockWindow = window))
        assertEquals((1..window).toList(), lib.playable())
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5])
    fun `a finished level unlocks exactly the window after it`(window: Int) {
        // Level 10 finished, mid-page, so the first-levels rule does not interfere.
        val lib = library(48, mapOf(10 to 1), LevelLibraryConfig(unlockWindow = window))
        assertEquals((1..window).toList() + (10..10 + window), lib.playable())
    }

    @Test
    fun `page stars sum stored stars and count unplayed levels as 0`() {
        val lib = library(48, mapOf(1 to 3, 2 to 2, 3 to 1, 25 to 3))
        assertEquals(6, lib.page(1)!!.earnedStars)
        assertEquals(3, lib.page(2)!!.earnedStars)
    }

    @Test
    fun `threshold reached opens the next page and its first three levels`() {
        val lib = library(48, page1Open)
        assertEquals(51, lib.page(1)!!.earnedStars)
        assertTrue(lib.page(2)!!.isOpen)
        assertTrue(lib.page(2)!!.isAccessible)
        assertEquals(listOf(25, 26, 27), lib.page(2)!!.playable())
    }

    @Test
    fun `exactly the default threshold opens the next page and one less does not`() {
        val at50 = finished(1..16, stars = 3) + (17 to 2)
        assertTrue(library(48, at50).page(2)!!.isOpen)
        val at49 = finished(1..16, stars = 3) + (17 to 1)
        assertFalse(library(48, at49).page(2)!!.isOpen)
    }

    @Test
    fun `threshold not reached keeps the next page closed even with level 24 finished`() {
        val lib = library(48, finished(1..24, stars = 2)) // 48 stars
        assertEquals(TileState.Locked, lib.state(25))
        assertFalse(lib.page(2)!!.isAccessible)
        assertEquals(listOf(1), lib.accessiblePages.map { it.number })
    }

    @Test
    fun `a finished level on a closed page stays playable but the page is not accessible`() {
        val lib = library(48, mapOf(1 to 1, 30 to 3))
        assertEquals(TileState.Completed(3), lib.state(30))
        assertEquals(TileState.Locked, lib.state(25))
        assertEquals(TileState.Locked, lib.state(31))
        assertFalse(lib.page(2)!!.isAccessible)
    }

    @Test
    fun `improving a result raises the page total and opens the next page`() {
        val before = finished(1..16, stars = 3) + (17 to 1) // 49
        assertEquals(TileState.Locked, library(48, before).state(25))
        val after = before + (17 to 3) // 51
        assertEquals(TileState.Unlocked, library(48, after).state(25))
    }

    @Test
    fun `pages open in order`() {
        // Page 2 holds 54 stars from earlier results, but page 1 is below the threshold.
        val stars = finished(1..10, stars = 3) + finished(25..42, stars = 3)
        val lib = library(72, stars)
        assertFalse(lib.page(2)!!.isOpen)
        assertFalse(lib.page(3)!!.isOpen)
        assertEquals(TileState.Locked, lib.state(49))
    }

    @Test
    fun `a further page opens once every page before it reaches the threshold`() {
        val lib = library(72, page1Open + finished(25..41, stars = 3))
        assertTrue(lib.page(3)!!.isOpen)
        assertEquals(TileState.Unlocked, lib.state(49))
    }

    @Test
    fun `changing the threshold changes when the next page opens`() {
        val stars = finished(1..10, stars = 2) // 20 stars
        assertEquals(TileState.Locked, library(48, stars, LevelLibraryConfig(50)).state(25))
        assertEquals(TileState.Unlocked, library(48, stars, LevelLibraryConfig(20)).state(25))
    }

    @Test
    fun `a threshold of 0 opens every page`() {
        val lib = library(72, emptyMap(), LevelLibraryConfig(0))
        assertEquals(listOf(1, 2, 3), lib.accessiblePages.map { it.number })
        assertEquals(listOf(1, 2, 3, 25, 26, 27, 49, 50, 51), lib.playable())
    }

    @Test
    fun `stars outside 0 to 3 are clamped`() {
        val lib = library(24, mapOf(1 to 7, 2 to -1))
        assertEquals(TileState.Completed(3), lib.state(1))
        assertEquals(TileState.Completed(0), lib.state(2))
        assertEquals(3, lib.page(1)!!.earnedStars)
    }

    @Test
    fun `records for levels that do not exist are ignored`() {
        val lib = library(24, mapOf(1 to 3, 99 to 3))
        assertEquals(3, lib.page(1)!!.earnedStars)
        assertEquals(1, lib.pages.size)
    }

    @Test
    fun `opens on page 1 with nothing finished`() {
        assertEquals(1, library(48, emptyMap()).initialPage())
    }

    @Test
    fun `opens on the page of the level after the highest finished`() {
        assertEquals(1, library(48, finished(1..5)).initialPage())
    }

    @Test
    fun `returning after a full page with enough stars opens page 2`() {
        assertEquals(2, library(48, finished(1..24, stars = 3)).initialPage())
    }

    @Test
    fun `a skipped level does not pull the library back`() {
        val stars = (finished(1..24, stars = 3) - 2) + finished(25..30, stars = 2)
        val lib = library(72, stars)
        assertEquals(TileState.Unlocked, lib.state(2))
        assertEquals(2, lib.initialPage())
    }

    @Test
    fun `next level on a closed page opens the last open page`() {
        assertEquals(1, library(48, finished(1..24, stars = 1)).initialPage())
    }

    @Test
    fun `a locked next level falls back to the last open page`() {
        // Highest finished is 30 on closed page 2; level 31 is locked, so page 1 is shown.
        assertEquals(1, library(48, mapOf(1 to 1, 30 to 3)).initialPage())
    }

    @Test
    fun `opens on the last page when every level is finished`() {
        assertEquals(2, library(30, finished(1..30, stars = 3)).initialPage())
    }

    @Test
    fun `layout helpers map levels to pages`() {
        assertEquals(1, LevelLibraryLayout.pageOf(1))
        assertEquals(1, LevelLibraryLayout.pageOf(24))
        assertEquals(2, LevelLibraryLayout.pageOf(25))
        assertEquals(25, LevelLibraryLayout.firstLevelOf(2))
        assertEquals(0, LevelLibraryLayout.pageCount(0))
        assertEquals(2, LevelLibraryLayout.pageCount(25))
    }
}
