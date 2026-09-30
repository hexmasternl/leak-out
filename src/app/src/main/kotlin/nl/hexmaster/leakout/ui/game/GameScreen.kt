package nl.hexmaster.leakout.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlin.math.roundToInt
import kotlin.random.Random
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/**
 * The game screen: the HUD across the top, the stack on the left and the board below the HUD.
 * [session] owns the running level; tests pass their own to read and set its values.
 */
@Composable
fun GameScreen(
    level: Level,
    onExit: () -> Unit,
    random: Random = Random.Default,
    session: GameSession = remember(level, random) { GameSession(level, random) },
) {
    val dragState = remember(session) { SectionDragState() }

    GameClock(session)
    LaunchedEffect(session, session.paused) {
        if (session.paused) dragState.cancel()
    }
    BackHandler(enabled = !session.pauseMenuOpen) { session.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { session.pause() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LeakOutColors.SurfaceDeep)
            .testTag(GameScreenTags.SCREEN),
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            GameHud(
                levelId = level.id,
                flowSpeed = session.flowSpeed,
                stars = session.stars,
                score = session.score,
                onPause = session::pause,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                StackPanel(
                    stack = session.stack,
                    dragState = dragState,
                    enabled = !session.paused,
                    modifier = Modifier.width(
                        StackLayout.metrics(360.dp - HudLayout.HEIGHT, level.stack.visibleSize).columnWidth + 16.dp,
                    ),
                )
                // Keyed on the attempt: a restart starts the field at its starting view, a pause keeps it.
                key(session.attempt) {
                    val fieldState = rememberPlayingFieldState(level.grid, level.start.position)
                    val target = remember(session, fieldState) { FieldDropTarget(session, fieldState) }
                    dragState.dropTarget = target
                    val highlighted by remember(dragState, target) {
                        derivedStateOf { dragState.dropPoint?.let(target::hoverCell) }
                    }
                    PlayingField(
                        board = session.board,
                        state = fieldState,
                        highlighted = highlighted,
                        enabled = !session.paused && !dragState.isBusy,
                        modifier = Modifier.padding(start = 12.dp).weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
        DragOverlay(dragState)
    }

    if (session.pauseMenuOpen) {
        PauseDialog(
            onResume = session::resume,
            onRestart = session::restart,
            onExit = onExit,
        )
    }
}

/**
 * Runs the game clock while the session is not paused. The loop ends on pause and starts again on
 * resume with a first delta of 0, so no time from the pause leaks in. It is an infinite animation,
 * so Compose tests only run it when they drive the clock themselves (`autoAdvance = false`).
 */
@Composable
private fun GameClock(session: GameSession) {
    LaunchedEffect(session, session.paused) {
        if (session.paused) return@LaunchedEffect
        var last = -1L
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                if (last < 0) last = now
                val dtMs = (now - last) / 1_000_000
                last += dtMs * 1_000_000
                session.tick(dtMs)
            }
        }
    }
}

/**
 * Drops on the playing field. The hover highlight and the drop both map the screen point to a
 * cell and ask the session's placement rule, so they cannot disagree.
 */
private class FieldDropTarget(private val session: GameSession, private val field: PlayingFieldState) : SectionDropTarget {
    fun hoverCell(position: Offset): Position? = field.cellAt(position)?.takeIf(session::canPlace)

    override fun canPlace(section: PipeSection, position: Offset): Boolean = hoverCell(position) != null

    override fun tryPlace(section: PipeSection, position: Offset): Boolean {
        val cell = field.cellAt(position) ?: return false
        return session.place(section, cell)
    }
}

@Composable
private fun DragOverlay(dragState: SectionDragState) {
    val section = dragState.draggedSection ?: return
    val density = LocalDensity.current
    val offsetPx = with(density) { DraggedCopyYOffset.toPx() }
    val copySizePx = with(density) { DraggedCopySize.toPx() }
    val target = if (dragState.isAnimatingBack) dragState.slotCenter else dragState.fingerPosition + Offset(0f, -offsetPx)
    val animated by animateOffsetAsState(target, tween(150), label = "dragCopyReturn")
    LaunchedEffect(dragState.isAnimatingBack, animated, target) {
        if (dragState.isAnimatingBack && (animated - target).getDistance() < 1f) {
            dragState.finishReturn()
        }
    }
    Box(
        Modifier
            .testTag(GameScreenTags.DRAGGED)
            .offset { IntOffset((animated.x - copySizePx / 2).roundToInt(), (animated.y - copySizePx / 2).roundToInt()) }
            .size(DraggedCopySize),
    ) {
        PipeSectionGlyph(section, Modifier.fillMaxSize())
    }
}

private val DraggedCopySize = 56.dp

