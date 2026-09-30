package nl.hexmaster.leakout.ui.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import nl.hexmaster.leakout.core.library.LevelLibrary
import nl.hexmaster.leakout.core.library.LevelLibraryConfig
import nl.hexmaster.leakout.core.library.LevelLibraryRules
import nl.hexmaster.leakout.core.library.LibraryPage

/**
 * What the level library screen shows.
 *
 * @param currentPage the 1-based number of the page shown; always an accessible page when there is one.
 * @param loaded false until the history has been read once, so the screen does not flash page 1.
 */
data class LevelLibraryUiState(
    val library: LevelLibrary,
    val currentPage: Int,
    val loaded: Boolean,
) {
    /** The pages the pager holds, in order. Locked pages are not part of it, so swiping cannot reach them. */
    val accessiblePages: List<LibraryPage> get() = library.accessiblePages

    /** Index of [currentPage] in [accessiblePages], 0 when it is not there. */
    val currentIndex: Int get() = accessiblePages.indexOfFirst { it.number == currentPage }.coerceAtLeast(0)

    val hasPrevious: Boolean get() = currentIndex > 0

    val hasNext: Boolean get() = currentIndex < accessiblePages.lastIndex

    /** The next page exists but cannot be entered yet: the screen shows star progress towards it. */
    val nextPageLocked: Boolean
        get() = library.page(currentPage + 1)?.isAccessible == false

    /** Stars earned on the current page, shown against [LevelLibrary.pageUnlockStars]. */
    val currentPageStars: Int get() = library.page(currentPage)?.earnedStars ?: 0

    companion object {
        val Loading = LevelLibraryUiState(LevelLibrary(emptyList(), 0), currentPage = 1, loaded = false)
    }
}

/** One-off effects for the host. */
sealed interface LevelLibraryEvent {
    /** Start [levelId]: the player tapped an unlocked or completed tile. */
    data class StartLevel(val levelId: Int) : LevelLibraryEvent

    /** The player tapped locked [levelId]: show brief locked feedback, start nothing. */
    data class LevelLocked(val levelId: Int) : LevelLibraryEvent
}

/**
 * Combines the history, the level count and [config] into [uiState] through [LevelLibraryRules].
 * The page the player picked is kept until [reopen], which the host calls whenever it navigates to
 * the library so it opens on a useful page again.
 */
class LevelLibraryViewModel(
    history: HistoryReader,
    private val catalog: LevelCatalog,
    private val config: LevelLibraryConfig = LevelLibraryConfig.Default,
) : ViewModel() {
    /** The page the player moved to; null means "the page [LevelLibrary.initialPage] picks". */
    private val selectedPage = MutableStateFlow<Int?>(null)
    private val _events = Channel<LevelLibraryEvent>(Channel.BUFFERED)

    val events: Flow<LevelLibraryEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<LevelLibraryUiState> =
        combine(history.stars(), selectedPage) { stars, selected ->
            val library = LevelLibraryRules.compute(catalog.levelCount(), stars, config)
            val page = selected?.takeIf { library.page(it)?.isAccessible == true } ?: library.initialPage()
            LevelLibraryUiState(library, page, loaded = true)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, LevelLibraryUiState.Loading)

    /** The player moved to page [number]. Pages without an unlocked level are refused. */
    fun onPageChange(number: Int) {
        if (uiState.value.library.page(number)?.isAccessible == true) selectedPage.value = number
    }

    fun onLevelTap(levelId: Int) {
        val tile = uiState.value.library.tile(levelId) ?: return
        _events.trySend(
            if (tile.isPlayable) LevelLibraryEvent.StartLevel(levelId) else LevelLibraryEvent.LevelLocked(levelId),
        )
    }

    /** Forgets the picked page, so the library opens on its initial page again. */
    fun reopen() {
        selectedPage.value = null
    }
}
