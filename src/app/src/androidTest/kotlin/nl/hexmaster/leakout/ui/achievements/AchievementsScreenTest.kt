package nl.hexmaster.leakout.ui.achievements

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.InMemoryAchievementRepository
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The achievements screen (achievements-screen spec, tasks 8.1-8.7). */
@RunWith(AndroidJUnit4::class)
class AchievementsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val signIn = MutableStateFlow<SignInState>(SignInState.SignedOut)
    private var backs = 0
    private var playGamesOpened = 0

    // 2026-10-02T12:00:00Z
    private val unlockedAt = 1_790_942_400_000L

    private fun show(progress: List<AchievementProgress> = emptyList(), state: SignInState = SignInState.SignedOut) {
        signIn.value = state
        val viewModel = AchievementsViewModel(InMemoryAchievementRepository(initial = progress), signIn)
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp)) {
                    AchievementsScreen(viewModel, onBack = { backs++ }, onOpenPlayGames = { playGamesOpened++ })
                }
            }
        }
        compose.waitForIdle()
    }

    private fun card(achievement: Achievement) = run {
        compose.onNodeWithTag(AchievementsTags.GRID).performScrollToKey(achievement.key)
        compose.onNodeWithTag(achievementCardTag(achievement))
    }

    @Test
    fun theTitleAndTheCountAreShown() {
        show(Achievement.entries.take(7).map { AchievementProgress(it, unlockedAtMs = unlockedAt) })
        compose.onNodeWithTag(AchievementsTags.TITLE).assertIsDisplayed().assertTextEquals("ACHIEVEMENTS")
        compose.onNodeWithTag(AchievementsTags.COUNT).assertIsDisplayed()
            .assertTextEquals("7 / 39")
            .assertContentDescriptionEquals("7 of 39 achievements unlocked")
    }

    @Test
    fun everyAchievementIsListedInCatalogueOrderWhileSignedOut() {
        show(state = SignInState.SignedOut)
        // The grid scrolls to each one in turn; the order itself is pinned by AchievementsViewModelTest.
        for (achievement in Achievement.entries) card(achievement).assertIsDisplayed()
        // Back at the top, the first row holds the first two side by side.
        val first = card(Achievement.entries.first()).fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag(achievementCardTag(Achievement.entries[1])).fetchSemanticsNode().boundsInRoot
        assertTrue("catalogue order runs left to right", second.left > first.left || second.top > first.top)
    }

    @Test
    fun aLockedIncrementalAchievementShowsItsProgress() {
        show(listOf(AchievementProgress(Achievement.JOURNEYMAN, steps = 370)))
        card(Achievement.JOURNEYMAN)
            .assertIsDisplayed()
            .assert(hasText("Journeyman"))
            .assert(hasText("Place 1,000 sections."))
            .assert(hasText("370 / 1000"))
        compose.onNode(
            hasTestTag(AchievementsTags.PROGRESS_BAR) and hasAnyAncestor(hasTestTag(achievementCardTag(Achievement.JOURNEYMAN))),
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun aScreenReaderReadsACardAsOneItem() {
        show(listOf(AchievementProgress(Achievement.JOURNEYMAN, steps = 370)))
        card(Achievement.JOURNEYMAN).assertContentDescriptionEquals("Journeyman, locked, 370 of 1000, Place 1,000 sections.")
    }

    @Test
    fun aLockedStandardAchievementHasNoProgress() {
        show()
        card(Achievement.ZERO_WASTE)
            .assert(hasText("Zero Waste"))
            .assert(hasText("Win a level with no unused sections."))
            .assertContentDescriptionEquals("Zero Waste, locked, Win a level with no unused sections.")
        compose.onNode(
            hasTestTag(AchievementsTags.PROGRESS_BAR) and hasAnyAncestor(hasTestTag(achievementCardTag(Achievement.ZERO_WASTE))),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun aLockedHiddenAchievementKeepsItsSecret() {
        show()
        card(Achievement.PHOTO_FINISH)
            .assertIsDisplayed()
            .assert(hasText("Hidden achievement"))
            .assert(hasText("Keep playing to find out."))
            .assert(hasText("Photo Finish").not())
            .assert(hasContentDescription("Photo Finish", substring = true).not())
            .assertContentDescriptionEquals("Hidden achievement, locked, Keep playing to find out.")
    }

    @Test
    fun anUnlockedHiddenAchievementShowsNameDescriptionAndDate() {
        show(listOf(AchievementProgress(Achievement.PHOTO_FINISH, unlockedAtMs = unlockedAt)))
        val date = AchievementCardText.date(unlockedAt, Locale.getDefault())
        card(Achievement.PHOTO_FINISH)
            .assert(hasText("Photo Finish"))
            .assert(hasText("Complete the route on the very tile the liquid was about to enter."))
            .assert(hasText("Unlocked $date"))
            .assert(hasText("Hidden achievement").not())
            .assert(hasContentDescription("Photo Finish, unlocked", substring = true))
    }

    @Test
    fun signedInShowsThePlayGamesButtonThatOpensPlayGames() {
        show(state = SignInState.SignedIn("Eduard"))
        compose.onNodeWithTag(AchievementsTags.PLAY_GAMES)
            .assertIsDisplayed()
            .assertTextEquals("Play Games")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(1, playGamesOpened) }
    }

    @Test
    fun withoutSignInThereIsNoPlayGamesButton() {
        show(state = SignInState.SignedOut)
        compose.onNodeWithTag(AchievementsTags.PLAY_GAMES).assertDoesNotExist()
        signIn.value = SignInState.Unavailable
        compose.waitForIdle()
        compose.onNodeWithTag(AchievementsTags.PLAY_GAMES).assertDoesNotExist()
        signIn.value = SignInState.SignedIn("Eduard")
        compose.waitForIdle()
        compose.onNodeWithTag(AchievementsTags.PLAY_GAMES).assertIsDisplayed()
    }

    @Test
    fun theBackButtonLeaves() {
        show()
        compose.onNodeWithTag(AchievementsTags.BACK)
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(1, backs) }
    }

    @Test
    fun systemBackLeaves() {
        show()
        Espresso.pressBack()
        compose.runOnIdle { assertEquals(1, backs) }
    }
}
