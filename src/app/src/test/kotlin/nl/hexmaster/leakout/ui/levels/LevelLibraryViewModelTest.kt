package nl.hexmaster.leakout.ui.levels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.leakout.core.library.LevelLibraryConfig
import nl.hexmaster.leakout.core.library.NextLevelResolver
import nl.hexmaster.leakout.core.library.TileState
import nl.hexmaster.leakout.history.InMemoryLevelHistoryRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A history the test drives directly. */
private class FakeHistory(initial: Map<Int, Int> = emptyMap()) : HistoryReader {
    val flow = MutableStateFlow(initial)
    override fun stars() = flow
}

private class FakeCatalog(var count: Int) : LevelCatalog {
    override fun levelCount() = count
}

@OptIn(ExperimentalCoroutinesApi::class)
class LevelLibraryViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        history: HistoryReader = FakeHistory(),
        levels: Int = 48,
        config: LevelLibraryConfig = LevelLibraryConfig.Default,
    ) = LevelLibraryViewModel(history, FakeCatalog(levels), config)

    private fun finished(ids: IntRange, stars: Int = 1) = ids.associateWith { stars }

    /** Page 1 with exactly [total] stars (at most 72), spread over its first levels. */
    private fun page1Stars(total: Int): Map<Int, Int> =
        finished(1..total / 3, stars = 3) + if (total % 3 > 0) mapOf(total / 3 + 1 to total % 3) else emptyMap()

    @Test
    fun `state is loaded from the history and level count`() {
        val state = viewModel().uiState.value
        assertTrue(state.loaded)
        assertEquals(2, state.library.pages.size)
        assertEquals(TileState.Unlocked, state.library.tile(1)!!.state)
        assertEquals(1, state.currentPage)
    }

    @Test
    fun `loading state before any history is not loaded`() {
        assertFalse(LevelLibraryUiState.Loading.loaded)
    }

    @Test
    fun `a new history emission updates the tiles`() {
        val history = FakeHistory()
        val vm = viewModel(history)
        history.flow.value = mapOf(1 to 3)
        assertEquals(TileState.Completed(3), vm.uiState.value.library.tile(1)!!.state)
        assertEquals(TileState.Unlocked, vm.uiState.value.library.tile(4)!!.state)
    }

    @Test
    fun `opens on the initial page`() {
        val vm = viewModel(FakeHistory(finished(1..24, stars = 3)))
        assertEquals(2, vm.uiState.value.currentPage)
        assertEquals(1, vm.uiState.value.currentIndex)
    }

    @Test
    fun `page change to an accessible page is kept`() {
        val vm = viewModel(FakeHistory(finished(1..24, stars = 3)))
        vm.onPageChange(1)
        assertEquals(1, vm.uiState.value.currentPage)
        assertTrue(vm.uiState.value.hasNext)
        assertFalse(vm.uiState.value.hasPrevious)
    }

    @Test
    fun `page change to a locked page is refused`() {
        val vm = viewModel()
        vm.onPageChange(2)
        assertEquals(1, vm.uiState.value.currentPage)
        vm.onPageChange(9)
        assertEquals(1, vm.uiState.value.currentPage)
    }

    @Test
    fun `a page holding a finished level is still refused while it is closed`() {
        val vm = viewModel(FakeHistory(mapOf(1 to 1, 30 to 3)))
        vm.onPageChange(2)
        assertEquals(1, vm.uiState.value.currentPage)
        assertEquals(listOf(1), vm.uiState.value.accessiblePages.map { it.number })
    }

    @Test
    fun `reopen returns to the initial page`() {
        val vm = viewModel(FakeHistory(finished(1..24, stars = 3)))
        vm.onPageChange(1)
        vm.reopen()
        assertEquals(2, vm.uiState.value.currentPage)
    }

    /** Opens the library as the play menu's Select level does: on the page of the next level. */
    private fun openFromPlayMenu(stars: Map<Int, Int>, levels: Int = 72): LevelLibraryUiState {
        val vm = viewModel(FakeHistory(stars), levels = levels)
        vm.onPageChange(1)
        vm.reopen(NextLevelResolver.resolve(levels, stars, LevelLibraryConfig.Default).page)
        return vm.uiState.value
    }

    @Test
    fun `opened from the play menu with highest completed 30 lands on page 2`() {
        assertEquals(2, openFromPlayMenu(finished(1..30, stars = 3)).currentPage)
    }

    @Test
    fun `opened from the play menu with highest completed 24 and enough stars lands on page 2`() {
        assertEquals(2, openFromPlayMenu(finished(1..24, stars = 3)).currentPage)
    }

    @Test
    fun `opened from the play menu with level 25 behind the star gate lands on page 1`() {
        assertEquals(1, openFromPlayMenu(finished(1..24, stars = 2)).currentPage)
    }

    @Test
    fun `opened from the play menu with nothing completed lands on page 1`() {
        assertEquals(1, openFromPlayMenu(emptyMap()).currentPage)
    }

    @Test
    fun `reopen on a page that cannot be entered is clamped to the nearest accessible page below`() {
        val vm = viewModel(FakeHistory(finished(1..5)), levels = 72)
        vm.reopen(3)
        assertEquals(1, vm.uiState.value.currentPage)
        vm.reopen(99)
        assertEquals(1, vm.uiState.value.currentPage)
    }

    @Test
    fun `reopen without a page keeps the useful page rule`() {
        val vm = viewModel(FakeHistory(finished(1..30, stars = 3)), levels = 72)
        vm.onPageChange(1)
        vm.reopen()
        assertEquals(2, vm.uiState.value.currentPage)
    }

    @Test
    fun `progress below the threshold`() {
        val state = viewModel(FakeHistory(page1Stars(34))).uiState.value
        assertEquals(34, state.currentPageStars)
        assertEquals(50, state.library.pageUnlockStars)
        assertEquals(0.68f, state.starProgress, 0.0001f)
        assertTrue(state.showStarProgress)
        assertFalse(state.starGoalReached)
        assertTrue(state.nextPageLocked)
        assertFalse(state.hasNext)
    }

    @Test
    fun `progress at the threshold is full and enables next`() {
        val vm = viewModel(FakeHistory(page1Stars(50)))
        vm.onPageChange(1)
        val state = vm.uiState.value
        assertEquals(1, state.currentPage)
        assertEquals(1f, state.starProgress, 0f)
        assertTrue(state.starGoalReached)
        assertTrue(state.showStarProgress)
        assertTrue(state.hasNext)
        assertFalse(state.nextPageLocked)
    }

    @Test
    fun `progress above the threshold stays full`() {
        val vm = viewModel(FakeHistory(page1Stars(55)))
        vm.onPageChange(1)
        val state = vm.uiState.value
        assertEquals(55, state.currentPageStars)
        assertEquals(1f, state.starProgress, 0f)
        assertTrue(state.starGoalReached)
    }

    @Test
    fun `no progress bar on the last page`() {
        val vm = viewModel(FakeHistory(finished(1..24, stars = 3)))
        val state = vm.uiState.value
        assertEquals(2, state.currentPage)
        assertFalse(state.showStarProgress)
        assertFalse(state.nextPageLocked)
    }

    @Test
    fun `no progress bar with a single page`() {
        val state = viewModel(levels = 24).uiState.value
        assertFalse(state.showStarProgress)
    }

    @Test
    fun `progress follows the current page`() {
        // Page 1 with 60 stars, page 2 with 12 stars, page 3 exists.
        val stars = page1Stars(60) + finished(25..30, stars = 2)
        val vm = viewModel(FakeHistory(stars), levels = 72)
        vm.onPageChange(1)
        assertEquals(60, vm.uiState.value.currentPageStars)
        assertEquals(1f, vm.uiState.value.starProgress, 0f)
        vm.onPageChange(2)
        val page2 = vm.uiState.value
        assertEquals(2, page2.currentPage)
        assertEquals(12, page2.currentPageStars)
        assertEquals(0.24f, page2.starProgress, 0.0001f)
        assertTrue(page2.showStarProgress)
        assertFalse(page2.starGoalReached)
    }

    @Test
    fun `next is disabled until the gate is met`() {
        val history = FakeHistory(page1Stars(49))
        val vm = viewModel(history)
        assertFalse(vm.uiState.value.hasNext)
        history.flow.value = page1Stars(50)
        vm.onPageChange(1)
        assertTrue(vm.uiState.value.hasNext)
    }

    @Test
    fun `a threshold of 0 shows a full bar`() {
        val state = viewModel(config = LevelLibraryConfig(0)).uiState.value
        assertEquals(1f, state.starProgress, 0f)
        assertTrue(state.starGoalReached)
    }

    @Test
    fun `star progress is clamped to 0 to 1`() {
        assertEquals(0f, starProgress(0, 50), 0f)
        assertEquals(0.5f, starProgress(25, 50), 0f)
        assertEquals(1f, starProgress(50, 50), 0f)
        assertEquals(1f, starProgress(72, 50), 0f)
        assertEquals(0f, starProgress(-3, 50), 0f)
        assertEquals(1f, starProgress(0, 0), 0f)
    }

    @Test
    fun `a changed threshold is used`() {
        val vm = viewModel(FakeHistory(finished(1..10, stars = 2)), config = LevelLibraryConfig(20))
        assertTrue(vm.uiState.value.hasNext)
    }

    @Test
    fun `tapping an unlocked level raises a start event`() = runTest {
        val vm = viewModel(FakeHistory(finished(1..6)))
        vm.onLevelTap(9)
        assertEquals(LevelLibraryEvent.StartLevel(9), vm.events.first())
    }

    @Test
    fun `tapping a skipped level raises a start event`() = runTest {
        val vm = viewModel(FakeHistory(mapOf(1 to 3, 4 to 2)))
        vm.onLevelTap(2)
        assertEquals(LevelLibraryEvent.StartLevel(2), vm.events.first())
    }

    @Test
    fun `tapping a completed level raises a start event`() = runTest {
        val vm = viewModel(FakeHistory(finished(1..6)))
        vm.onLevelTap(3)
        assertEquals(LevelLibraryEvent.StartLevel(3), vm.events.first())
    }

    @Test
    fun `tapping a locked level raises locked feedback, not a start`() = runTest {
        val vm = viewModel()
        vm.onLevelTap(4)
        assertEquals(LevelLibraryEvent.LevelLocked(4), vm.events.first())
    }

    @Test
    fun `tapping a level that does not exist does nothing`() = runTest {
        val vm = viewModel(levels = 10)
        vm.onLevelTap(11)
        vm.onLevelTap(1)
        assertEquals(LevelLibraryEvent.StartLevel(1), vm.events.first())
    }

    @Test
    fun `the level history is read as the stars of each highscore`() = runTest {
        val history = InMemoryLevelHistoryRepository()
        val reader = history.asHistoryReader()
        history.recordCompletion(1, 200, 2)
        history.recordCompletion(1, 100, 3)
        assertEquals(mapOf(1 to 2), reader.stars().first())
        history.recordCompletion(1, 300, 3)
        history.recordCompletion(2, 5, 0)
        assertEquals(mapOf(1 to 3, 2 to 0), reader.stars().first())
    }

    @Test
    fun `a recorded win updates the library`() = runTest {
        val history = InMemoryLevelHistoryRepository()
        val vm = viewModel(history.asHistoryReader())
        assertEquals(TileState.Locked, vm.uiState.value.library.tile(4)!!.state)
        history.recordCompletion(1, 50, 1)
        assertEquals(TileState.Completed(1), vm.uiState.value.library.tile(1)!!.state)
        assertEquals(TileState.Unlocked, vm.uiState.value.library.tile(4)!!.state)
    }

    @Test
    fun `a win with 0 stars completes the level and extends the window`() = runTest {
        val history = InMemoryLevelHistoryRepository()
        val vm = viewModel(history.asHistoryReader())
        history.recordCompletion(1, 5, 0)
        assertEquals(TileState.Completed(0), vm.uiState.value.library.tile(1)!!.state)
        assertEquals(TileState.Unlocked, vm.uiState.value.library.tile(4)!!.state)
    }
}
