package nl.hexmaster.leakout.ui.designer

import androidx.lifecycle.SavedStateHandle
import nl.hexmaster.leakout.core.level.DesignStatus
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.EndpointKind
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelDraft
import nl.hexmaster.leakout.core.level.LevelParser
import nl.hexmaster.leakout.core.level.LevelWriter
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.ParseResult
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PortalPair
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.ReservoirType
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StarBenchmarks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeLevels(private val levels: List<Level>) : DesignerLevels {
    override fun levelCount() = levels.size
    override fun load(id: Int) = levels.firstOrNull { it.id == id }
}

private class RecordingExporter : LevelExporter {
    val exports = mutableListOf<Pair<String, String>>()
    override fun export(fileName: String, json: String) {
        exports += fileName to json
    }
}

class LevelDesignerViewModelTest {
    private val bundled = (1..4).map { id ->
        LevelDraft.new(id).withName("Level $id").paintBlocked(listOf(Position(3, id % 5)), blocked = true).level
    }
    private val exporter = RecordingExporter()

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        LevelDesignerViewModel(FakeLevels(bundled), exporter, saved)

    private val LevelDesignerViewModel.state get() = uiState.value

    /** Blocks the column in front of the finish, which leaves the level valid but unsolvable. */
    private fun LevelDesignerViewModel.wallOffFinish() {
        selectTool(DesignerTool.BLOCK)
        onDragStart(Position(4, 0))
        (1..4).forEach { onDrag(Position(4, it)) }
        onDragEnd()
    }

    @Test
    fun `opens on a blank design with id one past the bundled levels`() {
        val state = viewModel().state
        assertEquals(LevelDraft.new(5), state.draft)
        assertFalse(state.dirty)
        assertEquals(DesignerTool.START, state.tool)
        assertTrue(state.status is DesignStatus.Solvable)
        assertTrue(state.canPlay)
    }

    @Test
    fun `copy from a bundled level loads all its fields`() {
        val vm = viewModel()
        vm.copyFrom(2)
        assertEquals(bundled[1], vm.state.level)
        assertFalse(vm.state.dirty)
    }

    @Test
    fun `copy from an unknown level changes nothing`() {
        val vm = viewModel()
        vm.copyFrom(99)
        assertEquals(LevelDraft.new(5), vm.state.draft)
    }

    @Test
    fun `edits mark the design dirty`() {
        val vm = viewModel()
        vm.changeColumns(+3)
        assertEquals(10, vm.state.level.grid.columns)
        assertTrue(vm.state.dirty)
    }

    @Test
    fun `selecting a tool does not mark the design dirty`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.BLOCK)
        assertFalse(vm.state.dirty)
    }

    @Test
    fun `a tap with the start tool moves the start`() {
        val vm = viewModel()
        vm.onCellTap(Position(2, 3))
        assertEquals(Position(2, 3), vm.state.level.start.position)
    }

    @Test
    fun `a tap with the finish tool on the start changes nothing`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.FINISH)
        vm.onCellTap(Position(0, 2))
        assertEquals(Position(6, 2), vm.state.level.finish.position)
        assertFalse(vm.state.dirty)
    }

    @Test
    fun `a block drag paints every cell to the opposite of the first one`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.BLOCK)
        vm.onCellTap(Position(3, 1))
        vm.onDragStart(Position(1, 1))
        listOf(2, 3, 4).forEach { vm.onDrag(Position(it, 1)) }
        vm.onDragEnd()
        assertEquals(setOf(Position(1, 1), Position(2, 1), Position(3, 1), Position(4, 1)), vm.state.level.blocked.toSet())

        vm.onDragStart(Position(2, 1))
        vm.onDrag(Position(3, 1))
        vm.onDragEnd()
        assertEquals(listOf(Position(1, 1), Position(4, 1)), vm.state.level.blocked)
    }

    @Test
    fun `steppers stop at the grid limits`() {
        val vm = viewModel()
        repeat(5) { vm.changeColumns(-1) }
        assertEquals(4, vm.state.level.grid.columns)
        assertFalse(vm.state.canShrinkColumns)
        assertTrue(vm.state.canGrowColumns)
        repeat(30) { vm.changeColumns(+1) }
        assertEquals(25, vm.state.level.grid.columns)
        assertFalse(vm.state.canGrowColumns)
        repeat(30) { vm.changeRows(+1) }
        assertEquals(15, vm.state.level.grid.rows)
        assertFalse(vm.state.canGrowRows)
    }

    @Test
    fun `a number updates the draft`() {
        val vm = viewModel()
        vm.setNumber(NumberField.CellsPerSecond, "0.8")
        vm.setNumber(NumberField.Weight(PipeSection.X), "1")
        assertEquals(0.8, vm.state.level.flow.cellsPerSecond, 0.0)
        assertEquals(1, vm.state.level.stack.weightOf(PipeSection.X))
        assertFalse(vm.state.isInvalid(NumberField.CellsPerSecond))
        assertTrue(vm.state.dirty)
    }

    @Test
    fun `text that is not a number is marked invalid and keeps the draft`() {
        val vm = viewModel()
        vm.setNumber(NumberField.InitialDelay, "abc")
        assertEquals(20_000L, vm.state.level.flow.initialDelayMs)
        assertTrue(vm.state.isInvalid(NumberField.InitialDelay))
        assertEquals("abc", vm.state.text(NumberField.InitialDelay))
        assertFalse(vm.state.dirty)

        vm.setNumber(NumberField.InitialDelay, "5000")
        assertFalse(vm.state.isInvalid(NumberField.InitialDelay))
        assertEquals(5_000L, vm.state.level.flow.initialDelayMs)
    }

    @Test
    fun `untouched fields show the draft's values`() {
        val state = viewModel().state
        assertEquals("5", state.text(NumberField.Id))
        assertEquals("0.4", state.text(NumberField.CellsPerSecond))
        assertEquals("7", state.text(NumberField.Weight(PipeSection.H)))
    }

    @Test
    fun `name, open side and rules are edited`() {
        val vm = viewModel()
        vm.setName("Drain")
        vm.setOpenSide(EndpointKind.FINISH, Side.TOP)
        vm.setAllowReplace(false)
        vm.setNumber(NumberField.ReplacePenalty, "200")
        val level = vm.state.level
        assertEquals("Drain", level.name)
        assertEquals(Side.TOP, level.finish.openSide)
        assertFalse(level.rules.allowReplace)
        assertEquals(200L, level.rules.replacePenaltyMs)
    }

    @Test
    fun `export hands the writer's output to the exporter and clears dirty`() {
        val vm = viewModel()
        vm.setName("Drain")
        assertTrue(vm.export())
        assertEquals(listOf("level_005.json" to LevelWriter.write(vm.state.level)), exporter.exports)
        assertFalse(vm.state.dirty)
        val parsed = (LevelParser.parse(exporter.exports.single().second) as ParseResult.Success).level
        assertEquals(vm.state.level, parsed)
    }

    @Test
    fun `export and test play are refused for an invalid design`() {
        val vm = viewModel()
        vm.setOpenSide(EndpointKind.START, Side.LEFT)
        assertTrue(vm.state.status is DesignStatus.Invalid)
        assertFalse(vm.state.canPlay)
        assertFalse(vm.export())
        assertNull(vm.testPlayLevel())
        assertTrue(exporter.exports.isEmpty())
        assertTrue(vm.state.dirty)
    }

    @Test
    fun `export and test play are refused for an unsolvable design`() {
        val vm = viewModel()
        vm.wallOffFinish()
        assertTrue(vm.state.status is DesignStatus.Unsolvable)
        assertFalse(vm.export())
        assertNull(vm.testPlayLevel())
    }

    @Test
    fun `test play returns the designed level when it is solvable`() {
        val vm = viewModel()
        vm.changeColumns(+1)
        assertEquals(vm.state.level, vm.testPlayLevel())
    }

    @Test
    fun `new blank replaces the design and clears dirty`() {
        val vm = viewModel()
        vm.copyFrom(3)
        vm.changeRows(+1)
        vm.setNumber(NumberField.InitialDelay, "x")
        vm.newBlank()
        assertEquals(LevelDraft.new(5), vm.state.draft)
        assertFalse(vm.state.dirty)
        assertFalse(vm.state.isInvalid(NumberField.InitialDelay))
    }

    // Mandatory sections

    @Test
    fun `the mandatory tools place, change and remove sections`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.MANDATORY_CROSS)
        vm.onCellTap(Position(3, 2))
        assertEquals(listOf(MandatorySection(Position(3, 2), PipeSection.X)), vm.state.level.mandatory)
        assertTrue(vm.state.dirty)

        vm.selectTool(DesignerTool.MANDATORY_RESERVOIR)
        vm.onCellTap(Position(3, 2))
        assertEquals(listOf(MandatorySection(Position(3, 2), PipeSection.RES)), vm.state.level.mandatory)

        vm.onCellTap(Position(3, 2))
        assertEquals(emptyList<MandatorySection>(), vm.state.level.mandatory)
    }

    @Test
    fun `a mandatory tool drag only acts on the cell it starts on`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.MANDATORY_CROSS)
        vm.onDragStart(Position(1, 1))
        listOf(2, 3, 4).forEach { vm.onDrag(Position(it, 1)) }
        vm.onDragEnd()
        assertEquals(listOf(MandatorySection(Position(1, 1), PipeSection.X)), vm.state.level.mandatory)
    }

    @Test
    fun `a mandatory tool tap on the start changes nothing`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.MANDATORY_RESERVOIR)
        vm.onCellTap(Position(0, 2))
        assertEquals(emptyList<MandatorySection>(), vm.state.level.mandatory)
        assertFalse(vm.state.dirty)
    }

    @Test
    fun `mandatory sections are exported`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.MANDATORY_CROSS)
        vm.onCellTap(Position(3, 2))
        assertTrue(vm.export())
        val parsed = (LevelParser.parse(exporter.exports.single().second) as ParseResult.Success).level
        assertEquals(listOf(MandatorySection(Position(3, 2), PipeSection.X)), parsed.mandatory)
        assertEquals(vm.state.level, parsed)
        assertEquals(vm.state.level, vm.testPlayLevel())
    }

    @Test
    fun `mandatory sections and the mandatory tool are restored`() {
        val saved = SavedStateHandle()
        val first = viewModel(saved)
        first.selectTool(DesignerTool.MANDATORY_RESERVOIR)
        first.onCellTap(Position(2, 1))
        val restored = viewModel(saved).state
        assertEquals(listOf(MandatorySection(Position(2, 1), PipeSection.RES)), restored.level.mandatory)
        assertEquals(DesignerTool.MANDATORY_RESERVOIR, restored.tool)
    }

    // Star thresholds

    @Test
    fun `stars follow the grid`() {
        val vm = viewModel()
        assertEquals(StarBenchmarks(200, 250, 330), vm.state.level.stars)
        vm.selectTool(DesignerTool.BLOCK)
        vm.onCellTap(Position(3, 1))
        assertEquals(StarBenchmarks.forFreeCells(32), vm.state.level.stars)
        vm.changeColumns(+1)
        assertEquals(StarBenchmarks.forFreeCells(37), vm.state.level.stars)
    }

    @Test
    fun `a copied level gets calculated stars and keeps its mandatory sections`() {
        val handWritten = bundled[0].copy(
            id = 9,
            stars = StarBenchmarks(1, 2, 3),
            mandatory = listOf(MandatorySection(Position(2, 2), PipeSection.X)),
        )
        val vm = LevelDesignerViewModel(FakeLevels(listOf(handWritten)), exporter, SavedStateHandle())
        vm.copyFrom(9)
        assertEquals(handWritten.mandatory, vm.state.level.mandatory)
        assertEquals(StarBenchmarks.forFreeCells(handWritten.freeCells), vm.state.level.stars)
    }

    @Test
    fun `state is restored from the saved state handle`() {
        val saved = SavedStateHandle()
        val first = viewModel(saved)
        first.copyFrom(2)
        first.selectTool(DesignerTool.BLOCK)
        first.onCellTap(Position(5, 4))
        val restored = viewModel(saved).state
        assertEquals(first.state.level, restored.level)
        assertEquals(DesignerTool.BLOCK, restored.tool)
        assertTrue(restored.dirty)
    }

    @Test
    fun `an oversized design is shrunk to fit when restored`() {
        val big = LevelDraft.new(5).level.copy(grid = GridSize(24, 24), finish = Endpoint(Position(6, 23), Side.TOP))
        val saved = SavedStateHandle(mapOf(LevelDesignerViewModel.KEY_DRAFT to LevelWriter.write(big)))
        val restored = viewModel(saved).state.level
        assertEquals(GridSize(24, 15), restored.grid)
        assertEquals(Endpoint(Position(6, 14), Side.TOP), restored.finish)
    }

    @Test
    fun `an invalid draft is restored too`() {
        val saved = SavedStateHandle()
        val first = viewModel(saved)
        first.setOpenSide(EndpointKind.START, Side.LEFT)
        assertEquals(first.state.level, viewModel(saved).state.level)
    }

    @Test
    fun `choosing a reservoir type sets the flow factor and marks the design dirty`() {
        for (type in ReservoirType.entries) {
            val vm = viewModel()
            vm.setReservoirType(type)
            assertEquals(type.factor, vm.state.level.flow.reservoirSpeedFactor, 0.0)
            assertEquals(type, vm.state.draft.reservoirType)
            assertEquals(type != ReservoirType.PURPLE_30, vm.state.dirty)
        }
    }

    @Test
    fun `a restored reservoir speed factor snaps to the nearest type`() {
        val level = LevelDraft.new(5).level
        val saved = SavedStateHandle(
            mapOf(LevelDesignerViewModel.KEY_DRAFT to LevelWriter.write(level.copy(flow = level.flow.copy(reservoirSpeedFactor = 0.5)))),
        )
        val restored = viewModel(saved).state
        assertEquals(ReservoirType.ORANGE_40, restored.draft.reservoirType)
        assertEquals(0.4, restored.level.flow.reservoirSpeedFactor, 0.0)
    }

    @Test
    fun `a chosen reservoir type is restored`() {
        val saved = SavedStateHandle()
        viewModel(saved).setReservoirType(ReservoirType.RED_10)
        assertEquals(ReservoirType.RED_10, viewModel(saved).state.draft.reservoirType)
    }

    @Test
    fun `file name is padded to three digits`() {
        assertEquals("level_005.json", LevelDesignerViewModel.fileName(5))
        assertEquals("level_123.json", LevelDesignerViewModel.fileName(123))
    }

    // Portals (the blank design: 7 by 5, start at 0,2 open right, finish at 6,2 open left)

    private fun LevelDesignerViewModel.placePair() {
        selectTool(DesignerTool.PORTAL)
        onCellTap(Position(1, 1))
        onCellTap(Position(6, 3))
    }

    @Test
    fun `the portal tool places a pair with two taps`() {
        val vm = viewModel()
        vm.placePair()
        assertEquals(
            listOf(PortalPair(Endpoint(Position(1, 1), Side.RIGHT), Endpoint(Position(6, 3), Side.LEFT))),
            vm.state.level.portals,
        )
        assertTrue(vm.state.dirty)
        assertTrue(vm.state.canPlay)
    }

    @Test
    fun `an unfinished pair disables export and test play`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.PORTAL)
        vm.onCellTap(Position(1, 1))
        val status = vm.state.status as DesignStatus.Invalid
        assertEquals(listOf("portals[0].b"), status.errors.map { it.field })
        assertTrue(Position(1, 1) in status.highlightedCells)
        assertFalse(vm.state.canPlay)
        assertFalse(vm.export())
        assertNull(vm.testPlayLevel())
    }

    @Test
    fun `selecting a portal does not mark the design dirty, moving it does`() {
        val vm = viewModel()
        vm.placePair()
        vm.export()
        vm.onCellTap(Position(6, 3))
        assertEquals(Position(6, 3), vm.state.draft.selectedPortal)
        assertFalse(vm.state.dirty)
        vm.onCellTap(Position(5, 4))
        assertEquals(Position(5, 4), vm.state.level.portals.single().b.position)
        assertTrue(vm.state.dirty)
    }

    @Test
    fun `the open side of the selected portal is set and its pair removed`() {
        val vm = viewModel()
        vm.placePair()
        vm.onCellTap(Position(1, 1))
        vm.setPortalOpenSide(Side.TOP)
        assertEquals(Side.TOP, vm.state.level.portals.single().a.openSide)
        vm.removeSelectedPair()
        assertEquals(emptyList<PortalPair>(), vm.state.level.portals)
    }

    @Test
    fun `a portal tool drag only acts on the cell it starts on`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.PORTAL)
        vm.onDragStart(Position(1, 1))
        vm.onDrag(Position(2, 1))
        vm.onDrag(Position(3, 1))
        vm.onDragEnd()
        assertEquals(Endpoint(Position(1, 1), Side.RIGHT), vm.state.draft.unpaired)
        assertEquals(emptyList<PortalPair>(), vm.state.level.portals)
    }

    @Test
    fun `portals are exported and test played`() {
        val vm = viewModel()
        vm.placePair()
        assertTrue(vm.export())
        val json = exporter.exports.single().second
        val parsed = (LevelParser.parse(json) as ParseResult.Success).level
        assertEquals(vm.state.level, parsed)
        assertEquals(vm.state.level.portals, vm.testPlayLevel()?.portals)
    }

    @Test
    fun `portals, the unpaired and the selected portal are restored`() {
        val saved = SavedStateHandle()
        val first = viewModel(saved)
        first.placePair()
        first.onCellTap(Position(2, 4))
        first.onCellTap(Position(6, 3))
        val restored = viewModel(saved).state
        assertEquals(first.state.level.portals, restored.level.portals)
        assertEquals(Endpoint(Position(2, 4), Side.RIGHT), restored.draft.unpaired)
        assertEquals(Position(6, 3), restored.draft.selectedPortal)
        assertEquals(DesignerTool.PORTAL, restored.tool)
        assertEquals(first.state.draft, restored.draft)
    }

    // Leaky tiles

    @Test
    fun `the leaky tile tool toggles a cell and a drag paints the cells it passes`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.LEAKY)
        vm.onCellTap(Position(3, 1))
        assertEquals(listOf(Position(3, 1)), vm.state.level.leaky)
        assertTrue(vm.state.dirty)
        vm.onCellTap(Position(3, 1))
        assertEquals(emptyList<Position>(), vm.state.level.leaky)

        vm.onDragStart(Position(1, 3))
        vm.onDrag(Position(2, 3))
        vm.onDrag(Position(3, 3))
        vm.onDragEnd()
        assertEquals(listOf(Position(1, 3), Position(2, 3), Position(3, 3)), vm.state.level.leaky)

        vm.clearLeaky()
        assertEquals(emptyList<Position>(), vm.state.level.leaky)
    }

    @Test
    fun `leaky tiles are copied, exported and test played`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.LEAKY)
        vm.onCellTap(Position(3, 2))
        assertTrue(vm.export())
        val parsed = (LevelParser.parse(exporter.exports.single().second) as ParseResult.Success).level
        assertEquals(listOf(Position(3, 2)), parsed.leaky)
        assertEquals(vm.state.level, parsed)
        assertEquals(listOf(Position(3, 2)), vm.testPlayLevel()?.leaky)

        val copied = LevelDesignerViewModel(FakeLevels(listOf(parsed.copy(id = 1))), exporter, SavedStateHandle())
        copied.copyFrom(1)
        assertEquals(listOf(Position(3, 2)), copied.state.level.leaky)
    }

    @Test
    fun `every route over a leaky tile keeps export and test play enabled`() {
        val vm = viewModel()
        vm.selectTool(DesignerTool.BLOCK)
        vm.onDragStart(Position(3, 0))
        (1..4).forEach { vm.onDrag(Position(3, it)) }
        vm.onDragEnd()
        vm.selectTool(DesignerTool.LEAKY)
        vm.onCellTap(Position(3, 2))
        assertEquals(false, (vm.state.status as DesignStatus.Solvable).leakFree)
        assertTrue(vm.state.canPlay)
    }

    @Test
    fun `leaky tiles and the leaky tile tool are restored`() {
        val saved = SavedStateHandle()
        val first = viewModel(saved)
        first.selectTool(DesignerTool.LEAKY)
        first.onCellTap(Position(2, 1))
        first.onCellTap(Position(4, 3))
        val restored = viewModel(saved).state
        assertEquals(listOf(Position(2, 1), Position(4, 3)), restored.level.leaky)
        assertEquals(DesignerTool.LEAKY, restored.tool)
        assertEquals(first.state.draft, restored.draft)
    }
}
