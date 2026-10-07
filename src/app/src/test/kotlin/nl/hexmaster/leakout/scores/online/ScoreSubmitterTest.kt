package nl.hexmaster.leakout.scores.online

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.achievements.playgames.RecordingLog
import nl.hexmaster.leakout.achievements.playgames.ServerAuthCodeSource
import nl.hexmaster.leakout.achievements.playgames.SignInState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** When scores are queued and sent, and what the status of a win becomes (spec `online-score-submission`). */
class ScoreSubmitterTest {
    private val api = FakeScoreServerApi()
    private val codes = FakeServerAuthCodes()
    private val signIn = MutableStateFlow<SignInState>(SignInState.SignedIn("Ann"))
    private val codeSource = MutableStateFlow<ServerAuthCodeSource?>(codes)
    private var networkAvailable: (() -> Unit)? = null
    private val network = CallbackNetworkMonitor { callback ->
        networkAvailable = callback
        ({ networkAvailable = null })
    }
    private val log = RecordingLog()

    private fun TestScope.submitter(
        queue: PendingScoreQueue = InMemoryPendingScoreQueue(),
        config: ScoreServerConfig = ScoreServerConfig("https://rg-leakout-prod-neu-api.jollywater-e41c70f8.northeurope.azurecontainerapps.io", "web-client"),
    ) = ScoreSubmitter(config, api, queue, signIn, codeSource, network, backgroundScope, log = log)

    private fun TestScope.settle(): Unit = testScheduler.runCurrent()

    @Test
    fun `an online win is sent and leaves the queue`() = runTest {
        val queue = InMemoryPendingScoreQueue()
        val status = submitter(queue).submitWin(4, 108)!!
        assertEquals(ScorePostStatus.InProgress, status.value)
        settle()
        assertEquals(ScorePostStatus.Succeeded, status.value)
        assertEquals(listOf(Triple("token-1", 4, 108)), api.scores)
        assertEquals(emptyMap<Int, Int>(), queue.current)
    }

    @Test
    fun `a total of zero is sent`() = runTest {
        submitter().submitWin(2, 0)
        settle()
        assertEquals(listOf(Triple("token-1", 2, 0)), api.scores)
    }

    @Test
    fun `nothing is queued or sent when signed out, signing in or unavailable`() = runTest {
        for (state in listOf(SignInState.SignedOut, SignInState.SigningIn, SignInState.Unavailable)) {
            signIn.value = state
            val queue = InMemoryPendingScoreQueue()
            assertNull(submitter(queue).submitWin(4, 108))
            settle()
            assertEquals(emptyMap<Int, Int>(), queue.current)
        }
        assertTrue(api.scores.isEmpty() && api.sessions.isEmpty())
    }

    @Test
    fun `nothing is queued or sent without a score server`() = runTest {
        val queue = InMemoryPendingScoreQueue()
        val submitter = submitter(queue, ScoreServerConfig("", "web-client"))
        assertNull(submitter.start())
        assertNull(submitter.submitWin(4, 108))
        settle()
        assertEquals(emptyMap<Int, Int>(), queue.current)
        assertTrue(api.scores.isEmpty())
    }

    @Test
    fun `an offline win stays queued and fails`() = runTest {
        val queue = InMemoryPendingScoreQueue()
        api.scoreAnswers += unavailable()
        val status = submitter(queue).submitWin(4, 108)!!
        settle()
        assertEquals(ScorePostStatus.Failed, status.value)
        assertEquals(mapOf(4 to 108), queue.current)
    }

    @Test
    fun `a failed sign-in keeps every score`() = runTest {
        val queue = InMemoryPendingScoreQueue(mapOf(3 to 120))
        codes.failing = true
        val status = submitter(queue).submitWin(4, 108)!!
        settle()
        assertEquals(ScorePostStatus.Failed, status.value)
        assertEquals(mapOf(3 to 120, 4 to 108), queue.current)
    }

    @Test
    fun `an offline send stops the flush and keeps the rest`() = runTest {
        val queue = InMemoryPendingScoreQueue(mapOf(3 to 120, 5 to 80))
        api.scoreAnswers += unavailable()
        submitter(queue).flush()
        assertEquals(listOf(3), api.scores.map { it.second })
        assertEquals(mapOf(3 to 120, 5 to 80), queue.current)
    }

    @Test
    fun `a rejected score is dropped and the others are still sent`() = runTest {
        val queue = InMemoryPendingScoreQueue(mapOf(3 to 120, 5 to 80))
        api.scoreAnswers += rejected()
        submitter(queue).flush()
        assertEquals(listOf(3, 5), api.scores.map { it.second })
        assertEquals(emptyMap<Int, Int>(), queue.current)
    }

    @Test
    fun `a rejected win fails`() = runTest {
        api.scoreAnswers += rejected()
        val status = submitter().submitWin(4, 108)!!
        settle()
        assertEquals(ScorePostStatus.Failed, status.value)
    }

    @Test
    fun `a refused token signs in again once and resends`() = runTest {
        val queue = InMemoryPendingScoreQueue()
        val submitter = submitter(queue)
        submitter.submitWin(3, 50)
        settle()
        api.scoreAnswers += unauthorized()
        val status = submitter.submitWin(4, 108)!!
        settle()
        assertEquals(ScorePostStatus.Succeeded, status.value)
        assertEquals(listOf("code-1", "code-2"), api.sessions)
        assertEquals(listOf(Triple("token-1", 4, 108), Triple("token-2", 4, 108)), api.scores.drop(1))
        assertEquals(emptyMap<Int, Int>(), queue.current)
    }

    @Test
    fun `a token refused twice keeps the score`() = runTest {
        val queue = InMemoryPendingScoreQueue()
        api.scoreAnswers += unauthorized()
        api.scoreAnswers += unauthorized()
        val status = submitter(queue).submitWin(4, 108)!!
        settle()
        assertEquals(ScorePostStatus.Failed, status.value)
        assertEquals(mapOf(4 to 108), queue.current)
        assertEquals(2, api.scores.size)
    }

    @Test
    fun `a higher score queued during a send stays queued`() = runTest {
        val queue = InMemoryPendingScoreQueue(mapOf(3 to 90))
        val gate = CompletableDeferred<Unit>().also { api.gate = it }
        val submitter = submitter(queue)
        backgroundScope.launch { submitter.flush() }
        testScheduler.runCurrent()
        queue.offer(3, 120)
        gate.complete(Unit)
        testScheduler.runCurrent()
        assertEquals(listOf(90), api.scores.map { it.third })
        assertEquals(mapOf(3 to 120), queue.current)
    }

    @Test
    fun `a win also sends the scores queued earlier`() = runTest {
        val queue = InMemoryPendingScoreQueue(mapOf(3 to 120))
        submitter(queue).submitWin(4, 108)
        settle()
        assertEquals(listOf(3 to 120, 4 to 108), api.scores.map { it.second to it.third })
        assertEquals(emptyMap<Int, Int>(), queue.current)
    }

    @Test
    fun `a lower win sends the queued best and succeeds`() = runTest {
        val queue = InMemoryPendingScoreQueue(mapOf(3 to 120))
        val status = submitter(queue).submitWin(3, 90)!!
        settle()
        assertEquals(ScorePostStatus.Succeeded, status.value)
        assertEquals(listOf(120), api.scores.map { it.third })
    }

    @Test
    fun `flushes never overlap`() = runTest {
        val queue = InMemoryPendingScoreQueue(mapOf(3 to 120))
        val gate = CompletableDeferred<Unit>().also { api.gate = it }
        val submitter = submitter(queue)
        repeat(3) { backgroundScope.launch { submitter.flush() } }
        testScheduler.runCurrent()
        assertEquals(1, api.scores.size)
        gate.complete(Unit)
        settle()
        assertEquals("the later flushes find the queue empty", 1, api.scores.size)
    }

    @Test
    fun `without an attached activity the win is queued but not sent`() = runTest {
        codeSource.value = null
        val queue = InMemoryPendingScoreQueue()
        val status = submitter(queue).submitWin(4, 108)!!
        settle()
        assertEquals(ScorePostStatus.Failed, status.value)
        assertEquals(mapOf(4 to 108), queue.current)
        assertTrue(api.scores.isEmpty())
    }

    @Test
    fun `signing in sends the queue`() = runTest {
        signIn.value = SignInState.SignedOut
        val queue = InMemoryPendingScoreQueue(mapOf(3 to 120))
        submitter(queue).start()
        settle()
        assertTrue(api.scores.isEmpty())
        signIn.value = SignInState.SignedIn("Ann")
        settle()
        assertEquals(listOf(3), api.scores.map { it.second })
        assertEquals(emptyMap<Int, Int>(), queue.current)
    }

    @Test
    fun `an activity attaching sends the queue`() = runTest {
        codeSource.value = null
        val queue = InMemoryPendingScoreQueue(mapOf(3 to 120))
        submitter(queue).start()
        settle()
        assertTrue(api.scores.isEmpty())
        codeSource.value = codes
        settle()
        assertEquals(emptyMap<Int, Int>(), queue.current)
    }

    @Test
    fun `coming back online sends the queue`() = runTest {
        val queue = InMemoryPendingScoreQueue(mapOf(3 to 120))
        api.scoreAnswers += unavailable()
        submitter(queue).start()
        settle()
        assertEquals("the start flush failed offline", mapOf(3 to 120), queue.current)
        networkAvailable!!()
        settle()
        assertEquals(emptyMap<Int, Int>(), queue.current)
        assertEquals(2, api.scores.size)
    }

    @Test
    fun `a storage failure fails the win without crashing`() = runTest {
        val broken = object : PendingScoreQueue by InMemoryPendingScoreQueue() {
            override suspend fun offer(level: Int, score: Int) = throw IOException("disk full")
        }
        val status = submitter(broken).submitWin(4, 108)!!
        settle()
        assertEquals(ScorePostStatus.Failed, status.value)
        assertTrue(log.failures.isNotEmpty())
    }
}
