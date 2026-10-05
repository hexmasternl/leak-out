package nl.hexmaster.leakout.achievements.playgames

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.InMemoryAchievementRepository
import nl.hexmaster.leakout.core.achievements.Achievement
import org.junit.Assert.assertEquals
import org.junit.Test

/** An achievement without a Play Games id (not yet in the console export) is never sent. */
class WithPlayGamesIdTest {
    private val ids = mapOf(Achievement.WET_FEET to "CgkIhe2j6-8FEAIQFg", Achievement.APPRENTICE to "CgkIhe2j6-8FEAIQEA")
    private val recording = RecordingAchievementSync()
    private val sync = WithPlayGamesId(recording) { ids[it].orEmpty() }

    @Test
    fun `calls for achievements with an id are passed on`() = runTest {
        sync.unlock(Achievement.WET_FEET)
        sync.setSteps(Achievement.APPRENTICE, 40)
        assertEquals(listOf("unlock wet_feet", "setSteps apprentice 40"), recording.calls)
    }

    @Test
    fun `calls for achievements with an empty id are dropped`() = runTest {
        sync.unlock(Achievement.TIPS_ADDICT)
        sync.setSteps(Achievement.STAR_COLLECTOR_3, 12)
        assertEquals(emptyList<String>(), recording.calls)
    }

    @Test
    fun `a push with an unpublished achievement still sends the others and counts no failure`() = runTest {
        val all = listOf(
            AchievementProgress(Achievement.WET_FEET, unlockedAtMs = 1L),
            AchievementProgress(Achievement.TIPS_ADDICT, unlockedAtMs = 2L),
        )
        val syncer = AchievementSyncer(InMemoryAchievementRepository(initial = all), MutableStateFlow(SignInState.SignedOut), backgroundScope, RecordingLog())
        val failures = syncer.push(sync, all)
        assertEquals(0, failures)
        assertEquals(listOf("unlock wet_feet"), recording.calls)
    }
}
