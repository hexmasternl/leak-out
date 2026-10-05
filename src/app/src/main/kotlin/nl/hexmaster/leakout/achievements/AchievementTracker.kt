package nl.hexmaster.leakout.achievements

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.AchievementRules
import nl.hexmaster.leakout.core.achievements.AttemptSummary
import nl.hexmaster.leakout.core.achievements.PlayerStats
import nl.hexmaster.leakout.history.LevelHistoryRepository

/**
 * Ties the pure [AchievementRules] to the stores. Each call reads the stats, the stored
 * achievements and the level history, evaluates, and writes the result in one transaction; calls
 * run one at a time, so two attempts can never both unlock the same achievement.
 *
 * The callers make sure each attempt is passed in once ([nl.hexmaster.leakout.ui.game.GameSession]
 * does); the tracker itself counts whatever it is given.
 *
 * @param scope outlives every screen (the application's), for the work started by [attemptAbandoned],
 *   [tipsReset] and [launchReconcile], which must finish even when the screen is left at that moment.
 */
class AchievementTracker(
    private val achievements: AchievementRepository,
    private val history: LevelHistoryRepository,
    private val scope: CoroutineScope,
) {
    private val lock = Mutex()

    /**
     * A won or lost attempt. On a win the history must already hold it (record the win first), so
     * the progress achievements see it. Returns what the attempt unlocked, for the result dialog.
     */
    suspend fun onAttemptEnded(summary: AttemptSummary): List<Achievement> = update(summary) { it.after(summary) }

    /** An attempt restarted or exited before it ended: only its [placements] count. Returns what that unlocked. */
    suspend fun onAttemptAbandoned(placements: Int): List<Achievement> = update(null) { it.afterAbandoned(placements) }

    /** The seen tips were reset with "Show tips again": counted in the stats. Returns what that unlocked. */
    suspend fun onTipsReset(): List<Achievement> = update(null) { it.afterTipsReset() }

    /**
     * Awards the progress and lifetime achievements the stored history and stats have earned: at
     * start-up (players who finished levels before achievements existed) and after a history change.
     * Idempotent: a second call unlocks nothing.
     */
    suspend fun reconcile(): List<Achievement> = update(null) { it }

    /** [onAttemptAbandoned] in the tracker's own scope, so leaving the screen does not cancel it. */
    fun attemptAbandoned(placements: Int): Job = scope.launch { onAttemptAbandoned(placements) }

    /** [onTipsReset] in the tracker's own scope, so leaving the settings screen does not cancel it. */
    fun tipsReset(): Job = scope.launch { onTipsReset() }

    /** [reconcile] in the tracker's own scope. */
    fun launchReconcile(): Job = scope.launch { reconcile() }

    private suspend fun update(summary: AttemptSummary?, next: (PlayerStats) -> PlayerStats): List<Achievement> =
        // A started update finishes: an attempt's placements are counted once, not lost halfway.
        withContext(NonCancellable) {
            lock.withLock {
                achievements.transaction {
                    val before = achievements.stats()
                    val after = next(before)
                    val stored = achievements.stored()
                    val stars = history.observeAll().first().associate { it.levelId to it.stars }
                    val evaluation = AchievementRules.evaluate(summary, before, after, stars, stored)
                    val stepsChanged = evaluation.steps.any { (achievement, steps) -> (stored[achievement]?.steps ?: 0) != steps }
                    if (evaluation.newlyUnlocked.isNotEmpty() || stepsChanged || after != before) {
                        achievements.apply(evaluation, after)
                    }
                    evaluation.newlyUnlocked
                }
            }
        }
}
