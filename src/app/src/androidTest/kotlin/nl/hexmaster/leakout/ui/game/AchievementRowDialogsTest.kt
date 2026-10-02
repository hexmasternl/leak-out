package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.score.FinalScore
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The "Achievement unlocked" row in the level complete and level lost dialogs (spec 5.1-5.4). */
@RunWith(AndroidJUnit4::class)
class AchievementRowDialogsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val score = FinalScore(earned = 120, unusedSections = List(4) { Position(it, 0) }, total = 108)
    private val tallyDone = WinTally.totalDurationMs(score, 2)
    private val four = listOf(Achievement.ZERO_WASTE, Achievement.MEASURE_TWICE, Achievement.STEADY_HANDS, Achievement.PERSONAL_BEST)

    @Test
    fun winWithOneAchievementShowsItsName() {
        showWin(tallyDone, listOf(Achievement.ZERO_WASTE))
        compose.onNodeWithTag(LevelCompleteTags.ACHIEVEMENTS)
            .assertIsDisplayed()
            .assert(hasText("ACHIEVEMENT UNLOCKED"))
            .assert(hasText("Zero Waste"))
            .assertContentDescriptionEquals("Achievement unlocked: Zero Waste")
    }

    @Test
    fun winWithFourAchievementsShowsTwoNamesAndTwoMoreButReadsThemAll() {
        showWin(tallyDone, four)
        compose.onNodeWithTag(LevelCompleteTags.ACHIEVEMENTS)
            .assertIsDisplayed()
            .assert(hasText("Zero Waste · Measure Twice +2 more"))
            .assertContentDescriptionEquals("Achievement unlocked: Zero Waste, Measure Twice, Steady Hands, Personal Best")
    }

    @Test
    fun winWithoutAchievementsHasNoRow() {
        showWin(tallyDone, emptyList())
        compose.onNodeWithTag(LevelCompleteTags.TOTAL).assertIsDisplayed()
        compose.onNodeWithTag(LevelCompleteTags.ACHIEVEMENTS).assertDoesNotExist()
        compose.onNodeWithText("ACHIEVEMENT UNLOCKED").assertDoesNotExist()
    }

    @Test
    fun theWinRowWaitsForTheTallyAndTheButtonsDoNotWaitForIt() {
        var age by mutableLongStateOf(0L)
        compose.setContent { LeakOutTheme { WinDialog(age, four) } }
        compose.onNodeWithTag(LevelCompleteTags.ACHIEVEMENTS).assertDoesNotExist()
        compose.onNodeWithTag(LevelCompleteTags.NEXT).assertIsNotEnabled()

        age = tallyDone
        compose.waitForIdle()
        // Same frame: the buttons are enabled the moment the tally is done, with the row.
        compose.onNodeWithTag(LevelCompleteTags.NEXT).assertIsEnabled()
        compose.onNodeWithTag(LevelCompleteTags.ACHIEVEMENTS).assertIsDisplayed()
    }

    @Test
    fun theRowStillAppearsWhenTheTrackerAnswersLate() {
        var unlocked by mutableStateOf(emptyList<Achievement>())
        compose.setContent { LeakOutTheme { WinDialog(tallyDone, unlocked) } }
        compose.onNodeWithTag(LevelCompleteTags.ACHIEVEMENTS).assertDoesNotExist()
        compose.onNodeWithTag(LevelCompleteTags.RESTART).assertIsEnabled()

        unlocked = listOf(Achievement.ZERO_WASTE)
        compose.waitForIdle()
        compose.onNodeWithTag(LevelCompleteTags.ACHIEVEMENTS).assertIsDisplayed()
        for (tag in listOf(LevelCompleteTags.NEXT, LevelCompleteTags.RESTART, LevelCompleteTags.EXIT)) {
            compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled()
        }
    }

    @Test
    fun theRowPopsInWithMotionPop() {
        compose.mainClock.autoAdvance = false
        compose.setContent { LeakOutTheme { AchievementUnlockedRow(listOf(Achievement.WET_FEET), reducedMotion = false) } }
        compose.mainClock.advanceTimeByFrame()
        val start = rowScale()
        compose.mainClock.advanceTimeBy(1_000)
        assertTrue("the row should start small: $start", start < 1f)
        compose.onNodeWithText("Wet Feet", substring = true)
            .assert(SemanticsMatcher.expectValue(AchievementRowScale, 1f))
    }

    @Test
    fun theRowIsStillWithReducedMotion() {
        compose.mainClock.autoAdvance = false
        compose.setContent { LeakOutTheme { AchievementUnlockedRow(listOf(Achievement.WET_FEET), reducedMotion = true) } }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Wet Feet", substring = true).assert(SemanticsMatcher.expectValue(AchievementRowScale, 1f))
    }

    @Test
    fun lossWithOneAchievementShowsWetFeet() {
        showLoss(listOf(Achievement.WET_FEET))
        compose.onNodeWithTag(LevelLostTags.ACHIEVEMENTS)
            .assertIsDisplayed()
            .assert(hasText("ACHIEVEMENT UNLOCKED"))
            .assert(hasText("Wet Feet"))
            .assertContentDescriptionEquals("Achievement unlocked: Wet Feet")
        compose.onNodeWithTag(LevelLostTags.RESTART).assertIsDisplayed()
        compose.onNodeWithTag(LevelLostTags.EXIT).assertIsDisplayed()
    }

    @Test
    fun lossReadsWetFeetAndInstantFlood() {
        showLoss(listOf(Achievement.WET_FEET, Achievement.INSTANT_FLOOD))
        compose.onNodeWithTag(LevelLostTags.ACHIEVEMENTS)
            .assert(hasText("Wet Feet · Instant Flood"))
            .assertContentDescriptionEquals("Achievement unlocked: Wet Feet, Instant Flood")
    }

    @Test
    fun lossWithFourAchievementsShowsTwoMore() {
        showLoss(listOf(Achievement.WET_FEET, Achievement.INSTANT_FLOOD, Achievement.APPRENTICE, Achievement.JOURNEYMAN))
        compose.onNodeWithTag(LevelLostTags.ACHIEVEMENTS)
            .assert(hasText("Wet Feet · Instant Flood +2 more"))
            .assertContentDescriptionEquals("Achievement unlocked: Wet Feet, Instant Flood, Apprentice, Journeyman")
    }

    @Test
    fun lossWithoutAchievementsHasNoRow() {
        showLoss(emptyList())
        compose.onNodeWithTag(LevelLostTags.SCORE).assertIsDisplayed()
        compose.onNodeWithTag(LevelLostTags.ACHIEVEMENTS).assertDoesNotExist()
    }

    private fun rowScale(): Float =
        compose.onNodeWithText("Wet Feet", substring = true).fetchSemanticsNode().config[AchievementRowScale]

    private fun showWin(age: Long, unlocked: List<Achievement>) {
        compose.setContent { LeakOutTheme { WinDialog(age, unlocked) } }
        compose.waitForIdle()
    }

    private fun showLoss(unlocked: List<Achievement>) {
        compose.setContent { LeakOutTheme { LevelLostDialog(level = 7, score = 85, onRestart = {}, onExit = {}, unlocked = unlocked) } }
        compose.waitForIdle()
    }

    @androidx.compose.runtime.Composable
    private fun WinDialog(age: Long, unlocked: List<Achievement>) {
        LevelCompleteDialog(
            level = 7,
            score = score,
            frame = winFrame(age, score, stars = 2),
            hasNextLevel = true,
            onNextLevel = {},
            onRestart = {},
            onExit = {},
            unlocked = unlocked,
        )
    }
}
