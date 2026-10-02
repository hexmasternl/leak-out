package nl.hexmaster.leakout.achievements.playgames

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.achievements.AchievementTracker
import nl.hexmaster.leakout.achievements.InMemoryAchievementRepository
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.AttemptOutcome
import nl.hexmaster.leakout.core.achievements.AttemptSummary
import nl.hexmaster.leakout.history.InMemoryLevelHistoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The app id decides whether Play Games is used at all; without it achievements still work locally. */
class PlayGamesServicesTest {
    private val store = InMemoryAchievementRepository()
    private var sdkStarts = 0

    private fun TestScope.services(appId: String?) = PlayGamesServices(
        appId = appId,
        achievements = store,
        scope = backgroundScope,
        initializeSdk = { sdkStarts++ },
        gatewayFor = { error("no activity in JVM tests") },
        syncFor = { error("no activity in JVM tests") },
        log = RecordingLog(),
    )

    @Test
    fun `only a positive number is a configured app id`() {
        for (id in listOf("201820010117", "1", " 42 ")) assertTrue(id, PlayGamesServices.isConfigured(id))
        for (id in listOf("0", "-5", "", "  ", "abc", "12ab", null)) assertFalse("$id", PlayGamesServices.isConfigured(id))
    }

    @Test
    fun `the shipped app id starts the SDK once and starts signed out`() = runTest {
        val services = services("201820010117")
        services.start()
        assertTrue(services.configured)
        assertEquals(1, sdkStarts)
        assertEquals(SignInState.SignedOut, services.auth.state.value)
    }

    @Test
    fun `the placeholder id starts no SDK, is unavailable and still decides achievements locally`() = runTest {
        val services = services("0")
        services.start()
        val sync = RecordingAchievementSync()
        services.syncer.bind(sync)

        // A win with no unused sections, tracked as the game screen does.
        val tracker = AchievementTracker(store, InMemoryLevelHistoryRepository(), backgroundScope)
        val unlocked = tracker.onAttemptEnded(zeroWasteWin())
        testScheduler.runCurrent()

        assertFalse(services.configured)
        assertEquals(0, sdkStarts)
        assertEquals(SignInState.Unavailable, services.auth.state.value)
        assertTrue(Achievement.ZERO_WASTE in unlocked)
        assertTrue(store.isUnlocked(Achievement.ZERO_WASTE))
        assertEquals("no Play Games call", emptyList<String>(), sync.calls)
    }

    private suspend fun InMemoryAchievementRepository.isUnlocked(achievement: Achievement): Boolean =
        stored()[achievement]?.unlocked == true

    private fun zeroWasteWin() = AttemptSummary(
        levelId = 3, outcome = AttemptOutcome.WON, finalTotal = 200, finalStars = 2, unusedSections = 0,
        distinctTilesLeft = 6, sectionsLeft = 6, crossesLeftTwice = 0, placements = 6, replacements = 0,
        everPaused = false, fastForwardAtWin = false, routeCompletedBeforeFlow = false, photoFinish = false,
        freeCells = 30, piecesOnBoard = 6, record = null,
    )
}
