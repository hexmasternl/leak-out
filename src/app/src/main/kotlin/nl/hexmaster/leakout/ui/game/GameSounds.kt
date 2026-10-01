package nl.hexmaster.leakout.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import nl.hexmaster.leakout.audio.GameSound
import nl.hexmaster.leakout.audio.SoundEffects
import nl.hexmaster.leakout.core.engine.GameState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Position

/**
 * The sound of a placement that did [result]. A refusal only sounds while [state] takes sections:
 * after a win or a leak, while paused or in fast forward the player has nothing to place, so a
 * tap on the field there is not a failed placement.
 */
fun placementSound(result: PlaceResult, state: GameState): GameSound? = when (result) {
    PlaceResult.Placed -> GameSound.SectionPlaced
    PlaceResult.Replaced -> GameSound.SectionDestroyed
    PlaceResult.Rejected -> GameSound.PlacementFailed.takeIf {
        state.outcome is Outcome.Playing && !state.paused && !state.fastForward
    }
}

/** Places the next section on [at] like [GameSession.place], plays its sound and returns whether it was placed. */
fun GameSession.placeWithSound(at: Position, soundEffects: SoundEffects?): Boolean {
    val before = state
    val result = place(at)
    placementSound(result, before)?.let { soundEffects?.play(it) }
    return result != PlaceResult.Rejected
}

/**
 * Plays [sound] once per [attempt] when [show] turns true, such as a dialog appearing. It waits for
 * the app to be in the foreground, so a dialog that appeared in the background sounds on return.
 */
@Composable
internal fun OncePerAttemptSound(session: GameSession, show: Boolean, attempt: Int, sound: GameSound, soundEffects: SoundEffects?) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val started = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    var playedFor by remember(session) { mutableStateOf<Int?>(null) }
    LaunchedEffect(show, attempt, started) {
        if (show && started && playedFor != attempt) {
            playedFor = attempt
            soundEffects?.play(sound)
        }
    }
}
