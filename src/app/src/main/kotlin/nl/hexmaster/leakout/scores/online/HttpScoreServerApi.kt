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
 * [ScoreServerApi] over `HttpURLConnection`: two small JSON posts do not need an HTTP library. Every
 * call gives up after [timeoutMs]; a failure is mapped to a [ScoreServerException] so the caller
 * knows whether to keep the score.
 */
class HttpScoreServerApi(
    baseUrl: String,
    private val timeoutMs: Long = TIMEOUT_MS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ScoreServerApi {
    private val base = baseUrl.trim().trimEnd('/')

    override suspend fun createSession(serverAuthCode: String): Session =
        ScoreServerJson.session(post("/api/players/sessions", ScoreServerJson.sessionRequest(serverAuthCode), accessToken = null))

    override suspend fun submitScore(accessToken: String, level: Int, score: Int) {
        post("/api/scores", ScoreServerJson.scoreRequest(level, score), accessToken)
    }

    private suspend fun post(path: String, body: String, accessToken: String?): String = try {
        withTimeout(timeoutMs) { runInterruptible(dispatcher) { exchange(path, body, accessToken) } }
    } catch (timeout: TimeoutCancellationException) {
        throw ScoreServerException.Unavailable("$path timed out", timeout)
    }

    private fun exchange(path: String, body: String, accessToken: String?): String {
        val connection = try {
            (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = CONNECT_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
        } catch (error: IOException) {
            throw ScoreServerException.Unavailable("$path could not be opened", error)
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
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
        const val TIMEOUT_MS = 15_000L
        private const val CONNECT_TIMEOUT_MS = 10_000
    }
}
