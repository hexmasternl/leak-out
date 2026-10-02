package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Placement on the smallest screen, 640 × 360 dp, with the 296 × 222 dp card of design-system.md 5.9. */
class CalloutPlacementTest {
    private val window = Size(640f, 360f)
    private val card = Size(296f, 222f)
    private val gutter = 16f
    private val gap = 12f

    private fun place(spotlight: Rect) = CalloutPlacement.place(window, spotlight, card, gutter, gap)

    private fun cardRect(position: CalloutPosition) = Rect(position.topLeft, card)

    private fun assertOnScreen(position: CalloutPosition) {
        val rect = cardRect(position)
        assertTrue("$rect inside the gutter", rect.left >= gutter && rect.top >= gutter && rect.right <= window.width - gutter && rect.bottom <= window.height - gutter)
    }

    @Test
    fun `a target in the left stack opens to the right, centred on it`() {
        val stack = Rect(4f, 52f, 96f, 356f)
        val position = place(stack)
        assertEquals(Offset(108f, 93f), position.topLeft)
        assertEquals(TipPointer(TipPointerSide.Left, 111f), position.pointer)
        assertOnScreen(position)
    }

    @Test
    fun `a centre cell of the field opens to the left, the only side with room`() {
        // The field runs from x 106 to 628 dp beside the stack; its centre cell is 342..392, plus 8 dp spotlight padding.
        val cell = Rect(334f, 172f, 400f, 238f)
        val position = place(cell)
        assertEquals(TipPointer(TipPointerSide.Right, 111f), position.pointer)
        assertEquals(cell.left - gap - card.width, position.topLeft.x)
        assertOnScreen(position)
        assertFalse(cardRect(position).overlaps(cell))
    }

    @Test
    fun `a target with no side wide enough gets the card in the other half without a pointer`() {
        val cell = Rect(300f, 172f, 366f, 238f)
        val position = place(cell)
        assertNull(position.pointer)
        assertEquals(gutter, position.topLeft.x)
        assertOnScreen(position)
    }

    @Test
    fun `a centre cell on a larger screen opens to the right`() {
        val position = CalloutPlacement.place(Size(1280f, 800f), Rect(600f, 380f, 666f, 446f), card, gutter, gap)
        assertEquals(TipPointerSide.Left, position.pointer?.side)
        assertEquals(678f, position.topLeft.x)
    }

    @Test
    fun `a bottom-right cell opens to the left and is kept on screen`() {
        val cell = Rect(558f, 280f, 624f, 346f)
        val position = place(cell)
        assertEquals(TipPointerSide.Right, position.pointer?.side)
        assertEquals(cell.left - gap - card.width, position.topLeft.x)
        // Centring on the cell would push the card below the gutter, so it is shifted up.
        assertEquals(window.height - gutter - card.height, position.topLeft.y)
        assertOnScreen(position)
        assertFalse(cardRect(position).overlaps(cell))
    }

    @Test
    fun `a target at the top gets the card below it when only that side fits`() {
        val wide = Rect(16f, 16f, 624f, 100f)
        val position = place(wide)
        assertEquals(TipPointerSide.Top, position.pointer?.side)
        assertEquals(112f, position.topLeft.y)
    }
}
