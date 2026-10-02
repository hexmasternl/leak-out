package nl.hexmaster.leakout.ui.designer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesignerGridLayoutTest {
    @Test
    fun `cells fit the shorter side and the grid is centred`() {
        val layout = DesignerGridLayout.compute(Size(800f, 400f), GridSize(7, 5))
        assertEquals(80f, layout.cellPx, 0f)
        assertEquals(Offset(120f, 0f), layout.origin)
    }

    @Test
    fun `a point maps to the cell under it`() {
        val layout = DesignerGridLayout.compute(Size(800f, 400f), GridSize(7, 5))
        assertEquals(Position(0, 0), layout.cellAt(Offset(121f, 1f)))
        assertEquals(Position(6, 4), layout.cellAt(Offset(679f, 399f)))
        assertEquals(Position(3, 2), layout.cellAt(layout.cellRect(Position(3, 2)).center))
    }

    @Test
    fun `points outside the grid map to no cell`() {
        val layout = DesignerGridLayout.compute(Size(800f, 400f), GridSize(7, 5))
        assertNull(layout.cellAt(Offset(119f, 10f)))
        assertNull(layout.cellAt(Offset(681f, 10f)))
    }
}
