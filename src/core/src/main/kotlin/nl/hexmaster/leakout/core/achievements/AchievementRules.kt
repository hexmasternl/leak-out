package nl.hexmaster.leakout.core.achievements

import nl.hexmaster.leakout.core.library.LevelLibraryLayout

/** What is stored for one achievement: its progress and whether it is unlocked. */
data class StoredAchievement(val steps: Int = 0, val unlocked: Boolean = false)

/**
 * The result of [AchievementRules.evaluate].
 *
 * @param newlyUnlocked the achievements this evaluation unlocked, in catalogue order; never one
 *   that was already unlocked.
 * @param steps the progress of every incremental achievement: at most its steps, never below what
 *   was stored, and the full steps once unlocked.
 */
data class Evaluation(val newlyUnlocked: List<Achievement>, val steps: Map<Achievement, Int>)

/**
 * Decides the achievements. Pure: everything comes in as plain values, so the rules are tested
 * without a database or Play Games, and running them twice on the same input changes nothing.
 */
object AchievementRules {
    const val NEVER_GIVE_UP_LOSSES = 3
    const val CROSSROADS_KING_CROSSES = 3
    const val LONG_WAY_ROUND_TILES = 30
    const val BIG_SPENDER_TOTAL = 500
    const val LONG_WAY_ROUND_2_TILES = 50
    const val LONG_WAY_ROUND_3_TILES = 100
    const val BIG_SPENDER_2_TOTAL = 1_000
    const val CROSSROAD_MASTER_CHAIN = 3
    const val HIGHWAY_RUN = 7
    const val SNAKE_RUN = 12
    const val ERASER_REPLACEMENTS = 20

    /** Dramatic Failure needs strictly more sections left than this. */
    const val DRAMATIC_FAILURE_SECTIONS = 50
    const val ON_YOUR_NERVES_TAPS = 5
    const val SNAIL_RESERVOIRS = 3

    /**
     * @param summary the attempt that just ended, or null at start-up and after a history change:
     *   then only progress and lifetime achievements can unlock.
     * @param statsBefore the player stats before [summary] was added; Never Give Up looks at the
     *   loss streak the win ended.
     * @param statsAfter the player stats with [summary] added.
     * @param history the stored stars per finished level id, after recording a win of [summary].
     * @param stored what is stored per achievement; a missing one has no progress and is locked.
     */
    fun evaluate(
        summary: AttemptSummary?,
        statsBefore: PlayerStats,
        statsAfter: PlayerStats,
        history: Map<Int, Int>,
        stored: Map<Achievement, StoredAchievement>,
    ): Evaluation {
        val steps = mutableMapOf<Achievement, Int>()
        val newlyUnlocked = mutableListOf<Achievement>()
        for (achievement in Achievement.entries) {
            val before = stored[achievement] ?: StoredAchievement()
            val earned = when (val type = achievement.type) {
                is AchievementType.Incremental -> {
                    val now = if (before.unlocked) {
                        type.steps
                    } else {
                        maxOf(before.steps, progress(type.measure, achievement.pack, history, statsAfter)).coerceAtMost(type.steps)
                    }
                    steps[achievement] = now
                    now >= type.steps
                }
                AchievementType.Standard -> isEarned(achievement, summary, statsBefore, statsAfter, history)
            }
            if (earned && !before.unlocked) newlyUnlocked += achievement
        }
        return Evaluation(newlyUnlocked, steps)
    }

    /**
     * The current count of [measure], unclamped. [pack] is the library page for the pack measures.
     * Levels are placed on pages by [LevelLibraryLayout.pageOf], so the size of the library never matters.
     */
    fun progress(measure: Measure, pack: Int?, history: Map<Int, Int>, stats: PlayerStats): Int {
        fun stars(levelId: Int) = (history[levelId] ?: 0).coerceIn(0, LevelLibraryLayout.MAX_STARS_PER_LEVEL)
        fun onPack(levelId: Int) = levelId >= 1 && LevelLibraryLayout.pageOf(levelId) == requireNotNull(pack) { "$measure needs a pack" }
        val perfect = LevelLibraryLayout.MAX_STARS_PER_LEVEL
        return when (measure) {
            Measure.LEVELS_FINISHED -> history.size
            Measure.STARS -> history.keys.sumOf(::stars)
            Measure.THREE_STAR_LEVELS -> history.keys.count { stars(it) == perfect }
            Measure.PACK_LEVELS_FINISHED -> history.keys.count(::onPack)
            Measure.PACK_THREE_STAR_LEVELS -> history.keys.count { onPack(it) && stars(it) == perfect }
            Measure.SECTIONS_PLACED -> stats.sectionsPlaced
        }
    }

    /** Whether the standard [achievement] is earned. Skill and hidden ones need an attempt; Wet Feet and Tips Addict only the stats. */
    private fun isEarned(
        achievement: Achievement,
        summary: AttemptSummary?,
        statsBefore: PlayerStats,
        statsAfter: PlayerStats,
        history: Map<Int, Int>,
    ): Boolean {
        val won = summary?.won == true
        return when (achievement) {
            Achievement.FIRST_LEAK_FIXED -> 1 in history
            Achievement.WET_FEET -> statsAfter.losses >= 1
            Achievement.TIPS_ADDICT -> statsAfter.tipsResets >= 1

            Achievement.ZERO_WASTE -> won && summary?.unusedSections == 0
            // The liquid can leave a cross twice before it leaks, so these two count on a loss too.
            Achievement.DOUBLE_CROSS -> (summary?.crossesLeftTwice ?: 0) >= 1
            Achievement.CROSSROADS_KING -> (summary?.crossesLeftTwice ?: 0) >= CROSSROADS_KING_CROSSES
            Achievement.AHEAD_OF_THE_FLOW -> won && summary?.routeCompletedBeforeFlow == true
            Achievement.FULL_THROTTLE -> won && summary?.fastForwardAtWin == true
            Achievement.MEASURE_TWICE -> won && summary?.replacements == 0
            Achievement.STEADY_HANDS -> won && summary?.everPaused == false
            Achievement.LONG_WAY_ROUND -> won && (summary?.distinctTilesLeft ?: 0) >= LONG_WAY_ROUND_TILES
            Achievement.BIG_SPENDER -> won && (summary?.finalTotal ?: 0) >= BIG_SPENDER_TOTAL
            Achievement.LONG_WAY_ROUND_2 -> won && (summary?.distinctTilesLeft ?: 0) >= LONG_WAY_ROUND_2_TILES
            Achievement.LONG_WAY_ROUND_3 -> won && (summary?.distinctTilesLeft ?: 0) >= LONG_WAY_ROUND_3_TILES
            Achievement.BIG_SPENDER_2 -> won && (summary?.finalTotal ?: 0) >= BIG_SPENDER_2_TOTAL
            // Unlike Double Cross, a chain of crosses needs the win.
            Achievement.CROSSROAD_MASTER -> won && (summary?.longestCrossChain ?: 0) >= CROSSROAD_MASTER_CHAIN
            Achievement.EFFICIENCY_MASTER -> won && summary?.minimumSections?.let { summary.piecesOnBoard <= it } == true
            Achievement.HIGHWAY -> won && (summary?.longestStraightRun ?: 0) >= HIGHWAY_RUN
            Achievement.SNAKE -> won && (summary?.longestCornerRun ?: 0) >= SNAKE_RUN
            Achievement.ERASER -> won && (summary?.replacements ?: 0) >= ERASER_REPLACEMENTS
            Achievement.SNAIL -> won && (summary?.reservoirsLeft ?: 0) >= SNAIL_RESERVOIRS

            Achievement.NEVER_GIVE_UP -> won && summary != null && statsBefore.lossStreak(summary.levelId) >= NEVER_GIVE_UP_LOSSES
            Achievement.PERSONAL_BEST -> won && summary?.record == RecordOutcome.NEW_HIGHSCORE

            Achievement.INSTANT_FLOOD -> summary?.lost == true && summary.sectionsLeft == 0
            Achievement.WALL_TO_WALL -> won && summary != null && summary.piecesOnBoard >= summary.freeCells
            Achievement.PHOTO_FINISH -> won && summary?.photoFinish == true
            Achievement.DRAMATIC_FAILURE -> summary?.lost == true && summary.sectionsLeft > DRAMATIC_FAILURE_SECTIONS
            // Decided when the attempt ends, won or lost.
            Achievement.ON_YOUR_NERVES -> (summary?.mostReplacementTaps ?: 0) >= ON_YOUR_NERVES_TAPS

            Achievement.LEVELS_FINISHED_1, Achievement.STAR_COLLECTOR_1, Achievement.STAR_COLLECTOR_2,
            Achievement.PERFECTIONIST_1, Achievement.PACK_1_COMPLETE, Achievement.PACK_1_PERFECT,
            Achievement.APPRENTICE, Achievement.JOURNEYMAN, Achievement.GUILDMASTER,
            Achievement.STAR_COLLECTOR_3, Achievement.STAR_COLLECTOR_4,
            -> error("${achievement.key} is incremental")
        }
    }
}
