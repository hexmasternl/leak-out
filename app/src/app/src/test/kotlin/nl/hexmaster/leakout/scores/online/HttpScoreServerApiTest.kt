package nl.hexmaster.leakout.scores.online

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The HTTP client against a local server: the requests it makes and how answers map to failures. */
class HttpScoreServerApiTest {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply { start() }
    private val base = "http://127.0.0.1:${server.address.port}"
    private val requests = mutableListOf<Triple<String, String?, String>>()

    @After
    fun stop() = server.stop(0)

    private fun answer(path: String, status: Int, body: String = "", delayMs: Long = 0) {
        server.createContext(path) { exchange ->
            requests += Triple(exchange.requestURI.path, exchange.requestHeaders.getFirst("Authorization"), exchange.requestBody.readBytes().decodeToString())
            Thread.sleep(delayMs)
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
    }

    private inline fun <reified T : ScoreServerException> assertFails(block: () -> Unit) {
        try {
            block()
            fail("expected ${T::class.simpleName}")
        } catch (error: ScoreServerException) {
            assertTrue("got $error", error is T)
        }
    }

    @Test
    fun `sign-in posts the code and reads the session`() = runBlocking {
        answer(
            "/api/players/sessions",
            200,
            """{"accessToken":"tok","expiresAt":"2026-10-07T13:00:00+00:00","playerId":"0f8fad5b-d9cb-469f-a165-70867728950e","displayName":"Ann"}""",
        )
        val session = HttpScoreServerApi("$base/").createSession("4/0Acode")
        assertEquals(Session("tok", Instant.parse("2026-10-07T13:00:00Z")), session)
        assertEquals(Triple("/api/players/sessions", null, """{"serverAuthCode":"4/0Acode"}"""), requests.single())
    }

    @Test
    fun `a score is posted with the bearer token`() = runBlocking {
        answer("/api/scores", 200, """{"level":4,"bestScore":520,"isNewBest":true}""")
        HttpScoreServerApi(base).submitScore("tok", 4, 520)
        assertEquals(Triple("/api/scores", "Bearer tok", """{"level":4,"score":520}"""), requests.single())
    }

    @Test
    fun `401 is unauthorized`() = runBlocking {
        answer("/api/scores", 401)
        assertFails<ScoreServerException.Unauthorized> { HttpScoreServerApi(base).submitScore("old", 4, 520) }
    }

    @Test
    fun `400 is a rejection`() = runBlocking {
        answer("/api/scores", 400, """{"errors":{"level":["The level must be at least 1."]}}""")
        assertFails<ScoreServerException.Rejected> { HttpScoreServerApi(base).submitScore("tok", 0, 520) }
    }

    @Test
    fun `503 is unavailable`() = runBlocking {
        answer("/api/players/sessions", 503)
        assertFails<ScoreServerException.Unavailable> { HttpScoreServerApi(base).createSession("code") }
    }

    @Test
    fun `the top scores are fetched with the bearer token`() = runBlocking {
        var method = ""
        server.createContext("/api/levels/7/scores/top") { exchange ->
            method = exchange.requestMethod
            requests += Triple(exchange.requestURI.path, exchange.requestHeaders.getFirst("Authorization"), exchange.requestBody.readBytes().decodeToString())
            val bytes = """{"level":7,"entries":[{"position":1,"displayName":"Ada","score":2140,"isCurrentPlayer":true}]}""".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        val board = HttpScoreServerApi(base).topScores("tok", 7)
        assertEquals(LevelLeaderboard(7, listOf(LeaderboardEntry(1, "Ada", 2140, isCurrentPlayer = true))), board)
        assertEquals("GET", method)
        assertEquals(Triple("/api/levels/7/scores/top", "Bearer tok", ""), requests.single())
    }

    @Test
    fun `top scores 401 is unauthorized`() = runBlocking {
        answer("/api/levels/7/scores/top", 401)
        assertFails<ScoreServerException.Unauthorized> { HttpScoreServerApi(base).topScores("old", 7) }
    }

    @Test
    fun `top scores 404 is a rejection`() = runBlocking {
        answer("/api/levels/7/scores/top", 404)
        assertFails<ScoreServerException.Rejected> { HttpScoreServerApi(base).topScores("tok", 7) }
    }

    @Test
    fun `top scores 500 is unavailable`() = runBlocking {
        answer("/api/levels/7/scores/top", 500)
        assertFails<ScoreServerException.Unavailable> { HttpScoreServerApi(base).topScores("tok", 7) }
    }

    @Test
    fun `an unreadable session is unavailable`() = runBlocking {
        answer("/api/players/sessions", 200, "<html>")
        assertFails<ScoreServerException.Unavailable> { HttpScoreServerApi(base).createSession("code") }
    }

    @Test
    fun `a refused connection is unavailable`() = runBlocking {
        val closedPort = ServerSocket(0).use { it.localPort }
        assertFails<ScoreServerException.Unavailable> { HttpScoreServerApi("http://127.0.0.1:$closedPort").submitScore("tok", 4, 520) }
    }

    @Test
    fun `a slow server times out as unavailable`() = runBlocking {
        answer("/api/scores", 200, "{}", delayMs = 2_000)
        assertFails<ScoreServerException.Unavailable> { HttpScoreServerApi(base, timeoutMs = 300).submitScore("tok", 4, 520) }
    }

    @Test
    fun `a server that is starting up is waited for beyond the connect timeout`() = runBlocking {
        answer("/api/scores", 200, """{"level":4,"bestScore":520,"isNewBest":true}""", delayMs = 800)
        HttpScoreServerApi(base, timeoutMs = 2_000, connectTimeoutMs = 200, readTimeoutMs = 1_500).submitScore("tok", 4, 520)
        assertEquals("/api/scores", requests.single().first)
    }

    @Test
    fun `a server that does not answer within the read timeout is unavailable`() = runBlocking {
        answer("/api/scores", 200, "{}", delayMs = 1_500)
        assertFails<ScoreServerException.Unavailable> {
            HttpScoreServerApi(base, timeoutMs = 5_000, connectTimeoutMs = 200, readTimeoutMs = 400).submitScore("tok", 4, 520)
        }
    }

    @Test
    fun `the default timeouts wait out a cold start within the spec limit`() {
        assertTrue(HttpScoreServerApi.CONNECT_TIMEOUT_MS <= 10_000)
        assertTrue(HttpScoreServerApi.READ_TIMEOUT_MS >= 30_000)
        assertTrue(HttpScoreServerApi.TIMEOUT_MS <= 35_000)
        assertTrue(HttpScoreServerApi.READ_TIMEOUT_MS < HttpScoreServerApi.TIMEOUT_MS)
    }
}
