package nl.hexmaster.leakout.ui.achievements

import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.InMemoryAchievementRepository
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.AchievementType
import nl.hexmaster.leakout.core.achievements.Evaluation
import nl.hexmaster.leakout.core.achievements.PlayerStats
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(initial: List<AchievementProgress> = emptyList(), signIn: SignInState = SignInState.SignedOut) =
        AchievementsViewModel(InMemoryAchievementRepository(initial = initial), MutableStateFlow(signIn))

    @Test
    fun `every achievement is listed in catalogue order, signed out too`() {
        val state = vm(signIn = SignInState.SignedOut).uiState.value!!
        assertEquals(Achievement.entries.toList(), state.cards.map { it.achievement })
        assertFalse(state.signedIn)
    }

    @Test
    fun `the count is unlocked out of the catalogue`() {
        val unlocked = Achievement.entries.take(7).map { AchievementProgress(it, unlockedAtMs = 1L) }
        val state = vm(unlocked).uiState.value!!
        assertEquals(7, state.unlocked)
        assertEquals(25, state.total)
    }

    @Test
    fun `only a signed-in player gets the Play Games button`() {
        assertTrue(vm(signIn = SignInState.SignedIn("Eduard")).uiState.value!!.signedIn)
        for (state in listOf(SignInState.SignedOut, SignInState.SigningIn, SignInState.Unavailable)) {
            assertFalse(state.toString(), vm(signIn = state).uiState.value!!.signedIn)
        }
    }

    @Test
    fun `a new unlock shows up on the screen`() = runTest {
        val store = InMemoryAchievementRepository()
        val vm = AchievementsViewModel(store, MutableStateFlow(SignInState.SignedOut))
        store.apply(Evaluation(listOf(Achievement.WET_FEET), emptyMap()), PlayerStats.NONE)
        assertEquals(CardLook.Unlocked, vm.uiState.value!!.cards.single { it.achievement == Achievement.WET_FEET }.look)
        assertEquals(1, vm.uiState.value!!.unlocked)
    }

    @Test
    fun `a locked incremental achievement shows its progress`() {
        val card = AchievementCard.of(AchievementProgress(Achievement.JOURNEYMAN, steps = 370))
        assertEquals(CardLook.Locked, card.look)
        assertEquals(370, card.progress)
        assertEquals(1000, card.steps)
        assertEquals(0.37f, card.fraction, 1e-6f)
    }

    @Test
    fun `a locked standard achievement has no progress bar`() {
        val card = AchievementCard.of(AchievementProgress(Achievement.ZERO_WASTE))
        assertEquals(CardLook.Locked, card.look)
        assertNull(card.progress)
        assertNull(card.steps)
        assertEquals(0f, card.fraction)
    }

    @Test
    fun `an unlocked incremental achievement shows its date instead of a bar`() {
        val card = AchievementCard.of(AchievementProgress(Achievement.APPRENTICE, steps = 100, unlockedAtMs = 5L))
        assertEquals(CardLook.Unlocked, card.look)
        assertNull(card.progress)
        assertEquals(5L, card.unlockedAtMs)
    }

    @Test
    fun `progress is clamped to the steps`() {
        val card = AchievementCard.of(AchievementProgress(Achievement.APPRENTICE, steps = 250))
        assertEquals(100, card.progress)
        assertEquals(1f, card.fraction)
    }

    @Test
    fun `every hidden achievement is hidden while locked and shown once unlocked`() {
        val hidden = Achievement.entries.filter { it.hidden }
        assertEquals(listOf(Achievement.INSTANT_FLOOD, Achievement.WALL_TO_WALL, Achievement.PHOTO_FINISH), hidden)
        for (achievement in hidden) {
            assertEquals(CardLook.HiddenLocked, AchievementCard.of(AchievementProgress(achievement)).look)
            assertEquals(CardLook.Unlocked, AchievementCard.of(AchievementProgress(achievement, unlockedAtMs = 1L)).look)
        }
    }

    @Test
    fun `every catalogue entry maps to the look its state asks for`() {
        for (achievement in Achievement.entries) {
            val locked = AchievementCard.of(AchievementProgress(achievement, steps = 1))
            val expected = if (achievement.hidden) CardLook.HiddenLocked else CardLook.Locked
            assertEquals(achievement.key, expected, locked.look)
            assertEquals(achievement.key, achievement.type is AchievementType.Incremental && !achievement.hidden, locked.progress != null)
        }
    }

    @Test
    fun `the date is the medium date in the given zone`() {
        // 2026-10-02T12:00:00Z
        assertEquals("2 Oct 2026", AchievementCardText.date(1_790_942_400_000, Locale.UK, ZoneOffset.UTC))
    }

    @Test
    fun `a screen reader hears name, state and progress first`() {
        assertEquals(
            "Journeyman, locked, 370 of 1000, Place 1,000 sections.",
            AchievementCardText.spoken("Journeyman", "locked", "370 of 1000", "Place 1,000 sections.", null),
        )
        assertEquals(
            "Photo Finish, unlocked, Complete the route., Unlocked 2 Oct 2026",
            AchievementCardText.spoken("Photo Finish", "unlocked", null, "Complete the route.", "Unlocked 2 Oct 2026"),
        )
    }
}
