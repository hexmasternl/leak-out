package nl.hexmaster.leakout.achievements.playgames

import android.util.Log
import kotlin.coroutines.cancellation.CancellationException

/**
 * Where failed Play Games calls are reported. Play Games is optional, so a failure is logged and
 * the caller carries on; the next push or sign-in tries again.
 */
fun interface PlayGamesLog {
    fun failed(what: String, error: Throwable)

    companion object {
        private const val TAG = "LeakOutPlayGames"

        val Android = PlayGamesLog { what, error -> Log.w(TAG, "Play Games $what failed", error) }
    }
}

/** Runs [block], reporting a failure (except cancellation) to this log and returning null instead. */
internal suspend fun <T> PlayGamesLog.call(what: String, block: suspend () -> T): T? =
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        failed(what, error)
        null
    }
