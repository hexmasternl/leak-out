package nl.hexmaster.leakout.ui.designer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.SavedStateHandle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.core.level.DesignStatus
import nl.hexmaster.leakout.core.level.EndpointKind
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelDraft
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevelDesignerScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val levels = object : DesignerLevels {
        private val bundled = (1..4).map { LevelDraft.new(it).withName("Level $it").level }
        override fun levelCount() = bundled.size
        override fun load(id: Int) = bundled.getOrNull(id - 1)
    }
    private val exports = mutableListOf<String>()
    private val testPlays = mutableListOf<Level>()
    private var backs = 0
    private lateinit var viewModel: LevelDesignerViewModel

    private val level get() = viewModel.uiState.value.level

    private fun show() {
        viewModel = LevelDesignerViewModel(levels, { name, _ -> exports += name }, SavedStateHandle())
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    LevelDesignerScreen(viewModel, onTestPlay = { testPlays += it }, onBack = { backs++ })
                }
            }
        }
        compose.waitForIdle()
    }

    /** The centre of [cell] in the grid node's own coordinates. */
    private fun centreOf(cell: Position): Offset {
        val size = compose.onNodeWithTag(DesignerTags.GRID).fetchSemanticsNode().size.toSize()
        return DesignerGridLayout.compute(size, level.grid).cellRect(cell).center
    }

    private fun clickTag(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun opensOnABlankSolvableDesign() {
        show()
        compose.onNodeWithTag(DesignerTags.GRID).assertIsDisplayed()
        compose.onNodeWithTag(DesignerTags.STATUS).assertTextEquals("Valid and solvable")
        compose.onNodeWithTag(DesignerTags.TEST_PLAY).assertIsEnabled()
        compose.onNodeWithTag(DesignerTags.EXPORT).assertIsEnabled()
        assertEquals(5, level.id)
    }

    @Test
    fun aTapWithTheStartToolMovesTheStart() {
        show()
        val target = centreOf(Position(2, 3))
        compose.onNodeWithTag(DesignerTags.GRID).performTouchInput { click(target) }
        compose.waitForIdle()
        assertEquals(Position(2, 3), level.start.position)
        assertEquals(Side.RIGHT, level.start.openSide)
    }

    @Test
    fun aDragWithTheBlockToolBlocksARow() {
        show()
        clickTag(DesignerTags.tool(DesignerTool.BLOCK))
        val cells = (1..4).map { Position(it, 1) }
        val points = cells.map(::centreOf)
        compose.onNodeWithTag(DesignerTags.GRID).performTouchInput {
            down(points.first())
            points.drop(1).forEach { moveTo(it) }
            up()
        }
        compose.waitForIdle()
        assertEquals(cells.toSet(), level.blocked.toSet())
    }

    @Test
    fun aTapWithTheMandatoryCrossToolPlacesACross() {
        show()
        clickTag(DesignerTags.tool(DesignerTool.MANDATORY_CROSS))
        val target = centreOf(Position(3, 2))
        compose.onNodeWithTag(DesignerTags.GRID).performTouchInput { click(target) }
        compose.waitForIdle()
        assertEquals(listOf(MandatorySection(Position(3, 2), PipeSection.X)), level.mandatory)
        compose.onNodeWithTag(DesignerTags.STATUS).performScrollTo().assertTextEquals("Valid and solvable")
    }

    @Test
    fun starValuesAreCalculatedAndCannotBeEdited() {
        show()
        compose.onNodeWithTag(DesignerTags.STAR_THREE).performScrollTo().assertTextEquals("330")
        compose.onNodeWithTag(DesignerTags.STAR_TWO).assertTextEquals("250")
        compose.onNodeWithTag(DesignerTags.STAR_ONE).assertTextEquals("200")
        listOf(DesignerTags.STAR_ONE, DesignerTags.STAR_TWO, DesignerTags.STAR_THREE).forEach { tag ->
            compose.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
        }

        clickTag(DesignerTags.tool(DesignerTool.BLOCK))
        val target = centreOf(Position(3, 1))
        compose.onNodeWithTag(DesignerTags.GRID).performTouchInput { click(target) }
        compose.waitForIdle()
        // 32 free cells: 320, 240 and 190.
        compose.onNodeWithTag(DesignerTags.STAR_THREE).performScrollTo().assertTextEquals("320")
        compose.onNodeWithTag(DesignerTags.STAR_TWO).assertTextEquals("240")
        compose.onNodeWithTag(DesignerTags.STAR_ONE).assertTextEquals("190")
    }

    @Test
    fun columnSteppersAreDisabledAtTheLimits() {
        show()
        repeat(3) { clickTag(DesignerTags.COLUMNS_DOWN) }
        compose.onNodeWithTag(DesignerTags.COLUMNS_VALUE).assertTextEquals("4")
        compose.onNodeWithTag(DesignerTags.COLUMNS_DOWN).assertIsNotEnabled()
        compose.onNodeWithTag(DesignerTags.COLUMNS_UP).assertIsEnabled()

        compose.runOnIdle { repeat(46) { viewModel.changeColumns(+1) } }
        compose.onNodeWithTag(DesignerTags.COLUMNS_VALUE).assertTextEquals("50")
        compose.onNodeWithTag(DesignerTags.COLUMNS_UP).assertIsNotEnabled()
        compose.onNodeWithTag(DesignerTags.COLUMNS_DOWN).assertIsEnabled()
    }

    @Test
    fun anInvalidDesignDisablesTestPlayAndExportAndListsItsErrors() {
        show()
        clickTag(DesignerTags.side(EndpointKind.START, Side.LEFT))
        compose.onNodeWithTag(DesignerTags.field(NumberField.InitialDelay)).performScrollTo().performTextReplacement("-5")
        compose.waitForIdle()
        assertTrue(viewModel.uiState.value.status is DesignStatus.Invalid)
        compose.onNodeWithTag(DesignerTags.STATUS).performScrollTo().assertTextEquals("Invalid: 2 problems")
        compose.onAllNodesWithTag(DesignerTags.ERROR).assertCountEquals(2)
        compose.onNodeWithTag(DesignerTags.TEST_PLAY).assertIsNotEnabled()
        compose.onNodeWithTag(DesignerTags.EXPORT).assertIsNotEnabled()
    }

    @Test
    fun textThatIsNotANumberKeepsTheDesign() {
        show()
        compose.onNodeWithTag(DesignerTags.field(NumberField.InitialDelay)).performScrollTo().performTextReplacement("abc")
        compose.waitForIdle()
        assertEquals(20_000L, level.flow.initialDelayMs)
        assertTrue(viewModel.uiState.value.isInvalid(NumberField.InitialDelay))
    }

    @Test
    fun exportAndTestPlayHandOverTheDesign() {
        show()
        clickTag(DesignerTags.EXPORT)
        assertEquals(listOf("level_005.json"), exports)
        clickTag(DesignerTags.TEST_PLAY)
        assertEquals(listOf(level), testPlays)
    }

    @Test
    fun startFromCopiesABundledLevel() {
        show()
        clickTag(DesignerTags.START_FROM)
        compose.onNodeWithTag(DesignerTags.START_FROM_DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(DesignerTags.copyLevel(2)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(DesignerTags.START_FROM_DIALOG).assertDoesNotExist()
        assertEquals("Level 2", level.name)
    }

    @Test
    fun backWithoutChangesLeavesAtOnce() {
        show()
        Espresso.pressBack()
        compose.waitForIdle()
        assertEquals(1, backs)
        compose.onNodeWithTag(DesignerTags.DISCARD_DIALOG).assertDoesNotExist()
    }

    @Test
    fun backWithChangesAsksToDiscard() {
        show()
        clickTag(DesignerTags.COLUMNS_UP)
        clickTag(DesignerTags.BACK)
        compose.onNodeWithTag(DesignerTags.DISCARD_DIALOG).assertIsDisplayed()
        assertEquals(0, backs)

        compose.onNodeWithTag(DesignerTags.DISCARD_CANCEL).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(DesignerTags.DISCARD_DIALOG).assertDoesNotExist()
        assertEquals(0, backs)
        assertEquals(8, level.grid.columns)

        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag(DesignerTags.DISCARD_CONFIRM).performClick()
        compose.waitForIdle()
        assertEquals(1, backs)
    }
}
