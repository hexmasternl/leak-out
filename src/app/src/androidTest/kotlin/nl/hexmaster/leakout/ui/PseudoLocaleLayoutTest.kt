package nl.hexmaster.leakout.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.audio.BundledMusic
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.library.LevelLibraryConfig
import nl.hexmaster.leakout.core.library.NextLevel
import nl.hexmaster.leakout.core.score.FinalScore
import nl.hexmaster.leakout.history.InMemoryLevelHistoryRepository
import nl.hexmaster.leakout.history.LevelHistory
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.ui.game.LevelCompleteDialog
import nl.hexmaster.leakout.ui.game.LevelLostDialog
import nl.hexmaster.leakout.ui.game.PauseDialogContent
import nl.hexmaster.leakout.ui.game.PauseDialogEntry
import nl.hexmaster.leakout.ui.game.WinTally
import nl.hexmaster.leakout.ui.game.winFrame
import nl.hexmaster.leakout.ui.levels.LevelLibraryScreen
import nl.hexmaster.leakout.ui.levels.LevelLibraryViewModel
import nl.hexmaster.leakout.ui.settings.AppInfo
import nl.hexmaster.leakout.ui.settings.SettingsScreen
import nl.hexmaster.leakout.ui.settings.SettingsViewModel
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import nl.hexmaster.leakout.ui.title.WelcomeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Translations run up to 40% longer than English. The `en-XA` pseudo-locale (debug builds only) makes
 * every string about that much longer and accented, so these tests show the smallest supported
 * screen, 640 × 360, with it and fail on any text that does not fit
 * (openspec/specs/app-localization, "Layouts fit translated text").
 */
@RunWith(AndroidJUnit4::class)
class PseudoLocaleLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            PseudoLocale {
                LeakOutTheme {
                    // The box stands for the safe area of the smallest screen, so device insets must not shrink it further.
                    Box(Modifier.size(640.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) { content() }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun pseudoLocaleIsActive() {
        show { WelcomeScreen(resolveNextLevel = { NEXT_LEVEL }, onStartLevel = {}, onOpenLevelLibrary = {}, onSettings = {}, onExit = {}) }
        // en-XA wraps every string in brackets; a missing debug resource set would leave plain English.
        assertTrue("en-XA resources missing: is pseudoLocalesEnabled on for debug?", texts().any { it.startsWith("[") })
    }

    @Test
    fun welcomeScreenFits() {
        show { WelcomeScreen(resolveNextLevel = { NEXT_LEVEL }, onStartLevel = {}, onOpenLevelLibrary = {}, onSettings = {}, onExit = {}) }
        assertNoTextOverflows()
    }

    @Test
    fun settingsScreenFits() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = SettingsViewModel(
            InMemorySettingsRepository(),
            InMemoryLevelHistoryRepository((1..10).map { LevelHistory(it, 100, 3) }),
            BundledMusic.load(context.assets),
        )
        show {
            SettingsScreen(viewModel = viewModel, appInfo = AppInfo("0.1.0", 1), onBack = {}, onSoundPreview = {}, onMusicVolumePreview = {})
        }
        assertNoTextOverflows()
    }

    @Test
    fun levelLibraryFits() {
        val history = MutableStateFlow(mapOf(1 to 3, 2 to 2))
        val viewModel = LevelLibraryViewModel(history = { history }, catalog = { 48 }, config = LevelLibraryConfig.Default)
        show { LevelLibraryScreen(viewModel, onStartLevel = {}, onBack = {}) }
        assertNoTextOverflows()
    }

    @Test
    fun pauseDialogFits() {
        show { PauseDialogContent({}, {}, {}, entry = PauseDialogEntry(0, 0, 1f, null)) }
        assertNoTextOverflows()
    }

    @Test
    fun levelCompleteDialogFits() {
        val score = FinalScore(earned = 120, unusedSections = List(4) { Position(it, 0) }, total = 108)
        show {
            LevelCompleteDialog(
                level = 7,
                score = score,
                frame = winFrame(WinTally.totalDurationMs(score, 2), score, stars = 2),
                hasNextLevel = true,
                onNextLevel = {},
                onRestart = {},
                onExit = {},
                newBest = true,
            )
        }
        assertNoTextOverflows()
    }

    @Test
    fun levelLostDialogFits() {
        show { LevelLostDialog(level = 7, score = 85, onRestart = {}, onExit = {}) }
        assertNoTextOverflows()
    }

    private fun texts(): List<String> =
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes().flatMap { node -> node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }

    /** Every text on screen, in every window (dialogs too), is laid out without being cut off. */
    private fun assertNoTextOverflows() {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("No text found on screen", nodes.isNotEmpty())
        val clipped = nodes.mapNotNull { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            layouts.firstOrNull()?.takeIf { it.hasVisualOverflow }?.layoutInput?.text?.text
        }
        assertEquals("Text cut off under en-XA at 640 × 360", emptyList<String>(), clipped)
    }

    private companion object {
        val NEXT_LEVEL = NextLevel(levelId = 1, page = 1, isUnlocked = true, highestCompleted = 0)
    }
}

/** Shows [content] with the `en-XA` pseudo-locale's resources, the way Android would with that device language. */
@Composable
private fun PseudoLocale(content: @Composable () -> Unit) {
    val base = LocalContext.current
    val configuration = remember(base) {
        Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag("en-XA")) }
    }
    val context = remember(base, configuration) { base.createConfigurationContext(configuration) }
    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides configuration,
        LocalResources provides context.resources,
    ) { content() }
}
