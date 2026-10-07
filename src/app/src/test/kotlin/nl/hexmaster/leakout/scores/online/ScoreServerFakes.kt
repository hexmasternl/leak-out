package nl.hexmaster.leakout.scores.online

import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.achievements.playgames.ServerAuthCodeSource

/**
 * A scripted score server. Sessions expire at [expiresAt]; [scoreAnswers] are thrown (or not, for
 * null) one per score call, after which every call succeeds; [gate] holds score calls until completed.
 */
class FakeScoreServerApi(var expiresAt: Instant = Instant.parse("2999-01-01T00:00:00Z")) : ScoreServerApi {
    val sessions = mutableListOf<String>()
    val scores = mutableListOf<Triple<String, Int, Int>>()
    val scoreAnswers = ArrayDeque<ScoreServerException?>()
    var sessionFailure: ScoreServerException? = null
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun createSession(serverAuthCode: String): Session {
        sessions += serverAuthCode
        sessionFailure?.let { throw it }
        return Session("token-${sessions.size}", expiresAt)
    }

    override suspend fun submitScore(accessToken: String, level: Int, score: Int) {
        scores += Triple(accessToken, level, score)
        gate?.await()
        scoreAnswers.removeFirstOrNull()?.let { throw it }
    }
}

/** Hands out numbered codes, or throws when [failing]. */
class FakeServerAuthCodes(var failing: Boolean = false) : ServerAuthCodeSource {
    val requests = mutableListOf<String>()

    override suspend fun serverAuthCode(webClientId: String): String {
        requests += webClientId
        if (failing) throw IllegalStateException("Play Games refused")
        return "code-${requests.size}"
    }
}

/** A [PendingScoreQueue] in memory with the same best-per-level rules. */
class InMemoryPendingScoreQueue(initial: Map<Int, Int> = emptyMap()) : PendingScoreQueue {
    private val state = MutableStateFlow(initial)
    override val pending: Flow<Map<Int, Int>> = state

    val current: Map<Int, Int> get() = state.value

    override suspend fun offer(level: Int, score: Int) {
        if ((state.value[level] ?: -1) < score) state.value = state.value + (level to score)
    }

    override suspend fun remove(level: Int, score: Int) {
        if (state.value[level] == score) state.value = state.value - level
    }
}

fun unavailable() = ScoreServerException.Unavailable("offline")

fun unauthorized() = ScoreServerException.Unauthorized("401")

fun rejected() = ScoreServerException.Rejected("400")
