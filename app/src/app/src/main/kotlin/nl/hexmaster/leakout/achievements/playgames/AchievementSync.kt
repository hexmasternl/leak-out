package nl.hexmaster.leakout.achievements.playgames

import nl.hexmaster.leakout.core.achievements.Achievement

/**
 * Where local achievements are pushed. Both calls are idempotent, so the whole state can be sent
 * again at any time; there is deliberately no `increment`.
 */
interface AchievementSync {
    /** Unlocks the standard [achievement]. */
    suspend fun unlock(achievement: Achievement)

    /** Sets the incremental [achievement]'s progress to [steps]; reaching its total unlocks it. */
    suspend fun setSteps(achievement: Achievement, steps: Int)
}

/**
 * [sync] for only the achievements that have a Play Games id. An achievement added to the catalogue
 * before it was created in the Play Console has an empty id in `games-ids.xml`; its calls are
 * dropped instead of sent with an invalid id. Once the id ships, the next push sends its state.
 */
class WithPlayGamesId(private val sync: AchievementSync, private val idOf: (Achievement) -> String) : AchievementSync {
    override suspend fun unlock(achievement: Achievement) {
        if (idOf(achievement).isNotBlank()) sync.unlock(achievement)
    }

    override suspend fun setSteps(achievement: Achievement, steps: Int) {
        if (idOf(achievement).isNotBlank()) sync.setSteps(achievement, steps)
    }
}

/** The sync of a build without Play Games: every call is dropped. */
object NoOpAchievementSync : AchievementSync {
    override suspend fun unlock(achievement: Achievement) = Unit

    override suspend fun setSteps(achievement: Achievement, steps: Int) = Unit
}
