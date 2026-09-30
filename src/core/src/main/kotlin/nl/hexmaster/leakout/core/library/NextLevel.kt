package nl.hexmaster.leakout.core.library

/**
 * The next level to be played: the one after the highest completed level, level 1 when nothing is
 * completed, the last level when everything is.
 *
 * @param page the library page holding [levelId].
 * @param isUnlocked whether [levelId] can be started by the [LevelLibraryRules].
 * @param highestCompleted the highest completed level id, 0 when none is.
 */
data class NextLevel(val levelId: Int, val page: Int, val isUnlocked: Boolean, val highestCompleted: Int) {
    /** Where "Continue" goes: start [levelId], or the library on the page of [highestCompleted] when it is locked. */
    val continueTarget: ContinueTarget
        get() = if (isUnlocked) {
            ContinueTarget.StartLevel(levelId)
        } else {
            ContinueTarget.OpenLibrary(LevelLibraryLayout.pageOf(highestCompleted.coerceAtLeast(1)))
        }
}

/** What "Continue" on the play menu does. */
sealed interface ContinueTarget {
    data class StartLevel(val levelId: Int) : ContinueTarget

    /** The next level cannot be started; show the library on [page] so the star hint explains why. */
    data class OpenLibrary(val page: Int) : ContinueTarget
}

/**
 * The one rule for "next level to be played", shared by "Continue" and "Select level" so they agree.
 * A level is completed when it has a history record, including a 0-star win, as in [LevelLibraryRules].
 */
object NextLevelResolver {
    /**
     * @param totalLevels the number of levels, ids `1..totalLevels`.
     * @param stars best stars per finished level id; a level missing from the map is unfinished.
     */
    fun resolve(
        totalLevels: Int,
        stars: Map<Int, Int>,
        config: LevelLibraryConfig = LevelLibraryConfig.Default,
    ): NextLevel {
        val count = totalLevels.coerceAtLeast(0)
        val highest = stars.keys.filter { it in 1..count }.maxOrNull() ?: 0
        val levelId = (highest + 1).coerceAtMost(count).coerceAtLeast(1)
        val library = LevelLibraryRules.compute(count, stars, config)
        val unlocked = library.tile(levelId)?.isPlayable == true
        return NextLevel(levelId, LevelLibraryLayout.pageOf(levelId), unlocked, highest)
    }
}
