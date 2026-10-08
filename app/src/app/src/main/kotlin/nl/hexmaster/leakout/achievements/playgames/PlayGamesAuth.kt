package nl.hexmaster.leakout.achievements.playgames

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Whether the player is signed in to Play Games. */
sealed interface SignInState {
    /** Play Games is not configured in this build (no positive app id): no SDK, no Sign in button. */
    data object Unavailable : SignInState

    /** Not signed in; the settings screen offers Sign in. */
    data object SignedOut : SignInState

    /** A silent check or a sign-in is running. */
    data object SigningIn : SignInState

    /** Signed in. [displayName] is empty when Play Games did not give one. */
    data class SignedIn(val displayName: String) : SignInState
}

/** The Play Games calls sign-in needs, so [PlayGamesAuth] can be tested without the SDK. */
interface SignInGateway {
    /** Whether the player is signed in now, without showing any UI. */
    suspend fun isAuthenticated(): Boolean

    /** Shows the Play Games sign-in; whether the player ended up signed in. */
    suspend fun signIn(): Boolean

    /** The signed-in player's display name. */
    suspend fun displayName(): String?
}

/**
 * The Play Games sign-in state for the whole app. Every Play Games client needs an activity, so
 * the activity starts the silent check when it is created ([checkSilently]) and the settings screen
 * calls [signIn]. A failed or cancelled sign-in is simply [SignInState.SignedOut]: the game keeps
 * working with its local achievements.
 *
 * @param configured whether this build has a Play Games app id; without one the state stays
 *   [SignInState.Unavailable] and no Play Games call is ever made.
 */
class PlayGamesAuth(
    val configured: Boolean,
    private val scope: CoroutineScope,
    private val gatewayFor: (Activity) -> SignInGateway = ::GmsSignInGateway,
    private val log: PlayGamesLog = PlayGamesLog.Android,
) {
    private val mutableState = MutableStateFlow(if (configured) SignInState.SignedOut else SignInState.Unavailable)

    val state: StateFlow<SignInState> = mutableState.asStateFlow()

    /** The silent check at start, for [activity]; shows no UI. */
    fun checkSilently(activity: Activity): Job? = gatewayOrNull(activity)?.let { gateway -> scope.launch { checkSilently(gateway) } }

    /** The Sign in button: shows the Play Games sign-in over [activity] when signed out. */
    fun signIn(activity: Activity): Job? = gatewayOrNull(activity)?.let { gateway -> scope.launch { signIn(gateway) } }

    // Made on the caller's (main) thread; without a configuration the SDK is never touched.
    private fun gatewayOrNull(activity: Activity): SignInGateway? = if (configured) gatewayFor(activity) else null

    /** [checkSilently] with [gateway]. Does nothing while signed in or signing in. */
    suspend fun checkSilently(gateway: SignInGateway) = authenticate(gateway) { gateway.isAuthenticated() }

    /** [signIn] with [gateway]. Only from [SignInState.SignedOut]. */
    suspend fun signIn(gateway: SignInGateway) {
        if (mutableState.value == SignInState.SignedOut) authenticate(gateway) { gateway.signIn() }
    }

    private suspend fun authenticate(gateway: SignInGateway, attempt: suspend () -> Boolean) {
        if (!configured) return
        val current = mutableState.value
        if (current is SignInState.SignedIn || current == SignInState.SigningIn) return
        mutableState.value = SignInState.SigningIn
        val signedIn = log.call("sign-in") { attempt() } == true
        mutableState.value = if (signedIn) {
            SignInState.SignedIn(log.call("display name") { gateway.displayName() }.orEmpty())
        } else {
            SignInState.SignedOut
        }
    }
}
