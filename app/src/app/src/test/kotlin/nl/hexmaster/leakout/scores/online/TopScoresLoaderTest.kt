package nl.hexmaster.leakout.scores.online

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.achievements.playgames.RecordingLog
import nl.hexmaster.leakout.achievements.playgames.ServerAuthCodeSource
import nl.hexmaster.leakout.achievements.playgames.SignInState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Loading the top 10 of a level (spec `level-top-scores`, "Top 10 is loaded from the score server"). */
class TopScoresLoaderTest {
    private val api = FakeScoreServerApi()
    private val codes = FakeServerAuthCodes()
    private val signIn = MutableStateFlow<SignInState>(SignInState.SignedIn("Ann"))
    private val codeSource = MutableStateFlow<ServerAuthCodeSource?>(codes)
    private val session = ScoreServerSession(api, "web-client")
    private val log = RecordingLog()
    private val loader = TopScoresLoader(api, session, signIn, codeSource, log)

    private fun entries(count: Int, own: Int? = null) =
        (1..count).map { LeaderboardEntry(it, "P$it", 2000 - it * 100, isCurrentPlayer = it == own) }

    @Test
    fun `the top 10 of the level is loaded with the player's token`() = runTest {
        api.leaderboard = { LevelLeaderboard(it, entries(10, own = 4)) }
        assertEquals(TopScoresState.Loaded(entries(10, own = 4)), loader.load(7))
        assertEquals(listOf("token-1" to 7), api.topScoreCalls)
    }

    @Test
    fun `fewer than 10 scores are shown as they are`() = runTest {
        api.leaderboard = { LevelLeaderboard(it, entries(3)) }
        assertEquals(TopScoresState.Loaded(entries(3)), loader.load(2))
    }

    @Test
    fun `more than 10 entries are cut to 10`() = runTest {
        api.leaderboard = { LevelLeaderboard(it, entries(12)) }
        assertEquals(TopScoresState.Loaded(entries(10)), loader.load(2))
    }

    @Test
    fun `a level without scores is empty`() = runTest {
        assertEquals(TopScoresState.Empty, loader.load(5))
    }

    @Test
    fun `signed out asks for a sign-in without calling the server`() = runTest {
        for (state in listOf(SignInState.SignedOut, SignInState.SigningIn)) {
            signIn.value = state
            assertEquals(TopScoresState.SignedOut(canSignIn = true), loader.load(7))
        }
        assertTrue(api.topScoreCalls.isEmpty())
        assertTrue(codes.requests.isEmpty())
    }

    @Test
    fun `without Play Games there is no sign-in to offer`() = runTest {
        signIn.value = SignInState.Unavailable
        assertEquals(TopScoresState.SignedOut(canSignIn = false), loader.load(7))
        assertTrue(api.topScoreCalls.isEmpty())
    }

    @Test
    fun `an expired token signs in again once and repeats the request`() = runTest {
        api.leaderboard = { LevelLeaderboard(it, entries(1)) }
        api.topScoreAnswers += unauthorized()
        assertEquals(TopScoresState.Loaded(entries(1)), loader.load(7))
        assertEquals(listOf("token-1" to 7, "token-2" to 7), api.topScoreCalls)
        assertEquals(2, codes.requests.size)
    }

    @Test
    fun `a second 401 fails without a third try`() = runTest {
        api.topScoreAnswers += listOf(unauthorized(), unauthorized())
        assertEquals(TopScoresState.Failed, loader.load(7))
        assertEquals(2, api.topScoreCalls.size)
    }

    @Test
    fun `offline, a rejection or no server auth code fail`() = runTest {
        api.topScoreAnswers += unavailable()
        assertEquals(TopScoresState.Failed, loader.load(7))
        api.topScoreAnswers += rejected()
        assertEquals(TopScoresState.Failed, loader.load(7))
        codes.failing = true
        session.invalidate()
        assertEquals(TopScoresState.Failed, loader.load(7))
        assertEquals(3, log.failures.size)
    }

    @Test
    fun `no attached activity fails`() = runTest {
        codeSource.value = null
        assertEquals(TopScoresState.Failed, loader.load(7))
        assertTrue(api.topScoreCalls.isEmpty())
    }

    @Test
    fun `every load asks the server again`() = runTest {
        loader.load(7)
        loader.load(7)
        assertEquals(2, api.topScoreCalls.size)
    }
}
