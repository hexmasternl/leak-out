package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.ui.theme.LeakOutColors

object GameScreenTags {
    const val SCREEN = "game_screen"
    const val STACK_PANEL = "stack_panel"
    const val FIELD = "playing_field"
    const val DRAGGED = "dragged_section"

    /** Every visible field cell has the tag `cell_<x>_<y>`. */
    const val CELL_PREFIX = "cell_"

    fun cell(x: Int, y: Int): String = "$CELL_PREFIX${x}_$y"
}

@Composable
fun StackPanel(
    stack: PipeStack,
    dragState: SectionDragState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BoxWithConstraints(
        modifier = modifier
            .testTag(GameScreenTags.STACK_PANEL)
            .fillMaxHeight()
            .background(LeakOutColors.SurfaceDeep.copy(alpha = 0.90f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val metrics = remember(maxHeight, stack.size) { StackLayout.metrics(maxHeight, stack.size) }
        val downOffset by animateFloatAsState(0f, tween(150), label = "stackMoveDown")
        Column(
            modifier = Modifier.width(metrics.columnWidth),
            verticalArrangement = Arrangement.spacedBy(metrics.gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            stack.sections.forEachIndexed { index, section ->
                val isNext = index == stack.sections.lastIndex
                StackSlot(
                    section = section,
                    isNext = isNext,
                    draggable = isNext && enabled,
                    isEmpty = isNext && dragState.draggedSection == section && dragState.isDragActive,
                    size = if (isNext) metrics.nextSlotSize else metrics.previewSlotSize,
                    dragState = dragState,
                    modifier = Modifier,
                    yOffset = downOffset,
                )
            }
        }
    }
}

@Composable
private fun StackSlot(
    section: PipeSection,
    isNext: Boolean,
    draggable: Boolean,
    isEmpty: Boolean,
    size: androidx.compose.ui.unit.Dp,
    dragState: SectionDragState,
    modifier: Modifier,
    yOffset: Float,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val contentDescription = if (isNext) {
        stringResource(R.string.game_stack_next_section_description, section.label())
    } else {
        section.label()
    }
    val slotModifier = modifier
        .size(size)
        .semantics { this.contentDescription = contentDescription }
        .onGloballyPositioned { coordinates = it }
        .background(
            if (isNext) LeakOutColors.SecondaryBase.copy(alpha = 0.28f) else LeakOutColors.SurfaceCard.copy(alpha = 0.08f),
            RoundedCornerShape(12.dp),
        )
        .border(
            if (isNext) 2.dp else 1.dp,
            if (isNext) LeakOutColors.SecondaryBase else Color.White.copy(alpha = 0.16f),
            RoundedCornerShape(12.dp),
        )
        .padding(4.dp)
        .then(
            if (draggable) {
                Modifier.pointerInput(section, dragState) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            coordinates?.let { coords ->
                                val position = coords.positionInRoot(offset)
                                val center = coords.positionInRoot(Offset(coords.size.width / 2f, coords.size.height / 2f))
                                val dropOffset = Offset(0f, with(density) { -DraggedCopyYOffset.toPx() })
                                dragState.beginDrag(section, position, center, dropOffset)
                            }
                        },
                        onDrag = { change, _ ->
                            coordinates?.let { coords -> dragState.moveTo(coords.positionInRoot(change.position)) }
                        },
                        onDragEnd = {
                            dragState.dropPoint?.let(dragState::endDrag)
                        },
                        onDragCancel = { dragState.cancelDrag() },
                    )
                }
            } else {
                Modifier
            },
        )
    Box(slotModifier, contentAlignment = Alignment.Center) {
        if (isEmpty) {
            Spacer(Modifier.size(size))
        } else {
            PipeSectionGlyph(section, Modifier.size(size - 8.dp))
        }
    }
}

@Composable
fun PipeSection.label(): String = when (this) {
    PipeSection.H -> stringResource(R.string.game_section_h)
    PipeSection.V -> stringResource(R.string.game_section_v)
    PipeSection.LT -> stringResource(R.string.game_section_lt)
    PipeSection.LB -> stringResource(R.string.game_section_lb)
    PipeSection.RT -> stringResource(R.string.game_section_rt)
    PipeSection.RB -> stringResource(R.string.game_section_rb)
    PipeSection.X -> stringResource(R.string.game_section_x)
    PipeSection.RES -> stringResource(R.string.game_section_res)
}

val DraggedCopyYOffset = 20.dp
