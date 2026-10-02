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

/** The sync of a build without Play Games: every call is dropped. */
object NoOpAchievementSync : AchievementSync {
    override suspend fun unlock(achievement: Achievement) = Unit

    override suspend fun setSteps(achievement: Achievement, steps: Int) = Unit
}
