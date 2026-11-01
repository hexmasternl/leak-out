package nl.hexmaster.leakout.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
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
import androidx.test.platform.app.InstrumentationRegistry
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.core.score.ScoreSheet

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
        mandatory: List<MandatorySection> = emptyList(),
        leaky: List<Position> = emptyList(),
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
        mandatory = mandatory,
        leaky = leaky,
    )

    /** The 16 × 12 level used for pan and zoom, with the start on the left edge halfway down. */
    fun large(rules: PlacementRules = PlacementRules(), cellsPerSecond: Double = 0.8): Level = level(
        grid = GridSize(16, 12),
        start = Endpoint(Position(0, 5), Side.RIGHT),
        finish = Endpoint(Position(15, 6), Side.LEFT),
        rules = rules,
        cellsPerSecond = cellsPerSecond,
    )

    /** The route of [level] with its default grid: start (0, 0) → (1, 0) → (2, 0) → down to (2, 3) → finish (3, 3). */
    val ROUTE: List<Pair<Position, PipeSection>> = listOf(
        Position(1, 0) to PipeSection.H,
        Position(2, 0) to PipeSection.LB,
        Position(2, 1) to PipeSection.V,
        Position(2, 2) to PipeSection.V,
        Position(2, 3) to PipeSection.RT,
    )
}

internal fun ComposeTestRule.px(value: Dp): Float = with(density) { value.toPx() }

/** The section's spoken name in a cell's content description, as the app's resources give it. */
internal val PipeSection.spoken: String
    get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(CellDescription.sectionName(this))

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

/**
 * Makes [section] the stack's next section, as if it had been drawn, and places it on [at] with a
 * place command. Returns whether the engine accepted the placement.
 */
internal fun GameSession.placeSection(section: PipeSection, at: Position): Boolean {
    state = state.copy(stack = state.stack.copy(sections = state.stack.sections.dropLast(1) + section))
    return send(GameCommand.Place(at))
}

internal fun GameSession.sendTick(dtMs: Long): Boolean = send(GameCommand.Tick(dtMs))

/** Sets the score of the attempt directly, for the HUD tests. The game only scores through the flow. */
internal fun GameSession.setScore(total: Int) {
    state = state.copy(sheet = ScoreSheet(total = total))
}

/** Ends the countdown, so the liquid is at the very start of the start tile. */
internal fun GameSession.endCountdown() {
    val waiting = state.flow as FlowState.Waiting
    send(GameCommand.Tick(waiting.remainingMs / state.flowTimeScale))
}

/** Sets the countdown before the flow directly, for the HUD tests. */
internal fun GameSession.setCountdown(remainingMs: Long) {
    state = state.copy(flow = FlowState.Waiting(remainingMs))
}

/** Runs the flow in ticks of [stepMs] until the level is won or lost. The outcome happens at the last tick. */
internal fun GameSession.flowToEnd(stepMs: Long = 100) {
    var guard = 0
    while (state.outcome is Outcome.Playing) {
        check(++guard < 100_000) { "the flow did not end" }
        check(send(GameCommand.Tick(stepMs))) { "a tick was refused" }
    }
}

/** Marks the section on [at] as entered by liquid, as the flow would, for the placement tests. */
internal fun GameSession.markLiquidEntered(at: Position) {
    state = state.copy(board = state.board.markLiquidEntered(at))
}

/** Asserts the HUD score bar reads [score] (as shown, e.g. "1,250"), whatever the stars after it. */
internal fun SemanticsNodeInteraction.assertScore(score: String): SemanticsNodeInteraction = assert(
    SemanticsMatcher("score bar reads Score $score") { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()?.startsWith("Score $score, ") == true
    },
)
