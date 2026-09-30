package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.random.Random
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.core.stack.WeightedPool
import nl.hexmaster.leakout.ui.theme.LeakOutColors

@Composable
fun GameScreen(level: Level, random: Random = Random.Default) {
    val pool = remember(level) { WeightedPool(level.stack.weights) }
    var stack by remember(level, random) { mutableStateOf(PipeStack.create(level.stack, random)) }
    val dragState = remember { SectionDragState() }
    val placed = remember { mutableStateMapOf<Position, PipeSection>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LeakOutColors.SurfaceDeep)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(12.dp)
            .testTag(GameScreenTags.SCREEN),
    ) {
        Row(Modifier.fillMaxSize()) {
            StackPanel(stack, dragState, Modifier.width(StackLayout.metrics(360.dp, level.stack.visibleSize).columnWidth + 16.dp))
            val target = PlaceholderBoard(level, placed, Modifier.padding(start = 12.dp).weight(1f))
            dragState.dropTarget = SectionDropTarget { section, position ->
                val accepted = target.tryPlace(section, position)
                if (accepted) stack = stack.afterPlacement(pool, random)
                accepted
            }
        }
        DragOverlay(dragState)
    }
}

@Composable
private fun DragOverlay(dragState: SectionDragState) {
    val section = dragState.draggedSection ?: return
    val density = LocalDensity.current
    val offsetPx = with(density) { DraggedCopyYOffset.toPx() }
    val copySizePx = with(density) { DraggedCopySize.toPx() }
    val target = if (dragState.isAnimatingBack) dragState.slotCenter else dragState.fingerPosition + Offset(0f, -offsetPx)
    val animated by animateOffsetAsState(target, tween(150), label = "dragCopyReturn")
    LaunchedEffect(dragState.isAnimatingBack, animated, target) {
        if (dragState.isAnimatingBack && (animated - target).getDistance() < 1f) {
            dragState.finishReturn()
        }
    }
    Box(
        Modifier
            .testTag(GameScreenTags.DRAGGED)
            .offset { IntOffset((animated.x - copySizePx / 2).roundToInt(), (animated.y - copySizePx / 2).roundToInt()) }
            .size(DraggedCopySize),
    ) {
        PipeSectionGlyph(section, Modifier.fillMaxSize())
    }
}

private val DraggedCopySize = 56.dp

