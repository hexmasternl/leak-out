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
        config: LevelLibraryConfig = LevelLibraryConfig(36),
    ) = LevelLibraryViewModel(history, FakeCatalog(levels), config)

    private fun finished(ids: IntRange, stars: Int = 1) = ids.associateWith { stars }

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
        assertEquals(TileState.Unlocked, vm.uiState.value.library.tile(2)!!.state)
    }

    @Test
    fun `opens on the initial page`() {
        val vm = viewModel(FakeHistory(finished(1..24)))
        assertEquals(2, vm.uiState.value.currentPage)
        assertEquals(1, vm.uiState.value.currentIndex)
    }

    @Test
    fun `page change to an accessible page is kept`() {
        val vm = viewModel(FakeHistory(finished(1..24)))
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
    fun `reopen returns to the initial page`() {
        val vm = viewModel(FakeHistory(finished(1..24)))
        vm.onPageChange(1)
        vm.reopen()
        assertEquals(2, vm.uiState.value.currentPage)
    }

    /** Opens the library as the play menu's Select level does: on the page of the next level. */
    private fun openFromPlayMenu(stars: Map<Int, Int>, levels: Int = 72): LevelLibraryUiState {
        val vm = viewModel(FakeHistory(stars), levels = levels)
        vm.onPageChange(1)
        vm.reopen(NextLevelResolver.resolve(levels, stars, LevelLibraryConfig(36)).page)
        return vm.uiState.value
    }

    @Test
    fun `opened from the play menu with highest completed 30 lands on page 2`() {
        assertEquals(2, openFromPlayMenu(finished(1..30)).currentPage)
    }

    @Test
    fun `opened from the play menu with highest completed 24 lands on page 2`() {
        assertEquals(2, openFromPlayMenu(finished(1..24)).currentPage)
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
        val vm = viewModel(FakeHistory(finished(1..30)), levels = 72)
        vm.onPageChange(1)
        vm.reopen()
        assertEquals(2, vm.uiState.value.currentPage)
    }

    @Test
    fun `next page locked shows star progress`() {
        val vm = viewModel(FakeHistory(finished(1..5, stars = 2)))
        val state = vm.uiState.value
        assertFalse(state.hasNext)
        assertTrue(state.nextPageLocked)
        assertEquals(10, state.currentPageStars)
        assertEquals(36, state.library.pageUnlockStars)
    }

    @Test
    fun `no locked hint on the last page`() {
        val vm = viewModel(FakeHistory(finished(1..24)))
        assertFalse(vm.uiState.value.nextPageLocked)
    }

    @Test
    fun `a changed threshold is used`() {
        val vm = viewModel(FakeHistory(finished(1..10, stars = 2)), config = LevelLibraryConfig(20))
        assertTrue(vm.uiState.value.hasNext)
    }

    @Test
    fun `tapping an unlocked level raises a start event`() = runTest {
        val vm = viewModel(FakeHistory(finished(1..6)))
        vm.onLevelTap(7)
        assertEquals(LevelLibraryEvent.StartLevel(7), vm.events.first())
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
        vm.onLevelTap(5)
        assertEquals(LevelLibraryEvent.LevelLocked(5), vm.events.first())
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
        assertEquals(TileState.Locked, vm.uiState.value.library.tile(2)!!.state)
        history.recordCompletion(1, 50, 1)
        assertEquals(TileState.Completed(1), vm.uiState.value.library.tile(1)!!.state)
        assertEquals(TileState.Unlocked, vm.uiState.value.library.tile(2)!!.state)
    }

    @Test
    fun `a win with 0 stars completes the level and unlocks the next`() = runTest {
        val history = InMemoryLevelHistoryRepository()
        val vm = viewModel(history.asHistoryReader())
        history.recordCompletion(1, 5, 0)
        assertEquals(TileState.Completed(0), vm.uiState.value.library.tile(1)!!.state)
        assertEquals(TileState.Unlocked, vm.uiState.value.library.tile(2)!!.state)
    }
}
