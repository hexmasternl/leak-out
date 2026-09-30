package nl.hexmaster.leakout.lab

import java.io.File
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.pipes.PipeArtLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PipeLabStateTest {
    private val library = PipeArtLibrary { name -> File("src/main/assets", name).inputStream() }
    private val state = PipeLabState(PipeSection.entries.associateWith { library.art(it).channels })

    private fun main(section: PipeSection) = state.progress(section, "channel-main")

    /** Ticks in 60 fps frames, like the frame loop. */
    private fun run(ms: Float) {
        var left = ms
        while (left > 0f) {
            val dt = minOf(left, 1000f / 60f)
            state.tick(dt)
            left -= dt
        }
    }

    @Test
    fun `defaults`() {
        assertFalse(state.running)
        assertEquals(1f, state.flowSpeed)
        assertEquals(0.3f, state.reservoirSpeedFactor)
        assertEquals(Side.LEFT, state.entry(PipeSection.H, "channel-main"))
        assertEquals(Side.TOP, state.entry(PipeSection.X, "channel-v"))
        assertTrue(PipeSection.entries.all { state.fills(it).isEmpty() })
    }

    @Test
    fun `nothing moves before start and while paused`() {
        state.tick(500f)
        assertEquals(0f, main(PipeSection.H))
        state.start()
        state.tick(250f)
        state.pause()
        state.tick(500f)
        assertEquals(0.25f, main(PipeSection.H), 1e-5f)
        state.start()
        state.tick(250f)
        assertEquals(0.5f, main(PipeSection.H), 1e-5f)
    }

    @Test
    fun `reset empties everything and stops`() {
        state.start()
        run(3000f)
        state.reset()
        assertFalse(state.running)
        assertTrue(PipeSection.entries.all { state.fills(it).isEmpty() })
    }

    @Test
    fun `a straight fills in half a second at 2 cells per second`() {
        state.flowSpeed = 2f
        state.start()
        run(250f)
        assertEquals(0.5f, main(PipeSection.H), 1e-4f)
        run(250f)
        assertEquals(1f, main(PipeSection.H), 1e-4f)
        run(1000f)
        assertEquals("a full tile stays full", 1f, main(PipeSection.H))
    }

    @Test
    fun `the reservoir is slower by its factor`() {
        state.reservoirSpeedFactor = 0.25f
        state.start()
        run(1000f)
        assertEquals(1f, main(PipeSection.V), 1e-4f)
        assertEquals(0.25f, main(PipeSection.RES), 1e-4f)
        run(3000f)
        assertEquals(1f, main(PipeSection.RES), 1e-4f)
    }

    @Test
    fun `changing the speed mid flow continues from the current progress`() {
        state.start()
        run(500f)
        assertEquals(0.5f, main(PipeSection.LT), 1e-4f)
        state.flowSpeed = 2f
        assertEquals(0.5f, main(PipeSection.LT), 1e-4f)
        run(125f)
        assertEquals(0.75f, main(PipeSection.LT), 1e-4f)
    }

    @Test
    fun `the cross fills horizontal first then vertical`() {
        state.start()
        run(500f)
        assertEquals(0.5f, state.progress(PipeSection.X, "channel-h"), 1e-4f)
        assertEquals(0f, state.progress(PipeSection.X, "channel-v"))
        run(1000f)
        assertEquals(1f, state.progress(PipeSection.X, "channel-h"))
        assertEquals(0.5f, state.progress(PipeSection.X, "channel-v"), 1e-4f)
        // One big frame carries the leftover from one channel to the next.
        state.reset()
        state.start()
        state.tick(1250f)
        assertEquals(0.25f, state.progress(PipeSection.X, "channel-v"), 1e-4f)
    }

    @Test
    fun `changing a direction empties only that tile`() {
        state.start()
        run(500f)
        state.reverse(PipeSection.H, "channel-main")
        assertEquals(Side.RIGHT, state.entry(PipeSection.H, "channel-main"))
        assertEquals(0f, main(PipeSection.H))
        assertEquals(0.5f, main(PipeSection.V), 1e-4f)
        run(250f)
        assertEquals(Side.RIGHT, state.fills(PipeSection.H).getValue("channel-main").entry)
    }

    @Test
    fun `cross directions are set separately`() {
        state.setEntry(PipeSection.X, "channel-h", Side.RIGHT)
        state.setEntry(PipeSection.X, "channel-v", Side.BOTTOM)
        assertEquals(Side.RIGHT, state.entry(PipeSection.X, "channel-h"))
        assertEquals(Side.BOTTOM, state.entry(PipeSection.X, "channel-v"))
        state.reverse(PipeSection.X, "channel-v")
        assertEquals(Side.RIGHT, state.entry(PipeSection.X, "channel-h"))
        assertEquals(Side.TOP, state.entry(PipeSection.X, "channel-v"))
    }

    @Test
    fun `entry must be a side of the channel and speeds stay in range`() {
        assertThrows(IllegalArgumentException::class.java) { state.setEntry(PipeSection.H, "channel-main", Side.TOP) }
        assertThrows(IllegalArgumentException::class.java) { state.setEntry(PipeSection.H, "channel-h", Side.LEFT) }
        state.flowSpeed = 10f
        assertEquals(4f, state.flowSpeed)
        state.flowSpeed = 0f
        assertEquals(0.1f, state.flowSpeed)
        state.reservoirSpeedFactor = 0f
        assertEquals(0.1f, state.reservoirSpeedFactor)
    }
}
