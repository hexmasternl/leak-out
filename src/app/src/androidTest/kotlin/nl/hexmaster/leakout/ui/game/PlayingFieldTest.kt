package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** The playing field on a bare 800 × 360 dp game screen: layout, pan, zoom, highlight and drops. */
@RunWith(AndroidJUnit4::class)
class PlayingFieldTest {
    val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(LandscapeRule()).around(compose)

    private lateinit var session: GameSession
    private val start = Position(0, 5)

    // 8.1
    @Test
    fun fieldFillsTheSpaceRightOfTheStackAndBelowTheHud() {
        show()
        val field = compose.fieldRect()
        val stack = compose.onNodeWithTag(GameScreenTags.STACK_PANEL).fetchSemanticsNode().boundsInRoot
        val hud = compose.onNodeWithTag(HudTags.HUD).fetchSemanticsNode().boundsInRoot
        val screen = compose.onNodeWithTag(GameScreenTags.SCREEN).fetchSemanticsNode().boundsInRoot
        val padding = compose.px(12.dp)

        assertTrue("field right of the stack", field.left >= stack.right)
        assertTrue("field below the HUD", field.top >= hud.bottom)
        assertEquals(screen.right - padding, field.right, 1f)
        assertEquals(screen.bottom - padding, field.bottom, 1f)
    }

    // 8.1
    @Test
    fun smallLevelIsShownCompletelyAndCentred() {
        show(FieldTest.level(grid = GridSize(8, 6)))
        val field = compose.fieldRect()
        val cells = (0 until 6).flatMap { y -> (0 until 8).map { x -> compose.cellRect(Position(x, y)) } }
        val grid = cells.reduce { a, b -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)) }

        assertEquals(minOf(field.width / 8, field.height / 6), cells[0].width, 1f)
        assertTrue(grid.left >= field.left - 1 && grid.right <= field.right + 1)
        assertTrue(grid.top >= field.top - 1 && grid.bottom <= field.bottom + 1)
        assertEquals(grid.left - field.left, field.right - grid.right, 1f)
        assertEquals(grid.top - field.top, field.bottom - grid.bottom, 1f)
    }

    // 8.1
    @Test
    fun largeLevelStartsAt40dpWithTheStartVisibleAndClipped() {
        show(FieldTest.large())
        val field = compose.fieldRect()
        val startRect = compose.cellRect(start)

        assertEquals(compose.px(40.dp), startRect.width, 1f)
        assertTrue("start cell inside the field", field.contains(startRect.topLeft) && startRect.bottom <= field.bottom + 1)
        val hud = compose.onNodeWithTag(HudTags.HUD).fetchSemanticsNode().boundsInRoot
        val stack = compose.onNodeWithTag(GameScreenTags.STACK_PANEL).fetchSemanticsNode().boundsInRoot
        for (node in cellNodes()) {
            val shown = node.boundsInRoot
            if (shown.isEmpty) continue
            assertTrue("cell ${node.tag()} over the HUD", shown.top >= hud.bottom - 1)
            assertTrue("cell ${node.tag()} over the stack", shown.left >= stack.right - 1)
            assertTrue("cell ${node.tag()} outside the field", shown.top >= field.top - 1 && shown.bottom <= field.bottom + 1)
        }
    }

    // 8.2
    @Test
    fun swipeMovesALargeLevelAndStopsAtTheEdges() {
        show(FieldTest.large())
        val before = compose.cellRect(start)

        swipeField(Offset(0f, -compose.px(60.dp)))
        val moved = compose.cellRect(start)
        assertTrue("moved up: ${before.top} -> ${moved.top}", moved.top < before.top - compose.px(20.dp))
        assertEquals("x fits and stays centred", before.left, moved.left, 1f)

        repeat(4) { swipeField(Offset(0f, -compose.px(200.dp))) }
        assertEquals(compose.fieldRect().bottom, compose.cellRect(Position(0, 11)).bottom, 1f)

        repeat(4) { swipeField(Offset(0f, compose.px(200.dp))) }
        assertEquals(compose.fieldRect().top, compose.cellRect(Position(0, 0)).top, 1f)
    }

    // 8.2
    @Test
    fun tapDoesNotMoveTheLevel() {
        show(FieldTest.large())
        val before = compose.cellRect(start)
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput { click(center) }
        compose.waitForIdle()
        assertSameRect(before, compose.cellRect(start))
    }

    // 8.2
    @Test
    fun levelThatFitsDoesNotPan() {
        show(FieldTest.level())
        val before = compose.cellRect(Position(1, 1))
        swipeField(Offset(compose.px(80.dp), compose.px(60.dp)))
        assertSameRect(before, compose.cellRect(Position(1, 1)))
    }

    // 8.3
    @Test
    fun pinchOutZoomsInUpTo120dp() {
        show(FieldTest.large())
        val focusCell = cellAt(compose.fieldRect().center)
        pinchField(outwards = true)
        val zoomed = compose.cellRect(focusCell)
        assertTrue("cells got larger: ${zoomed.width}", zoomed.width > compose.px(40.dp) + 1)
        assertTrue("the cell under the fingers stays there", zoomed.contains(compose.fieldRect().center))

        repeat(5) { pinchField(outwards = true) }
        assertEquals(compose.px(120.dp), compose.cellRect(cellAt(compose.fieldRect().center)).width, 1f)
    }

    // 8.3
    @Test
    fun pinchInStopsAtTheStartingSize() {
        show(FieldTest.large())
        pinchField(outwards = false)
        assertEquals(compose.px(40.dp), compose.cellRect(start).width, 1f)

        pinchField(outwards = true)
        repeat(3) { pinchField(outwards = false) }
        assertEquals(compose.px(40.dp), compose.cellRect(cellAt(compose.fieldRect().center)).width, 1f)
    }

    // 8.3
    @Test
    fun twistDoesNotRotateOrMoveTheLevel() {
        show(FieldTest.large())
        val before = compose.cellRect(start)
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput {
            val radius = 100.dp.toPx()
            fun at(angle: Double) = center + Offset((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
            down(0, at(0.0))
            down(1, at(PI))
            for (step in 1..20) {
                val angle = step * (PI / 2) / 20
                updatePointerTo(0, at(angle))
                updatePointerTo(1, at(angle + PI))
                move()
            }
            up(0)
            up(1)
        }
        compose.waitForIdle()
        assertSameRect(before, compose.cellRect(start))
    }

    // 8.4
    @Test
    fun highlightFollowsTheDropPointOnlyOverTilesThatAccept() {
        show(FieldTest.level(blocked = listOf(Position(2, 2))))
        compose.runOnIdle {
            session.placeSection(PipeSection.V, Position(2, 1))
            session.markLiquidEntered(Position(2, 1))
            session.placeSection(PipeSection.V, Position(1, 3))
        }

        compose.beginDragTo(compose.cellRect(Position(1, 1)).center)
        compose.assertHighlighted(Position(1, 1))
        compose.moveDragTo(compose.cellRect(Position(1, 2)).center)
        compose.assertHighlighted(Position(1, 2))
        compose.moveDragTo(compose.cellRect(Position(1, 3)).center)
        compose.assertHighlighted(Position(1, 3))

        for (refusing in listOf(Position(2, 2), Position(0, 0), Position(3, 3), Position(2, 1))) {
            compose.moveDragTo(compose.cellRect(refusing).center)
            compose.assertNothingHighlighted()
        }
        compose.moveDragTo(compose.onNodeWithTag(GameScreenTags.STACK_PANEL).fetchSemanticsNode().boundsInRoot.center)
        compose.assertNothingHighlighted()
        compose.moveDragTo(compose.onNodeWithTag(HudTags.HUD).fetchSemanticsNode().boundsInRoot.center)
        compose.assertNothingHighlighted()

        compose.moveDragTo(compose.cellRect(Position(1, 1)).center)
        compose.assertHighlighted(Position(1, 1))
        compose.releaseDrag()
        compose.assertNothingHighlighted()
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Placed H")
    }

    // 8.5
    @Test
    fun dropsPlaceReplaceAndRespectLiquidAndTheReplacement() {
        // The countdown outlasts the replacement: the ticks below must not start the liquid.
        show(FieldTest.level(rules = PlacementRules(allowReplace = true, replacePenaltyMs = 60_000), initialDelayMs = 120_000))
        compose.runOnIdle {
            session.placeSection(PipeSection.V, Position(1, 1))
            session.placeSection(PipeSection.V, Position(2, 1))
            session.markLiquidEntered(Position(2, 1))
        }

        compose.dropOnCell(Position(1, 2))
        compose.cellNode(Position(1, 2)).assertContentDescriptionEquals("Placed H")

        compose.dropOnCell(Position(2, 1))
        compose.cellNode(Position(2, 1)).assertContentDescriptionEquals("Placed V, filled")

        // The V breaks: the tile is empty and busy until the H lands.
        compose.dropOnCell(Position(1, 1))
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Empty")
            .assert(SemanticsMatcher.expectValue(ReplacingCell, true))
        compose.runOnIdle { assertEquals(2, session.state.board.pipes.size) }

        // The stack cannot be picked up, so nothing is dragged and nothing is placed.
        compose.beginDragTo(compose.cellRect(Position(2, 2)).center)
        compose.assertNothingHighlighted()
        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertDoesNotExist()
        compose.releaseDrag()
        compose.cellNode(Position(2, 2)).assertContentDescriptionEquals("Empty")

        compose.runOnIdle { session.sendTick(60_000) }
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Placed H")
            .assert(SemanticsMatcher.keyNotDefined(ReplacingCell))
        compose.beginDragTo(compose.cellRect(Position(2, 2)).center)
        compose.assertHighlighted(Position(2, 2))
        compose.releaseDrag()
        compose.cellNode(Position(2, 2)).assertContentDescriptionEquals("Placed H")
    }

    @Test
    fun tapsPlaceReplaceAndRespectLiquidTheReplacementAndBlockedCells() {
        show(
            FieldTest.level(
                blocked = listOf(Position(0, 3)),
                rules = PlacementRules(allowReplace = true, replacePenaltyMs = 60_000),
                initialDelayMs = 120_000,
            ),
        )
        compose.runOnIdle {
            session.placeSection(PipeSection.V, Position(1, 1))
            session.placeSection(PipeSection.V, Position(2, 1))
            session.markLiquidEntered(Position(2, 1))
        }

        tapCell(Position(1, 2))
        compose.cellNode(Position(1, 2)).assertContentDescriptionEquals("Placed H")

        tapCell(Position(2, 1))
        compose.cellNode(Position(2, 1)).assertContentDescriptionEquals("Placed V, filled")

        tapCell(Position(1, 1))
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Empty")

        // The replacement locks placing until the new section lands.
        tapCell(Position(2, 2))
        compose.cellNode(Position(2, 2)).assertContentDescriptionEquals("Empty")

        compose.runOnIdle { session.sendTick(60_000) }
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Placed H")
        tapCell(Position(0, 3))
        compose.cellNode(Position(0, 3)).assertContentDescriptionEquals("Blocked")
        tapCell(Position(0, 0))
        compose.cellNode(Position(0, 0)).assertContentDescriptionEquals("Start")
        tapCell(Position(2, 2))
        compose.cellNode(Position(2, 2)).assertContentDescriptionEquals("Placed H")
        compose.runOnIdle { assertEquals(4, session.state.board.pipes.size) }
    }

    @Test
    fun swipesPinchesAndPausedTapsDoNotPlace() {
        show(FieldTest.large())
        swipeField(Offset(-compose.px(120.dp), 0f))
        pinchField(outwards = true)
        compose.runOnIdle { assertEquals(0, session.state.board.pipes.size) }

        compose.runOnIdle { session.send(GameCommand.Pause) }
        compose.waitForIdle()
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput { click(center) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, session.state.board.pipes.size) }
    }

    private fun tapCell(cell: Position) {
        val target = cellRectInField(cell).center
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput { click(target) }
        compose.waitForIdle()
    }

    // 8.6
    @Test
    fun draggingASectionOverTheFieldDoesNotPanIt() {
        show(FieldTest.large())
        val before = compose.cellRect(start)
        val field = compose.fieldRect()
        compose.beginDragTo(field.center)
        compose.moveDragTo(Offset(field.center.x, field.top + compose.px(20.dp)))
        compose.moveDragTo(Offset(field.right - compose.px(20.dp), field.bottom - compose.px(20.dp)))
        compose.releaseDrag()
        assertSameRect(before, compose.cellRect(start))
    }

    // 8.6
    @Test
    fun dropAfterPanAndZoomLandsOnTheTileUnderTheDropPoint() {
        show(FieldTest.large())
        pinchField(outwards = true)
        swipeField(Offset(-compose.px(120.dp), -compose.px(60.dp)))
        val point = compose.fieldRect().center + Offset(compose.px(30.dp), compose.px(10.dp))
        val cell = cellAt(point)
        compose.cellNode(cell).assertContentDescriptionEquals("Empty")

        compose.dropAt(point)

        compose.cellNode(cell).assertContentDescriptionEquals("Placed H")
        compose.onAllNodes(hasContentDescription("Placed H"), useUnmergedTree = true).assertCountEquals(1)
    }

    // 8.7
    @Test
    fun pausingDuringADragClearsTheHighlight() {
        show()
        compose.beginDragTo(compose.cellRect(Position(1, 1)).center)
        compose.assertHighlighted(Position(1, 1))

        compose.runOnIdle { session.send(GameCommand.Pause) }
        compose.waitForIdle()

        compose.assertNothingHighlighted()
        compose.releaseDrag()
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Empty")
    }

    // 8.7
    @Test
    fun resumeKeepsTheViewAndRestartResetsIt() {
        show(FieldTest.large())
        val initial = compose.cellRect(start)
        compose.dropOnCell(Position(3, 5))
        pinchField(outwards = true)
        swipeField(Offset(-compose.px(100.dp), -compose.px(50.dp)))
        val probe = cellAt(compose.fieldRect().center)
        val moved = compose.cellRect(probe)

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.waitForIdle()
        assertSameRect(moved, compose.cellRect(probe))

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.RESTART).performClick()
        compose.waitForIdle()
        assertSameRect(initial, compose.cellRect(start))
        compose.onAllNodes(hasContentDescription("Placed H"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun placedSectionIsDrawnWithItsPipeArt() {
        show()
        val cell = Position(1, 1)
        assertTrue(session.placeSection(PipeSection.H, cell))
        compose.waitForIdle()

        compose.cellNode(cell).assert(SemanticsMatcher.expectValue(FieldPipeSection, PipeSection.H))
        compose.cellNode(Position(2, 2)).assert(SemanticsMatcher.keyNotDefined(FieldPipeSection))
        assertPipeArtOfH(cell)
    }

    @Test
    fun pipeArtFollowsZoom() {
        show(FieldTest.large())
        pinchField(outwards = true)
        val cell = cellAt(compose.fieldRect().center)
        assertTrue(session.placeSection(PipeSection.H, cell))
        compose.waitForIdle()

        assertTrue("zoomed in", compose.cellRect(cell).width > compose.px(40.dp) + 1)
        assertPipeArtOfH(cell)
    }

    @Test
    fun liquidFlowsThroughThePipeAndTheTileKeepsItsColour() {
        show()
        val first = Position(1, 0)
        compose.runOnIdle {
            assertTrue(session.placeSection(PipeSection.H, first))
            assertTrue(session.placeSection(PipeSection.H, Position(2, 0)))
            session.endCountdown()
        }
        compose.waitForIdle()
        val dry = fieldPixels()
        val rect = cellRectInField(first)

        // 0.8 cells per second: 1250 ms through the start, then half of the first H.
        compose.runOnIdle { session.sendTick(1_250 + 625) }
        compose.waitForIdle()
        val half = fieldPixels()
        assertTrue("liquid near the entry", half.isLiquidAt(rect, 0.15f, 0.5f))
        assertTrue("liquid just behind the middle", half.isLiquidAt(rect, 0.4f, 0.5f))
        assertTrue("dry past the head", !half.isLiquidAt(rect, 0.7f, 0.5f) && !half.isLiquidAt(rect, 0.9f, 0.5f))
        assertTileUnchanged(dry, half, rect)

        // A quarter later the front has moved on a quarter of the pipe, at the same speed.
        compose.runOnIdle { session.sendTick(312) }
        compose.waitForIdle()
        val threeQuarters = fieldPixels()
        assertTrue("liquid at 0.65", threeQuarters.isLiquidAt(rect, 0.65f, 0.5f))
        assertTrue("dry at 0.9", !threeQuarters.isLiquidAt(rect, 0.9f, 0.5f))

        // On into the next H: the first is full to its exit border, and still only its pipe is blue.
        compose.runOnIdle { session.sendTick(313 + 625) }
        compose.waitForIdle()
        val full = fieldPixels()
        for (fx in listOf(0.1f, 0.5f, 0.95f)) assertTrue("full at $fx", full.isLiquidAt(rect, fx, 0.5f))
        assertTrue("the next H fills from its entry", full.isLiquidAt(cellRectInField(Position(2, 0)), 0.15f, 0.5f))
        assertTileUnchanged(dry, full, rect)
    }

    private fun fieldPixels(): PixelMap = compose.onNodeWithTag(GameScreenTags.FIELD).captureToImage().toPixelMap()

    private fun cellRectInField(cell: Position): Rect {
        val field = compose.fieldRect()
        return compose.cellRect(cell).translate(-field.left, -field.top)
    }

    /** Liquid at [fx] across the bore around [fy], off the lighter highlight on the centre line. */
    private fun PixelMap.isLiquidAt(rect: Rect, fx: Float, fy: Float): Boolean =
        listOf(-0.07f, -0.05f, 0.05f, 0.07f).any { dy ->
            val c = this[(rect.left + rect.width * fx).toInt(), (rect.top + rect.height * (fy + dy)).toInt()]
            c.red < 0.5f && c.green > 0.45f && c.blue > 0.6f && c.blue - c.red > 0.35f
        }

    /** Away from the bore the cell looks exactly as it did while dry: the tile is not tinted. */
    private fun assertTileUnchanged(dry: PixelMap, wet: PixelMap, rect: Rect) {
        for (fx in listOf(0.1f, 0.5f, 0.9f)) {
            for (fy in listOf(0.1f, 0.2f, 0.8f, 0.9f)) {
                val x = (rect.left + rect.width * fx).toInt()
                val y = (rect.top + rect.height * fy).toInt()
                val a = dry[x, y]
                val b = wet[x, y]
                val same = abs(a.red - b.red) < 0.02f && abs(a.green - b.green) < 0.02f && abs(a.blue - b.blue) < 0.02f
                assertTrue("tile colour at ($fx, $fy) changed from $a to $b", same)
            }
        }
    }

    /**
     * The H art spans the whole cell: at its centre line the dark bore, just above it the lighter
     * steel body (view box y ≈ 34, which the old stand-in left as background), from edge to edge.
     */
    private fun assertPipeArtOfH(cell: Position) {
        val field = compose.fieldRect()
        val image = compose.onNodeWithTag(GameScreenTags.FIELD).captureToImage().toPixelMap()
        val rect = compose.cellRect(cell).translate(-field.left, -field.top)
        for (fx in listOf(0.2f, 0.5f, 0.8f)) {
            val bore = image.brightnessAt(rect, fx, 0.5f)
            val body = image.brightnessAt(rect, fx, 0.34f)
            assertTrue("steel body $body brighter than the bore $bore at $fx", body > bore + 0.15f)
        }
    }

    private fun PixelMap.brightnessAt(rect: Rect, fx: Float, fy: Float): Float {
        val c = this[(rect.left + rect.width * fx).toInt(), (rect.top + rect.height * fy).toInt()]
        return c.red + c.green + c.blue
    }

    private fun show(level: Level = FieldTest.level()) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                // The test activity shows the system bars; the game hides them, so measure a bare 360 dp screen.
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session)
                }
            }
        }
        compose.waitForIdle()
    }

    /** One finger drags from the field centre by [delta] (in pixels). */
    private fun swipeField(delta: Offset) {
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput {
            down(center)
            for (step in 1..10) moveTo(center + delta * (step / 10f))
            up()
        }
        compose.waitForIdle()
    }

    /** Two fingers around the field centre move apart ([outwards]) or together. */
    private fun pinchField(outwards: Boolean) {
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput {
            val near = Offset(40.dp.toPx(), 0f)
            val far = Offset(160.dp.toPx(), 0f)
            if (outwards) {
                pinch(center - near, center - far, center + near, center + far)
            } else {
                pinch(center - far, center - near, center + far, center + near)
            }
        }
        compose.waitForIdle()
    }

    private fun cellNodes() = compose.onAllNodes(
        SemanticsMatcher("is a field cell") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(GameScreenTags.CELL_PREFIX) == true },
        useUnmergedTree = true,
    ).fetchSemanticsNodes()

    private fun androidx.compose.ui.semantics.SemanticsNode.tag(): String = config[SemanticsProperties.TestTag]

    /** The cell whose rect holds [point] (root pixels). */
    private fun cellAt(point: Offset): Position {
        val node = cellNodes().first { Rect(it.positionInRoot, it.size.let { s -> androidx.compose.ui.geometry.Size(s.width.toFloat(), s.height.toFloat()) }).contains(point) }
        val (x, y) = node.tag().removePrefix(GameScreenTags.CELL_PREFIX).split("_").map(String::toInt)
        return Position(x, y)
    }

    private fun assertSameRect(expected: Rect, actual: Rect) {
        val close = abs(expected.left - actual.left) <= 1 && abs(expected.top - actual.top) <= 1 && abs(expected.width - actual.width) <= 1
        assertTrue("expected $expected but was $actual", close)
    }
}
