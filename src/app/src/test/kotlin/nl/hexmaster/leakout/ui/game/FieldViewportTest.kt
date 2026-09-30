package nl.hexmaster.leakout.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldViewportTest {
    // With density 1, dp and px are the same, so the numbers read as dp.
    private val density = Density(1f)
    private val large = GridSize(16, 12)
    private val largeField = Size(700f, 312f)

    private fun largeView(start: Position = Position(0, 5)) = FieldViewport.initial(largeField, large, start, density)

    @Test
    fun `a small level fits and is centred`() {
        val view = FieldViewport.initial(Size(600f, 300f), GridSize(8, 6), Position(0, 0), density)
        assertEquals(50f, view.cellPx, 0.001f)
        assertEquals(Offset(100f, 0f), view.offset)
        assertEquals(50f, view.minCellPx, 0.001f)
    }

    @Test
    fun `the limits scale with the density`() {
        val view = FieldViewport.initial(Size(1575f, 819f), large, Position(0, 5), Density(2.625f))
        assertEquals(105f, view.cellPx, 0.001f)
        assertEquals(315f, view.maxCellPx, 0.001f)
    }

    @Test
    fun `a tiny grid is capped at 120 and cannot zoom`() {
        val view = FieldViewport.initial(Size(600f, 300f), GridSize(2, 2), Position(0, 0), density)
        assertEquals(120f, view.cellPx, 0.001f)
        assertEquals(Offset(180f, 30f), view.offset)
        assertEquals(view, view.zoomBy(2f, Offset(300f, 150f)))
        assertEquals(view, view.zoomBy(0.5f, Offset(300f, 150f)))
    }

    @Test
    fun `a large level starts at 40 with the start cell visible`() {
        for (start in listOf(Position(0, 5), Position(0, 0), Position(15, 11), Position(8, 11))) {
            val view = largeView(start)
            assertEquals(40f, view.cellPx, 0.001f)
            val rect = view.cellRect(start)
            val field = Rect(Offset.Zero, largeField)
            assertTrue("start $start in view", field.contains(rect.topLeft) && rect.right <= field.right && rect.bottom <= field.bottom)
        }
    }

    @Test
    fun `a large level centres the start as far as the edges allow`() {
        assertEquals(Offset(30f, -64f), largeView(Position(0, 5)).offset)
        assertEquals(Offset(30f, -168f), largeView(Position(15, 11)).offset)
        assertEquals(Offset(30f, 0f), largeView(Position(0, 0)).offset)
    }

    @Test
    fun `zoom stops at the minimum and maximum cell size`() {
        val view = largeView()
        assertEquals(40f, view.zoomBy(0.1f, Offset(350f, 156f)).cellPx, 0.001f)
        assertEquals(120f, view.zoomBy(100f, Offset(350f, 156f)).cellPx, 0.001f)
        assertEquals(120f, view.zoomBy(2f, Offset.Zero).zoomBy(2f, Offset.Zero).cellPx, 0.001f)
    }

    @Test
    fun `zoom keeps the focus on the same grid point`() {
        val view = largeView()
        val focus = Offset(350f, 156f)
        val gridPoint = (focus - view.offset) / view.cellPx
        val zoomed = view.zoomBy(1.5f, focus)
        assertEquals(60f, zoomed.cellPx, 0.001f)
        val after = zoomed.offset + gridPoint * zoomed.cellPx
        assertEquals(focus.x, after.x, 0.01f)
        assertEquals(focus.y, after.y, 0.01f)
    }

    @Test
    fun `pan is clamped at every edge`() {
        val view = largeView().zoomBy(3f, Offset.Zero)
        val contentWidth = 120f * 16
        val contentHeight = 120f * 12
        assertEquals(Offset(0f, 0f), view.panBy(Offset(10_000f, 10_000f)).offset)
        assertEquals(Offset(700f - contentWidth, 312f - contentHeight), view.panBy(Offset(-10_000f, -10_000f)).offset)
        val moved = view.panBy(Offset(-50f, -40f))
        assertEquals(view.offset + Offset(-50f, -40f), moved.offset)
    }

    @Test
    fun `an axis that fits stays centred and does not pan`() {
        val view = largeView()
        val panned = view.panBy(Offset(-200f, -50f))
        assertEquals(30f, panned.offset.x, 0.001f)
        assertEquals(view.offset.y - 50f, panned.offset.y, 0.001f)

        val small = FieldViewport.initial(Size(600f, 300f), GridSize(8, 6), Position(0, 0), density)
        assertEquals(small, small.panBy(Offset(-80f, 90f)))
    }

    @Test
    fun `zooming out after a pan to a corner re-clamps`() {
        val cornered = largeView().zoomBy(3f, Offset.Zero).panBy(Offset(-10_000f, -10_000f))
        val out = cornered.zoomBy(0.01f, Offset(350f, 156f))
        assertEquals(40f, out.cellPx, 0.001f)
        assertEquals(30f, out.offset.x, 0.001f)
        assertEquals(312f - 480f, out.offset.y, 0.001f)
    }

    @Test
    fun `cellAt maps field points to cells`() {
        val view = largeView()
        assertEquals(Position(0, 1), view.cellAt(Offset(31f, 0f)))
        assertEquals(Position(2, 5), view.cellAt(Offset(30f + 2 * 40f + 5f, -64f + 5 * 40f + 39f)))
    }

    @Test
    fun `cellAt follows a pan and a zoom`() {
        val view = largeView().zoomBy(2f, Offset(350f, 156f)).panBy(Offset(-100f, -30f))
        for (position in listOf(Position(5, 4), Position(8, 6), Position(10, 5))) {
            val rect = view.cellRect(position)
            if (rect.center.x in 0f..700f && rect.center.y in 0f..312f) {
                assertEquals(position, view.cellAt(rect.center))
            }
        }
        assertEquals(80f, view.cellPx, 0.001f)
    }

    @Test
    fun `cellAt is null outside the grid and outside the field`() {
        val view = largeView()
        assertNull(view.cellAt(Offset(10f, 100f)))
        assertNull(view.cellAt(Offset(695f, 100f)))
        assertNull(view.cellAt(Offset(100f, -1f)))
        assertNull(view.cellAt(Offset(100f, 312f)))
        assertNull(view.cellAt(Offset(-5f, 100f)))
    }

    @Test
    fun `visible cells cover only what is in the field`() {
        val view = largeView()
        val visible = view.visibleCells()
        assertTrue(visible.all { it.y in 1..9 })
        assertEquals(16 * 9, visible.size)
    }
}
