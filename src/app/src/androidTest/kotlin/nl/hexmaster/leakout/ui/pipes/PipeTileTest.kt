package nl.hexmaster.leakout.ui.pipes

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PipeTileTest {
    @get:Rule
    val compose = createComposeRule()

    private var fills by mutableStateOf<Map<String, ChannelFill>>(emptyMap())

    private fun show(section: PipeSection) {
        compose.setContent { PipeTile(section, Modifier.size(120.dp), fills) }
    }

    private fun pixels(section: PipeSection): PixelMap =
        compose.onNodeWithTag(pipeTileTag(section)).captureToImage().toPixelMap()

    /** Liquid is the saturated aqua; steel, bore, tank and badge colours all fail this. */
    private fun Color.isLiquid() = red < 0.5f && green > 0.45f && blue > 0.6f && blue - red > 0.35f

    /** Liquid pixels in the box given in fractions of the tile. */
    private fun PixelMap.liquidIn(x0: Float, x1: Float, y0: Float, y1: Float): Int {
        var count = 0
        for (x in (x0 * width).roundToInt() until (x1 * width).roundToInt()) {
            for (y in (y0 * height).roundToInt() until (y1 * height).roundToInt()) {
                if (this[x, y].isLiquid()) count++
            }
        }
        return count
    }

    @Test
    fun everySectionDrawsAtTwoSizes() {
        var shown by mutableStateOf(PipeSection.H)
        compose.setContent {
            Row {
                PipeTile(shown, Modifier.size(48.dp))
                PipeTile(shown, Modifier.size(96.dp))
            }
        }
        for (section in PipeSection.entries) {
            shown = section
            compose.waitForIdle()
            val tiles = compose.onAllNodesWithTag(pipeTileTag(section))
            tiles[0].assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
            tiles[1].assertWidthIsEqualTo(96.dp).assertHeightIsEqualTo(96.dp)
            for (i in 0..1) {
                // The art covers a good part of the tile; the rest shows the background, which is
                // what the top-left corner pixel is for every section (none is open at a corner).
                val image = tiles[i].captureToImage().toPixelMap()
                val background = image[0, 0]
                var art = 0
                for (x in 0 until image.width) for (y in 0 until image.height) if (image[x, y] != background) art++
                val share = art.toFloat() / (image.width * image.height)
                assertTrue("$section covers $share of its tile", share > 0.25f && share < 0.95f)
            }
        }
    }

    @Test
    fun dryPipeHasNoLiquid() {
        var shown by mutableStateOf(PipeSection.H)
        compose.setContent { PipeTile(shown, Modifier.size(120.dp)) }
        for (section in PipeSection.entries) {
            shown = section
            compose.waitForIdle()
            assertEquals(section.name, 0, pixels(section).liquidIn(0f, 1f, 0f, 1f))
        }
    }

    @Test
    fun straightHalfFilledFromEitherSide() {
        show(PipeSection.H)
        fills = mapOf("channel-main" to ChannelFill(0.5f, Side.LEFT))
        compose.waitForIdle()
        pixels(PipeSection.H).let {
            assertTrue("left half filled", it.liquidIn(0.02f, 0.4f, 0.45f, 0.55f) > 0)
            assertEquals("right part empty", 0, it.liquidIn(0.65f, 0.98f, 0.3f, 0.7f))
        }
        fills = mapOf("channel-main" to ChannelFill(0.5f, Side.RIGHT))
        compose.waitForIdle()
        pixels(PipeSection.H).let {
            assertTrue("right half filled", it.liquidIn(0.6f, 0.98f, 0.45f, 0.55f) > 0)
            assertEquals("left part empty", 0, it.liquidIn(0.02f, 0.35f, 0.3f, 0.7f))
        }
    }

    @Test
    fun cornerFillsTheLegOfItsEntrySide() {
        show(PipeSection.LT)
        fills = mapOf("channel-main" to ChannelFill(0.3f, Side.TOP))
        compose.waitForIdle()
        pixels(PipeSection.LT).let {
            assertTrue("top leg filled", it.liquidIn(0.45f, 0.55f, 0.01f, 0.1f) > 0)
            assertEquals("left leg empty", 0, it.liquidIn(0.01f, 0.1f, 0.4f, 0.6f))
        }
    }

    @Test
    fun crossChannelsAreIndependent() {
        show(PipeSection.X)
        fills = mapOf("channel-h" to ChannelFill.full(Side.RIGHT))
        compose.waitForIdle()
        pixels(PipeSection.X).let {
            assertTrue("horizontal filled", it.liquidIn(0.02f, 0.98f, 0.45f, 0.55f) > 0)
            assertEquals("top leg dry", 0, it.liquidIn(0.4f, 0.6f, 0.01f, 0.2f))
            assertEquals("bottom leg dry", 0, it.liquidIn(0.4f, 0.6f, 0.8f, 0.99f))
        }
        fills = mapOf(
            "channel-h" to ChannelFill.full(Side.RIGHT),
            "channel-v" to ChannelFill(0.5f, Side.BOTTOM),
        )
        compose.waitForIdle()
        pixels(PipeSection.X).let {
            assertTrue("bottom leg filled", it.liquidIn(0.45f, 0.55f, 0.8f, 0.99f) > 0)
            assertEquals("top leg still dry", 0, it.liquidIn(0.4f, 0.6f, 0.01f, 0.2f))
        }
    }

    @Test
    fun reservoirTankFillsBottomUp() {
        show(PipeSection.RES)
        fills = mapOf("channel-main" to ChannelFill(0.5f, Side.LEFT))
        compose.waitForIdle()
        pixels(PipeSection.RES).let {
            assertTrue("inlet stub filled", it.liquidIn(0.02f, 0.18f, 0.45f, 0.55f) > 0)
            assertTrue("lower half of the tank filled", it.liquidIn(0.4f, 0.6f, 0.55f, 0.72f) > 0)
            assertEquals("upper half of the tank dry", 0, it.liquidIn(0.35f, 0.65f, 0.28f, 0.45f))
            assertEquals("outlet stub dry", 0, it.liquidIn(0.82f, 0.98f, 0.4f, 0.6f))
        }
    }

    @Test
    fun loadingAllAssetsIsFast() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val library = PipeArtLibrary { assets.open(it) }
        val ms = measureTimeMillis { library.preloadAll() }
        assertTrue("Loading took $ms ms", ms < 100)
    }
}
