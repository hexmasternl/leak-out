package nl.hexmaster.leakout.scores.online

import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.hexmaster.leakout.achievements.playgames.ServerAuthCodeSource

/**
 * The score server sign-in of this process. The token lives in memory only: it is reused until
 * [margin] before it expires, and a new one costs a fresh Play Games server auth code.
 */
class ScoreServerSession(
    private val api: ScoreServerApi,
    private val webClientId: String,
    private val now: () -> Instant = Instant::now,
    private val margin: Duration = Duration.ofSeconds(60),
) {
    private val lock = Mutex()
    private var session: Session? = null

    /** A token the server should still accept, signing in with a code from [codes] when needed. */
    suspend fun token(codes: ServerAuthCodeSource): String = lock.withLock {
        session?.takeIf { now() < it.expiresAt - margin }?.let { return@withLock it.accessToken }
        val code = try {
            codes.serverAuthCode(webClientId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw ScoreServerException.Unavailable("no server auth code", error)
        }
        api.createSession(code).also { session = it }.accessToken
    }

    /** Forgets the token, after the server refused it. */
    suspend fun invalidate() {
        lock.withLock { session = null }
    }
}
