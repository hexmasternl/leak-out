package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.game.FieldTest
import nl.hexmaster.leakout.ui.game.GameScreen
import nl.hexmaster.leakout.ui.game.GameScreenTags
import nl.hexmaster.leakout.ui.game.GameSession
import nl.hexmaster.leakout.ui.game.SectionDragState
import nl.hexmaster.leakout.ui.game.StackPanel
import nl.hexmaster.leakout.ui.game.PauseTags
import nl.hexmaster.leakout.ui.game.cellRect
import nl.hexmaster.leakout.ui.game.dropOnCell
import nl.hexmaster.leakout.ui.game.fieldRect
import nl.hexmaster.leakout.ui.game.placeSection
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The one-time tips on the game screen (spec `game-play-onboarding`). */
@RunWith(AndroidJUnit4::class)
class GameTipsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        level: Level = FieldTest.level(id = 4),
        session: GameSession = GameSession(level, Random(7)),
        tips: InMemorySettingsRepository = InMemorySettingsRepository(),
        width: Dp = 800.dp,
        height: Dp = 360.dp,
    ): GameSession {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(width, height).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session, tips = tips)
                }
            }
        }
        compose.waitForIdle()
        return session
    }

    private val GameSession.remainingMs: Long get() = (state.flow as FlowState.Waiting).remainingMs

    private fun shownTip(): TipId? = compose.onAllNodesWithTag(TipOverlayTags.OVERLAY).fetchSemanticsNodes()
        .firstOrNull()?.config?.getOrNull(ShownTip)

    private fun spotlights(): List<Rect> = compose.onNodeWithTag(TipOverlayTags.OVERLAY).fetchSemanticsNode().config[TipSpotlights]

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }

    /** A tap with the small moves a real finger makes, which [tap] leaves out. */
    private fun fingerTap(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performTouchInput { fingerTap() }
        compose.waitForIdle()
    }

    private fun stepShown(step: Int) = compose.onNode(hasText("$step / 5"), useUnmergedTree = true).assertIsDisplayed()

    // A Samsung Galaxy S21 in landscape: 2400 × 1080 pixels at 420 dpi.
    private val s21Width = 914.dp
    private val s21Height = 411.dp

    @Test
    fun aFingerTapOnSkipTourOnTheFirstStepClosesTheTour() {
        val tips = InMemorySettingsRepository()
        val session = show(tips = tips, width = s21Width, height = s21Height)
        fingerTap(TipOverlayTags.SKIP)
        assertEquals(null, shownTip())
        assertFalse(session.state.paused)
        assertEquals(setOf(TipId.Intro), runBlocking { tips.seenTips.first() })
    }

    @Test
    fun aFingerTapOnNextMovesToTheNextStep() {
        show(width = s21Width, height = s21Height)
        fingerTap(TipOverlayTags.NEXT)
        stepShown(2)
    }

    @Test
    fun aFingerTapOnNextWhileTheFieldPansToTheFinishStillMovesOn() {
        show(FieldTest.large(), width = s21Width, height = s21Height)
        compose.mainClock.autoAdvance = false
        try {
            // Into the finish step, which pans the field to the finish and glides the spotlight there.
            compose.onNodeWithTag(TipOverlayTags.NEXT, useUnmergedTree = true).performClick()
            compose.mainClock.advanceTimeBy(64)
            compose.onNodeWithTag(TipOverlayTags.NEXT, useUnmergedTree = true).performTouchInput { fingerTap() }
            compose.mainClock.advanceTimeBy(1_000)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        stepShown(3)
    }

    @Test
    fun theFirstLevelPlayedShowsTheTourWithTheCountdownStillFull() {
        val session = show(FieldTest.level(id = 4, initialDelayMs = 10_000))
        assertEquals(TipId.Intro, shownTip())
        compose.mainClock.advanceTimeBy(3_000)
        assertEquals(10_000L, session.remainingMs)
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.onNode(hasText("1 / 5"), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun theStartStepHighlightsTheStartTile() {
        show()
        val start = compose.cellRect(Position(0, 0))
        assertEquals(listOf(start), spotlights())
    }

    @Test
    fun theTourIsNotShownOnceSeen() {
        val session = show(tips = InMemorySettingsRepository(initialSeenTips = setOf(TipId.Intro)))
        assertEquals(null, shownTip())
        assertFalse(session.state.paused)
        assertEquals(session.state.attempt, session.tipsCheckedAttempt)
    }

    @Test
    fun finishingTheTourStoresItAndStartsTheCountdown() {
        val tips = InMemorySettingsRepository()
        val session = show(tips = tips)
        repeat(4) { tap(TipOverlayTags.NEXT) }
        compose.onNode(hasText("5 / 5"), useUnmergedTree = true).assertIsDisplayed()
        tap(TipOverlayTags.DONE)
        assertEquals(null, shownTip())
        assertFalse(session.state.paused)
        assertEquals(setOf(TipId.Intro), runBlocking { tips.seenTips.first() })
    }

    @Test
    fun skippingTheTourClosesItAndStoresIt() {
        val tips = InMemorySettingsRepository()
        val session = show(tips = tips)
        tap(TipOverlayTags.NEXT)
        tap(TipOverlayTags.NEXT)
        tap(TipOverlayTags.SKIP)
        assertEquals(null, shownTip())
        assertFalse(session.state.paused)
        assertEquals(setOf(TipId.Intro), runBlocking { tips.seenTips.first() })
    }

    @Test
    fun backSkipsTheTourAndTheScreenStays() {
        val tips = InMemorySettingsRepository()
        show(tips = tips)
        tap(TipOverlayTags.NEXT)
        Espresso.pressBack()
        compose.waitForIdle()
        assertEquals(null, shownTip())
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        assertEquals(setOf(TipId.Intro), runBlocking { tips.seenTips.first() })
    }

    @Test
    fun theFinishOfALargeLevelIsPannedIntoViewAndTheViewReturnsAfterTheTour() {
        show(FieldTest.large())
        val field = compose.fieldRect()
        val startView = compose.cellRect(Position(0, 5))
        tap(TipOverlayTags.NEXT)
        compose.mainClock.advanceTimeBy(500)
        val finish = compose.cellRect(Position(15, 6))
        assertTrue("$finish inside $field", field.contains(finish.topLeft) && field.contains(finish.bottomRight - Offset(1f, 1f)))
        assertEquals(listOf(finish), spotlights())
        tap(TipOverlayTags.SKIP)
        compose.mainClock.advanceTimeBy(500)
        assertEquals(startView, compose.cellRect(Position(0, 5)))
    }

    @Test
    fun noDragReachesTheGameDuringATip() {
        val session = show()
        compose.dropOnCell(Position(1, 0))
        assertTrue(session.state.board.pipes.isEmpty())
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput { swipeLeft() }
        assertEquals(TipId.Intro, shownTip())
    }

    @Test
    fun aCrossInTheStartingStackComesAfterTheTourAndOnlyOnce() {
        val level = FieldTest.level(id = 4)
        val session = GameSession(level, Random(7))
        session.state = session.state.copy(stack = session.state.stack.copy(sections = listOf(PipeSection.H, PipeSection.X, PipeSection.H, PipeSection.H, PipeSection.H)))
        val tips = InMemorySettingsRepository(initialSeenTips = setOf(TipId.Intro))
        show(level, session, tips)
        assertEquals(TipId.Cross, shownTip())
        compose.onNode(hasText("TIP"), useUnmergedTree = true).assertIsDisplayed()
        tap(TipOverlayTags.DONE)
        assertEquals(setOf(TipId.Intro, TipId.Cross), runBlocking { tips.seenTips.first() })
        // Another cross drawn later shows nothing.
        compose.runOnIdle { session.placeSection(PipeSection.X, Position(1, 0)) }
        compose.waitForIdle()
        assertEquals(null, shownTip())
    }

    @Test
    fun aReservoirDrawnOntoTheStackShowsItsTipWithTheLevelSpeed() {
        val level = FieldTest.level(id = 4)
        val session = GameSession(level, Random(7))
        show(level, session, InMemorySettingsRepository(initialSeenTips = setOf(TipId.Intro)))
        assertEquals(null, shownTip())
        compose.runOnIdle {
            session.state = session.state.copy(stack = session.state.stack.copy(sections = listOf(PipeSection.RES) + session.state.stack.sections.drop(1)))
        }
        compose.waitForIdle()
        assertEquals(TipId.Reservoir, shownTip())
        compose.onNode(hasText("30 %", substring = true), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun comingBackToTheAppDuringATipShowsThePauseDialogAndResumeKeepsTheTip() {
        val session = show()
        compose.runOnIdle { session.pauseByUser() }
        compose.waitForIdle()
        compose.onNodeWithTag(PauseTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        assertEquals(TipId.Intro, shownTip())
        assertTrue(session.state.paused)
    }

    @Test
    fun aScreenReaderReadsTheStepAndTheHighlightedElement() {
        show()
        compose.onNode(hasContentDescription("Step 1 of 5"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasContentDescription("Highlighted: start valve", substring = true), useUnmergedTree = true).assertIsDisplayed()
        val card = compose.onNodeWithTag(TipOverlayTags.CARD).fetchSemanticsNode()
        assertEquals("The Start", card.config.getOrNull(SemanticsProperties.PaneTitle))
    }

    @Test
    fun onAScreenTooShortForTheStackTheCutOffSlotsAreNotFullyVisible() {
        val level = FieldTest.level(id = 4, visibleSize = 10)
        val stack = GameSession(level, Random(7)).state.stack
        val targets = TipTargets()
        compose.setContent {
            LeakOutTheme {
                CompositionLocalProvider(LocalTipTargets provides targets) {
                    // Far shorter than ten slots need, so the top of the rail is cut off.
                    Box(Modifier.size(120.dp, 160.dp)) {
                        StackPanel(stack = stack, visibleSize = 10, dragState = SectionDragState())
                    }
                }
            }
        }
        compose.waitForIdle()
        val visible = compose.runOnIdle { targets.fullyVisibleSlots }!!
        assertTrue("the next slot is visible", stack.size - 1 in visible)
        assertTrue("the top slot is cut off", 0 !in visible)
    }

    private fun mandatoryLevel(): Level = FieldTest.level(id = 25).copy(
        mandatory = listOf(MandatorySection(Position(1, 2), PipeSection.X), MandatorySection(Position(2, 1), PipeSection.RES)),
    )

    @Test
    fun theFirstLevelWithMandatorySectionsHighlightsEveryOneOfThem() {
        val level = mandatoryLevel()
        val session = GameSession(level, Random(7))
        val tips = InMemorySettingsRepository(initialSeenTips = setOf(TipId.Intro))
        show(level, session, tips)
        assertEquals(TipId.Mandatory, shownTip())
        assertTrue(session.state.paused)
        assertEquals(setOf(compose.cellRect(Position(1, 2)), compose.cellRect(Position(2, 1))), spotlights().toSet())
        tap(TipOverlayTags.DONE)
        assertEquals(setOf(TipId.Intro, TipId.Mandatory), runBlocking { tips.seenTips.first() })
    }

    @Test
    fun theMandatoryTipShowsOnce() {
        val level = mandatoryLevel()
        show(level, GameSession(level, Random(7)), InMemorySettingsRepository(initialSeenTips = setOf(TipId.Intro, TipId.Mandatory)))
        assertEquals(null, shownTip())
    }

    @Test
    fun theMandatoryTipComesAfterTheTour() {
        val level = mandatoryLevel()
        show(level, GameSession(level, Random(7)))
        assertEquals(TipId.Intro, shownTip())
        tap(TipOverlayTags.SKIP)
        assertEquals(TipId.Mandatory, shownTip())
    }
}
