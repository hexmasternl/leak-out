package nl.hexmaster.leakout.achievements.playgames

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.InMemoryAchievementRepository
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.Evaluation
import nl.hexmaster.leakout.core.achievements.PlayerStats
import org.junit.Assert.assertEquals
import org.junit.Test

/** The syncer pushes the whole local state to Play Games, and only while signed in. */
class AchievementSyncerTest {
    private val store = InMemoryAchievementRepository(
        initial = listOf(AchievementProgress(Achievement.ZERO_WASTE, unlockedAtMs = 5L), AchievementProgress(Achievement.APPRENTICE, steps = 40)),
    )
    private val signIn = MutableStateFlow<SignInState>(SignInState.SignedOut)
    private val sync = RecordingAchievementSync()
    private val log = RecordingLog()

    private fun TestScope.started(): AchievementSyncer =
        AchievementSyncer(store, signIn, backgroundScope, log).apply {
            bind(sync)
            start()
            testScheduler.runCurrent()
        }

    private val fullPush = listOf("unlock zero_waste", "setSteps apprentice 40")

    @Test
    fun `nothing is pushed while signed out`() = runTest {
        started()
        assertEquals(emptyList<String>(), sync.calls)
    }

    @Test
    fun `signing in pushes every unlock and every progress`() = runTest {
        started()
        signIn.value = SignInState.SignedIn("Eduard")
        testScheduler.runCurrent()
        assertEquals(fullPush, sync.calls)
    }

    @Test
    fun `a new unlock while signed in is pushed`() = runTest {
        signIn.value = SignInState.SignedIn("Eduard")
        started()
        sync.calls.clear()

        store.apply(Evaluation(listOf(Achievement.WET_FEET), emptyMap()), PlayerStats(losses = 1))
        testScheduler.runCurrent()

        assertEquals(listOf("unlock zero_waste", "setSteps apprentice 40", "unlock wet_feet"), sync.calls)
    }

    @Test
    fun `an unlocked incremental achievement is pushed with its full steps, never with increment`() = runTest {
        store.apply(Evaluation(listOf(Achievement.APPRENTICE), mapOf(Achievement.APPRENTICE to 100)), PlayerStats(sectionsPlaced = 100))
        val syncer = AchievementSyncer(store, signIn, backgroundScope, log)
        syncer.push(sync, store.observeAll().first())
        assertEquals(listOf("unlock zero_waste", "setSteps apprentice 100"), sync.calls)
    }

    @Test
    fun `pushing the same state twice makes the same calls twice`() = runTest {
        val syncer = AchievementSyncer(store, signIn, backgroundScope, log)
        val all = store.observeAll().first()
        syncer.push(sync, all)
        syncer.push(sync, all)
        assertEquals(fullPush + fullPush, sync.calls)
    }

    @Test
    fun `a failed push leaves local data alone and the next push sends it again`() = runTest {
        sync.failing = setOf(Achievement.ZERO_WASTE)
        val before = store.observeAll().first()
        val syncer = AchievementSyncer(store, signIn, backgroundScope, log)

        assertEquals(1, syncer.push(sync, before))
        assertEquals("the other calls are still made", fullPush, sync.calls)
        assertEquals(listOf("unlock zero_waste"), log.failures)
        assertEquals(before, store.observeAll().first())

        sync.failing = emptySet()
        assertEquals(0, syncer.push(sync, store.observeAll().first()))
        assertEquals(fullPush + fullPush, sync.calls)
    }

    @Test
    fun `signing in again after a failure pushes again`() = runTest {
        sync.failing = setOf(Achievement.ZERO_WASTE)
        started()
        signIn.value = SignInState.SignedIn("Eduard")
        testScheduler.runCurrent()
        sync.failing = emptySet()
        signIn.value = SignInState.SignedOut
        testScheduler.runCurrent()
        signIn.value = SignInState.SignedIn("Eduard")
        testScheduler.runCurrent()
        assertEquals(fullPush + fullPush, sync.calls)
    }

    @Test
    fun `unavailable makes no calls`() = runTest {
        signIn.value = SignInState.Unavailable
        started()
        store.apply(Evaluation(listOf(Achievement.WET_FEET), emptyMap()), PlayerStats(losses = 1))
        testScheduler.runCurrent()
        assertEquals(emptyList<String>(), sync.calls)
    }

    @Test
    fun `without a bound sync nothing is pushed, and binding one pushes`() = runTest {
        signIn.value = SignInState.SignedIn("Eduard")
        val syncer = AchievementSyncer(store, signIn, backgroundScope, log)
        syncer.start()
        testScheduler.runCurrent()
        assertEquals(emptyList<String>(), sync.calls)

        syncer.bind(sync)
        testScheduler.runCurrent()
        assertEquals(fullPush, sync.calls)

        syncer.unbind(sync)
        store.apply(Evaluation(listOf(Achievement.WET_FEET), emptyMap()), PlayerStats(losses = 1))
        testScheduler.runCurrent()
        assertEquals("unbound: nothing more", fullPush, sync.calls)
    }

    @Test
    fun `progress at 0 is not sent`() = runTest {
        val syncer = AchievementSyncer(InMemoryAchievementRepository(), signIn, backgroundScope, log)
        syncer.push(sync, InMemoryAchievementRepository().observeAll().first())
        assertEquals(emptyList<String>(), sync.calls)
    }

    @Test
    fun `the no-op sync accepts every call`() = runTest {
        val syncer = AchievementSyncer(store, signIn, backgroundScope, log)
        assertEquals(0, syncer.push(NoOpAchievementSync, store.observeAll().first()))
    }
}
