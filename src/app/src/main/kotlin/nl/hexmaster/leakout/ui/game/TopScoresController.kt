package nl.hexmaster.leakout.ui.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.scores.online.ScoreServerConfig
import nl.hexmaster.leakout.scores.online.TopScores
import nl.hexmaster.leakout.scores.online.TopScoresState

/**
 * What the game screen needs for the top 10 (spec `level-top-scores`): the [scores] to load, the
 * Play Games [signIn] state and the manual sign-in ([onSignIn]). A game screen without one shows no
 * Top 10 button anywhere.
 */
class TopScoresAccess(
    val scores: TopScores,
    val signIn: StateFlow<SignInState>,
    val onSignIn: () -> Unit,
) {
    companion object {
        /**
         * The access for a level from the level library, or null when the build has no score server.
         * A test play from the level designer never asks for one.
         */
        fun forLibraryLevel(
            config: ScoreServerConfig,
            scores: TopScores,
            signIn: StateFlow<SignInState>,
            onSignIn: () -> Unit,
        ): TopScoresAccess? = if (config.enabled) TopScoresAccess(scores, signIn, onSignIn) else null
    }
}

/**
 * The state of one opened top 10 dialog for [level]: it loads when [start]ed and again on [retry],
 * and loads once more when the player becomes signed in while the dialog asks for it. Everything
 * runs in [scope], so closing the dialog (leaving its composition) cancels a load.
 */
class TopScoresController(
    private val level: Int,
    private val access: TopScoresAccess,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<TopScoresState>(TopScoresState.Loading)

    val state: StateFlow<TopScoresState> = mutableState.asStateFlow()

    private var load: Job? = null

    /** Loads the top 10 and watches the sign-in; once per opening. */
    fun start() {
        retry()
        scope.launch {
            access.signIn.collect { signIn ->
                if (signIn is SignInState.SignedIn && mutableState.value is TopScoresState.SignedOut) retry()
            }
        }
    }

    /** Loads the top 10 again, from the loading state. */
    fun retry() {
        load?.cancel()
        mutableState.value = TopScoresState.Loading
        load = scope.launch { mutableState.value = access.scores.load(level) }
    }

    /** The sign-in action of the signed-out state; the top 10 loads once it succeeds. */
    fun signIn() = access.onSignIn()
}
