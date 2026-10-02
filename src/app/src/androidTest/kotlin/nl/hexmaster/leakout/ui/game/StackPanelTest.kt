package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.pipes.pipeTileTag
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StackPanelTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var session: GameSession

    // 4.1
    @Test
    fun nextSectionIsAtTheBottomAndNothingIsDrawnOutsideThePanel() {
        show(FieldTest.level(visibleSize = 10))

        val panel = compose.onNodeWithTag(GameScreenTags.STACK_PANEL).fetchSemanticsNode().boundsInRoot
        val next = compose.nextNode().fetchSemanticsNode().boundsInRoot
        val slots = compose.onAllNodes(hasStackSectionTag(), useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue(slots.isNotEmpty())
        for (slot in slots) {
            val drawn = slot.boundsInRoot
            assertTrue(drawn.top >= panel.top && drawn.bottom <= panel.bottom)
            if (drawn.height > 0f) assertTrue("the next section is the lowest slot", drawn.bottom <= next.bottom + 0.5f)
        }
        // The panel holds 20 sections, but only the ones reaching into the rail (plus one above it) are composed.
        assertTrue(slots.size < 20)
    }

    // 4.2
    @Test
    fun sectionsFallIntoTheirNewSlotAfterAPlacement() {
        show(FieldTest.level())
        val secondFromBottom = compose.runOnIdle { session.state.stack.let { it.idAt(it.size - 2) } }
        val before = glyphTop(secondFromBottom)
        compose.mainClock.autoAdvance = false

        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.H, Position(1, 1))) }
        compose.mainClock.advanceTimeByFrame()
        val atStart = glyphTop(secondFromBottom)
        compose.mainClock.advanceTimeBy(100)
        val at100 = glyphTop(secondFromBottom)
        compose.mainClock.advanceTimeBy(300)
        val atRest = glyphTop(secondFromBottom)
        compose.mainClock.advanceTimeBy(500)
        val later = glyphTop(secondFromBottom)

        assertEquals("the section starts where it was", before.value, atStart.value, 1f)
        assertTrue("it falls", at100 > atStart)
        assertTrue("it rests below where it was", atRest > at100)
        assertEquals("it rests within 400 ms", later.value, atRest.value, 0.5f)
    }

    // 4.2
    @Test
    fun twoQuickPlacementsNeverMoveASectionUp() {
        show(FieldTest.level())
        val id = compose.runOnIdle { session.state.stack.let { it.idAt(it.size - 3) } }
        compose.mainClock.autoAdvance = false

        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.H, Position(1, 1))) }
        var last = glyphTop(id)
        repeat(6) {
            compose.mainClock.advanceTimeByFrame()
            val now = glyphTop(id)
            assertTrue(now >= last - 0.5.dp)
            last = now
        }
        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.H, Position(1, 2))) }
        repeat(40) {
            compose.mainClock.advanceTimeByFrame()
            val now = glyphTop(id)
            // Only the landing bounce, at most 4 dp, goes up.
            assertTrue("moved up from $last to $now", now >= last - 4.5.dp)
            last = now
        }
    }

    // 4.2
    @Test
    fun withReducedMotionSectionsSlideInOneHundredSixtyMilliseconds() {
        setAnimatorScale("0")
        try {
            show(FieldTest.level())
            val id = compose.runOnIdle { session.state.stack.let { it.idAt(it.size - 2) } }
            compose.mainClock.autoAdvance = false

            compose.runOnIdle { assertTrue(session.placeSection(PipeSection.H, Position(1, 1))) }
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeBy(176)
            val atEnd = glyphTop(id)
            var last = atEnd
            repeat(10) {
                compose.mainClock.advanceTimeByFrame()
                val now = glyphTop(id)
                assertEquals("no bounce after 160 ms", last.value, now.value, 0.5f)
                last = now
            }
        } finally {
            setAnimatorScale("1")
        }
    }

    // 4.3
    @Test
    fun theNextSectionCanBeDraggedWhileItFalls() {
        show(FieldTest.level())
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.H, Position(1, 1))) }
        compose.mainClock.advanceTimeBy(50)
        compose.mainClock.autoAdvance = true

        compose.dropOnCell(Position(1, 2))

        compose.cellNode(Position(1, 2)).assertContentDescriptionEquals("Placed H")
    }

    // 4.3
    @Test
    fun rejectedDropsLeaveTheStackUnchanged() {
        show(FieldTest.level(blocked = listOf(Position(2, 2))))
        val before = compose.runOnIdle { session.state.stack }

        compose.dropOnCell(Position(2, 2))
        compose.dropOnCell(Position(0, 0))

        compose.runOnIdle { assertEquals(before, session.state.stack) }
    }

    // 4.4
    @Test
    fun onlyFullyVisibleSlotsAreDescribed() {
        show(FieldTest.level(visibleSize = 10), width = 640.dp)

        val described = compose.onAllNodes(
            hasAnyAncestor(hasTestTag(GameScreenTags.STACK_PANEL)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        val panel = compose.onNodeWithTag(GameScreenTags.STACK_PANEL).fetchSemanticsNode().boundsInRoot

        // A rail of 360 dp minus the HUD fully shows the bottom 10 of the 20 sections.
        assertEquals(10, described.size)
        for (node in described) {
            assertTrue(node.boundsInRoot.height >= described.minOf { it.boundsInRoot.height } - 0.5f)
            assertTrue(node.boundsInRoot.top >= panel.top)
        }
        assertEquals(1, described.count { it.config[SemanticsProperties.ContentDescription].single().startsWith("Next: ") })
        compose.nextNode().assertContentDescriptionEquals("Next: Horizontal straight")
    }

    private fun hasStackSectionTag() = SemanticsMatcher("has a stack section tag") { node ->
        node.config.getOrElseNullable(SemanticsProperties.TestTag) { null }?.startsWith(GameScreenTags.STACK_SECTION_PREFIX) == true
    }

    /** Top of the pipe art of the stack section [id], where it is drawn (falling or not). */
    private fun glyphTop(id: Int): Dp = compose.onNode(
        hasTestTag(pipeTileTag(PipeSection.H)) and hasAnyAncestor(hasTestTag(GameScreenTags.stackSection(id))),
        useUnmergedTree = true,
    ).getUnclippedBoundsInRoot().top

    private fun setAnimatorScale(value: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("settings put global animator_duration_scale $value")
            .close()
    }

    private fun show(level: Level, width: Dp = 800.dp) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(width, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session)
                }
            }
        }
        compose.waitForIdle()
    }
}
