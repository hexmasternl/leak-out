package nl.hexmaster.leakout.history

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Test

/** The keep-the-best rule and its validation, through the in-memory history that applies it. */
class LevelHistoryRulesTest {
    private val history = InMemoryLevelHistoryRepository()

    private suspend fun assertRejected(record: suspend () -> Unit) {
        try {
            record()
            fail("Expected the recording to be rejected")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `a first win creates the record`() = runTest {
        val result = history.recordCompletion(levelId = 3, score = 240, stars = 2)
        assertEquals(RecordResult.FirstCompletion, result)
        assertEquals(LevelHistory(3, 240, 2), history.get(3))
        assertTrue(history.isFinished(3))
    }

    @Test
    fun `a better score replaces highscore and stars together`() = runTest {
        history.recordCompletion(3, 240, 2)
        val result = history.recordCompletion(3, 380, 3)
        assertEquals(RecordResult.NewHighscore(previous = 240), result)
        assertEquals(LevelHistory(3, 380, 3), history.get(3))
    }

    @Test
    fun `a lower score keeps the record`() = runTest {
        history.recordCompletion(3, 380, 3)
        val result = history.recordCompletion(3, 200, 1)
        assertEquals(RecordResult.NoImprovement, result)
        assertEquals(LevelHistory(3, 380, 3), history.get(3))
    }

    @Test
    fun `an equal score keeps the record, even with more stars`() = runTest {
        history.recordCompletion(3, 240, 2)
        val result = history.recordCompletion(3, 240, 3)
        assertEquals(RecordResult.NoImprovement, result)
        assertEquals(LevelHistory(3, 240, 2), history.get(3))
    }

    @Test
    fun `a better score with fewer stars still replaces both`() = runTest {
        // Stars belong to the score they were earned with, so they are never maxed separately.
        history.recordCompletion(3, 240, 3)
        history.recordCompletion(3, 250, 2)
        assertEquals(LevelHistory(3, 250, 2), history.get(3))
    }

    @Test
    fun `a win with 0 points and 0 stars still counts as finished`() = runTest {
        assertEquals(RecordResult.FirstCompletion, history.recordCompletion(1, 0, 0))
        assertTrue(history.isFinished(1))
        assertEquals(RecordResult.NewHighscore(previous = 0), history.recordCompletion(1, 20, 1))
    }

    @Test
    fun `an unplayed level has no record`() = runTest {
        history.recordCompletion(1, 100, 1)
        assertNull(history.get(2))
        assertFalse(history.isFinished(2))
    }

    @Test
    fun `a negative score is rejected and leaves the history unchanged`() = runTest {
        history.recordCompletion(3, 240, 2)
        assertRejected { history.recordCompletion(3, -5, 3) }
        assertRejected { history.recordCompletion(4, -5, 3) }
        assertEquals(listOf(LevelHistory(3, 240, 2)), history.observeAll().first())
    }

    @Test
    fun `-1 and 4 stars are rejected and leave the history unchanged`() = runTest {
        history.recordCompletion(3, 240, 2)
        for (stars in listOf(-1, 4)) {
            assertRejected { history.recordCompletion(3, 999, stars) }
            assertRejected { history.recordCompletion(5, 999, stars) }
        }
        assertEquals(listOf(LevelHistory(3, 240, 2)), history.observeAll().first())
    }

    @Test
    fun `the rule reports the row to write only when something changes`() {
        val stored = LevelHistory(3, 240, 2)
        assertEquals(stored, LevelHistoryRules.record(null, 3, 240, 2).record)
        assertEquals(LevelHistory(3, 300, 3), LevelHistoryRules.record(stored, 3, 300, 3).record)
        assertNull(LevelHistoryRules.record(stored, 3, 240, 3).record)
        assertNull(LevelHistoryRules.record(stored, 3, 100, 1).record)
    }

    @Test
    fun `all records are read in level order`() = runTest {
        history.recordCompletion(4, 10, 1)
        history.recordCompletion(1, 20, 2)
        history.recordCompletion(2, 30, 3)
        assertEquals(listOf(1, 2, 4), history.observeAll().first().map { it.levelId })
        assertEquals(LevelHistory(2, 30, 3), history.observe(2).first())
    }
}
