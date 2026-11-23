package nl.hexmaster.leakout.scores.online

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.achievements.playgames.RecordingLog
import nl.hexmaster.leakout.achievements.playgames.ServerAuthCodeSource
import nl.hexmaster.leakout.achievements.playgames.SignInState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The daily pipe client: its requests, the JSON it reads, and how 404, 409 and failures come out. */
class DailyPipeApiTest {
    private val date = LocalDate.of(2026, 11, 2)
    private val streak = DailyStreak(current = 4, best = 7, wonToday = true)
    private val pipe = DailyPipe(date, """{"id":20261102}""", 1234L, streak, DailyBestScore(380, 2))
    private val result = DailyScoreResult(date, DailyBestScore(380, 2), isNewBest = true, rank = 5, streak = streak)
    private val top = DailyTopScores(date, listOf(DailyLeaderboardEntry(1, "Ada", 400, 3, false)), player = DailyPlayerRank(5, 120, 1))

    // HTTP client against a local server.

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply { start() }
    private val base = "http://127.0.0.1:${server.address.port}"
    private val requests = mutableListOf<Triple<String, String?, String>>()

    @After
    fun stop() = server.stop(0)

    private fun answer(path: String, status: Int, body: String = "") {
        server.createContext(path) { exchange ->
            requests += Triple(
                "${exchange.requestMethod} ${exchange.requestURI.path}",
                exchange.requestHeaders.getFirst("Authorization"),
                exchange.requestBody.readBytes().decodeToString(),
            )
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
    }

    @Test
    fun `today's pipe is read with the level kept as JSON for the parser`() = runBlocking {
        answer(
            "/api/daily-pipes/today",
            200,
            """{"date":"2026-11-02","level":{"id":20261102,"name":"Daily Pipe 2026-11-02"},"stackSeed":9007199254740991,""" +
                """"streak":{"current":3,"best":7,"wonToday":false},"bestScore":null}""",
        )

        val read = HttpScoreServerApi(base).todaysDailyPipe("tok")

        assertEquals(date, read.date)
        assertEquals("""{"id":20261102,"name":"Daily Pipe 2026-11-02"}""", read.levelJson)
        assertEquals(9_007_199_254_740_991L, read.stackSeed)
        assertEquals(DailyStreak(3, 7, wonToday = false), read.streak)
        assertNull(read.bestScore)
        assertEquals(Triple("GET /api/daily-pipes/today", "Bearer tok", ""), requests.single())
    }

    @Test
    fun `a result is posted for the pipe's date and the answer read`() = runBlocking {
        answer(
            "/api/daily-pipes/2026-11-02/scores",
            200,
            """{"date":"2026-11-02","bestScore":{"score":380,"stars":2},"isNewBest":true,"rank":5,"streak":{"current":4,"best":7,"wonToday":true}}""",
        )

        assertEquals(result, HttpScoreServerApi(base).submitDailyScore("tok", date, 380, 2))
        assertEquals(Triple("POST /api/daily-pipes/2026-11-02/scores", "Bearer tok", """{"score":380,"stars":2}"""), requests.single())
    }

    @Test
    fun `the day's top scores are read in ranking order`() = runBlocking {
        answer(
            "/api/daily-pipes/2026-11-02/scores/top",
            200,
            """{"date":"2026-11-02","entries":[{"position":2,"displayName":"Me","score":380,"stars":2,"isCurrentPlayer":true},""" +
                """{"position":1,"displayName":"Ada","score":400,"stars":3,"isCurrentPlayer":false}],"player":{"position":2,"score":380,"stars":2}}""",
        )

        val read = HttpScoreServerApi(base).dailyTopScores("tok", date)

        assertEquals(listOf("Ada", "Me"), read.entries.map { it.displayName })
        assertEquals(DailyPlayerRank(position = 2, score = 380, stars = 2), read.player)
        assertTrue(read.entries[1].isCurrentPlayer)
    }

    @Test
    fun `a top scores answer without the player has no own place`() = runBlocking {
        answer("/api/daily-pipes/2026-11-02/scores/top", 200, """{"date":"2026-11-02","entries":[],"player":null}""")

        assertNull(HttpScoreServerApi(base).dailyTopScores("tok", date).player)
    }

    @Test
    fun `a player outside the entries keeps their own position and best score`() = runBlocking {
        answer(
            "/api/daily-pipes/2026-11-02/scores/top",
            200,
            """{"date":"2026-11-02","entries":[{"position":1,"displayName":"Ada","score":512,"stars":3,"isCurrentPlayer":false}],""" +
                """"player":{"position":76,"score":125,"stars":1}}""",
        )

        assertEquals(DailyPlayerRank(position = 76, score = 125, stars = 1), HttpScoreServerApi(base).dailyTopScores("tok", date).player)
    }

    @Test
    fun `404 and 409 are rejections carrying their status`() = runBlocking {
        answer("/api/daily-pipes/today", 404)
        answer("/api/daily-pipes/2026-11-02/scores", 409)

        assertEquals(404, rejectedStatus { HttpScoreServerApi(base).todaysDailyPipe("tok") })
        assertEquals(409, rejectedStatus { HttpScoreServerApi(base).submitDailyScore("tok", date, 1, 1) })
    }

    @Test
    fun `an unreadable answer is the server's fault`() = runBlocking {
        answer("/api/daily-pipes/today", 200, """{"date":"yesterday"}""")

        try {
            HttpScoreServerApi(base).todaysDailyPipe("tok")
            fail("expected Unavailable")
        } catch (expected: ScoreServerException.Unavailable) {
            assertTrue(expected.message!!.contains("daily pipe"))
        }
    }

    private suspend fun rejectedStatus(call: suspend () -> Unit): Int? = try {
        call()
        fail("expected Rejected")
        null
    } catch (rejected: ScoreServerException.Rejected) {
        rejected.status
    }

    // DailyPipeApi on a scripted server.

    private val fake = FakeDailyPipeServer()
    private val codes = FakeServerAuthCodes()
    private val signIn = MutableStateFlow<SignInState>(SignInState.SignedIn("Ann"))
    private val codeSource = MutableStateFlow<ServerAuthCodeSource?>(codes)
    private val log = RecordingLog()
    private val sessionApi = FakeScoreServerApi()
    private val client = DailyPipeApi(fake, ScoreServerSession(sessionApi, "web-client"), signIn, codeSource, log)

    @Test
    fun `today's pipe is downloaded with a server token`() = runBlocking {
        fake.today = { pipe }

        assertEquals(DailyPipeDownload.Available(pipe), client.today())
        assertEquals(listOf("token-1"), fake.tokens)
    }

    @Test
    fun `no pipe yet is not a failure`() = runBlocking {
        fake.today = { throw ScoreServerException.Rejected("404", 404) }

        assertEquals(DailyPipeDownload.NotPublished, client.today())
        assertEquals(emptyList<String>(), log.failures)
    }

    @Test
    fun `offline, another rejection or signed out fail the download`() = runBlocking {
        fake.today = { throw unavailable() }
        assertEquals(DailyPipeDownload.Failed, client.today())

        fake.today = { throw ScoreServerException.Rejected("400", 400) }
        assertEquals(DailyPipeDownload.Failed, client.today())

        signIn.value = SignInState.SignedOut
        fake.today = { pipe }
        assertEquals(DailyPipeDownload.Failed, client.today())
    }

    @Test
    fun `without an activity for a server auth code the download fails`() = runBlocking {
        codeSource.value = null
        fake.today = { pipe }

        assertEquals(DailyPipeDownload.Failed, client.today())
    }

    @Test
    fun `an expired token signs in again once`() = runBlocking {
        var calls = 0
        fake.today = { if (calls++ == 0) throw unauthorized() else pipe }

        assertEquals(DailyPipeDownload.Available(pipe), client.today())
        assertEquals(listOf("token-1", "token-2"), fake.tokens)
    }

    @Test
    fun `a win is submitted and the day's top scores loaded`() = runBlocking {
        fake.submit = { result }
        fake.top = { top }

        assertEquals(DailySubmission.Accepted(result, top), client.submit(date, 380, 2))
        assertEquals(listOf(Triple(date, 380, 2)), fake.submissions)
    }

    @Test
    fun `a submission after the day ended says so`() = runBlocking {
        fake.submit = { throw ScoreServerException.Rejected("409", 409) }

        assertEquals(DailySubmission.Ended, client.submit(date, 380, 2))
    }

    @Test
    fun `a failed submission can be tried again`() = runBlocking {
        fake.submit = { throw unavailable() }
        assertEquals(DailySubmission.Failed, client.submit(date, 380, 2))

        fake.submit = { throw ScoreServerException.Rejected("400", 400) }
        assertEquals(DailySubmission.Failed, client.submit(date, 380, 2))
    }

    @Test
    fun `top scores that fail to load leave the submission accepted`() = runBlocking {
        fake.submit = { result }
        fake.top = { throw unavailable() }

        assertEquals(DailySubmission.Accepted(result, null), client.submit(date, 380, 2))
        assertEquals(listOf("daily top scores"), log.failures)
    }

    @Test
    fun `the day's top scores load on their own`() = runBlocking {
        fake.top = { top }

        assertEquals(DailyTopResult.Loaded(top), client.top(date))
        assertEquals(listOf(date), fake.topDates)
    }

    @Test
    fun `top scores that fail to load or a signed-out player give a failed leaderboard`() = runBlocking {
        fake.top = { throw unavailable() }
        assertEquals(DailyTopResult.Failed, client.top(date))

        signIn.value = SignInState.SignedOut
        fake.top = { top }
        assertEquals(DailyTopResult.Failed, client.top(date))
    }

    @Test
    fun `top scores with an expired token sign in again once`() = runBlocking {
        var calls = 0
        fake.top = { if (calls++ == 0) throw unauthorized() else top }

        assertEquals(DailyTopResult.Loaded(top), client.top(date))
        assertEquals(listOf("token-1", "token-2"), fake.tokens)
    }

    /** A scripted [DailyPipeServerApi]. */
    private class FakeDailyPipeServer : DailyPipeServerApi {
        val tokens = mutableListOf<String>()
        val submissions = mutableListOf<Triple<LocalDate, Int, Int>>()
        var today: () -> DailyPipe = { error("no answer") }
        var submit: () -> DailyScoreResult = { error("no answer") }
        var top: () -> DailyTopScores = { error("no answer") }

        override suspend fun todaysDailyPipe(accessToken: String): DailyPipe {
            tokens += accessToken
            return today()
        }

        override suspend fun submitDailyScore(accessToken: String, date: LocalDate, score: Int, stars: Int): DailyScoreResult {
            submissions += Triple(date, score, stars)
            return submit()
        }

        val topDates = mutableListOf<LocalDate>()

        override suspend fun dailyTopScores(accessToken: String, date: LocalDate): DailyTopScores {
            tokens += accessToken
            topDates += date
            return top()
        }
    }
}
