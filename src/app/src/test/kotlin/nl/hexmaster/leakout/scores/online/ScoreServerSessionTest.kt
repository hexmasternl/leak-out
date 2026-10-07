package nl.hexmaster.leakout.scores.online

import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The token is reused while valid and replaced near its expiry (spec: authenticates with a server auth code). */
class ScoreServerSessionTest {
    private val issued = Instant.parse("2026-10-07T12:00:00Z")
    private var now = issued
    private val api = FakeScoreServerApi(expiresAt = issued + Duration.ofHours(1))
    private val codes = FakeServerAuthCodes()
    private val session = ScoreServerSession(api, "web-client", now = { now })

    @Test
    fun `the first token signs in with a code for the web client`() = runTest {
        assertEquals("token-1", session.token(codes))
        assertEquals(listOf("web-client"), codes.requests)
        assertEquals(listOf("code-1"), api.sessions)
    }

    @Test
    fun `a valid token is reused`() = runTest {
        session.token(codes)
        now = issued + Duration.ofMinutes(10)
        assertEquals("token-1", session.token(codes))
        assertEquals(1, api.sessions.size)
    }

    @Test
    fun `a token about to expire is replaced`() = runTest {
        session.token(codes)
        now = issued + Duration.ofMinutes(59) + Duration.ofSeconds(30)
        assertEquals("token-2", session.token(codes))
    }

    @Test
    fun `an invalidated token is replaced`() = runTest {
        session.token(codes)
        session.invalidate()
        assertEquals("token-2", session.token(codes))
    }

    @Test
    fun `concurrent requests sign in once`() = runTest {
        val first = async { session.token(codes) }
        val second = async { session.token(codes) }
        assertEquals("token-1", first.await())
        assertEquals("token-1", second.await())
        assertEquals(1, api.sessions.size)
    }

    @Test
    fun `a refused code is unavailable`() = runTest {
        codes.failing = true
        try {
            session.token(codes)
            fail("expected a failure")
        } catch (error: ScoreServerException) {
            assertTrue(error is ScoreServerException.Unavailable)
        }
        assertTrue(api.sessions.isEmpty())
    }
}
