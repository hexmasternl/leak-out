package nl.hexmaster.leakout.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.StarBenchmarks

/** Levels and gestures shared by the game screen tests. Positions are in root pixels. */
internal object FieldTest {
    /** A level whose stack only draws [section], so the next section is always known. */
    fun level(
        grid: GridSize = GridSize(4, 4),
        start: Endpoint = Endpoint(Position(0, 0), Side.RIGHT),
        finish: Endpoint = Endpoint(Position(grid.columns - 1, grid.rows - 1), Side.LEFT),
        blocked: List<Position> = emptyList(),
        visibleSize: Int = 5,
        rules: PlacementRules = PlacementRules(),
        section: PipeSection = PipeSection.H,
        id: Int = 99,
        initialDelayMs: Long = 10_000,
        cellsPerSecond: Double = 0.8,
        stars: StarBenchmarks = StarBenchmarks(100, 200, 350),
    ): Level = Level(
        id = id,
        name = "Test",
        grid = grid,
        start = start,
        finish = finish,
        blocked = blocked,
        flow = FlowSettings(initialDelayMs = initialDelayMs, cellsPerSecond = cellsPerSecond),
        stack = StackSettings(visibleSize = visibleSize, weights = PipeSection.entries.associateWith { if (it == section) 1 else 0 }),
        rules = rules,
        stars = stars,
    )

    /** The 16 × 12 level used for pan and zoom, with the start on the left edge halfway down. */
    fun large(rules: PlacementRules = PlacementRules()): Level = level(
        grid = GridSize(16, 12),
        start = Endpoint(Position(0, 5), Side.RIGHT),
        finish = Endpoint(Position(15, 6), Side.LEFT),
        rules = rules,
    )
}

internal fun ComposeTestRule.px(value: Dp): Float = with(density) { value.toPx() }

internal fun ComposeTestRule.cellNode(cell: Position): SemanticsNodeInteraction =
    onNodeWithTag(GameScreenTags.cell(cell.x, cell.y), useUnmergedTree = true)

/** The cell's rect in root pixels, not clipped by the field. */
internal fun ComposeTestRule.cellRect(cell: Position): Rect =
    cellNode(cell).fetchSemanticsNode().let { Rect(it.positionInRoot, it.size.toSize()) }

internal fun ComposeTestRule.fieldRect(): Rect =
    onNodeWithTag(GameScreenTags.FIELD).fetchSemanticsNode().boundsInRoot

internal fun ComposeTestRule.nextNode(section: String = "Horizontal straight"): SemanticsNodeInteraction =
    onNodeWithContentDescription("Next: $section", useUnmergedTree = true)

internal fun ComposeTestRule.assertNothingHighlighted() {
    onAllNodes(SemanticsMatcher.expectValue(FieldCellHighlighted, true), useUnmergedTree = true).assertCountEquals(0)
}

internal fun ComposeTestRule.assertHighlighted(cell: Position) {
    onAllNodes(SemanticsMatcher.expectValue(FieldCellHighlighted, true), useUnmergedTree = true).assertCountEquals(1)
    cellNode(cell).assert(SemanticsMatcher.expectValue(FieldCellHighlighted, true))
}


/**
 * Presses the next section and moves the finger so the drop point (the dragged copy's centre,
 * [DraggedCopyYOffset] above the finger) is at [dropPoint]. The finger stays down.
 */
internal fun ComposeTestRule.beginDragTo(dropPoint: Offset) {
    val next = nextNode()
    val origin = next.fetchSemanticsNode().positionInRoot
    next.performTouchInput {
        down(center)
        // Two steps: the first passes the touch slop, the second lands exactly.
        moveTo(center + Offset(40f, 0f))
        moveTo(dropPoint + Offset(0f, px(DraggedCopyYOffset)) - origin)
    }
    waitForIdle()
}

/** Moves a drag that is going on so the drop point is at [dropPoint]. */
internal fun ComposeTestRule.moveDragTo(dropPoint: Offset) {
    val next = nextNode()
    val origin = next.fetchSemanticsNode().positionInRoot
    next.performTouchInput { moveTo(dropPoint + Offset(0f, px(DraggedCopyYOffset)) - origin) }
    waitForIdle()
}

internal fun ComposeTestRule.releaseDrag() {
    nextNode().performTouchInput { up() }
    mainClock.advanceTimeBy(300)
    waitForIdle()
}

/** Drags the next section so it is dropped on the centre of [cell]. */
internal fun ComposeTestRule.dropOnCell(cell: Position) {
    val target = cellRect(cell).center
    beginDragTo(target)
    releaseDrag()
}

internal fun ComposeTestRule.dropAt(dropPoint: Offset) {
    beginDragTo(dropPoint)
    releaseDrag()
}

/**
 * Freezes the device in landscape, as the game runs, for the tests inside it. Put it outside the
 * compose rule so the activity already starts in landscape.
 */
internal class LandscapeRule : org.junit.rules.ExternalResource() {
    private val automation get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation

    override fun before() {
        automation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_90)
    }

    override fun after() {
        automation.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE)
    }
}
