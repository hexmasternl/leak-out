package nl.hexmaster.leakout.history

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryLevelHistoryClearAllTest {
    private fun history() = InMemoryLevelHistoryRepository(
        listOf(LevelHistory(1, 100, 3), LevelHistory(2, 80, 2), LevelHistory(3, 40, 1)),
    )

    @Test
    fun `clearing removes every record`() = runTest {
        val history = history()
        history.clearAll()
        assertTrue(history.observeAll().first().isEmpty())
        for (level in 1..3) assertNull(history.get(level))
        assertTrue(!history.isFinished(1))
    }

    @Test
    fun `observers of all records receive an empty list`() = runTest {
        val history = history()
        val emptied = async(start = CoroutineStart.UNDISPATCHED) { history.observeAll().first { it.isEmpty() } }
        history.clearAll()
        assertEquals(emptyList<LevelHistory>(), emptied.await())
    }

    @Test
    fun `observers of one level see it unfinished`() = runTest {
        val history = history()
        val cleared = async(start = CoroutineStart.UNDISPATCHED) { history.observe(2).first { it == null } }
        history.clearAll()
        assertNull(cleared.await())
    }

    @Test
    fun `a level finished after clearing is a first completion again`() = runTest {
        val history = history()
        history.clearAll()
        assertEquals(RecordResult.FirstCompletion, history.recordCompletion(1, 10, 1))
        assertEquals(listOf(LevelHistory(1, 10, 1)), history.observeAll().first())
    }

    @Test
    fun `clearing an empty history is harmless`() = runTest {
        val history = InMemoryLevelHistoryRepository()
        history.clearAll()
        assertTrue(history.observeAll().first().isEmpty())
    }
}
