package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.scores.online.LeaderboardEntry
import nl.hexmaster.leakout.scores.online.TopScoresState
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.components.displayUppercase
import nl.hexmaster.leakout.ui.components.panelScale
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

object TopScoresTags {
    const val DIALOG = "top10_dialog"
    const val BACKDROP = "top10_backdrop"
    const val LEVEL = "top10_level"
    const val TITLE = "top10_title"
    const val CLOSE = "top10_close"
    const val LIST = "top10_list"
    const val LOADING = "top10_loading"
    const val EMPTY = "top10_empty"
    const val SIGNED_OUT = "top10_signed_out"
    const val SIGN_IN = "top10_sign_in"
    const val ERROR = "top10_error"
    const val RETRY = "top10_retry"
    const val LAYOUT_PREFIX = "top10"
    const val LAYOUT_REGULAR = "top10_layout_regular"
    const val LAYOUT_COMPACT = "top10_layout_compact"

    /** The row of rank [position]. */
    fun row(position: Int) = "top10_row_$position"
}

/** Sizes and timings of the top 10 dialog, from design-system.md 5.6 and 7.2. */
object TopScoresMetrics {
    val RowHeight: Dp = 40.dp
    val RowPadding: Dp = 12.dp
    val RankWidth: Dp = 32.dp
    val HeaderGap: Dp = 12.dp
    val ColumnGap: Dp = 24.dp
    val StateMessageMaxWidth: Dp = 280.dp
    val StateButtonWidth: Dp = 200.dp
    val RowRise: Dp = 8.dp
    const val ROW_STAGGER_MS = 30
    const val ROW_FADE_MS = 160
    const val REGULAR_ROWS = 10
    const val COMPACT_ROWS_PER_COLUMN = 5

    /** The list's space: 10 rows in one column (Regular) or 5 rows in each of two (Compact). */
    fun listHeight(layout: ResultDialogLayout): Dp =
        RowHeight * if (layout == ResultDialogLayout.Regular) REGULAR_ROWS else COMPACT_ROWS_PER_COLUMN
}

/**
 * The global top 10 of [level] (spec `level-top-scores`, design-system.md 5.6) on the opaque backdrop,
 * over the dialog that opened it. It loads when it opens, through [access]; Close or Back calls
 * [onClose], a tap on the backdrop does nothing. Opened [fromPause], the backdrop is already there
 * and does not fade in again.
 */
@Composable
fun TopScoresDialog(level: Int, access: TopScoresAccess, fromPause: Boolean, onClose: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        FullScreenDialogWindow()
        // The scope ends with the dialog, so closing it cancels a load that is still running.
        val scope = rememberCoroutineScope()
        val controller = remember(level, access) { TopScoresController(level, access, scope) }
        LaunchedEffect(controller) { controller.start() }
        val state by controller.state.collectAsState()
        val entry = remember(reducedMotion, fromPause) {
            PauseDialogEntry.select(reducedMotion).let { if (fromPause) it.copy(backdropFadeMs = 0) else it }
        }
        TopScoresDialogContent(
            level = level,
            state = state,
            onClose = onClose,
            onRetry = controller::retry,
            onSignIn = controller::signIn,
            entry = entry,
            reducedMotion = reducedMotion,
        )
    }
}

/** The backdrop and the panel with the entry motion; the dialog window around it is [TopScoresDialog]. */
@Composable
internal fun TopScoresDialogContent(
    level: Int,
    state: TopScoresState,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    entry: PauseDialogEntry,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val backdropAlpha = remember { Animatable(if (entry.backdropFadeMs == 0) 1f else 0f) }
    val panelAlpha = remember { Animatable(if (entry.panelFadeMs == 0) 1f else 0f) }
    val scale = remember { Animatable(entry.initialScale) }
    LaunchedEffect(entry) {
        if (entry.backdropFadeMs > 0) backdropAlpha.animateTo(1f, tween(entry.backdropFadeMs, easing = LinearOutSlowInEasing))
    }
    LaunchedEffect(entry) {
        if (entry.panelFadeMs > 0) panelAlpha.animateTo(1f, tween(entry.panelFadeMs, easing = LinearOutSlowInEasing))
    }
    LaunchedEffect(entry) {
        entry.scaleSpec?.let { scale.animateTo(1f, it) }
    }
    val paneName = stringResource(R.string.top10_pane_title)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .testTag(TopScoresTags.BACKDROP)
                .fillMaxSize()
                .graphicsLayer { alpha = backdropAlpha.value }
                .background(PauseBackdropBrush)
                // Taps on the backdrop are swallowed: they neither close the dialog nor reach what is under it.
                .pointerInput(Unit) { detectTapGestures { } },
        )
        ResultDialogFrame(
            measureRegular = { TopScoresPanelContent(ResultDialogLayout.Regular, level, state, {}, {}, {}, reducedMotion, measuring = true) },
        ) { layout ->
            ResultDialogPanel(
                layout = layout,
                modifier = Modifier
                    .testTag(TopScoresTags.DIALOG)
                    .semantics {
                        paneTitle = paneName
                        panelScale = scale.value
                    }
                    .graphicsLayer {
                        alpha = panelAlpha.value
                        scaleX = scale.value
                        scaleY = scale.value
                    },
            ) {
                Box(Modifier.testTag("${TopScoresTags.LAYOUT_PREFIX}_layout_${layout.name.lowercase()}")) {
                    TopScoresPanelContent(layout, level, state, onClose, onRetry, onSignIn, reducedMotion)
                }
            }
        }
    }
}

/**
 * The header (level, title, Close) above the list's space, which is the same size in every state.
 * When the window is too short even for that, the list scrolls under the fixed header.
 */
@Composable
private fun TopScoresPanelContent(
    layout: ResultDialogLayout,
    level: Int,
    state: TopScoresState,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    reducedMotion: Boolean,
    measuring: Boolean = false,
) {
    // Tags only on the shown content, never on the copy that is only measured.
    val tag: Modifier.(String) -> Modifier = { if (measuring) this else testTag(it) }
    Column(Modifier.padding(ResultDialogMetrics.Padding)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.result_eyebrow, level).displayUppercase(),
                    style = LeakOutTextStyles.DisplayXs,
                    color = LeakOutColors.Liquid300,
                    modifier = Modifier.tag(TopScoresTags.LEVEL),
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painterResource(R.drawable.ic_trophy), contentDescription = null, tint = LeakOutColors.Brass300, modifier = Modifier.size(24.dp))
                    Text(
                        text = stringResource(R.string.top10_title),
                        style = LeakOutTextStyles.DisplayLg,
                        color = LeakOutColors.TextPrimary,
                        modifier = Modifier.tag(TopScoresTags.TITLE).semantics { heading() },
                    )
                }
            }
            CloseButton(onClose, Modifier.tag(TopScoresTags.CLOSE))
        }
        Spacer(Modifier.height(TopScoresMetrics.HeaderGap))
        // A fixed height, which the column shortens to what is left when the window is too short.
        Box(Modifier.fillMaxWidth().height(TopScoresMetrics.listHeight(layout)), contentAlignment = Alignment.Center) {
            when (state) {
                TopScoresState.Loading -> StateMessage(
                    message = stringResource(R.string.top10_loading),
                    indicator = { LoadingRing(reducedMotion) },
                    modifier = Modifier.tag(TopScoresTags.LOADING),
                )
                TopScoresState.Empty -> StateMessage(
                    message = stringResource(R.string.top10_empty),
                    indicator = { StateIcon(R.drawable.ic_trophy, LeakOutColors.TextMuted) },
                    modifier = Modifier.tag(TopScoresTags.EMPTY),
                )
                is TopScoresState.SignedOut -> StateMessage(
                    message = stringResource(R.string.top10_signed_out),
                    indicator = { StateIcon(R.drawable.ic_trophy, LeakOutColors.TextMuted) },
                    action = stringResource(R.string.top10_sign_in).takeIf { state.canSignIn },
                    onAction = onSignIn,
                    actionModifier = Modifier.tag(TopScoresTags.SIGN_IN),
                    modifier = Modifier.tag(TopScoresTags.SIGNED_OUT),
                )
                TopScoresState.Failed -> StateMessage(
                    message = stringResource(R.string.top10_error),
                    indicator = { StateIcon(R.drawable.ic_warning, LeakOutColors.Leak500) },
                    action = stringResource(R.string.top10_retry),
                    onAction = onRetry,
                    actionModifier = Modifier.tag(TopScoresTags.RETRY),
                    modifier = Modifier.tag(TopScoresTags.ERROR),
                )
                is TopScoresState.Loaded -> TopScoresList(layout, state.entries, reducedMotion, animate = !measuring, tag = tag)
            }
        }
    }
}

/** The 48 dp glass icon button of design-system.md 5.1 with the `close` icon. */
@Composable
private fun CloseButton(onClose: () -> Unit, modifier: Modifier) {
    val label = stringResource(R.string.top10_close)
    Box(
        modifier = modifier
            .size(48.dp)
            .background(LeakOutColors.SurfaceOverlay, CircleShape)
            .border(1.dp, LeakOutColors.LineStrong, CircleShape)
            .clickable(role = Role.Button, onClick = onClose)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = LeakOutColors.TextPrimary, modifier = Modifier.size(24.dp))
    }
}

/** One of the loading, empty, signed-out and error states: an indicator, a message and an optional glass button. */
@Composable
private fun StateMessage(
    message: String,
    indicator: @Composable () -> Unit,
    modifier: Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
    actionModifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { indicator() }
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = LeakOutTextStyles.BodyMd,
            color = LeakOutColors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = TopScoresMetrics.StateMessageMaxWidth),
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            FlowButton(
                text = action,
                onClick = onAction,
                variant = FlowButtonVariant.Secondary,
                size = FlowButtonSize.Medium,
                modifier = actionModifier.width(TopScoresMetrics.StateButtonWidth),
            )
        }
    }
}

@Composable
private fun StateIcon(icon: Int, tint: Color) {
    Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(40.dp))
}

/** A 32 dp `liquid.500` ring on a `line.subtle` track; a still quarter arc with reduced motion (7.4). */
@Composable
private fun LoadingRing(reducedMotion: Boolean) {
    val modifier = Modifier.size(32.dp)
    if (reducedMotion) {
        CircularProgressIndicator(
            progress = { 0.25f },
            modifier = modifier,
            color = LeakOutColors.Liquid500,
            strokeWidth = 3.dp,
            trackColor = LeakOutColors.LineSubtle,
        )
    } else {
        CircularProgressIndicator(modifier = modifier, color = LeakOutColors.Liquid500, strokeWidth = 3.dp, trackColor = LeakOutColors.LineSubtle)
    }
}

/**
 * The ranked rows: one column in Regular, ranks 1–5 and 6–10 side by side in Compact, each column a
 * traversal group so a screen reader still reads 1 to 10 in order. Scrolls when the space is short.
 */
@Composable
private fun TopScoresList(
    layout: ResultDialogLayout,
    entries: List<LeaderboardEntry>,
    reducedMotion: Boolean,
    animate: Boolean,
    tag: Modifier.(String) -> Modifier,
) {
    val scroll = rememberScrollState()
    val rows = @Composable { column: List<LeaderboardEntry> ->
        column.forEachIndexed { index, entry ->
            TopScoreRow(
                entry = entry,
                topLine = index > 0 && !entry.isCurrentPlayer && !column[index - 1].isCurrentPlayer,
                appearDelayMs = if (reducedMotion) 0 else (entry.position - 1).coerceAtLeast(0) * TopScoresMetrics.ROW_STAGGER_MS,
                rise = !reducedMotion,
                animate = animate,
                modifier = Modifier.tag(TopScoresTags.row(entry.position)),
            )
        }
    }
    when (layout) {
        ResultDialogLayout.Regular -> Column(Modifier.fillMaxSize().verticalScroll(scroll).tag(TopScoresTags.LIST)) { rows(entries) }
        ResultDialogLayout.Compact -> Row(Modifier.fillMaxSize().verticalScroll(scroll).tag(TopScoresTags.LIST)) {
            Column(Modifier.weight(1f).semantics { isTraversalGroup = true }) {
                rows(entries.take(TopScoresMetrics.COMPACT_ROWS_PER_COLUMN))
            }
            Box(
                Modifier
                    .padding(horizontal = TopScoresMetrics.ColumnGap / 2)
                    .width(1.dp)
                    .height(TopScoresMetrics.RowHeight * TopScoresMetrics.COMPACT_ROWS_PER_COLUMN)
                    .background(LeakOutColors.LineSubtle),
            )
            Column(Modifier.weight(1f).semantics { isTraversalGroup = true }) {
                rows(entries.drop(TopScoresMetrics.COMPACT_ROWS_PER_COLUMN))
            }
        }
    }
}

/**
 * One 40 dp row: rank (1–3 in `brass.300`), name (one line, ellipsized; "YOU" after the player's own)
 * and score, read as one item. The own row has a `liquid.tint` wash and a `liquid.500` edge; other
 * rows are separated by `line.subtle` lines. It fades in after [appearDelayMs], rising 8 dp unless
 * motion is reduced.
 */
@Composable
private fun TopScoreRow(
    entry: LeaderboardEntry,
    topLine: Boolean,
    appearDelayMs: Int,
    rise: Boolean,
    animate: Boolean,
    modifier: Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val score = HudFormat.score(entry.score, locale)
    val own = entry.isCurrentPlayer
    val description = stringResource(
        if (own) R.string.top10_row_own_description else R.string.top10_row_description,
        entry.position,
        entry.displayName,
        score,
    )
    val appear = remember(entry) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(entry, animate) {
        if (animate) appear.animateTo(1f, tween(TopScoresMetrics.ROW_FADE_MS, delayMillis = appearDelayMs, easing = LinearOutSlowInEasing))
    }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TopScoresMetrics.RowHeight)
            .graphicsLayer {
                alpha = appear.value
                if (rise) translationY = (1f - appear.value) * TopScoresMetrics.RowRise.toPx()
            }
            .then(if (own) Modifier.background(LeakOutColors.LiquidTint, shape).border(1.dp, LeakOutColors.Liquid500, shape) else Modifier)
            .drawBehind {
                if (topLine) drawLine(LeakOutColors.LineSubtle, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx())
            }
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = TopScoresMetrics.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = HudFormat.score(entry.position, locale),
            style = LeakOutTextStyles.NumericMd,
            color = if (entry.position <= 3) LeakOutColors.Brass300 else LeakOutColors.TextSecondary,
            maxLines = 1,
            modifier = Modifier.width(TopScoresMetrics.RankWidth),
        )
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.displayName,
                style = LeakOutTextStyles.BodyMd,
                color = LeakOutColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (own) {
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.top10_you).displayUppercase(), style = LeakOutTextStyles.DisplayXs, color = LeakOutColors.Liquid300, maxLines = 1)
            }
        }
        Text(
            text = score,
            style = LeakOutTextStyles.NumericMd,
            color = LeakOutColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private val PreviewEntries = listOf("Ada", "Pipewizard", "Sam", "Jamie", "Fleur", "Mo", "Linde van der Waterleiding-Smit", "Kees", "Joy", "Ravi")
    .mapIndexed { index, name -> LeaderboardEntry(index + 1, name, listOf(2140, 1980, 1450, 1300, 1210, 1120, 990, 870, 640, 510)[index], isCurrentPlayer = index == 3) }

@Composable
private fun TopScoresPreview(state: TopScoresState) {
    LeakOutTheme {
        // At rest: no fade or scale, so the static preview shows the final look.
        TopScoresDialogContent(7, state, {}, {}, {}, PauseDialogEntry(0, 0, 1f, null), reducedMotion = true)
    }
}

@Preview(name = "Top 10, 640 × 360 (Compact)", widthDp = 640, heightDp = 360)
@Composable
private fun TopScoresCompactPreview() = TopScoresPreview(TopScoresState.Loaded(PreviewEntries))

@Preview(name = "Top 10, 1280 × 800 (Regular)", widthDp = 1280, heightDp = 800)
@Composable
private fun TopScoresRegularPreview() = TopScoresPreview(TopScoresState.Loaded(PreviewEntries))

@Preview(name = "Top 10, 640 × 360, 130 % font", widthDp = 640, heightDp = 360, fontScale = 1.3f)
@Composable
private fun TopScoresLargeFontPreview() = TopScoresPreview(TopScoresState.Loaded(PreviewEntries))

@Preview(name = "Top 10, signed out", widthDp = 640, heightDp = 360)
@Composable
private fun TopScoresSignedOutPreview() = TopScoresPreview(TopScoresState.SignedOut(canSignIn = true))

@Preview(name = "Top 10, error", widthDp = 640, heightDp = 360)
@Composable
private fun TopScoresErrorPreview() = TopScoresPreview(TopScoresState.Failed)
