package nl.hexmaster.leakout.core.flow

import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection

/**
 * The one rule for when a mandatory section is used: the liquid has run through every channel it
 * has, in either direction. The route check, the win or loss at the finish and the accessibility
 * text all ask this, so they cannot disagree.
 */
object MandatoryProgress {
    /** The channels the liquid must run through: both axes of a cross, the single channel of anything else. */
    fun channelsOf(section: PipeSection): Set<Channel> =
        if (section == PipeSection.X) setOf(Channel.HORIZONTAL, Channel.VERTICAL) else setOf(Channel.SINGLE)

    fun isUsed(mandatory: MandatorySection, used: Set<ChannelUse>): Boolean =
        channelsOf(mandatory.section).all { ChannelUse(mandatory.position, it) in used }

    /** The sections of [mandatory] that are not used by [used], in their given order. */
    fun unused(mandatory: List<MandatorySection>, used: Set<ChannelUse>): List<MandatorySection> =
        mandatory.filterNot { isUsed(it, used) }
}
