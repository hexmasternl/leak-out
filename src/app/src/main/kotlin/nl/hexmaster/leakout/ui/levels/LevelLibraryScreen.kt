package nl.hexmaster.leakout.ui.levels

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.library.LevelLibraryConfig
import nl.hexmaster.leakout.core.library.LevelLibraryRules
import nl.hexmaster.leakout.ui.components.CartoonButton
import nl.hexmaster.leakout.ui.components.CartoonButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Stable handles for UI tests, independent of copy and layout. */
object LevelLibraryTags {
    const val SCREEN = "level_library_screen"
    const val PAGER = "level_library_pager"
    const val PREVIOUS = "level_library_previous"
    const val NEXT = "level_library_next"
    const val HINT = "level_library_hint"
    const val TITLE = "level_library_title"
}

/**
 * The level library bound to [viewModel]: forwards page changes and taps, starts a level through
 * [onStartLevel] and acknowledges taps on locked tiles. Back leaves through [onBack].
 */
@Composable
fun LevelLibraryScreen(
    viewModel: LevelLibraryViewModel,
    onStartLevel: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var lockedFeedback by remember { mutableStateOf<LockedFeedback?>(null) }
    val startLevel by rememberUpdatedState(onStartLevel)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LevelLibraryEvent.StartLevel -> startLevel(event.levelId)
                is LevelLibraryEvent.LevelLocked ->
                    lockedFeedback = LockedFeedback(event.levelId, (lockedFeedback?.nonce ?: 0) + 1)
            }
        }
    }
    BackHandler(onBack = onBack)
    LevelLibraryContent(
        state = state,
        onPageChange = viewModel::onPageChange,
        onLevelTap = viewModel::onLevelTap,
        lockedFeedback = lockedFeedback,
        modifier = modifier,
    )
}

/**
 * Stateless level library: header, the pager over accessible pages between previous/next buttons,
 * and a hint row with the locked-tap message or the star progress bar towards the next page.
 * Pages that are not open are not in the pager, so neither swiping nor the buttons reach them.
 */
@Composable
fun LevelLibraryContent(
    state: LevelLibraryUiState,
    onPageChange: (Int) -> Unit,
    onLevelTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lockedFeedback: LockedFeedback? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LeakOutColors.SurfaceDeep)
            .testTag(LevelLibraryTags.SCREEN),
    ) {
        val pages = state.accessiblePages
        if (!state.loaded || pages.isEmpty()) return@Box

        val pagerState = rememberPagerState(initialPage = state.currentIndex) { pages.size }
        val currentPages by rememberUpdatedState(pages)
        val pageChanged by rememberUpdatedState(onPageChange)
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { index ->
                currentPages.getOrNull(index)?.let { pageChanged(it.number) }
            }
        }
        val scope = rememberCoroutineScope()
        fun scrollBy(delta: Int) {
            scope.launch { pagerState.animateScrollToPage((pagerState.targetPage + delta).coerceIn(0, pages.lastIndex)) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = LevelGridLayout.SCREEN_PADDING_H_DP.dp, vertical = LevelGridLayout.SCREEN_PADDING_V_DP.dp),
            verticalArrangement = Arrangement.spacedBy(LevelGridLayout.SECTION_SPACING_DP.dp),
        ) {
            Header(currentPage = state.currentPage)
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LevelGridLayout.SECTION_SPACING_DP.dp),
            ) {
                PageButton(
                    text = "<",
                    description = stringResource(R.string.level_library_previous),
                    enabled = state.hasPrevious,
                    tag = LevelLibraryTags.PREVIOUS,
                    onClick = { scrollBy(-1) },
                )
                Pager(pagerState, state, onLevelTap, lockedFeedback, Modifier.weight(1f).fillMaxHeight())
                PageButton(
                    text = ">",
                    description = stringResource(R.string.level_library_next),
                    enabled = state.hasNext,
                    tag = LevelLibraryTags.NEXT,
                    onClick = { scrollBy(1) },
                )
            }
            Hint(state, lockedFeedback)
        }
    }
}

@Composable
private fun Pager(
    pagerState: PagerState,
    state: LevelLibraryUiState,
    onLevelTap: (Int) -> Unit,
    lockedFeedback: LockedFeedback?,
    modifier: Modifier,
) {
    val pages = state.accessiblePages
    HorizontalPager(state = pagerState, modifier = modifier.testTag(LevelLibraryTags.PAGER)) { index ->
        pages.getOrNull(index)?.let { page ->
            LevelGridPage(page = page, onLevelTap = onLevelTap, lockedFeedback = lockedFeedback)
        }
    }
}

@Composable
private fun Header(currentPage: Int) {
    val title = stringResource(R.string.level_library_title).uppercase()
    val page = stringResource(R.string.level_library_page, currentPage)
    // Sized in dp like the tiles so large font scaling cannot push the grid off the screen.
    val textSize = with(LocalDensity.current) { HEADER_TEXT_DP.dp.toSp() }
    Row(
        modifier = Modifier.fillMaxWidth().height(LevelGridLayout.HEADER_HEIGHT_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = LeakOutTextStyles.Header2.copy(fontSize = textSize),
            color = LeakOutColors.SurfaceCard,
            modifier = Modifier.testTag(LevelLibraryTags.TITLE),
        )
        Text(text = page, style = LeakOutTextStyles.Header2.copy(fontSize = textSize), color = LeakOutColors.TextMuted)
    }
}

@Composable
private fun PageButton(text: String, description: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    CartoonButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        variant = CartoonButtonVariant.Primary,
        faceHeight = 48.dp,
        modifier = Modifier
            .width(LevelGridLayout.NAV_BUTTON_WIDTH_DP.dp)
            .testTag(tag)
            .semantics { contentDescription = description },
    )
}

/**
 * The hint row: the locked-tap message for [LOCKED_MESSAGE_MS], otherwise the star progress towards
 * the next page. Both share this fixed-height row so the grid never moves; on the last page it is empty.
 */
@Composable
private fun Hint(state: LevelLibraryUiState, lockedFeedback: LockedFeedback?) {
    var lockedMessageFor by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(lockedFeedback) {
        if (lockedFeedback == null) return@LaunchedEffect
        lockedMessageFor = lockedFeedback.levelId
        delay(LOCKED_MESSAGE_MS)
        lockedMessageFor = null
    }
    val textSize = with(LocalDensity.current) { HINT_TEXT_DP.dp.toSp() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(LevelGridLayout.HINT_HEIGHT_DP.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        val lockedLevel = lockedMessageFor
        when {
            lockedLevel != null -> Text(
                text = stringResource(R.string.level_library_locked_message, lockedLevel),
                style = LeakOutTextStyles.Header2.copy(fontSize = textSize),
                color = LeakOutColors.SurfaceCard,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.testTag(LevelLibraryTags.HINT),
            )
            state.showStarProgress -> StarProgressBar(
                earned = state.currentPageStars,
                threshold = state.library.pageUnlockStars,
                progress = state.starProgress,
                reached = state.starGoalReached,
                nextPage = state.currentPage + 1,
            )
        }
    }
}

private const val HEADER_TEXT_DP = 20f
private const val HINT_TEXT_DP = 15f
const val LOCKED_MESSAGE_MS = 1500L

private fun previewState(stars: Map<Int, Int>, levels: Int = 48, page: Int? = null): LevelLibraryUiState {
    val library = LevelLibraryRules.compute(levels, stars, LevelLibraryConfig.Default)
    return LevelLibraryUiState(library, page ?: library.initialPage(), loaded = true)
}

@Preview(name = "Library: below the threshold 800x360", widthDp = 800, heightDp = 360)
@Composable
private fun LevelLibraryBelowThresholdPreview() {
    // Levels 2 and 3 skipped, 15 to 17 playable ahead; 34 of 50 stars.
    val stars = mapOf(1 to 3, 4 to 2, 14 to 2) + (5..13).associateWith { 3 }
    LeakOutTheme {
        LevelLibraryContent(previewState(stars, page = 1), onPageChange = {}, onLevelTap = {})
    }
}

@Preview(name = "Library: threshold reached 640x320 (small)", widthDp = 640, heightDp = 320)
@Composable
private fun LevelLibraryReachedPreview() {
    LeakOutTheme {
        LevelLibraryContent(previewState((1..19).associateWith { 3 }, page = 1), onPageChange = {}, onLevelTap = {})
    }
}

@Preview(name = "Library: last page 800x360", widthDp = 800, heightDp = 360)
@Composable
private fun LevelLibraryLastPagePreview() {
    LeakOutTheme {
        LevelLibraryContent(previewState((1..26).associateWith { 3 }, levels = 30), onPageChange = {}, onLevelTap = {})
    }
}
