package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The HUD only keeps clear of a display cutout that reaches into its band (issue #184). */
@RunWith(AndroidJUnit4::class)
class HudCutoutTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cameraHoleBesideTheFieldLeavesTheHudFullWidth() {
        val width = screenWidth()
        // A camera hole in the middle of the right edge, far below the HUD.
        show(listOf(CutoutRect(width - 90, 4_000, width, 4_120)))

        val hud = compose.onNodeWithTag(HudTags.HUD).getUnclippedBoundsInRoot()
        assertEquals(0f, hud.left.value, 0.5f)
        assertEquals(800f, hud.right.value, 0.5f)
    }

    @Test
    fun cameraHoleBesideTheStackLeavesTheHudFullWidth() {
        show(listOf(CutoutRect(0, 4_000, 90, 4_120)))

        val hud = compose.onNodeWithTag(HudTags.HUD).getUnclippedBoundsInRoot()
        assertEquals(0f, hud.left.value, 0.5f)
        assertEquals(800f, hud.right.value, 0.5f)
    }

    @Test
    fun cutoutInTheHudBandPushesOnlyThatSideIn() {
        val cutoutRight = with(compose.density) { 40.dp.roundToPx() }
        // A cutout on the left that covers every possible HUD band.
        show(listOf(CutoutRect(0, 0, cutoutRight, 100_000)))

        val hud = compose.onNodeWithTag(HudTags.HUD).getUnclippedBoundsInRoot()
        assertEquals(40f, hud.left.value, 0.5f)
        assertEquals(800f, hud.right.value, 0.5f)
    }

    @Test
    fun squareCornersKeepTheDefaultContentPadding() {
        show(emptyList(), corners = emptyList())

        val hud = compose.onNodeWithTag(HudTags.HUD).getUnclippedBoundsInRoot()
        val level = compose.onNodeWithTag(HudTags.LEVEL).getUnclippedBoundsInRoot()
        val cog = compose.onNodeWithTag(HudTags.PAUSE).getUnclippedBoundsInRoot()
        assertEquals(hud.left.value + HudContentStart.value, level.left.value, 0.5f)
        assertEquals(hud.right.value - HudContentEnd.value, cog.right.value, 0.5f)
    }

    @Test
    fun roundedCornersMoveTheContentInsideTheCurve() {
        val width = screenWidth()
        val radius = with(compose.density) { 200.dp.roundToPx() }
        // Large rounded corners, so the curve still reaches in below a visible status bar.
        show(
            emptyList(),
            corners = listOf(
                ScreenCorner(radius, radius, radius, onLeft = true),
                ScreenCorner(radius, width - radius, radius, onLeft = false),
            ),
        )

        val hud = compose.onNodeWithTag(HudTags.HUD).getUnclippedBoundsInRoot()
        val level = compose.onNodeWithTag(HudTags.LEVEL).getUnclippedBoundsInRoot()
        val cog = compose.onNodeWithTag(HudTags.PAUSE).getUnclippedBoundsInRoot()
        // The panel still spans the full width; only its content moves in.
        assertEquals(0f, hud.left.value, 0.5f)
        assertTrue(level.left.value > hud.left.value + HudContentStart.value)
        assertTrue(cog.right.value < hud.right.value - HudContentEnd.value)
    }

    private fun screenWidth(): Int {
        var width = 0
        compose.runOnUiThread { width = compose.activity.window.decorView.rootView.width }
        return width
    }

    private fun show(cutouts: List<CutoutRect>, corners: List<ScreenCorner> = emptyList()) {
        val level = level()
        compose.setContent {
            LeakOutTheme {
                CompositionLocalProvider(LocalCutoutRects provides cutouts, LocalScreenCorners provides corners) {
                    // The test activity shows the system bars; the game hides them, so measure a bare screen.
                    Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                        GameScreen(level = level, onExit = {}, session = GameSession(level, Random(7)))
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun level(): Level = Level(
        id = 1,
        name = "Test",
        grid = GridSize(4, 4),
        start = Endpoint(Position(0, 0), Side.RIGHT),
        finish = Endpoint(Position(3, 3), Side.LEFT),
        blocked = emptyList(),
        flow = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 0.8),
        stack = StackSettings(
            visibleSize = 5,
            weights = PipeSection.entries.associateWith { if (it == PipeSection.H) 1 else 0 },
        ),
        rules = PlacementRules(),
        stars = StarBenchmarks(100, 200, 350),
    )
}
