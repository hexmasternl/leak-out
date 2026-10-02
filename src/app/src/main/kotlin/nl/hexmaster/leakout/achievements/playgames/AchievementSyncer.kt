package nl.hexmaster.leakout.achievements.playgames

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.AchievementRepository

/**
 * Keeps Play Games in step with the local store, which stays the source of truth: it pushes the
 * full local state whenever the player becomes signed in, whenever the local achievements change
 * while signed in, and whenever an activity [bind]s a new sync. Every call is idempotent, so there
 * is no "already sent" bookkeeping: a failed push is logged, local data is never touched, and the
 * next trigger sends everything again.
 */
class AchievementSyncer(
    private val achievements: AchievementRepository,
    private val signIn: StateFlow<SignInState>,
    private val scope: CoroutineScope,
    private val log: PlayGamesLog = PlayGamesLog.Android,
) {
    // Play Games clients belong to an activity, so the sync comes and goes with it.
    private val bound = MutableStateFlow<AchievementSync?>(null)

    /** Pushes through [sync] from now on (the activity's Play Games client). */
    fun bind(sync: AchievementSync) {
        bound.value = sync
    }

    /** Stops using [sync], unless another one was bound since. */
    fun unbind(sync: AchievementSync) {
        bound.compareAndSet(sync, null)
    }

    /** Starts watching the sign-in state, the local store and the bound sync; once per process. */
    fun start(): Job = scope.launch {
        combine(signIn, achievements.observeAll(), bound) { state, all, sync -> Triple(state, all, sync) }
            // A newer state replaces a push still running: it sends a superset anyway.
            .collectLatest { (state, all, sync) ->
                if (state is SignInState.SignedIn && sync != null) push(sync, all)
            }
    }

    /**
     * Sends [all] through [sync]: an unlock for every unlocked standard achievement and the steps of
     * every incremental one with progress (its total once unlocked). Returns the number of calls
     * that failed; the others are still made.
     */
    suspend fun push(sync: AchievementSync, all: List<AchievementProgress>): Int {
        var failures = 0
        for (progress in all) {
            val achievement = progress.achievement
            val total = achievement.steps
            val sent = when {
                total != null && progress.unlocked -> log.call("setSteps ${achievement.key}") { sync.setSteps(achievement, total) }
                // Play Games refuses 0 steps, and 0 is where it starts anyway.
                total != null && progress.steps > 0 ->
                    log.call("setSteps ${achievement.key}") { sync.setSteps(achievement, progress.steps.coerceAtMost(total)) }
                total == null && progress.unlocked -> log.call("unlock ${achievement.key}") { sync.unlock(achievement) }
                else -> Unit
            }
            if (sent == null) failures++
        }
        return failures
    }
}
