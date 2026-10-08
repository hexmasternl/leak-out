package nl.hexmaster.leakout.scores.online

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout

/**
 * [ScoreServerApi] over `HttpURLConnection`: a few small JSON calls do not need an HTTP library. Every
 * call gives up after [timeoutMs]; a failure is mapped to a [ScoreServerException] so the caller
 * knows whether to keep the score. The server scales to zero and its ingress holds a request while it
 * starts, so a cold start shows up as a slow answer: [readTimeoutMs] is long enough to wait it out,
 * while [connectTimeoutMs] stays short so an unreachable server fails fast.
 */
class HttpScoreServerApi(
    baseUrl: String,
    private val timeoutMs: Long = TIMEOUT_MS,
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = READ_TIMEOUT_MS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ScoreServerApi {
    private val base = baseUrl.trim().trimEnd('/')

    override suspend fun createSession(serverAuthCode: String): Session =
        ScoreServerJson.session(call("POST", "/api/players/sessions", ScoreServerJson.sessionRequest(serverAuthCode), accessToken = null))

    override suspend fun submitScore(accessToken: String, level: Int, score: Int) {
        call("POST", "/api/scores", ScoreServerJson.scoreRequest(level, score), accessToken)
    }

    override suspend fun topScores(accessToken: String, level: Int): LevelLeaderboard =
        ScoreServerJson.leaderboard(call("GET", "/api/levels/$level/scores/top", body = null, accessToken))

    private suspend fun call(method: String, path: String, body: String?, accessToken: String?): String = try {
        withTimeout(timeoutMs) { runInterruptible(dispatcher) { exchange(method, path, body, accessToken) } }
    } catch (timeout: TimeoutCancellationException) {
        throw ScoreServerException.Unavailable("$path timed out", timeout)
    }

    private fun exchange(method: String, path: String, body: String?, accessToken: String?): String {
        val connection = try {
            (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = body != null
                if (body != null) setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
        } catch (error: IOException) {
            throw ScoreServerException.Unavailable("$path could not be opened", error)
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            return when (status) {
                in 200..299 -> connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                401 -> throw ScoreServerException.Unauthorized("$path answered 401")
                in 400..499 -> throw ScoreServerException.Rejected("$path answered $status")
                else -> throw ScoreServerException.Unavailable("$path answered $status")
            }
        } catch (error: IOException) {
            throw ScoreServerException.Unavailable("$path failed", error)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** A whole call, including the connect; the spec's upper limit. */
        const val TIMEOUT_MS = 35_000L

        /** Opening the connection; a cold start does not slow this down. */
        const val CONNECT_TIMEOUT_MS = 10_000

        /** Waiting for the answer once connected; covers a cold start of the server (about 15 s). */
        const val READ_TIMEOUT_MS = 30_000
    }
}
