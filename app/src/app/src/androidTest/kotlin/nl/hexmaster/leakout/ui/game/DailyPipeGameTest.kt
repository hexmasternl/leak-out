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
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.core.engine.GameEngine
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.LevelWriter
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.scores.online.DailyBestScore
import nl.hexmaster.leakout.scores.online.DailyLeaderboardEntry
import nl.hexmaster.leakout.scores.online.DailyPipe
import nl.hexmaster.leakout.scores.online.DailyPipeDownload
import nl.hexmaster.leakout.scores.online.DailyPipes
import nl.hexmaster.leakout.scores.online.DailyPlayerRank
import nl.hexmaster.leakout.scores.online.DailyScoreResult
import nl.hexmaster.leakout.scores.online.DailyStreak
import nl.hexmaster.leakout.scores.online.DailySubmission
import nl.hexmaster.leakout.scores.online.DailyTopResult
import nl.hexmaster.leakout.scores.online.DailyTopScores
import nl.hexmaster.leakout.ui.daily.DailyLeaderboardTags
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
 * button downloads the pipe, which opens paused behind the day's leaderboard; the game deals the
 * seeded stack, and a win shows the result with only Next, which leads to the leaderboard again
 * with Restart and Exit.
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
    private var exits = 0

    private val topFive = listOf("Player v" to 512, "Player w" to 511, "Player x" to 501, "Player y" to 498, "Player z" to 495)
        .mapIndexed { index, (name, score) -> DailyLeaderboardEntry(index + 1, name, score, 3, isCurrentPlayer = false) }

    @Test
    fun theButtonOpensTheDownloadedPipePausedBehindTheLeaderboard() {
        server.download = DailyPipeDownload.Available(DailyPipe(date, LevelWriter.write(level), 4242L, streak.copy(wonToday = false), null))
        server.top = DailyTopResult.Loaded(DailyTopScores(date, topFive, DailyPlayerRank(position = 76, score = 125, stars = 1)))
        compose.mainClock.autoAdvance = false
        showWelcomeThenGame()

        compose.onNodeWithTag(WelcomeScreenTags.DAILY_PIPE).performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertExists()
        compose.onNodeWithTag(HudTags.LEVEL).assertContentDescriptionEquals("Daily Pipe")
        compose.runOnIdle {
            assertEquals("dealt from the server's seed", GameEngine.seeded(4242L).start(level).stack, session.state.stack)
        }
        compose.onNodeWithTag(DailyLeaderboardTags.DIALOG).assertExists()
        compose.onNodeWithTag(DailyLeaderboardTags.row(1), useUnmergedTree = true).assertContentDescriptionEquals("Position 1, Player v, 512 points")
        compose.onNodeWithTag(DailyLeaderboardTags.row(76), useUnmergedTree = true)
            .assertContentDescriptionEquals("Position 76, Ann, you, 125 points")

        compose.mainClock.advanceTimeBy(3_000)
        compose.runOnIdle { assertEquals("nothing runs before Start", 0L, session.state.clockMs) }

        compose.onNodeWithTag(DailyLeaderboardTags.START).performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag(DailyLeaderboardTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertTrue("the countdown runs after Start", session.state.clockMs > 0) }
    }

    @Test
    fun aLeaderboardThatCannotBeLoadedStillLetsThePlayerStart() {
        server.top = DailyTopResult.Failed
        showGame(start = false)

        compose.onNodeWithTag(DailyLeaderboardTags.FAILED, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(DailyLeaderboardTags.START).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(DailyLeaderboardTags.DIALOG).assertDoesNotExist()
    }

    @Test
    fun anEmptyDayShowsThatThereAreNoScoresYet() {
        server.top = DailyTopResult.Loaded(DailyTopScores(date, emptyList(), player = null))
        showGame(start = false)

        compose.onNodeWithTag(DailyLeaderboardTags.EMPTY, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun backOnTheStartingLeaderboardLeavesTheGame() {
        showGame(start = false)
        compose.onNodeWithTag(DailyLeaderboardTags.DIALOG).assertIsDisplayed()

        Espresso.pressBack()
        compose.waitForIdle()

        assertEquals(1, exits)
    }

    @Test
    fun aWinShowsOnlyNextWhichLeadsToTheLeaderboardWithRestartAndExit() {
        server.submission = DailySubmission.Accepted(DailyScoreResult(date, DailyBestScore(60, 2), isNewBest = true, rank = 2, streak = streak))
        showGame()
        val before = server.topRequests
        // After the win the server has the new result.
        server.top = DailyTopResult.Loaded(
            DailyTopScores(date, listOf(topFive[0], DailyLeaderboardEntry(2, "Ann", 60, 2, isCurrentPlayer = true)), DailyPlayerRank(2, 60, 2)),
        )

        winAndShowDialog()

        compose.runOnIdle { assertEquals(listOf(Triple(date, 60, 2)), server.submitted) }
        compose.onNodeWithTag(DailyResultTags.STREAK, useUnmergedTree = true).assertTextContains("4 day streak")
        compose.onNodeWithTag(DailyResultTags.RANK, useUnmergedTree = true).assertTextContains("You are #2 today")
        compose.onNodeWithTag(LevelCompleteTags.NEXT).assertIsDisplayed()
        compose.onNodeWithTag(LevelCompleteTags.RESTART).assertDoesNotExist()
        compose.onNodeWithTag(LevelCompleteTags.EXIT).assertDoesNotExist()
        compose.onNodeWithTag(LevelCompleteTags.TOP10).assertDoesNotExist()

        compose.onNodeWithTag(LevelCompleteTags.NEXT).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(DailyLeaderboardTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(DailyLeaderboardTags.row(2), useUnmergedTree = true).assertContentDescriptionEquals("Position 2, Ann, you, 60 points")
        compose.onNodeWithTag(DailyLeaderboardTags.RESTART).assertIsDisplayed()
        compose.onNodeWithTag(DailyLeaderboardTags.START).assertDoesNotExist()
        compose.runOnIdle { assertEquals("loaded again after the win", before + 1, server.topRequests) }

        compose.onNodeWithTag(DailyLeaderboardTags.EXIT).performClick()
        compose.waitForIdle()
        assertEquals(1, exits)
    }

    @Test
    fun restartOnTheLeaderboardAfterAWinOpensTheSamePipePausedAgain() {
        server.submission = DailySubmission.Accepted(DailyScoreResult(date, DailyBestScore(60, 2), true, 1, streak))
        showGame()
        val first = compose.runOnIdle { session.state.stack }
        winAndShowDialog()
        compose.onNodeWithTag(LevelCompleteTags.NEXT).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(DailyLeaderboardTags.RESTART).performClick()
        compose.waitForIdle()

        compose.runOnIdle {
            assertEquals(Outcome.Playing, session.state.outcome)
            assertEquals("the same stack again", first, session.state.stack)
        }
        compose.onNodeWithTag(DailyLeaderboardTags.START).assertIsDisplayed()
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

        server.submission = DailySubmission.Accepted(DailyScoreResult(date, DailyBestScore(60, 2), true, 1, streak))
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

    /** The game screen as `MainActivity` shows a daily pipe; with [start] the starting leaderboard is closed with Start. */
    private fun showGame(start: Boolean = true) {
        session = GameSession(level, GameEngine.seeded(4242L))
        compose.setContent { LeakOutTheme { DailyGame(DailyPipeGame(date, level, 4242L, streak)) } }
        compose.waitForIdle()
        if (start) {
            compose.onNodeWithTag(DailyLeaderboardTags.START).performClick()
            compose.waitForIdle()
        }
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
        compose.mainClock.advanceTimeBy(500)
    }

    @Composable
    private fun DailyGame(game: DailyPipeGame) {
        Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
            GameScreen(
                level = game.level,
                onExit = { exits++ },
                session = session,
                levelLabel = "Daily Pipe",
                daily = remember { DailyResults.of(server, game.date) { "Ann" } },
            )
        }
    }

    private class FakeDailyServer : DailyPipes {
        var download: DailyPipeDownload = DailyPipeDownload.Failed
        var submission: DailySubmission = DailySubmission.Failed
        var top: DailyTopResult = DailyTopResult.Loaded(DailyTopScores(LocalDate.of(2026, 11, 2), emptyList(), player = null))
        val submitted = mutableListOf<Triple<LocalDate, Int, Int>>()
        var topRequests = 0

        override suspend fun today(): DailyPipeDownload = download

        override suspend fun submit(date: LocalDate, score: Int, stars: Int): DailySubmission {
            submitted += Triple(date, score, stars)
            return submission
        }

        override suspend fun top(date: LocalDate): DailyTopResult {
            topRequests++
            return top
        }
    }
}
