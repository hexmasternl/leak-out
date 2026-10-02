package nl.hexmaster.leakout.achievements.playgames

import android.app.Activity
import android.content.Context
import com.google.android.gms.games.AchievementsClient
import com.google.android.gms.games.PlayGames
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import nl.hexmaster.leakout.achievements.AchievementResources
import nl.hexmaster.leakout.core.achievements.Achievement

/** Sign-in through the Play Games SDK's clients for [activity]. */
class GmsSignInGateway(private val activity: Activity) : SignInGateway {
    private val signIn = PlayGames.getGamesSignInClient(activity)

    override suspend fun isAuthenticated(): Boolean = signIn.isAuthenticated.await().isAuthenticated

    override suspend fun signIn(): Boolean = signIn.signIn().await().isAuthenticated

    override suspend fun displayName(): String? = PlayGames.getPlayersClient(activity).currentPlayer.await()?.displayName
}

/**
 * Pushes to Play Games with the "immediate" calls, so a failure (offline) reaches the syncer, which
 * logs it and sends everything again on its next push. Never `increment`: a retried increment
 * would count twice, a retried `setSteps` cannot.
 *
 * @param idOf the Play Games id of an achievement (`R.string.achievement_<key>`).
 */
class PlayGamesAchievementSync(
    private val client: AchievementsClient,
    private val idOf: (Achievement) -> String,
) : AchievementSync {
    override suspend fun unlock(achievement: Achievement) {
        client.unlockImmediate(idOf(achievement)).await()
    }

    override suspend fun setSteps(achievement: Achievement, steps: Int) {
        client.setStepsImmediate(idOf(achievement), steps).await()
    }

    companion object {
        /** The sync for [activity], with the ids from `games-ids.xml`. */
        fun of(activity: Activity): PlayGamesAchievementSync {
            val context: Context = activity.applicationContext
            return PlayGamesAchievementSync(PlayGames.getAchievementsClient(activity)) { context.getString(AchievementResources.playGamesId(it)) }
        }
    }
}

/**
 * Opens the Play Games achievements UI over [activity]. A failure (signed out meanwhile, Play Games
 * missing) is only logged: the in-app achievements screen stays where it is.
 */
fun showPlayGamesAchievements(activity: Activity, log: PlayGamesLog = PlayGamesLog.Android) {
    PlayGames.getAchievementsClient(activity).achievementsIntent
        .addOnSuccessListener { intent ->
            // For a result, as the Play Games docs show: the UI reports a sign-out through it.
            @Suppress("DEPRECATION")
            activity.startActivityForResult(intent, REQUEST_ACHIEVEMENTS_UI)
        }
        .addOnFailureListener { error -> log.failed("achievements UI", error) }
}

private const val REQUEST_ACHIEVEMENTS_UI = 9003

/** Waits for a Play Services task without adding the coroutines-play-services dependency for one function. */
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        when {
            task.isCanceled -> continuation.cancel(CancellationException("Play Games task cancelled"))
            task.isSuccessful -> continuation.resume(task.result)
            else -> continuation.resumeWithException(task.exception ?: IllegalStateException("Play Games task failed"))
        }
    }
}
