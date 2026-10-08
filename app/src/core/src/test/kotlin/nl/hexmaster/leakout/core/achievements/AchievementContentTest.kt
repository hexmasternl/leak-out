package nl.hexmaster.leakout.core.achievements

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import nl.hexmaster.leakout.core.library.LevelLibraryLayout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Every progress milestone must be reachable with the levels that ship, so a tier cannot be
 * published before its levels exist, and pack achievements only exist for full library pages.
 */
class AchievementContentTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("incremental")
    fun `the milestone fits the bundled levels`(achievement: Achievement) {
        val type = achievement.type as AchievementType.Incremental
        val count = bundledIds.size
        when (type.measure) {
            Measure.LEVELS_FINISHED, Measure.THREE_STAR_LEVELS ->
                assertTrue(type.steps <= count, "${achievement.key} needs ${type.steps} levels, $count are bundled")
            Measure.STARS -> {
                val max = count * LevelLibraryLayout.MAX_STARS_PER_LEVEL
                assertTrue(type.steps <= max, "${achievement.key} needs ${type.steps} stars, $max can be earned")
            }
            Measure.PACK_LEVELS_FINISHED, Measure.PACK_THREE_STAR_LEVELS -> {
                val page = checkNotNull(achievement.pack)
                val first = LevelLibraryLayout.firstLevelOf(page)
                val levels = (first until first + LevelLibraryLayout.LEVELS_PER_PAGE).toSet()
                assertTrue(bundledIds.containsAll(levels), "${achievement.key}: library page $page is not full")
                assertEquals(LevelLibraryLayout.LEVELS_PER_PAGE, type.steps, "${achievement.key} counts a whole page")
            }
            Measure.SECTIONS_PLACED -> Unit // Not tied to content: any level takes placements.
        }
    }

    @Test
    fun `every bundled level with 3 stars unlocks every progress achievement`() {
        val evaluation = evaluateAttempt(null, history = history(bundledIds, stars = 3))
        val progress = Achievement.entries.filter { it.group == AchievementGroup.PROGRESS }
        assertTrue(evaluation.newlyUnlocked.containsAll(progress), "missing ${progress - evaluation.newlyUnlocked.toSet()}")
    }

    @Test
    fun `pack achievements count the levels of their page by the library layout`() {
        for (achievement in Achievement.entries.filter { it.pack != null }) {
            val page = checkNotNull(achievement.pack)
            val onPage = bundledIds.filter { LevelLibraryLayout.pageOf(it) == page }
            val offPage = bundledIds.filter { LevelLibraryLayout.pageOf(it) != page }
            val evaluation = evaluateAttempt(null, history = history(offPage, stars = 3) + history(onPage.drop(1), stars = 3))
            assertEquals(onPage.size - 1, evaluation.steps[achievement], achievement.key)
        }
    }

    @Test
    fun `a library that grows by a page changes no progress`() {
        // The rules take no library size: the same history gives the same result with 24 or 48 levels shipped.
        val tenFinished = history(1..10, stars = 2)
        val evaluation = evaluateAttempt(null, history = tenFinished)
        assertTrue(Achievement.LEVELS_FINISHED_1 in evaluation.newlyUnlocked)
        assertEquals(10, evaluation.steps[Achievement.PACK_1_COMPLETE])
        assertEquals(24, Achievement.PACK_1_COMPLETE.steps)
    }

    companion object {
        @JvmStatic
        fun incremental(): List<Achievement> = Achievement.entries.filter { it.isIncremental }

        private val bundledIds: List<Int> by lazy {
            val dir = File(System.getProperty("leakout.levelsDir") ?: fail("System property leakout.levelsDir is not set"))
            dir.listFiles { file -> file.name.matches(Regex("""level_\d{3}\.json""")) }.orEmpty()
                .map { it.name.substring(6, 9).toInt() }
                .sorted()
                .also { ids -> assertEquals((1..ids.size).toList(), ids, "bundled level ids must run from 1 without gaps") }
        }
    }
}
