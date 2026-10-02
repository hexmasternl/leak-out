package nl.hexmaster.leakout.ui.game

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.game.onboarding.TipTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Panning a tip's target into view, and which stack slots count as fully visible. */
class FieldRevealTest {
    private val density = Density(1f)
    // Narrower than the 16 columns at the 40 dp minimum (640 dp), so the field pans both ways.
    private val field = Size(500f, 312f)
    private val large = GridSize(16, 12)

    private fun startView() = FieldViewport.initial(field, large, Position(0, 5), density)

    private fun FieldViewport.fieldRect() = Rect(0f, 0f, fieldSize.width, fieldSize.height)

    @Test
    fun `the finish of a 16 x 12 level is panned into view`() {
        val start = startView()
        val finish = Position(15, 6)
        assertTrue(start.cellRect(finish).left >= field.width)
        val revealed = start.revealCell(finish, marginPx = 16f)
        val rect = revealed.cellRect(finish)
        // At the grid edge the pan limits win over the margin, but the whole cell is in view.
        assertTrue("$rect is inside", rect.left >= 0f && rect.right <= field.width + 0.01f)
        assertTrue(rect.top >= 0f && rect.bottom <= field.height)
        assertEquals(start.cellPx, revealed.cellPx, 0f)
    }

    @Test
    fun `a cell already in view does not move the view`() {
        val start = startView()
        assertEquals(start, start.revealCell(Position(1, 5), marginPx = 16f))
    }

    @Test
    fun `revealing stays within the pan limits`() {
        val revealed = startView().revealCell(Position(15, 11), marginPx = 100f)
        assertEquals(field.width - revealed.cellPx * large.columns, revealed.offset.x, 0.01f)
        assertEquals(field.height - revealed.cellPx * large.rows, revealed.offset.y, 0.01f)
    }

    @Test
    fun `the view after the tour is the level-start view`() {
        val start = startView()
        val panned = start.revealCell(Position(15, 6), 16f)
        val back = FieldViewport.initial(panned.fieldSize, large, Position(0, 5), density)
        assertEquals(start, back)
    }

    @Test
    fun `only slots wholly inside the visible stack panel are fully visible`() {
        val visible = Rect(0f, 0f, 80f, 300f)
        val slots = mapOf(
            0 to Rect(10f, -20f, 50f, 20f), // cut off at the top
            1 to Rect(10f, 0f, 50f, 40f), // exactly at the top edge
            2 to Rect(10f, 260f, 60f, 310f), // reaches past the bottom
            3 to Rect(10f, 100f, 50f, 140f),
        )
        assertEquals(setOf(1, 3), TipTargets.fullyVisible(visible, slots))
    }
}
