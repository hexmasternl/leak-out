package nl.hexmaster.leakout.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.history.InMemoryLevelHistoryRepository
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.game.FieldTest
import nl.hexmaster.leakout.ui.game.GameScreen
import nl.hexmaster.leakout.ui.game.onboarding.TipOverlayTags
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Show tips again button on the Progress panel (settings-screen). */
@RunWith(AndroidJUnit4::class)
class SettingsShowTipsAgainTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var settings: InMemorySettingsRepository

    private fun showScreen(seen: Set<TipId>) {
        settings = InMemorySettingsRepository(initialSeenTips = seen)
        val viewModel = SettingsViewModel(settings, InMemoryLevelHistoryRepository())
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp)) { SettingsScreen(viewModel = viewModel, appInfo = AppInfo("0.1.0", 1), onBack = {}) }
            }
        }
        // The panels appear once the settings and the history have been read.
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(SettingsTags.SHOW_TIPS_AGAIN).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun button() = compose.onNodeWithTag(SettingsTags.SHOW_TIPS_AGAIN).performScrollTo()

    @Test
    fun showTipsAgainResetsTheTips() {
        showScreen(setOf(TipId.Intro, TipId.Cross))
        button().performClick()
        compose.waitForIdle()
        assertEquals(emptySet<TipId>(), settings.seenTips.value)
        button().assertIsNotEnabled()
        compose.onNodeWithText("Tips will show again when you play").assertIsDisplayed()
    }

    @Test
    fun theButtonIsDisabledWhenNoTipWasSeen() {
        showScreen(emptySet())
        button().assertIsNotEnabled()
    }

    @Test
    fun theButtonIsReportedAsAnEnabledButton() {
        showScreen(setOf(TipId.Intro))
        button().assertIsEnabled().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button))
    }

    @Test
    fun aLevelStartedAfterTheResetShowsTheTour() {
        settings = InMemorySettingsRepository(initialSeenTips = setOf(TipId.Intro))
        val viewModel = SettingsViewModel(settings, InMemoryLevelHistoryRepository())
        var playing by mutableStateOf(false)
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    if (playing) {
                        GameScreen(level = FieldTest.level(), onExit = {}, random = Random(7), tips = settings)
                    } else {
                        SettingsScreen(viewModel = viewModel, appInfo = AppInfo("0.1.0", 1), onBack = {})
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(SettingsTags.SHOW_TIPS_AGAIN).performScrollTo().performClick()
        compose.waitForIdle()
        playing = true
        compose.waitForIdle()
        compose.onNodeWithTag(TipOverlayTags.OVERLAY).assertIsDisplayed()
    }
}
