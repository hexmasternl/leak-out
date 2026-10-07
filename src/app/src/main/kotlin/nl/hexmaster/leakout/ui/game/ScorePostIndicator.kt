package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import nl.hexmaster.leakout.scores.online.ScorePostStatus
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/** One frame of the status dot: its colour and opacity, or [done] once it is gone. */
data class DotFrame(val color: Color, val alpha: Float, val done: Boolean = false)

/** The blinks of the status dot (spec `online-score-submission`), on the wall clock. */
object ScorePostDot {
    const val PROGRESS_HALF_MS = 400L
    const val SUCCESS_ON_MS = 300L
    const val SUCCESS_OFF_MS = 300L
    const val FAILURE_ON_MS = 200L
    const val FAILURE_OFF_MS = 200L
    const val FAILURE_BLINKS = 3
    const val DIMMED = 0.2f

    // design-system.md: brass 300 (the yellow accent), success 500 and leak 500.
    val Progress = LeakOutColors.Brass300
    val Success = LeakOutColors.Success500
    val Failure = LeakOutColors.Leak500

    /** What the dot shows [elapsedMs] after [status] began. */
    fun frame(status: ScorePostStatus, elapsedMs: Long): DotFrame = when (status) {
        // Blinks until the sending ends.
        ScorePostStatus.InProgress -> DotFrame(Progress, if ((elapsedMs / PROGRESS_HALF_MS) % 2 == 0L) 1f else DIMMED)
        ScorePostStatus.Succeeded -> when {
            elapsedMs < SUCCESS_ON_MS -> DotFrame(Success, 1f)
            elapsedMs < SUCCESS_ON_MS + SUCCESS_OFF_MS -> DotFrame(Success, 0f)
            else -> DotFrame(Success, 0f, done = true)
        }
        ScorePostStatus.Failed -> {
            val cycle = FAILURE_ON_MS + FAILURE_OFF_MS
            when {
                elapsedMs >= cycle * FAILURE_BLINKS -> DotFrame(Failure, 0f, done = true)
                elapsedMs % cycle < FAILURE_ON_MS -> DotFrame(Failure, 1f)
                else -> DotFrame(Failure, 0f)
            }
        }
    }
}

object ScorePostIndicatorTags {
    const val DOT = "score_post_dot"
}

/**
 * The small dot that shows the sending of a won level's score: yellow blinking while it runs, one
 * green blink when the score reached the server, three red blinks when it did not, then nothing.
 * Decorative: no semantics, no touch. A sending that had already ended when the dot is first
 * shown (coming back from the background) plays nothing.
 */
@Composable
fun ScorePostIndicator(status: StateFlow<ScorePostStatus>, modifier: Modifier = Modifier) {
    val current by status.collectAsState()
    val endedBeforeShown = remember(status) { status.value != ScorePostStatus.InProgress }
    if (endedBeforeShown) return
    var elapsedMs by remember(current) { mutableLongStateOf(0L) }
    LaunchedEffect(current) {
        // The yellow blink has no end of its own; marked as such, it does not keep the UI busy (tests).
        val frame: suspend (onFrame: (Long) -> Unit) -> Unit = if (current == ScorePostStatus.InProgress) {
            { onFrame -> withInfiniteAnimationFrameMillis(onFrame) }
        } else {
            { onFrame -> withFrameMillis(onFrame) }
        }
        var start = -1L
        while (!ScorePostDot.frame(current, elapsedMs).done) {
            frame { now ->
                if (start < 0) start = now
                elapsedMs = now - start
            }
        }
    }
    val frame = ScorePostDot.frame(current, elapsedMs)
    if (frame.done) return
    Box(
        modifier
            .testTag(ScorePostIndicatorTags.DOT)
            .clearAndSetSemantics {}
            .size(10.dp)
            .graphicsLayer { alpha = frame.alpha }
            .background(frame.color, CircleShape),
    )
}
