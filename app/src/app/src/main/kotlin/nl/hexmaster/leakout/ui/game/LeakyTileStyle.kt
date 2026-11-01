package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.level.Position

/** How wet a leaky tile looks (design system 6.2 and 6.5). */
enum class LeakyWetness {
    /** The liquid has not reached the tile: a faint damp stain. */
    DAMP,

    /** The liquid head is in the section on the tile: drops fall from the pipe. */
    DRIPPING,

    /** The liquid has passed the tile: a darker wet stain for the rest of the attempt. */
    WET,
}

/**
 * How a leaky tile and the liquid passing it look. Kept free of drawing so it is JVM-testable.
 */
object LeakyTileStyle {
    /** The drops falling at the same time while the head is in a section on a leaky tile. */
    const val DROP_COUNT: Int = 3

    /** How long one drop takes to fall from the bore to the cell's bottom edge, in game milliseconds. */
    const val DROP_FALL_MS: Long = 600

    /** The time between two drops, so the three are evenly staggered. */
    const val DROP_STAGGER_MS: Long = DROP_FALL_MS / DROP_COUNT

    /** The damp stain's alpha before the liquid has passed, and the wet stain's after. */
    const val DAMP_ALPHA: Float = 0.25f
    const val WET_ALPHA: Float = 0.45f

    /**
     * How wet the leaky tile on [position], holding [cell], looks while the liquid head is in
     * [head] (null when the liquid is not flowing). Drops only fall while the head is in the tile's
     * section; with [reducedMotion] there are no drops and the wet stain shows as soon as the liquid
     * has entered the section.
     */
    fun wetness(cell: Cell, position: Position, head: Position?, reducedMotion: Boolean): LeakyWetness {
        val entered = (cell as? Cell.Pipe)?.liquidEntered == true
        return when {
            !entered -> LeakyWetness.DAMP
            position == head && !reducedMotion -> LeakyWetness.DRIPPING
            else -> LeakyWetness.WET
        }
    }

    /** The stain's alpha for [wetness]. */
    fun stainAlpha(wetness: LeakyWetness): Float = if (wetness == LeakyWetness.WET) WET_ALPHA else DAMP_ALPHA

    /**
     * How far each of the [DROP_COUNT] drops has fallen at game time [nowMs], 0 (at the bore) to
     * just below 1 (at the bottom edge). Game time stands still on pause, so the drops freeze with it.
     */
    fun drops(nowMs: Long): List<Float> = List(DROP_COUNT) { index ->
        (nowMs + index * DROP_STAGGER_MS).mod(DROP_FALL_MS).toFloat() / DROP_FALL_MS
    }
}
