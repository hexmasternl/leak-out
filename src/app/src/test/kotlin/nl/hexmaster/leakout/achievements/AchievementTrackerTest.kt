package nl.hexmaster.leakout.achievements

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.AttemptOutcome
import nl.hexmaster.leakout.core.achievements.AttemptSummary
import nl.hexmaster.leakout.core.achievements.PlayerStats
import nl.hexmaster.leakout.core.achievements.RecordOutcome
import nl.hexmaster.leakout.history.InMemoryLevelHistoryRepository
import nl.hexmaster.leakout.history.LevelHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The tracker over in-memory stores: stats, unlocks and steps written once per call, backfill and reset. */
class AchievementTrackerTest {
    private val history = InMemoryLevelHistoryRepository()
    private val store = InMemoryAchievementRepository(clock = { 42L })

    private fun TestScope.tracker() = AchievementTracker(store, history, backgroundScope)

    private suspend fun progress(achievement: Achievement): AchievementProgress =
        store.observeAll().first().first { it.achievement == achievement }

    @Test
    fun `a loss counts its placements, the loss and the streak, and unlocks Wet Feet`() = runTest {
        val unlocked = tracker().onAttemptEnded(loss(levelId = 4, placements = 6, sectionsLeft = 2))
        assertEquals(listOf(Achievement.WET_FEET), unlocked)
        assertEquals(PlayerStats(sectionsPlaced = 6, losses = 1, lossStreakByLevel = mapOf(4 to 1)), store.currentStats)
        assertEquals(42L, progress(Achievement.WET_FEET).unlockedAtMs)
        assertEquals(6, progress(Achievement.APPRENTICE).steps)
    }

    @Test
    fun `an unlock is reported once, by the attempt that earned it`() = runTest {
        val tracker = tracker()
        assertEquals(listOf(Achievement.WET_FEET), tracker.onAttemptEnded(loss(sectionsLeft = 1)))
        assertEquals(emptyList<Achievement>(), tracker.onAttemptEnded(loss(sectionsLeft = 1)))
        assertEquals(2, store.currentStats.losses)
    }

    @Test
    fun `a win after three losses in a row unlocks Never Give Up and clears the streak`() = runTest {
        val tracker = tracker()
        repeat(3) { tracker.onAttemptEnded(loss(levelId = 5, sectionsLeft = 1)) }
        history.recordCompletion(5, 120, 1)
        val unlocked = tracker.onAttemptEnded(win(levelId = 5, record = RecordOutcome.FIRST_COMPLETION))
        assertTrue(Achievement.NEVER_GIVE_UP in unlocked)
        assertEquals(emptyMap<Int, Int>(), store.currentStats.lossStreakByLevel)
    }

    @Test
    fun `a win sees the history it was recorded in`() = runTest {
        history.recordCompletion(1, 300, 3)
        val unlocked = tracker().onAttemptEnded(win(levelId = 1, record = RecordOutcome.FIRST_COMPLETION))
        assertTrue(Achievement.FIRST_LEAK_FIXED in unlocked)
        assertEquals(1, progress(Achievement.PACK_1_COMPLETE).steps)
        assertEquals(3, progress(Achievement.STAR_COLLECTOR_1).steps)
    }

    @Test
    fun `an abandoned attempt adds only its placements`() = runTest {
        tracker().onAttemptAbandoned(4)
        assertEquals(PlayerStats(sectionsPlaced = 4), store.currentStats)
        assertEquals(4, progress(Achievement.APPRENTICE).steps)
    }

    @Test
    fun `an abandoned attempt can unlock a placement tier`() = runTest {
        val tracker = tracker()
        tracker.onAttemptAbandoned(99)
        assertEquals(listOf(Achievement.APPRENTICE), tracker.onAttemptAbandoned(1))
        assertEquals(100, progress(Achievement.APPRENTICE).steps)
    }

    @Test
    fun `an abandoned attempt launched in the tracker scope finishes`() = runTest {
        tracker().attemptAbandoned(7).join()
        assertEquals(7, store.currentStats.sectionsPlaced)
    }

    @Test
    fun `reconcile backfills the progress achievements of an existing history`() = runTest {
        // 14 finished levels on page 1, level 1 among them, 2 stars each.
        for (level in 1..14) history.recordCompletion(level, 100, 2)
        val unlocked = tracker().reconcile()
        assertEquals(listOf(Achievement.FIRST_LEAK_FIXED, Achievement.LEVELS_FINISHED_1, Achievement.STAR_COLLECTOR_1), unlocked)
        assertEquals(14, progress(Achievement.PACK_1_COMPLETE).steps)
        assertEquals(28, progress(Achievement.STAR_COLLECTOR_2).steps)
        assertEquals(PlayerStats.NONE, store.currentStats)
    }

    @Test
    fun `reconcile is idempotent`() = runTest {
        for (level in 1..3) history.recordCompletion(level, 100, 3)
        val tracker = tracker()
        tracker.reconcile()
        val applied = store.applied.size
        assertEquals(emptyList<Achievement>(), tracker.reconcile())
        assertEquals("nothing changed, nothing written", applied, store.applied.size)
    }

    @Test
    fun `reconcile with nothing stored and no history writes nothing`() = runTest {
        assertEquals(emptyList<Achievement>(), tracker().reconcile())
        assertEquals(0, store.applied.size)
    }

    @Test
    fun `a progress reset keeps unlocks, steps and stats`() = runTest {
        for (level in 1..10) history.recordCompletion(level, 100, 3)
        val tracker = tracker()
        tracker.onAttemptAbandoned(12)
        tracker.reconcile()
        assertTrue(progress(Achievement.LEVELS_FINISHED_1).unlocked)

        history.clearAll()
        assertEquals(emptyList<Achievement>(), tracker.reconcile())

        assertEquals(emptyList<LevelHistory>(), history.observeAll().first())
        assertTrue(progress(Achievement.LEVELS_FINISHED_1).unlocked)
        assertTrue(progress(Achievement.FIRST_LEAK_FIXED).unlocked)
        assertEquals("progress never goes down", 10, progress(Achievement.PACK_1_COMPLETE).steps)
        assertEquals(12, store.currentStats.sectionsPlaced)
    }

    @Test
    fun `a locked achievement has no unlock time`() = runTest {
        tracker().reconcile()
        assertNull(progress(Achievement.ZERO_WASTE).unlockedAtMs)
    }
}

class AchievementTrackerTipsTest {
    private val store = InMemoryAchievementRepository(clock = { 42L })

    private fun TestScope.tracker() = AchievementTracker(store, InMemoryLevelHistoryRepository(), backgroundScope)

    @Test
    fun `the first tips reset unlocks Tips Addict and the second unlocks nothing`() = runTest {
        val tracker = tracker()
        assertEquals(listOf(Achievement.TIPS_ADDICT), tracker.onTipsReset())
        assertEquals(emptyList<Achievement>(), tracker.onTipsReset())
        assertEquals(2, store.currentStats.tipsResets)
        assertTrue(store.observeAll().first().first { it.achievement == Achievement.TIPS_ADDICT }.unlocked)
    }

    @Test
    fun `a tips reset started in the tracker's scope is counted`() = runTest {
        tracker().tipsReset().join()
        assertEquals(1, store.currentStats.tipsResets)
    }

    @Test
    fun `attempts keep the tips resets`() = runTest {
        val tracker = tracker()
        tracker.onTipsReset()
        tracker.onAttemptEnded(loss(placements = 3))
        assertEquals(PlayerStats(sectionsPlaced = 3, losses = 1, lossStreakByLevel = mapOf(3 to 1), tipsResets = 1), store.currentStats)
    }
}

internal fun loss(levelId: Int = 3, placements: Int = 0, sectionsLeft: Int = 0) = summary(levelId, AttemptOutcome.LOST, placements, sectionsLeft, null)

internal fun win(levelId: Int = 3, placements: Int = 5, record: RecordOutcome? = RecordOutcome.FIRST_COMPLETION) =
    summary(levelId, AttemptOutcome.WON, placements, sectionsLeft = placements, record = record)

private fun summary(levelId: Int, outcome: AttemptOutcome, placements: Int, sectionsLeft: Int, record: RecordOutcome?) = AttemptSummary(
    levelId = levelId,
    outcome = outcome,
    finalTotal = if (outcome == AttemptOutcome.WON) 120 else null,
    finalStars = if (outcome == AttemptOutcome.WON) 1 else 0,
    unusedSections = 1,
    distinctTilesLeft = sectionsLeft,
    sectionsLeft = sectionsLeft,
    crossesLeftTwice = 0,
    placements = placements,
    replacements = 1,
    everPaused = true,
    fastForwardAtWin = false,
    routeCompletedBeforeFlow = false,
    photoFinish = false,
    freeCells = 40,
    piecesOnBoard = placements,
    record = record,
)
