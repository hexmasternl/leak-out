package nl.hexmaster.leakout.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import nl.hexmaster.leakout.core.engine.GameState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.haptics.HapticEffect
import nl.hexmaster.leakout.haptics.Haptics

/** Whether an [attempt] is lost. The level lost haptic starts when this turns true. */
data class AttemptLoss(val attempt: Int, val lost: Boolean) {
    companion object {
        fun of(state: GameState): AttemptLoss = AttemptLoss(state.attempt, state.outcome is Outcome.Lost)
    }
}

/**
 * Whether the attempt was lost on the way from [before] to [after]: [after] is lost and [before]
 * was not, or was another attempt. A win is not a loss, so it never counts.
 */
fun lostOnTheWay(before: AttemptLoss, after: AttemptLoss): Boolean =
    after.lost && (!before.lost || before.attempt != after.attempt)

/**
 * The game's haptics that follow the state of [session] (spec `game-haptics`): one second at full
 * strength the moment an attempt is lost, once per attempt, without waiting for the dialog. Any
 * vibration stops when the app leaves the foreground and when the game screen goes away.
 */
@Composable
internal fun GameHaptics(session: GameSession, haptics: Haptics?) {
    if (haptics == null) return
    LaunchedEffect(session, haptics) {
        var before: AttemptLoss? = null
        snapshotFlow { AttemptLoss.of(session.state) }.collect { after ->
            val lost = before?.let { lostOnTheWay(it, after) } == true
            before = after
            if (lost) haptics.play(HapticEffect.LevelLost)
        }
    }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val started = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    LaunchedEffect(haptics, started) { if (!started) haptics.stop() }
    DisposableEffect(session, haptics) { onDispose { haptics.stop() } }
}
