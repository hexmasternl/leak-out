package nl.hexmaster.leakout.achievements.playgames

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The sign-in state: silent check, manual sign-in, failures and the unconfigured build. */
class PlayGamesAuthTest {
    private val log = RecordingLog()

    private fun TestScope.auth(configured: Boolean = true) =
        PlayGamesAuth(configured, backgroundScope, gatewayFor = { error("no activity in JVM tests") }, log = log)

    @Test
    fun `a configured build starts signed out`() = runTest {
        assertEquals(SignInState.SignedOut, auth().state.value)
    }

    @Test
    fun `the silent check finds a signed-in player and their name`() = runTest {
        val auth = auth()
        auth.checkSilently(FakeSignInGateway(authenticated = true, name = "Eduard"))
        assertEquals(SignInState.SignedIn("Eduard"), auth.state.value)
    }

    @Test
    fun `a failed silent check is signed out and shows no sign-in`() = runTest {
        val auth = auth()
        val gateway = FakeSignInGateway(authenticated = false)
        auth.checkSilently(gateway)
        assertEquals(SignInState.SignedOut, auth.state.value)
        assertEquals(0, gateway.signInCalls)
    }

    @Test
    fun `a silent check that throws is signed out and logged`() = runTest {
        val auth = auth()
        auth.checkSilently(object : SignInGateway {
            override suspend fun isAuthenticated(): Boolean = throw IllegalStateException("no Play Games")
            override suspend fun signIn(): Boolean = false
            override suspend fun displayName(): String? = null
        })
        assertEquals(SignInState.SignedOut, auth.state.value)
        assertEquals(listOf("sign-in"), log.failures)
    }

    @Test
    fun `signing in from signed out ends signed in`() = runTest {
        val auth = auth()
        auth.signIn(FakeSignInGateway(signInSucceeds = true, name = "Eduard"))
        assertEquals(SignInState.SignedIn("Eduard"), auth.state.value)
    }

    @Test
    fun `a cancelled sign-in returns to signed out`() = runTest {
        val auth = auth()
        auth.signIn(FakeSignInGateway(signInSucceeds = false))
        assertEquals(SignInState.SignedOut, auth.state.value)
    }

    @Test
    fun `a sign-in that throws returns to signed out`() = runTest {
        val auth = auth()
        auth.signIn(FakeSignInGateway(throwOnSignIn = true))
        assertEquals(SignInState.SignedOut, auth.state.value)
    }

    @Test
    fun `the state is signing in while the sign-in runs`() = runTest {
        val auth = auth()
        val seen = mutableListOf<SignInState>()
        auth.signIn(object : SignInGateway {
            override suspend fun isAuthenticated() = false
            override suspend fun signIn(): Boolean {
                seen += auth.state.value
                return true
            }
            override suspend fun displayName(): String? = null
        })
        assertEquals(listOf<SignInState>(SignInState.SigningIn), seen)
        assertEquals("a missing name is empty", SignInState.SignedIn(""), auth.state.value)
    }

    @Test
    fun `signing in again while signed in does nothing`() = runTest {
        val auth = auth()
        auth.checkSilently(FakeSignInGateway(authenticated = true, name = "Eduard"))
        val gateway = FakeSignInGateway(name = "Someone else")
        auth.signIn(gateway)
        auth.checkSilently(gateway)
        assertEquals(0, gateway.signInCalls)
        assertEquals(SignInState.SignedIn("Eduard"), auth.state.value)
    }

    @Test
    fun `an unconfigured build stays unavailable and never calls Play Games`() = runTest {
        val auth = auth(configured = false)
        val gateway = FakeSignInGateway(authenticated = true)
        auth.checkSilently(gateway)
        auth.signIn(gateway)
        assertEquals(SignInState.Unavailable, auth.state.value)
        assertEquals(0, gateway.signInCalls)
    }
}
