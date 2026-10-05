package nl.hexmaster.leakout.core.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.achievements.Achievement.AHEAD_OF_THE_FLOW
import nl.hexmaster.leakout.core.achievements.Achievement.APPRENTICE
import nl.hexmaster.leakout.core.achievements.Achievement.BIG_SPENDER
import nl.hexmaster.leakout.core.achievements.Achievement.BIG_SPENDER_2
import nl.hexmaster.leakout.core.achievements.Achievement.CROSSROAD_MASTER
import nl.hexmaster.leakout.core.achievements.Achievement.CROSSROADS_KING
import nl.hexmaster.leakout.core.achievements.Achievement.DOUBLE_CROSS
import nl.hexmaster.leakout.core.achievements.Achievement.DRAMATIC_FAILURE
import nl.hexmaster.leakout.core.achievements.Achievement.EFFICIENCY_MASTER
import nl.hexmaster.leakout.core.achievements.Achievement.ERASER
import nl.hexmaster.leakout.core.achievements.Achievement.FIRST_LEAK_FIXED
import nl.hexmaster.leakout.core.achievements.Achievement.FULL_THROTTLE
import nl.hexmaster.leakout.core.achievements.Achievement.GUILDMASTER
import nl.hexmaster.leakout.core.achievements.Achievement.HIGHWAY
import nl.hexmaster.leakout.core.achievements.Achievement.INSTANT_FLOOD
import nl.hexmaster.leakout.core.achievements.Achievement.JOURNEYMAN
import nl.hexmaster.leakout.core.achievements.Achievement.LEVELS_FINISHED_1
import nl.hexmaster.leakout.core.achievements.Achievement.LONG_WAY_ROUND
import nl.hexmaster.leakout.core.achievements.Achievement.LONG_WAY_ROUND_2
import nl.hexmaster.leakout.core.achievements.Achievement.LONG_WAY_ROUND_3
import nl.hexmaster.leakout.core.achievements.Achievement.MEASURE_TWICE
import nl.hexmaster.leakout.core.achievements.Achievement.NEVER_GIVE_UP
import nl.hexmaster.leakout.core.achievements.Achievement.ON_YOUR_NERVES
import nl.hexmaster.leakout.core.achievements.Achievement.PACK_1_COMPLETE
import nl.hexmaster.leakout.core.achievements.Achievement.PACK_1_PERFECT
import nl.hexmaster.leakout.core.achievements.Achievement.PERFECTIONIST_1
import nl.hexmaster.leakout.core.achievements.Achievement.PERSONAL_BEST
import nl.hexmaster.leakout.core.achievements.Achievement.PHOTO_FINISH
import nl.hexmaster.leakout.core.achievements.Achievement.SNAKE
import nl.hexmaster.leakout.core.achievements.Achievement.STAR_COLLECTOR_1
import nl.hexmaster.leakout.core.achievements.Achievement.STAR_COLLECTOR_2
import nl.hexmaster.leakout.core.achievements.Achievement.STEADY_HANDS
import nl.hexmaster.leakout.core.achievements.Achievement.TIPS_ADDICT
import nl.hexmaster.leakout.core.achievements.Achievement.WALL_TO_WALL
import nl.hexmaster.leakout.core.achievements.Achievement.WET_FEET
import nl.hexmaster.leakout.core.achievements.Achievement.ZERO_WASTE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class AchievementRulesTest {
    private val skillAndHidden = Achievement.entries.filter { it.group == AchievementGroup.SKILL || it.group == AchievementGroup.HIDDEN }

    @Test
    fun `the neutral attempt unlocks nothing`() {
        assertEquals(emptyList(), evaluateAttempt(summary()).newlyUnlocked)
        val wetFeet = mapOf(WET_FEET to StoredAchievement(unlocked = true))
        assertEquals(emptyList(), evaluateAttempt(lostSummary(), statsBefore = PlayerStats(losses = 1), stored = wetFeet).newlyUnlocked)
    }

    @Nested
    inner class Progress {
        @Test
        fun `first level finished with 0 stars`() {
            val evaluation = evaluateAttempt(null, history = mapOf(1 to 0))
            assertTrue(FIRST_LEAK_FIXED in evaluation.newlyUnlocked)
            assertEquals(1, evaluation.steps[LEVELS_FINISHED_1])
            assertEquals(1, evaluation.steps[PACK_1_COMPLETE])
            assertEquals(0, evaluation.steps[PACK_1_PERFECT])
        }

        @Test
        fun `First Leak Fixed needs level 1 itself`() {
            assertFalse(FIRST_LEAK_FIXED in evaluateAttempt(null, history = mapOf(2 to 3, 3 to 3)).newlyUnlocked)
        }

        @Test
        fun `star collector and perfectionist progress`() {
            val evaluation = evaluateAttempt(null, history = mapOf(1 to 3, 2 to 2, 3 to 3))
            assertEquals(8, evaluation.steps[STAR_COLLECTOR_1])
            assertEquals(8, evaluation.steps[STAR_COLLECTOR_2])
            assertEquals(2, evaluation.steps[PERFECTIONIST_1])
            assertEquals(2, evaluation.steps[PACK_1_PERFECT])
        }

        @Test
        fun `9 finished levels are not yet the milestone and 10 are`() {
            assertFalse(LEVELS_FINISHED_1 in evaluateAttempt(null, history = history(1..9)).newlyUnlocked)
            val ten = evaluateAttempt(null, history = history(1..10))
            assertTrue(LEVELS_FINISHED_1 in ten.newlyUnlocked)
            assertEquals(10, ten.steps[LEVELS_FINISHED_1])
        }

        @Test
        fun `progress is clamped to the steps`() {
            val evaluation = evaluateAttempt(null, history = history(1..30, stars = 3))
            assertEquals(10, evaluation.steps[LEVELS_FINISHED_1])
            assertEquals(25, evaluation.steps[STAR_COLLECTOR_1])
            assertEquals(60, evaluation.steps[STAR_COLLECTOR_2])
            assertEquals(10, evaluation.steps[PERFECTIONIST_1])
            assertEquals(24, evaluation.steps[PACK_1_COMPLETE])
            assertEquals(24, evaluation.steps[PACK_1_PERFECT])
            assertTrue(evaluation.newlyUnlocked.containsAll(Achievement.entries.filter { it.group == AchievementGroup.PROGRESS }))
        }

        @Test
        fun `star milestones unlock at their thresholds`() {
            // 8 levels with 3 stars and 1 with 1 star: 25 stars.
            val evaluation = evaluateAttempt(null, history = history(1..8, stars = 3) + (9 to 1))
            assertTrue(STAR_COLLECTOR_1 in evaluation.newlyUnlocked)
            assertFalse(STAR_COLLECTOR_2 in evaluation.newlyUnlocked)
            assertEquals(25, evaluation.steps[STAR_COLLECTOR_2])
        }

        @Test
        fun `ten 3-star levels make a perfectionist`() {
            assertTrue(PERFECTIONIST_1 in evaluateAttempt(null, history = history(3..12, stars = 3)).newlyUnlocked)
        }

        @Test
        fun `pack complete with some 0-star levels is not pack perfect`() {
            val evaluation = evaluateAttempt(null, history = history(1..12, stars = 3) + history(13..24, stars = 0))
            assertTrue(PACK_1_COMPLETE in evaluation.newlyUnlocked)
            assertFalse(PACK_1_PERFECT in evaluation.newlyUnlocked)
            assertEquals(12, evaluation.steps[PACK_1_PERFECT])
        }

        @Test
        fun `every level of page 1 with 3 stars is pack perfect`() {
            assertTrue(PACK_1_PERFECT in evaluateAttempt(null, history = history(1..24, stars = 3)).newlyUnlocked)
        }

        @Test
        fun `levels on another page do not count for pack 1`() {
            val evaluation = evaluateAttempt(null, history = history(1..23, stars = 3) + (25 to 3))
            assertEquals(23, evaluation.steps[PACK_1_COMPLETE])
            assertEquals(23, evaluation.steps[PACK_1_PERFECT])
            assertFalse(PACK_1_COMPLETE in evaluation.newlyUnlocked)
            assertEquals(10, evaluation.steps[LEVELS_FINISHED_1])
        }

        @Test
        fun `adding levels to the library changes no progress rule`() {
            // The rules never see the library size: 10 finished levels out of 24 or out of 48 are the same input.
            val evaluation = evaluateAttempt(null, history = history(30..39))
            assertTrue(LEVELS_FINISHED_1 in evaluation.newlyUnlocked)
            assertEquals(0, evaluation.steps[PACK_1_COMPLETE])
        }

        @Test
        fun `stars outside 0 to 3 are clamped as the library does`() {
            assertEquals(3, evaluateAttempt(null, history = mapOf(1 to 7, 2 to -1)).steps[STAR_COLLECTOR_1])
        }

        @Test
        fun `an existing player with 14 finished levels gets the progress achievements at start`() {
            val evaluation = evaluateAttempt(null, history = history(1..14, stars = 1))
            assertTrue(FIRST_LEAK_FIXED in evaluation.newlyUnlocked)
            assertTrue(LEVELS_FINISHED_1 in evaluation.newlyUnlocked)
            assertEquals(14, evaluation.steps[PACK_1_COMPLETE])
        }
    }

    @Nested
    inner class Skill {
        @Test
        fun `zero waste win`() {
            assertTrue(ZERO_WASTE in evaluateAttempt(summary(unusedSections = 0)).newlyUnlocked)
        }

        @Test
        fun `one unused section is not zero waste`() {
            assertFalse(ZERO_WASTE in evaluateAttempt(summary(unusedSections = 1)).newlyUnlocked)
        }

        @Test
        fun `a cross passed twice unlocks Double Cross on a win and on a loss`() {
            assertTrue(DOUBLE_CROSS in evaluateAttempt(summary(crossesLeftTwice = 1)).newlyUnlocked)
            assertTrue(DOUBLE_CROSS in evaluateAttempt(lostSummary(crossesLeftTwice = 1)).newlyUnlocked)
            assertFalse(CROSSROADS_KING in evaluateAttempt(summary(crossesLeftTwice = 2)).newlyUnlocked)
        }

        @Test
        fun `three crosses passed twice make a Crossroads King, also on a loss`() {
            val evaluation = evaluateAttempt(summary(crossesLeftTwice = 3))
            assertTrue(CROSSROADS_KING in evaluation.newlyUnlocked)
            assertTrue(DOUBLE_CROSS in evaluation.newlyUnlocked)
            assertTrue(CROSSROADS_KING in evaluateAttempt(lostSummary(crossesLeftTwice = 3)).newlyUnlocked)
        }

        @Test
        fun `route ready before the flow and won`() {
            assertTrue(AHEAD_OF_THE_FLOW in evaluateAttempt(summary(routeCompletedBeforeFlow = true)).newlyUnlocked)
        }

        @Test
        fun `route completed after the flow started`() {
            assertFalse(AHEAD_OF_THE_FLOW in evaluateAttempt(summary(routeCompletedBeforeFlow = false)).newlyUnlocked)
        }

        @Test
        fun `fast forward into the finish`() {
            assertTrue(FULL_THROTTLE in evaluateAttempt(summary(fastForwardAtWin = true)).newlyUnlocked)
        }

        @Test
        fun `no replacement is Measure Twice and one replacement is not`() {
            assertTrue(MEASURE_TWICE in evaluateAttempt(summary(replacements = 0)).newlyUnlocked)
            assertFalse(MEASURE_TWICE in evaluateAttempt(summary(replacements = 1)).newlyUnlocked)
        }

        @Test
        fun `a pause blocks Steady Hands`() {
            assertFalse(STEADY_HANDS in evaluateAttempt(summary(everPaused = true)).newlyUnlocked)
            assertTrue(STEADY_HANDS in evaluateAttempt(summary(everPaused = false)).newlyUnlocked)
        }

        @Test
        fun `30 distinct tiles left is the long way round and 29 is not`() {
            assertTrue(LONG_WAY_ROUND in evaluateAttempt(summary(distinctTilesLeft = 30)).newlyUnlocked)
            assertFalse(LONG_WAY_ROUND in evaluateAttempt(summary(distinctTilesLeft = 29, sectionsLeft = 31)).newlyUnlocked)
        }

        @Test
        fun `a final total of 500 is a big spender and 499 is not`() {
            assertTrue(BIG_SPENDER in evaluateAttempt(summary(finalTotal = 500)).newlyUnlocked)
            assertFalse(BIG_SPENDER in evaluateAttempt(summary(finalTotal = 499)).newlyUnlocked)
        }

        @Test
        fun `a loss unlocks only the cross achievements of the skill group`() {
            // Every win-only fact is set, but the attempt was lost.
            val lost = summary(
                outcome = AttemptOutcome.LOST, unusedSections = 0, replacements = 0, everPaused = false,
                fastForwardAtWin = true, routeCompletedBeforeFlow = true, distinctTilesLeft = 40, finalTotal = 900,
                crossesLeftTwice = 3, photoFinish = true, piecesOnBoard = 46, longestCrossChain = 3,
                longestStraightRun = 7, longestCornerRun = 12, minimumSections = 46,
            )
            val skill = evaluateAttempt(lost).newlyUnlocked.filter { it.group == AchievementGroup.SKILL || it.group == AchievementGroup.HIDDEN }
            assertEquals(listOf(DOUBLE_CROSS, CROSSROADS_KING), skill)
        }
    }

    @Nested
    inner class Lifetime {
        @Test
        fun `placements add up across attempts, abandoned ones included`() {
            val before = PlayerStats(sectionsPlaced = 98)
            val after = before.afterAbandoned(3)
            val evaluation = AchievementRules.evaluate(null, before, after, emptyMap(), emptyMap())
            assertTrue(APPRENTICE in evaluation.newlyUnlocked)
            assertEquals(100, evaluation.steps[APPRENTICE])
            assertEquals(101, evaluation.steps[JOURNEYMAN])
            assertEquals(101, evaluation.steps[GUILDMASTER])
        }

        @Test
        fun `the placement tiers unlock at 1000 and 5000`() {
            fun unlocked(placed: Int) = AchievementRules.evaluate(null, PlayerStats.NONE, PlayerStats(sectionsPlaced = placed), emptyMap(), emptyMap()).newlyUnlocked
            assertEquals(listOf(APPRENTICE), unlocked(999))
            assertEquals(listOf(APPRENTICE, JOURNEYMAN), unlocked(1_000))
            assertEquals(listOf(APPRENTICE, JOURNEYMAN), unlocked(4_999))
            assertEquals(listOf(APPRENTICE, JOURNEYMAN, GUILDMASTER), unlocked(5_000))
        }

        @Test
        fun `a win after three losses in a row on that level`() {
            val stats = (1..3).fold(PlayerStats.NONE) { s, _ -> s.after(lostSummary(levelId = 5)) }
            assertEquals(3, stats.lossStreak(5))
            assertTrue(NEVER_GIVE_UP in evaluateAttempt(summary(levelId = 5), statsBefore = stats).newlyUnlocked)
        }

        @Test
        fun `the streak is per level`() {
            val stats = PlayerStats.NONE
                .after(lostSummary(levelId = 5))
                .after(lostSummary(levelId = 6))
                .after(lostSummary(levelId = 5))
            assertFalse(NEVER_GIVE_UP in evaluateAttempt(summary(levelId = 5), statsBefore = stats).newlyUnlocked)
        }

        @Test
        fun `a fourth loss is not a win`() {
            val stats = PlayerStats(losses = 3, lossStreakByLevel = mapOf(5 to 3))
            assertFalse(NEVER_GIVE_UP in evaluateAttempt(lostSummary(levelId = 5), statsBefore = stats).newlyUnlocked)
        }

        @Test
        fun `a win clears the streak, so the next win is no comeback`() {
            val stats = PlayerStats(losses = 3, lossStreakByLevel = mapOf(5 to 3)).after(summary(levelId = 5))
            assertEquals(0, stats.lossStreak(5))
            assertFalse(NEVER_GIVE_UP in evaluateAttempt(summary(levelId = 5), statsBefore = stats).newlyUnlocked)
        }

        @Test
        fun `a new highscore is a personal best`() {
            assertTrue(PERSONAL_BEST in evaluateAttempt(summary(record = RecordOutcome.NEW_HIGHSCORE)).newlyUnlocked)
        }

        @Test
        fun `a first completion or no improvement is not a personal best`() {
            assertFalse(PERSONAL_BEST in evaluateAttempt(summary(record = RecordOutcome.FIRST_COMPLETION)).newlyUnlocked)
            assertFalse(PERSONAL_BEST in evaluateAttempt(summary(record = RecordOutcome.NO_IMPROVEMENT)).newlyUnlocked)
        }

        @Test
        fun `the first loss gets wet feet`() {
            assertTrue(WET_FEET in evaluateAttempt(lostSummary()).newlyUnlocked)
        }

        @Test
        fun `a win gets no wet feet`() {
            assertFalse(WET_FEET in evaluateAttempt(summary()).newlyUnlocked)
        }

        @Test
        fun `wet feet is awarded from the stats alone at start`() {
            val stats = PlayerStats(losses = 2)
            assertTrue(WET_FEET in AchievementRules.evaluate(null, stats, stats, emptyMap(), emptyMap()).newlyUnlocked)
        }
    }

    @Nested
    inner class Hidden {
        @Test
        fun `a leak before the liquid left any placed section is an instant flood`() {
            assertTrue(INSTANT_FLOOD in evaluateAttempt(lostSummary(sectionsLeft = 0)).newlyUnlocked)
            assertFalse(INSTANT_FLOOD in evaluateAttempt(lostSummary(sectionsLeft = 1)).newlyUnlocked)
        }

        @Test
        fun `a full board on a win is wall to wall`() {
            assertTrue(WALL_TO_WALL in evaluateAttempt(summary(freeCells = 46, piecesOnBoard = 46)).newlyUnlocked)
            assertFalse(WALL_TO_WALL in evaluateAttempt(summary(freeCells = 46, piecesOnBoard = 45)).newlyUnlocked)
        }

        @Test
        fun `a full board on a loss is not wall to wall`() {
            val lost = summary(outcome = AttemptOutcome.LOST, freeCells = 46, piecesOnBoard = 46)
            assertFalse(WALL_TO_WALL in evaluateAttempt(lost).newlyUnlocked)
        }

        @Test
        fun `a photo finish needs the win`() {
            assertTrue(PHOTO_FINISH in evaluateAttempt(summary(photoFinish = true)).newlyUnlocked)
            assertFalse(PHOTO_FINISH in evaluateAttempt(summary(outcome = AttemptOutcome.LOST, photoFinish = true)).newlyUnlocked)
        }
    }

    @Nested
    inner class LaterAdditions {
        private fun unlocks(summary: AttemptSummary) = evaluateAttempt(summary).newlyUnlocked

        @Test
        fun `one win unlocks every Long Way Round tier it reaches`() {
            val unlocked = unlocks(summary(distinctTilesLeft = 104))
            assertTrue(unlocked.containsAll(listOf(LONG_WAY_ROUND, LONG_WAY_ROUND_2, LONG_WAY_ROUND_3)))
        }

        @Test
        fun `between two Long Way Round tiers`() {
            val unlocked = unlocks(summary(distinctTilesLeft = 49))
            assertTrue(LONG_WAY_ROUND in unlocked)
            assertFalse(LONG_WAY_ROUND_2 in unlocked)
            assertTrue(LONG_WAY_ROUND_2 in unlocks(summary(distinctTilesLeft = 50)))
            assertFalse(LONG_WAY_ROUND_3 in unlocks(summary(distinctTilesLeft = 99)))
            assertTrue(LONG_WAY_ROUND_3 in unlocks(summary(distinctTilesLeft = 100)))
        }

        @Test
        fun `a cross counts once for the Long Way Round tiers`() {
            assertFalse(LONG_WAY_ROUND_2 in unlocks(summary(distinctTilesLeft = 49, sectionsLeft = 52, crossesLeftTwice = 3)))
        }

        @Test
        fun `a loss is no long way round`() {
            val lost = summary(outcome = AttemptOutcome.LOST, distinctTilesLeft = 60, sectionsLeft = 30)
            assertTrue(unlocks(lost).none { it == LONG_WAY_ROUND || it == LONG_WAY_ROUND_2 || it == LONG_WAY_ROUND_3 })
        }

        @Test
        fun `the penalty counts for Big Spender II`() {
            // 1009 earned, 4 unused sections: a final total of 997.
            val unlocked = unlocks(summary(finalTotal = 997, unusedSections = 4))
            assertTrue(BIG_SPENDER in unlocked)
            assertFalse(BIG_SPENDER_2 in unlocked)
            assertTrue(unlocks(summary(finalTotal = 1_000)).containsAll(listOf(BIG_SPENDER, BIG_SPENDER_2)))
        }

        @Test
        fun `three crosses in a row on a win make a Crossroad Master`() {
            assertTrue(CROSSROAD_MASTER in unlocks(summary(longestCrossChain = 3)))
            assertFalse(CROSSROAD_MASTER in unlocks(summary(longestCrossChain = 2)))
        }

        @Test
        fun `three crosses in a row and a leak are no Crossroad Master`() {
            assertFalse(CROSSROAD_MASTER in unlocks(summary(outcome = AttemptOutcome.LOST, longestCrossChain = 3)))
        }

        @Test
        fun `seven straights in a row on a win are a Highway`() {
            assertTrue(HIGHWAY in unlocks(summary(longestStraightRun = 7)))
            assertFalse(HIGHWAY in unlocks(summary(longestStraightRun = 6)))
            assertFalse(HIGHWAY in unlocks(summary(outcome = AttemptOutcome.LOST, longestStraightRun = 7)))
        }

        @Test
        fun `twelve corners in a row on a win are a Snake and eleven are not`() {
            assertTrue(SNAKE in unlocks(summary(longestCornerRun = 12)))
            assertFalse(SNAKE in unlocks(summary(longestCornerRun = 11)))
            assertFalse(SNAKE in unlocks(summary(outcome = AttemptOutcome.LOST, longestCornerRun = 12)))
        }

        @Test
        fun `a win on the shortest route is Efficiency Master`() {
            assertTrue(EFFICIENCY_MASTER in unlocks(summary(minimumSections = 9, piecesOnBoard = 9, unusedSections = 0)))
        }

        @Test
        fun `one extra section is no Efficiency Master`() {
            assertFalse(EFFICIENCY_MASTER in unlocks(summary(minimumSections = 9, piecesOnBoard = 10, unusedSections = 1)))
        }

        @Test
        fun `replacements do not count against Efficiency Master`() {
            assertTrue(EFFICIENCY_MASTER in unlocks(summary(minimumSections = 9, piecesOnBoard = 9, unusedSections = 0, replacements = 2, placements = 11)))
        }

        @Test
        fun `Efficiency Master needs a win and a solvable level`() {
            assertFalse(EFFICIENCY_MASTER in unlocks(summary(outcome = AttemptOutcome.LOST, minimumSections = 9, piecesOnBoard = 9)))
            assertFalse(EFFICIENCY_MASTER in unlocks(summary(minimumSections = null, piecesOnBoard = 9)))
        }

        @Test
        fun `twenty replacements on a win are an Eraser`() {
            assertTrue(ERASER in unlocks(summary(replacements = 20, placements = 40)))
            assertFalse(ERASER in unlocks(summary(replacements = 19, placements = 40)))
            assertFalse(ERASER in unlocks(summary(outcome = AttemptOutcome.LOST, replacements = 20, placements = 40)))
        }

        @Test
        fun `the first tips reset is Tips Addict, from the stats alone`() {
            val stats = PlayerStats.NONE.afterTipsReset()
            assertTrue(TIPS_ADDICT in AchievementRules.evaluate(null, PlayerStats.NONE, stats, emptyMap(), emptyMap()).newlyUnlocked)
            assertFalse(TIPS_ADDICT in AchievementRules.evaluate(null, PlayerStats.NONE, PlayerStats.NONE, emptyMap(), emptyMap()).newlyUnlocked)
        }

        @Test
        fun `a second tips reset unlocks nothing`() {
            val stored = mapOf(TIPS_ADDICT to StoredAchievement(unlocked = true))
            val stats = PlayerStats(tipsResets = 1)
            assertEquals(emptyList(), AchievementRules.evaluate(null, stats, stats.afterTipsReset(), emptyMap(), stored).newlyUnlocked)
        }

        @Test
        fun `a loss after more than 50 sections is a Dramatic Failure`() {
            assertTrue(DRAMATIC_FAILURE in unlocks(lostSummary(sectionsLeft = 51)))
            assertFalse(DRAMATIC_FAILURE in unlocks(lostSummary(sectionsLeft = 50)))
            assertFalse(DRAMATIC_FAILURE in unlocks(summary(distinctTilesLeft = 60)))
        }

        @Test
        fun `five taps on one replacement are On Your Nerves, won or lost`() {
            assertTrue(ON_YOUR_NERVES in unlocks(summary(mostReplacementTaps = 5)))
            assertTrue(ON_YOUR_NERVES in unlocks(lostSummary(mostReplacementTaps = 5)))
            // Two replacements tapped 3 times each: the attempt's highest count is 3.
            assertFalse(ON_YOUR_NERVES in unlocks(summary(mostReplacementTaps = 3)))
        }
    }

    @Nested
    inner class Evaluation {
        @Test
        fun `an unlocked achievement is never unlocked again`() {
            val stored = mapOf(ZERO_WASTE to StoredAchievement(unlocked = true))
            assertFalse(ZERO_WASTE in evaluateAttempt(summary(unusedSections = 0), stored = stored).newlyUnlocked)
        }

        @Test
        fun `an unlocked incremental achievement is never unlocked again and keeps its full steps`() {
            val stored = mapOf(LEVELS_FINISHED_1 to StoredAchievement(steps = 10, unlocked = true))
            val evaluation = evaluateAttempt(null, history = emptyMap(), stored = stored)
            assertFalse(LEVELS_FINISHED_1 in evaluation.newlyUnlocked)
            assertEquals(10, evaluation.steps[LEVELS_FINISHED_1])
        }

        @Test
        fun `progress never goes down`() {
            val stored = mapOf(STAR_COLLECTOR_1 to StoredAchievement(steps = 20))
            val evaluation = evaluateAttempt(null, history = history(1..4, stars = 3), stored = stored)
            assertEquals(20, evaluation.steps[STAR_COLLECTOR_1])
        }

        @Test
        fun `stored progress above the steps is clamped and unlocks`() {
            val stored = mapOf(APPRENTICE to StoredAchievement(steps = 150))
            val evaluation = evaluateAttempt(null, stored = stored)
            assertEquals(100, evaluation.steps[APPRENTICE])
            assertTrue(APPRENTICE in evaluation.newlyUnlocked)
        }

        @Test
        fun `evaluation without an attempt unlocks no skill or hidden achievement`() {
            val evaluation = evaluateAttempt(null, history = mapOf(1 to 3))
            assertTrue(FIRST_LEAK_FIXED in evaluation.newlyUnlocked)
            assertTrue(evaluation.newlyUnlocked.none { it in skillAndHidden || it == NEVER_GIVE_UP || it == PERSONAL_BEST })
        }

        @Test
        fun `steps hold exactly the incremental achievements`() {
            assertEquals(Achievement.entries.filter { it.isIncremental }.toSet(), evaluateAttempt(null).steps.keys)
        }

        @Test
        fun `newly unlocked achievements come in catalogue order`() {
            val evaluation = evaluateAttempt(
                summary(unusedSections = 0, replacements = 0, everPaused = false, photoFinish = true, levelId = 1),
                history = mapOf(1 to 3),
            )
            assertEquals(evaluation.newlyUnlocked.sortedBy { it.ordinal }, evaluation.newlyUnlocked)
            assertEquals(listOf(FIRST_LEAK_FIXED, ZERO_WASTE, MEASURE_TWICE, STEADY_HANDS, PHOTO_FINISH), evaluation.newlyUnlocked)
        }

        @Test
        fun `evaluating twice with the result stored unlocks nothing new`() {
            val attempt = summary(unusedSections = 0, crossesLeftTwice = 3)
            val first = evaluateAttempt(attempt, history = history(1..10, stars = 3))
            val stored = Achievement.entries.associateWith { a ->
                StoredAchievement(steps = first.steps[a] ?: 0, unlocked = a in first.newlyUnlocked)
            }
            val second = evaluateAttempt(attempt, history = history(1..10, stars = 3), stored = stored)
            assertEquals(emptyList(), second.newlyUnlocked)
            assertEquals(first.steps, second.steps)
        }

        @ParameterizedTest
        @EnumSource(Achievement::class)
        fun `a stored unlock is never returned again whatever the input`(achievement: Achievement) {
            val stored = mapOf(achievement to StoredAchievement(steps = achievement.steps ?: 0, unlocked = true))
            val everything = summary(
                levelId = 1, unusedSections = 0, replacements = 0, everPaused = false, fastForwardAtWin = true,
                routeCompletedBeforeFlow = true, distinctTilesLeft = 46, finalTotal = 900, crossesLeftTwice = 3,
                photoFinish = true, piecesOnBoard = 46, record = RecordOutcome.NEW_HIGHSCORE, placements = 6_000,
            )
            val statsBefore = PlayerStats(losses = 3, lossStreakByLevel = mapOf(1 to 3))
            val evaluation = evaluateAttempt(everything, statsBefore = statsBefore, history = history(1..24, stars = 3), stored = stored)
            assertFalse(achievement in evaluation.newlyUnlocked)
        }

        @Test
        fun `every achievement can be unlocked by the rules`() {
            val everything = summary(
                levelId = 1, unusedSections = 0, replacements = 0, everPaused = false, fastForwardAtWin = true,
                routeCompletedBeforeFlow = true, distinctTilesLeft = 46, finalTotal = 900, crossesLeftTwice = 3,
                photoFinish = true, piecesOnBoard = 46, record = RecordOutcome.NEW_HIGHSCORE, placements = 6_000,
            )
            val won = evaluateAttempt(
                everything,
                statsBefore = PlayerStats(losses = 3, lossStreakByLevel = mapOf(1 to 3)),
                history = history(1..24, stars = 3),
            )
            val flood = evaluateAttempt(lostSummary(sectionsLeft = 0))
            // The later additions that cannot share a single attempt with the ones above.
            val later = evaluateAttempt(
                summary(
                    distinctTilesLeft = 100, finalTotal = 1_000, longestCrossChain = 3, longestStraightRun = 7, longestCornerRun = 12,
                    replacements = 20, placements = 60, minimumSections = 10, piecesOnBoard = 10, mostReplacementTaps = 5,
                ),
            )
            val drama = evaluateAttempt(lostSummary(sectionsLeft = 51))
            val tips = AchievementRules.evaluate(null, PlayerStats.NONE, PlayerStats(tipsResets = 1), emptyMap(), emptyMap())
            val all = won.newlyUnlocked + flood.newlyUnlocked + later.newlyUnlocked + drama.newlyUnlocked + tips.newlyUnlocked
            assertEquals(Achievement.entries.toSet(), all.toSet())
        }
    }
}
