package nl.hexmaster.leakout.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.Alignment
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
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.history.LevelHistoryRepository
import nl.hexmaster.leakout.history.RecordResult
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/**
 * The game screen: the HUD across the top, the stack on the left and the board below the HUD, with
 * the fast forward button in the bottom right corner once the route is complete. A leak shows the
 * spurt for [LeakTiming.EFFECT_MS] and then the level lost dialog. A win flashes the finish for
 * [WinTiming.FINISH_EFFECT_MS] and then shows the level complete dialog; its Next level button calls
 * [onNextLevel] and stays disabled when that is null (the last level). The win is recorded in
 * [history] as soon as it happens, and the dialog shows when it set a new best; a leak is never recorded.
 *
 * Every view shows only what it reads from the [GameSession]'s state, and every action is a
 * command to the engine. [session] owns the running level; tests pass their own to drive it.
 */
@Composable
fun GameScreen(
    level: Level,
    onExit: () -> Unit,
    onNextLevel: (() -> Unit)? = null,
    history: LevelHistoryRepository? = null,
    random: Random = Random.Default,
    session: GameSession = remember(level, random) { GameSession(level, random) },
) {
    val dragState = remember(session) { SectionDragState() }

    // Each value is derived, so a clock tick recomposes only what changed, not the whole screen.
    val attempt = session.select { state.attempt }
    val currentLevel = session.select { state.level }
    val paused = session.select { state.paused }
    val outcome = session.select { state.outcome }
    val isStopped = session.select { state.isStopped }
    val fastForward = session.select { state.fastForward }
    val showPauseDialog = session.select { showPauseDialog }
    val showLostDialog = session.select { showLostDialog }
    val showWinDialog = session.select { showWinDialog }

    GameClock(session, paused)
    // Keyed on the attempt: every won attempt is offered to the history, which keeps the best.
    val won = outcome is Outcome.Won
    LaunchedEffect(session, history, attempt, won) {
        if (won && history != null) session.recordWin(history)
    }
    LaunchedEffect(session, isStopped) {
        if (isStopped) dragState.cancel()
    }
    // A new attempt (restart, another level) ends any drag, so the section goes back to its slot.
    LaunchedEffect(session, attempt) { dragState.cancel() }
    BackHandler(enabled = outcome is Outcome.Playing && !paused) { session.send(GameCommand.Pause) }
    // During the leak effect and the finish flash Back does nothing. Once shown, a dialog handles
    // Back itself: the lost dialog as Exit, the level complete dialog as Exit once its tally is done.
    BackHandler(enabled = outcome !is Outcome.Playing && !showLostDialog && !showWinDialog) {}
    // In the background the end effects are skipped, so the finished dialog shows on return.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        when (session.state.outcome) {
            is Outcome.Won -> session.skipWinAnimation()
            is Outcome.Lost -> session.skipLeakEffect()
            Outcome.Playing -> session.send(GameCommand.Pause)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LeakOutColors.SurfaceDeep)
            .testTag(GameScreenTags.SCREEN),
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            GameHud(
                levelId = currentLevel.id,
                flowSpeed = session.select { state.flowSpeed },
                stars = session.select { state.stars },
                score = session.select { state.score },
                onPause = { session.send(GameCommand.Pause) },
                pauseEnabled = outcome is Outcome.Playing,
                modifier = Modifier.fillMaxWidth(),
            )
            // Keyed on the attempt: a new attempt starts the stack and the field fresh, a pause keeps them.
            key(attempt) {
                Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                    StackPanel(
                        stack = session.select { state.stack },
                        dragState = dragState,
                        enabled = !isStopped && !fastForward,
                        modifier = Modifier.width(
                            StackLayout.metrics(360.dp - HudLayout.HEIGHT, currentLevel.stack.visibleSize).columnWidth + 16.dp,
                        ),
                    )
                    val fieldState = rememberPlayingFieldState(currentLevel.grid, currentLevel.start.position)
                    val target = remember(session, fieldState) { FieldDropTarget(session, fieldState) }
                    dragState.dropTarget = target
                    val highlighted by remember(dragState, target) {
                        derivedStateOf { dragState.dropPoint?.let(target::hoverCell) }
                    }
                    val lost = outcome as? Outcome.Lost
                    // Pan and zoom stay on after a win or a loss, so the player can look at the field.
                    PlayingField(
                        board = session.select { state.board },
                        state = fieldState,
                        highlighted = highlighted,
                        enabled = !paused && !dragState.isBusy,
                        modifier = Modifier.padding(start = 12.dp).weight(1f).fillMaxHeight(),
                        awards = session.select { state.awards },
                        nowMs = { session.state.clockMs },
                        leakAt = lost?.at,
                        leakSide = lost?.side,
                        lossAgeMs = { session.lossAgeMs },
                        head = session.select { (state.flow as? FlowState.Flowing)?.position },
                        filled = session.select { state.flow.used },
                        winAgeMs = if (outcome is Outcome.Won) ({ session.winAgeMs }) else null,
                    )
                }
            }
        }
        // Over the field's bottom right corner, which is also the screen's: the field reaches both edges.
        AnimatedVisibility(
            visible = session.select { showFastForward },
            enter = scaleIn(tween(200)) + fadeIn(tween(200)),
            exit = scaleOut(tween(200)) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            FastForwardButton(
                active = fastForward,
                onClick = {
                    session.toggleFastForward()
                    // A second finger can tap the button during a drag; the section goes back to its slot.
                    if (session.state.fastForward) dragState.cancel()
                },
            )
        }
        DragOverlay(dragState)
    }

    if (showPauseDialog) {
        PauseDialog(
            onResume = { session.send(GameCommand.Resume) },
            onRestart = { session.send(GameCommand.Restart) },
            onExit = onExit,
        )
    }
    if (showLostDialog) {
        LevelLostDialog(
            score = session.select { state.score },
            onRestart = { session.send(GameCommand.Restart) },
            onExit = onExit,
        )
    }
    if (showWinDialog) {
        val score = session.select { state.finalScore }
        val frame = session.select { winFrame }
        if (score != null && frame != null) {
            LevelCompleteDialog(
                score = score,
                frame = frame,
                hasNextLevel = onNextLevel != null,
                onNextLevel = { onNextLevel?.invoke() },
                onRestart = { session.send(GameCommand.Restart) },
                onExit = onExit,
                newBest = session.select { recordResult is RecordResult.NewHighscore },
            )
        }
    }
}

/**
 * Reads one value from the session, recomposing the caller only when that value changes. The game
 * state changes every frame (the clock), but most views only care about a part that rarely does.
 */
@Composable
private fun <T> GameSession.select(read: GameSession.() -> T): T {
    val value by remember(this) { derivedStateOf { read() } }
    return value
}

/**
 * Sends a tick to the engine every frame while the game is not [paused]. The loop ends on pause and
 * starts again on resume with a first delta of 0, so no time from the pause leaks in. It is an
 * infinite animation, so Compose tests only run it when they drive the clock themselves
 * (`autoAdvance = false`).
 */
@Composable
private fun GameClock(session: GameSession, paused: Boolean) {
    LaunchedEffect(session, paused) {
        if (paused) return@LaunchedEffect
        var last = -1L
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                if (last < 0) last = now
                val dtMs = (now - last) / 1_000_000
                last += dtMs * 1_000_000
                session.send(GameCommand.Tick(dtMs))
            }
        }
    }
}

/**
 * Drops on the playing field. The hover highlight and the drop both map the screen point to a
 * cell and ask the engine, so they cannot disagree. The engine places the stack's next section,
 * which is always the one being dragged.
 */
private class FieldDropTarget(private val session: GameSession, private val field: PlayingFieldState) : SectionDropTarget {
    fun hoverCell(position: Offset): Position? = field.cellAt(position)?.takeIf(session::canPlace)

    override fun canPlace(section: PipeSection, position: Offset): Boolean = hoverCell(position) != null

    override fun tryPlace(section: PipeSection, position: Offset): Boolean {
        val cell = field.cellAt(position) ?: return false
        return session.send(GameCommand.Place(cell))
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
