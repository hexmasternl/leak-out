package nl.hexmaster.leakout.core.achievements

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.engine.GameEngine
import nl.hexmaster.leakout.core.engine.GameState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.library.LevelLibraryLayout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Pack 1 Perfect and Perfectionist I need 3 stars on library page 1, so every level on that page
 * must be able to reach its `stars.three` (10 points per free cell) through the engine. Each level
 * is played with a long route that sweeps the grid ([ScriptedPlay.longPath]: more sections the
 * liquid leaves, more points), built during the countdown with a forced stack, as
 * AchievementReachabilityTest does for Big Spender.
 */
class ThreeStarReachabilityTest {
    private val scripted = ScriptedPlay(GameEngine(Random(5)))

    private fun longWin(id: Int): GameState {
        val level = ScriptedPlay.bundled(id)
        return scripted.play(scripted.build(scripted.start(id), scripted.routeOf(level, scripted.longPath(level, Random(id)))))
    }

    @Test
    fun `library page 1 is full`() {
        assertEquals(LevelLibraryLayout.LEVELS_PER_PAGE, pageOne().size)
    }

    @ParameterizedTest(name = "level {0}")
    @MethodSource("pageOne")
    fun `a long route reaches 3 stars`(id: Int) {
        val end = longWin(id)
        assertIs<Outcome.Won>(end.outcome, "level $id was not won")
        val total = end.finalScore?.total ?: 0
        val three = end.level.stars.three
        assertTrue(total >= three, "level $id: best total $total is below the 3-star score $three")
        assertEquals(3, end.finalStars, "level $id")
    }

    @Test
    fun `3 stars on every page 1 level unlock Pack 1 Perfect and Perfectionist I`() {
        val history = pageOne().associateWith { longWin(it).finalStars }
        val unlocked = evaluateAttempt(null, history = history).newlyUnlocked
        assertTrue(Achievement.PACK_1_PERFECT in unlocked, "unlocked $unlocked")
        assertTrue(Achievement.PERFECTIONIST_1 in unlocked, "unlocked $unlocked")
    }

    companion object {
        @JvmStatic
        fun pageOne(): List<Int> = ScriptedPlay.bundledIds().filter { LevelLibraryLayout.pageOf(it) == 1 }
    }
}
