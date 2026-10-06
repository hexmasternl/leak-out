package nl.hexmaster.leakout.ui.game

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import nl.hexmaster.leakout.audio.GameSound
import nl.hexmaster.leakout.audio.SoundEffects
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.Award
import nl.hexmaster.leakout.core.engine.GapDistance
import nl.hexmaster.leakout.core.engine.GameState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.haptics.HapticEffect
import nl.hexmaster.leakout.haptics.Haptics

/**
 * The sound of a placement that did [result]. A refusal only sounds while [state] takes sections:
 * after a win or a leak, while paused, in fast forward or during a replacement the player has
 * nothing to place, so a tap on the field there is not a failed placement.
 */
fun placementSound(result: PlaceResult, state: GameState): GameSound? = when (result) {
    PlaceResult.Placed -> GameSound.SectionPlaced
    PlaceResult.Replaced -> GameSound.SectionDestroyed
    PlaceResult.Rejected -> GameSound.PlacementFailed.takeIf {
        state.outcome is Outcome.Playing && !state.paused && !state.fastForward && state.replacement == null
    }
}

/**
 * Places the next section on [at] like [GameSession.place], plays its sound and returns whether it
 * was placed. A replacement without a penalty lands at once, so its placement sound follows the destroy.
 * Only the destroy of a replacement vibrates through [haptics]; a placement or a refusal does not.
 */
fun GameSession.placeWithSound(at: Position, soundEffects: SoundEffects?, haptics: Haptics? = null): Boolean {
    val before = state
    val result = place(at)
    placementSound(result, before)?.let { soundEffects?.play(it) }
    if (result == PlaceResult.Replaced) haptics?.play(HapticEffect.SectionReplaced)
    if (result == PlaceResult.Replaced && state.replacement == null) soundEffects?.play(GameSound.SectionPlaced)
    return result != PlaceResult.Rejected
}

/**
 * Whether the replacement running in [before] landed on the way to [after]: same attempt, the
 * replacement is over and its section is on the tile. One that ended with the attempt did not land.
 */
fun landed(before: GameState, after: GameState): Boolean {
    val replacement = before.replacement ?: return false
    return after.attempt == before.attempt &&
        after.replacement == null &&
        (after.board[replacement.at] as? Cell.Pipe)?.section == replacement.incoming
}

/** Plays the placement sound each time a replacement section lands in [session]. */
@Composable
internal fun ReplacementLandingSound(session: GameSession, soundEffects: SoundEffects?) {
    LaunchedEffect(session, soundEffects) {
        snapshotFlow { session.lastLanding }.drop(1).filterNotNull().collect { soundEffects?.play(GameSound.SectionPlaced) }
    }
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

/** The newest award of an attempt. A points popup starts when it changes to another award. */
data class NewestAward(val attempt: Int, val award: Award?) {
    companion object {
        fun of(state: GameState): NewestAward = NewestAward(state.attempt, state.awards.lastOrNull())
    }
}

/**
 * Whether a points popup started on the way from [before] to [after]. Awards are only appended, so a
 * new newest award means at least one new popup; several in one frame share one drop. A restart
 * (another attempt) and awards ageing out start none.
 */
fun popupStarted(before: NewestAward, after: NewestAward): Boolean =
    after.attempt == before.attempt && after.award != null && after.award != before.award

/**
 * Plays the points awarded sound when a points popup starts in [session], at most once per
 * [DropThrottle.DROP_MIN_INTERVAL_MS] of [nowMs]. It follows the game state rather than the popup
 * drawing, so popups off-screen in a zoomed field sound as well.
 */
@Composable
internal fun PointsAwardedSound(session: GameSession, soundEffects: SoundEffects?, nowMs: () -> Long = SystemClock::uptimeMillis) {
    val currentNowMs by rememberUpdatedState(nowMs)
    LaunchedEffect(session, soundEffects) {
        val throttle = DropThrottle { currentNowMs() }
        var before: NewestAward? = null
        snapshotFlow { NewestAward.of(session.state) }.collect { after ->
            val started = before?.let { popupStarted(it, after) } == true
            before = after
            if (started && throttle.tryDrop()) soundEffects?.play(GameSound.PointsAwarded)
        }
    }
}

/**
 * The heartbeat's volume factor for [state] (spec `game-play-gap-warning`): from the gap distance,
 * and null while paused. [GapDistance] is null after a win or a loss and before the flow starts.
 */
fun heartbeatFactor(state: GameState): Float? =
    if (state.paused) null else GapWarning.heartbeatFactor(GapDistance.of(state))

/**
 * Loops the heartbeat through [soundEffects] while the liquid closes in on a gap, following the
 * distance every frame. It stops when the app leaves the foreground and when the screen goes away.
 */
@Composable
internal fun GapHeartbeat(session: GameSession, soundEffects: SoundEffects?) {
    if (soundEffects == null) return
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val started = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    LaunchedEffect(session, soundEffects, started) {
        if (!started) {
            soundEffects.heartbeat(null)
            return@LaunchedEffect
        }
        snapshotFlow { heartbeatFactor(session.state) }.collect(soundEffects::heartbeat)
    }
    DisposableEffect(session, soundEffects) { onDispose { soundEffects.heartbeat(null) } }
}
