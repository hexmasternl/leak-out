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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.achievements.AchievementTracker
import nl.hexmaster.leakout.audio.GameSound
import nl.hexmaster.leakout.audio.SoundEffects
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.engine.GapDistance
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.LossReason
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.history.LevelHistoryRepository
import nl.hexmaster.leakout.history.RecordResult
import nl.hexmaster.leakout.settings.SettingsRepository
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.components.KeepScreenOn
import nl.hexmaster.leakout.ui.game.onboarding.DragHint
import nl.hexmaster.leakout.ui.game.onboarding.LocalTipTargets
import nl.hexmaster.leakout.ui.game.onboarding.TipOverlay
import nl.hexmaster.leakout.ui.game.onboarding.TipOverlayTags
import nl.hexmaster.leakout.ui.game.onboarding.TipScheduler
import nl.hexmaster.leakout.ui.game.onboarding.TipTargets
import nl.hexmaster.leakout.ui.game.onboarding.TipText
import nl.hexmaster.leakout.ui.game.onboarding.tipContent
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.pipes.rememberPipeArtLibrary
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/**
 * The game screen: the HUD across the top, the stack on the left and the board below the HUD, with
 * the fast forward button in the bottom right corner once the route is complete. A leak shows the
 * spurt for [LeakTiming.EFFECT_MS] and then the level lost dialog. A win flashes the finish for
 * [WinTiming.FINISH_EFFECT_MS] and then shows the level complete dialog; its Next level button calls
 * [onNextLevel] and stays disabled when that is null (the last level). The win is recorded in
 * [history] as soon as it happens, and the dialog shows when it set a new best; a leak is never recorded.
 * Every ended attempt goes to [achievements] once (a win after it is recorded), and a restart or exit
 * of an unfinished attempt hands it its placements; without it no achievements are tracked (test play).
 *
 * Every view shows only what it reads from the [GameSession]'s state, and every action is a
 * command to the engine. [session] owns the running level; tests pass their own to drive it.
 * Placements and the two dialogs play their sound through [soundEffects]; without it the game is silent.
 */
@Composable
fun GameScreen(
    level: Level,
    onExit: () -> Unit,
    onNextLevel: (() -> Unit)? = null,
    history: LevelHistoryRepository? = null,
    random: Random = Random.Default,
    session: GameSession = remember(level, random) { GameSession(level, random) },
    soundEffects: SoundEffects? = null,
    tips: SettingsRepository? = null,
    achievements: AchievementTracker? = null,
) {
    // The display stays on for the whole game screen, its dialogs included.
    KeepScreenOn()
    val dragState = remember(session) { SectionDragState() }
    // Parse every pipe asset once up front, so no frame mid-game pays for a section's first draw.
    val pipes = rememberPipeArtLibrary().let { library -> remember(library) { library.apply { preloadAll() } } }

    // Each value is derived, so a clock tick recomposes only what changed, not the whole screen.
    val attempt = session.select { state.attempt }
    val currentLevel = session.select { state.level }
    val paused = session.select { state.paused }
    val outcome = session.select { state.outcome }
    val isStopped = session.select { state.isStopped }
    val fastForward = session.select { state.fastForward }
    val replacing = session.select { isReplacing }
    val showPauseDialog = session.select { showPauseDialog }
    val showLostDialog = session.select { showLostDialog }
    val showWinDialog = session.select { showWinDialog }

    // A new attempt starts the field fresh, a pause keeps it. The tips point at its cells.
    val fieldState = remember(attempt, currentLevel) { PlayingFieldState(currentLevel.grid, currentLevel.start.position) }
    val targets = remember(fieldState) { TipTargets(fieldState) }
    val activeTip = session.select { activeTip }
    // Until the level-start tips have been looked at, the countdown must not move.
    val waitingForTips = tips != null && outcome is Outcome.Playing && session.select { tipsCheckedAttempt } != attempt

    GameClock(session, paused || waitingForTips)
    ReplacementLandingSound(session, soundEffects)
    GapHeartbeat(session, soundEffects)
    OncePerAttemptSound(session, showWinDialog, attempt, GameSound.LevelComplete, soundEffects)
    OncePerAttemptSound(session, showLostDialog, attempt, GameSound.LevelFailed, soundEffects)
    // Keyed on the attempt: every won attempt is offered to the history, which keeps the best, and
    // then every ended attempt to the achievements; the session makes both happen once per attempt.
    val won = outcome is Outcome.Won
    val ended = outcome !is Outcome.Playing
    LaunchedEffect(session, history, achievements, attempt, ended) {
        if (won && history != null) session.recordWin(history)
        if (ended && achievements != null) session.recordAchievements(achievements)
    }
    // A replacement takes no input: a drag a second finger still holds goes back to its slot.
    LaunchedEffect(session, isStopped, replacing) {
        if (isStopped || replacing) dragState.cancel()
    }
    // A new attempt (restart, another level) ends any drag, so the section goes back to its slot.
    LaunchedEffect(session, attempt) { dragState.cancel() }
    BackHandler(enabled = outcome is Outcome.Playing && !paused) { session.pauseByUser() }
    // During the leak effect and the finish flash Back does nothing. Once shown, a dialog handles
    // Back itself: the lost dialog as Exit, the level complete dialog as Exit once its tally is done.
    BackHandler(enabled = outcome !is Outcome.Playing && !showLostDialog && !showWinDialog) {}
    // In the background the end effects are skipped, so the finished dialog shows on return.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        when (session.state.outcome) {
            is Outcome.Won -> session.skipWinAnimation()
            is Outcome.Lost -> session.skipLeakEffect()
            Outcome.Playing -> session.pauseByUser()
        }
    }

    CompositionLocalProvider(LocalTipTargets provides targets) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LeakOutColors.SurfaceDeep)
            .testTag(GameScreenTags.SCREEN),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                // Under a tip only the tip is read out; the game is dimmed and frozen.
                .semantics { if (activeTip != null) hideFromAccessibility() },
        ) {
            GameHud(
                levelId = currentLevel.id,
                flowSpeed = session.select { state.flowSpeed },
                countdownRemainingMs = session.select { (state.flow as? FlowState.Waiting)?.remainingMs },
                initialDelayMs = currentLevel.flow.initialDelayMs,
                stars = currentLevel.stars,
                score = session.select { state.score },
                attempt = attempt,
                onPause = { session.pauseByUser() },
                pauseEnabled = outcome is Outcome.Playing,
                modifier = Modifier.fillMaxWidth(),
                gapDistance = { GapDistance.of(session.state) },
                clockMs = { session.state.clockMs },
            )
            // Keyed on the attempt: a new attempt starts the stack and the field fresh, a pause keeps them.
            key(attempt) {
                Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                    StackPanel(
                        stack = session.select { state.stack },
                        visibleSize = currentLevel.stack.visibleSize,
                        dragState = dragState,
                        enabled = !isStopped && !fastForward && !replacing,
                        modifier = Modifier.width(
                            StackLayout.metrics(360.dp - HudLayout.HEIGHT, currentLevel.stack.visibleSize).columnWidth + 16.dp,
                        ),
                    )
                    val target = remember(session, fieldState, soundEffects) { FieldDropTarget(session, fieldState, soundEffects) }
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
                        missed = session.select { missedMandatory },
                        lossAgeMs = { session.lossAgeMs },
                        head = session.select { (state.flow as? FlowState.Flowing)?.position },
                        filled = session.select { state.flow.used },
                        flow = { session.state.flow },
                        winAgeMs = if (outcome is Outcome.Won) ({ session.winAgeMs }) else null,
                        replacement = session.select { state.replacement },
                        landing = session.select { lastLanding?.takeIf { it.attempt == state.attempt } },
                        // A tap places the next section like a drop; the engine refuses cells that do not accept it.
                        onTap = { cell -> session.placeWithSound(cell, soundEffects) },
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
                enabled = session.select { fastForwardEnabled },
                onClick = {
                    session.toggleFastForward()
                    // A second finger can tap the button during a drag; the section goes back to its slot.
                    if (session.state.fastForward) dragState.cancel()
                },
            )
        }
        DragOverlay(dragState)
        if (tips != null) GameTips(session, tips, targets, fieldState, showPauseDialog)
    }
    }

    if (showPauseDialog) {
        PauseDialog(
            onResume = { session.resumeByUser() },
            onRestart = { session.restart(achievements) },
            onExit = {
                session.abandonAttempt(achievements)
                onExit()
            },
        )
    }
    if (showLostDialog) {
        LevelLostDialog(
            level = currentLevel.id,
            score = session.select { state.score },
            onRestart = { session.restart(achievements) },
            onExit = onExit,
            reason = session.select { (state.outcome as? Outcome.Lost)?.reason ?: LossReason.LEAK },
            unlocked = session.select { unlockedThisAttempt },
        )
    }
    if (showWinDialog) {
        val score = session.select { state.finalScore }
        val frame = session.select { winFrame }
        if (score != null && frame != null) {
            LevelCompleteDialog(
                level = currentLevel.id,
                score = score,
                frame = frame,
                hasNextLevel = onNextLevel != null,
                onNextLevel = { onNextLevel?.invoke() },
                onRestart = { session.restart(achievements) },
                onExit = onExit,
                newBest = session.select { recordResult is RecordResult.NewHighscore },
                unlocked = session.select { unlockedThisAttempt },
            )
        }
    }
}

/**
 * The one-time tips (spec `game-play-onboarding`): looks at which are due at level start (once the
 * stack has been laid out and the seen tips are read), after every placement and when the visible
 * stack slots change, shows them over the game with [TipOverlay] and stores each one as seen in
 * [tips] when it is closed. Back acts as Skip tour or Got it, except while the pause dialog is up.
 */
@Composable
private fun GameTips(
    session: GameSession,
    tips: SettingsRepository,
    targets: TipTargets,
    fieldState: PlayingFieldState,
    pauseDialogShown: Boolean,
) {
    val seen by remember(tips) { tips.seenTips }.collectAsState(initial = null)
    val attempt = session.select { state.attempt }
    val stack = session.select { state.stack }
    val outcome = session.select { state.outcome }
    val paused = session.select { state.paused }
    val activeTip = session.select { activeTip }
    val step = session.select { tourStep }
    val checked = session.select { tipsCheckedAttempt }
    val visibleSlots = targets.fullyVisibleSlots
    val reducedMotion = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    val revealMarginPx = with(LocalDensity.current) { RevealMargin.toPx() }

    val mandatory = session.select { state.level.mandatory.map { it.position } }

    fun due(atLevelStart: Boolean): List<TipId> =
        TipScheduler.dueTips(stack, outcome, seen.orEmpty(), atLevelStart, visibleSlots.orEmpty(), mandatory)

    LaunchedEffect(session, attempt, seen != null, visibleSlots != null, checked) {
        if (checked != attempt && seen != null && visibleSlots != null) session.offerTips(due(atLevelStart = true), atLevelStart = true)
    }
    // A placement is the only thing that changes the stack; a resize can change the visible slots.
    LaunchedEffect(session, stack, visibleSlots, seen, activeTip, paused, checked) {
        if (checked == attempt && activeTip == null && !paused && seen != null && visibleSlots != null) session.offerTips(due(atLevelStart = false))
    }

    // A tap within a double tap of closing a tip was meant for the tip, not for the game under it.
    var guardingTaps by remember { mutableStateOf(false) }
    val doubleTapMs = LocalViewConfiguration.current.doubleTapTimeoutMillis
    LaunchedEffect(guardingTaps) {
        if (guardingTaps) {
            delay(doubleTapMs)
            guardingTaps = false
        }
    }

    fun close() {
        val tip = session.closeTip() ?: return
        guardingTaps = true
        scope.launch { tips.markTipSeen(tip) }
        // After the tour and the mandatory tip, which may pan, the field goes back to the view the level starts with.
        if (tip == TipId.Intro || tip == TipId.Mandatory) scope.launch { fieldState.animateTo(fieldState.initialView(), reducedMotion) }
    }
    BackHandler(enabled = activeTip != null && !pauseDialogShown) { close() }

    if (activeTip == null && guardingTaps) {
        Box(
            Modifier
                .fillMaxSize()
                .testTag(TipOverlayTags.TAP_GUARD)
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
        )
    }
    val tip = activeTip ?: return
    val level = session.state.level
    val hintCell = remember(attempt, stack) { TipScheduler.hintCell(level, session.state.board) }
    // Field targets outside the view are panned into it first.
    val revealed = when {
        tip == TipId.Mandatory -> mandatory.firstOrNull()
        tip != TipId.Intro -> null
        step == 1 -> level.finish.position
        step == 3 -> hintCell
        else -> null
    }
    LaunchedEffect(tip, step, revealed) {
        val view = fieldState.viewport ?: return@LaunchedEffect
        if (revealed != null) fieldState.animateTo(view.revealCell(revealed, revealMarginPx), reducedMotion)
    }
    val next = stack.size - 1
    val spotlights = when (tip) {
        TipId.Intro -> when (step) {
            0 -> listOfNotNull(targets.cell(level.start.position))
            1 -> listOfNotNull(targets.cell(level.finish.position))
            2 -> listOfNotNull(targets.stackColumn)
            3 -> listOfNotNull(targets.slot(next), hintCell?.let(targets::cell))
            else -> listOfNotNull(targets.fieldBounds())
        }
        TipId.Cross -> listOfNotNull(TipScheduler.tipSlot(stack, PipeSection.X, visibleSlots.orEmpty())?.let(targets::slot))
        TipId.Reservoir -> listOfNotNull(TipScheduler.tipSlot(stack, PipeSection.RES, visibleSlots.orEmpty())?.let(targets::slot))
        // Every mandatory section that is in the field's view; on a large level the first one is panned into it.
        TipId.Mandatory -> {
            val field = targets.fieldBounds()
            mandatory.mapNotNull(targets::cell).filter { field == null || it.overlaps(field) }
        }
    }
    val hint = if (tip == TipId.Intro && step == 3) {
        val from = targets.slot(next)
        val to = hintCell?.let(targets::cell)
        if (from != null && to != null) DragHint(stack.next, from, to) else null
    } else {
        null
    }
    TipOverlay(
        content = tipContent(tip, step, TipText.percent(level.flow.reservoirSpeedFactor)),
        spotlights = spotlights,
        hint = hint,
        onNext = session::nextTourStep,
        onSkip = ::close,
        onDone = ::close,
        reducedMotion = reducedMotion,
    )
}

/** How far a revealed cell is kept from the field edge, so its spotlight and ring are not cut off. */
private val RevealMargin = 16.dp

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
 * which is always the one being dragged. A drop on a tile plays its sound; letting go off the
 * level's tiles is a cancel and stays silent.
 */
private class FieldDropTarget(
    private val session: GameSession,
    private val field: PlayingFieldState,
    private val soundEffects: SoundEffects?,
) : SectionDropTarget {
    fun hoverCell(position: Offset): Position? = field.cellAt(position)?.takeIf(session::canPlace)

    override fun canPlace(section: PipeSection, position: Offset): Boolean = hoverCell(position) != null

    override fun tryPlace(section: PipeSection, position: Offset): Boolean {
        val cell = field.cellAt(position) ?: return false
        return session.placeWithSound(cell, soundEffects)
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
