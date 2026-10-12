package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.core.board.Board
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
import nl.hexmaster.leakout.core.stack.Dealer
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.game.GameHud
import nl.hexmaster.leakout.ui.game.HudLayout
import nl.hexmaster.leakout.ui.game.PlayingField
import nl.hexmaster.leakout.ui.game.PlayingFieldState
import nl.hexmaster.leakout.ui.game.SectionDragState
import nl.hexmaster.leakout.ui.game.StackLayout
import nl.hexmaster.leakout.ui.game.StackPanel
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/**
 * Still pictures of the tip overlay (design-system.md 5.9) for the design review on issue #36:
 * the real HUD, stack rail and field of a small level under the real [TipOverlay], with animations
 * off. No button does anything.
 */
enum class TipMockupStep { Start, Placing, Tap, Cross }

object TipMockupTags {
    const val SCENE = "tip_mockup_scene"
    const val CALLOUT = TipOverlayTags.CARD
}

private val MockLevel = Level(
    id = 4,
    name = "Mock-up",
    grid = GridSize(8, 5),
    start = Endpoint(Position(0, 2), Side.RIGHT),
    finish = Endpoint(Position(7, 1), Side.LEFT),
    blocked = listOf(Position(3, 2), Position(5, 3)),
    flow = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 0.8),
    stack = StackSettings(visibleSize = 5),
    rules = PlacementRules(),
    stars = StarBenchmarks(100, 200, 350),
)

/** Top to bottom; the last one is the next section. The `X` sits in a slot the rail shows whole. */
private val MockStackSections = listOf(PipeSection.V, PipeSection.H, PipeSection.LT, PipeSection.X, PipeSection.V, PipeSection.H)

@Composable
fun TipMockupScene(step: TipMockupStep, modifier: Modifier = Modifier) {
    val board = remember { Board.from(MockLevel) }
    val stack = remember { PipeStack(MockStackSections, Dealer.initial(MockLevel.stack)) }
    val dragState = remember { SectionDragState() }
    val fieldState = remember { PlayingFieldState(MockLevel.grid, MockLevel.start.position) }
    val targets = remember { TipTargets(fieldState) }
    val hintCell = remember { TipScheduler.hintCell(MockLevel, board) }

    CompositionLocalProvider(LocalTipTargets provides targets) {
        Box(modifier.fillMaxSize().background(LeakOutColors.SurfaceDeep).testTag(TipMockupTags.SCENE)) {
            Column(Modifier.fillMaxSize()) {
                GameHud(MockLevel.id, 0.8, 10_000, 10_000, MockLevel.stars, 0, onPause = {}, modifier = Modifier.fillMaxWidth(), reducedMotion = true)
                Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                    StackPanel(
                        stack = stack,
                        visibleSize = MockLevel.stack.visibleSize,
                        dragState = dragState,
                        enabled = false,
                        modifier = Modifier.width(StackLayout.metrics(360.dp - HudLayout.HEIGHT, MockLevel.stack.visibleSize).columnWidth + 16.dp),
                    )
                    PlayingField(
                        board = board,
                        state = fieldState,
                        highlighted = null,
                        enabled = false,
                        modifier = Modifier.padding(start = 12.dp).weight(1f).fillMaxHeight(),
                    )
                }
            }

            val next = stack.size - 1
            val crossSlot = TipScheduler.tipSlot(stack, PipeSection.X, targets.fullyVisibleSlots.orEmpty())
            val spotlights = when (step) {
                TipMockupStep.Start -> listOfNotNull(targets.cell(MockLevel.start.position))
                TipMockupStep.Placing -> listOfNotNull(targets.slot(next), hintCell?.let(targets::cell))
                TipMockupStep.Tap -> listOfNotNull(hintCell?.let(targets::cell))
                TipMockupStep.Cross -> listOfNotNull(crossSlot?.let(targets::slot))
            }
            // Wait one layout pass, until the targets are known.
            if (spotlights.isEmpty()) return@Box
            val content = when (step) {
                TipMockupStep.Start -> tipContent(TipId.Intro, 0, 30)
                TipMockupStep.Placing -> tipContent(TipId.Intro, 3, 30)
                TipMockupStep.Tap -> tipContent(TipId.Intro, 4, 30)
                TipMockupStep.Cross -> tipContent(TipId.Cross, 0, 30)
            }
            val hint: TipHint? = when (step) {
                TipMockupStep.Placing -> if (spotlights.size == 2) DragHint(stack.next, spotlights[0], spotlights[1]) else null
                TipMockupStep.Tap -> TapHint(spotlights[0])
                else -> null
            }
            TipOverlay(content, spotlights, hint, onNext = {}, onSkip = {}, onDone = {}, reducedMotion = true)
        }
    }
}

@Preview(name = "Tip mock-up: start step, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun TipMockupStartPreview() {
    LeakOutTheme { TipMockupScene(TipMockupStep.Start) }
}

@Preview(name = "Tip mock-up: placing step with hint, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun TipMockupPlacingPreview() {
    LeakOutTheme { TipMockupScene(TipMockupStep.Placing) }
}

@Preview(name = "Tip mock-up: tap to place step, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun TipMockupTapPreview() {
    LeakOutTheme { TipMockupScene(TipMockupStep.Tap) }
}

@Preview(name = "Tip mock-up: cross tip on the stack, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun TipMockupCrossPreview() {
    LeakOutTheme { TipMockupScene(TipMockupStep.Cross) }
}
