package nl.hexmaster.leakout.scores.online

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.hexmaster.leakout.achievements.playgames.PlayGamesLog
import nl.hexmaster.leakout.achievements.playgames.ServerAuthCodeSource
import nl.hexmaster.leakout.achievements.playgames.SignInState

/** How the sending of a won level's score is going; drives the status dot. */
enum class ScorePostStatus {
    InProgress,

    /** The score reached the server. */
    Succeeded,

    /** The score is still queued (or the server refused it). */
    Failed,
}

/** What the game screen needs: hand over a win, get its sending status (null: nothing is sent). */
fun interface ScoreSubmissions {
    fun submitWin(level: Int, score: Int): StateFlow<ScorePostStatus>?
}

/**
 * Sends the scores of won levels to the score server through the [queue] (spec
 * `online-score-submission`). A win is queued and the queue is flushed; a flush also runs when the
 * player becomes signed in with an activity attached (app start, sign-in) and when the device gets
 * a network connection. Only one flush runs at a time, and every flush runs in [scope], so leaving
 * a screen never cancels one.
 */
class ScoreSubmitter(
    private val config: ScoreServerConfig,
    private val api: ScoreServerApi,
    private val queue: PendingScoreQueue,
    private val signIn: StateFlow<SignInState>,
    private val serverAuthCodes: StateFlow<ServerAuthCodeSource?>,
    private val network: NetworkMonitor,
    private val scope: CoroutineScope,
    private val session: ScoreServerSession = ScoreServerSession(api, config.webClientId),
    private val log: PlayGamesLog = PlayGamesLog.Android,
) : ScoreSubmissions {
    private val flushLock = Mutex()

    // The last answer per level, so a win whose score an earlier flush already sent knows how it went.
    private val answers = ConcurrentHashMap<Int, Answer>()

    private val canSend: Boolean get() = config.enabled && signIn.value is SignInState.SignedIn

    /** Watches the sign-in, the attached activity and the network; once per process. Idle without a configuration. */
    fun start(): Job? {
        if (!config.enabled) return null
        return scope.launch {
            launch {
                combine(signIn, serverAuthCodes) { state, codes -> state is SignInState.SignedIn && codes != null }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { flush() }
            }
            launch { guarded("network watch") { network.availability().collect { flush() } } }
        }
    }

    /** Queues [score] for [level] and sends the queue, when the player is signed in and a server is configured. */
    override fun submitWin(level: Int, score: Int): StateFlow<ScorePostStatus>? {
        if (!canSend) return null
        val status = MutableStateFlow(ScorePostStatus.InProgress)
        scope.launch {
            status.value = guarded("queue level $level") {
                queue.offer(level, score)
                flush()
                statusOf(level, score)
            } ?: ScorePostStatus.Failed
        }
        return status.asStateFlow()
    }

    /** Sends every queued score, unless signing in to the server is impossible right now. */
    suspend fun flush() {
        guarded("flush") {
            flushLock.withLock {
                val codes = serverAuthCodes.value
                if (!canSend || codes == null) return@withLock
                for ((level, score) in queue.snapshot().toSortedMap()) {
                    when (val answer = send(codes, level, score)) {
                        Answer.Accepted, Answer.Rejected -> {
                            answers[level] = answer
                            queue.remove(level, score)
                        }
                        // Offline or the server is down: the rest would fail the same way.
                        Answer.Unavailable -> break
                    }
                }
            }
        }
    }

    // The queue's storage can fail too; that must never take the app's scope (and the app) down.
    private suspend fun <T> guarded(what: String, block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        log.failed("score server $what", error)
        null
    }

    private suspend fun send(codes: ServerAuthCodeSource, level: Int, score: Int): Answer = try {
        try {
            api.submitScore(session.token(codes), level, score)
        } catch (expired: ScoreServerException.Unauthorized) {
            session.invalidate()
            api.submitScore(session.token(codes), level, score)
        }
        Answer.Accepted
    } catch (rejected: ScoreServerException.Rejected) {
        log.failed("score for level $level", rejected)
        Answer.Rejected
    } catch (failed: ScoreServerException) {
        // Unavailable, or Unauthorized twice: keep the score for the next flush.
        log.failed("score for level $level", failed)
        Answer.Unavailable
    }

    /** Succeeded once [level] left the queue because the server took a score at least as high as [score]. */
    private suspend fun statusOf(level: Int, score: Int): ScorePostStatus {
        val stillQueued = (queue.snapshot()[level] ?: -1) >= score
        return if (!stillQueued && answers[level] == Answer.Accepted) ScorePostStatus.Succeeded else ScorePostStatus.Failed
    }

    private enum class Answer { Accepted, Rejected, Unavailable }
}
