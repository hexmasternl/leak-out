package nl.hexmaster.leakout.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.InMemoryAchievementRepository
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.history.InMemoryLevelHistoryRepository
import nl.hexmaster.leakout.history.LevelHistory
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Play Games panel of the settings screen (settings-screen delta spec, tasks 7.1-7.5). */
@RunWith(AndroidJUnit4::class)
class SettingsPlayGamesTest {
    @get:Rule
    val compose = createComposeRule()

    private val signIn = MutableStateFlow<SignInState>(SignInState.SignedOut)
    private var signIns = 0
    private var opened = 0
    private lateinit var store: InMemoryAchievementRepository

    private fun unlocked(count: Int) = Achievement.entries.take(count).map { AchievementProgress(it, unlockedAtMs = 1L) }

    private fun showScreen(state: SignInState, unlocked: Int = 0, records: List<LevelHistory> = emptyList()) {
        signIn.value = state
        store = InMemoryAchievementRepository(initial = unlocked(unlocked))
        val viewModel = SettingsViewModel(
            InMemorySettingsRepository(),
            InMemoryLevelHistoryRepository(records),
            achievements = store,
            signIn = signIn,
        )
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp)) {
                    SettingsScreen(
                        viewModel = viewModel,
                        appInfo = AppInfo("0.1.0", 1),
                        onBack = {},
                        onSignIn = {
                            signIns++
                            // As PlayGamesAuth does: the state is SigningIn until the sign-in finishes.
                            signIn.value = SignInState.SigningIn
                        },
                        onOpenAchievements = { opened++ },
                    )
                }
            }
        }
    }

    private fun status() = compose.onNodeWithTag(PlayGamesPanelTags.STATUS).performScrollTo()

    private fun signInButton() = compose.onNodeWithTag(PlayGamesPanelTags.SIGN_IN)

    @Test
    fun thePanelIsAtTheTopOfTheRightColumn() {
        showScreen(SignInState.SignedOut)
        val panel = compose.onNodeWithTag(PlayGamesPanelTags.PANEL).assertIsDisplayed().getUnclippedBoundsInRoot()
        val progress = compose.onNodeWithTag(SettingsTags.PROGRESS_PANEL).getUnclippedBoundsInRoot()
        val about = compose.onNodeWithTag(SettingsTags.ABOUT_PANEL).getUnclippedBoundsInRoot()
        val audio = compose.onNodeWithTag(SettingsTags.AUDIO_PANEL).getUnclippedBoundsInRoot()
        assertTrue("Play Games, then Progress, then About", panel.bottom <= progress.top && progress.bottom <= about.top)
        assertTrue("in the right column", panel.left > audio.right)
        compose.onNodeWithText("PLAY GAMES").assertIsDisplayed()
    }

    @Test
    fun signedOutShowsNotSignedInAndAnEnabledSignInButton() {
        showScreen(SignInState.SignedOut)
        status().assertTextEquals("Not signed in")
        signInButton().performScrollTo().assertIsDisplayed().assertIsEnabled().assertTextEquals("Sign in").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun tappingSignInStartsTheSignInAndDisablesTheButtonUntilItFinishes() {
        showScreen(SignInState.SignedOut)
        signInButton().performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, signIns) }
        signInButton().assertIsDisplayed().assertIsNotEnabled()
        // A second tap while signing in starts nothing.
        signInButton().performClick()
        compose.runOnIdle { assertEquals(1, signIns) }

        signIn.value = SignInState.SignedIn("Eduard")
        compose.waitForIdle()
        status().assertTextEquals("Signed in as Eduard")
        signInButton().assertDoesNotExist()
    }

    @Test
    fun signingInShowsADisabledSignInButton() {
        showScreen(SignInState.SigningIn)
        signInButton().performScrollTo().assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun signedInShowsTheNameAndNoSignInButton() {
        showScreen(SignInState.SignedIn("Eduard"))
        status().assertTextEquals("Signed in as Eduard")
        signInButton().assertDoesNotExist()
    }

    @Test
    fun unavailableSaysSoAndHasNoSignInButton() {
        showScreen(SignInState.Unavailable)
        status().assertTextEquals("Play Games is not available")
        signInButton().assertDoesNotExist()
    }

    @Test
    fun theAchievementsButtonShowsTheCountInEveryState() {
        showScreen(SignInState.SignedOut, unlocked = 12)
        for (state in listOf(SignInState.SignedOut, SignInState.SigningIn, SignInState.SignedIn("Eduard"), SignInState.Unavailable)) {
            signIn.value = state
            compose.waitForIdle()
            compose.onNodeWithTag(PlayGamesPanelTags.ACHIEVEMENTS)
                .performScrollTo()
                .assertIsDisplayed()
                .assertIsEnabled()
                .assert(hasText("Achievements"))
                .assert(hasText("12 / 25"))
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun tappingAchievementsWhileSignedOutOpensTheAchievementsScreen() {
        showScreen(SignInState.SignedOut, unlocked = 3)
        compose.onNodeWithTag(PlayGamesPanelTags.ACHIEVEMENTS).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun achievementsSurviveAReset() {
        showScreen(SignInState.SignedOut, unlocked = 9, records = listOf(LevelHistory(1, 100, 3)))
        compose.onNodeWithTag(PlayGamesPanelTags.ACHIEVEMENTS).performScrollTo().assert(hasText("9 / 25"))
        compose.onNodeWithTag(SettingsTags.RESET_PROGRESS).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTags.RESET_CONFIRM).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(SettingsTags.PROGRESS_SUMMARY).assertTextEquals("No levels finished yet")
        compose.onNodeWithTag(PlayGamesPanelTags.ACHIEVEMENTS).performScrollTo().assert(hasText("9 / 25"))
    }
}
