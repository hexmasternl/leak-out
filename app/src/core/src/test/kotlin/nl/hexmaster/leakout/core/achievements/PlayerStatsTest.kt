package nl.hexmaster.leakout.core.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerStatsTest {
    @Test
    fun `new stats are empty`() {
        assertEquals(PlayerStats(0, 0, emptyMap(), 0), PlayerStats.NONE)
        assertEquals(0, PlayerStats.NONE.lossStreak(1))
    }

    @Test
    fun `a loss adds its placements, a loss and one to that level's streak`() {
        val stats = PlayerStats(sectionsPlaced = 10, losses = 1, lossStreakByLevel = mapOf(5 to 1, 6 to 2))
            .after(lostSummary(levelId = 5, placements = 4))
        assertEquals(PlayerStats(14, 2, mapOf(5 to 2, 6 to 2)), stats)
    }

    @Test
    fun `a win adds its placements and clears only that level's streak`() {
        val stats = PlayerStats(sectionsPlaced = 10, losses = 3, lossStreakByLevel = mapOf(5 to 3, 6 to 1))
            .after(summary(levelId = 5, placements = 7))
        assertEquals(PlayerStats(17, 3, mapOf(6 to 1)), stats)
    }

    @Test
    fun `an abandoned attempt only adds its placements`() {
        val before = PlayerStats(sectionsPlaced = 98, losses = 2, lossStreakByLevel = mapOf(5 to 2))
        assertEquals(before.copy(sectionsPlaced = 102), before.afterAbandoned(4))
        assertEquals(before, before.afterAbandoned(0))
    }

    @Test
    fun `negative placements are refused`() {
        assertFailsWith<IllegalArgumentException> { PlayerStats.NONE.afterAbandoned(-1) }
    }

    @Test
    fun `a tips reset adds one and changes nothing else`() {
        val before = PlayerStats(sectionsPlaced = 12, losses = 2, lossStreakByLevel = mapOf(4 to 1))
        val once = before.afterTipsReset()
        assertEquals(before.copy(tipsResets = 1), once)
        assertEquals(2, once.afterTipsReset().tipsResets)
    }

    @Test
    fun `attempts keep the tips resets`() {
        val stats = PlayerStats(tipsResets = 1).after(lostSummary()).after(summary()).afterAbandoned(3)
        assertEquals(1, stats.tipsResets)
    }

    @Test
    fun `updates leave the original stats unchanged`() {
        val before = PlayerStats(sectionsPlaced = 1)
        before.after(lostSummary())
        before.afterAbandoned(5)
        assertEquals(PlayerStats(sectionsPlaced = 1), before)
    }
}
