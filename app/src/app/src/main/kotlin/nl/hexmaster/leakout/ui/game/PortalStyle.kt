package nl.hexmaster.leakout.ui.game

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/** The symbol of a portal pair, so colour is never the only cue (design system 2.5). */
enum class PortalSymbol(@StringRes val spokenName: Int) {
    CIRCLE(R.string.portal_symbol_circle),
    TRIANGLE(R.string.portal_symbol_triangle),
    SQUARE(R.string.portal_symbol_square),
}

/**
 * How a portal pair looks: pair 1, 2 and 3 take the design system's `portal.1` to `portal.3`
 * colour and their own symbol, so two pairs always differ in both. Kept free of drawing so it is
 * JVM-testable.
 */
object PortalStyle {
    /** One turn of the swirl on the ring, `motion.drift`. */
    const val SWIRL_PERIOD_MS: Long = 1_600

    private val colors = listOf(LeakOutColors.Portal1, LeakOutColors.Portal2, LeakOutColors.Portal3)

    /** The colour of pair [pair], starting at 1. */
    fun color(pair: Int): Color = colors[(pair - 1).mod(colors.size)]

    /** The symbol of pair [pair], starting at 1. */
    fun symbol(pair: Int): PortalSymbol = PortalSymbol.entries[(pair - 1).mod(PortalSymbol.entries.size)]

    /**
     * The swirl's angle on the ring at game time [nowMs], in degrees. Game time stands still on
     * pause, so the swirl freezes with it; with reduced motion the ring does not turn at all.
     */
    fun swirlDegrees(nowMs: Long, reducedMotion: Boolean): Float =
        if (reducedMotion) 0f else (nowMs.mod(SWIRL_PERIOD_MS)).toFloat() / SWIRL_PERIOD_MS * 360f

    /**
     * How full the stub of the portal on [position] is, 0 to 1. The pair's progress is split over
     * its two portals: 0 to 0.5 fills the entry portal's stub from its open side to the ring, 0.5
     * to 1 the exit portal's stub from the ring to its open side. Both stay full once the pair is
     * passed ([used] holds its channel).
     */
    fun stubProgress(position: Position, portal: Cell.Portal, used: Set<ChannelUse>, head: FlowState.Flowing?): Float = when {
        ChannelUse(position, Channel.PORTAL) in used -> 1f
        head == null || head.channel != Channel.PORTAL -> 0f
        head.position == position -> (head.progress.toFloat() * 2f).coerceIn(0f, 1f)
        head.position == portal.partner -> (head.progress.toFloat() * 2f - 1f).coerceIn(0f, 1f)
        else -> 0f
    }
}
