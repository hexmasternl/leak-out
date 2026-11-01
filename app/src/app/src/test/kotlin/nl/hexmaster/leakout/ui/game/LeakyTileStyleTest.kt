package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeakyTileStyleTest {
    private val at = Position(3, 2)
    private val dry = Cell.Pipe(PipeSection.H)
    private val entered = Cell.Pipe(PipeSection.H, liquidEntered = true)

    @Test
    fun `an empty or dry leaky tile is damp`() {
        assertEquals(LeakyWetness.DAMP, LeakyTileStyle.wetness(Cell.Empty, at, head = null, reducedMotion = false))
        assertEquals(LeakyWetness.DAMP, LeakyTileStyle.wetness(dry, at, head = null, reducedMotion = false))
    }

    @Test
    fun `drops fall while the head is in the section`() {
        assertEquals(LeakyWetness.DRIPPING, LeakyTileStyle.wetness(entered, at, head = at, reducedMotion = false))
    }

    @Test
    fun `the tile keeps a wet stain once the head has left`() {
        assertEquals(LeakyWetness.WET, LeakyTileStyle.wetness(entered, at, head = Position(4, 2), reducedMotion = false))
        assertEquals(LeakyWetness.WET, LeakyTileStyle.wetness(entered, at, head = null, reducedMotion = false))
    }

    @Test
    fun `with reduced motion there are no drops and the stain shows as soon as the head enters`() {
        assertEquals(LeakyWetness.WET, LeakyTileStyle.wetness(entered, at, head = at, reducedMotion = true))
        assertEquals(LeakyWetness.DAMP, LeakyTileStyle.wetness(dry, at, head = null, reducedMotion = true))
    }

    @Test
    fun `the wet stain is darker than the damp one`() {
        assertEquals(0.25f, LeakyTileStyle.stainAlpha(LeakyWetness.DAMP), 1e-6f)
        assertEquals(0.25f, LeakyTileStyle.stainAlpha(LeakyWetness.DRIPPING), 1e-6f)
        assertEquals(0.45f, LeakyTileStyle.stainAlpha(LeakyWetness.WET), 1e-6f)
    }

    @Test
    fun `three drops fall 600 ms each, evenly staggered`() {
        val start = LeakyTileStyle.drops(0L)
        assertEquals(3, start.size)
        assertEquals(0f, start[0], 1e-6f)
        assertEquals(200f / 600f, start[1], 1e-6f)
        assertEquals(400f / 600f, start[2], 1e-6f)
        assertEquals(0.5f, LeakyTileStyle.drops(300L)[0], 1e-6f)
    }

    @Test
    fun `drops loop on the game clock and stay within the fall`() {
        assertEquals(LeakyTileStyle.drops(150L), LeakyTileStyle.drops(150L + LeakyTileStyle.DROP_FALL_MS * 7))
        for (now in 0L..1_200L step 37) {
            assertTrue(LeakyTileStyle.drops(now).all { it >= 0f && it < 1f })
        }
    }

    @Test
    fun `the drops stand still while the game clock does`() {
        assertEquals(LeakyTileStyle.drops(4_321L), LeakyTileStyle.drops(4_321L))
    }
}
