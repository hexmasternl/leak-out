package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.random.Random
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors

object GameScreenTags {
    const val SCREEN = "game_screen"
    const val STACK_PANEL = "stack_panel"
    const val FIELD = "playing_field"
    const val DRAGGED = "dragged_section"

    /** Every visible field cell has the tag `cell_<x>_<y>`. */
    const val CELL_PREFIX = "cell_"

    /** Every composed stack slot has the tag `stack_section_<id>`, with the section's `PipeStack.idAt`. */
    const val STACK_SECTION_PREFIX = "stack_section_"

    fun cell(x: Int, y: Int): String = "$CELL_PREFIX${x}_$y"

    fun stackSection(id: Int): String = "$STACK_SECTION_PREFIX$id"
}

/**
 * The stack rail (design system 5.4): the next section at the bottom, the rest above it, clipped at the top edge.
 * The slot sizes come from [visibleSize], so those bottom slots always fit; the sections fall into place under
 * gravity after every placement.
 */
@Composable
fun StackPanel(
    stack: PipeStack,
    visibleSize: Int,
    dragState: SectionDragState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val reducedMotion = rememberReducedMotion()
    BoxWithConstraints(
        modifier = modifier
            .testTag(GameScreenTags.STACK_PANEL)
            .fillMaxHeight()
            .background(LeakOutColors.SurfaceDeep.copy(alpha = 0.90f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
            .padding(8.dp)
            .clipToBounds(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val railHeight = maxHeight
        val metrics = remember(railHeight, visibleSize) { StackLayout.metrics(railHeight, visibleSize) }
        val tops = remember(metrics, stack.size, railHeight) { StackLayout.slotTops(metrics, stack.size, railHeight) }
        val fullyVisible = remember(metrics, stack.size, railHeight) { StackLayout.fullyVisibleSlots(metrics, stack.size, railHeight) }
        val composed = remember(metrics, stack.size, railHeight) { StackLayout.composedSlots(metrics, stack.size, railHeight) }

        val motion = remember { StackMotion() }
        val falls = motion.sync(stack, metrics.pitch.value, reducedMotion)
        LaunchedEffect(falls) { motion.run() }

        stack.sections.forEachIndexed { index, section ->
            if (index !in composed) return@forEachIndexed
            val id = stack.idAt(index)
            val isNext = index == stack.sections.lastIndex
            key(id) {
                StackSlot(
                    id = id,
                    section = section,
                    isNext = isNext,
                    accessible = index in fullyVisible,
                    draggable = isNext && enabled,
                    isEmpty = isNext && dragState.draggedSection == section && dragState.isDragActive,
                    metrics = metrics,
                    motion = motion,
                    dragState = dragState,
                    modifier = Modifier.offset(y = tops[index]),
                )
            }
        }
    }
}

/**
 * The frame clock and the [StackFalls] of one stack panel. [sync] runs in composition and only reads the clock
 * without observing it, so the panel does not recompose every frame; the slots read it in their graphics layers.
 */
@Stable
private class StackMotion {
    var frameMs by mutableLongStateOf(0L)
        private set
    private var falls: StackFalls? by mutableStateOf(null)

    fun sync(stack: PipeStack, pitch: Float, reduced: Boolean): StackFalls = Snapshot.withoutReadObservation {
        val ids = stack.sections.indices.map(stack::idAt)
        val current = falls
        val next = current?.sync(ids, stack.placements, pitch, frameMs, reduced) ?: StackFalls.resting(ids, stack.placements)
        if (next !== current) falls = next
        next
    }

    /** Offset in dp above the slot. Reads the frame clock, so call it from a graphics layer or a recomposing scope. */
    fun offsetOf(id: Int): Float = falls?.offsetOf(id, frameMs) ?: 0f

    fun alphaOf(id: Int): Float = falls?.alphaOf(id, frameMs) ?: 1f

    /** Advances the clock every frame while a section is still falling. */
    suspend fun run() {
        do {
            withFrameMillis { now ->
                falls = falls?.stamp(now)
                frameMs = now
            }
        } while (falls?.isActive(frameMs) == true)
    }
}

@Composable
private fun StackSlot(
    id: Int,
    section: PipeSection,
    isNext: Boolean,
    accessible: Boolean,
    draggable: Boolean,
    isEmpty: Boolean,
    metrics: StackLayoutMetrics,
    motion: StackMotion,
    dragState: SectionDragState,
    modifier: Modifier,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val contentDescription = if (isNext) {
        stringResource(R.string.game_stack_next_section_description, section.label())
    } else {
        section.label()
    }
    val slotSize = if (isNext) metrics.nextSlotSize else metrics.previewSlotSize
    // The section falling into the next slot grows from a preview into the next section while it falls.
    val growth = if (isNext && !isEmpty) 1f - (motion.offsetOf(id) / metrics.pitch.value).coerceIn(0f, 1f) else 1f
    val visualSize = if (isNext) lerp(metrics.previewSlotSize, metrics.nextSlotSize, growth) else slotSize
    val frameColor = if (isNext) lerp(Color.White.copy(alpha = 0.16f), LeakOutColors.SecondaryBase, growth) else Color.White.copy(alpha = 0.16f)
    val fillColor = if (isNext) {
        lerp(LeakOutColors.SurfaceCard.copy(alpha = 0.08f), LeakOutColors.SecondaryBase.copy(alpha = 0.28f), growth)
    } else {
        LeakOutColors.SurfaceCard.copy(alpha = 0.08f)
    }
    val frameWidth = if (isNext) lerp(1.dp, 2.dp, growth) else 1.dp

    val slotModifier = modifier
        .testTag(GameScreenTags.stackSection(id))
        .size(slotSize)
        .then(
            if (accessible) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier.clearAndSetSemantics {}
            },
        )
        .onGloballyPositioned { coordinates = it }
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
    // The slot stays at its rest position for layout, touch and semantics; only the drawing falls.
    Box(slotModifier, contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .size(visualSize)
                .graphicsLayer {
                    translationY = if (isEmpty) 0f else -motion.offsetOf(id).dp.toPx()
                    alpha = motion.alphaOf(id)
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .background(fillColor, RoundedCornerShape(12.dp))
                .border(frameWidth, frameColor, RoundedCornerShape(12.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!isEmpty) PipeSectionGlyph(section, Modifier.size(visualSize - 8.dp))
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

@Composable
private fun StackRailPreview(visibleSize: Int) {
    val stack = PipeStack.create(StackSettings(visibleSize = visibleSize), Random(visibleSize))
    // The rail as GameScreen lays it out on a 360 dp tall screen: below the HUD, inside the 12 dp row padding.
    Box(Modifier.background(LeakOutColors.SurfaceDeep).padding(top = HudLayout.HEIGHT).padding(12.dp)) {
        StackPanel(
            stack = stack,
            visibleSize = visibleSize,
            dragState = remember { SectionDragState() },
            modifier = Modifier.width(StackLayout.metrics(360.dp - HudLayout.HEIGHT, visibleSize).columnWidth + 16.dp),
        )
    }
}

@Preview(name = "Stack rail, visible size 5, 360 dp", widthDp = 120, heightDp = 360)
@Composable
private fun StackRailFivePreview() = StackRailPreview(visibleSize = 5)

@Preview(name = "Stack rail, visible size 10, 360 dp", widthDp = 120, heightDp = 360)
@Composable
private fun StackRailTenPreview() = StackRailPreview(visibleSize = 10)
