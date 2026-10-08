package nl.hexmaster.leakout.scores.online

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import nl.hexmaster.leakout.achievements.playgames.PlayGamesLog
import nl.hexmaster.leakout.achievements.playgames.ServerAuthCodeSource
import nl.hexmaster.leakout.achievements.playgames.SignInState

/** What the top 10 dialog shows (spec `level-top-scores`). */
sealed interface TopScoresState {
    /** Waiting for the score server. */
    data object Loading : TopScoresState

    /** At most 10 [entries], highest first. */
    data class Loaded(val entries: List<LeaderboardEntry>) : TopScoresState

    /** The level has no scores yet. */
    data object Empty : TopScoresState

    /** Not signed in to Play Games; [canSignIn] is false when Play Games is not available on this device. */
    data class SignedOut(val canSignIn: Boolean) : TopScoresState

    /** The scores could not be loaded (offline, timeout, server error); Try again may help. */
    data object Failed : TopScoresState
}

/** Loads the top 10 of a level; never [TopScoresState.Loading], which is the caller's state while this runs. */
fun interface TopScores {
    suspend fun load(level: Int): TopScoresState
}

/**
 * Loads the global top 10 of a level from the score server for the signed-in player, with the same
 * [session] as score posting: a 401 signs in to the server again once. Nothing is stored; every
 * opening of the dialog loads afresh.
 */
class TopScoresLoader(
    private val api: ScoreServerApi,
    private val session: ScoreServerSession,
    private val signIn: StateFlow<SignInState>,
    private val serverAuthCodes: StateFlow<ServerAuthCodeSource?>,
    private val log: PlayGamesLog = PlayGamesLog.Android,
) : TopScores {
    override suspend fun load(level: Int): TopScoresState {
        when (val state = signIn.value) {
            is SignInState.SignedIn -> Unit
            else -> return TopScoresState.SignedOut(canSignIn = state != SignInState.Unavailable)
        }
        val codes = serverAuthCodes.value ?: return TopScoresState.Failed
        return try {
            val board = try {
                api.topScores(session.token(codes), level)
            } catch (expired: ScoreServerException.Unauthorized) {
                session.invalidate()
                api.topScores(session.token(codes), level)
            }
            val entries = board.entries.take(MAX_ENTRIES)
            if (entries.isEmpty()) TopScoresState.Empty else TopScoresState.Loaded(entries)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            log.failed("top scores of level $level", error)
            TopScoresState.Failed
        }
    }

    companion object {
        const val MAX_ENTRIES = 10
    }
}
