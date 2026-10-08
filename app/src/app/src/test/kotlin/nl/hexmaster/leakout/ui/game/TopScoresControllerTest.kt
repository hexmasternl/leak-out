package nl.hexmaster.leakout.ui.game

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.scores.online.LeaderboardEntry
import nl.hexmaster.leakout.scores.online.ScoreServerConfig
import nl.hexmaster.leakout.scores.online.TopScores
import nl.hexmaster.leakout.scores.online.TopScoresState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** One opened top 10 dialog: when it loads (spec `level-top-scores`). */
class TopScoresControllerTest {
    private val signIn = MutableStateFlow<SignInState>(SignInState.SignedIn("Ann"))
    private val answers = ArrayDeque<TopScoresState>()
    private val loads = mutableListOf<Int>()
    private var gate: CompletableDeferred<Unit>? = null
    private val scores = TopScores { level ->
        loads += level
        gate?.await()
        answers.removeFirstOrNull() ?: TopScoresState.Empty
    }
    private var signIns = 0
    private val access = TopScoresAccess(scores, signIn) { signIns++ }
    private val loaded = TopScoresState.Loaded(listOf(LeaderboardEntry(1, "Ada", 2140, isCurrentPlayer = false)))

    @Test
    fun `it loads the level when it opens, from the loading state`() = runTest {
        gate = CompletableDeferred()
        answers += loaded
        val controller = TopScoresController(7, access, backgroundScope)
        controller.start()
        testScheduler.runCurrent()
        assertEquals(TopScoresState.Loading, controller.state.value)
        gate!!.complete(Unit)
        testScheduler.runCurrent()
        assertEquals(loaded, controller.state.value)
        assertEquals(listOf(7), loads)
    }

    @Test
    fun `try again loads once more`() = runTest {
        answers += listOf(TopScoresState.Failed, loaded)
        val controller = TopScoresController(7, access, backgroundScope)
        controller.start()
        testScheduler.runCurrent()
        assertEquals(TopScoresState.Failed, controller.state.value)
        controller.retry()
        assertEquals(TopScoresState.Loading, controller.state.value)
        testScheduler.runCurrent()
        assertEquals(loaded, controller.state.value)
        assertEquals(listOf(7, 7), loads)
    }

    @Test
    fun `signing in from the dialog loads the top 10`() = runTest {
        signIn.value = SignInState.SignedOut
        answers += listOf(TopScoresState.SignedOut(canSignIn = true), loaded)
        val controller = TopScoresController(7, access, backgroundScope)
        controller.start()
        testScheduler.runCurrent()
        controller.signIn()
        assertEquals(1, signIns)
        signIn.value = SignInState.SigningIn
        testScheduler.runCurrent()
        assertEquals(1, loads.size)
        signIn.value = SignInState.SignedIn("Ann")
        testScheduler.runCurrent()
        assertEquals(loaded, controller.state.value)
        assertEquals(2, loads.size)
    }

    @Test
    fun `being signed in already does not load twice`() = runTest {
        val controller = TopScoresController(7, access, backgroundScope)
        controller.start()
        testScheduler.runCurrent()
        assertEquals(1, loads.size)
    }

    @Test
    fun `a retry replaces a load that is still running`() = runTest {
        gate = CompletableDeferred()
        answers += listOf(TopScoresState.Failed, loaded)
        val controller = TopScoresController(7, access, backgroundScope)
        controller.start()
        testScheduler.runCurrent()
        controller.retry()
        testScheduler.runCurrent()
        gate!!.complete(Unit)
        testScheduler.runCurrent()
        // Both loads started, but the first was cancelled while waiting: only one took an answer.
        assertEquals(2, loads.size)
        assertEquals(1, answers.size)
        assertEquals(TopScoresState.Failed, controller.state.value)
    }

    @Test
    fun `the top 10 is offered only for a build with a score server`() {
        val configured = ScoreServerConfig("https://scores.example", "web-client")
        assertNotNull(TopScoresAccess.forLibraryLevel(configured, scores, signIn) {})
        assertNull(TopScoresAccess.forLibraryLevel(ScoreServerConfig("", "web-client"), scores, signIn) {})
        assertNull(TopScoresAccess.forLibraryLevel(ScoreServerConfig("https://scores.example", ""), scores, signIn) {})
        assertNull(TopScoresAccess.forLibraryLevel(ScoreServerConfig("http://scores.example", "web-client"), scores, signIn) {})
    }
}
