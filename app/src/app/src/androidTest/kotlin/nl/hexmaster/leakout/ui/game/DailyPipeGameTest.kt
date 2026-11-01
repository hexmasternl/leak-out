package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.core.engine.GameEngine
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.LevelWriter
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.scores.online.DailyBestScore
import nl.hexmaster.leakout.scores.online.DailyLeaderboardEntry
import nl.hexmaster.leakout.scores.online.DailyPipe
import nl.hexmaster.leakout.scores.online.DailyPipeDownload
import nl.hexmaster.leakout.scores.online.DailyPipes
import nl.hexmaster.leakout.scores.online.DailyScoreResult
import nl.hexmaster.leakout.scores.online.DailyStreak
import nl.hexmaster.leakout.scores.online.DailySubmission
import nl.hexmaster.leakout.scores.online.DailyTopScores
import nl.hexmaster.leakout.ui.daily.DailyPipeGame
import nl.hexmaster.leakout.ui.daily.DailyPipeViewModel
import nl.hexmaster.leakout.ui.daily.DailyResultTags
import nl.hexmaster.leakout.ui.daily.DailyResults
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import nl.hexmaster.leakout.ui.title.WelcomeScreen
import nl.hexmaster.leakout.ui.title.WelcomeScreenTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opening and winning the Daily Pipe against a fake server (spec `daily-pipe`): the welcome screen's
 * button downloads the pipe, the game deals the seeded stack, and the win's result, streak and top
 * scores show in the level complete dialog.
 */
@RunWith(AndroidJUnit4::class)
class DailyPipeGameTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val date = LocalDate.of(2026, 11, 2)
    // With every standard section on the stack, so the downloaded level passes LevelValidator.
    private val level = FieldTest.level(id = 20261102, stars = StarBenchmarks(20, 40, 200)).copy(stack = StackSettings())
    private val streak = DailyStreak(current = 4, best = 7, wonToday = true)
    private val server = FakeDailyServer()
    private lateinit var session: GameSession

    @Test
    fun theButtonOpensTheDownloadedPipeAndAWinShowsStreakRankAndTopScores() {
        server.download = DailyPipeDownload.Available(DailyPipe(date, LevelWriter.write(level), 4242L, streak.copy(wonToday = false), null))
        server.submission = DailySubmission.Accepted(
            DailyScoreResult(date, DailyBestScore(60, 2), isNewBest = true, rank = 2, streak = streak),
            DailyTopScores(
                date,
                listOf(DailyLeaderboardEntry(1, "Ada", 90, 3, false), DailyLeaderboardEntry(2, "Me", 60, 2, true)),
                playerPosition = 2,
            ),
        )
        showWelcomeThenGame()

        compose.onNodeWithTag(WelcomeScreenTags.DAILY_PIPE).performClick()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(HudTags.LEVEL).assertContentDescriptionEquals("Daily Pipe")
        compose.runOnIdle {
            assertEquals("dealt from the server's seed", GameEngine.seeded(4242L).start(level).stack, session.state.stack)
        }

        winAndShowDialog()

        compose.runOnIdle { assertEquals(listOf(Triple(date, 60, 2)), server.submitted) }
        compose.onNodeWithTag(DailyResultTags.STREAK, useUnmergedTree = true).assertTextContains("4 day streak")
        compose.onNodeWithTag(DailyResultTags.RANK, useUnmergedTree = true).assertTextContains("You are #2 today")
        compose.onNodeWithTag(DailyResultTags.TOP, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(DailyResultTags.TOP, useUnmergedTree = true).onChildren().assertAny(hasText("1. Ada · 90"))
    }

    @Test
    fun aWinAfterTheDayEndedSaysSo() {
        server.submission = DailySubmission.Ended
        showGame()

        winAndShowDialog()

        compose.onNodeWithTag(DailyResultTags.ENDED, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(DailyResultTags.STREAK, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun aFailedSubmissionCanBeTriedAgain() {
        server.submission = DailySubmission.Failed
        showGame()
        winAndShowDialog()
        compose.onNodeWithTag(DailyResultTags.FAILED, useUnmergedTree = true).assertIsDisplayed()

        server.submission = DailySubmission.Accepted(DailyScoreResult(date, DailyBestScore(60, 2), true, 1, streak), null)
        compose.onNodeWithTag(DailyResultTags.RETRY, useUnmergedTree = true).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(DailyResultTags.STREAK, useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, server.submitted.size) }
    }

    @Test
    fun aRestartDealsTheSameStackAgain() {
        showGame()
        val first = compose.runOnIdle { session.state.stack }

        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE.take(2)) session.placeSection(section, at)
            assertTrue(session.restart(null))
        }
        compose.waitForIdle()

        compose.runOnIdle { assertEquals(first, session.state.stack) }
    }

    private fun winAndShowDialog() {
        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) assertTrue(session.placeSection(section, at))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
        compose.waitForIdle()
        val end = compose.runOnIdle { WinTally.totalDurationMs(session.state.finalScore!!, session.state.finalStars) }
        compose.runOnIdle { session.sendTick(WinTiming.FINISH_EFFECT_MS + end) }
        compose.waitForIdle()
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
    }

    /** The game screen as `MainActivity` shows a daily pipe. */
    private fun showGame() {
        session = GameSession(level, GameEngine.seeded(4242L))
        compose.setContent { LeakOutTheme { DailyGame(DailyPipeGame(date, level, 4242L, streak)) } }
        compose.waitForIdle()
    }

    /** The welcome screen with a signed-in, online player, which opens the game like `MainActivity`. */
    private fun showWelcomeThenGame() {
        val viewModel = DailyPipeViewModel(server, MutableStateFlow(SignInState.SignedIn("Ann")), MutableStateFlow(true))
        compose.setContent {
            LeakOutTheme {
                var game by remember { mutableStateOf<DailyPipeGame?>(null) }
                val opened by viewModel.opened.collectAsState()
                LaunchedEffect(opened) {
                    val ready = opened ?: return@LaunchedEffect
                    viewModel.consumeOpened()
                    session = GameSession(ready.level, GameEngine.seeded(ready.stackSeed))
                    game = ready
                }
                val current = game
                if (current == null) {
                    val entry by viewModel.state.collectAsState()
                    WelcomeScreen(
                        resolveNextLevel = { error("not used") },
                        onStartLevel = {},
                        onOpenLevelLibrary = {},
                        onSettings = {},
                        onExit = {},
                        dailyPipe = entry,
                        onDailyPipe = viewModel::open,
                    )
                } else {
                    DailyGame(current)
                }
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun DailyGame(game: DailyPipeGame) {
        Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
            GameScreen(
                level = game.level,
                onExit = {},
                session = session,
                levelLabel = "Daily Pipe",
                daily = remember { DailyResults { score, stars -> server.submit(game.date, score, stars) } },
            )
        }
    }

    private class FakeDailyServer : DailyPipes {
        var download: DailyPipeDownload = DailyPipeDownload.Failed
        var submission: DailySubmission = DailySubmission.Failed
        val submitted = mutableListOf<Triple<LocalDate, Int, Int>>()

        override suspend fun today(): DailyPipeDownload = download

        override suspend fun submit(date: LocalDate, score: Int, stars: Int): DailySubmission {
            submitted += Triple(date, score, stars)
            return submission
        }
    }
}
