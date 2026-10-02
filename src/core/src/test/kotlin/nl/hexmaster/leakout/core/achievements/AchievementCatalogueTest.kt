package nl.hexmaster.leakout.core.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class AchievementCatalogueTest {
    @Test
    fun `the launch catalogue uses 595 XP and leaves at least 400 free`() {
        assertEquals(595, Achievement.totalXp)
        assertTrue(Achievement.MAX_TOTAL_XP - Achievement.totalXp >= 400)
    }

    @Test
    fun `the group totals match the design`() {
        val byGroup = Achievement.entries.groupBy { it.group }.mapValues { (_, list) -> list.sumOf { it.xp } }
        assertEquals(
            mapOf(
                AchievementGroup.PROGRESS to 170,
                AchievementGroup.SKILL to 230,
                AchievementGroup.LIFETIME to 115,
                AchievementGroup.HIDDEN to 80,
            ),
            byGroup,
        )
    }

    @Test
    fun `keys are unique`() {
        val keys = Achievement.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @ParameterizedTest
    @EnumSource(Achievement::class)
    fun `xp is a positive multiple of 5`(achievement: Achievement) {
        assertTrue(achievement.xp > 0 && achievement.xp % 5 == 0, "${achievement.key}: ${achievement.xp}")
    }

    @ParameterizedTest
    @EnumSource(Achievement::class)
    fun `incremental steps are within the Play Games limits`(achievement: Achievement) {
        val steps = achievement.steps ?: return
        assertTrue(steps in Achievement.STEPS_RANGE, "${achievement.key}: $steps")
        assertTrue(achievement.isIncremental)
    }

    @ParameterizedTest
    @EnumSource(Achievement::class)
    fun `keys are snake case and usable as resource names`(achievement: Achievement) {
        assertTrue(achievement.key.matches(Regex("[a-z][a-z0-9_]*")), achievement.key)
        assertEquals(achievement.name.lowercase(), achievement.key)
    }

    @ParameterizedTest
    @EnumSource(Achievement::class)
    fun `exactly the hidden group is hidden`(achievement: Achievement) {
        assertEquals(achievement.group == AchievementGroup.HIDDEN, achievement.hidden)
    }

    @ParameterizedTest
    @EnumSource(Achievement::class)
    fun `pack achievements and only those name their library page`(achievement: Achievement) {
        val match = Regex("pack_(\\d+)_(complete|perfect)").matchEntire(achievement.key)
        if (match == null) {
            assertNull(achievement.pack)
            val measure = (achievement.type as? AchievementType.Incremental)?.measure
            assertTrue(measure != Measure.PACK_LEVELS_FINISHED && measure != Measure.PACK_THREE_STAR_LEVELS)
        } else {
            assertEquals(match.groupValues[1].toInt(), achievement.pack)
            val expected = if (match.groupValues[2] == "complete") Measure.PACK_LEVELS_FINISHED else Measure.PACK_THREE_STAR_LEVELS
            assertEquals(expected, (achievement.type as AchievementType.Incremental).measure)
        }
    }

    @ParameterizedTest
    @EnumSource(Achievement::class)
    fun `every key resolves back to its achievement`(achievement: Achievement) {
        assertEquals(achievement, Achievement.fromKey(achievement.key))
    }

    @Test
    fun `an unknown key resolves to nothing`() {
        assertNull(Achievement.fromKey("master_plumber"))
    }

    /**
     * The entries published in the Play Console. Play Games cannot delete them or change their
     * steps, so this list may only grow, and no line in it may ever change.
     */
    private val published: List<Published> = listOf(
        Published("first_leak_fixed", null, hidden = false, xp = 10),
        Published("levels_finished_1", 10, hidden = false, xp = 15),
        Published("star_collector_1", 25, hidden = false, xp = 15),
        Published("star_collector_2", 60, hidden = false, xp = 30),
        Published("perfectionist_1", 10, hidden = false, xp = 30),
        Published("pack_1_complete", 24, hidden = false, xp = 30),
        Published("pack_1_perfect", 24, hidden = false, xp = 40),
        Published("zero_waste", null, hidden = false, xp = 30),
        Published("double_cross", null, hidden = false, xp = 20),
        Published("crossroads_king", null, hidden = false, xp = 40),
        Published("ahead_of_the_flow", null, hidden = false, xp = 30),
        Published("full_throttle", null, hidden = false, xp = 15),
        Published("measure_twice", null, hidden = false, xp = 20),
        Published("steady_hands", null, hidden = false, xp = 15),
        Published("long_way_round", null, hidden = false, xp = 30),
        Published("big_spender", null, hidden = false, xp = 30),
        Published("apprentice", 100, hidden = false, xp = 10),
        Published("journeyman", 1_000, hidden = false, xp = 25),
        Published("guildmaster", 5_000, hidden = false, xp = 40),
        Published("never_give_up", null, hidden = false, xp = 20),
        Published("personal_best", null, hidden = false, xp = 15),
        Published("wet_feet", null, hidden = false, xp = 5),
        Published("instant_flood", null, hidden = true, xp = 20),
        Published("wall_to_wall", null, hidden = true, xp = 30),
        Published("photo_finish", null, hidden = true, xp = 30),
    )

    private data class Published(val key: String, val steps: Int?, val hidden: Boolean, val xp: Int)

    @Test
    fun `every published entry is still in the catalogue unchanged`() {
        for (entry in published) {
            val achievement = assertNotNull(Achievement.fromKey(entry.key), "${entry.key} was removed")
            assertEquals(entry, Published(achievement.key, achievement.steps, achievement.hidden, achievement.xp))
        }
    }

    @Test
    fun `the launch catalogue is exactly the published list, in order`() {
        assertEquals(published.map { it.key }, Achievement.entries.take(published.size).map { it.key })
        assertEquals(25, published.size)
    }

    @Test
    fun `the catalogue never exceeds the Play Games XP budget`() {
        assertTrue(Achievement.totalXp <= Achievement.MAX_TOTAL_XP)
    }
}
