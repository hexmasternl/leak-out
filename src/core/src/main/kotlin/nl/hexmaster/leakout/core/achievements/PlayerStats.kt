package nl.hexmaster.leakout.core.achievements

/**
 * The player's lifetime stats. They last across attempts and survive a progress reset.
 *
 * @param sectionsPlaced every accepted placement ever, replacements and abandoned attempts included.
 * @param losses attempts ever lost.
 * @param lossStreakByLevel per level id, the losses since the last win on that level; a level
 *   without a streak is left out.
 * @param tipsResets times the seen tips were reset with "Show tips again" on the settings screen.
 */
data class PlayerStats(
    val sectionsPlaced: Int = 0,
    val losses: Int = 0,
    val lossStreakByLevel: Map<Int, Int> = emptyMap(),
    val tipsResets: Int = 0,
) {
    fun lossStreak(levelId: Int): Int = lossStreakByLevel[levelId] ?: 0

    /** The stats after the finished attempt [summary]: its placements added, and a loss counted or the streak cleared by a win. */
    fun after(summary: AttemptSummary): PlayerStats {
        val placed = afterAbandoned(summary.placements)
        return when (summary.outcome) {
            AttemptOutcome.LOST -> placed.copy(
                losses = losses + 1,
                lossStreakByLevel = lossStreakByLevel + (summary.levelId to lossStreak(summary.levelId) + 1),
            )
            AttemptOutcome.WON -> placed.copy(lossStreakByLevel = lossStreakByLevel - summary.levelId)
        }
    }

    /** The stats after an attempt that was restarted or exited before it ended, with [placements] accepted placements. */
    fun afterAbandoned(placements: Int): PlayerStats {
        require(placements >= 0) { "Placements cannot be negative, were $placements" }
        return copy(sectionsPlaced = sectionsPlaced + placements)
    }

    /** The stats after the seen tips were reset once more. */
    fun afterTipsReset(): PlayerStats = copy(tipsResets = tipsResets + 1)

    companion object {
        val NONE = PlayerStats()
    }
}
