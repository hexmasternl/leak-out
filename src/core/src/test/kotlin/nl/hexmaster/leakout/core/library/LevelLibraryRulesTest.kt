package nl.hexmaster.leakout.core.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LevelLibraryRulesTest {
    private val config = LevelLibraryConfig(pageUnlockStars = 36)

    private fun library(total: Int, stars: Map<Int, Int>, cfg: LevelLibraryConfig = config) =
        LevelLibraryRules.compute(total, stars, cfg)

    private fun LevelLibrary.state(id: Int): TileState = tile(id)!!.state

    /** Levels [ids] finished, each with [stars]. */
    private fun finished(ids: IntRange, stars: Int = 1): Map<Int, Int> = ids.associateWith { stars }

    @Test
    fun `default threshold is the provisional 36`() {
        assertEquals(36, LevelLibraryConfig.Default.pageUnlockStars)
        assertEquals(LevelLibraryConfig.DEFAULT_PAGE_UNLOCK_STARS, LevelLibraryConfig().pageUnlockStars)
    }

    @Test
    fun `a negative threshold is rejected`() {
        assertFailsWith<IllegalArgumentException> { LevelLibraryConfig(-1) }
    }

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
    fun `no levels means no pages and page 1 to open on`() {
        val lib = library(0, emptyMap())
        assertTrue(lib.pages.isEmpty())
        assertEquals(1, lib.initialPage())
    }

    @Test
    fun `fresh install unlocks only level 1`() {
        val lib = library(48, emptyMap())
        assertEquals(TileState.Unlocked, lib.state(1))
        (2..48).forEach { assertEquals(TileState.Locked, lib.state(it), "level $it") }
        assertEquals(listOf(1), lib.accessiblePages.map { it.number })
    }

    @Test
    fun `finishing a level unlocks the next`() {
        val lib = library(48, mapOf(1 to 1, 2 to 3))
        assertEquals(TileState.Completed(1), lib.state(1))
        assertEquals(TileState.Completed(3), lib.state(2))
        assertEquals(TileState.Unlocked, lib.state(3))
    }

    @Test
    fun `skipping is not possible`() {
        val lib = library(48, mapOf(1 to 3, 2 to 3))
        assertEquals(TileState.Locked, lib.state(4))
    }

    @Test
    fun `a finished level stays unlocked even if its predecessor is not finished`() {
        val lib = library(48, mapOf(5 to 2))
        assertEquals(TileState.Completed(2), lib.state(5))
        assertEquals(TileState.Unlocked, lib.state(6))
        assertEquals(TileState.Locked, lib.state(4))
    }

    @Test
    fun `page stars sum stored stars and count unplayed levels as 0`() {
        val lib = library(48, mapOf(1 to 3, 2 to 2, 3 to 1, 25 to 3))
        assertEquals(6, lib.page(1)!!.earnedStars)
        assertEquals(3, lib.page(2)!!.earnedStars)
    }

    @Test
    fun `threshold reached unlocks the first level of the next page`() {
        // 12 levels with 3 stars = 36, level 24 not finished.
        val lib = library(48, finished(1..12, stars = 3))
        assertEquals(TileState.Unlocked, lib.state(25))
        assertEquals(TileState.Locked, lib.state(26))
        assertTrue(lib.page(2)!!.isAccessible)
    }

    @Test
    fun `threshold not reached keeps the next page locked`() {
        val lib = library(48, finished(1..17, stars = 2)) // 34 stars, level 24 not finished
        assertEquals(TileState.Locked, lib.state(25))
        assertFalse(lib.page(2)!!.isAccessible)
        assertEquals(listOf(1), lib.accessiblePages.map { it.number })
    }

    @Test
    fun `finishing level 24 unlocks level 25 by the sequential rule`() {
        val lib = library(48, finished(1..24, stars = 1)) // 24 stars, below the threshold
        assertEquals(TileState.Unlocked, lib.state(25))
    }

    @Test
    fun `the star gate only opens the first level of a page`() {
        val lib = library(72, finished(1..12, stars = 3))
        assertEquals(TileState.Unlocked, lib.state(25))
        assertEquals(TileState.Locked, lib.state(49))
        assertFalse(lib.page(3)!!.isAccessible)
    }

    @Test
    fun `improving a result raises the page total`() {
        val before = finished(1..11, stars = 3) + (12 to 1) // 34
        assertEquals(TileState.Locked, library(48, before).state(25))
        val after = before + (12 to 3) // 36
        assertEquals(TileState.Unlocked, library(48, after).state(25))
    }

    @Test
    fun `changing the threshold changes when the next page opens`() {
        val stars = finished(1..10, stars = 2) // 20 stars
        assertEquals(TileState.Locked, library(48, stars, LevelLibraryConfig(36)).state(25))
        assertEquals(TileState.Unlocked, library(48, stars, LevelLibraryConfig(20)).state(25))
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
    fun `opens on the page of the lowest unlocked unfinished level`() {
        assertEquals(1, library(48, emptyMap()).initialPage())
        assertEquals(1, library(48, finished(1..5)).initialPage())
        assertEquals(2, library(48, finished(1..24)).initialPage())
    }

    @Test
    fun `opens on the last accessible page when every unlocked level is finished`() {
        assertEquals(2, library(30, finished(1..30)).initialPage())
    }

    @Test
    fun `an unfinished level on an earlier page wins over the next page`() {
        // Page 2 opened by stars, but level 13 on page 1 is still unfinished.
        assertEquals(1, library(48, finished(1..12, stars = 3)).initialPage())
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
